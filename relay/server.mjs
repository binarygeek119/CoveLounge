import http from "node:http";
import { randomInt } from "node:crypto";
import { WebSocketServer } from "ws";

const ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
const PIN_LENGTH = 8;
const LIFETIME_MS = 600_000;
const PORT = Number(process.env.PORT || 8787);
const KEEP_ALIVE_MS = 15_000;

/** @type {Map<string, Waiter>} */
const waiters = new Map();

/**
 * @typedef {{
 *   expiresAt: number,
 *   send: (payload: object) => void,
 *   close: () => void,
 *   timer: ReturnType<typeof setTimeout>,
 * }} Waiter
 */

function normalizePin(value) {
	return String(value || "")
		.toUpperCase()
		.replace(/[^A-Z0-9]/g, "");
}

function makePin() {
	for (let attempt = 0; attempt < 32; attempt++) {
		let pin = "";
		for (let i = 0; i < PIN_LENGTH; i++) {
			pin += ALPHABET[randomInt(ALPHABET.length)];
		}
		if (!waiters.has(pin)) {
			return pin;
		}
	}
	throw new Error("pin space exhausted");
}

function json(res, status, body) {
	const data = JSON.stringify(body);
	res.writeHead(status, {
		"Content-Type": "application/json; charset=utf-8",
		"Cache-Control": "no-store",
		"Access-Control-Allow-Origin": "*",
	});
	res.end(data);
}

function cors(res) {
	res.setHeader("Access-Control-Allow-Origin", "*");
	res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
	res.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
}

function dropWaiter(pin, reason) {
	const waiter = waiters.get(pin);
	if (!waiter) {
		return;
	}
	clearTimeout(waiter.timer);
	waiters.delete(pin);
	if (reason === "expired") {
		try {
			waiter.send({ type: "expired" });
		} catch {
			// already closed
		}
	}
	try {
		waiter.close();
	} catch {
		// already closed
	}
}

function registerWaiter(send, close) {
	const pin = makePin();
	const expiresAt = Date.now() + LIFETIME_MS;
	const waiter = {
		expiresAt,
		send,
		close,
		timer: setTimeout(() => dropWaiter(pin, "expired"), LIFETIME_MS),
	};
	waiters.set(pin, waiter);
	send({ pin, expiresIn: Math.floor(LIFETIME_MS / 1000) });
	return pin;
}

async function readBody(req) {
	const chunks = [];
	for await (const chunk of req) {
		chunks.push(chunk);
		if (chunks.reduce((n, c) => n + c.length, 0) > 64_000) {
			throw new Error("body too large");
		}
	}
	return Buffer.concat(chunks).toString("utf8");
}

function handleDeliver(req, res, pin) {
	if (req.method === "OPTIONS") {
		cors(res);
		res.writeHead(204);
		res.end();
		return;
	}

	if (req.method !== "POST") {
		json(res, 405, { message: "method not allowed" });
		return;
	}

	readBody(req)
		.then((raw) => {
			let ciphertext = "";
			try {
				ciphertext = String(JSON.parse(raw || "{}").ciphertext || "");
			} catch {
				ciphertext = "";
			}
			if (!ciphertext) {
				json(res, 400, { message: "ciphertext required" });
				return;
			}

			const waiter = waiters.get(pin);
			if (!waiter || Date.now() >= waiter.expiresAt) {
				dropWaiter(pin, "gone");
				json(res, 404, { message: "unknown pin" });
				return;
			}

			try {
				waiter.send({ type: "payload", ciphertext });
			} catch {
				json(res, 404, { message: "unknown pin" });
				return;
			}
			dropWaiter(pin, "delivered");
			cors(res);
			res.writeHead(204);
			res.end();
		})
		.catch(() => json(res, 400, { message: "invalid body" }));
}

function handleLongPollWait(req, res) {
	res.writeHead(200, {
		"Content-Type": "application/x-ndjson; charset=utf-8",
		"Cache-Control": "no-store",
		Connection: "keep-alive",
		"Access-Control-Allow-Origin": "*",
		"X-Accel-Buffering": "no",
	});

	const send = (payload) => {
		res.write(`${JSON.stringify(payload)}\n`);
	};
	const close = () => {
		try {
			res.end();
		} catch {
			// already ended
		}
	};

	const pin = registerWaiter(send, close);
	req.on("close", () => dropWaiter(pin, "gone"));
}

function handleHttp(req, res) {
	const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);
	const path = url.pathname.replace(/\/+$/, "") || "/";

	if (req.method === "OPTIONS") {
		cors(res);
		res.writeHead(204);
		res.end();
		return;
	}

	if (path === "/health" && req.method === "GET") {
		json(res, 200, { ok: true, waiting: waiters.size });
		return;
	}

	const deliver = path.match(/^\/v1\/pins\/([^/]+)\/deliver$/);
	if (deliver) {
		handleDeliver(req, res, normalizePin(decodeURIComponent(deliver[1])));
		return;
	}

	if (path === "/v1/wait" && req.method === "GET") {
		handleLongPollWait(req, res);
		return;
	}

	json(res, 404, { message: "not found" });
}

const server = http.createServer(handleHttp);
const wss = new WebSocketServer({ noServer: true });

server.on("upgrade", (req, socket, head) => {
	const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);
	const path = url.pathname.replace(/\/+$/, "") || "/";
	if (path !== "/v1/wait") {
		socket.destroy();
		return;
	}
	wss.handleUpgrade(req, socket, head, (ws) => wss.emit("connection", ws, req));
});

wss.on("connection", (ws) => {
	const send = (payload) => {
		if (ws.readyState === ws.OPEN) {
			ws.send(JSON.stringify(payload));
		}
	};
	const close = () => {
		try {
			ws.close();
		} catch {
			// already closed
		}
	};

	const pin = registerWaiter(send, close);
	const ping = setInterval(() => {
		if (ws.readyState === ws.OPEN) {
			ws.ping();
		}
	}, KEEP_ALIVE_MS);

	ws.on("close", () => {
		clearInterval(ping);
		dropWaiter(pin, "gone");
	});
	ws.on("error", () => {
		clearInterval(ping);
		dropWaiter(pin, "gone");
	});
});

server.listen(PORT, () => {
	console.log(`channelflow-pin-server listening on ${PORT}`);
});

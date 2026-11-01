# CoveLounge pin server

Repo: [github.com/binarygeek119/CoveLounge/tree/main/relay](https://github.com/binarygeek119/CoveLounge/tree/main/relay)

Small always-on pin server for a Google Cloud VM. CoveLounge apps connect and receive a random **8-character quick PIN**. The user types that PIN on Cove’s **Quick Pin** tab. The server encrypts its public and local M3U/XMLTV URLs with the PIN as the seed; this process forwards the ciphertext to the waiting app and never opens it.

[Cove](https://github.com/yourcove/cove) stays on your home machine. Live TV does not go through this VM.

Protocol for the C# Quick Pin tab and the WIP client: [PROTOCOL.md](PROTOCOL.md).

## Local preview

Needs Go 1.22+.

```bash
RELAY_DEV=1 PORT=43123 go run ./cmd/pinserver
```

Open http://127.0.0.1:43123

- **App** — get a PIN (stands in for the CoveLounge client)
- **Quick Pin** — type the PIN, send (stands in for Cove)

Docker:

```bash
cp .env.example .env
# for local HTTP only:
# RELAY_DEV=1 PORT=43123
docker compose up --build
```

GitHub Actions publishes `linux/amd64` images to GHCR on every push to `main`:

```bash
docker pull ghcr.io/binarygeek119/covelounge-pin-server:latest
```

Tags: `latest` (main), `main`, `sha-<commit>`, and `v*` release tags.

## Production (DuckDNS + TLS)

On the VM, with ports **80** and **443** open:

```bash
export DUCKDNS_SUBDOMAIN=covelounge
export DUCKDNS_TOKEN=your-token-from-duckdns.org
# RELAY_DEV must be unset
./pinserver
```

The process updates `covelounge.duckdns.org` every 5 minutes and gets a Let’s Encrypt certificate for that name.

## Google Cloud

You need:

- [gcloud CLI](https://cloud.google.com/sdk/docs/install) and `gcloud auth login`
- A Google Cloud billing account
- A [DuckDNS](https://www.duckdns.org) account with subdomain `covelounge` (or another name if that one is taken) and its token

From this repo:

```bash
chmod +x scripts/gcp-setup.sh
./scripts/gcp-setup.sh
```

The script creates a GCP project, an e2-micro VM, firewall rules for 80/443, stores the DuckDNS token on the VM, points `covelounge.duckdns.org` at the VM, and starts the pin server from `ghcr.io/binarygeek119/covelounge-pin-server:latest`. Cove and the app should use:

`https://covelounge.duckdns.org`

To refresh the VM after a new GitHub build:

```bash
gcloud compute ssh covelounge-pin --project=covelounge-pin --zone=us-central1-a --command="
  cd /opt/covelounge-pin && sudo docker-compose pull && sudo docker-compose up -d
"
```

## Environment

| Variable | Purpose |
| --- | --- |
| `RELAY_DEV` | `1` for local HTTP (no TLS, no DuckDNS) |
| `PORT` | Listen port in dev (default `43123`) |
| `BIND` | Listen address in dev (default `0.0.0.0`) |
| `DUCKDNS_SUBDOMAIN` | DuckDNS name without `.duckdns.org` |
| `DUCKDNS_TOKEN` | DuckDNS token |
| `CERT_DIR` | Let’s Encrypt cache (default `/var/lib/covelounge-pin/certs`) |
| `ACME_EMAIL` | Optional contact for Let’s Encrypt |

No request logs, IPs, PINs, or URLs are written. Fatal errors only go to stderr.

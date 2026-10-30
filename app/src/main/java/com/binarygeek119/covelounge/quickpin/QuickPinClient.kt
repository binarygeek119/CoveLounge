package com.binarygeek119.covelounge.quickpin

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

sealed class PinEvent {
    data class Pin(val pin: String, val display: String, val expiresIn: Int) : PinEvent()
    data class Payload(val ciphertext: String) : PinEvent()
    data class Expired(val message: String = "expired") : PinEvent()
    data class Error(val message: String) : PinEvent()
}

class QuickPinClient(private val relayUrl: String) {
    private val client = OkHttpClient()

    fun connect(): Flow<PinEvent> = callbackFlow {
        val wsUrl = relayUrl.replaceFirst("https", "wss").replaceFirst("http", "ws") + "/v1/wait"
        val request = Request.Builder().url(wsUrl).build()
        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "pin" -> trySend(PinEvent.Pin(json.getString("pin"), json.getString("display"), json.optInt("expiresIn", 600)))
                        "payload" -> trySend(PinEvent.Payload(json.getString("ciphertext")))
                        "expired" -> trySend(PinEvent.Expired())
                        else -> trySend(PinEvent.Error("unknown message"))
                    }
                } catch (e: Exception) {
                    trySend(PinEvent.Error(e.message ?: "parse error"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(PinEvent.Error(t.message ?: "connection failed"))
                close()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }
        })
        awaitClose { ws.close(1000, null) }
    }
}

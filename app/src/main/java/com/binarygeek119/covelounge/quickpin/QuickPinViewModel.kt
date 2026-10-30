package com.binarygeek119.covelounge.quickpin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binarygeek119.covelounge.data.ServerStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class QuickPinViewModel(app: Application, private val relayUrl: String = "https://covelounge.duckdns.org") : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val client = QuickPinClient(relayUrl)

    private val _pinDisplay = MutableStateFlow<String?>(null)
    val pinDisplay: StateFlow<String?> = _pinDisplay
    private var rawPin: String? = null
    private val _status = MutableStateFlow("Connecting…")
    val status: StateFlow<String> = _status
    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    fun start() {
        viewModelScope.launch {
            client.connect().collect { event ->
                when (event) {
                    is PinEvent.Pin -> {
                        rawPin = event.pin
                        _pinDisplay.value = event.display
                        _status.value = "Enter this PIN in Cove"
                    }
                    is PinEvent.Payload -> {
                        val pin = rawPin
                        if (pin == null) { _status.value = "No pin"; return@collect }
                        try {
                            val plain = QuickPinCrypto.decrypt(pin, event.ciphertext)
                            val json = JSONObject(plain)
                            val serverUrl = json.getString("serverUrl")
                            val apiToken = json.getString("apiToken")
                            store.save(serverUrl, apiToken, null)
                            _status.value = "Connected"
                            _success.value = true
                        } catch (e: Exception) {
                            _status.value = "Decrypt failed: ${e.message}"
                        }
                    }
                    is PinEvent.Expired -> _status.value = "Pin expired"
                    is PinEvent.Error -> _status.value = event.message
                }
            }
        }
    }
}

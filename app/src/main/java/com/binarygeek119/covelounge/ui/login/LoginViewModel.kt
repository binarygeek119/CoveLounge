package com.binarygeek119.covelounge.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import com.binarygeek119.covelounge.data.network.LoginRequest
import com.binarygeek119.covelounge.util.PluginHealthChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val session = SessionRepository(store)

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    fun login(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            try {
                val api = session.createApi(serverUrl, null)
                val res = api.login(LoginRequest(username, password))
                store.save(serverUrl, res.token, res.refreshToken)
                session.initFromStore(serverUrl, res.token)
                viewModelScope.launch { PluginHealthChecker.check(serverUrl) }
                _success.value = true
            } catch (e: Exception) {
                _status.value = "Login failed: ${e.message}"
            }
        }
    }

    fun loginWithApiKey(serverUrl: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val api = session.createApi(serverUrl, apiKey)
                api.me()
                store.save(serverUrl, apiKey, null)
                session.initFromStore(serverUrl, apiKey)
                viewModelScope.launch { PluginHealthChecker.check(serverUrl) }
                _success.value = true
            } catch (e: Exception) {
                _status.value = "API key failed: ${e.message}"
            }
        }
    }
}

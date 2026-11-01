package com.binarygeek119.covelounge.ui.pin

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.binarygeek119.covelounge.R
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import com.binarygeek119.covelounge.data.SettingsStore
import com.binarygeek119.covelounge.data.network.CoveApi
import retrofit2.HttpException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random

@Composable
fun AppPinScreen(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val store = remember { SettingsStore(context) }
    val serverStore = remember { ServerStore(context) }
    val session = remember { SessionRepository(serverStore) }

    var storedPinHash by remember { mutableStateOf<String?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    var forgotCode by remember { mutableStateOf<String?>(null) }
    var forgotAppId by remember { mutableStateOf<String?>(null) }
    var forgotStatus by remember { mutableStateOf("") }
    var isPolling by remember { mutableStateOf(false) }
    var forgotError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Collect stored PIN hash
    LaunchedEffect(Unit) {
        store.appPin.collect { hash ->
            storedPinHash = hash
        }
    }

    // Shake animation for wrong PIN
    val shakeOffset by animateFloatAsState(
        targetValue = if (showError) 1f else 0f,
        animationSpec = tween(100),
        finishedListener = {
            if (showError) {
                showError = false
                enteredPin = ""
            }
        },
        label = "shake"
    )

    // Verify PIN when 4 digits entered
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4 && !isVerifying && !showForgot) {
            isVerifying = true
            val hash = SettingsStore.hashPin(enteredPin)
            if (hash == storedPinHash) {
                onUnlocked()
            } else {
                showError = true
            }
            isVerifying = false
        }
    }

    fun startReset() {
        forgotError = null
        forgotStatus = "Connecting to Cove..."
        scope.launch {
            try {
                val serverUrl = serverStore.serverUrl.firstOrNullSync()
                val token = serverStore.accessToken.firstOrNullSync()
                if (serverUrl.isNullOrBlank() || token.isNullOrBlank()) {
                    forgotError = "Not connected to Cove. Please log in first."
                    return@launch
                }
                val api = session.createApi(serverUrl, token)
                // Generate a 6-digit code locally
                val code = (100000 + java.util.Random().nextInt(900000)).toString()
                forgotCode = code
                val reg = api.loungeRegisterReset(mapOf("ResetCode" to code))
                if (!reg.ok) {
                    forgotError = reg.error ?: "Failed to register reset code."
                    return@launch
                }
                forgotStatus = "Enter code $code in Cove (along with your Cove password) to verify your identity."
                isPolling = true
                // Poll for approval
                scope.launch {
                    while (isPolling) {
                        delay(3000)
                        if (!isPolling) break
                        try {
                            val st = api.loungeResetStatus()
                            if (st.approved) {
                                isPolling = false
                                store.removeAppPin()
                                onUnlocked()
                                break
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                forgotError = "HTTP ${e.code()}: ${body ?: e.message()}"
            } catch (e: Exception) {
                forgotError = e.message ?: "Failed to start reset."
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = "CoveLounge", modifier = Modifier.size(96.dp))

            if (!showForgot) {
                Text("Enter PIN", color = Color.White)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.graphicsLayer { translationX = if (showError) shakeOffset * 20f else 0f }
                ) {
                    for (i in 0 until 4) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (i < enteredPin.length) Color(0xFF2196F3) else Color(0x33FFFFFF),
                                    CircleShape
                                )
                        )
                    }
                }

                if (showError) {
                    Text("Incorrect PIN", color = Color.Red)
                }

                NumberPad(
                    onDigit = { digit ->
                        if (enteredPin.length < 4) {
                            enteredPin += digit.toString()
                        }
                    },
                    onBackspace = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    }
                )

                TextButton(onClick = {
                    showForgot = true
                    forgotCode = null
                    forgotError = null
                    forgotStatus = ""
                    isPolling = false
                }) {
                    Text("Forgot PIN", color = Color(0xFF2196F3))
                }
            } else {
                Text("Forgot PIN", color = Color.White, fontWeight = FontWeight.Bold)

                if (forgotCode == null && !isPolling) {
                    Text("Start a PIN reset. A code will be generated for you to enter in Cove.", color = Color.Gray)
                    if (forgotError != null) {
                        Text(forgotError!!, color = Color.Red)
                    }
                    Button(onClick = { startReset() }) {
                        Text("Start reset")
                    }
                } else {
                    if (forgotCode != null) {
                        Text("Verification code:", color = Color.White)
                        Text(forgotCode!!, color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
                    }
                    Text(forgotStatus, color = Color.White)
                    Text("Go to Cove and enter this code (plus your Cove password) to verify.", color = Color.Gray)
                    if (isPolling) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                            Text("Waiting for verification...", color = Color.Gray)
                        }
                    }
                    if (forgotError != null) {
                        Text(forgotError!!, color = Color.Red)
                    }
                }

                TextButton(onClick = {
                    showForgot = false
                    isPolling = false
                    forgotCode = null
                    forgotAppId = null
                    forgotError = null
                    enteredPin = ""
                }) {
                    Text("Back to PIN entry", color = Color(0xFF2196F3))
                }
            }
        }
    }
}

// Extension to allow firstOrNull on Flow without collecting scope
private fun <T> kotlinx.coroutines.flow.Flow<T>.firstOrNullSync(): T? {
    return try { kotlinx.coroutines.runBlocking { this@firstOrNullSync.first() } } catch (_: Exception) { null }
}

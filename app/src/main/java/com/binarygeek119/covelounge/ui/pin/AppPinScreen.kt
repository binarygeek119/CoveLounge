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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import com.binarygeek119.covelounge.data.SettingsStore
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

    val appPinFlow = store.appPin
    var storedPinHash by remember { mutableStateOf<String?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    var forgotCode by remember { mutableStateOf<String?>(null) }
    var forgotStatus by remember { mutableStateOf("") }
    var isPolling by remember { mutableStateOf(false) }

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
        if (enteredPin.length == 4 && !isVerifying) {
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

    // Forgot PIN polling
    LaunchedEffect(isPolling, forgotCode) {
        if (isPolling && forgotCode != null) {
            // Poll server for verification
            // For now, simulate - in production, poll actual endpoint
            // val api = ... createApi ...
            // while (isPolling) {
            //     delay(2000)
            //     val verified = api.checkPinResetStatus(forgotCode)
            //     if (verified) { store.removeAppPin(); onUnlocked(); break }
            // }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo
            Text("CoveLounge", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

            if (!showForgot) {
                // PIN entry
                Text("Enter PIN", color = Color.White)

                // PIN dots
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

                // Number pad
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

                // Forgot PIN button
                TextButton(onClick = {
                    showForgot = true
                    // Generate a random 6-digit code
                    val code = (100000 + Random().nextInt(900000)).toString()
                    forgotCode = code
                    forgotStatus = "Enter code $code in Cove to verify your identity"
                    isPolling = true
                }) {
                    Text("Forgot PIN", color = Color(0xFF2196F3))
                }
            } else {
                // Forgot PIN flow
                Text("Forgot PIN", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(forgotStatus, color = Color.White)

                if (forgotCode != null) {
                    Text("Verification code: $forgotCode", color = Color(0xFFFFC107), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }

                Text("Go to Cove and enter this code to verify your identity.", color = Color.Gray)

                // Simulate verification for testing (remove in production)
                Button(onClick = {
                    scope.launch {
                        // In production: poll server endpoint
                        // For now, simulate success after user confirms
                        store.removeAppPin()
                        isPolling = false
                        onUnlocked()
                    }
                }) {
                    Text("I've verified in Cove")
                }

                TextButton(onClick = {
                    showForgot = false
                    isPolling = false
                    forgotCode = null
                    enteredPin = ""
                }) {
                    Text("Back to PIN entry", color = Color(0xFF2196F3))
                }
            }
        }
    }
}

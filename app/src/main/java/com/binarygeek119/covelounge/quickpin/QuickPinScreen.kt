package com.binarygeek119.covelounge.quickpin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun QuickPinScreen(viewModel: QuickPinViewModel, onConnected: () -> Unit) {
    val pin by viewModel.pinDisplay.collectAsState()
    val status by viewModel.status.collectAsState()
    val success by viewModel.success.collectAsState()
    if (success) onConnected()

    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(status, color = Color.White)
        pin?.let { Text("PIN: $it", color = Color.White) }
    }
}

package com.binarygeek119.covelounge.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoggedIn: () -> Unit, onQuickPin: () -> Unit = {}) {
    val status by viewModel.status.collectAsState()
    val success by viewModel.success.collectAsState()
    if (success) onLoggedIn()

    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var useApiKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Connect to Cove")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = { Text("Server URL") })
        if (useApiKey) {
            OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") })
        } else {
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            if (useApiKey) viewModel.loginWithApiKey(serverUrl, apiKey)
            else viewModel.login(serverUrl, username, password)
        }) { Text(if (useApiKey) "Connect with API Key" else "Sign In") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { useApiKey = !useApiKey }) { Text(if (useApiKey) "Use username/password" else "Use API key") }
        Button(onClick = onQuickPin) { Text("Quick Pin") }
        status?.let { Text(it) }
    }
}

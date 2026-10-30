package com.binarygeek119.covelounge.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import com.binarygeek119.covelounge.data.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val serverStore = remember { ServerStore(context) }
    val session = remember { SessionRepository(serverStore) }
    var accountName by remember { mutableStateOf<String?>(null) }
    var permissions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAbout by remember { mutableStateOf(false) }
    val url by serverStore.serverUrl.collectAsState(initial = null)
    val token by serverStore.accessToken.collectAsState(initial = null)
    val previewMode by store.previewMode.collectAsState(initial = "Off")
    val screenshotCount by store.screenshotCount.collectAsState(initial = 10)
    val screenshotInterval by store.screenshotInterval.collectAsState(initial = 10)
    var selectedMode by remember { mutableStateOf(previewMode) }
    var selectedCount by remember { mutableStateOf(screenshotCount) }
    var selectedInterval by remember { mutableStateOf(screenshotInterval) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(previewMode, screenshotCount, screenshotInterval) {
        selectedMode = previewMode
        selectedCount = screenshotCount
        selectedInterval = screenshotInterval
    }
    LaunchedEffect(url, token) {
        val u = url
        val t = token
        if (u != null && t != null) {
            try {
                val api = session.createApi(u, t)
                val me = api.me()
                accountName = me.user?.username
                permissions = me.permissions
            } catch (_: Exception) {}
        }
    }

    var category by remember { mutableStateOf("main") }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (category) {
            "main" -> {
                Text("Settings")
                Text("Account", color = Color.White, modifier = Modifier.clickable { category = "account" })
                Text("Playback", color = Color.White, modifier = Modifier.clickable { category = "playback" })
                Text("About", color = Color.White, modifier = Modifier.clickable { category = "about" })
                Button(onClick = onBack) { Text("Back") }
            }
            "account" -> {
                Text("Account")
                accountName?.let { Text("Account: $it") }
                if (permissions.isNotEmpty()) Text("Permissions: ${permissions.joinToString(", ")}")
                Button(onClick = onLogout) { Text("Log out") }
                Button(onClick = { category = "main" }) { Text("Back") }
            }
            "playback" -> {
                Text("Playback")
                Text("Preview mode: $selectedMode")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Off", "Muted", "Low", "Full").forEach { mode ->
                        Button(onClick = { selectedMode = mode }) { Text(mode) }
                    }
                }
                Text("Screenshot count: $selectedCount")
                Slider(value = selectedCount.toFloat(), onValueChange = { selectedCount = it.toInt() }, valueRange = 1f..30f, modifier = Modifier.fillMaxWidth())
                Text("Screenshot interval (sec): $selectedInterval")
                Slider(value = selectedInterval.toFloat(), onValueChange = { selectedInterval = it.toInt() }, valueRange = 10f..120f, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        store.setPreviewMode(selectedMode)
                        store.setScreenshotCount(selectedCount)
                        store.setScreenshotInterval(selectedInterval)
                    }
                    category = "main"
                }) { Text("Save") }
                Button(onClick = { category = "main" }) { Text("Cancel") }
            }
            "about" -> {
                Text("About")
                Text("CoveLounge Android TV client")
                Button(onClick = { showAbout = true }) { Text("Version info") }
                Button(onClick = { category = "main" }) { Text("Back") }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About CoveLounge") },
            text = { Text("Android TV client for Cove.\nVersion 0.2.x") },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("OK") } }
        )
    }
}
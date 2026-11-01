package com.binarygeek119.covelounge.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import com.binarygeek119.covelounge.BuildConfig
import com.binarygeek119.covelounge.data.SettingsStore
import com.binarygeek119.covelounge.ui.pin.AppPinScreen
import com.binarygeek119.covelounge.ui.pin.NumberPad
import com.binarygeek119.covelounge.util.ImageCache
import com.binarygeek119.covelounge.util.UpdateChecker
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
    var updateInfo by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    val url by serverStore.serverUrl.collectAsState(initial = null)
    val token by serverStore.accessToken.collectAsState(initial = null)
    val previewMode by store.previewMode.collectAsState(initial = "Off")
    val screenshotCount by store.screenshotCount.collectAsState(initial = 10)
    val screenshotInterval by store.screenshotInterval.collectAsState(initial = 10)
    val useTranscode by store.useTranscode.collectAsState(initial = false)
    val transcodeResolution by store.transcodeResolution.collectAsState(initial = "720p")
    val appPin by store.appPin.collectAsState(initial = null)
    val ratingsOnExit by store.ratingsOnExit.collectAsState(initial = false)
    var selectedMode by remember { mutableStateOf(previewMode) }
    var selectedCount by remember { mutableStateOf(screenshotCount) }
    var selectedInterval by remember { mutableStateOf(screenshotInterval) }
    var selectedUseTranscode by remember { mutableStateOf(useTranscode) }
    var selectedResolution by remember { mutableStateOf(transcodeResolution) }
    var selectedRatingsOnExit by remember { mutableStateOf(ratingsOnExit) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(previewMode, screenshotCount, screenshotInterval, useTranscode, transcodeResolution, ratingsOnExit) {
        selectedMode = previewMode
        selectedCount = screenshotCount
        selectedInterval = screenshotInterval
        selectedUseTranscode = useTranscode
        selectedResolution = transcodeResolution
        selectedRatingsOnExit = ratingsOnExit
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
    BackHandler {
        if (category != "main") category = "main" else onBack()
    }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (category) {
            "main" -> {
                Text("Settings")
                MenuRow("Account") { category = "account" }
                MenuRow("Playback") { category = "playback" }
                MenuRow("About") { category = "about" }
                Button(onClick = onBack) { Text("Back") }
            }
            "account" -> {
                Text("Account")
                accountName?.let { Text("Account: $it") }
                if (permissions.isNotEmpty()) Text("Permissions: ${permissions.joinToString(", ")}")
                var cacheBytes by remember { mutableStateOf<Long?>(null) }
                LaunchedEffect(Unit) { cacheBytes = ImageCache.sizeBytes(context) }
                val cacheMb = cacheBytes?.let { String.format("%.1f", it / (1024.0 * 1024.0)) }
                Button(onClick = {
                    scope.launch {
                        cacheBytes = 0L
                        ImageCache.clear(context)
                        cacheBytes = ImageCache.sizeBytes(context)
                    }
                }) { Text(if (cacheMb != null) "Clear image cache ($cacheMb MB)" else "Clear image cache") }
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
                Text("Use transcoding: $selectedUseTranscode")
                Button(onClick = { selectedUseTranscode = !selectedUseTranscode }) { Text(if (selectedUseTranscode) "On" else "Off") }
                Text("Transcode resolution: $selectedResolution")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("480p", "720p", "1080p").forEach { res ->
                        Button(onClick = { selectedResolution = res }) { Text(res) }
                    }
                }
                Text("Show ratings page on exit: $selectedRatingsOnExit")
                Button(onClick = { selectedRatingsOnExit = !selectedRatingsOnExit }) { Text(if (selectedRatingsOnExit) "On" else "Off") }
                Button(onClick = {
                    scope.launch {
                        store.setPreviewMode(selectedMode)
                        store.setScreenshotCount(selectedCount)
                        store.setScreenshotInterval(selectedInterval)
                        store.setUseTranscode(selectedUseTranscode)
                        store.setTranscodeResolution(selectedResolution)
                        store.setRatingsOnExit(selectedRatingsOnExit)
                    }
                    category = "main"
                }) { Text("Save") }
                Button(onClick = { category = "main" }) { Text("Cancel") }
            }
            "apppin" -> {
                Text("App PIN")
                if (appPin != null) {
                    Text("PIN is currently set", color = Color.Green)
                    Text("Remove PIN", color = Color.White, modifier = Modifier.clickable {
                        scope.launch { store.removeAppPin() }
                    })
                } else {
                    Text("No PIN set", color = Color.Gray)
                    var newPin by remember { mutableStateOf("") }
                    var confirmPin by remember { mutableStateOf("") }
                    var step by remember { mutableStateOf(0) } // 0=enter, 1=confirm
                    var error by remember { mutableStateOf<String?>(null) }

                    if (step == 0) {
                        Text("Enter new 4-digit PIN")
                        NumberPad(
                            onDigit = { d -> if (newPin.length < 4) newPin += d.toString() },
                            onBackspace = { if (newPin.isNotEmpty()) newPin = newPin.dropLast(1) }
                        )
                        Text("Entered: ${"*".repeat(newPin.length)}", color = Color.White)
                        if (newPin.length == 4) {
                            Button(onClick = { step = 1; error = null }) { Text("Next") }
                        }
                    } else {
                        Text("Confirm PIN")
                        NumberPad(
                            onDigit = { d -> if (confirmPin.length < 4) confirmPin += d.toString() },
                            onBackspace = { if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1) }
                        )
                        Text("Entered: ${"*".repeat(confirmPin.length)}", color = Color.White)
                        error?.let { Text(it, color = Color.Red) }
                        if (confirmPin.length == 4) {
                            Button(onClick = {
                                if (newPin == confirmPin) {
                                    scope.launch {
                                        store.setAppPin(newPin)
                                        newPin = ""
                                        confirmPin = ""
                                        step = 0
                                        error = null
                                    }
                                } else {
                                    error = "PINs do not match"
                                    confirmPin = ""
                                }
                            }) { Text("Save PIN") }
                        }
                        Button(onClick = { step = 0; confirmPin = ""; error = null }) { Text("Back") }
                    }
                }
                Button(onClick = { category = "main" }) { Text("Back") }
            }
            "about" -> {
                Text("About")
                Text("CoveLounge Android TV client")
                Button(onClick = { showAbout = true }) { Text("Version info") }
                Button(onClick = {
                    checkingUpdate = true
                    scope.launch {
                        val info = UpdateChecker.checkLatest()
                        checkingUpdate = false
                        if (info != null && UpdateChecker.isNewer(BuildConfig.VERSION_NAME, info.tag)) {
                            updateInfo = info
                        } else {
                            showAbout = true
                        }
                    }
                }) { Text(if (checkingUpdate) "Checking..." else "Check for updates") }
                Button(onClick = { category = "main" }) { Text("Back") }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About CoveLounge") },
            text = { Text("Android TV client for Cove.\nVersion ${BuildConfig.VERSION_NAME}") },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("OK") } }
        )
    }

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Update available") },
            text = { Text("Version ${info.tag} is available. Download and install?") },
            confirmButton = {
                TextButton(onClick = {
                    if (info.apkUrl != null) {
                        scope.launch {
                            val file = UpdateChecker.downloadApk(context, info.apkUrl)
                            if (file != null) UpdateChecker.installApk(context, file)
                            updateInfo = null
                        }
                    }
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) { Text("Later") }
            }
        )
    }
}


@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.material3.Text(
        text = text,
        color = if (focused) Color(0xFF2196F3) else Color.White,
        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(4.dp)
    )
}

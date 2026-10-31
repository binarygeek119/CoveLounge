package com.binarygeek119.covelounge.ui.media

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.binarygeek119.covelounge.data.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(baseUrl: String, token: String, type: String, id: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val useTranscode by settings.useTranscode.collectAsState(initial = false)
    val transcodeResolution by settings.transcodeResolution.collectAsState(initial = "720p")

    val url = when (type) {
        "video" -> if (useTranscode)
            "$baseUrl/api/stream/video/$id/transcode?resolution=$transcodeResolution&access_token=$token"
        else
            "$baseUrl/api/stream/video/$id/hls/master.m3u8?access_token=$token"
        "preview" -> "$baseUrl/api/stream/video/$id/preview?access_token=$token"
        else -> "$baseUrl/api/audios/$id/stream?access_token=$token"
    }

    var showGearMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler { if (showGearMenu) showGearMenu = false else onBack() }

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    // Track playback position so switching quality resumes where you were
    var savedPosition by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (player.playbackState == Player.STATE_READY) savedPosition = player.currentPosition
        }
    }
    LaunchedEffect(url) {
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        if (savedPosition > 0) player.seekTo(savedPosition)
        player.prepare()
        player.playWhenReady = true
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player } },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = { showGearMenu = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Playback settings", tint = Color.White)
        }
    }

    if (showGearMenu) {
        AlertDialog(
            onDismissRequest = { showGearMenu = false },
            title = { Text("Playback quality") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch { settings.setUseTranscode(false) }
                            showGearMenu = false
                        },
                        border = if (!useTranscode) BorderStroke(2.dp, Color(0xFF2196F3)) else null
                    ) { Text("Direct (original)") }
                    listOf("480p", "720p", "1080p").forEach { res ->
                        val selected = useTranscode && transcodeResolution == res
                        Button(
                            onClick = {
                                scope.launch {
                                    settings.setUseTranscode(true)
                                    settings.setTranscodeResolution(res)
                                }
                                showGearMenu = false
                            },
                            border = if (selected) BorderStroke(2.dp, Color(0xFF2196F3)) else null
                        ) { Text(res) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGearMenu = false }) { Text("Close") }
            }
        )
    }
}

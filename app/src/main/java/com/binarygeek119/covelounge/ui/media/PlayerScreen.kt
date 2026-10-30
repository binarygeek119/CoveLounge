package com.binarygeek119.covelounge.ui.media

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.binarygeek119.covelounge.data.SettingsStore

@Composable
fun PlayerScreen(baseUrl: String, token: String, type: String, id: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val useTranscode by settings.useTranscode.collectAsState(initial = false)
    val transcodeResolution by settings.transcodeResolution.collectAsState(initial = "720p")

    val url = when (type) {
        "video" -> if (useTranscode) "$baseUrl/api/stream/video/$id/transcode?resolution=$transcodeResolution&access_token=$token"
        else "$baseUrl/api/stream/video/$id/hls/master.m3u8?access_token=$token"
        "preview" -> "$baseUrl/api/stream/video/$id/preview?access_token=$token"
        else -> "$baseUrl/api/audios/$id/stream?access_token=$token"
    }

    BackHandler { onBack() }
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(url) {
        onDispose { player.release() }
    }
    AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
}
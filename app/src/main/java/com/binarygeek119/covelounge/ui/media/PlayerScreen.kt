package com.binarygeek119.covelounge.ui.media

import android.net.Uri
import android.view.KeyEvent
import com.binarygeek119.covelounge.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.app.Application
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import com.binarygeek119.covelounge.data.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(baseUrl: String, token: String, type: String, id: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val api = remember(baseUrl, token) { SessionRepository(com.binarygeek119.covelounge.data.ServerStore(app)).createApi(baseUrl, token) }
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
    val gearFr = remember { FocusRequester() }
    var gearFocused by remember { mutableStateOf(false) }
    val heartFr = remember { FocusRequester() }
    var heartFocused by remember { mutableStateOf(false) }
    val playbackFr = remember { FocusRequester() }
    var playbackFocused by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val thumbFr = remember { FocusRequester() }
    var thumbFocused by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(0) }
    var viewCount by remember { mutableStateOf(0) }

    // Start with the gear focused; the AndroidView (PlayerView) steals View focus right
    // after attaching, so retry until focus sticks durably.
    LaunchedEffect(Unit) {
        repeat(40) {
            runCatching { playbackFr.requestFocus() }
            delay(250)
            if (playbackFocused) {
                delay(300)
                if (playbackFocused) return@LaunchedEffect
            }
        }
    }

    val hostType = when (type) {
        "audio" -> "Audio"
        else -> "Video"
    }
    LaunchedEffect(api, hostType, id) {
        try { val e = api.getEngagement(hostType, id); isFavorite = e.isFavorite; viewCount = e.playCount } catch (_: Exception) {}
    }
    fun toggleFavorite() {
        scope.launch {
            try {
                val res = api.setFavorite(hostType, id, com.binarygeek119.covelounge.data.network.EntityFavoriteDto(!isFavorite))
                isFavorite = res.isFavorite
            } catch (_: Exception) {}
        }
    }

    fun likeVideo() {
        scope.launch {
            try {
                val count = when (type) {
                    "audio" -> api.likeAudio(id)
                    else -> api.likeVideo(id)
                }
                likeCount = count
            } catch (_: Exception) {}
        }
    }

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

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) false
                else when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        when {
                            playbackFocused -> { if (player.isPlaying) player.pause() else player.play(); true }
                            gearFocused -> { showGearMenu = true; true }
                            heartFocused -> { toggleFavorite(); true }
                            thumbFocused -> { likeVideo(); true }
                            else -> { if (player.isPlaying) player.pause() else player.play(); true }
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (heartFocused || thumbFocused || gearFocused) {
                            runCatching { playbackFr.requestFocus() }; true
                        } else {
                            showGearMenu = true; true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (playbackFocused) {
                            runCatching { heartFr.requestFocus() }; true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (playbackFocused) { player.seekBack(); true }
                        else false // let focus move between controls
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (playbackFocused) { player.seekForward(); true }
                        else false
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        if (player.isPlaying) player.pause() else player.play()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> { player.seekForward(); true }
                    KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> { player.seekBack(); true }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { PlayerView(it).apply {
                this.player = player
                isFocusable = false // keys always flow through the Compose handlers above
            } },
            modifier = Modifier.fillMaxSize()
        )
        // Invisible focus holder for "playback mode": while focused, D-pad seeks/plays.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(playbackFr)
                .onFocusChanged { playbackFocused = it.isFocused }
                .focusable()
        )
        // All controls in one row below the seek bar (bottom of the player)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { toggleFavorite() },
                modifier = Modifier
                    .focusRequester(heartFr)
                    .onFocusChanged { heartFocused = it.isFocused }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fa_heart),
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF4081) else Color.White
                )
            }
            IconButton(
                onClick = { likeVideo() },
                modifier = Modifier
                    .focusRequester(thumbFr)
                    .onFocusChanged { thumbFocused = it.isFocused }
            ) {
                Icon(
                    imageVector = Icons.Filled.ThumbUp,
                    contentDescription = "Like",
                    tint = if (thumbFocused) Color(0xFF2196F3) else Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_fa_eye_regular),
                    contentDescription = "Views",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(" $viewCount", color = Color.White)
            }
            IconButton(
                onClick = { showGearMenu = true },
                modifier = Modifier
                    .focusRequester(gearFr)
                    .onFocusChanged { gearFocused = it.isFocused }
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Playback settings",
                    tint = if (gearFocused) Color(0xFF2196F3) else Color.White
                )
            }
        }
    }

    if (showGearMenu) {
        val resolutions = listOf("480p", "720p", "1080p")
        val options = listOf("Direct (original)") + resolutions
        val selectedIdx = if (!useTranscode) 0 else 1 + resolutions.indexOf(transcodeResolution)
        val optionFrs = remember { List(options.size) { FocusRequester() } }
        // Put remote focus on the currently active quality when the menu opens
        LaunchedEffect(showGearMenu, selectedIdx) {
            repeat(20) {
                if (runCatching { optionFrs[selectedIdx].requestFocus() }.isSuccess) return@LaunchedEffect
                delay(100)
            }
        }
        AlertDialog(
            onDismissRequest = { showGearMenu = false },
            title = { Text("Playback quality") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { i, label ->
                        val selected = if (i == 0) !useTranscode
                            else useTranscode && transcodeResolution == resolutions[i - 1]
                        Button(
                            onClick = {
                                scope.launch {
                                    if (i == 0) settings.setUseTranscode(false)
                                    else {
                                        settings.setUseTranscode(true)
                                        settings.setTranscodeResolution(resolutions[i - 1])
                                    }
                                }
                                showGearMenu = false
                            },
                            border = if (selected) BorderStroke(2.dp, Color(0xFF2196F3)) else null,
                            modifier = Modifier.focusRequester(optionFrs[i])
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGearMenu = false }) { Text("Close") }
            }
        )
    }
}

package com.binarygeek119.covelounge.ui.media

import android.net.Uri
import android.util.Log
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.KeyEvent
import com.binarygeek119.covelounge.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
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
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
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
import coil.imageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val ratingsOnExit by settings.ratingsOnExit.collectAsState(initial = false)


    // Use transcode endpoint with ?start= parameter for reliable seeking
    var startSeconds by remember { mutableStateOf(0.0) }
    val startParam = if (startSeconds > 0) "&start=$startSeconds" else ""
    val url = when (type) {
        "video" -> "$baseUrl/api/stream/video/$id/transcode?resolution=$transcodeResolution&access_token=$token$startParam"
        "preview" -> "$baseUrl/api/stream/video/$id/preview?access_token=$token"
        else -> "$baseUrl/api/audios/$id/stream?access_token=$token"
    }

    var showGearMenu by remember { mutableStateOf(false) }
    var showRatings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val gearFr = remember { FocusRequester() }
    var gearFocused by remember { mutableStateOf(false) }
    val heartFr = remember { FocusRequester() }
    var heartFocused by remember { mutableStateOf(false) }
    val playbackFr = remember { FocusRequester() }
    var playbackFocused by remember { mutableStateOf(false) }
    val playPauseFr = remember { FocusRequester() }
    var playPauseFocused by remember { mutableStateOf(false) }
    val rewindFr = remember { FocusRequester() }
    var rewindFocused by remember { mutableStateOf(false) }
    val forwardFr = remember { FocusRequester() }
    var forwardFocused by remember { mutableStateOf(false) }
    val sliderFr = remember { FocusRequester() }
    var sliderFocused by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val thumbFr = remember { FocusRequester() }
    var thumbFocused by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(0) }
    var viewCount by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf<String?>(null) }
    var performers by remember { mutableStateOf<List<com.binarygeek119.covelounge.data.network.PerformerSummaryDto>>(emptyList()) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var spriteBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sliderWidth by remember { mutableStateOf(0) }

    // Start with the gear focused; the AndroidView (PlayerView) steals View focus right
    // after attaching, so retry until focus sticks durably.
    LaunchedEffect(Unit) {
        repeat(40) {
            val r = runCatching { playbackFr.requestFocus() }
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

    BackHandler {
        when {
            showRatings -> showRatings = false
            showGearMenu -> showGearMenu = false
            ratingsOnExit -> showRatings = true
            else -> onBack()
        }
    }

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    // Track playback position so switching quality resumes where you were
    var savedPosition by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val st = player.playbackState
            if (st == Player.STATE_READY || st == Player.STATE_BUFFERING) {
                savedPosition = player.currentPosition
                if (!isScrubbing && player.playbackState == Player.STATE_READY) position = (startSeconds * 1000).toLong() + player.currentPosition
                val d = player.duration
                if (d > 0) duration = d
            }
                        if (player.playbackState == Player.STATE_ENDED && type != "audio" && !showRatings) {
                showRatings = true
                player.pause()
            }
        }
    }
    LaunchedEffect(url) {
        Log.d("SeekFix", "Re-preparing player: url=$url, startSeconds=$startSeconds")
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.playWhenReady = true
        // Reset savedPosition when using start-based seek
        if (startSeconds > 0) savedPosition = 0L
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
                            playbackFocused -> { if (player.isPlaying) player.pause() else player.play(); true }
                            sliderFocused -> { if (player.isPlaying) player.pause() else player.play(); true }
                            playPauseFocused -> { if (player.isPlaying) player.pause() else player.play(); true }
                            gearFocused -> { showGearMenu = true; true }
                            heartFocused -> { toggleFavorite(); true }
                            thumbFocused -> { likeVideo(); true }
                            else -> { if (player.isPlaying) player.pause() else player.play(); true }
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (sliderFocused || playPauseFocused || rewindFocused || forwardFocused || heartFocused || thumbFocused || gearFocused) {
                            runCatching { playbackFr.requestFocus() }; true
                        } else {
                            showGearMenu = true; true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (playbackFocused) {
                            runCatching { sliderFr.requestFocus() }
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (playbackFocused || sliderFocused) {
                            val newPos = (position - 15000).coerceAtLeast(0L)
                            Log.d("SeekFix", "LEFT seek to $newPos ms")
                            startSeconds = newPos / 1000.0
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (playbackFocused || sliderFocused) {
                            val newPos = (position + 15000).coerceAtMost(duration)
                            Log.d("SeekFix", "RIGHT seek to $newPos ms")
                            startSeconds = newPos / 1000.0
                            true
                        } else false
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
                useController = false
                isFocusable = false // keys always flow through the Compose handlers above
            } },
            modifier = Modifier.fillMaxSize()
        )
        // Load sprite sheet for scrubbing preview (videos only)
    LaunchedEffect(baseUrl, token, id, type) {
        if (type == "audio") return@LaunchedEffect
        val url = "$baseUrl/api/stream/video/$id/sprite?access_token=$token"
        try {
            val req = ImageRequest.Builder(context).data(url).build()
            val result = context.imageLoader.execute(req)
            val d = result.drawable
            if (d is BitmapDrawable) spriteBitmap = d.bitmap
        } catch (_: Exception) {}
    }

    // Fetch title for display
    LaunchedEffect(api, type, id) {
        try {
            when (type) {
                "audio" -> {
                    val dto = api.audioDetail(id)
                    title = dto.title
                }
                else -> {
                    val dto = api.videoDetail(id)
                    title = dto.title
                    performers = dto.performers ?: emptyList()
                    val d = dto.files?.firstOrNull()?.duration ?: 0.0
                    if (d > 0) duration = (d * 1000).toLong()
                }
            }
        } catch (_: Exception) {}
    }

    // Title at top-left
    title?.let {
        Text(
            text = it,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
        )
    }

    // Invisible focus holder for "playback mode": while focused, D-pad seeks/plays.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(playbackFr)
                .onFocusChanged { playbackFocused = it.isFocused }
                .focusable()
        )
        // Custom control panel: seek bar on top, buttons below
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Performer images: lower-left, above seek bar, remote-navigable
            if (performers.isNotEmpty()) {
                var focusedPerformer by remember { mutableStateOf(-1) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    performers.forEachIndexed { idx, perf ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .onFocusChanged { if (it.isFocused) focusedPerformer = idx else if (focusedPerformer == idx) focusedPerformer = -1 }
                                .background(if (focusedPerformer == idx) Color(0xFF2196F3) else Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape)
                                .clickable { /* navigate to performer page (later) */ }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = "$baseUrl/api/performers/${perf.id}/image?max=128&access_token=$token",
                                contentDescription = perf.name,
                                modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().onGloballyPositioned { sliderWidth = it.size.width }) {
                if (isScrubbing && previewBitmap != null) {
                    val frac = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                    val density = LocalDensity.current
                    val offsetX = with(density) { (frac * sliderWidth - 40.dp.toPx()).toDp() }
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "preview",
                        modifier = Modifier
                            .size(80.dp)
                            .offset(x = offsetX)
                            .align(Alignment.TopStart)
                    )
                }
                Slider(
                    value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { v ->
                        isScrubbing = true
                        position = (v * duration).toLong()
                        val sb = spriteBitmap
                        if (sb != null) {
                            val frameCount = minOf(81, maxOf(1, (duration / 1000 / 2).toInt())).coerceAtLeast(1)
                            val cols = kotlin.math.ceil(kotlin.math.sqrt(frameCount.toDouble())).toInt()
                            val rows = kotlin.math.ceil(frameCount.toDouble() / cols).toInt()
                            val intervalMs = (duration / frameCount).coerceAtLeast(1L)
                            val idx = minOf(frameCount - 1, (position / intervalMs).toInt().coerceAtLeast(0))
                            val tileW = sb.width / cols
                            val tileH = sb.height / rows
                            val col = idx % cols
                            val row = idx / cols
                            if (tileW > 0 && tileH > 0) previewBitmap = Bitmap.createBitmap(sb, col * tileW, row * tileH, tileW, tileH)
                        }
                    },
                    onValueChangeFinished = {
                        isScrubbing = false
                        previewBitmap = null
                        if (duration > 0) {
                            val targetSeconds = position / 1000.0
                            Log.d("SeekFix", "Slider seek to $position ms ($targetSeconds s)")
                            startSeconds = targetSeconds
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(sliderFr).onFocusChanged { sliderFocused = it.isFocused }.padding(top = if (isScrubbing && previewBitmap != null) 88.dp else 0.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { if (player.isPlaying) player.pause() else player.play() },
                    modifier = Modifier.focusRequester(playPauseFr).onFocusChanged { playPauseFocused = it.isFocused }
                ) {
                    Icon(
                        imageVector = if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (playPauseFocused) Color(0xFF2196F3) else Color.White
                    )
                }
                IconButton(
                    onClick = { player.seekBack() },
                    modifier = Modifier.focusRequester(rewindFr).onFocusChanged { rewindFocused = it.isFocused }
                ) {
                    Icon(imageVector = Icons.Filled.FastRewind, contentDescription = "Rewind", tint = if (rewindFocused) Color(0xFF2196F3) else Color.White)
                }
                IconButton(
                    onClick = { player.seekForward() },
                    modifier = Modifier.focusRequester(forwardFr).onFocusChanged { forwardFocused = it.isFocused }
                ) {
                    Icon(imageVector = Icons.Filled.FastForward, contentDescription = "Forward", tint = if (forwardFocused) Color(0xFF2196F3) else Color.White)
                }
                IconButton(
                    onClick = { toggleFavorite() },
                    modifier = Modifier
                        .focusRequester(heartFr)
                        .onFocusChanged { heartFocused = it.isFocused }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fa_heart),
                        contentDescription = "Favorite",
                        tint = if (heartFocused) Color(0xFF2196F3) else if (isFavorite) Color(0xFFFF4081) else Color.White
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fa_eye_regular),
                        contentDescription = "Views",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(" $viewCount", color = Color.White)
                }
            }
        }
    }

    if (showRatings) {
        com.binarygeek119.covelounge.ui.ratings.RatingsScreen(
            baseUrl = baseUrl,
            token = token,
            type = type,
            id = id,
            onExit = { showRatings = false; onBack() }
        )
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

package com.binarygeek119.covelounge.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.view.KeyEvent
import android.view.TextureView
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.imageLoader
import com.binarygeek119.covelounge.data.MediaCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverFlow(
    items: List<MediaCard>,
    onOpen: (MediaCard) -> Unit,
    onFocus: () -> Unit = {},
    onUp: () -> Unit = {},
    onDown: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    previewMode: String = "Off",
    previewUrlProvider: (MediaCard) -> String? = { null },
    fullVideoUrlProvider: (MediaCard) -> String? = { null },
    intervalSeconds: Int = 10,
    volumeLevel: Int = 3,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.height(220.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No screenshots")
        }
        return
    }
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()
    var focusedPosition by remember { mutableStateOf(1) }
    var initialJumpDone by remember { mutableStateOf(false) }
    val leftArrowFr = remember { FocusRequester() }
    val rightArrowFr = remember { FocusRequester() }
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(pagerState.currentPage) {
        val nextIdx = (pagerState.currentPage + 1) % items.size
        val nextItem = items.getOrNull(nextIdx) ?: return@LaunchedEffect
        try {
            val req = coil.request.ImageRequest.Builder(context).data(nextItem.posterUrl).build()
            context.imageLoader.enqueue(req)
        } catch (_: Exception) {}
    }
    LaunchedEffect(items, intervalSeconds) {
        while (true) {
            delay(intervalSeconds * 1000L)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }
    LaunchedEffect(pagerState.currentPage, previewMode) {
        initialJumpDone = false
        if (previewMode == "Off" || items.isEmpty()) {
            player.stop()
            return@LaunchedEffect
        }
        val item = items[pagerState.currentPage]
        val url = if (previewMode == "Video") (fullVideoUrlProvider(item) ?: previewUrlProvider(item)) else previewUrlProvider(item)
        if (url == null) { player.stop(); return@LaunchedEffect }
        player.setMediaItem(MediaItem.fromUri(url))
        player.volume = when (volumeLevel) {
            0 -> 0f
            1 -> 0.2f
            2 -> 0.5f
            else -> 1f
        }
        if (previewMode == "Video") {
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        val durSec = (player.duration / 1000).toInt()
                        val startSec = if (durSec <= 0) 20 else when {
                            durSec < 120 -> 0
                            durSec > 3600 -> 1200
                            durSec > 1800 -> 300
                            durSec > 900 -> 180
                            else -> 20
                        }
                        if (startSec > 0) player.seekTo(startSec * 1000L)
                        player.removeListener(this)
                    }
                }
            })
        }
        player.prepare()
        player.playWhenReady = true
    }
    Box(modifier = modifier.height(260.dp).fillMaxWidth().onPreviewKeyEvent { event ->
        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
            when (event.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> { onUp(); true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { onDown(); true }
                // Positions: 0 = left arrow, 1 = center item, 2 = right arrow
                KeyEvent.KEYCODE_DPAD_LEFT -> when (focusedPosition) {
                    1 -> { runCatching { leftArrowFr.requestFocus() }; true }
                    2 -> { runCatching { focusRequester.requestFocus() }; true }
                    else -> false
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> when (focusedPosition) {
                    0 -> { runCatching { focusRequester.requestFocus() }; true }
                    1 -> { runCatching { rightArrowFr.requestFocus() }; true }
                    else -> false
                }
                else -> false
            }
        } else false
    }) {
        HorizontalPager(state = pagerState, userScrollEnabled = false, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            if ((previewMode == "Previews" || previewMode == "Video") && page == pagerState.currentPage) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    var showVideo by remember { mutableStateOf(false) }
                    LaunchedEffect(page, previewMode) { delay(1200); showVideo = true }
                    val videoAlpha by animateFloatAsState(targetValue = if (showVideo) 1f else 0f, animationSpec = tween(1200), label = "vidAlpha")
                    AndroidView(
                        factory = { android.view.TextureView(it).apply { player.setVideoTextureView(this) } },
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(videoAlpha * (if (focusedPosition == 1) 1f else 0f))
                            .then(if (page == pagerState.currentPage) Modifier.focusRequester(focusRequester) else Modifier)
                            .onFocusChanged { if (it.isFocused) { focusedPosition = 1; onFocus() } }
                            .clickable { onOpen(item) }
                    )
                }
            } else {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (page == pagerState.currentPage) Modifier.focusRequester(focusRequester) else Modifier)
                        .alpha(if (focusedPosition == 1) 1f else 0f)
                        .onFocusChanged { if (it.isFocused) { focusedPosition = 1; onFocus() } }
                        .clickable { if (page == pagerState.currentPage) onOpen(item) },
                    contentScale = ContentScale.Crop
                )
            }
        }
        // bottom blur/transition
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .focusProperties { canFocus = false }
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
        )
        // left arrow
        IconButton(
            onClick = {
                val prev = if (pagerState.currentPage > 0) pagerState.currentPage - 1 else items.size - 1
                scope.launch { pagerState.animateScrollToPage(prev) }
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .focusRequester(leftArrowFr)
                .onFocusChanged { if (it.isFocused) focusedPosition = 0 }
                .background(if (focusedPosition == 0) Color.White else Color.Transparent, shape = RoundedCornerShape(8.dp))
        ) { Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous", tint = if (focusedPosition == 0) Color.Black else Color(0xFF2196F3)) }
        // right arrow
        IconButton(
            onClick = {
                val next = (pagerState.currentPage + 1) % items.size
                scope.launch { pagerState.animateScrollToPage(next) }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .focusRequester(rightArrowFr)
                .onFocusChanged { if (it.isFocused) focusedPosition = 2 }
                .background(if (focusedPosition == 2) Color.White else Color.Transparent, shape = RoundedCornerShape(8.dp))
        ) { Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next", tint = if (focusedPosition == 2) Color.Black else Color(0xFF2196F3)) }

        // metadata overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 40.dp)
        ) {
            Text(items[pagerState.currentPage].title ?: "", color = Color.White, fontWeight = FontWeight.Bold)
            val performers = items[pagerState.currentPage].performers
            if (performers.isNotEmpty()) {
                Text(performers.joinToString(", "), color = Color.White, fontSize = 12.sp)
            }
        }

        // dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .focusProperties { canFocus = false }
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == pagerState.currentPage) 10.dp else 6.dp)
                        .background(
                            if (i == pagerState.currentPage) Color(0xFFFFD700) else Color(0xFFADD8E6),
                            CircleShape
                        )
                )
            }
        }
    }
}

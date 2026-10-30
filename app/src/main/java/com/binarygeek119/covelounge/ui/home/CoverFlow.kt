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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.view.KeyEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.data.MediaCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverFlow(
    items: List<MediaCard>,
    onOpen: (MediaCard) -> Unit,
    onFocus: () -> Unit = {},
    previewMode: String = "Off",
    previewUrlProvider: (MediaCard) -> String? = { null },
    intervalSeconds: Int = 10,
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
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(items) {
        while (true) {
            delay(intervalSeconds * 1000L)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }
    LaunchedEffect(pagerState.currentPage, previewMode) {
        if (previewMode == "Off" || items.isEmpty()) {
            player.stop()
            return@LaunchedEffect
        }
        val url = previewUrlProvider(items[pagerState.currentPage]) ?: return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(url))
        player.volume = when (previewMode) {
            "Muted" -> 0f
            "Low" -> 0.3f
            "Full" -> 1f
            else -> 0f
        }
        player.prepare()
        player.playWhenReady = true
    }
    Box(modifier = modifier.height(260.dp).fillMaxWidth().onPreviewKeyEvent { event ->
        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) true else false
    }) {
        HorizontalPager(state = pagerState, userScrollEnabled = false, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            if (previewMode != "Off" && page == pagerState.currentPage) {
                AndroidView(
                    factory = { PlayerView(it).apply { this.player = player; useController = false } },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (focusedPosition == 1) 1f else 0f)
                        .onFocusChanged { if (it.isFocused) { focusedPosition = 1; onFocus() } }
                        .clickable { onOpen(item) }
                )
            } else {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
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

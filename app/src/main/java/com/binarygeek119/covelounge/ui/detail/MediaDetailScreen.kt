package com.binarygeek119.covelounge.ui.detail

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.R
import com.binarygeek119.covelounge.data.network.PerformerSummaryDto

@Composable
fun MediaDetailScreen(
    viewModel: MediaDetailViewModel,
    baseUrl: String,
    token: String,
    onBack: () -> Unit,
    onPlay: (startPositionMs: Long) -> Unit,
    onPreview: () -> Unit = {}
) {
    val title by viewModel.title.collectAsState()
    val details by viewModel.details.collectAsState()
    val posterUrl by viewModel.posterUrl.collectAsState()
    val likeCount by viewModel.likeCount.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val rating by viewModel.rating.collectAsState()
    val performers by viewModel.performers.collectAsState()
    val resumeTime by viewModel.resumeTime.collectAsState()
    val playFr = remember { FocusRequester() }
    var playFocused by remember { mutableStateOf(false) }
    val circlePlayFr = remember { FocusRequester() }
    var circlePlayFocused by remember { mutableStateOf(false) }
    val previewFr = remember { FocusRequester() }
    var previewFocused by remember { mutableStateOf(false) }
    val likeFr = remember { FocusRequester() }
    var likeFocused by remember { mutableStateOf(false) }
    val heartFr = remember { FocusRequester() }
    var heartFocused by remember { mutableStateOf(false) }
    val performersFr = remember { FocusRequester() }
    var performersFocused by remember { mutableStateOf(false) }
    var performerOffset by remember { mutableStateOf(0) }
    // Start remote focus on the play control so the media item is selectable right away
    LaunchedEffect(Unit) {
        repeat(10) {
            if (runCatching { playFr.requestFocus() }.isSuccess) return@LaunchedEffect
            kotlinx.coroutines.delay(100)
        }
    }
    BackHandler { onBack() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Back", color = Color.White, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxSize()) {
            posterUrl?.let {
                AsyncImage(model = it, contentDescription = title, modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                // Button row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Continue watching (circle-play) - only if partly viewed
                    if (resumeTime > 0) {
                        Box(
                            modifier = Modifier
                                .focusRequester(circlePlayFr)
                                .onFocusChanged { circlePlayFocused = it.isFocused }
                                .onPreviewKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { viewModel.recordPlay(); onPlay((resumeTime * 1000).toLong()); true }
                                            else -> false
                                        }
                                    } else false
                                }
                                .background(if (circlePlayFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), CircleShape)
                                .clickable { viewModel.recordPlay(); onPlay((resumeTime * 1000).toLong()) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_fa_circle_play),
                                contentDescription = "Continue watching",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    // Play
                    Box(
                        modifier = Modifier
                            .focusRequester(playFr)
                            .onFocusChanged { playFocused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { viewModel.recordPlay(); onPlay(0L); true }
                                        else -> false
                                    }
                                } else false
                            }
                            .background(if (playFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), CircleShape)
                            .clickable { viewModel.recordPlay(); onPlay(0L) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fa_play),
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    // Preview
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .focusRequester(previewFr)
                            .onFocusChanged { previewFocused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { onPreview(); true }
                                        else -> false
                                    }
                                } else false
                            }
                            .background(if (previewFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), CircleShape)
                            .clickable { onPreview() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = "Preview",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    // Like
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .focusRequester(likeFr)
                            .onFocusChanged { likeFocused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { viewModel.like(); true }
                                        else -> false
                                    }
                                } else false
                            }
                            .background(if (likeFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), CircleShape)
                            .clickable { viewModel.like() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ThumbUp,
                            contentDescription = "Like",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(" $likeCount", color = Color.White, modifier = Modifier.padding(start = 4.dp))
                    // Favorite
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .focusRequester(heartFr)
                            .onFocusChanged { heartFocused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { viewModel.toggleFavorite(); true }
                                        else -> false
                                    }
                                } else false
                            }
                            .background(if (heartFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), CircleShape)
                            .clickable { viewModel.toggleFavorite() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = if (heartFocused) Color(0xFF2196F3) else if (isFavorite) Color(0xFFFF4081) else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                // Rating row (visual only)
                rating?.let { r ->
                    val normalized = if (r <= 5) r * 20 else r
                    val stars = normalized / 20.0
                    Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (i in 0 until 5) {
                            val threshold = i + 1
                            val icon = when {
                                stars >= threshold -> Icons.Filled.Star
                                stars >= threshold - 0.5 -> Icons.Filled.StarHalf
                                else -> Icons.Filled.StarBorder
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Performers row (2 visible, L/R scrolls)
                if (performers.isNotEmpty()) {
                    val visibleCount = 2
                    val maxOffset = (performers.size - visibleCount).coerceAtLeast(0)
                    val shown = performers.drop(performerOffset).take(visibleCount)
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .focusRequester(performersFr)
                            .onFocusChanged { performersFocused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (performerOffset > 0) performerOffset -= 1
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            if (performerOffset < maxOffset) performerOffset += 1
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .background(if (performersFocused) Color(0x332196F3) else Color.Transparent, RoundedCornerShape(8.dp))
                            .focusable()
                            .padding(4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            shown.forEach { perf ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = "$baseUrl/api/performers/${perf.id}/image?max=128&access_token=$token",
                                        contentDescription = perf.name,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.TopCenter
                                    )
                                    Text(perf.name ?: "", color = Color.White, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                // Metadata below performers
                Text(title ?: "Untitled", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                details?.let { Text(it, color = Color.LightGray, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

package com.binarygeek119.covelounge.ui.detail

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
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
import coil.compose.AsyncImage

@Composable
fun MediaDetailScreen(
    viewModel: MediaDetailViewModel,
    baseUrl: String,
    token: String,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPreview: () -> Unit = {}
) {
    val title by viewModel.title.collectAsState()
    val details by viewModel.details.collectAsState()
    val posterUrl by viewModel.posterUrl.collectAsState()
    val likeCount by viewModel.likeCount.collectAsState()
    val playFr = remember { FocusRequester() }
    var playFocused by remember { mutableStateOf(false) }
    val previewFr = remember { FocusRequester() }
    var previewFocused by remember { mutableStateOf(false) }
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
                // Play control: icon only, selectable with the remote (OK opens playback)
                Box(
                    modifier = Modifier
                        .focusRequester(playFr)
                        .onFocusChanged { playFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                        viewModel.recordPlay(); onPlay(); true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .background(if (playFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), CircleShape)
                        .clickable { viewModel.recordPlay(); onPlay() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(title ?: "Untitled", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                details?.let { Text(it, color = Color.LightGray, modifier = Modifier.padding(top = 8.dp)) }
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
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
                Button(onClick = { viewModel.like() }, modifier = Modifier.padding(top = 8.dp)) { Text("Like (${likeCount})") }
            }
        }
    }
}

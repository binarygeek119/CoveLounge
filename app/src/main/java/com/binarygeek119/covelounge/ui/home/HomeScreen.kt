package com.binarygeek119.covelounge.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border

import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import androidx.compose.material3.AlertDialog
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.binarygeek119.covelounge.R
import com.binarygeek119.covelounge.data.MediaCard
import com.binarygeek119.covelounge.data.MediaKind
import com.binarygeek119.covelounge.data.SettingsStore

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    baseUrl: String,
    token: String,
    onOpenItem: (MediaKind, Int) -> Unit,
    onOpenFolder: (MediaKind) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val screenshots by viewModel.screenshots.collectAsState()
    val recentVideos by viewModel.recentVideos.collectAsState()
    val recentAudios by viewModel.recentAudios.collectAsState()
    val recentImages by viewModel.recentImages.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val previewMode by settingsStore.previewMode.collectAsState(initial = "Off")

    var showSettings by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "CoveLounge",
                    modifier = Modifier.height(40.dp),
                    contentScale = ContentScale.Fit
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Search",
                        color = Color.White,
                        modifier = Modifier.clickable { onSearchClick() }
                    )
                    Text(
                        text = "Settings",
                        color = Color.White,
                        modifier = Modifier.clickable { onSettingsClick() }
                    )
                }
            }
        }

        if (isLoading) {
            Text("Loading...", color = Color.Gray, modifier = Modifier.padding(16.dp))
        }
        error?.let { Text("Error: $it", color = Color.Red, modifier = Modifier.padding(16.dp)) }
        if (error != null) {
            androidx.compose.material3.Button(onClick = { viewModel.reload() }) {
                Text("Retry")
            }
        }

        // Spotlight bar
        if (screenshots.isEmpty()) {
            Text("No screenshots available", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else {
            CoverFlow(
                items = screenshots,
                onOpen = { card -> onOpenItem(card.kind, card.id) },
                previewMode = previewMode,
                previewUrlProvider = { card ->
                    if (card.kind == MediaKind.VIDEO) "$baseUrl/api/stream/video/${card.id}/preview?access_token=$token" else null
                }
            )
        }

        if (continueWatching.isNotEmpty()) {
            MediaRow(
                title = "Continue Watching",
                items = continueWatching,
                onFolderClick = {},
                onItemClick = { card -> onOpenItem(MediaKind.VIDEO, card.id) }
            )
        }

        // Vertical selector area
        val verticalPagerState = rememberPagerState(pageCount = { 3 })
        val scope = rememberCoroutineScope()
        val rows = listOf(
            Triple("Videos", recentVideos, MediaKind.VIDEO),
            Triple("Audios", recentAudios, MediaKind.AUDIO),
            Triple("Images", recentImages, MediaKind.IMAGE)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .onPreviewKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                if (verticalPagerState.currentPage > 0) {
                                    scope.launch { verticalPagerState.animateScrollToPage(verticalPagerState.currentPage - 1) }
                                }
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (verticalPagerState.currentPage < 2) {
                                    scope.launch { verticalPagerState.animateScrollToPage(verticalPagerState.currentPage + 1) }
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            VerticalPager(
                state = verticalPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val (title, items, kind) = rows[page]
                MediaRow(
                    title = title,
                    items = items,
                    onFolderClick = {
                        when (kind) {
                            MediaKind.VIDEO -> onOpenFolder(MediaKind.VIDEO)
                            MediaKind.AUDIO -> onOpenFolder(MediaKind.AUDIO)
                            MediaKind.IMAGE -> onOpenFolder(MediaKind.IMAGE)
                        }
                    },
                    onItemClick = { card -> onOpenItem(kind, card.id) }
                )
            }
            // Fixed selector overlay
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(150.dp, 100.dp)
                    .border(4.dp, Color.White, RoundedCornerShape(12.dp))
            )
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = { Text("Preview mode and screenshot filters will be configurable here.") },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Close") }
            }
        )
    }
}

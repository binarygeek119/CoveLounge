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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
    val recentGalleries by viewModel.recentGalleries.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val previewMode by settingsStore.previewMode.collectAsState(initial = "Off")
    val screenshotInterval by settingsStore.screenshotInterval.collectAsState(initial = 10)

    var showSettings by remember { mutableStateOf(false) }
    val searchFr = remember { FocusRequester() }
    val settingsFr = remember { FocusRequester() }
    val coverFr = remember { FocusRequester() }

    // Initial focus: spotlight center (or Search if no screenshots), so the remote works right away
    LaunchedEffect(screenshots) {
        runCatching {
            if (screenshots.isNotEmpty()) coverFr.requestFocus() else searchFr.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            Text("Loading...", color = Color.Gray, modifier = Modifier.padding(16.dp))
        }
        error?.let { Text("Error: $it", color = Color.Red, modifier = Modifier.padding(16.dp)) }
        if (error != null) {
            androidx.compose.material3.Button(onClick = { viewModel.reload() }) {
                Text("Retry")
            }
        }

        // Vertical selector area
        val verticalPagerState = rememberPagerState(pageCount = { if (continueWatching.isNotEmpty()) 5 else 4 })
        val scope = rememberCoroutineScope()
        val rows = buildList {
            if (continueWatching.isNotEmpty()) add(Triple("Continue Watching", continueWatching, MediaKind.VIDEO))
            add(Triple("Videos", recentVideos, MediaKind.VIDEO))
            add(Triple("Audios", recentAudios, MediaKind.AUDIO))
            add(Triple("Images", recentImages, MediaKind.IMAGE))
            add(Triple("Galleries", recentGalleries, MediaKind.GALLERY))
        }
        val focusRequesters = remember { List(rows.size) { FocusRequester() } }

        // Spotlight bar (fills top, with logo/settings overlay)
        Box(modifier = Modifier.fillMaxWidth()) {
            if (screenshots.isEmpty()) {
                Text("No screenshots available", color = Color.Gray, modifier = Modifier.padding(16.dp))
            } else {
                CoverFlow(
                    items = screenshots,
                    onOpen = { card -> onOpenItem(card.kind, card.id) },
                    onUp = { runCatching { searchFr.requestFocus() } },
                    onDown = {
                        val p = verticalPagerState.currentPage
                        focusRequesters.getOrNull(p)?.let { fr -> runCatching { fr.requestFocus() } }
                    },
                    focusRequester = coverFr,
                    previewMode = previewMode,
                    previewUrlProvider = { card ->
                        if (card.kind == MediaKind.VIDEO) "$baseUrl/api/stream/video/${card.id}/preview?access_token=$token" else null
                    },
                    intervalSeconds = screenshotInterval
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    TopButton(
                        text = "Search",
                        modifier = Modifier
                            .focusRequester(searchFr)
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> { settingsFr.requestFocus(); true }
                                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                        android.view.KeyEvent.KEYCODE_ENTER -> { onSearchClick(); true }
                                        // Swallow direction keys so focus can't escape this area; only DOWN returns to the spotlight
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_UP -> true
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> { runCatching { coverFr.requestFocus() }; true }
                                        else -> false
                                    }
                                } else false
                            },
                        onClick = onSearchClick
                    )
                    TopButton(
                        text = "Settings",
                        modifier = Modifier
                            .focusRequester(settingsFr)
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> { searchFr.requestFocus(); true }
                                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                        android.view.KeyEvent.KEYCODE_ENTER -> { onSettingsClick(); true }
                                        // Swallow direction keys so focus can't escape this area; only DOWN returns to the spotlight
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT, android.view.KeyEvent.KEYCODE_DPAD_UP -> true
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> { runCatching { coverFr.requestFocus() }; true }
                                        else -> false
                                    }
                                } else false
                            },
                        onClick = onSettingsClick
                    )
                }
            }
        }

        // Vertical pager rows
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .onPreviewKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                if (verticalPagerState.currentPage > 0) {
                                    val target = verticalPagerState.currentPage - 1
                                    scope.launch {
                                        verticalPagerState.animateScrollToPage(target)
                                        focusRequesters.getOrElse(target) { FocusRequester() }.requestFocus()
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (verticalPagerState.currentPage < rows.size - 1) {
                                    val target = verticalPagerState.currentPage + 1
                                    scope.launch {
                                        verticalPagerState.animateScrollToPage(target)
                                        focusRequesters.getOrElse(target) { FocusRequester() }.requestFocus()
                                    }
                                    true
                                } else {
                                    false
                                }
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
                    focusRequester = focusRequesters.getOrElse(page) { FocusRequester() },
                    onFolderClick = {
                        when (kind) {
                            MediaKind.VIDEO -> onOpenFolder(MediaKind.VIDEO)
                            MediaKind.AUDIO -> onOpenFolder(MediaKind.AUDIO)
                            MediaKind.IMAGE -> onOpenFolder(MediaKind.IMAGE)
                            MediaKind.GALLERY -> onOpenFolder(MediaKind.GALLERY)
                        }
                    },
                    onItemClick = { card -> onOpenItem(kind, card.id) }
                )
            }
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

@Composable
private fun TopButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = text,
        color = if (focused) Color(0xFF2196F3) else Color.White,
        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(4.dp)
    )
}

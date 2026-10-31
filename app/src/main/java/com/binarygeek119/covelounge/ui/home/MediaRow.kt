package com.binarygeek119.covelounge.ui.home

import android.view.KeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.data.MediaCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaRow(
    title: String,
    rowIndex: Int = 0,
    items: List<MediaCard>,
    onFolderClick: () -> Unit,
    onItemClick: (MediaCard) -> Unit,
    onRowFocused: (Int) -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    val total = (items.size + 1).coerceAtLeast(1) // folder + items
    val pagerState = rememberPagerState(pageCount = { Int.MAX_VALUE })
    var sel by remember { mutableStateOf(0) }
    var lap by remember { mutableStateOf(0) }
    var rowFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    val move: (Int) -> Unit = { dir ->
        var s = sel + dir
        var l = lap
        if (s < 0) { s = total - 1; l -= 1 }
        if (s >= total) { s = 0; l += 1 }
        sel = s
        lap = l
        onRowFocused(rowIndex)
        scope.launch { pagerState.animateScrollToPage(l * total + s) }
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        Box(
            modifier = Modifier
                .height(110.dp)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    rowFocused = it.isFocused
                    if (it.isFocused) onRowFocused(rowIndex)
                }
                .onPreviewKeyEvent { event ->
                    if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) false
                    else when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> if (total > 1) { move(-1); true } else false
                        KeyEvent.KEYCODE_DPAD_RIGHT -> if (total > 1) { move(1); true } else false
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (sel == 0) onFolderClick() else onItemClick(items[sel - 1])
                            true
                        }
                        else -> false
                    }
                }
                .focusable()
        ) {
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = (screenWidthDp - 16 - 150).dp),
                pageSpacing = 8.dp,
                userScrollEnabled = false,
                modifier = Modifier.height(110.dp)
            ) { page ->
                val idx = ((page % total) + total) % total
                Box(modifier = Modifier.fillMaxSize()) {
                    if (idx == 0) {
                        FolderCard()
                    } else {
                        ItemCard(item = items[idx - 1])
                    }
                }
            }
            // Fixed selector overlay (visible only when this row has focus)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(160.dp, 110.dp)
                    .alpha(if (rowFocused) 1f else 0f)
                    .border(4.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp))
            )
        }
        // dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until total) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (i == sel) 10.dp else 6.dp)
                        .background(
                            if (i == sel) Color.White else Color(0xFFADD8E6),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun FolderCard() {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = com.binarygeek119.covelounge.R.drawable.logo),
            contentDescription = "Folder",
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun ItemCard(item: MediaCard) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            placeholder = ColorPainter(Color(0xFF2A2A3A)),
            error = ColorPainter(Color(0xFF3A1A1A)),
            modifier = Modifier
                .width(150.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = item.title ?: "Untitled",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

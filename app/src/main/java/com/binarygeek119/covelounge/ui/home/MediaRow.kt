package com.binarygeek119.covelounge.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.focusable
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.data.MediaCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaRow(
    title: String,
    rowIndex: Int = 0,
    items: List<MediaCard>,
    onFolderClick: () -> Unit,
    onItemClick: (MediaCard) -> Unit,
    onRowFocused: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val total = items.size + 1 // folder + items
    if (total <= 1 && items.isEmpty()) {
        // still show folder
    }
    val pagerState = rememberPagerState(pageCount = { if (total <= 0) 1 else Int.MAX_VALUE })
    var focusedIndex by remember { mutableStateOf(0) }
    val current = focusedIndex
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        Box(modifier = Modifier.height(110.dp).fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(150.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = (screenWidthDp - 16 - 150).dp),
                pageSpacing = 8.dp,
                modifier = Modifier.height(110.dp)
            ) { page ->
                val idx = if (total <= 0) 0 else page % total
                val isCurrent = idx == focusedIndex
                Box(
                    modifier = Modifier
                        .onFocusChanged { if (it.isFocused) { focusedIndex = idx; onRowFocused(rowIndex) } }
                        .focusable()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (idx == 0) onFolderClick() else onItemClick(items[idx - 1])
                        }
                ) {
                    if (idx == 0) {
                        FolderCard(onClick = {})
                    } else {
                        val item = items[idx - 1]
                        ItemCard(item = item, onClick = {})
                    }
                }
            }
            // Fixed selector overlay
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(160.dp, 110.dp)
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
            for (i in 0 until total.coerceAtLeast(1)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (i == current) 10.dp else 6.dp)
                        .background(
                            if (i == current) Color.White else Color(0xFFADD8E6),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun FolderCard(onClick: () -> Unit) {
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
private fun ItemCard(item: MediaCard, onClick: () -> Unit) {
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

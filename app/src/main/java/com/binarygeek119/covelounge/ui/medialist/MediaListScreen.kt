package com.binarygeek119.covelounge.ui.medialist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.data.MediaCard
import com.binarygeek119.covelounge.data.MediaKind
import androidx.compose.ui.graphics.painter.ColorPainter

@Composable
fun MediaListScreen(
    viewModel: MediaListViewModel,
    baseUrl: String,
    token: String,
    onBack: () -> Unit,
    onOpenItem: (MediaCard) -> Unit
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val direction by viewModel.direction.collectAsState()
    val query by viewModel.query.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()

    LaunchedEffect(Unit) { viewModel.init(baseUrl, token) }

    BackHandler { onBack() }

    val title = when (viewModel.kind) {
        MediaKind.VIDEO -> "Videos"
        MediaKind.AUDIO -> "Music"
        MediaKind.IMAGE -> "Photos"
        MediaKind.GALLERY -> "Galleries"
    }

    val firstItemFr = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        // Sort / filter bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("created_at" to "Recent", "title" to "Title", "rating" to "Rating").forEach { (key, label) ->
                TextButton(onClick = { viewModel.setSort(key) }) {
                    Text(label, color = if (sort == key) Color(0xFF2196F3) else Color.White, fontWeight = if (sort == key) FontWeight.Bold else FontWeight.Normal)
                }
            }
            TextButton(onClick = { viewModel.setDirection(if (direction == "desc") "asc" else "desc") }) {
                Text(if (direction == "desc") "Desc" else "Asc", color = Color.White)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                label = { Text("Search") },
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.search() }) { Text("Go", color = Color.White) }
        }

        if (items.isEmpty() && loading) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = Color(0xFF2196F3))
            }
        } else if (items.isEmpty()) {
            Text("No media found", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { index, item ->
                    FolderCardItem(
                        item = item,
                        modifier = if (index == 0) Modifier.focusRequester(firstItemFr) else Modifier,
                        onClick = { onOpenItem(item) }
                    )
                }
                if (hasMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                            if (loading) {
                                CircularProgressIndicator(color = Color(0xFF2196F3))
                            } else {
                                TextButton(onClick = { viewModel.loadMore() }) { Text("Load more", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }

    var focusedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(items.isNotEmpty()) {
        if (items.isNotEmpty() && !focusedOnce) {
            focusedOnce = true
            runCatching { firstItemFr.requestFocus() }
        }
    }
}

@Composable
private fun FolderCardItem(item: MediaCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                    (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                     event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                     event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                    onClick()
                    true
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .then(if (focused) Modifier.border(3.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp)) else Modifier)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            placeholder = ColorPainter(Color(0xFF2A2A3A)),
            error = ColorPainter(Color(0xFF3A1A1A)),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Text(
            text = item.title ?: "Untitled",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        val r = item.rating
        if (r != null) {
            val normalized = if (r <= 5) r * 20 else r
            val stars = normalized / 20.0
            Row(modifier = Modifier.padding(top = 2.dp)) {
                for (i in 0 until 5) {
                    val threshold = i + 1
                    val icon = when {
                        stars >= threshold -> Icons.Filled.Star
                        stars >= threshold - 0.5 -> Icons.Filled.StarHalf
                        else -> Icons.Filled.StarBorder
                    }
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

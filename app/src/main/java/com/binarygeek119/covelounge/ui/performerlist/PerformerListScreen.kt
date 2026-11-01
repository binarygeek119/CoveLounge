package com.binarygeek119.covelounge.ui.performerlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.painter.ColorPainter

@Composable
fun PerformerListScreen(
    viewModel: PerformerListViewModel,
    baseUrl: String,
    token: String,
    onBack: () -> Unit,
    onOpenPerformer: (Int, String?, String?) -> Unit = { _, _, _ -> }
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val query by viewModel.query.collectAsState()

    LaunchedEffect(Unit) { viewModel.init(baseUrl, token) }
    BackHandler { onBack() }

    var showSearch by remember { mutableStateOf(false) }
    val searchFr = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var sortAsc by remember { mutableStateOf(true) }
    var showNumbers by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    var scrollToIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex >= 0) {
            gridState.animateScrollToItem(scrollToIndex)
            scrollToIndex = -1
        }
    }

    val sorted = if (selectedLetter == null) items else items.filter { it.name?.startsWith(selectedLetter!!, ignoreCase = true) == true }

    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Performers", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            if (showSearch) {
                LaunchedEffect(Unit) { runCatching { searchFr.requestFocus() }; keyboardController?.show() }
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setQuery(it) },
                    label = { Text("Search performers") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search(); keyboardController?.hide() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFr).padding(bottom = 8.dp)
                )
            }

            if (sorted.isEmpty() && loading) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Color(0xFF2196F3))
                }
            } else if (sorted.isEmpty()) {
                Text("No performers found", color = Color.Gray, modifier = Modifier.padding(16.dp))
            } else {
                val firstFr = remember { FocusRequester() }
                var focusedOnce by remember { mutableStateOf(false) }
                LaunchedEffect(sorted.isNotEmpty()) {
                    if (items.isNotEmpty() && !focusedOnce) {
                        focusedOnce = true
                        runCatching { firstFr.requestFocus() }
                    }
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(170.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(sorted) { index, item ->
                        var focused by remember { mutableStateOf(false) }
                        Column(
                            modifier = (if (index == 0) Modifier.focusRequester(firstFr) else Modifier)
                                .fillMaxWidth()
                                .onFocusChanged { focused = it.isFocused }
                                .onPreviewKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                        (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                         event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                                         event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                                        onOpenPerformer(item.id, item.name, item.imageUrl)
                                        true
                                    } else false
                                }
                                .focusable()
                                .then(if (focused) Modifier.border(3.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp)) else Modifier)
                                .clickable { onOpenPerformer(item.id, item.name, item.imageUrl) }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.name,
                                placeholder = ColorPainter(Color(0xFF2A2A3A)),
                                error = ColorPainter(Color(0xFF3A1A1A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alignment = Alignment.TopCenter
                            )
                            Text(
                                text = item.name ?: "Performer",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                if (hasMore) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                        if (loading) {
                            CircularProgressIndicator(color = Color(0xFF2196F3))
                        } else {
                            androidx.compose.material3.TextButton(onClick = { viewModel.loadMore() }) { Text("Load more", color = Color.White) }
                        }
                    }
                }
            }
        }

        // Right ABC/filter sidebar
        Column(
            modifier = Modifier
                .width(60.dp)
                .fillMaxSize()
                .padding(start = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search icon
            var searchFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .onFocusChanged { searchFocused = it.isFocused }
                    .background(if (searchFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .clickable { showSearch = !showSearch }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
            }
            // Filter icon
            var filterFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .onFocusChanged { filterFocused = it.isFocused }
                    .background(if (filterFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .clickable { sortAsc = !sortAsc; viewModel.setDirection(if (sortAsc) "asc" else "desc") }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = Color.White)
            }
            // Toggle button between numbers and letters
            var toggleFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .onFocusChanged { toggleFocused = it.isFocused }
                    .background(if (toggleFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .clickable { showNumbers = !showNumbers }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Text(if (showNumbers) "ABC" else "0-9", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            // Sort direction toggle
            var sortFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .onFocusChanged { sortFocused = it.isFocused }
                    .background(if (sortFocused) Color(0xFF2196F3) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .clickable {
                        sortAsc = !sortAsc
                        viewModel.setDirection(if (sortAsc) "asc" else "desc")
                    }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.SwapVert, contentDescription = "Sort direction", tint = if (sortAsc) Color.White else Color(0xFF0D47A1))
            }

            val letters = if (showNumbers) "0123456789" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            letters.forEach { ch ->
                val isSelected = selectedLetter == ch.toString()
                var letterFocused by remember { mutableStateOf(false) }
                Text(
                    text = ch.toString(),
                    color = if (isSelected) Color(0xFF0D47A1) else if (letterFocused) Color(0xFF2196F3) else Color.White,
                    fontWeight = if (isSelected || letterFocused) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .onFocusChanged { letterFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                 event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                                 event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                                if (selectedLetter == ch.toString()) { selectedLetter = null } else { selectedLetter = ch.toString(); viewModel.loadAll() }
                                true
                            } else false
                        }
                        .focusable()
                        .clickable {
                            if (selectedLetter == ch.toString()) {
                                selectedLetter = null
                            } else {
                                selectedLetter = ch.toString()
                                viewModel.loadAll()
                            }
                        }
                        .padding(vertical = 2.dp)
                )
            }
            }
        }
    }
}

package com.binarygeek119.covelounge.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.data.MediaCard
import com.binarygeek119.covelounge.data.MediaKind

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    baseUrl: String,
    token: String,
    onBack: () -> Unit,
    onOpenItem: (MediaCard) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.results.collectAsState()
    var filter by remember { mutableStateOf("All") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Back", color = Color.White, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search") }, modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.search(query) }) { Text("Go") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            listOf("All", "Videos", "Audios", "Images").forEach { f ->
                TextButton(onClick = { filter = f }) { Text(f, color = if (filter == f) Color.White else Color.Gray) }
            }
        }
        val filtered = if (filter == "All") results else results.filter {
            when (filter) {
                "Videos" -> it.kind == MediaKind.VIDEO
                "Audios" -> it.kind == MediaKind.AUDIO
                "Images" -> it.kind == MediaKind.IMAGE
                else -> true
            }
        }
        if (filtered.isEmpty()) {
            Text("No results", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { item ->
                    Column(modifier = Modifier.clickable { onOpenItem(item) }) {
                        AsyncImage(model = item.posterUrl, contentDescription = item.title, modifier = Modifier.fillMaxWidth().height(120.dp))
                        Text(item.title ?: "Untitled", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
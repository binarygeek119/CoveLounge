package com.binarygeek119.covelounge.ui.medialist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.data.MediaCard

@Composable
fun MediaListScreen(
    viewModel: MediaListViewModel,
    baseUrl: String,
    token: String,
    onBack: () -> Unit,
    onOpenItem: (MediaCard) -> Unit
) {
    val items by viewModel.items.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Back", color = Color.White, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        if (items.isEmpty()) {
            Text("No media found", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    Column(modifier = Modifier.clickable { onOpenItem(item) }) {
                        AsyncImage(model = item.posterUrl, contentDescription = item.title, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)))
                        Text(item.title ?: "Untitled", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
package com.binarygeek119.covelounge.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MediaDetailScreen(
    viewModel: MediaDetailViewModel,
    baseUrl: String,
    token: String,
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    val title by viewModel.title.collectAsState()
    val details by viewModel.details.collectAsState()
    val posterUrl by viewModel.posterUrl.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Back", color = Color.White, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        posterUrl?.let {
            AsyncImage(model = it, contentDescription = title, modifier = Modifier.fillMaxWidth().height(240.dp), contentScale = ContentScale.Crop)
        }
        Text(title ?: "Untitled", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        details?.let { Text(it, color = Color.Gray, modifier = Modifier.padding(top = 8.dp)) }
        Button(onClick = onPlay, modifier = Modifier.padding(top = 16.dp)) { Text("Play") }
    }
}
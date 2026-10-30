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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.data.MediaKind

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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Back", color = Color.White, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        posterUrl?.let {
            AsyncImage(model = it, contentDescription = title, modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        }
        if (viewModel.kind == MediaKind.VIDEO) {
            Text("Sprite sheet:", color = Color.White, modifier = Modifier.padding(top = 8.dp))
            AsyncImage(model = "$baseUrl/api/stream/video/${viewModel.id}/sprite?access_token=$token", contentDescription = "Sprite", modifier = Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Crop)
        }
        Text(title ?: "Untitled", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        details?.let { Text(it, color = Color.Gray, modifier = Modifier.padding(top = 8.dp)) }
        Button(onClick = { viewModel.recordPlay(); onPlay() }, modifier = Modifier.padding(top = 16.dp)) { Text("Play") }
        Button(onClick = onPreview, modifier = Modifier.padding(top = 8.dp)) { Text("Preview") }
        Button(onClick = { viewModel.like() }, modifier = Modifier.padding(top = 8.dp)) { Text("Like (${likeCount})") }
    }
}
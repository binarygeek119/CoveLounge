package com.binarygeek119.covelounge.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    baseUrl: String,
    token: String,
    onPlayVideo: (Int) -> Unit,
    onPlayAudio: (Int) -> Unit,
    onViewImage: (Int) -> Unit
) {
    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val images by viewModel.images.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Videos")
        LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(videos) { v ->
                Column(Modifier.clickable { onPlayVideo(v.id) }) {
                    AsyncImage(model = "$baseUrl/api/videos/${v.id}/image?max=256&access_token=$token", contentDescription = v.title, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Crop)
                    Text(v.title ?: "Video ${v.id}")
                }
            }
        }
        Text("Audios")
        LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(audios) { a ->
                Column(Modifier.clickable { onPlayAudio(a.id) }) {
                    AsyncImage(model = "$baseUrl/api/audios/${a.id}/image?max=256&access_token=$token", contentDescription = a.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                    Text(a.title ?: "Audio ${a.id}")
                }
            }
        }
        Text("Images")
        LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(images) { i ->
                Column(Modifier.clickable { onViewImage(i.id) }) {
                    AsyncImage(model = "$baseUrl/api/stream/image/${i.id}?max=256&access_token=$token", contentDescription = i.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                    Text(i.title ?: "Image ${i.id}")
                }
            }
        }
    }
}

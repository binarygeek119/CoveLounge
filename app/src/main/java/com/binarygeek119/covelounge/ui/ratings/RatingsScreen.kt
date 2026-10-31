package com.binarygeek119.covelounge.ui.ratings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import kotlinx.coroutines.launch

@Composable
fun RatingsScreen(
    baseUrl: String,
    token: String,
    type: String,
    id: Int,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val session = remember { SessionRepository(ServerStore(app)) }
    val api = remember(baseUrl, token) { session.createApi(baseUrl, token) }
    val hostType = if (type == "audio") "Audio" else "Video"

    val ratingState = remember { mutableStateOf(0) }
    val isFavoriteState = remember { mutableStateOf(false) }
    val likeCountState = remember { mutableStateOf(0) }
    val titleState = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(api, hostType, id) {
        try {
            val eng = api.getEngagement(hostType, id)
            ratingState.value = eng.rating ?: 0
            isFavoriteState.value = eng.isFavorite
            likeCountState.value = eng.likeCount
        } catch (_: Exception) {}
        try {
            titleState.value = if (type == "audio") api.audioDetail(id).title else api.videoDetail(id).title
        } catch (_: Exception) {}
    }

    val rating = ratingState.value
    val isFavorite = isFavoriteState.value
    val likeCount = likeCountState.value
    val title = titleState.value

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title ?: "Rate this", color = Color.White)
        Text("Rating", color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until 5) {
                val starValue = (i + 1) * 20
                val filled = rating >= starValue
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "star ${i + 1}",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { ratingState.value = starValue }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ThumbUp, contentDescription = "Like", tint = Color.White)
            Text("Likes: $likeCount", color = Color.White)
            Button(onClick = {
                scope.launch {
                    try {
                        val c = if (type == "audio") api.likeAudio(id) else api.likeVideo(id)
                        likeCountState.value = c
                    } catch (_: Exception) {}
                }
            }) { Text("Like") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Favorite", color = Color.White)
            Button(onClick = { isFavoriteState.value = !isFavoriteState.value }) { Text(if (isFavorite) "On" else "Off") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    try {
                        api.setRating(hostType, id, com.binarygeek119.covelounge.data.network.VideoRatingDto(ratingState.value))
                        api.setFavorite(hostType, id, com.binarygeek119.covelounge.data.network.EntityFavoriteDto(isFavoriteState.value))
                    } catch (_: Exception) {}
                    onExit()
                }
            }) { Text("Confirm") }
            Button(onClick = onExit) { Text("Exit") }
        }
    }
}

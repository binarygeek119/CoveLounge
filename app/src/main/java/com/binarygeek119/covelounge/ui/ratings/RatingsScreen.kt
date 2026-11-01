package com.binarygeek119.covelounge.ui.ratings

import android.app.Application
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
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

    // Star focus state
    val starFocusRequesters = remember { List(5) { FocusRequester() } }
    var focusedStarIndex by remember { mutableStateOf(0) }

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
        // Focus the first star after data loads
        kotlinx.coroutines.delay(200)
        repeat(10) {
            if (runCatching { starFocusRequesters[0].requestFocus() }.isSuccess) return@repeat
            kotlinx.coroutines.delay(100)
        }
    }

    val rating = ratingState.value
    val isFavorite = isFavoriteState.value
    val likeCount = likeCountState.value
    val title = titleState.value

    // Star click cycle: empty → half → full → empty
    fun onStarClick(index: Int) {
        val halfValue = index * 20 + 10
        val fullValue = (index + 1) * 20
        ratingState.value = when {
            ratingState.value < halfValue -> halfValue  // empty → half
            ratingState.value >= halfValue && ratingState.value < fullValue -> fullValue  // half → full
            else -> index * 20  // full → empty
        }
    }

    // Star display icon
    fun starIcon(index: Int): androidx.compose.ui.graphics.vector.ImageVector {
        val halfThreshold = index * 20 + 10
        val fullThreshold = (index + 1) * 20
        return when {
            rating >= fullThreshold -> Icons.Filled.Star
            rating >= halfThreshold -> Icons.AutoMirrored.Filled.StarHalf
            else -> Icons.Filled.StarBorder
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title ?: "Rate this", color = Color.White)
        Text("Rating", color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until 5) {
                val starIndex = i
                val isFocused = (focusedStarIndex == i)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(starFocusRequesters[i])
                        .onFocusChanged { focused ->
                            if (focused.isFocused) focusedStarIndex = i
                        }
                        .background(if (isFocused) Color(0xFF2196F3) else Color.Transparent, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onStarClick(starIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = starIcon(i),
                        contentDescription = "star ${i + 1}",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(32.dp)
                    )
                }
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

package com.binarygeek119.covelounge.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.binarygeek119.covelounge.R
import com.binarygeek119.covelounge.data.MediaCard
import com.binarygeek119.covelounge.data.MediaKind

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    baseUrl: String,
    token: String,
    onPlayVideo: (Int) -> Unit,
    onPlayAudio: (Int) -> Unit,
    onViewImage: (Int) -> Unit,
    onOpenFolder: (MediaKind) -> Unit
) {
    val screenshots by viewModel.screenshots.collectAsState()
    val recentVideos by viewModel.recentVideos.collectAsState()
    val recentAudios by viewModel.recentAudios.collectAsState()
    val recentImages by viewModel.recentImages.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "CoveLounge",
                    modifier = Modifier.height(40.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "Settings",
                    color = Color.White,
                    modifier = Modifier.clickable { showSettings = true }
                )
            }
        }

        // Hero cover flow
        CoverFlow(
            items = screenshots,
            onOpen = { card ->
                when (card.kind) {
                    MediaKind.VIDEO -> onPlayVideo(card.id)
                    MediaKind.AUDIO -> onPlayAudio(card.id)
                    MediaKind.IMAGE -> onViewImage(card.id)
                }
            }
        )

        // Media rows
        MediaRow(
            title = "Videos",
            items = recentVideos,
            onFolderClick = { onOpenFolder(MediaKind.VIDEO) },
            onItemClick = { card -> onPlayVideo(card.id) }
        )
        MediaRow(
            title = "Audios",
            items = recentAudios,
            onFolderClick = { onOpenFolder(MediaKind.AUDIO) },
            onItemClick = { card -> onPlayAudio(card.id) }
        )
        MediaRow(
            title = "Images",
            items = recentImages,
            onFolderClick = { onOpenFolder(MediaKind.IMAGE) },
            onItemClick = { card -> onViewImage(card.id) }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = { Text("Preview mode and screenshot filters will be configurable here.") },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Close") }
            }
        )
    }
}

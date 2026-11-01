package com.binarygeek119.covelounge.ui.media

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage

@Composable
fun ImageScreen(baseUrl: String, token: String, id: Int, onBack: () -> Unit) {
    val url = "$baseUrl/api/stream/image/$id?access_token=$token"
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = url, contentDescription = null, modifier = Modifier.weight(1f))
    }
}

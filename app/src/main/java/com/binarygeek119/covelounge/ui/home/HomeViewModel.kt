package com.binarygeek119.covelounge.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import com.binarygeek119.covelounge.data.network.AudioDto
import com.binarygeek119.covelounge.data.network.ImageDto
import com.binarygeek119.covelounge.data.network.VideoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val session = SessionRepository(store)

    private val _videos = MutableStateFlow<List<VideoDto>>(emptyList())
    val videos: StateFlow<List<VideoDto>> = _videos
    private val _audios = MutableStateFlow<List<AudioDto>>(emptyList())
    val audios: StateFlow<List<AudioDto>> = _audios
    private val _images = MutableStateFlow<List<ImageDto>>(emptyList())
    val images: StateFlow<List<ImageDto>> = _images

    private var api: com.binarygeek119.covelounge.data.network.CoveApi? = null

    fun init(serverUrl: String?, token: String?) {
        if (serverUrl == null || token == null) return
        api = session.createApi(serverUrl, token)
        load()
    }

    private fun load() {
        val a = api ?: return
        viewModelScope.launch {
            try { _videos.value = a.videos().items } catch (_: Exception) {}
            try { _audios.value = a.audios().items } catch (_: Exception) {}
            try { _images.value = a.images().items } catch (_: Exception) {}
        }
    }
}

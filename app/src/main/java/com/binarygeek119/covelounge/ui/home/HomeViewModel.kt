package com.binarygeek119.covelounge.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binarygeek119.covelounge.data.MediaCard
import com.binarygeek119.covelounge.data.MediaKind
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val session = SessionRepository(store)

    private val _screenshots = MutableStateFlow<List<MediaCard>>(emptyList())
    val screenshots: StateFlow<List<MediaCard>> = _screenshots

    private val _recentVideos = MutableStateFlow<List<MediaCard>>(emptyList())
    val recentVideos: StateFlow<List<MediaCard>> = _recentVideos
    private val _recentAudios = MutableStateFlow<List<MediaCard>>(emptyList())
    val recentAudios: StateFlow<List<MediaCard>> = _recentAudios
    private val _recentImages = MutableStateFlow<List<MediaCard>>(emptyList())
    val recentImages: StateFlow<List<MediaCard>> = _recentImages

    private var api: com.binarygeek119.covelounge.data.network.CoveApi? = null
    private var baseUrl: String = ""
    private var token: String = ""

    fun init(serverUrl: String?, authToken: String?) {
        if (serverUrl == null || authToken == null) return
        if (api != null && baseUrl == serverUrl && token == authToken) return
        baseUrl = serverUrl.trimEnd('/')
        token = authToken
        api = session.createApi(serverUrl, authToken)
        load()
    }

    private fun load() {
        val a = api ?: return
        viewModelScope.launch {
            try {
                val res = a.videos(page = 1, perPage = 30, sort = "random", seed = (0..100000).random())
                _screenshots.value = res.items.map { v ->
                    MediaCard(v.id, v.title, "$baseUrl/api/stream/video/${v.id}/screenshot?access_token=$token", MediaKind.VIDEO)
                }
            } catch (_: Exception) {}
            try {
                val res = a.videos(page = 1, perPage = 15, sort = "created_at", direction = "desc")
                _recentVideos.value = res.items.map { v ->
                    MediaCard(v.id, v.title, "$baseUrl/api/videos/${v.id}/image?max=256&access_token=$token", MediaKind.VIDEO)
                }
            } catch (_: Exception) {}
            try {
                val res = a.audios(page = 1, perPage = 15, sort = "created_at", direction = "desc")
                _recentAudios.value = res.items.map { aud ->
                    MediaCard(aud.id, aud.title, "$baseUrl/api/audios/${aud.id}/image?max=256&access_token=$token", MediaKind.AUDIO)
                }
            } catch (_: Exception) {}
            try {
                val res = a.images(page = 1, perPage = 15, sort = "created_at", direction = "desc")
                _recentImages.value = res.items.map { img ->
                    MediaCard(img.id, img.title, "$baseUrl/api/stream/image/${img.id}?max=256&access_token=$token", MediaKind.IMAGE)
                }
            } catch (_: Exception) {}
        }
    }

    fun screenshotUrl(card: MediaCard): String = card.posterUrl
}

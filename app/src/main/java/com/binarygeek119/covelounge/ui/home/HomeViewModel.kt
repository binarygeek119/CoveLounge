package com.binarygeek119.covelounge.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binarygeek119.covelounge.data.MediaCard
import com.binarygeek119.covelounge.data.MediaKind
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SettingsStore
import com.binarygeek119.covelounge.data.SessionRepository
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val settingsStore = SettingsStore(app)
    private val session = SessionRepository(store)

    private val _screenshots = MutableStateFlow<List<MediaCard>>(emptyList())
    val screenshots: StateFlow<List<MediaCard>> = _screenshots

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _recentVideos = MutableStateFlow<List<MediaCard>>(emptyList())
    val recentVideos: StateFlow<List<MediaCard>> = _recentVideos
    private val _recentAudios = MutableStateFlow<List<MediaCard>>(emptyList())
    val recentAudios: StateFlow<List<MediaCard>> = _recentAudios
    private val _recentImages = MutableStateFlow<List<MediaCard>>(emptyList())
    val recentImages: StateFlow<List<MediaCard>> = _recentImages

    private val _recentGalleries = MutableStateFlow<List<MediaCard>>(emptyList())
    val recentGalleries: StateFlow<List<MediaCard>> = _recentGalleries

    private val _continueWatching = MutableStateFlow<List<MediaCard>>(emptyList())
    val continueWatching: StateFlow<List<MediaCard>> = _continueWatching

    private var api: com.binarygeek119.covelounge.data.network.CoveApi? = null
    private var baseUrl: String = ""
    private var token: String = ""

    fun reload() {
        load()
    }

    fun init(serverUrl: String?, authToken: String?) {
        if (serverUrl == null || authToken == null) return
        if (api != null && baseUrl == serverUrl && token == authToken) return
        baseUrl = serverUrl.trimEnd('/')
        token = authToken
        api = session.createApi(serverUrl, authToken)
        viewModelScope.launch {
            try {
                val me = api?.me()
                val perms = me?.permissions ?: emptyList()
                // Log available permissions for debugging; don't block loading.
                android.util.Log.d("HomeViewModel", "Permissions: $perms")
                if (perms.isEmpty()) {
                    _error.value = "No permissions returned; media may not load."
                }
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 403) {
                    _error.value = "Authentication failed; please sign in again."
                }
            } catch (e: Exception) { _error.value = e.message ?: "Failed to load media" }
        }
        load()
    }

    private fun load() {
        val a = api ?: run { _isLoading.value = false; return }
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val count = settingsStore.screenshotCount.first().coerceIn(1, 30)
                val res = a.videos(page = 1, perPage = count, sort = "random", seed = (0..100000).random())
                _screenshots.value = res.items.map { v ->
                    MediaCard(v.id, v.title, "$baseUrl/api/stream/video/${v.id}/screenshot?access_token=$token", MediaKind.VIDEO)
                }
            } catch (e: Exception) { _error.value = e.message ?: "Failed to load media" }
            try {
                val res = a.videos(page = 1, perPage = 15, sort = "created_at", direction = "desc")
                _recentVideos.value = res.items.map { v ->
                    val path = v.imagePath ?: "/api/stream/video/${v.id}/screenshot"
                    val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                    val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                    MediaCard(v.id, v.title, authUrl, MediaKind.VIDEO)
                }
            } catch (e: Exception) { _error.value = e.message ?: "Failed to load media" }
            try {
                val res = a.videos(page = 1, perPage = 15, sort = "last_played_at", direction = "desc")
                _continueWatching.value = res.items.map { v ->
                    MediaCard(v.id, v.title, "$baseUrl/api/videos/${v.id}/image?max=256&access_token=$token", MediaKind.VIDEO)
                }
            } catch (e: Exception) { _error.value = e.message ?: "Failed to load media" }
            try {
                val res = a.audios(page = 1, perPage = 15, sort = "created_at", direction = "desc")
                _recentAudios.value = res.items.map { aud ->
                    val path = aud.imagePath ?: "/api/audios/${aud.id}/image?max=256"
                    val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                    val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                    MediaCard(aud.id, aud.title, authUrl, MediaKind.AUDIO)
                }
            } catch (e: Exception) { _error.value = e.message ?: "Failed to load media" }
            try {
                val res = a.images(page = 1, perPage = 15, sort = "created_at", direction = "desc")
                _recentImages.value = res.items.map { img ->
                    MediaCard(img.id, img.title, "$baseUrl/api/stream/image/${img.id}?max=256&access_token=$token", MediaKind.IMAGE)
                }
            } catch (e: Exception) { _error.value = e.message ?: "Failed to load media" }
            try {
                val res = a.galleries(page = 1, perPage = 15, sort = "created_at", direction = "desc")
                _recentGalleries.value = res.items.map { g ->
                    MediaCard(g.id, g.title, "$baseUrl/api/galleries/${g.id}/cover?access_token=$token", MediaKind.GALLERY)
                }
            } catch (e: Exception) { _error.value = e.message ?: "Failed to load media" }
        }
        _isLoading.value = false
    }

    fun screenshotUrl(card: MediaCard): String = card.posterUrl
}

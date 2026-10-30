package com.binarygeek119.covelounge.ui.medialist

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

class MediaListViewModel(app: Application, val kind: MediaKind) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val session = SessionRepository(store)

    private val _items = MutableStateFlow<List<MediaCard>>(emptyList())
    val items: StateFlow<List<MediaCard>> = _items

    private var api: com.binarygeek119.covelounge.data.network.CoveApi? = null
    private var baseUrl: String = ""
    private var token: String = ""

    fun init(serverUrl: String?, authToken: String?) {
        if (serverUrl == null || authToken == null) return
        if (api != null) return
        baseUrl = serverUrl.trimEnd('/')
        token = authToken
        api = session.createApi(serverUrl, authToken)
        load()
    }

    private fun load() {
        val a = api ?: return
        viewModelScope.launch {
            try {
                when (kind) {
                    MediaKind.VIDEO -> {
                        val res = a.videos(page = 1, perPage = 50, sort = "created_at", direction = "desc")
                        _items.value = res.items.map { v ->
                            val path = v.imagePath ?: "/api/videos/${v.id}/image?max=256"
                            val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                            val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                            MediaCard(v.id, v.title, authUrl, MediaKind.VIDEO)
                        }
                    }
                    MediaKind.AUDIO -> {
                        val res = a.audios(page = 1, perPage = 50, sort = "created_at", direction = "desc")
                        _items.value = res.items.map { aud ->
                            val path = aud.imagePath ?: "/api/audios/${aud.id}/image?max=256"
                            val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                            val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                            MediaCard(aud.id, aud.title, authUrl, MediaKind.AUDIO)
                        }
                    }
                    MediaKind.IMAGE -> {
                        val res = a.images(page = 1, perPage = 50, sort = "created_at", direction = "desc")
                        _items.value = res.items.map { img ->
                            MediaCard(img.id, img.title, "$baseUrl/api/stream/image/${img.id}?max=256&access_token=$token", MediaKind.IMAGE)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

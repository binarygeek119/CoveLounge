package com.binarygeek119.covelounge.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binarygeek119.covelounge.data.MediaKind
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MediaDetailViewModel(app: Application, val kind: MediaKind, val id: Int) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val session = SessionRepository(store)

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title
    private val _details = MutableStateFlow<String?>(null)
    val details: StateFlow<String?> = _details
    private val _posterUrl = MutableStateFlow<String?>(null)
    val posterUrl: StateFlow<String?> = _posterUrl

    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount

    private var api: com.binarygeek119.covelounge.data.network.CoveApi? = null
    private var baseUrl: String = ""
    private var token: String = ""

    fun like() {
        val a = api ?: return
        viewModelScope.launch {
            try {
                val count = when (kind) {
                    MediaKind.VIDEO -> a.likeVideo(id)
                    MediaKind.AUDIO -> a.likeAudio(id)
                    MediaKind.IMAGE -> a.likeImage(id)
                }
                _likeCount.value = count
            } catch (_: Exception) {}
        }
    }

    fun recordPlay() {
        val a = api ?: return
        viewModelScope.launch {
            try {
                when (kind) {
                    MediaKind.VIDEO -> a.recordVideoPlay(id)
                    MediaKind.AUDIO -> a.recordAudioPlay(id)
                    MediaKind.IMAGE -> {}
                }
            } catch (_: Exception) {}
        }
    }

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
                        val dto = a.videoDetail(id)
                        _title.value = dto.title
                        _details.value = dto.details
                        _posterUrl.value = dto.imagePath?.let { p ->
                            val url = if (p.startsWith("http")) p else "$baseUrl${if (p.startsWith("/")) p else "/$p"}"
                            if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                        } ?: "$baseUrl/api/stream/video/$id/screenshot?access_token=$token"
                    }
                    MediaKind.AUDIO -> {
                        val dto = a.audioDetail(id)
                        _title.value = dto.title
                        _details.value = dto.details
                        _posterUrl.value = dto.imagePath?.let { p ->
                            val url = if (p.startsWith("http")) p else "$baseUrl${if (p.startsWith("/")) p else "/$p"}"
                            if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                        } ?: "$baseUrl/api/audios/$id/image?max=256&access_token=$token"
                    }
                    MediaKind.IMAGE -> {
                        val dto = a.imageDetail(id)
                        _title.value = dto.title
                        _details.value = dto.details
                        _posterUrl.value = "$baseUrl/api/stream/image/$id?max=512&access_token=$token"
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
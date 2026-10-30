package com.binarygeek119.covelounge.ui.search

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

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val session = SessionRepository(store)

    private val _results = MutableStateFlow<List<MediaCard>>(emptyList())
    val results: StateFlow<List<MediaCard>> = _results

    private var api: com.binarygeek119.covelounge.data.network.CoveApi? = null
    private var baseUrl: String = ""
    private var token: String = ""

    fun init(serverUrl: String?, authToken: String?) {
        if (serverUrl == null || authToken == null) return
        if (api != null) return
        baseUrl = serverUrl.trimEnd('/')
        token = authToken
        api = session.createApi(serverUrl, authToken)
    }

    fun search(query: String) {
        val a = api ?: return
        viewModelScope.launch {
            val list = mutableListOf<MediaCard>()
            try {
                val res = a.videos(page = 1, perPage = 20, q = query)
                list += res.items.map { v ->
                    MediaCard(v.id, v.title, "$baseUrl/api/videos/${v.id}/image?max=256&access_token=$token", MediaKind.VIDEO)
                }
            } catch (_: Exception) {}
            try {
                val res = a.audios(page = 1, perPage = 20, q = query)
                list += res.items.map { aud ->
                    MediaCard(aud.id, aud.title, "$baseUrl/api/audios/${aud.id}/image?max=256&access_token=$token", MediaKind.AUDIO)
                }
            } catch (_: Exception) {}
            try {
                val res = a.images(page = 1, perPage = 20, q = query)
                list += res.items.map { img ->
                    MediaCard(img.id, img.title, "$baseUrl/api/stream/image/${img.id}?max=256&access_token=$token", MediaKind.IMAGE)
                }
            } catch (_: Exception) {}
            _results.value = list
        }
    }
}
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

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _sort = MutableStateFlow("created_at")
    val sort: StateFlow<String> = _sort

    private val _direction = MutableStateFlow("desc")
    val direction: StateFlow<String> = _direction

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    private var api: com.binarygeek119.covelounge.data.network.CoveApi? = null
    private var baseUrl: String = ""
    private var token: String = ""
    private var page: Int = 1

    fun init(serverUrl: String?, authToken: String?) {
        if (serverUrl == null || authToken == null) return
        if (api != null) return
        baseUrl = serverUrl.trimEnd('/')
        token = authToken
        api = session.createApi(serverUrl, authToken)
        load(reset = true)
    }

    fun setSort(value: String) {
        if (_sort.value == value) return
        _sort.value = value
        load(reset = true)
    }

    fun setDirection(value: String) {
        if (_direction.value == value) return
        _direction.value = value
        load(reset = true)
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun search() {
        load(reset = true)
    }

    fun loadMore() {
        if (_loading.value || !_hasMore.value) return
        page += 1
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        val a = api ?: return
        if (reset) {
            page = 1
            _items.value = emptyList()
            _hasMore.value = true
        }
        _loading.value = true
        viewModelScope.launch {
            try {
                val q = _query.value.trim().ifEmpty { null }
                val sortVal = _sort.value
                val dirVal = _direction.value
                val perPage = 60
                when (kind) {
                    MediaKind.VIDEO -> {
                        val res = a.videos(page = page, perPage = perPage, sort = sortVal, direction = dirVal, q = q)
                        val mapped = res.items.map { v ->
                            val path = v.imagePath ?: "/api/stream/video/${v.id}/screenshot"
                            val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                            val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                            MediaCard(v.id, v.title, authUrl, MediaKind.VIDEO, v.performers?.mapNotNull { it.name } ?: emptyList(), v.rating)
                        }
                        append(mapped, res)
                    }
                    MediaKind.AUDIO -> {
                        val res = a.audios(page = page, perPage = perPage, sort = sortVal, direction = dirVal, q = q)
                        val mapped = res.items.map { aud ->
                            val path = aud.imagePath ?: "/api/audios/${aud.id}/image?max=256"
                            val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                            val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                            MediaCard(aud.id, aud.title, authUrl, MediaKind.AUDIO, rating = aud.rating)
                        }
                        append(mapped, res)
                    }
                    MediaKind.IMAGE -> {
                        val res = a.images(page = page, perPage = perPage, sort = sortVal, direction = dirVal, q = q)
                        val mapped = res.items.map { img ->
                            MediaCard(img.id, img.title, "$baseUrl/api/stream/image/${img.id}?max=256&access_token=$token", MediaKind.IMAGE, rating = img.rating)
                        }
                        append(mapped, res)
                    }
                    MediaKind.GALLERY -> {
                        val res = a.galleries(page = page, perPage = perPage, sort = sortVal, direction = dirVal, q = q)
                        val mapped = res.items.map { g ->
                            MediaCard(g.id, g.title, "$baseUrl/api/galleries/${g.id}/cover?access_token=$token", MediaKind.GALLERY, rating = g.rating)
                        }
                        append(mapped, res)
                    }
                }
            } catch (_: Exception) {
                if (reset) _items.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    private fun append(mapped: List<MediaCard>, res: com.binarygeek119.covelounge.data.network.PaginatedResponse<*>) {
        val current = _items.value
        _items.value = current + mapped
        // determine hasMore
        val total = res.totalCount
        val perPage = 60
        _hasMore.value = if (total > 0) (page * perPage < total) else (mapped.size >= perPage)
    }
}

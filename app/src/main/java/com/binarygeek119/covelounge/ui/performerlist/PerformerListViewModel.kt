package com.binarygeek119.covelounge.ui.performerlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PerformerCard(val id: Int, val name: String?, val imageUrl: String)

class PerformerListViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val session = SessionRepository(store)

    private val _items = MutableStateFlow<List<PerformerCard>>(emptyList())
    val items: StateFlow<List<PerformerCard>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _direction = MutableStateFlow("asc")
    val direction: StateFlow<String> = _direction

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

    fun setQuery(value: String) { _query.value = value }

    fun setDirection(value: String) { if (_direction.value != value) { _direction.value = value; load(reset = true) } }

    fun search() { load(reset = true) }

    fun loadMore() {
        if (_loading.value || !_hasMore.value) return
        page++
        load(reset = false)
    }

    fun loadAll() {
        if (_loading.value) return
        val a = api ?: return
        _loading.value = true
        viewModelScope.launch {
            try {
                val all = mutableListOf<com.binarygeek119.covelounge.data.network.PerformerDto>()
                var pg = 1
                while (true) {
                    val res = a.performers(page = pg, perPage = 100, sort = "name", direction = _direction.value, q = null)
                    if (res.items.isEmpty()) break
                    all.addAll(res.items)
                    if (res.items.size < 100) break
                    pg++
                }
                _items.value = all.map { p ->
                    val path = p.imagePath ?: "/api/performers/${p.id}/image?max=256"
                    val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                    val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                    PerformerCard(p.id, p.name, authUrl)
                }
                _hasMore.value = false
            } catch (_: Exception) {
            } finally {
                _loading.value = false
            }
        }
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
                val res = a.performers(page = page, perPage = 60, sort = "name", direction = _direction.value, q = q)
                val mapped = res.items.map { p ->
                    val path = p.imagePath ?: "/api/performers/${p.id}/image?max=256"
                    val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                    val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                    PerformerCard(p.id, p.name, authUrl)
                }
                _items.value = _items.value + mapped
                _hasMore.value = res.items.size >= 60
            } catch (_: Exception) {
                if (reset) _items.value = emptyList()
                _hasMore.value = false
            } finally {
                _loading.value = false
            }
        }
    }
}

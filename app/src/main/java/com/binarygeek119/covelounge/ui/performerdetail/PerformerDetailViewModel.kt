package com.binarygeek119.covelounge.ui.performerdetail

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

class PerformerDetailViewModel(app: Application, val performerId: Int, val performerName: String?, val performerImage: String?) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val session = SessionRepository(store)

    private val _gender = MutableStateFlow<String?>(null)
    val gender: StateFlow<String?> = _gender
    private val _born = MutableStateFlow<String?>(null)
    val born: StateFlow<String?> = _born
    private val _country = MutableStateFlow<String?>(null)
    val country: StateFlow<String?> = _country
    private val _ethnicity = MutableStateFlow<String?>(null)
    val ethnicity: StateFlow<String?> = _ethnicity
    private val _height = MutableStateFlow<String?>(null)
    val height: StateFlow<String?> = _height
    private val _measurements = MutableStateFlow<String?>(null)
    val measurements: StateFlow<String?> = _measurements
    private val _eyeColor = MutableStateFlow<String?>(null)
    val eyeColor: StateFlow<String?> = _eyeColor
    private val _hairColor = MutableStateFlow<String?>(null)
    val hairColor: StateFlow<String?> = _hairColor
    private val _fakeTits = MutableStateFlow<String?>(null)
    val fakeTits: StateFlow<String?> = _fakeTits
    private val _tattoos = MutableStateFlow<String?>(null)
    val tattoos: StateFlow<String?> = _tattoos
    private val _piercings = MutableStateFlow<String?>(null)
    val piercings: StateFlow<String?> = _piercings
    private val _career = MutableStateFlow<String?>(null)
    val career: StateFlow<String?> = _career

    private val _videos = MutableStateFlow<List<MediaCard>>(emptyList())
    val videos: StateFlow<List<MediaCard>> = _videos

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var api: com.binarygeek119.covelounge.data.network.CoveApi? = null
    private var baseUrl: String = ""
    private var token: String = ""

    fun init(serverUrl: String?, authToken: String?) {
        if (serverUrl == null || authToken == null) return
        if (api != null) return
        baseUrl = serverUrl.trimEnd('/')
        token = authToken
        api = session.createApi(serverUrl, authToken)
        fetchPerformerDetail()
        val cached = PerformerDetailCache.get(performerId)
        if (cached != null) {
            _videos.value = cached.videos
            _loading.value = false
        } else {
            load()
        }
    }

    private fun fetchPerformerDetail() {
        val a = api ?: return
        viewModelScope.launch {
            try {
                val dto = a.performerDetail(performerId)
                _gender.value = dto.gender
                _born.value = dto.born
                _country.value = dto.country
                _ethnicity.value = dto.ethnicity
                _height.value = dto.height
                _measurements.value = dto.measurements
                _eyeColor.value = dto.eyeColor
                _hairColor.value = dto.hairColor
                _fakeTits.value = dto.fakeTits
                _tattoos.value = dto.tattoos
                _piercings.value = dto.piercings
                _career.value = dto.career
            } catch (_: Exception) {}
        }
    }

    private fun load() {
        val a = api ?: return
        _loading.value = true
        viewModelScope.launch {
            try {
                val all = mutableListOf<com.binarygeek119.covelounge.data.network.VideoDto>()
                var pg = 1
                while (true) {
                    val r = a.videos(page = pg, perPage = 100, sort = "created_at", direction = "desc")
                    if (r.items.isEmpty()) break
                    all.addAll(r.items)
                    if (r.items.size < 100) break
                    pg++
                }
                val name = performerName?.lowercase()
                val filtered = all.filter { v ->
                    name == null || (v.performers?.any { it.name?.lowercase() == name } == true)
                }
                val mapped = filtered.map { v ->
                    val path = v.imagePath ?: "/api/stream/video/${v.id}/screenshot"
                    val url = if (path.startsWith("http")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
                    val authUrl = if (url.contains("access_token=")) url else "$url${if (url.contains("?")) "&" else "?"}access_token=$token"
                    MediaCard(v.id, v.title, authUrl, MediaKind.VIDEO, v.performers?.mapNotNull { it.name } ?: emptyList(), v.rating)
                }
                _videos.value = mapped
                PerformerDetailCache.put(performerId, PerformerDetailCache.CachedPerformer(performerName, performerImage, mapped))
            } catch (_: Exception) {
                _videos.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}

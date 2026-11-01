package com.binarygeek119.covelounge.ui.performerdetail

import com.binarygeek119.covelounge.data.MediaCard

object PerformerDetailCache {
    private const val MAX = 10
    private val cache = LinkedHashMap<Int, CachedPerformer>(MAX, 0.75f, true)

    data class CachedPerformer(
        val name: String?,
        val image: String?,
        val videos: List<MediaCard>
    )

    @Synchronized
    fun get(id: Int): CachedPerformer? = cache[id]

    @Synchronized
    fun put(id: Int, value: CachedPerformer) {
        cache[id] = value
        if (cache.size > MAX) {
            val it = cache.entries.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
            }
        }
    }
}

package com.binarygeek119.covelounge.ui.media

import com.binarygeek119.covelounge.data.network.PerformerSummaryDto

object PlayerPerformerCache {
    private var cachedVideoId: Int = -1
    private var cachedPerformers: List<PerformerSummaryDto> = emptyList()

    fun get(videoId: Int): List<PerformerSummaryDto>? {
        return if (cachedVideoId == videoId) cachedPerformers else null
    }

    fun put(videoId: Int, performers: List<PerformerSummaryDto>) {
        cachedVideoId = videoId
        cachedPerformers = performers
    }

    fun clear() {
        cachedVideoId = -1
        cachedPerformers = emptyList()
    }
}

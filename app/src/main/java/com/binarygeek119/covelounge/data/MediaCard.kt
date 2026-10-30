package com.binarygeek119.covelounge.data

enum class MediaKind { VIDEO, AUDIO, IMAGE, GALLERY }

data class MediaCard(
    val id: Int,
    val title: String?,
    val posterUrl: String,
    val kind: MediaKind
)
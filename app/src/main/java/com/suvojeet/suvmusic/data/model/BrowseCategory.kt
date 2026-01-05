package com.suvojeet.suvmusic.data.model

/**
 * Represents a browse category (mood/genre) from YouTube Music.
 */
data class BrowseCategory(
    val title: String,
    val browseId: String,
    val params: String? = null,
    val thumbnailUrl: String? = null,
    val color: Long? = null
)

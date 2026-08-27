package com.suvojeet.suvmusic.simpmusic

import kotlinx.serialization.Serializable

@Serializable
data class SimpMusicApiResponse(
    val success: Boolean,
    val data: List<LyricsData>
)

@Serializable
data class LyricsData(
    val duration: Int? = null,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    // Enhanced LRC ("LRC v2") with word-level timestamps. When present, prefer
    // this over [syncedLyrics] for word-by-word highlighting.
    val richSyncLyrics: String? = null
)

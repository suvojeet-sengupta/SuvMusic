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
    val plainLyrics: String? = null
)

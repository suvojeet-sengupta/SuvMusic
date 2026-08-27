package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song

/** Platform-neutral search and stream-resolution boundary for YouTube sources. */
interface YouTubeSource {
    suspend fun search(query: String): List<YouTubeSearchResult>
    suspend fun resolveStreamSong(result: YouTubeSearchResult): Song?
}

data class YouTubeSearchResult(
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val url: String,
    val thumbnailUrl: String?,
)

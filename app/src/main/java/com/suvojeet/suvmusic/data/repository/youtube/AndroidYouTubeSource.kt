package com.suvojeet.suvmusic.data.repository.youtube

import com.suvojeet.suvmusic.core.domain.repository.YouTubeSearchResult
import com.suvojeet.suvmusic.core.domain.repository.YouTubeSource
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.youtube.search.YouTubeSearchService
import com.suvojeet.suvmusic.data.repository.youtube.streaming.YouTubeStreamingService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android host adapter for the shared extraction boundary.
 * Existing search and stream services remain the source of truth until their
 * internals are migrated out of the Android application module.
 */
@Singleton
class AndroidYouTubeSource @Inject constructor(
    private val searchService: YouTubeSearchService,
    private val streamingService: YouTubeStreamingService,
) : YouTubeSource {
    override suspend fun search(query: String): List<YouTubeSearchResult> =
        searchService.search(query).map { song ->
            YouTubeSearchResult(
                title = song.title,
                uploader = song.artist,
                durationSeconds = song.duration / 1000L,
                url = "https://music.youtube.com/watch?v=${song.id}",
                thumbnailUrl = song.thumbnailUrl,
            )
        }

    override suspend fun resolveStreamSong(result: YouTubeSearchResult): Song? {
        val videoId = result.url.youtubeVideoId() ?: return null
        val details = streamingService.getSongDetails(videoId) ?: return null
        val streamUrl = streamingService.getStreamUrl(videoId) ?: return null
        return details.copy(streamUrl = streamUrl)
    }
}

private fun String.youtubeVideoId(): String? = when {
    contains("watch?v=") -> substringAfter("watch?v=").substringBefore('&')
    contains("youtu.be/") -> substringAfter("youtu.be/").substringBefore('?')
    else -> null
}

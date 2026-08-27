package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song

/**
 * Offline-first Linux recommendation adapter. It ranks local tracks for now;
 * the same boundary can later combine shared remote recommendations.
 */
class DesktopRecommendationSource(
    private val localMediaSource: LocalMediaSource,
) : RecommendationSource {
    override suspend fun getPersonalizedRecommendations(limit: Int): List<Song> =
        localMediaSource.getAllLocalSongs().take(limit.coerceAtLeast(0))

    override suspend fun getUpNext(currentSong: Song, currentQueue: List<Song>, count: Int): List<Song> =
        localMediaSource.getAllLocalSongs()
            .filter { it.id != currentSong.id && it.id !in currentQueue.map(Song::id).toSet() }
            .take(count.coerceAtLeast(0))
}

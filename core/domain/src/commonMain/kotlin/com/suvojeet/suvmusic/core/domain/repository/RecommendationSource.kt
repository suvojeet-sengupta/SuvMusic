package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song

/** Shared recommendation boundary for Home and playback queue features. */
interface RecommendationSource {
    suspend fun getPersonalizedRecommendations(limit: Int = 20): List<Song>
    suspend fun getUpNext(currentSong: Song, currentQueue: List<Song> = emptyList(), count: Int = 15): List<Song>
}

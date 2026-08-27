package com.suvojeet.suvmusic.recommendation

import com.suvojeet.suvmusic.core.domain.repository.RecommendationSource
import com.suvojeet.suvmusic.core.model.Song
import javax.inject.Inject
import javax.inject.Singleton

/** Android host adapter; RecommendationEngine remains the production source of truth. */
@Singleton
class AndroidRecommendationSource @Inject constructor(
    private val engine: RecommendationEngine,
) : RecommendationSource {
    override suspend fun getPersonalizedRecommendations(limit: Int): List<Song> =
        engine.getPersonalizedRecommendations(limit)

    override suspend fun getUpNext(currentSong: Song, currentQueue: List<Song>, count: Int): List<Song> =
        engine.getUpNext(currentSong, currentQueue, count)
}

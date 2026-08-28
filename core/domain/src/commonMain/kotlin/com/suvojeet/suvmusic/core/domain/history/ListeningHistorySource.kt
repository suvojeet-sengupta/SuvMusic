package com.suvojeet.suvmusic.core.domain.history

import kotlinx.coroutines.flow.Flow

/** Stable common history item used by shared Home and recommendation surfaces. */
data class ListeningHistoryItem(
    val songId: String,
    val songTitle: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Long,
    val playCount: Long,
    val lastPlayed: Long,
)

/** Platform-neutral read boundary for recent listening history. */
interface ListeningHistorySource {
    fun observeRecent(limit: Int = 20): Flow<List<ListeningHistoryItem>>
}

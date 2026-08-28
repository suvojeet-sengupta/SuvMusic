package com.suvojeet.suvmusic.data

import com.suvojeet.suvmusic.core.domain.history.ListeningHistoryItem
import com.suvojeet.suvmusic.core.domain.history.ListeningHistorySource
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Android adapter; Room remains the production history store during migration. */
@Singleton
class AndroidListeningHistorySource @Inject constructor(
    private val repository: ListeningHistoryRepository,
) : ListeningHistorySource {
    override fun observeRecent(limit: Int): Flow<List<ListeningHistoryItem>> =
        repository.getRecentlyPlayed(limit.coerceAtLeast(1)).map { rows ->
            rows.map { row ->
                ListeningHistoryItem(
                    songId = row.songId,
                    songTitle = row.songTitle,
                    artist = row.artist,
                    thumbnailUrl = row.thumbnailUrl,
                    duration = row.duration,
                    playCount = row.playCount.toLong(),
                    lastPlayed = row.lastPlayed,
                )
            }
        }
}

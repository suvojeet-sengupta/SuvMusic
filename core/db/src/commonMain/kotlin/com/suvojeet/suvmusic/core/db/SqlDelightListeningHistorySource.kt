package com.suvojeet.suvmusic.core.db

import com.suvojeet.suvmusic.core.domain.history.ListeningHistoryItem
import com.suvojeet.suvmusic.core.domain.history.ListeningHistorySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** SQLDelight-backed common history adapter for desktop and future Android hosts. */
class SqlDelightListeningHistorySource(
    private val store: ListeningHistoryStore,
) : ListeningHistorySource {
    override fun observeRecent(limit: Int): Flow<List<ListeningHistoryItem>> =
        store.observeRecent(limit.coerceAtLeast(1).toLong()).map { rows ->
            rows.map { row ->
                ListeningHistoryItem(
                    songId = row.songId,
                    songTitle = row.songTitle,
                    artist = row.artist,
                    thumbnailUrl = row.thumbnailUrl,
                    duration = row.duration,
                    playCount = row.playCount,
                    lastPlayed = row.lastPlayed,
                )
            }
        }
}

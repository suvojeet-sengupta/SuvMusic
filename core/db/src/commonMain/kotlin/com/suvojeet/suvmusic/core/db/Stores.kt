package com.suvojeet.suvmusic.core.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

/** Stable common model exposed by the KMP library store. */
data class LibraryEntry(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val type: String,
    val timestamp: Long,
)

/** Stable common model exposed by the KMP listening-history store. */
data class HistoryEntry(
    val songId: String,
    val songTitle: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Long,
    val playCount: Long,
    val lastPlayed: Long,
)

/** Shared persistence facade for Android and the JVM desktop host. */
class LibraryStore(database: SuvMusicDatabase) {
    private val queries = database.libraryItemsQueries

    fun observeAll() = queries
        .selectAllItems()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> rows.map { row ->
            LibraryEntry(row.id, row.title, row.subtitle, row.thumbnailUrl, row.type, row.timestamp)
        } }

    fun observeByType(type: String) = queries
        .selectItemsByType(type)
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> rows.map { row ->
            LibraryEntry(row.id, row.title, row.subtitle, row.thumbnailUrl, row.type, row.timestamp)
        } }

    fun save(
        id: String,
        title: String,
        subtitle: String?,
        thumbnailUrl: String?,
        type: String,
        timestamp: Long,
    ) = queries.insertOrReplaceItem(id, title, subtitle, thumbnailUrl, type, timestamp)

    fun renamePlaylist(id: String, title: String) = queries.updateItemName(title, id)

    fun updatePlaylistThumbnail(id: String, thumbnailUrl: String?) =
        queries.updateItemThumbnail(thumbnailUrl, id)

    fun delete(id: String) = queries.deleteItemById(id)

    fun isSaved(id: String): Boolean = queries.isItemSaved(id).executeAsOne()
}

/** Shared listening-history query facade used by recommendations and desktop stats. */
class ListeningHistoryStore(database: SuvMusicDatabase) {
    private val queries = database.listeningHistoryQueries

    fun observeAll() = queries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> rows.map { row ->
            HistoryEntry(
                songId = row.songId,
                songTitle = row.songTitle,
                artist = row.artist,
                thumbnailUrl = row.thumbnailUrl,
                duration = row.duration,
                playCount = row.playCount,
                lastPlayed = row.lastPlayed,
            )
        } }

    fun observeRecent(limit: Long) = queries
        .selectRecentByLastPlayed(limit)
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> rows.map { row ->
            HistoryEntry(
                songId = row.songId,
                songTitle = row.songTitle,
                artist = row.artist,
                thumbnailUrl = row.thumbnailUrl,
                duration = row.duration,
                playCount = row.playCount,
                lastPlayed = row.lastPlayed,
            )
        } }

    fun count(): Long = queries.countAll().executeAsOne()

    fun totalListeningTimeMs(): Long = when (val total = queries.sumTotalListeningTime().executeAsOne()) {
        is Long -> total
        is Int -> total.toLong()
        else -> 0L
    }

    fun clear() = queries.deleteAll()
}

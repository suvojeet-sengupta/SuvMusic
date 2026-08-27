package com.suvojeet.suvmusic.core.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers

/**
 * Shared persistence facade for the KMP hosts. Generated SQLDelight models stay
 * internal to this module, while both Android and desktop use the same queries.
 */
class LibraryStore(database: SuvMusicDatabase) {
    private val queries = database.libraryItemsQueries

    fun observeAll() = queries
        .selectAllItems()
        .asFlow()
        .mapToList(Dispatchers.Default)

    fun observeByType(type: String) = queries
        .selectItemsByType(type)
        .asFlow()
        .mapToList(Dispatchers.Default)

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

    fun observeRecent(limit: Long) = queries
        .selectRecentByLastPlayed(limit)
        .asFlow()
        .mapToList(Dispatchers.Default)

    fun count(): Long = queries.countAll().executeAsOne()

    fun totalListeningTimeMs(): Long = queries.sumTotalListeningTime().executeAsOne() ?: 0L

    fun clear() = queries.deleteAll()
}

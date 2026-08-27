package com.suvojeet.suvmusic.core.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
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
data class PlaylistSummary(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val timestamp: Long,
    val songCount: Long,
)

data class PlaylistSongEntry(
    val playlistId: String,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val duration: Long,
    val source: String,
    val localUri: String?,
    val releaseDate: String?,
    val addedAt: Long,
    val order: Long,
)

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

    fun getById(id: String): LibraryEntry? = queries
        .selectItemById(id)
        .executeAsOneOrNull()
        ?.let { row -> LibraryEntry(row.id, row.title, row.subtitle, row.thumbnailUrl, row.type, row.timestamp) }

    fun observePlaylists() = queries
        .selectItemsByTypeWithSongCount("PLAYLIST")
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> rows.map { row ->
            PlaylistSummary(row.id, row.title, row.subtitle, row.thumbnailUrl, row.timestamp, row.songCount)
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

    fun deletePlaylist(id: String) = queries.transaction {
        queries.deletePlaylistSongs(id)
        queries.deleteItemById(id)
    }

    fun insertPlaylistSong(song: PlaylistSongEntry) = queries.insertOrReplacePlaylistSong(
        song.playlistId,
        song.songId,
        song.title,
        song.artist,
        song.album,
        song.thumbnailUrl,
        song.duration,
        song.source,
        song.localUri,
        song.releaseDate,
        song.addedAt,
        song.order,
    )

    fun isSaved(id: String): Boolean = queries.isItemSaved(id).executeAsOne()

    fun observeSaved(id: String) = queries
        .isItemSaved(id)
        .asFlow()
        .mapToOne(Dispatchers.Default)

    fun nextPlaylistOrder(playlistId: String): Long =
        queries.nextPlaylistOrder(playlistId).executeAsOne()

    fun observePlaylistSongs(playlistId: String) = queries
        .selectPlaylistSongs(playlistId)
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> rows.map { row ->
            PlaylistSongEntry(
                playlistId = row.playlistId,
                songId = row.songId,
                title = row.title,
                artist = row.artist,
                album = row.album,
                thumbnailUrl = row.thumbnailUrl,
                duration = row.duration,
                source = row.source,
                localUri = row.localUri,
                releaseDate = row.releaseDate,
                addedAt = row.addedAt,
                order = row.order,
            )
        } }

    fun isSongInPlaylist(playlistId: String, songId: String): Boolean =
        queries.isSongInPlaylist(playlistId, songId).executeAsOne()

    fun countPlaylistSongs(playlistId: String): Long =
        queries.countPlaylistSongs(playlistId).executeAsOne()

    fun replacePlaylistSongs(playlistId: String, songs: List<PlaylistSongEntry>) {
        queries.transaction {
            queries.deletePlaylistSongs(playlistId)
            songs.forEach { song ->
                queries.insertOrReplacePlaylistSong(
                    song.playlistId,
                    song.songId,
                    song.title,
                    song.artist,
                    song.album,
                    song.thumbnailUrl,
                    song.duration,
                    song.source,
                    song.localUri,
                    song.releaseDate,
                    song.addedAt,
                    song.order,
                )
            }
        }
    }

    fun deleteSongFromPlaylist(playlistId: String, songId: String) =
        queries.deleteSongFromPlaylist(playlistId, songId)
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

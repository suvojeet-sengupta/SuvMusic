package com.suvojeet.suvmusic.core.db

import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.LibraryItem
import com.suvojeet.suvmusic.core.model.LibraryItemType
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Common repository implementation used by both hosts. The Android Room
 * implementation remains available until all Android consumers are switched.
 */
class SqlDelightLibraryRepository(
    private val libraryStore: LibraryStore,
) : LibraryRepository {
    override suspend fun savePlaylist(playlist: Playlist) {
        libraryStore.save(playlist.id, playlist.title, playlist.author, playlist.thumbnailUrl, "PLAYLIST", 0L)
        savePlaylistSongs(playlist.id, playlist.songs)
    }

    override suspend fun savePlaylistSongs(playlistId: String, songs: List<Song>) =
        libraryStore.replacePlaylistSongs(playlistId, songs.mapIndexed { index, song -> song.toEntry(playlistId, index) })

    override suspend fun appendPlaylistSongs(playlistId: String, songs: List<Song>, startOrder: Int) =
        songs.forEachIndexed { index, song ->
            libraryStore.insertPlaylistSong(song.toEntry(playlistId, startOrder + index))
        }

    override suspend fun getCachedPlaylistSongs(playlistId: String): List<Song> =
        libraryStore.observePlaylistSongs(playlistId).firstValue().map(::toSong)

    override fun getCachedPlaylistSongsFlow(playlistId: String): Flow<List<Song>> =
        libraryStore.observePlaylistSongs(playlistId).map { rows -> rows.map(::toSong) }

    override fun getPlaylistSongCountFlow(playlistId: String): Flow<Int> =
        libraryStore.observePlaylistSongs(playlistId).map { it.size }

    override suspend fun isSongInPlaylist(playlistId: String, songId: String) =
        libraryStore.isSongInPlaylist(playlistId, songId)

    override suspend fun updatePlaylistThumbnail(playlistId: String, thumbnailUrl: String?) =
        libraryStore.updatePlaylistThumbnail(playlistId, thumbnailUrl)

    override suspend fun updatePlaylistName(playlistId: String, name: String) =
        libraryStore.renamePlaylist(playlistId, name)

    override suspend fun replacePlaylistSongs(playlistId: String, songs: List<Song>) =
        savePlaylistSongs(playlistId, songs)

    override suspend fun removePlaylist(playlistId: String) = libraryStore.deletePlaylist(playlistId)

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) =
        libraryStore.deleteSongFromPlaylist(playlistId, songId)

    override suspend fun addSongToPlaylist(playlistId: String, song: Song) =
        libraryStore.insertPlaylistSong(song.toEntry(playlistId, 0))

    override suspend fun saveAlbum(album: Album) =
        libraryStore.save(album.id, album.title, album.artist, album.thumbnailUrl, "ALBUM", 0L)

    override suspend fun removeAlbum(albumId: String) = libraryStore.delete(albumId)

    override fun isPlaylistSaved(playlistId: String): Flow<Boolean> = flow {
        emit(libraryStore.isSaved(playlistId))
    }

    override fun isAlbumSaved(albumId: String): Flow<Boolean> = flow {
        emit(libraryStore.isSaved(albumId))
    }

    override suspend fun getPlaylistById(id: String): LibraryItem? = libraryStore.getById(id)?.toLibraryItem()

    override fun getSavedPlaylists(): Flow<List<PlaylistDisplayItem>> = libraryStore.observePlaylists().map { rows ->
        rows.map { row ->
            PlaylistDisplayItem(
                id = row.id,
                name = row.title,
                url = "https://music.youtube.com/playlist?list=${row.id}",
                uploaderName = row.subtitle.orEmpty(),
                thumbnailUrl = row.thumbnailUrl,
                songCount = row.songCount.toInt(),
            )
        }
    }

    override fun getSavedAlbums(): Flow<List<LibraryItem>> = libraryStore.observeByType("ALBUM").map { rows ->
        rows.map { it.toLibraryItem() }
    }

    override suspend fun saveArtist(artist: Artist) =
        libraryStore.save(artist.id, artist.name, artist.subscribers, artist.thumbnailUrl, "ARTIST", 0L)

    override suspend fun removeArtist(artistId: String) = libraryStore.delete(artistId)

    override fun getSavedArtists(): Flow<List<LibraryItem>> = libraryStore.observeByType("ARTIST").map { rows ->
        rows.map { it.toLibraryItem() }
    }

    override fun isArtistSaved(artistId: String): Flow<Boolean> = flow {
        emit(libraryStore.isSaved(artistId))
    }

    private fun Song.toEntry(playlistId: String, order: Int) = PlaylistSongEntry(
        playlistId = playlistId,
        songId = id,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        source = source.name,
        localUri = localUri,
        releaseDate = releaseDate,
        addedAt = addedAt,
        order = order.toLong(),
    )

    private fun toSong(row: PlaylistSongEntry) = Song(
        id = row.songId,
        title = row.title,
        artist = row.artist,
        album = row.album.orEmpty(),
        duration = row.duration,
        thumbnailUrl = row.thumbnailUrl,
        source = runCatching { SongSource.valueOf(row.source) }.getOrDefault(SongSource.YOUTUBE),
        localUri = row.localUri,
        releaseDate = row.releaseDate,
        addedAt = row.addedAt,
    )

    private fun LibraryEntry.toLibraryItem() = LibraryItem(
        id = id,
        title = title,
        subtitle = subtitle.orEmpty(),
        thumbnailUrl = thumbnailUrl,
        type = runCatching { LibraryItemType.valueOf(type) }.getOrDefault(LibraryItemType.UNKNOWN),
        timestamp = timestamp,
    )

    private suspend fun <T> Flow<List<T>>.firstValue(): List<T> = first()
}

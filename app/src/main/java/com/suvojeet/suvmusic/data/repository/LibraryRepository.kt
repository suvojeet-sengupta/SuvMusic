package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.data.local.entity.PlaylistSongEntity
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Playlist
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val libraryDao: LibraryDao
) {
    suspend fun savePlaylist(playlist: Playlist) {
        val entity = LibraryEntity(
            id = playlist.id,
            title = playlist.title,
            subtitle = playlist.author,
            thumbnailUrl = playlist.thumbnailUrl,
            type = "PLAYLIST"
        )
        libraryDao.insertItem(entity)
        
        // Also cache songs if present
        if (playlist.songs.isNotEmpty()) {
            savePlaylistSongs(playlist.id, playlist.songs)
        }
    }

    suspend fun savePlaylistSongs(playlistId: String, songs: List<Song>) {
        val entities = songs.mapIndexed { index, song ->
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration,
                source = song.source.name,
                order = index
            )
        }
        libraryDao.deletePlaylistSongs(playlistId)
        libraryDao.insertPlaylistSongs(entities)
    }

    suspend fun appendPlaylistSongs(playlistId: String, songs: List<Song>, startOrder: Int) {
        val entities = songs.mapIndexed { index, song ->
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration,
                source = song.source.name,
                order = startOrder + index
            )
        }
        libraryDao.insertPlaylistSongs(entities)
    }

    suspend fun getCachedPlaylistSongs(playlistId: String): List<Song> {
        return libraryDao.getPlaylistSongs(playlistId).map { entity ->
            Song(
                id = entity.songId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album ?: "",
                thumbnailUrl = entity.thumbnailUrl,
                duration = entity.duration,
                source = try { SongSource.valueOf(entity.source) } catch (e: Exception) { SongSource.YOUTUBE }
            )
        }
    }

    fun getCachedPlaylistSongsFlow(playlistId: String): Flow<List<Song>> {
        return libraryDao.getPlaylistSongsFlow(playlistId).map { entities ->
            entities.map { entity ->
                Song(
                    id = entity.songId,
                    title = entity.title,
                    artist = entity.artist,
                    album = entity.album ?: "",
                    thumbnailUrl = entity.thumbnailUrl,
                    duration = entity.duration,
                    source = try { SongSource.valueOf(entity.source) } catch (e: Exception) { SongSource.YOUTUBE }
                )
            }
        }
    }

    suspend fun removePlaylist(playlistId: String) {
        libraryDao.deleteItem(playlistId)
        libraryDao.deletePlaylistSongs(playlistId)
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        libraryDao.deleteSongFromPlaylist(playlistId, songId)
    }

    suspend fun saveAlbum(album: Album) {
        val entity = LibraryEntity(
            id = album.id,
            title = album.title,
            subtitle = album.artist,
            thumbnailUrl = album.thumbnailUrl,
            type = "ALBUM"
        )
        libraryDao.insertItem(entity)
    }

    suspend fun removeAlbum(albumId: String) {
        libraryDao.deleteItem(albumId)
    }

    fun isPlaylistSaved(playlistId: String): Flow<Boolean> {
        return libraryDao.isItemSavedFlow(playlistId)
    }

    fun isAlbumSaved(albumId: String): Flow<Boolean> {
        return libraryDao.isItemSavedFlow(albumId)
    }

    suspend fun getPlaylistById(id: String): LibraryEntity? = libraryDao.getItem(id)

    fun getSavedPlaylists(): Flow<List<LibraryEntity>> {
        return libraryDao.getItemsByType("PLAYLIST")
    }

    fun getSavedAlbums(): Flow<List<LibraryEntity>> {
        return libraryDao.getItemsByType("ALBUM")
    }
}

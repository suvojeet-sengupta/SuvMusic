package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Playlist
import kotlinx.coroutines.flow.Flow
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
    }

    suspend fun removePlaylist(playlistId: String) {
        libraryDao.deleteItem(playlistId)
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

    fun getSavedPlaylists(): Flow<List<LibraryEntity>> {
        return libraryDao.getItemsByType("PLAYLIST")
    }

    fun getSavedAlbums(): Flow<List<LibraryEntity>> {
        return libraryDao.getItemsByType("ALBUM")
    }
}

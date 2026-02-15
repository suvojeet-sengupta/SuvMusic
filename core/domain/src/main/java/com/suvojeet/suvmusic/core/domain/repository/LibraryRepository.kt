package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.LibraryItem
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    suspend fun savePlaylist(playlist: Playlist)
    suspend fun savePlaylistSongs(playlistId: String, songs: List<Song>)
    suspend fun appendPlaylistSongs(playlistId: String, songs: List<Song>, startOrder: Int)
    suspend fun getCachedPlaylistSongs(playlistId: String): List<Song>
    fun getCachedPlaylistSongsFlow(playlistId: String): Flow<List<Song>>
    fun getPlaylistSongCountFlow(playlistId: String): Flow<Int>
    suspend fun replacePlaylistSongs(playlistId: String, songs: List<Song>)
    suspend fun removePlaylist(playlistId: String)
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    suspend fun addSongToPlaylist(playlistId: String, song: Song)
    suspend fun saveAlbum(album: Album)
    suspend fun removeAlbum(albumId: String)
    fun isPlaylistSaved(playlistId: String): Flow<Boolean>
    fun isAlbumSaved(albumId: String): Flow<Boolean>
    suspend fun getPlaylistById(id: String): LibraryItem?
    fun getSavedPlaylists(): Flow<List<LibraryItem>>
    fun getSavedAlbums(): Flow<List<LibraryItem>>
    suspend fun saveArtist(artist: Artist)
    suspend fun removeArtist(artistId: String)
    fun getSavedArtists(): Flow<List<LibraryItem>>
    fun isArtistSaved(artistId: String): Flow<Boolean>
}

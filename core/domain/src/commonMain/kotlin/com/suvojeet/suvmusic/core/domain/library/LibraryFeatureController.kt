package com.suvojeet.suvmusic.core.domain.library

import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.LibraryItem
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import kotlinx.coroutines.flow.Flow

/** Shared library feature state and mutations for both Compose hosts. */
class LibraryFeatureController(
    private val repository: LibraryRepository,
) {
    val savedPlaylists: Flow<List<PlaylistDisplayItem>> = repository.getSavedPlaylists()
    val savedAlbums: Flow<List<LibraryItem>> = repository.getSavedAlbums()
    val savedArtists: Flow<List<LibraryItem>> = repository.getSavedArtists()

    fun isPlaylistSaved(id: String): Flow<Boolean> = repository.isPlaylistSaved(id)
    fun isAlbumSaved(id: String): Flow<Boolean> = repository.isAlbumSaved(id)
    fun isArtistSaved(id: String): Flow<Boolean> = repository.isArtistSaved(id)

    suspend fun removePlaylist(id: String) = repository.removePlaylist(id)
    suspend fun removeAlbum(id: String) = repository.removeAlbum(id)
    suspend fun removeArtist(id: String) = repository.removeArtist(id)
    suspend fun renamePlaylist(id: String, name: String) = repository.updatePlaylistName(id, name)
}

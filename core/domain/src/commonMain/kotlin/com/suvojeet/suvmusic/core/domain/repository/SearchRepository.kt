package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for music search operations.
 * Implemented by app-layer repositories (YouTube, JioSaavn, Local).
 */
interface SearchRepository {
    fun searchSongs(query: String): Flow<List<Song>>
    fun searchAlbums(query: String): Flow<List<Album>>
    fun searchArtists(query: String): Flow<List<Artist>>
    fun searchPlaylists(query: String): Flow<List<Playlist>>
    suspend fun getSongDetails(songId: String): Song?
}

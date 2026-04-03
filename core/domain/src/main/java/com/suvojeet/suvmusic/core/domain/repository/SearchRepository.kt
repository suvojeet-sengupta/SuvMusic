package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SearchResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for music search operations.
 * Implemented by app-layer repositories (YouTube, JioSaavn, Local).
 */
interface SearchRepository {
    fun searchSongs(query: String): Flow<List<Song>>
    fun searchAlbums(query: String): Flow<List<SearchResult>>
    fun searchArtists(query: String): Flow<List<SearchResult>>
    fun searchPlaylists(query: String): Flow<List<SearchResult>>
    suspend fun getSongDetails(songId: String): Song?
}

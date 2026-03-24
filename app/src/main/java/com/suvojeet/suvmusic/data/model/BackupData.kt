package com.suvojeet.suvmusic.data.model

import com.suvojeet.suvmusic.core.data.local.entity.DislikedArtist
import com.suvojeet.suvmusic.core.data.local.entity.DislikedSong
import com.suvojeet.suvmusic.core.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.core.data.local.entity.PlaylistSongEntity
import com.suvojeet.suvmusic.core.data.local.entity.SongGenre

/**
 * Complete backup model for SuvMusic.
 * Contains all user data: settings, library, and history.
 */
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Settings (Serialized from DataStore)
    val settings: Map<String, Any?> = emptyMap(),
    
    // Library Data
    val libraryItems: List<LibraryEntity> = emptyList(),
    val playlistSongs: List<PlaylistSongEntity> = emptyList(),
    
    // History and Preferences
    val listeningHistory: List<ListeningHistory> = emptyList(),
    val dislikedSongs: List<DislikedSong> = emptyList(),
    val dislikedArtists: List<DislikedArtist> = emptyList(),
    val songGenres: List<SongGenre> = emptyList()
)

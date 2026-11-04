package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistManagementUiState(
    val userPlaylists: List<PlaylistDisplayItem> = emptyList(),
    val isLoadingPlaylists: Boolean = false,
    val isCreatingPlaylist: Boolean = false,
    val isAddingSong: Boolean = false,
    val showAddToPlaylistSheet: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val selectedSongs: List<Song> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing playlist operations like creation and adding songs.
 * Separate from PlaylistViewModel which is for viewing a specific playlist.
 */
class PlaylistManagementViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val libraryRepository: LibraryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlaylistManagementUiState())
    val uiState: StateFlow<PlaylistManagementUiState> = _uiState.asStateFlow()
    
    /**
     * Load user's editable playlists from YouTube Music.
     */
    fun loadUserPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlaylists = true) }
            
            val playlists = youTubeRepository.getUserEditablePlaylists()
            
            _uiState.update { 
                it.copy(
                    userPlaylists = playlists,
                    isLoadingPlaylists = false
                )
            }
        }
    }
    
    /**
     * Show the Add to Playlist sheet for a song.
     */
    fun showAddToPlaylistSheet(song: Song) {
        _uiState.update { 
            it.copy(
                showAddToPlaylistSheet = true,
                selectedSongs = listOf(song)
            )
        }
        loadUserPlaylists()
    }

    /**
     * Show the Add to Playlist sheet for multiple songs.
     */
    fun showAddToPlaylistSheet(songs: List<Song>) {
        if (songs.isEmpty()) return
        _uiState.update { 
            it.copy(
                showAddToPlaylistSheet = true,
                selectedSongs = songs
            )
        }
        loadUserPlaylists()
    }
    
    /**
     * Hide the Add to Playlist sheet.
     */
    fun hideAddToPlaylistSheet() {
        _uiState.update { 
            it.copy(
                showAddToPlaylistSheet = false,
                selectedSongs = emptyList()
            )
        }
    }
    
    /**
     * Show the Create Playlist dialog.
     */
    fun showCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = true) }
    }
    
    /**
     * Hide the Create Playlist dialog.
     */
    fun hideCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = false) }
    }
    
    /**
     * Create a new playlist on YouTube Music.
     */
    fun createPlaylist(title: String, description: String, isPrivate: Boolean, syncWithYt: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingPlaylist = true) }
            
            val songs = _uiState.value.selectedSongs
            val playlistId = if (syncWithYt && sessionManager.canPushToYouTube()) {
                val privacyStatus = if (isPrivate) "PRIVATE" else "PUBLIC"
                youTubeRepository.createPlaylist(title, description, privacyStatus)
            } else {
                // Create Local Playlist
                try {
                    val id = "local_" + UUID.randomUUID().toString()
                    val playlist = Playlist(
                        id = id, 
                        title = title, 
                        author = "You", 
                        thumbnailUrl = songs.firstOrNull()?.thumbnailUrl, // Use first song thumb if creating from songs
                        songs = emptyList()
                    )
                    libraryRepository.savePlaylist(playlist)
                    id
                } catch (e: Exception) {
                    null
                }
            }
            
            if (playlistId != null) {
                if (songs.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isCreatingPlaylist = false,
                            showCreatePlaylistDialog = false,
                            successMessage = "Created \"$title\""
                        )
                    }
                } else {
                    val added = youTubeRepository.addSongsToAnyPlaylist(playlistId, songs).isSuccess

                    if (added && !playlistId.startsWith("local_")) {
                        // Persist a local copy so a freshly-created synced playlist shows its
                        // songs immediately instead of waiting for a YouTube re-fetch.
                        try {
                            libraryRepository.savePlaylist(
                                Playlist(
                                    id = playlistId,
                                    title = title,
                                    author = "You",
                                    thumbnailUrl = songs.firstOrNull()?.thumbnailUrl,
                                    songs = songs
                                )
                            )
                        } catch (e: Exception) {
                            // Best-effort — the songs already synced to YouTube.
                        }
                    }

                    val msg = when {
                        !added -> "Created \"$title\""
                        songs.size == 1 -> "Created \"$title\" and added ${songs[0].title}"
                        else -> "Created \"$title\" and added ${songs.size} songs"
                    }
                    _uiState.update {
                        it.copy(
                            isCreatingPlaylist = false,
                            showCreatePlaylistDialog = false,
                            showAddToPlaylistSheet = false,
                            selectedSongs = emptyList(),
                            successMessage = msg
                        )
                    }
                }

                // Refresh playlists
                loadUserPlaylists()
            } else {
                _uiState.update { 
                    it.copy(
                        isCreatingPlaylist = false,
                        errorMessage = "Failed to create playlist"
                    )
                }
            }
        }
    }
    
    /**
     * Add selected songs to an existing playlist.
     */
    fun addSongsToPlaylist(playlistId: String) {
        val songs = _uiState.value.selectedSongs
        if (songs.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingSong = true) }

            val result = youTubeRepository.addSongsToAnyPlaylist(playlistId, songs)
            val success = result.isSuccess
            val message = result.describe(songs)

            _uiState.update {
                it.copy(
                    isAddingSong = false,
                    showAddToPlaylistSheet = false,
                    selectedSongs = emptyList(),
                    successMessage = if (success) message else null,
                    errorMessage = if (!success) message else null
                )
            }
        }
    }

    /**
     * Clear any messages.
     */
    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}

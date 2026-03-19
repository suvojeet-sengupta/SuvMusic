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
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val selectedSong: Song? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing playlist operations like creation and adding songs.
 * Separate from PlaylistViewModel which is for viewing a specific playlist.
 */
@HiltViewModel
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
                selectedSong = song
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
                selectedSong = null
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
            
            val song = _uiState.value.selectedSong
            val playlistId = if (syncWithYt && sessionManager.isLoggedIn()) {
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
                        thumbnailUrl = song?.thumbnailUrl, // Use song thumb if creating from a song
                        songs = emptyList()
                    )
                    libraryRepository.savePlaylist(playlist)
                    id
                } catch (e: Exception) {
                    null
                }
            }
            
            if (playlistId != null) {
                // If there's a selected song, add it to the new playlist
                if (song != null) {
                    val added = if (syncWithYt && sessionManager.isLoggedIn()) {
                        youTubeRepository.addSongToPlaylist(playlistId, song.id)
                    } else {
                         try {
                             libraryRepository.addSongToPlaylist(playlistId, song)
                             true
                         } catch (e: Exception) {
                             false
                         }
                    }

                    if (added) {
                        _uiState.update { 
                            it.copy(
                                isCreatingPlaylist = false,
                                showCreatePlaylistDialog = false,
                                showAddToPlaylistSheet = false,
                                selectedSong = null,
                                successMessage = "Created \"$title\" and added ${song.title}"
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isCreatingPlaylist = false,
                                showCreatePlaylistDialog = false,
                                successMessage = "Created \"$title\""
                            )
                        }
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isCreatingPlaylist = false,
                            showCreatePlaylistDialog = false,
                            successMessage = "Created \"$title\""
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
     * Add selected song to an existing playlist.
     */
    fun addSongToPlaylist(playlistId: String) {
        val song = _uiState.value.selectedSong ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingSong = true) }
            
            var success = false
            var message: String? = null

            if (playlistId.startsWith("local_")) {
                try {
                    // 1. Check for duplicates
                    if (libraryRepository.isSongInPlaylist(playlistId, song.id)) {
                        success = false
                        message = "${song.title} is already in this playlist"
                    } else {
                        // 2. Add song
                        libraryRepository.addSongToPlaylist(playlistId, song)
                        
                        // 3. Auto-update thumbnail if playlist has none
                        val playlistItem = libraryRepository.getPlaylistById(playlistId)
                        if (playlistItem != null && playlistItem.thumbnailUrl.isNullOrBlank()) {
                            libraryRepository.updatePlaylistThumbnail(playlistId, song.thumbnailUrl)
                        }
                        
                        success = true
                        message = "Added ${song.title} to playlist"
                    }
                } catch (e: Exception) {
                    success = false
                    message = "Failed to add ${song.title}"
                }
            } else {
                success = youTubeRepository.addSongToPlaylist(playlistId, song.id)
                message = if (success) "Added ${song.title} to playlist" else "Failed to add to YouTube playlist"
            }
            
            _uiState.update { 
                it.copy(
                    isAddingSong = false,
                    showAddToPlaylistSheet = false,
                    selectedSong = null,
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

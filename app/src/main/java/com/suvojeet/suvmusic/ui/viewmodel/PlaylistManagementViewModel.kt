package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Playlist
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.LibraryRepository
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
                        thumbnailUrl = null, 
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
                val song = _uiState.value.selectedSong
                if (song != null) {
                    val added = if (syncWithYt && sessionManager.isLoggedIn()) {
                        youTubeRepository.addSongToPlaylist(playlistId, song.id)
                    } else {
                         // For local playlist, we need to implement adding song (LibraryRepository logic)
                         // Assuming savePlaylist works for updates or we have addSongToPlaylist in libraryRepo
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
                                successMessage = "Created \"$title\" and added song"
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
                        errorMessage = "Failed to create playlist."
                    )
                }
            }
        }
    }
    
    /**
     * Add a song to an existing playlist.
     */
    fun addSongToPlaylist(playlistId: String) {
        val song = _uiState.value.selectedSong ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingSong = true) }
            
            val success = youTubeRepository.addSongToPlaylist(playlistId, song.id)
            
            _uiState.update { 
                it.copy(
                    isAddingSong = false,
                    showAddToPlaylistSheet = false,
                    selectedSong = null,
                    successMessage = if (success) "Added to playlist" else null,
                    errorMessage = if (!success) "Failed to add to playlist" else null
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

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
                // If there are selected songs, add them to the new playlist
                if (songs.isNotEmpty()) {
                    var success = false
                    if (syncWithYt && sessionManager.isLoggedIn() && !playlistId.startsWith("local_")) {
                        success = youTubeRepository.addSongsToPlaylist(playlistId, songs.map { it.id })
                    } else {
                        var successCount = 0
                        for (song in songs) {
                             try {
                                 libraryRepository.addSongToPlaylist(playlistId, song)
                                 successCount++
                             } catch (e: Exception) {
                                 // Skip
                             }
                        }
                        success = successCount > 0
                    }

                    if (success) {
                        val msg = if (songs.size == 1) "Created \"$title\" and added ${songs[0].title}"
                                 else "Created \"$title\" and added ${songs.size} songs"
                        _uiState.update { 
                            it.copy(
                                isCreatingPlaylist = false,
                                showCreatePlaylistDialog = false,
                                showAddToPlaylistSheet = false,
                                selectedSongs = emptyList(),
                                successMessage = msg
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
     * Add selected songs to an existing playlist.
     */
    fun addSongsToPlaylist(playlistId: String) {
        val songs = _uiState.value.selectedSongs
        if (songs.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingSong = true) }
            
            var successCount = 0
            var lastMessage: String? = null

            if (playlistId.startsWith("local_")) {
                for (song in songs) {
                    try {
                        // 1. Check for duplicates
                        if (libraryRepository.isSongInPlaylist(playlistId, song.id)) {
                            lastMessage = "${song.title} is already in this playlist"
                        } else {
                            // 2. Add song
                            libraryRepository.addSongToPlaylist(playlistId, song)
                            
                            // 3. Auto-update thumbnail if playlist has none
                            val playlistItem = libraryRepository.getPlaylistById(playlistId)
                            if (playlistItem != null && playlistItem.thumbnailUrl.isNullOrBlank()) {
                                libraryRepository.updatePlaylistThumbnail(playlistId, song.thumbnailUrl)
                            }
                            
                            successCount++
                        }
                    } catch (e: Exception) {
                        lastMessage = "Failed to add ${song.title}"
                    }
                }
            } else {
                val success = youTubeRepository.addSongsToPlaylist(playlistId, songs.map { it.id })
                if (success) successCount = songs.size
                else lastMessage = "Failed to add songs to YouTube"
            }
            
            val success = successCount > 0
            val finalMessage = when {
                songs.size == 1 && successCount == 1 -> "Added ${songs[0].title} to playlist"
                songs.size == 1 && successCount == 0 -> lastMessage ?: "Failed to add to playlist"
                successCount == songs.size -> "Added $successCount songs to playlist"
                successCount > 0 -> "Added $successCount of ${songs.size} songs to playlist"
                else -> lastMessage ?: "Failed to add songs to playlist"
            }
            
            _uiState.update { 
                it.copy(
                    isAddingSong = false,
                    showAddToPlaylistSheet = false,
                    selectedSongs = emptyList(),
                    successMessage = if (success) finalMessage else null,
                    errorMessage = if (!success) finalMessage else null
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

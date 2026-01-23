package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Playlist
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.LibraryRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditable: Boolean = false,
    val isRenaming: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isSaved: Boolean = false
) {
    val isUserPlaylist: Boolean
        get() = isEditable // Alias for clarity
}

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: com.suvojeet.suvmusic.data.repository.JioSaavnRepository,
    private val sessionManager: com.suvojeet.suvmusic.data.SessionManager,
    private val musicPlayer: com.suvojeet.suvmusic.player.MusicPlayer,
    private val downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository,
    private val libraryRepository: LibraryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle[Destination.Playlist.ARG_PLAYLIST_ID])
    private val initialName: String? = savedStateHandle.get<String>(Destination.Playlist.ARG_NAME)?.let { 
        try { URLDecoder.decode(it, "UTF-8").takeIf { decoded -> decoded.isNotBlank() } } catch (e: Exception) { null }
    }
    private val initialThumbnail: String? = savedStateHandle.get<String>(Destination.Playlist.ARG_THUMBNAIL)?.let {
        try { URLDecoder.decode(it, "UTF-8").takeIf { decoded -> decoded.isNotBlank() } } catch (e: Exception) { null }
    }
    
    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()
    
    val batchProgress = downloadRepository.batchProgress

    init {
        // Set initial login state
        _uiState.update { it.copy(isLoggedIn = sessionManager.isLoggedIn()) }
        
        // Show initial data from navigation immediately
        if (initialName != null || initialThumbnail != null) {
            _uiState.update {
                it.copy(
                    playlist = Playlist(
                        id = playlistId,
                        title = initialName ?: "Loading...",
                        author = "",
                        thumbnailUrl = initialThumbnail,
                        songs = emptyList()
                    ),
                    isLoading = true
                )
            }
        }
        loadPlaylist()
        checkLibraryStatus()
    }

    private fun checkLibraryStatus() {
        viewModelScope.launch {
            libraryRepository.isPlaylistSaved(playlistId).collect { isSaved ->
                _uiState.update { it.copy(isSaved = isSaved) }
            }
        }
    }

    fun toggleSaveToLibrary() {
        viewModelScope.launch {
            val playlist = _uiState.value.playlist ?: return@launch
            if (_uiState.value.isSaved) {
                libraryRepository.removePlaylist(playlist.id)
            } else {
                libraryRepository.savePlaylist(playlist)
            }
        }
    }

    private fun loadPlaylist() {
        checkEditable()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentSource = sessionManager.getMusicSource()
                
                // Smart loading based on source preference
                val playlist = if (currentSource == com.suvojeet.suvmusic.data.MusicSource.JIOSAAVN) {
                    // In HQ Audio mode, prioritize JioSaavn
                    val jioPlaylist = jioSaavnRepository.getPlaylist(playlistId)
                    if (jioPlaylist != null) {
                        jioPlaylist
                    } else {
                        // Fallback to YouTube if not found in JioSaavn (e.g. user clicked a YT playlist)
                        youTubeRepository.getPlaylist(playlistId)
                    }
                } else {
                    // In YouTube mode, prioritize YouTube
                    try {
                        youTubeRepository.getPlaylist(playlistId)
                    } catch (e: Exception) {
                        // Fallback to JioSaavn if not found in YouTube (e.g. user clicked a Jio playlist)
                        jioSaavnRepository.getPlaylist(playlistId) ?: throw e
                    }
                }
                
                // Merge with initial data:
                // - Prefer navigation thumbnail (it's the correct playlist art from Home screen)
                // - Fallback to API data only if nav data is missing
                val finalPlaylist = playlist.copy(
                    title = if (playlist.title == "Unknown Playlist" && initialName != null) initialName else playlist.title,
                    thumbnailUrl = initialThumbnail ?: playlist.thumbnailUrl,
                    author = playlist.author.takeIf { it.isNotBlank() } ?: ""
                )
                
                _uiState.update { 
                    it.copy(
                        playlist = finalPlaylist,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }


    private fun checkEditable() {
        viewModelScope.launch {
            try {
                val userPlaylists = youTubeRepository.getUserEditablePlaylists()
                val isEditable = userPlaylists.any { it.id == playlistId } || playlistId.startsWith("PL") 
                
                _uiState.update { it.copy(isEditable = isEditable || playlistId == "LM") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEditable = false) }
            }
        }
    }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRenaming = true) }
            val success = youTubeRepository.renamePlaylist(playlistId, newName)
            if (success) {
                // Refresh
                loadPlaylist()
            }
            _uiState.update { it.copy(isRenaming = false) }
        }
    }
    
    fun createPlaylist(title: String, description: String, isPrivate: Boolean) {
        viewModelScope.launch {
             _uiState.update { it.copy(isCreating = true) }
             val privacyStatus = if (isPrivate) "PRIVATE" else "PUBLIC"
             youTubeRepository.createPlaylist(title, description, privacyStatus)
             _uiState.update { it.copy(isCreating = false) }
        }
    }

    fun reorderSong(fromIndex: Int, toIndex: Int) {
        val currentPlaylist = _uiState.value.playlist ?: return
        val songs = currentPlaylist.songs.toMutableList()
        
        // Optimistic update
        val movedSong = songs.removeAt(fromIndex)
        songs.add(toIndex, movedSong)
        
        _uiState.update { 
            it.copy(playlist = currentPlaylist.copy(songs = songs))
        }
        
        viewModelScope.launch {
            val setVideoId = movedSong.setVideoId
            
            if (setVideoId != null) {
                // Determine predecessor
                // if toIndex == 0, predecessor is null (move to top)
                // else predecessor is the song at toIndex - 1 (in the NEW list)
                val predecessorId = if (toIndex > 0) songs[toIndex - 1].setVideoId else null
                
                youTubeRepository.moveSongInPlaylist(playlistId, setVideoId, predecessorId)
            } else {
                // Revert if we can't move (no setVideoId)
                loadPlaylist()
            }
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val success = youTubeRepository.deletePlaylist(playlistId)
            _uiState.update { it.copy(isDeleting = false, deleteSuccess = success) }
        }
    }

    fun playNext(songs: List<Song>) {
        musicPlayer.playNext(songs)
    }

    fun addToQueue(songs: List<Song>) {
        musicPlayer.addToQueue(songs)
    }

    fun downloadPlaylist(songs: List<Song>) {
        viewModelScope.launch {
            downloadRepository.downloadSongs(songs)
        }
    }
}


package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Playlist
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
    val error: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
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

    init {
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
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val playlist = youTubeRepository.getPlaylist(playlistId)
                
                // Merge with initial data - prefer API data but fallback to nav params
                val finalPlaylist = playlist.copy(
                    title = if (playlist.title == "Unknown Playlist" && initialName != null) initialName else playlist.title,
                    thumbnailUrl = playlist.thumbnailUrl ?: initialThumbnail
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
}


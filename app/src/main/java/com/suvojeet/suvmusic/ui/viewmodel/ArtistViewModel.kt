package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ArtistError {
    NETWORK,
    AUTH_REQUIRED,
    UNKNOWN
}

    data class ArtistUiState(
    val artist: Artist? = null,
    val isLoading: Boolean = false,
    val error: ArtistError? = null,
    val isSubscribing: Boolean = false,
    val isStartingRadio: Boolean = false
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[Destination.Artist.ARG_ARTIST_ID])

    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        loadArtist()
    }

    fun loadArtist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Heuristic to determine source
                val isYouTubeId = artistId.startsWith("UC") || artistId.startsWith("FE") || artistId.startsWith("VL")
                
                val artist = if (isYouTubeId) {
                    youTubeRepository.getArtist(artistId)
                } else {
                    jioSaavnRepository.getArtist(artistId)
                }

                if (artist != null) {
                    _uiState.update {
                        it.copy(
                            artist = artist,
                            isLoading = false
                        )
                    }
                } else {
                    // Determine error type based on session state
                    val errorType = if (!sessionManager.isLoggedIn() && isYouTubeId) {
                        ArtistError.AUTH_REQUIRED
                    } else {
                        ArtistError.NETWORK
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorType
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = ArtistError.UNKNOWN,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleSubscribe() {
        val currentArtist = _uiState.value.artist ?: return
        if (currentArtist.id.startsWith("UC") || currentArtist.id.startsWith("FE")) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubscribing = true) }
                val newStatus = !currentArtist.isSubscribed
                val success = youTubeRepository.subscribe(currentArtist.id, newStatus)
                
                if (success) {
                    // Update local state immediately for responsiveness
                    _uiState.update { 
                        it.copy(
                            artist = currentArtist.copy(isSubscribed = newStatus),
                            isSubscribing = false 
                        ) 
                    }
                    // Background refresh to get latest count/status
                    loadArtist()
                } else {
                    _uiState.update { it.copy(isSubscribing = false) }
                }
            }
        }
    }

    fun startRadio(onPlaylistReady: (String) -> Unit) {
        val currentArtist = _uiState.value.artist ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingRadio = true) }
            // Try to get radio ID from artist page
            var radioId = currentArtist.channelId?.let { youTubeRepository.getArtistRadioId(it) }
            
            // Fallback: search for "Artist Name Radio" or just play top songs
            if (radioId == null) {
                // For now, if we can't find specific radio, we can just return
                 _uiState.update { it.copy(isStartingRadio = false) }
                 return@launch
            }
            
            _uiState.update { it.copy(isStartingRadio = false) }
            onPlaylistReady(radioId)
        }
    }
}
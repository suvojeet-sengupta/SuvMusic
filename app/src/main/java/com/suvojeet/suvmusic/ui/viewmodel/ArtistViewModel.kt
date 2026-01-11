package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.Artist
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
    val error: ArtistError? = null
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
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
                val artist = youTubeRepository.getArtist(artistId)

                if (artist != null) {
                    _uiState.update {
                        it.copy(
                            artist = artist,
                            isLoading = false
                        )
                    }
                } else {
                    // Determine error type based on session state
                    val errorType = if (!sessionManager.isLoggedIn()) {
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
        viewModelScope.launch {
            youTubeRepository.subscribe(currentArtist.id, true)
            loadArtist()
        }
    }
}
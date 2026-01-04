package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class ArtistUiState(
    val artist: Artist? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[Destination.Artist.ARG_ARTIST_ID])
    
    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        loadArtist()
    }

    private fun loadArtist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val artist = youTubeRepository.getArtist(artistId)
                _uiState.update { 
                    it.copy(
                        artist = artist,
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
    
    fun toggleSubscribe() {
        val currentArtist = _uiState.value.artist ?: return
        viewModelScope.launch {
            // Optimistic update if we had a boolean "isSubscribed" in Artist model
            // But we don't, and the API returns localized text like "SUBSCRIBED" or "4.2M subscribers"
            // So we blindly try to subscribe based on assumption or button state passed from UI
            // For now, let's just trigger it.
             youTubeRepository.subscribe(currentArtist.id, true) // Defaulting to true for now, need logic
             // Ideally we should refresh the artist to see the new state
             loadArtist()
        }
    }
}

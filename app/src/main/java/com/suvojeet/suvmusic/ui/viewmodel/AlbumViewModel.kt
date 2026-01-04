package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumUiState(
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle[Destination.Album.ARG_ALBUM_ID])
    
    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        loadAlbum()
    }

    private fun loadAlbum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val album = youTubeRepository.getAlbum(albumId)
                _uiState.update { 
                    it.copy(
                        album = album,
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

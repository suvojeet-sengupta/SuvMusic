package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val playlists: List<PlaylistDisplayItem> = emptyList(),
    val localSongs: List<Song> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(),
    val likedSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val localAudioRepository: LocalAudioRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
        observeDownloads()
    }
    
    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.downloadedSongs.collect { downloads ->
                _uiState.update { it.copy(downloadedSongs = downloads) }
            }
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val playlists = youTubeRepository.getUserPlaylists()
                val localSongs = localAudioRepository.getAllLocalSongs()
                val likedSongs = youTubeRepository.getLikedMusic()
                
                _uiState.update { 
                    it.copy(
                        playlists = playlists,
                        localSongs = localSongs,
                        likedSongs = likedSongs,
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
    
    fun refresh() {
        loadData()
    }
}
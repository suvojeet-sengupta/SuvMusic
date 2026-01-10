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
    private val jioSaavnRepository: com.suvojeet.suvmusic.data.repository.JioSaavnRepository,
    private val localAudioRepository: LocalAudioRepository,
    private val downloadRepository: DownloadRepository,
    private val sessionManager: com.suvojeet.suvmusic.data.SessionManager
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
                // Refresh downloads to scan for new files in Downloads/SuvMusic
                downloadRepository.refreshDownloads()
                
                // Get current source preference
                val musicSource = sessionManager.getMusicSource()
                
                // Fetch from both sources
                val ytPlaylists = try {
                    youTubeRepository.getUserPlaylists()
                } catch (e: Exception) {
                    emptyList()
                }
                
                // Only show JioSaavn playlists if Source is HQ Audio (JioSaavn)
                val jioPlaylists = if (musicSource == com.suvojeet.suvmusic.data.MusicSource.JIOSAAVN) {
                    try {
                        jioSaavnRepository.getFeaturedPlaylists()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                
                val allPlaylists = ytPlaylists + jioPlaylists
                val localSongs = localAudioRepository.getAllLocalSongs()
                val likedSongs = try {
                    youTubeRepository.getLikedMusic()
                } catch (e: Exception) {
                    emptyList()
                }
                
                _uiState.update { 
                    it.copy(
                        playlists = allPlaylists,
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
    
    fun createPlaylist(
        title: String,
        description: String,
        isPrivate: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val privacyStatus = if (isPrivate) "PRIVATE" else "PUBLIC"
            val playlistId = youTubeRepository.createPlaylist(title, description, privacyStatus)
            if (playlistId != null) {
                // Refresh playlists to show the new one
                val playlists = youTubeRepository.getUserPlaylists()
                _uiState.update { it.copy(playlists = playlists) }
            }
            onComplete()
        }
    }
    
    fun refresh() {
        loadData()
    }
}
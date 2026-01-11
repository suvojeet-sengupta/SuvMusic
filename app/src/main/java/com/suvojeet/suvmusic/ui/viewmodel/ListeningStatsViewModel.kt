package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.local.dao.ArtistStats
import com.suvojeet.suvmusic.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListeningStatsUiState(
    val totalSongsPlayed: Int = 0,
    val totalListeningTimeMs: Long = 0L,
    val topSongs: List<ListeningHistory> = emptyList(),
    val topArtists: List<ArtistStats> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ListeningStatsViewModel @Inject constructor(
    private val listeningHistoryRepository: ListeningHistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ListeningStatsUiState())
    val uiState: StateFlow<ListeningStatsUiState> = _uiState.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Load stats
                val stats = listeningHistoryRepository.getListeningStats()
                val topSongs = listeningHistoryRepository.getTopSongs(10)
                val topArtists = listeningHistoryRepository.getTopArtists(5)
                
                // Collect flows
                topSongs.collect { songs ->
                    topArtists.let { artists ->
                        _uiState.update {
                            it.copy(
                                totalSongsPlayed = stats.totalSongsPlayed,
                                totalListeningTimeMs = stats.totalListeningTimeMs,
                                topSongs = songs,
                                topArtists = artists,
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun refresh() {
        loadStats()
    }
}

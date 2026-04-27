package com.suvojeet.suvmusic.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.core.model.RecentlyPlayed
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RecentsViewModel @Inject constructor(
    private val repository: ListeningHistoryRepository,
    private val downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository
) : ViewModel() {

    val incognitoModeEnabled: StateFlow<Boolean> = repository.sessionManager.privacyModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedSongs = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongs = _selectedSongs.asStateFlow()

    fun toggleSelection(songId: String) {
        _selectedSongs.update { current ->
            if (current.contains(songId)) current - songId else current + songId
        }
    }

    fun clearSelection() {
        _selectedSongs.value = emptySet()
    }

    fun setIncognitoMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.sessionManager.setPrivacyModeEnabled(enabled)
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            downloadRepository.downloadSong(song)
        }
    }

    // Fetch history for the last 30 days (1 month)
    // The user mentioned "upto 3months... upto 1monty". 
    // I'll stick to 30 days as the most reasonable interpretation of "upto 1monty" correction.
    val recentSongs: StateFlow<List<RecentlyPlayed>> = repository.getHistoryForTimePeriod(30)
        .map { historyList ->
            historyList.map { history ->
                history.toRecentlyPlayed()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun ListeningHistory.toRecentlyPlayed(): RecentlyPlayed {
        val song = Song(
            id = this.songId,
            title = this.songTitle,
            artist = this.artist,
            album = this.album,
            duration = this.duration,
            thumbnailUrl = this.thumbnailUrl,
            source = try {
                SongSource.valueOf(this.source)
            } catch (e: Exception) {
                SongSource.YOUTUBE
            },
            localUri = this.localUri,
            artistId = this.artistId
        )
        return RecentlyPlayed(song, this.lastPlayed)
    }
}

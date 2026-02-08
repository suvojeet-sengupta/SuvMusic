package com.suvojeet.suvmusic.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.data.model.RecentlyPlayed
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val repository: ListeningHistoryRepository
) : ViewModel() {

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
            localUri = this.localUri?.let { android.net.Uri.parse(it) },
            artistId = this.artistId
        )
        return RecentlyPlayed(song, this.lastPlayed)
    }
}

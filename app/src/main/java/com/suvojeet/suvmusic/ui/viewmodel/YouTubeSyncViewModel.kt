package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.YouTubeSyncSelection
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class YouTubeSyncViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<PlaylistDisplayItem>>(emptyList())
    val playlists: StateFlow<List<PlaylistDisplayItem>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val excludedPlaylists: StateFlow<Set<String>> = sessionManager.youtubeSyncExcludedPlaylistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val pullOnly: StateFlow<Boolean> = sessionManager.youtubePullOnlyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            val fetched = runCatching {
                youTubeRepository.getUserPlaylists(autoSave = false, includeUnsynced = true)
            }.getOrDefault(emptyList())
            _playlists.value = YouTubeSyncSelection.pickerItems(fetched)
            _isLoading.value = false
        }
    }

    fun setPullOnly(enabled: Boolean) {
        viewModelScope.launch { sessionManager.setYouTubePullOnlyEnabled(enabled) }
    }

    fun setPlaylistSynced(playlistId: String, synced: Boolean) {
        viewModelScope.launch { sessionManager.setYouTubePlaylistSynced(playlistId, synced) }
    }
}

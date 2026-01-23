package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    
    val downloadedSongs: StateFlow<List<Song>> = downloadRepository.downloadedSongs
    
    private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongIds: StateFlow<Set<String>> = _selectedSongIds
    
    val isSelectionMode = _selectedSongIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(songId)
            // If deleted song was selected, remove it from selection
            _selectedSongIds.value = _selectedSongIds.value - songId
        }
    }
    
    fun toggleSelection(songId: String) {
        val currentSelection = _selectedSongIds.value
        if (currentSelection.contains(songId)) {
            _selectedSongIds.value = currentSelection - songId
        } else {
            _selectedSongIds.value = currentSelection + songId
        }
    }
    
    fun selectAll() {
        val allIds = downloadedSongs.value.map { it.id }.toSet()
        _selectedSongIds.value = allIds
    }
    
    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }
    
    fun deleteSelected() {
        viewModelScope.launch {
            val idsToDelete = _selectedSongIds.value.toList()
            downloadRepository.deleteDownloads(idsToDelete)
            clearSelection()
        }
    }
    
    fun deleteAll() {
        viewModelScope.launch {
            downloadRepository.deleteAllDownloads()
            clearSelection()
        }
    }
    
    fun refreshDownloads() {
        downloadRepository.refreshDownloads()
    }
}

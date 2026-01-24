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
    val queueState: StateFlow<List<Song>> = downloadRepository.queueState
    val downloadingIds: StateFlow<Set<String>> = downloadRepository.downloadingIds
    val downloadProgress: StateFlow<Map<String, Float>> = downloadRepository.downloadProgress
    
    val downloadItems: StateFlow<List<DownloadItem>> = kotlinx.coroutines.flow.combine(
        downloadedSongs,
        queueState,
        downloadingIds,
        downloadProgress
    ) { downloaded, queued, downloading, progressMap ->
        val allSongs = (downloaded + queued).distinctBy { it.id }
        
        val collections = allSongs.filter { it.collectionId != null }
            .groupBy { it.collectionId!! }
            .map { (id, groupSongs) ->
                val first = groupSongs.first()
                DownloadItem.CollectionItem(
                    id = id,
                    title = first.collectionName ?: "Unknown Collection",
                    thumbnailUrl = first.thumbnailUrl,
                    songs = groupSongs.map { song ->
                        val isDownloading = downloading.contains(song.id)
                        val progress = progressMap[song.id] ?: if (downloaded.any { it.id == song.id }) 1.0f else 0.0f
                        SongStatus(song, isDownloading, progress)
                    }
                )
            }
        
        val singles = allSongs.filter { it.collectionId == null }
            .map { song ->
                val isDownloading = downloading.contains(song.id)
                val progress = progressMap[song.id] ?: if (downloaded.any { it.id == song.id }) 1.0f else 0.0f
                DownloadItem.SongItem(song, isDownloading, progress)
            }
            
        (collections + singles).sortedBy { 
            when(it) {
                is DownloadItem.CollectionItem -> it.title
                is DownloadItem.SongItem -> it.song.title
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
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

data class SongStatus(
    val song: Song,
    val isDownloading: Boolean,
    val progress: Float
)

sealed class DownloadItem {
    data class SongItem(
        val song: Song,
        val isDownloading: Boolean = false,
        val progress: Float = 1.0f
    ) : DownloadItem()
    
    data class CollectionItem(
        val id: String,
        val title: String,
        val thumbnailUrl: String?,
        val songs: List<SongStatus>
    ) : DownloadItem()
}

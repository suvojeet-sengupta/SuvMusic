package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumUiState(
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val selectedSongIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)

class AlbumViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val localAudioRepository: com.suvojeet.suvmusic.data.repository.LocalAudioRepository,
    private val musicPlayer: com.suvojeet.suvmusic.player.MusicPlayer,
    private val downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository,
    private val libraryRepository: LibraryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle[Destination.Album.ARG_ALBUM_ID])
    private val initialName: String? = savedStateHandle.get<String>(Destination.Album.ARG_NAME)?.let { 
        try { java.net.URLDecoder.decode(it, "UTF-8").takeIf { decoded -> decoded.isNotBlank() } } catch (e: Exception) { null }
    }
    private val initialThumbnail: String? = savedStateHandle.get<String>(Destination.Album.ARG_THUMBNAIL)?.let {
        try { java.net.URLDecoder.decode(it, "UTF-8").takeIf { decoded -> decoded.isNotBlank() } } catch (e: Exception) { null }
    }
    
    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()
    
    val batchProgress = downloadRepository.batchProgress
    val queueState = downloadRepository.queueState

    init {
        // Initial state from navigation args
        if (initialName != null || initialThumbnail != null) {
            _uiState.update {
                it.copy(
                    album = Album(
                        id = albumId,
                        title = initialName ?: "Loading...",
                        artist = "", // Artist unknown initially unless passed, but title is most important
                        thumbnailUrl = initialThumbnail
                    ),
                    isLoading = true
                )
            }
        }
        loadAlbum()
        checkLibraryStatus()
    }

    private fun checkLibraryStatus() {
        viewModelScope.launch {
            libraryRepository.isAlbumSaved(albumId).collect { isSaved ->
                _uiState.update { it.copy(isSaved = isSaved) }
            }
        }
    }

    fun toggleSaveToLibrary() {
        viewModelScope.launch {
            val album = _uiState.value.album ?: return@launch
            if (_uiState.value.isSaved) {
                libraryRepository.removeAlbum(album.id)
            } else {
                libraryRepository.saveAlbum(album)
            }
        }
    }

    private fun loadAlbum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Check if it's a local album (numeric ID)
                val isLocal = albumId.toLongOrNull() != null
                
                val album = if (isLocal) {
                    val id = albumId.toLong()
                    val localAlbums = localAudioRepository.getAllLocalAlbums()
                    val albumBase = localAlbums.find { it.id == albumId }
                    if (albumBase != null) {
                        val songs = localAudioRepository.getSongsByAlbum(id)
                        albumBase.copy(songs = songs)
                    } else null
                } else {
                    youTubeRepository.getAlbum(albumId)
                }
                
                // Merge with initial data if fetch failed partially or returns default
                val finalAlbum = if (album != null) {
                    album.copy(
                        title = if (album.title == "Unknown Album" && initialName != null) initialName else album.title,
                        thumbnailUrl = album.thumbnailUrl ?: initialThumbnail
                    )
                } else {
                     // If completely failed but we have initial data, keep showing that (though song list will be empty)
                     if (initialName != null) {
                         Album(id = albumId, title = initialName, artist = "", thumbnailUrl = initialThumbnail)
                     } else null
                }
                
                _uiState.update { 
                    it.copy(
                        album = finalAlbum,
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

    fun playNext(songs: List<Song>) {
        musicPlayer.playNext(songs)
    }

    fun addToQueue(songs: List<Song>) {
        musicPlayer.addToQueue(songs)
    }

    fun downloadAlbum(album: Album) {
        viewModelScope.launch {
            downloadRepository.downloadAlbum(album)
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            downloadRepository.downloadSong(song)
        }
    }

    fun toggleSongSelection(song: Song) {
        val currentSelected = _uiState.value.selectedSongIds.toMutableSet()
        if (currentSelected.contains(song.id)) {
            currentSelected.remove(song.id)
        } else {
            currentSelected.add(song.id)
        }
        _uiState.update { it.copy(
            selectedSongIds = currentSelected,
            isSelectionMode = currentSelected.isNotEmpty()
        ) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(
            selectedSongIds = emptySet(),
            isSelectionMode = false
        ) }
    }

    fun playNextSelectedSongs() {
        val selectedIds = _uiState.value.selectedSongIds
        val currentAlbum = _uiState.value.album ?: return
        val selectedSongs = currentAlbum.songs.filter { it.id in selectedIds }
        if (selectedSongs.isNotEmpty()) {
            musicPlayer.playNext(selectedSongs)
            clearSelection()
        }
    }

    fun addToQueueSelectedSongs() {
        val selectedIds = _uiState.value.selectedSongIds
        val currentAlbum = _uiState.value.album ?: return
        val selectedSongs = currentAlbum.songs.filter { it.id in selectedIds }
        if (selectedSongs.isNotEmpty()) {
            musicPlayer.addToQueue(selectedSongs)
            clearSelection()
        }
    }

    fun reorderSong(fromIndex: Int, toIndex: Int) {
        val currentAlbum = _uiState.value.album ?: return
        val songs = currentAlbum.songs.toMutableList()
        if (fromIndex !in songs.indices || toIndex !in songs.indices) return
        
        val movedSong = songs.removeAt(fromIndex)
        songs.add(toIndex, movedSong)
        
        _uiState.update { 
            it.copy(album = currentAlbum.copy(songs = songs))
        }
    }
}

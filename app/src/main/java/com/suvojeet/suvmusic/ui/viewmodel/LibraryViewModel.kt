package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.SuvMusicApplication
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.LibraryRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.player.MusicPlayer
import com.suvojeet.suvmusic.service.ImportStatus
import com.suvojeet.suvmusic.service.SpotifyImportService
import com.suvojeet.suvmusic.util.SpotifyImportHelper
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
    val libraryArtists: List<Artist> = emptyList(),
    val libraryAlbums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val importState: ImportState = ImportState.Idle,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isSyncingLikedSongs: Boolean = false
)

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Processing(
        val currentSong: String,
        val currentArtist: String,
        val thumbnail: String?,
        val currentIndex: Int,
        val totalSongs: Int,
        val successCount: Int,
        val status: String // "Searching...", "Adding...", "Failed"
    ) : ImportState()
    data class Success(val successCount: Int, val totalCount: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val localAudioRepository: LocalAudioRepository,
    private val downloadRepository: DownloadRepository,
    private val sessionManager: SessionManager,
    private val spotifyImportHelper: SpotifyImportHelper,
    private val libraryRepository: LibraryRepository,
    private val musicPlayer: MusicPlayer
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        _uiState.update { it.copy(isLoggedIn = sessionManager.isLoggedIn()) }
        loadData()
        observeDownloads()
        observeLibraryPlaylists()
        observeImportService()
    }

    private fun observeImportService() {
        viewModelScope.launch {
            SpotifyImportService.importState.collect { status ->
                val newState = when (status.state) {
                    ImportStatus.State.IDLE -> ImportState.Idle
                    ImportStatus.State.PREPARING -> ImportState.Loading
                    ImportStatus.State.PROCESSING -> ImportState.Processing(
                        currentSong = status.currentSong,
                        currentArtist = status.currentArtist,
                        thumbnail = status.thumbnail,
                        currentIndex = status.progress,
                        totalSongs = status.total,
                        successCount = status.successCount,
                        status = "Importing..."
                    )
                    ImportStatus.State.COMPLETED -> {
                        refresh()
                        ImportState.Success(status.successCount, status.total)
                    }
                    ImportStatus.State.ERROR -> ImportState.Error(status.error ?: "Unknown Error")
                    ImportStatus.State.CANCELLED -> ImportState.Error("Import Cancelled")
                }
                _uiState.update { it.copy(importState = newState) }
            }
        }
    }
    
    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.downloadedSongs.collect { songs ->
                _uiState.update { it.copy(downloadedSongs = songs) }
            }
        }
    }

    fun loadData(forceRefresh: Boolean = false, preloadedLikedSongs: List<Song>? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Load local audio
                val local = localAudioRepository.getAllLocalSongs()
                _uiState.update { it.copy(localSongs = local) }

                // Load liked songs (cached)
                val liked = preloadedLikedSongs ?: youTubeRepository.getLikedMusic(fetchAll = false)
                _uiState.update { it.copy(likedSongs = liked) }

                // Load library playlists
                if (sessionManager.isLoggedIn()) {
                    val playlists = youTubeRepository.getUserPlaylists()
                    _uiState.update { it.copy(playlists = playlists) }

                    val artists = youTubeRepository.getLibraryArtists()
                    _uiState.update { it.copy(libraryArtists = artists) }

                    val albums = youTubeRepository.getLibraryAlbums()
                    _uiState.update { it.copy(libraryAlbums = albums) }
                }

                if (forceRefresh && sessionManager.isLoggedIn()) {
                    refresh()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                var syncedSongs: List<Song>? = null
                if (sessionManager.isLoggedIn()) {
                    youTubeRepository.getUserPlaylists()
                    youTubeRepository.getLibraryArtists()
                    youTubeRepository.getLibraryAlbums()
                    syncedSongs = youTubeRepository.getLikedMusic(fetchAll = true)
                }
                loadData(forceRefresh = false, preloadedLikedSongs = syncedSongs)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Refresh failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }


    private fun observeLibraryPlaylists() {
        viewModelScope.launch {
            libraryRepository.getSavedPlaylists().collect { savedItems ->
                val displayItems = savedItems.map { entity ->
                    PlaylistDisplayItem(
                        id = entity.id,
                        name = entity.title,
                        url = "https://music.youtube.com/playlist?list=${entity.id}",
                        uploaderName = entity.subtitle ?: "",
                        thumbnailUrl = entity.thumbnailUrl,
                        songCount = 0
                    )
                }
                _uiState.update { state ->
                    val combined = (state.playlists + displayItems).distinctBy { it.id }
                    state.copy(playlists = combined)
                }
            }
        }
    }
    
    fun createPlaylist(title: String, description: String, isPrivate: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val privacyStatus = if (isPrivate) "PRIVATE" else "PUBLIC"
                val playlistId = youTubeRepository.createPlaylist(title, description, privacyStatus)
                if (playlistId != null) {
                    refresh()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create playlist: ${e.message}") }
            } finally {
                onComplete()
            }
        }
    }


    fun importSpotifyPlaylist(url: String) {
        val context = SuvMusicApplication.instance
        val intent = Intent(context, SpotifyImportService::class.java).apply {
            action = SpotifyImportService.ACTION_START
            putExtra(SpotifyImportService.EXTRA_URL, url)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun cancelImport() {
        val context = SuvMusicApplication.instance
        val intent = Intent(context, SpotifyImportService::class.java).apply {
            action = SpotifyImportService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    fun resetImportState() {
        cancelImport()
        _uiState.update { it.copy(importState = ImportState.Idle) }
    }

    fun downloadPlaylist(playlistItem: PlaylistDisplayItem) {
        viewModelScope.launch {
            try {
                val fullPlaylist = youTubeRepository.getPlaylist(playlistItem.id)
                downloadRepository.downloadPlaylist(fullPlaylist)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to download playlist: ${e.message}") }
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                val success = youTubeRepository.deletePlaylist(playlistId)
                if (success) {
                    refresh()
                    libraryRepository.removePlaylist(playlistId)
                }
            } catch (e: Exception) {
                 _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun shufflePlay(playlistId: String) {
        viewModelScope.launch {
            try {
                val playlist = youTubeRepository.getPlaylist(playlistId)
                if (playlist.songs.isNotEmpty()) {
                    val shuffled = playlist.songs.shuffled()
                    musicPlayer.playSong(shuffled.first(), shuffled, 0, true)
                }
            } catch (e: Exception) { }
        }
    }

    fun playNext(playlistId: String) {
         viewModelScope.launch {
            try {
                val playlist = youTubeRepository.getPlaylist(playlistId)
                musicPlayer.playNext(playlist.songs)
            } catch (e: Exception) { }
        }
    }

    fun addToQueue(playlistId: String) {
         viewModelScope.launch {
            try {
                val playlist = youTubeRepository.getPlaylist(playlistId)
                musicPlayer.addToQueue(playlist.songs)
            } catch (e: Exception) { }
        }
    }
}
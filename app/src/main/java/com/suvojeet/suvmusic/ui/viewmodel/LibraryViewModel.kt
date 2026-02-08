package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.SuvMusicApplication
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
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
    val isSyncingLikedSongs: Boolean = false,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val sortOption: LibrarySortOption = LibrarySortOption.DATE_ADDED,
    val selectedFilter: LibraryFilter = LibraryFilter.PLAYLISTS,
    val top50SongCount: Int = 0,
    val cachedSongCount: Int = 0
)

enum class LibraryViewMode {
    GRID, LIST
}

enum class LibrarySortOption {
    DATE_ADDED, NAME
}

enum class LibraryFilter(val title: String) {
    PLAYLISTS("Playlists"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists")
}

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
    private val musicPlayer: MusicPlayer,
    private val workManager: androidx.work.WorkManager,
    private val cache: androidx.media3.datasource.cache.Cache,
    private val listeningHistoryDao: com.suvojeet.suvmusic.data.local.dao.ListeningHistoryDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        _uiState.update { it.copy(isLoggedIn = sessionManager.isLoggedIn()) }
        loadData()
        observeDownloads()
        observeLibraryPlaylists()
        observeImportService()
        observeLikedSongs()
        schedulePeriodicSync()
    }

    private fun observeLikedSongs() {
        viewModelScope.launch {
            libraryRepository.getCachedPlaylistSongsFlow("LM").collect { songs ->
                _uiState.update { it.copy(likedSongs = songs) }
            }
        }
    }

    private fun schedulePeriodicSync() {
        if (!sessionManager.isLoggedIn()) return
        
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.suvojeet.suvmusic.data.worker.LikedSongsSyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES // Minimum interval
        )
            .setConstraints(
                androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "LikedSongsPeriodicSync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
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

                // Liked songs are now observed via Flow, no need to fetch here manually
                // But if it's the first run and empty, we might want to trigger a sync
                // observeLikedSongs takes care of updates.
                
                // If we have preloaded songs (e.g. from refresh), we don't need to do anything as flow updates
                // But for force refresh logic, we might need to trigger sync.

                // Load library playlists
                if (sessionManager.isLoggedIn()) {
                    val playlists = youTubeRepository.getUserPlaylists()
                    _uiState.update { it.copy(playlists = playlists) }

                    val artists = youTubeRepository.getLibraryArtists()
                    _uiState.update { it.copy(libraryArtists = artists) }

                    val albums = youTubeRepository.getLibraryAlbums()
                    _uiState.update { it.copy(libraryAlbums = albums) }

                    // Fetch Top 50 (RTM) Count
                    try {
                        val top50 = youTubeRepository.getPlaylist("RTM")
                        _uiState.update { it.copy(top50SongCount = top50.songs.size) }
                    } catch (e: Exception) {
                        // Ignore failure for optional smart playlist
                    }
                }

                loadCachedSongCount()

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
                if (sessionManager.isLoggedIn()) {
                    syncLikedSongs() // Trigger background sync
                    youTubeRepository.getUserPlaylists()
                    youTubeRepository.getLibraryArtists()
                    youTubeRepository.getLibraryAlbums()
                }
                loadCachedSongCount()
                loadData(forceRefresh = false)
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
    
    fun createPlaylist(title: String, description: String, isPrivate: Boolean, syncWithYt: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                if (syncWithYt && sessionManager.isLoggedIn()) {
                    val privacyStatus = if (isPrivate) "PRIVATE" else "PUBLIC"
                    val playlistId = youTubeRepository.createPlaylist(title, description, privacyStatus)
                    if (playlistId != null) {
                        refresh()
                    }
                } else {
                    // Create Local Playlist
                    val id = "local_" + java.util.UUID.randomUUID().toString()
                    val playlist = com.suvojeet.suvmusic.core.model.Playlist(
                        id = id, 
                        title = title, 
                        author = "You", 
                        thumbnailUrl = null, 
                        songs = emptyList()
                    )
                    libraryRepository.savePlaylist(playlist)
                    // Refresh local lists
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

    fun syncLikedSongs() {
        if (!sessionManager.isLoggedIn()) return
        
        _uiState.update { it.copy(isSyncingLikedSongs = true) }
        
        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.suvojeet.suvmusic.data.worker.LikedSongsSyncWorker>()
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "LikedSongsValidSync",
            androidx.work.ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        
        // We can listen to the work status if needed, but for now we just rely on Flow updates
        // To update the spinner, we could observe the WorkInfo, but for simplicity
        // in this step, let's just reset the spinner after a delay or separate observer.
        // Actually, let's keep it simple: UI already observes Flow. 
        // We'll set isSyncing to false after a short delay since the real work is background.
        // Or better, observe the work status.
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(syncRequest.id).collect { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                     _uiState.update { it.copy(isSyncingLikedSongs = false) }
                }
            }
        }
    }

    fun playOfflineShuffle() {
        viewModelScope.launch {
            try {
                val downloaded = _uiState.value.downloadedSongs
                val local = _uiState.value.localSongs
                val cached = getCachedSongs()
                
                val allOffline = (downloaded + local + cached).distinctBy { it.id }
                if (allOffline.isNotEmpty()) {
                    val shuffled = allOffline.shuffled()
                    musicPlayer.playSong(shuffled.first(), shuffled, 0, true)
                }
            } catch (e: Exception) { }
        }
    }

    private suspend fun getCachedSongs(): List<Song> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val allKeys = cache.keys
        val allHistory = listeningHistoryDao.getAllHistory()
        val historyMap = allHistory.associateBy { it.songId }
        
        val cachedSongs = mutableListOf<Song>()
        
        for (key in allKeys) {
            val songId = key.removePrefix("audio_").removePrefix("video_").substringBefore("_")
            val history = historyMap[songId]
            if (history != null) {
                cachedSongs.add(Song(
                    id = history.songId,
                    title = history.songTitle,
                    artist = history.artist,
                    album = history.album ?: "",
                    duration = history.duration,
                    thumbnailUrl = history.thumbnailUrl,
                    source = try { 
                        com.suvojeet.suvmusic.core.model.SongSource.valueOf(history.source) 
                    } catch (e: Exception) { 
                        com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE 
                    },
                    localUri = history.localUri?.let { android.net.Uri.parse(it) },
                    artistId = history.artistId
                ))
            }
        }
        cachedSongs
    }

    fun setViewMode(mode: LibraryViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setSortOption(option: LibrarySortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun setFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    private suspend fun loadCachedSongCount() {
        val count = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val allKeys = cache.keys
            val allHistory = listeningHistoryDao.getAllHistory()
            val historyIds = allHistory.map { it.songId }.toSet()
            
            val cachedIds = mutableSetOf<String>()
            
            for (key in allKeys) {
                val songId = key.removePrefix("audio_").removePrefix("video_").substringBefore("_")
                if (historyIds.contains(songId)) {
                    cachedIds.add(songId)
                }
            }
            
            // Also include downloaded songs
            val downloadedIds = downloadRepository.downloadedSongs.value.map { it.id }
            cachedIds.addAll(downloadedIds)
            
            cachedIds.size
        }
        _uiState.update { it.copy(cachedSongCount = count) }
    }
}
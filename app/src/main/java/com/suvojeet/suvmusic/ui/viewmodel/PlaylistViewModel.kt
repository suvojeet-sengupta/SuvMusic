package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditable: Boolean = false,
    val isRenaming: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isSaved: Boolean = false,
    val userPlaylists: List<com.suvojeet.suvmusic.core.model.PlaylistDisplayItem> = emptyList(),
    val isLoadingPlaylists: Boolean = false,
    val showAddToPlaylistSheet: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val isCreatingPlaylist: Boolean = false,
    val selectedSong: Song? = null
) {
    val isUserPlaylist: Boolean
        get() = isEditable // Alias for clarity
}

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: com.suvojeet.suvmusic.data.repository.JioSaavnRepository,
    private val sessionManager: com.suvojeet.suvmusic.data.SessionManager,
    private val musicPlayer: com.suvojeet.suvmusic.player.MusicPlayer,
    private val downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository,
    private val localAudioRepository: com.suvojeet.suvmusic.data.repository.LocalAudioRepository,
    private val libraryRepository: LibraryRepository,
    private val cache: androidx.media3.datasource.cache.Cache,
    private val listeningHistoryDao: com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle[Destination.Playlist.ARG_PLAYLIST_ID])
    private val initialName: String? = savedStateHandle.get<String>(Destination.Playlist.ARG_NAME)?.let { 
        try { URLDecoder.decode(it, "UTF-8").takeIf { decoded -> decoded.isNotBlank() } } catch (e: Exception) { null }
    }
    private val initialThumbnail: String? = savedStateHandle.get<String>(Destination.Playlist.ARG_THUMBNAIL)?.let {
        try { URLDecoder.decode(it, "UTF-8").takeIf { decoded -> decoded.isNotBlank() } } catch (e: Exception) { null }
    }
    
    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()
    
    val batchProgress = downloadRepository.batchProgress

    init {
        // Set initial login state
        _uiState.update { it.copy(isLoggedIn = sessionManager.isLoggedIn()) }
        
        // Show initial data from navigation immediately
        if (initialName != null || initialThumbnail != null) {
            _uiState.update {
                it.copy(
                    playlist = Playlist(
                        id = playlistId,
                        title = initialName ?: "Loading...",
                        author = "",
                        thumbnailUrl = initialThumbnail,
                        songs = emptyList()
                    ),
                    isLoading = true
                )
            }
        }
        loadPlaylist()
        checkLibraryStatus()
    }

    private fun checkLibraryStatus() {
        viewModelScope.launch {
            libraryRepository.isPlaylistSaved(playlistId).collect { isSaved ->
                _uiState.update { it.copy(isSaved = isSaved) }
            }
        }
    }

    fun toggleSaveToLibrary() {
        viewModelScope.launch {
            val playlist = _uiState.value.playlist ?: return@launch
            val isCurrentlySaved = _uiState.value.isSaved
            
            // Sync with YouTube if logged in
            if (sessionManager.isLoggedIn()) {
                val rating = if (isCurrentlySaved) "INDIFFERENT" else "LIKE"
                launch {
                    youTubeRepository.ratePlaylist(playlist.id, rating)
                }
            }

            if (isCurrentlySaved) {
                libraryRepository.removePlaylist(playlist.id)
            } else {
                libraryRepository.savePlaylist(playlist)
            }
        }
    }

    private fun loadPlaylist() {
        checkEditable()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentSource = sessionManager.getMusicSource()
                
                // Smart loading based on source preference
                // Smart loading based on source preference
                // Smart loading based on source preference
                val playlist = if (playlistId == "LM") {
                    // Liked Songs - Load from local library
                    val songs = libraryRepository.getCachedPlaylistSongs("LM")
                    Playlist(
                        id = "LM",
                        title = "Liked",
                        author = "You",
                        thumbnailUrl = initialThumbnail ?: songs.firstOrNull()?.thumbnailUrl,
                        songs = songs
                    )
                } else if (playlistId == "CACHED_ALL") {
                     // Cached Songs - Now including ALL cached items from player cache
                     val songs = loadAllCachedSongs()
                     Playlist(
                         id = "CACHED_ALL",
                         title = "Cached Songs",
                         author = "Local Device",
                         thumbnailUrl = initialThumbnail ?: songs.firstOrNull()?.thumbnailUrl,
                         songs = songs
                     )
                } else if (playlistId == "TOP_50") {
                     // My Top 50 (Mapped to Supermix RTM)
                     val supermix = try {
                         youTubeRepository.getPlaylist("RTM")
                     } catch (e: Exception) {
                         Playlist("TOP_50", "My Top 50", "YouTube Music", null, emptyList())
                     }
                     supermix.copy(
                         id = "TOP_50",
                         title = "My Top 50", // Override title
                         author = "You"
                     )
                } else if (playlistId == "DEVICE_SONGS") {
                     // Device Local Songs
                     val songs = localAudioRepository.getAllLocalSongs()
                     Playlist(
                         id = "DEVICE_SONGS",
                         title = "Device files",
                         author = "Local Storage",
                         thumbnailUrl = null,
                         songs = songs
                     )
                } else if (currentSource == com.suvojeet.suvmusic.data.MusicSource.JIOSAAVN) {
                    // In HQ Audio mode, prioritize JioSaavn
                    val jioPlaylist = jioSaavnRepository.getPlaylist(playlistId)
                    if (jioPlaylist != null) {
                        jioPlaylist
                    } else {
                        // Fallback to YouTube if not found in JioSaavn (e.g. user clicked a YT playlist)
                        youTubeRepository.getPlaylist(playlistId)
                    }
                } else {
                    // In YouTube mode, prioritize YouTube
                    try {
                        youTubeRepository.getPlaylist(playlistId)
                    } catch (e: Exception) {
                        // Fallback to JioSaavn if not found in YouTube (e.g. user clicked a Jio playlist)
                        jioSaavnRepository.getPlaylist(playlistId) ?: throw e
                    }
                }
                
                // Merge with initial data:
                // - Prefer navigation thumbnail (it's the correct playlist art from Home screen)
                // - Fallback to API data only if nav data is missing
                val finalPlaylist = playlist.copy(
                    title = if (playlist.title == "Unknown Playlist" && initialName != null) initialName else playlist.title,
                    thumbnailUrl = initialThumbnail ?: playlist.thumbnailUrl,
                    author = playlist.author.takeIf { it.isNotBlank() } ?: ""
                )
                
                _uiState.update { 
                    it.copy(
                        playlist = finalPlaylist,
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


    private fun checkEditable() {
        viewModelScope.launch {
            try {
                val userPlaylists = youTubeRepository.getUserEditablePlaylists()
                val isEditable = userPlaylists.any { it.id == playlistId } || playlistId.startsWith("PL") 
                
                _uiState.update { it.copy(isEditable = isEditable || playlistId == "LM") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEditable = false) }
            }
        }
    }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRenaming = true) }
            val success = youTubeRepository.renamePlaylist(playlistId, newName)
            if (success) {
                // Refresh
                loadPlaylist()
            }
            _uiState.update { it.copy(isRenaming = false) }
        }
    }
    
    fun createPlaylist(title: String, description: String, isPrivate: Boolean, syncWithYt: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingPlaylist = true) }
            try {
                if (syncWithYt && sessionManager.isLoggedIn()) {
                    val privacyStatus = if (isPrivate) "PRIVATE" else "PUBLIC"
                    youTubeRepository.createPlaylist(title, description, privacyStatus)
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
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create playlist: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isCreatingPlaylist = false) }
                hideCreatePlaylistDialog()
            }
        }
    }

    fun reorderSong(fromIndex: Int, toIndex: Int) {
        val currentPlaylist = _uiState.value.playlist ?: return
        val songs = currentPlaylist.songs.toMutableList()
        
        // Optimistic update
        val movedSong = songs.removeAt(fromIndex)
        songs.add(toIndex, movedSong)
        
        _uiState.update { 
            it.copy(playlist = currentPlaylist.copy(songs = songs))
        }
        
        viewModelScope.launch {
            val setVideoId = movedSong.setVideoId
            
            if (setVideoId != null) {
                // Determine predecessor
                // if toIndex == 0, predecessor is null (move to top)
                // else predecessor is the song at toIndex - 1 (in the NEW list)
                val predecessorId = if (toIndex > 0) songs[toIndex - 1].setVideoId else null
                
                youTubeRepository.moveSongInPlaylist(playlistId, setVideoId, predecessorId)
            } else {
                // Revert if we can't move (no setVideoId)
                loadPlaylist()
            }
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val success = youTubeRepository.deletePlaylist(playlistId)
            _uiState.update { it.copy(isDeleting = false, deleteSuccess = success) }
        }
    }

    fun playNext(songs: List<Song>) {
        musicPlayer.playNext(songs)
    }

    fun addToQueue(songs: List<Song>) {
        musicPlayer.addToQueue(songs)
    }

    fun playNext(song: Song) {
        musicPlayer.playNext(listOf(song))
    }

    fun addToQueue(song: Song) {
        musicPlayer.addToQueue(listOf(song))
    }

    fun downloadPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            downloadRepository.downloadPlaylist(playlist)
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            downloadRepository.downloadSong(song)
        }
    }
    
    fun addToPlaylist(song: Song) {
        showAddToPlaylistSheet(song)
    }

    // Add to Playlist management
    fun showAddToPlaylistSheet(song: Song) {
        _uiState.update { 
            it.copy(
                showAddToPlaylistSheet = true,
                selectedSong = song
            )
        }
        loadUserPlaylists()
    }

    fun hideAddToPlaylistSheet() {
        _uiState.update { 
            it.copy(
                showAddToPlaylistSheet = false,
                selectedSong = null
            )
        }
    }

    private fun loadUserPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlaylists = true) }
            val playlists = youTubeRepository.getUserEditablePlaylists()
            _uiState.update { 
                it.copy(
                    userPlaylists = playlists,
                    isLoadingPlaylists = false
                )
            }
        }
    }

    fun addSongToPlaylist(playlistId: String) {
        val song = _uiState.value.selectedSong ?: return
        viewModelScope.launch {
            youTubeRepository.addSongToPlaylist(playlistId, song.id)
            hideAddToPlaylistSheet()
        }
    }

    fun showCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = true) }
    }

    fun hideCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = false) }
    }

    private suspend fun loadAllCachedSongs(): List<Song> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val allKeys = cache.keys
        val allHistory = listeningHistoryDao.getAllHistory()
        val cachedSongs = mutableListOf<Song>()

        // Map history by song ID for fast lookup
        val historyMap = allHistory.associateBy { it.songId }

        for (key in allKeys) {
            // Keys can be direct songId or "audio_songId" or "video_songId"
            val songId = key.removePrefix("audio_").removePrefix("video_").substringBefore("_")
            
            val history = historyMap[songId]
            if (history != null) {
                // Verify if the resource is actually fully or substantially cached
                // For simplicity, we assume if the key exists, it's partially cached and playable
                val song = Song(
                    id = history.songId,
                    title = history.songTitle,
                    artist = history.artist,
                    album = history.album,
                    duration = history.duration,
                    thumbnailUrl = history.thumbnailUrl,
                    source = try { 
                        com.suvojeet.suvmusic.core.model.SongSource.valueOf(history.source) 
                    } catch (e: Exception) { 
                        com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE 
                    },
                    localUri = history.localUri?.let { android.net.Uri.parse(it) },
                    artistId = history.artistId
                )
                if (cachedSongs.none { it.id == song.id }) {
                    cachedSongs.add(song)
                }
            }
        }
        
        // Also include downloaded songs which might not be in player cache but are local
        val downloaded = downloadRepository.downloadedSongs.value
        downloaded.forEach { song ->
            if (cachedSongs.none { it.id == song.id }) {
                cachedSongs.add(song)
            }
        }

        cachedSongs.sortedByDescending { historyMap[it.id]?.lastPlayed ?: 0L }
    }
}


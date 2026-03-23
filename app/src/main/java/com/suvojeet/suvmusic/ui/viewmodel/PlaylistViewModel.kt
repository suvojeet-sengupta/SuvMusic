package com.suvojeet.suvmusic.ui.viewmodel

import android.widget.Toast
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SortOrder
import com.suvojeet.suvmusic.core.model.SortType
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
import java.util.UUID
import javax.inject.Inject

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val originalSongs: List<Song> = emptyList(),
    val sortType: SortType = SortType.CUSTOM,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
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
    val selectedSong: Song? = null,
    val selectedSongIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
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
        viewModelScope.launch {
            // Load saved sort settings
            val sortTypeStr = sessionManager.getPlaylistSortType()
            val sortOrderAsc = sessionManager.getPlaylistSortOrder()
            val savedSortType = try { SortType.valueOf(sortTypeStr) } catch (e: Exception) { SortType.CUSTOM }
            val savedSortOrder = if (sortOrderAsc) SortOrder.ASCENDING else SortOrder.DESCENDING
            
            _uiState.update { it.copy(
                isLoggedIn = sessionManager.isLoggedIn(),
                sortType = savedSortType,
                sortOrder = savedSortOrder
            ) }

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
            observePlaylistChanges()
        }
        checkLibraryStatus()
    }

    private fun observePlaylistChanges() {
        if (playlistId.startsWith("local_") || playlistId == "LM" || playlistId == "CACHED_ALL") {
            viewModelScope.launch {
                libraryRepository.getCachedPlaylistSongsFlow(playlistId).collect { songs ->
                    _uiState.update { state ->
                        val updatedPlaylist = state.playlist?.copy(songs = songs)
                        state.copy(
                            playlist = updatedPlaylist,
                            originalSongs = songs
                        )
                    }
                    applySort()
                }
            }
        }
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

    /**
     * Public refresh method for pull-to-refresh and post-modification reloads.
     */
    fun refreshPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                loadPlaylistInternal()
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun loadPlaylist() {
        checkEditable()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadPlaylistInternal()
        }
    }

    private suspend fun loadPlaylistInternal() {
        // Fast Load Strategy: Show local version from database first (if it exists)
        val localVersion = if (playlistId != "CACHED_ALL" && playlistId != "DEVICE_SONGS" && playlistId != "TOP_50") {
             try {
                 val item = libraryRepository.getPlaylistById(playlistId)
                 val songs = libraryRepository.getCachedPlaylistSongs(playlistId)
                 if (songs.isNotEmpty() || item != null) {
                    Playlist(
                        id = playlistId,
                        title = item?.title ?: (if (playlistId == "LM") "Liked" else "Local Playlist"),
                        author = item?.subtitle ?: (if (playlistId == "LM") "You" else "You"),
                        thumbnailUrl = initialThumbnail ?: item?.thumbnailUrl ?: songs.firstOrNull()?.thumbnailUrl,
                        songs = songs
                    )
                 } else {
                     null
                 }
             } catch (e: Exception) { null }
        } else null

        if (localVersion != null) {
            _uiState.update { 
                it.copy(
                    playlist = localVersion,
                    originalSongs = localVersion.songs,
                    isLoading = false // Hide loader since we have some content
                )
            }
            applySort()
        }

        try {
            val currentSource = sessionManager.getMusicSource()
            
            // Smart loading based on source preference
            val playlist = if (playlistId == "LM") {
                // Liked Songs - Load from local library (already handled above but for consistency)
                val songs = libraryRepository.getCachedPlaylistSongs("LM")
                Playlist(
                    id = "LM",
                    title = "Liked",
                    author = "You",
                    thumbnailUrl = initialThumbnail ?: songs.firstOrNull()?.thumbnailUrl,
                    songs = songs
                )
            } else if (localVersion != null) {
                // We already have a local version, which means this is an imported or local playlist.
                // Re-fetch songs to ensure they are up to date, but keep it as a local playlist.
                val songs = libraryRepository.getCachedPlaylistSongs(playlistId)
                localVersion.copy(songs = songs)
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
            
            // Merge with initial data
            val finalPlaylist = playlist.copy(
                title = if (playlist.title == "Unknown Playlist" && initialName != null) initialName else playlist.title,
                thumbnailUrl = initialThumbnail ?: playlist.thumbnailUrl,
                author = playlist.author.takeIf { it.isNotBlank() } ?: ""
            )
            
            _uiState.update { 
                it.copy(
                    playlist = finalPlaylist,
                    originalSongs = finalPlaylist.songs,
                    isLoading = false
                )
            }
            // Re-apply current sort if any
            applySort()
        } catch (e: Exception) {
            // Only show error if we don't even have cached content
            if (_uiState.value.playlist == null || _uiState.value.playlist?.songs?.isEmpty() == true) {
                _uiState.update { 
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setSort(sortType: SortType) {
        _uiState.update { it.copy(sortType = sortType) }
        viewModelScope.launch {
            sessionManager.setPlaylistSortType(sortType.name)
        }
        applySort()
    }

    fun toggleSortOrder() {
        val nextOrder = if (_uiState.value.sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
        _uiState.update { it.copy(sortOrder = nextOrder) }
        viewModelScope.launch {
            sessionManager.setPlaylistSortOrder(nextOrder == SortOrder.ASCENDING)
        }
        applySort()
    }

    private fun applySort() {
        val state = _uiState.value
        val currentPlaylist = state.playlist ?: return
        val originalSongs = state.originalSongs
        
        if (state.sortType == SortType.CUSTOM) {
            _uiState.update { 
                it.copy(playlist = currentPlaylist.copy(songs = originalSongs))
            }
            return
        }

        val sortedSongs = when (state.sortType) {
            SortType.TRACK_NAME -> originalSongs.sortedBy { it.title.lowercase() }
            SortType.ARTIST_NAME -> originalSongs.sortedBy { it.artist.lowercase() }
            SortType.ALBUM_NAME -> originalSongs.sortedBy { it.album.lowercase() }
            SortType.PLAY_TIME -> originalSongs.sortedBy { it.duration }
            SortType.DATE_ADDED -> originalSongs.sortedBy { it.addedAt } 
            else -> originalSongs
        }

        val finalSongs = if (state.sortOrder == SortOrder.ASCENDING) sortedSongs else sortedSongs.reversed()

        _uiState.update { 
            it.copy(playlist = currentPlaylist.copy(songs = finalSongs))
        }
    }


    private fun checkEditable() {
        viewModelScope.launch {
            try {
                val isLocal = libraryRepository.getPlaylistById(playlistId) != null
                val userPlaylists = youTubeRepository.getUserEditablePlaylists()
                val isYtEditable = userPlaylists.any { it.id == playlistId }
                
                _uiState.update { it.copy(isEditable = isLocal || isYtEditable || playlistId == "LM") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEditable = false) }
            }
        }
    }

    fun createPlaylist(title: String, description: String, isPrivate: Boolean, syncWithYt: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingPlaylist = true) }
            
            val song = _uiState.value.selectedSong
            try {
                val playlistId = if (syncWithYt && sessionManager.isLoggedIn()) {
                    val privacyStatus = if (isPrivate) "PRIVATE" else "PUBLIC"
                    youTubeRepository.createPlaylist(title, description, privacyStatus)
                } else {
                    // Create Local Playlist
                    val id = "local_" + UUID.randomUUID().toString()
                    val playlist = Playlist(
                        id = id, 
                        title = title, 
                        author = "You", 
                        thumbnailUrl = song?.thumbnailUrl, 
                        songs = emptyList()
                    )
                    libraryRepository.savePlaylist(playlist)
                    id
                }

                if (playlistId != null && song != null) {
                    if (syncWithYt && sessionManager.isLoggedIn()) {
                        youTubeRepository.addSongToPlaylist(playlistId, song.id)
                    } else {
                        libraryRepository.addSongToPlaylist(playlistId, song)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create playlist: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isCreatingPlaylist = false) }
                hideCreatePlaylistDialog()
            }
        }
    }

    fun renamePlaylist(newName: String) {
        if (newName.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isRenaming = true) }
            
            val isLocal = libraryRepository.getPlaylistById(playlistId) != null
            val success = if (isLocal) {
                try {
                    libraryRepository.updatePlaylistName(playlistId, newName)
                    true
                } catch (e: Exception) {
                    false
                }
            } else {
                youTubeRepository.renamePlaylist(playlistId, newName)
            }
            
            if (success) {
                // Refresh data to show new name
                loadPlaylist()
            } else {
                _uiState.update { it.copy(error = "Failed to rename playlist") }
            }
            _uiState.update { it.copy(isRenaming = false) }
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
            val isLocal = libraryRepository.getPlaylistById(playlistId) != null || playlistId == "LM"
            if (isLocal) {
                // Local reorder - just replace the whole list in DB
                libraryRepository.savePlaylistSongs(playlistId, songs)
                return@launch
            }

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
            
            val isLocal = libraryRepository.getPlaylistById(playlistId) != null || playlistId == "LM"
            val success = if (isLocal) {
                try {
                    libraryRepository.removePlaylist(playlistId)
                    true
                } catch (e: Exception) {
                    false
                }
            } else {
                val ytSuccess = youTubeRepository.deletePlaylist(playlistId)
                if (ytSuccess) {
                    libraryRepository.removePlaylist(playlistId)
                }
                ytSuccess
            }
            
            _uiState.update { 
                it.copy(
                    isDeleting = false, 
                    deleteSuccess = success,
                    error = if (!success) "Failed to delete playlist" else null
                ) 
            }
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
    
    fun exportPlaylistToM3U(context: android.content.Context) {
        val playlist = _uiState.value.playlist ?: return
        val songs = playlist.songs
        if (songs.isEmpty()) return

        viewModelScope.launch {
            try {
                val m3uContent = StringBuilder("#EXTM3U\n")
                for (song in songs) {
                    m3uContent.append("#EXTINF:${song.duration / 1000},${song.artist} - ${song.title}\n")
                    // For YouTube songs, use the URL. For local songs, use the URI.
                    val url = if (song.localUri != null) {
                        song.localUri.toString()
                    } else {
                        "https://www.youtube.com/watch?v=${song.id}"
                    }
                    m3uContent.append("$url\n")
                }

                val safeTitle = playlist.title.replace(Regex("[^a-zA-Z0-9]"), "_")
                val fileName = "$safeTitle.m3u"
                
                val playlistsDir = java.io.File(context.cacheDir, "playlists")
                if (!playlistsDir.exists()) playlistsDir.mkdirs()
                
                val tempFile = java.io.File(playlistsDir, fileName)
                tempFile.writeText(m3uContent.toString())
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    tempFile
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/x-mpegurl"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(intent, "Export Playlist")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to export: ${e.localizedMessage}") }
            }
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

    fun addSongToPlaylist(targetPlaylistId: String) {
        val song = _uiState.value.selectedSong ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlaylists = true) }
            val isLocal = targetPlaylistId.startsWith("local_") || targetPlaylistId == "LM"
            var success = false
            var message: String? = null

            if (isLocal) {
                try {
                    if (libraryRepository.isSongInPlaylist(targetPlaylistId, song.id)) {
                        success = false
                        message = "${song.title} is already in this playlist"
                    } else {
                        libraryRepository.addSongToPlaylist(targetPlaylistId, song)
                        success = true
                        message = "Added ${song.title} to playlist"
                    }
                } catch (e: Exception) {
                    success = false
                    message = "Failed to add ${song.title}"
                }
            } else {
                success = youTubeRepository.addSongToPlaylist(targetPlaylistId, song.id)
                message = if (success) "Added ${song.title} to playlist" else "Failed to add to YouTube playlist"
            }
            
            _uiState.update { 
                it.copy(
                    isLoadingPlaylists = false,
                    showAddToPlaylistSheet = false,
                    selectedSong = null,
                    successMessage = if (success) message else null,
                    errorMessage = if (!success) message else null
                )
            }
            
            // Reload if the song was added to the currently viewed playlist
            if (success && targetPlaylistId == playlistId) {
                refreshPlaylist()
            }
        }
    }

    fun showCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = true) }
    }

    fun hideCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = false) }
    }

    fun removeSongFromPlaylist(song: Song) {
        viewModelScope.launch {
            val isLocal = playlistId.startsWith("local_") || playlistId == "LM"
            var success = false
            var message: String? = null

            if (isLocal) {
                try {
                    libraryRepository.removeSongFromPlaylist(playlistId, song.id)
                    success = true
                    message = "Removed ${song.title} from playlist"
                } catch (e: Exception) {
                    success = false
                    message = "Failed to remove ${song.title}"
                }
            } else {
                val setVideoId = song.setVideoId
                if (setVideoId != null) {
                    success = youTubeRepository.removeSongFromPlaylist(playlistId, setVideoId)
                    message = if (success) "Removed ${song.title} from playlist" else "Failed to remove from YouTube playlist"
                } else {
                    success = false
                    message = "Cannot remove this song from YouTube playlist"
                }
            }
            
            if (success) {
                refreshPlaylist()
            }

            _uiState.update { 
                it.copy(
                    successMessage = if (success) message else null,
                    errorMessage = if (!success) message else null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun toggleSongSelection(song: Song) {
        val id = song.setVideoId ?: song.id
        val currentSelected = _uiState.value.selectedSongIds.toMutableSet()
        
        if (currentSelected.contains(id)) {
            currentSelected.remove(id)
        } else {
            currentSelected.add(id)
        }
        
        _uiState.update { 
            it.copy(
                selectedSongIds = currentSelected,
                isSelectionMode = currentSelected.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { 
            it.copy(
                selectedSongIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun moveSelectedSongs(toIndex: Int) {
        val selectedIds = _uiState.value.selectedSongIds
        if (selectedIds.isEmpty()) return
        
        val currentPlaylist = _uiState.value.playlist ?: return
        val songs = currentPlaylist.songs.toMutableList()
        
        // Find songs to move
        val songsToMove = songs.filter { (it.setVideoId ?: it.id) in selectedIds }
        
        // Remove them from current positions
        songs.removeAll(songsToMove)
        
        // Insert at target index
        val safeToIndex = toIndex.coerceIn(0, songs.size)
        songs.addAll(safeToIndex, songsToMove)
        
        _uiState.update { 
            it.copy(
                playlist = currentPlaylist.copy(songs = songs),
                originalSongs = songs // Update original as well for CUSTOM sort
            )
        }
        
        // If it's a remote playlist, we might need multiple API calls or a batch reorder
        // For now, let's at least update local state and clear selection
        viewModelScope.launch {
            // Note: Implementing batch move for YouTube might be complex, 
            // but for local playlists it's straightforward (already updated originalSongs)
            if (playlistId.startsWith("local_") || playlistId == "LM") {
                libraryRepository.replacePlaylistSongs(playlistId, songs)
            } else {
                // For YT, we might need to sync the whole order or use move multiple times
                // Simplest is to reload for now to ensure consistency with server
                // loadPlaylist() 
            }
        }
        clearSelection()
    }

    fun removeSelectedSongs() {
        val selectedIds = _uiState.value.selectedSongIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            val isLocal = playlistId.startsWith("local_") || playlistId == "LM"
            val success = if (isLocal) {
                try {
                    selectedIds.forEach { songId ->
                        libraryRepository.removeSongFromPlaylist(playlistId, songId)
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            } else {
                youTubeRepository.removeSongsFromPlaylist(playlistId, selectedIds.toList())
            }
            
            if (success) {
                clearSelection()
                refreshPlaylist()
            }
        }
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


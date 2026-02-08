package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.Comment
import com.suvojeet.suvmusic.data.model.DownloadState
import com.suvojeet.suvmusic.data.model.OutputDevice
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.model.VideoQuality
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.LibraryRepository
import com.suvojeet.suvmusic.data.repository.LyricsRepository
import com.suvojeet.suvmusic.data.repository.SponsorBlockRepository
import com.suvojeet.suvmusic.data.repository.SponsorSegment
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.player.MusicPlayer
import com.suvojeet.suvmusic.player.SleepTimerManager
import com.suvojeet.suvmusic.player.SleepTimerOption
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType
import com.suvojeet.suvmusic.recommendation.RecommendationEngine
import com.suvojeet.suvmusic.service.DownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject

import com.suvojeet.suvmusic.discord.DiscordManager

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val downloadRepository: DownloadRepository,
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val libraryRepository: LibraryRepository,
    private val lyricsRepository: LyricsRepository,
    private val sleepTimerManager: SleepTimerManager,
    private val sessionManager: SessionManager,
    private val recommendationEngine: RecommendationEngine,
    private val sponsorBlockRepository: SponsorBlockRepository,
    private val discordManager: DiscordManager,
    private val audioARManager: com.suvojeet.suvmusic.player.AudioARManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    val playerState: StateFlow<PlayerState> = musicPlayer.playerState
    
    // Stable player state that ignores frequent progress updates for UI optimization
    val playbackInfo = musicPlayer.playerState.map { state ->
        // Return a copy with progress fields reset to avoid triggering changes
        state.copy(currentPosition = 0L, duration = 0L, bufferedPercentage = 0)
    }.distinctUntilChanged()
    
    private val _lyricsState = MutableStateFlow<Lyrics?>(null)
    val lyricsState: StateFlow<Lyrics?> = _lyricsState.asStateFlow()
    
    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()
    
    // Lyrics Provider Selection
    private val _selectedLyricsProvider = MutableStateFlow(LyricsProviderType.AUTO)
    val selectedLyricsProvider: StateFlow<LyricsProviderType> = _selectedLyricsProvider.asStateFlow()

    // Dynamic Lyrics Providers State
    private val _enabledLyricsProviders = MutableStateFlow<Map<LyricsProviderType, Boolean>>(
        mapOf(
            LyricsProviderType.AUTO to true,
            LyricsProviderType.BETTER_LYRICS to true,
            LyricsProviderType.SIMP_MUSIC to true,
            LyricsProviderType.LRCLIB to true,
            LyricsProviderType.JIOSAAVN to true,
            LyricsProviderType.YOUTUBE to true
        )
    )
    val enabledLyricsProviders: StateFlow<Map<LyricsProviderType, Boolean>> = _enabledLyricsProviders.asStateFlow()

    private val _commentsState = MutableStateFlow<List<Comment>?>(null)
    val commentsState: StateFlow<List<Comment>?> = _commentsState.asStateFlow()

    private val _isFetchingComments = MutableStateFlow(false)
    val isFetchingComments: StateFlow<Boolean> = _isFetchingComments.asStateFlow()

    private val _isLoadingMoreComments = MutableStateFlow(false)
    val isLoadingMoreComments: StateFlow<Boolean> = _isLoadingMoreComments.asStateFlow()
    
    private val _isPostingComment = MutableStateFlow(false)
    val isPostingComment: StateFlow<Boolean> = _isPostingComment.asStateFlow()
    
    private val _commentPostSuccess = MutableStateFlow<Boolean?>(null)
    val commentPostSuccess: StateFlow<Boolean?> = _commentPostSuccess.asStateFlow()

    val sponsorSegments: StateFlow<List<SponsorSegment>> = sponsorBlockRepository.currentSegments
    
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
    
    // Sleep Timer
    val sleepTimerOption: StateFlow<SleepTimerOption> = sleepTimerManager.currentOption
    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimerManager.remainingTimeMs
    
    fun setSleepTimer(option: SleepTimerOption, customMinutes: Int? = null) {
        sleepTimerManager.startTimer(option, customMinutes)
    }
    
    // Radio Mode State
    private val _isRadioMode = MutableStateFlow(false)
    val isRadioMode: StateFlow<Boolean> = _isRadioMode.asStateFlow()
    
    private val _isLoadingMoreSongs = MutableStateFlow(false)
    val isLoadingMoreSongs: StateFlow<Boolean> = _isLoadingMoreSongs.asStateFlow()
    
    // MiniPlayer Visibility State
    private val _isMiniPlayerDismissed = MutableStateFlow(false)
    val isMiniPlayerDismissed: StateFlow<Boolean> = _isMiniPlayerDismissed.asStateFlow()

    // Fullscreen State
    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()
    
    private var radioBaseSongId: String? = null

    // History Sync State
    private var lastSyncedVideoId: String? = null
    private var currentSongPlayTime: Long = 0
    private var isHistorySyncEnabled = false
    private val HISTORY_SYNC_THRESHOLD_MS = 30000L // 30 seconds
    
    init {
        observeCurrentSong()
        observeDownloads()
        observeDownloadStateConsistency()
        observeQueuePositionForAutoplay()
        observeLyricsProviderSettings()
        
        // Observe history sync setting
        viewModelScope.launch {
            sessionManager.youtubeHistorySyncEnabledFlow.collect { enabled ->
                isHistorySyncEnabled = enabled
            }
        }

        // Ticker loop for tracking playback time
        viewModelScope.launch {
            while(true) {
                delay(1000)
                if (playerState.value.isPlaying) {
                    currentSongPlayTime += 1000
                    checkAndSyncHistory()
                }
            }
        }

        // Initialize Discord Manager with saved settings
        viewModelScope.launch {
            val token = sessionManager.getDiscordToken()
            val enabled = sessionManager.isDiscordRpcEnabled()
            val useDetails = sessionManager.isDiscordUseDetailsEnabled()
            discordManager.initialize(token, enabled, useDetails)
            
            // Listen for setting changes
            launch {
                sessionManager.discordRpcEnabledFlow.collect { newEnabled ->
                    val currToken = sessionManager.getDiscordToken()
                    val currUseDetails = sessionManager.isDiscordUseDetailsEnabled()
                    discordManager.updateSettings(currToken, newEnabled, currUseDetails)
                    updateDiscordPresence()
                }
            }
            launch {
                sessionManager.discordTokenFlow.collect { newToken ->
                    val currEnabled = sessionManager.isDiscordRpcEnabled()
                    val currUseDetails = sessionManager.isDiscordUseDetailsEnabled()
                    discordManager.updateSettings(newToken, currEnabled, currUseDetails)
                    updateDiscordPresence()
                }
            }
            launch {
                sessionManager.discordUseDetailsFlow.collect { newUseDetails ->
                    val currToken = sessionManager.getDiscordToken()
                    val currEnabled = sessionManager.isDiscordRpcEnabled()
                    discordManager.updateSettings(currToken, currEnabled, newUseDetails)
                    updateDiscordPresence()
                }
            }
        }
    }
    
    private fun observeDownloadStateConsistency() {
        viewModelScope.launch {
            // Monitor for when download state is reset to NOT_DOWNLOADED (e.g. by player transition)
            // but the song is actually downloaded.
            playerState.map { it.currentSong to it.downloadState }
                .distinctUntilChanged()
                .collect { (song, downloadState) ->
                    if (song != null && downloadState == DownloadState.NOT_DOWNLOADED) {
                        checkDownloadStatus(song)
                    }
                }
        }
    }
    
    private fun observeLyricsProviderSettings() {
        viewModelScope.launch {
            combine(
                sessionManager.enableBetterLyricsFlow,
                sessionManager.enableSimpMusicFlow,
                sessionManager.developerModeFlow
            ) { betterLyricsEnabled, simpMusicEnabled, devMode ->
                mapOf(
                    LyricsProviderType.AUTO to true, // Always enabled
                    LyricsProviderType.BETTER_LYRICS to betterLyricsEnabled,
                    LyricsProviderType.SIMP_MUSIC to simpMusicEnabled,
                    LyricsProviderType.LRCLIB to true,
                    LyricsProviderType.JIOSAAVN to devMode,
                    LyricsProviderType.YOUTUBE to true
                )
            }.collectLatest { newMap ->
                val previousMap = _enabledLyricsProviders.value
                _enabledLyricsProviders.value = newMap
                
                // If currently selected provider was disabled, switch to AUTO
                val currentSelection = _selectedLyricsProvider.value
                if (currentSelection != LyricsProviderType.AUTO && newMap[currentSelection] == false) {
                    Log.d("PlayerViewModel", "Current provider $currentSelection disabled, switching to AUTO")
                    switchLyricsProvider(LyricsProviderType.AUTO)
                }
            }
        }
    }
    
    private fun observeCurrentSong() {
        viewModelScope.launch {
            playerState.map { it.currentSong }
                .distinctUntilChanged()
                .collectLatest { song ->
                    if (song != null) {
                        _isMiniPlayerDismissed.value = false // Show mini player when a new song starts
                        checkLikeStatus(song)
                        checkDownloadStatus(song)
                        
                        // Reset sync state for new song
                        if (song.id != lastSyncedVideoId) {
                            currentSongPlayTime = 0
                            // Allow re-sync if it's a new song ID. 
                            // If it's the same song ID (repeat), we might not want to spam sync.
                            // But usually repeat implies a new listen. 
                            // For safety, let's reset lastSyncedVideoId if the song CHANGED.
                             lastSyncedVideoId = null 
                        }

                        // Reset provider to AUTO on song change unless user specifically locked a provider?
                        // For now, let's keep it persistent or reset. Resetting is safer for "Best Match".
                        _selectedLyricsProvider.value = LyricsProviderType.AUTO
                        
                        // Clear old data synchronously to prevent stale display
                        _lyricsState.value = null
                        _commentsState.value = null
                        
                        fetchLyrics(song.id)
                        fetchComments(song.id)
                        
                        updateDiscordPresence()
                    } else {
                        _lyricsState.value = null
                        _commentsState.value = null
                        updateDiscordPresence()
                    }
                }
        }

        // Observe play/pause state for Discord
        viewModelScope.launch {
            playerState.map { it.isPlaying }
                .distinctUntilChanged()
                .collect { isPlaying ->
                    updateDiscordPresence()
                }
        }
    }
    
    private fun updateDiscordPresence() {
        val song = playerState.value.currentSong
        val isPlaying = playerState.value.isPlaying
        val position = playerState.value.currentPosition
        
        if (song != null) {
            discordManager.updatePresence(
                title = song.title,
                artist = song.artist,
                imageUrl = song.thumbnailUrl ?: "",
                isPlaying = isPlaying,
                duration = song.duration * 1000, 
                currentPosition = position
            )
        }
    }

    
    private fun observeDownloads() {
        viewModelScope.launch {
            // Wait a bit for downloads to be loaded, then check initial state
            delay(500)
            val currentSong = playerState.value.currentSong
            if (currentSong != null) {
                checkDownloadStatus(currentSong)
            }
        }
        
        viewModelScope.launch {
            downloadRepository.downloadedSongs.collect {
                val currentSong = playerState.value.currentSong
                if (currentSong != null) {
                    checkDownloadStatus(currentSong)
                }
            }
        }
        
        viewModelScope.launch {
            downloadRepository.downloadingIds.collect { downloadingIds ->
                val currentSong = playerState.value.currentSong ?: return@collect
                if (downloadingIds.contains(currentSong.id)) {
                    musicPlayer.updateDownloadState(DownloadState.DOWNLOADING)
                } else {
                     // If it was downloading and now it's not, check if it's downloaded or failed
                     // This overlaps with the downloadedSongs collector but handles the transition faster
                     if (downloadRepository.isDownloaded(currentSong.id)) {
                         musicPlayer.updateDownloadState(DownloadState.DOWNLOADED)
                     } else {
                         musicPlayer.updateDownloadState(DownloadState.NOT_DOWNLOADED)
                     }
                }
            }
        }
    }
    
    private fun checkLikeStatus(song: Song) {
        viewModelScope.launch {
            val likedSongs = youTubeRepository.getLikedMusic()
            val isLiked = likedSongs.any { it.id == song.id }
            musicPlayer.updateLikeStatus(isLiked)
        }
    }
    
    private fun checkDownloadStatus(song: Song) {
        if (downloadRepository.isDownloading(song.id)) {
            musicPlayer.updateDownloadState(DownloadState.DOWNLOADING)
        } else if (downloadRepository.isDownloaded(song.id)) {
            musicPlayer.updateDownloadState(DownloadState.DOWNLOADED)
        } else {
            musicPlayer.updateDownloadState(DownloadState.NOT_DOWNLOADED)
        }
    }
    
    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = 0) {
        // Reset radio base so Autoplay adapts to this new song
        radioBaseSongId = null
        _isRadioMode.value = false
        musicPlayer.updateRadioMode(false)
        musicPlayer.playSong(song, queue, startIndex)
    }
    
    fun playNext(song: Song) {
        musicPlayer.playNext(listOf(song))
    }
    
    fun addToQueue(song: Song) {
        musicPlayer.addToQueue(listOf(song))
    }
    
    fun play() {
        musicPlayer.play()
    }
    
    fun pause() {
        musicPlayer.pause()
    }
    
    fun togglePlayPause() {
        musicPlayer.togglePlayPause()
    }

    fun stop() {
        musicPlayer.stop()
        _isMiniPlayerDismissed.value = false // Reset for next time
    }

    fun dismissMiniPlayer() {
        _isMiniPlayerDismissed.value = true
    }

    fun showMiniPlayer() {
        _isMiniPlayerDismissed.value = false
    }
    
    fun seekTo(position: Long) {
        musicPlayer.seekTo(position)
    }
    
    fun seekToNext() {
        musicPlayer.seekToNext()
    }
    
    fun seekToPrevious() {
        musicPlayer.seekToPrevious()
    }
    
    fun toggleShuffle() {
        musicPlayer.toggleShuffle()
    }
    
    fun toggleRepeat() {
        musicPlayer.toggleRepeat()
    }
    
    fun toggleAutoplay() {
        musicPlayer.toggleAutoplay()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        musicPlayer.moveInQueue(fromIndex, toIndex)
    }

    fun removeQueueItems(indices: List<Int>) {
        musicPlayer.removeFromQueue(indices)
    }

    fun clearQueue() {
        musicPlayer.clearQueue()
    }

    fun saveQueueAsPlaylist(title: String, description: String, isPrivate: Boolean, syncWithYt: Boolean = true, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val songs = playerState.value.queue
                if (songs.isEmpty()) {
                    onComplete(false)
                    return@launch
                }

                if (syncWithYt && sessionManager.isLoggedIn()) {
                    val privacyStatus = if (isPrivate) "PRIVATE" else "PUBLIC"
                    val playlistId = youTubeRepository.createPlaylist(title, description, privacyStatus)
                    
                    if (playlistId != null) {
                        val videoIds = songs.map { it.id }
                        val success = youTubeRepository.addSongsToPlaylist(playlistId, videoIds)
                        onComplete(success)
                    } else {
                        onComplete(false)
                    }
                } else {
                    // Create Local Playlist from Queue
                    val id = "local_q_" + java.util.UUID.randomUUID().toString()
                    val playlist = com.suvojeet.suvmusic.data.model.Playlist(
                        id = id,
                        title = title,
                        author = "You",
                        thumbnailUrl = songs.firstOrNull()?.thumbnailUrl,
                        songs = songs
                    )
                    libraryRepository.savePlaylist(playlist)
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error saving queue as playlist", e)
                onComplete(false)
            }
        }
    }
    
    fun toggleVideoMode() {
        musicPlayer.toggleVideoMode()
    }

    fun setVideoQuality(quality: VideoQuality) {
        musicPlayer.setVideoQuality(quality)
    }
    
    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }
    
    fun setFullScreen(isFullScreen: Boolean) {
        _isFullScreen.value = isFullScreen
    }

    fun dismissVideoError() {
        musicPlayer.dismissVideoError()
    }

    fun switchOutputDevice(device: OutputDevice) {
        musicPlayer.switchOutputDevice(device)
    }
    
    fun refreshDevices() {
        musicPlayer.refreshDevices()
    }
    
    fun setPlaybackParameters(speed: Float, pitch: Float) {
        musicPlayer.setPlaybackParameters(speed, pitch)
    }
    
    fun getPlayer() = musicPlayer.getPlayer()
    
    /**
     * Start a radio based on the given song.
     * Uses YT Music recommendations when logged in, local history-based recommendations when not.
     * Creates an endless queue that auto-loads more songs as you near the end.
     */
    fun startRadio(song: Song) {
        viewModelScope.launch {
            _isRadioMode.value = true
            musicPlayer.updateRadioMode(true)
            radioBaseSongId = song.id
            
            // Play immediately with just the selected song
            musicPlayer.playSong(song)
            
            try {
                val radioSongs = mutableListOf<Song>()
                
                // 1. Try YT Music recommendations first (works best when logged in)
                if (song.source == SongSource.YOUTUBE || song.source == SongSource.DOWNLOADED) {
                    val relatedSongs = youTubeRepository.getRelatedSongs(song.id)
                    if (relatedSongs.isNotEmpty()) {
                        // De-duplicate and filter current song
                        val filtered = relatedSongs.filter { it.id != song.id }
                            .distinctBy { it.id }
                        radioSongs.addAll(filtered.take(30))
                    }
                }
                
                // 2. If not enough songs or not a YouTube song, use local recommendation engine
                if (radioSongs.size < 10) {
                    val localRecommendations = recommendationEngine.getPersonalizedRecommendations(30)
                    // Filter out songs already in queue (currently just the playing song)
                    // and any we just fetched
                    val existingIds = mutableSetOf(song.id)
                    existingIds.addAll(radioSongs.map { it.id })
                    
                    val newSongs = localRecommendations.filter { it.id !in existingIds }
                    radioSongs.addAll(newSongs)
                }
                
                // 3. Last resort fallback: trending songs
                if (radioSongs.isEmpty()) {
                    val fallback = youTubeRepository.search("trending music", YouTubeRepository.FILTER_SONGS)
                    radioSongs.addAll(fallback.filter { it.id != song.id }.take(10))
                }
                
                // Add recommendations to queue
                if (radioSongs.isNotEmpty()) {
                    musicPlayer.addToQueue(radioSongs)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error starting radio", e)
                // Already playing, so no fallback needed
            }
        }
    }
    
    /**
     * Observe queue position and automatically load more songs when autoplay is enabled
     * and the user is approaching the end of the queue.
     */
    private fun observeQueuePositionForAutoplay() {
        viewModelScope.launch {
            combine(
                playerState.map { Triple(it.currentIndex, it.queue.size, it.isAutoplayEnabled) }.distinctUntilChanged(),
                _isRadioMode
            ) { triple, radioMode ->
                val (currentIndex, queueSize, isAutoplayEnabled) = triple
                Pair(triple, radioMode)
            }.collect { (triple, radioMode) ->
                val (currentIndex, queueSize, isAutoplayEnabled) = triple
                // When autoplay is enabled OR radio mode is on, and we're within 3 songs of the end, load more
                if ((isAutoplayEnabled || radioMode) && queueSize > 0 && currentIndex >= queueSize - 3) {
                    loadMoreAutoplaySongs()
                }
            }
        }
    }
    
    /**
     * Load more songs for autoplay/radio queue.
     * Called automatically when near end of queue (infinite scroll) or when autoplay needs more songs.
     */
    fun loadMoreAutoplaySongs() {
        val state = playerState.value
        val isAutoplayEnabled = state.isAutoplayEnabled
        val radioMode = _isRadioMode.value
        
        // Allow loading if radio mode OR autoplay is enabled
        if (!radioMode && !isAutoplayEnabled) return
        if (_isLoadingMoreSongs.value) return // Prevent duplicate loads
        
        val currentSong = state.currentSong ?: return
        var baseSongId = radioBaseSongId ?: currentSong.id
        
        viewModelScope.launch {
            _isLoadingMoreSongs.value = true
            try {
                var moreSongs = youTubeRepository.getRelatedSongs(baseSongId)
                
                // Filter out songs already in queue
                val currentQueue = playerState.value.queue
                val existingIds = currentQueue.map { it.id }.toSet()
                var newSongs = moreSongs.filter { it.id !in existingIds }
                
                // If no new songs found from the tail, try using the currently playing song as seed
                // This ensures we stay related to what the user is actually listening to
                if (newSongs.isEmpty() && baseSongId != currentSong.id) {
                    baseSongId = currentSong.id
                    moreSongs = youTubeRepository.getRelatedSongs(baseSongId)
                    newSongs = moreSongs.filter { it.id !in existingIds }
                }
                
                if (newSongs.isNotEmpty()) {
                    musicPlayer.addToQueue(newSongs.take(10))
                    // Update base song for next batch (use last added song for variety)
                    radioBaseSongId = newSongs.lastOrNull()?.id ?: baseSongId
                } else {
                    // Strict YT Music recommendations requested:
                    // Removed local fallback to ensure accuracy and relevance.
                    Log.w("PlayerViewModel", "Could not find more related songs from YouTube Music")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error loading more radio songs", e)
            } finally {
                _isLoadingMoreSongs.value = false
            }
        }
    }
    
    /**
     * Load more songs for endless radio queue.
     * Called automatically when near end of queue (infinite scroll).
     * @deprecated Use loadMoreAutoplaySongs() instead
     */
    fun loadMoreRadioSongs() {
        loadMoreAutoplaySongs()
    }
    
    /**
     * Stop radio mode and clear the endless queue behavior.
     */
    fun stopRadio() {
        _isRadioMode.value = false
        musicPlayer.updateRadioMode(false)
        radioBaseSongId = null
    }
    
    /**
     * Play a song from a deep link (YouTube/YouTube Music URL).
     * Fetches song details from YouTube and starts playback.
     */
    fun playFromDeepLink(videoId: String) {
        viewModelScope.launch {
            try {
                // Fetch song details from YouTube
                val song = youTubeRepository.getSongDetails(videoId)
                if (song != null) {
                    playSong(song)
                }
            } catch (e: Exception) {
                // Handle error - could show a toast or error state
                e.printStackTrace()
            }
        }
    }

    private fun checkAndSyncHistory() {
        if (!isHistorySyncEnabled) return
        val currentSong = playerState.value.currentSong ?: return
        
        // Only sync YouTube songs
        if (currentSong.source != SongSource.YOUTUBE) return
        
        // Avoid duplicate syncs for the same session of the same song
        if (lastSyncedVideoId == currentSong.id) return
        
        // Sync if played enough
        if (currentSongPlayTime >= HISTORY_SYNC_THRESHOLD_MS) {
            lastSyncedVideoId = currentSong.id
            viewModelScope.launch {
                Log.d("PlayerViewModel", "Triggering history sync for ${currentSong.title}")
                youTubeRepository.markAsWatched(currentSong.id)
            }
        }
    }
    
    /**
     * Play an audio file from a local URI (opened from external file manager).
     * Extracts metadata using MediaMetadataRetriever and creates a local Song.
     */
    fun playFromLocalUri(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                // Take persistent permission if it's a content URI
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        // Permission might not be grantable, continue anyway
                        Log.w("PlayerViewModel", "Could not take persistent permission: ${e.message}")
                    }
                }
                
                // Extract metadata from the audio file
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: getFileNameFromUri(context, uri)
                        ?: "Unknown Title"
                    
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                        ?: "Unknown Artist"
                    
                    val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?: ""
                    
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val duration = durationStr?.toLongOrNull() ?: 0L
                    
                    // Try to get album art
                    val embeddedArt = retriever.embeddedPicture
                    val albumArtUri: android.net.Uri? = if (embeddedArt != null) {
                        // Create a temporary file for album art or use a content URI approach
                        // For simplicity, we'll use the audio URI itself as a reference
                        null // Album art will be extracted by Coil if needed
                    } else {
                        null
                    }
                    
                    // Create a unique ID from the URI
                    val songId = uri.toString().hashCode().toLong()
                    
                    val song = Song.fromLocal(
                        id = songId,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        albumArtUri = albumArtUri,
                        contentUri = uri
                    )
                    
                    // Play the song
                    playSong(song)
                    
                } finally {
                    retriever.release()
                }
                
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error playing local file", e)
                e.printStackTrace()
            }
        }
    }
    
    fun calibrateAudioAr() {
        audioARManager.calibrate()
    }

    /**
     * Extract filename from a content URI.
     */
    private fun getFileNameFromUri(context: Context, uri: android.net.Uri): String? {
        var fileName: String? = null
        
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
        
        if (fileName == null) {
            fileName = uri.lastPathSegment
        }
        
        // Remove file extension
        return fileName?.substringBeforeLast(".")
    }
    
    fun downloadCurrentSong() {
        val song = playerState.value.currentSong ?: return
        if (downloadRepository.isDownloaded(song.id) || downloadRepository.isDownloading(song.id)) return
        
        musicPlayer.updateDownloadState(DownloadState.DOWNLOADING)
        
        // Start foreground service for background download with notification
        DownloadService.startDownload(context, song)
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(songId)
            // Update state immediately if it's current song
            val currentSong = playerState.value.currentSong
            if (currentSong != null && currentSong.id == songId) {
                musicPlayer.updateDownloadState(DownloadState.NOT_DOWNLOADED)
            }
        }
    }
    
    /**
     * Download current song with progressive playback.
     * Starts playing after first ~30 seconds download, continues downloading in background.
     * Use this for "Download & Play" feature.
     */
    fun downloadAndPlayCurrentSong() {
        val song = playerState.value.currentSong ?: return
        if (downloadRepository.isDownloading(song.id)) return
        
        // If already downloaded, just play from local
        if (downloadRepository.isDownloaded(song.id)) {
            // Already playing or can seek to start
            return
        }
        
        musicPlayer.updateDownloadState(DownloadState.DOWNLOADING)
        viewModelScope.launch {
            val success = downloadRepository.downloadSongProgressive(song) { tempUri ->
                // First chunk ready - start playing from temp file
                Log.d("PlayerViewModel", "Progressive download ready, playing from: $tempUri")
                // The song is already playing (streaming), we just continue
                // The file will be saved when download completes
            }
            
            if (success) {
                musicPlayer.updateDownloadState(DownloadState.DOWNLOADED)
            } else {
                musicPlayer.updateDownloadState(DownloadState.FAILED)
            }
        }
    }
    
    /**
     * Download and immediately start playing a song (not current).
     * Perfect for clicking download on a song and having it play while downloading.
     */
    fun downloadAndPlay(song: Song) {
        if (downloadRepository.isDownloading(song.id)) return
        
        // If already downloaded, play from local
        if (downloadRepository.isDownloaded(song.id)) {
            val downloadedSong = downloadRepository.downloadedSongs.value.find { it.id == song.id }
            if (downloadedSong != null) {
                playSong(downloadedSong)
            }
            return
        }
        
        viewModelScope.launch {
            downloadRepository.downloadSongProgressive(song) { tempUri ->
                // First chunk ready - start playback from temp file
                val tempSong = song.copy(
                    source = SongSource.DOWNLOADED,
                    localUri = tempUri
                )
                playSong(tempSong)
            }
        }
    }    

    
    private fun fetchLyrics(videoId: String, provider: LyricsProviderType = LyricsProviderType.AUTO) {
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            _lyricsState.value = null
            
            val currentSong = playerState.value.currentSong
            if (currentSong != null && currentSong.id == videoId) {
                try {
                    val lyrics = lyricsRepository.getLyrics(currentSong, provider)
                    // Check if song is still the same after fetch
                    if (playerState.value.currentSong?.id == videoId) {
                        _lyricsState.value = lyrics
                    }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Error fetching lyrics", e)
                }
            }
            
            _isFetchingLyrics.value = false
        }
    }
    
    fun switchLyricsProvider(provider: LyricsProviderType) {
        _selectedLyricsProvider.value = provider
        val currentSong = playerState.value.currentSong ?: return
        fetchLyrics(currentSong.id, provider)
    }
    
    private fun fetchComments(videoId: String) {
        viewModelScope.launch {
            _isFetchingComments.value = true
            _commentsState.value = null
            
            // Only fetch for YouTube/Downloaded source which have valid video IDs
            val currentSong = playerState.value.currentSong
            if (currentSong != null && (currentSong.source == SongSource.YOUTUBE || currentSong.source == SongSource.DOWNLOADED)) {
                 val comments = youTubeRepository.getComments(videoId)
                 _commentsState.value = comments
            }
            
            _isFetchingComments.value = false
        }
    }
    
    fun loadMoreComments() {
        if (_isLoadingMoreComments.value || _isFetchingComments.value) return
        val currentSong = playerState.value.currentSong ?: return
        
        viewModelScope.launch {
            _isLoadingMoreComments.value = true
            val moreComments = youTubeRepository.getMoreComments(currentSong.id)
            if (moreComments.isNotEmpty()) {
                val currentComments = _commentsState.value ?: emptyList()
                _commentsState.value = currentComments + moreComments
            }
            _isLoadingMoreComments.value = false
        }
    }

    /**
     * Post a comment on the current song's video.
     */
    fun postComment(commentText: String) {
        val song = playerState.value.currentSong ?: return
        if (commentText.isBlank()) return
        
        viewModelScope.launch {
            _isPostingComment.value = true
            _commentPostSuccess.value = null
            
            val success = youTubeRepository.postComment(song.id, commentText)
            _commentPostSuccess.value = success
            
            if (success) {
                // Optimistically add comment
                val userAvatar = sessionManager.getUserAvatar()
                
                val newComment = Comment(
                    id = "temp_${System.currentTimeMillis()}",
                    authorName = "You", 
                    authorThumbnailUrl = userAvatar,
                    text = commentText,
                    timestamp = "Just now",
                    likeCount = "0",
                    replyCount = 0
                )
                
                val currentComments = _commentsState.value ?: emptyList()
                _commentsState.value = listOf(newComment) + currentComments
            }
            
            _isPostingComment.value = false
            
            // Clear the success state after a delay
            delay(2000)
            _commentPostSuccess.value = null
        }
    }
    
    fun likeCurrentSong() {
        val song = playerState.value.currentSong ?: return
        likeSong(song)
    }

    fun likeSong(song: Song) {
        val isCurrent = song.id == playerState.value.currentSong?.id
        val currentLikeState = if (isCurrent) playerState.value.isLiked else false // We don't track like state for all queue items in PlayerState
        
        viewModelScope.launch {
            val rating = if (!currentLikeState) "LIKE" else "INDIFFERENT"
            val success = youTubeRepository.rateSong(song.id, rating)
            if (success) {
                if (isCurrent) {
                    musicPlayer.updateLikeStatus(!currentLikeState)
                }
                
                if (rating == "LIKE") {
                    if (youTubeRepository.isOnline()) {
                        youTubeRepository.getLikedMusic(fetchAll = false)
                    }
                } else {
                    youTubeRepository.removeFromLikedCache(song.id)
                }
            }
        }
    }

    fun isDownloaded(songId: String): Boolean {
        return downloadRepository.isDownloaded(songId)
    }

    fun dislikeCurrentSong() {
        val song = playerState.value.currentSong ?: return
        val currentDislikeState = playerState.value.isDisliked
        
        // Optimistic update
        musicPlayer.updateDislikeStatus(!currentDislikeState)
        
        viewModelScope.launch {
            val rating = if (!currentDislikeState) "DISLIKE" else "INDIFFERENT"
            val success = youTubeRepository.rateSong(song.id, rating)
            if (!success) {
                // Revert on failure
                musicPlayer.updateDislikeStatus(currentDislikeState)
            }
        }
    }
    
    /**
     * Restore last playback state if available.
     */
    suspend fun restoreLastPlayback() {
        val lastState = sessionManager.getLastPlaybackState() ?: return
        
        try {
            val (queue, index) = withContext(Dispatchers.Default) {
                val jsonArray = JSONArray(lastState.queueJson)
                val queueList = mutableListOf<Song>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    queueList.add(
                        Song(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            album = obj.optString("album", ""),
                            thumbnailUrl = obj.optString("thumbnailUrl", null),
                            duration = obj.getLong("duration"),
                            source = try { 
                                SongSource.valueOf(obj.getString("source")) 
                            } catch (e: Exception) { 
                                SongSource.YOUTUBE 
                            }
                        )
                    )
                }
                Pair(queueList, lastState.index)
            }
            
            if (queue.isNotEmpty() && index in queue.indices) {
                val song = queue[index]
                // Load song without auto-playing (user can resume manually)
                musicPlayer.playSong(song, queue, index, autoPlay = false)
                
                // Seek to saved position after a delay (allow media to load)
                delay(1000)
                musicPlayer.seekTo(lastState.position)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun updateDominantColor(color: Int) {
        musicPlayer.updateDominantColor(color)
    }

    override fun onCleared() {
        super.onCleared()
        // Don't release player here - it's shared
    }
}

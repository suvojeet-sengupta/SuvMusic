package com.suvojeet.suvmusic.ui.viewmodel

import android.app.PendingIntent
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
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.data.model.VideoQuality
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
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
import com.suvojeet.suvmusic.recommendation.SmartQueueManager
import com.suvojeet.suvmusic.service.DownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val smartQueueManager: SmartQueueManager,
    private val sponsorBlockRepository: SponsorBlockRepository,
    private val listeningHistoryRepository: ListeningHistoryRepository,
    private val discordManager: DiscordManager,
    private val audioARManager: com.suvojeet.suvmusic.player.AudioARManager,
    private val spatialAudioProcessor: com.suvojeet.suvmusic.player.SpatialAudioProcessor,
    @param:ApplicationContext private val context: Context
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

    private val _artistCredits = MutableStateFlow<List<com.suvojeet.suvmusic.ui.components.ArtistCreditInfo>>(emptyList())
    val artistCredits: StateFlow<List<com.suvojeet.suvmusic.ui.components.ArtistCreditInfo>> = _artistCredits.asStateFlow()

    private val _showMultipleArtistsDialog = MutableStateFlow(false)
    val showMultipleArtistsDialog: StateFlow<Boolean> = _showMultipleArtistsDialog.asStateFlow()

    private val _pendingIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingIntent = _pendingIntent.asStateFlow()

    fun consumePendingIntent() {
        _pendingIntent.value = null
    }
    
    // Lyrics Provider Selection
    private val _selectedLyricsProvider = MutableStateFlow(LyricsProviderType.AUTO)
    val selectedLyricsProvider: StateFlow<LyricsProviderType> = _selectedLyricsProvider.asStateFlow()

    private val _enabledLyricsProviders = MutableStateFlow<Map<LyricsProviderType, Boolean>>(emptyMap())
    val enabledLyricsProviders: StateFlow<Map<LyricsProviderType, Boolean>> = _enabledLyricsProviders.asStateFlow()

    // Comments State
    private val _commentsState = MutableStateFlow<List<Comment>?>(null)
    val commentsState: StateFlow<List<Comment>?> = _commentsState.asStateFlow()

    private val _isFetchingComments = MutableStateFlow(false)
    val isFetchingComments: StateFlow<Boolean> = _isFetchingComments.asStateFlow()

    private val _isLoadingMoreComments = MutableStateFlow(false)
    val isLoadingMoreComments: StateFlow<Boolean> = _isLoadingMoreComments.asStateFlow()
    
    private val _isLoadingMoreSongs = MutableStateFlow(false)
    val isLoadingMoreSongs: StateFlow<Boolean> = _isLoadingMoreSongs.asStateFlow()
    
    // MiniPlayer Visibility State
    private val _isMiniPlayerDismissed = MutableStateFlow(false)
    val isMiniPlayerDismissed: StateFlow<Boolean> = _isMiniPlayerDismissed.asStateFlow()

    // Fullscreen State (Video)
    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()
    
    // Player Sheet Expansion State
    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()
    
    // Queue Selection State
    private val _selectedQueueIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedQueueIndices: StateFlow<Set<Int>> = _selectedQueueIndices.asStateFlow()

    // Derived Queue Sections
    /** Songs from index 0 to currentIndex - 1 — already played */
    val historySongs: StateFlow<List<Song>> = playerState.map { state ->
        if (state.currentIndex <= 0) emptyList()
        else state.queue.subList(0, state.currentIndex.coerceAtMost(state.queue.size))
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Songs after currentIndex — the actual upcoming / "up next" songs */
    val upNextSongs: StateFlow<List<Song>> = playerState.map { state ->
        if (state.currentIndex < 0 || state.currentIndex >= state.queue.size - 1) emptyList()
        else state.queue.subList(state.currentIndex + 1, state.queue.size)
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

                        // Notify recommendation engine of song change for adaptive recommendations
                        recommendationEngine.onSongPlayed(song)

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
                        fetchArtistCredits(song.artist, song.source)

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

    private fun checkLikeStatus(song: Song) {
        viewModelScope.launch {
            val isLiked = libraryRepository.isSongFavorite(song.id)
            musicPlayer.updateLikeStatus(isLiked)
        }
    }

    private fun checkDownloadStatus(song: Song) {
        viewModelScope.launch {
            val isDownloaded = downloadRepository.isDownloaded(song.id)
            musicPlayer.updateDownloadState(if (isDownloaded) DownloadState.DOWNLOADED else DownloadState.NOT_DOWNLOADED)
        }
    }

    fun toggleLike() {
        val song = playerState.value.currentSong ?: return
        viewModelScope.launch {
            val currentlyLiked = playerState.value.isLiked
            if (currentlyLiked) {
                libraryRepository.removeSongFromFavorites(song.id)
            } else {
                libraryRepository.addSongToFavorites(song)
            }
            musicPlayer.updateLikeStatus(!currentlyLiked)
        }
    }

    fun toggleDislike() {
        val currentlyDisliked = playerState.value.isDisliked
        musicPlayer.updateDislikeStatus(!currentlyDisliked)
        
        // Also remove like if disliking
        if (!currentlyDisliked && playerState.value.isLiked) {
            val song = playerState.value.currentSong ?: return
            viewModelScope.launch {
                libraryRepository.removeSongFromFavorites(song.id)
                musicPlayer.updateLikeStatus(false)
            }
        }
    }

    fun playPause() = musicPlayer.togglePlayPause()
    fun seekToNext() = musicPlayer.seekToNext()
    fun seekToPrevious() = musicPlayer.seekToPrevious()
    fun seekTo(position: Long) = musicPlayer.seekTo(position)
    fun setShuffleMode(enabled: Boolean) = musicPlayer.setShuffleMode(enabled)
    fun setRepeatMode(mode: Int) = musicPlayer.setRepeatMode(mode)
    fun stop() = musicPlayer.stop()

    fun expandPlayer() { _isPlayerExpanded.value = true }
    fun collapsePlayer() { _isPlayerExpanded.value = false }
    
    fun setFullScreen(full: Boolean) { _isFullScreen.value = full }

    /**
     * Toggles video mode. 
     * If enabled, tries to find a matching video for the current song.
     */
    fun toggleVideoMode() {
        val currentSong = playerState.value.currentSong ?: return
        val isCurrentlyVideo = playerState.value.isVideoMode
        
        if (isCurrentlyVideo) {
            // Switch back to audio
            musicPlayer.setVideoMode(false)
        } else {
            // Switch to video
            musicPlayer.setVideoMode(true)
        }
    }

    fun setSleepTimer(option: SleepTimerOption, customMinutes: Int? = null) {
        sleepTimerManager.setTimer(option, customMinutes)
    }

    val sleepTimerOption: StateFlow<SleepTimerOption> = sleepTimerManager.timerOption
    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimerManager.remainingTimeMs

    fun switchOutputDevice(device: OutputDevice) {
        // Implementation for switching output device
    }

    fun refreshDevices() {
        // Implementation for refreshing output devices
    }

    fun startRadio(song: Song, playlistId: String?) {
        radioBaseSongId = song.id
        _isRadioMode.value = true
        smartQueueManager.startRadio(song, playlistId)
    }

    fun loadMoreRadioSongs() {
        if (_isLoadingMoreSongs.value) return
        _isLoadingMoreSongs.value = true
        viewModelScope.launch {
            smartQueueManager.loadMoreRadioSongs()
            _isLoadingMoreSongs.value = false
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song), index: Int = 0) {
        musicPlayer.playSong(song, queue, index)
    }

    fun addToQueue(song: Song) = smartQueueManager.addToQueue(song)
    
    fun playNext(song: Song) = smartQueueManager.playNext(song)

    fun clearQueue() = musicPlayer.clearQueue()

    fun toggleQueueSelection(index: Int) {
        _selectedQueueIndices.update { current ->
            if (current.contains(index)) current - index else current + index
        }
    }

    fun clearQueueSelection() {
        _selectedQueueIndices.value = emptySet()
    }

    fun removeSelectedFromQueue() {
        val indices = _selectedQueueIndices.value.sortedDescending()
        musicPlayer.removeIndicesFromQueue(indices)
        _selectedQueueIndices.value = emptySet()
    }

    /**
     * Download current song.
     */
    fun downloadCurrentSong() {
        val song = playerState.value.currentSong ?: return
        if (downloadRepository.isDownloaded(song.id)) return
        if (downloadRepository.isDownloading(song.id)) return

        // If streaming, we can start a progressive download
        if (playerState.value.isPlaying) {
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

    private fun fetchArtistCredits(artistString: String, source: com.suvojeet.suvmusic.core.model.SongSource = com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE) {
        viewModelScope.launch {
            val artistNames = parseArtistNames(artistString)
            
            // Show placeholders immediately
            _artistCredits.value = artistNames.map { name ->
                com.suvojeet.suvmusic.ui.components.ArtistCreditInfo(
                    name = name,
                    role = "Vocals",
                    thumbnailUrl = null,
                    artistId = null
                )
            }
            
            // Fetch thumbnails
            val updatedCredits = artistNames.map { name ->
                try {
                    val searchResults = if (source == com.suvojeet.suvmusic.core.model.SongSource.JIOSAAVN) {
                        jioSaavnRepository.searchArtists(name)
                    } else {
                        youTubeRepository.searchArtists(name)
                    }
                    
                    val matchingArtist = searchResults.firstOrNull { 
                        it.name.contains(name, ignoreCase = true) || 
                        name.contains(it.name, ignoreCase = true)
                    } ?: searchResults.firstOrNull()
                    
                    com.suvojeet.suvmusic.ui.components.ArtistCreditInfo(
                        name = name,
                        role = "Vocals",
                        thumbnailUrl = matchingArtist?.thumbnailUrl,
                        artistId = matchingArtist?.id
                    )
                } catch (e: Exception) {
                    com.suvojeet.suvmusic.ui.components.ArtistCreditInfo(
                        name = name,
                        role = "Vocals",
                        thumbnailUrl = null,
                        artistId = null
                    )
                }
            }
            _artistCredits.value = updatedCredits
        }
    }

    private fun parseArtistNames(artistString: String): List<String> {
        if (artistString.isBlank()) return emptyList()
        val separatorRegex = Regex("[,&]|\\b(feat\\.?|ft\\.?|with|x)\\b", RegexOption.IGNORE_CASE)
        return artistString.split(separatorRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun toggleMultipleArtistsDialog(show: Boolean) {
        _showMultipleArtistsDialog.value = show
    }

    fun loadMoreComments() {
        if (_isLoadingMoreComments.value || _isFetchingComments.value) return
        val currentSong = playerState.value.currentSong ?: return

        viewModelScope.launch {
            _isLoadingMoreComments.value = true
            val moreComments = youTubeRepository.getMoreComments(currentSong.id)
            if (moreComments != null) {
                _commentsState.update { current ->
                    if (current == null) moreComments else current + moreComments
                }
            }
            _isLoadingMoreComments.value = false
        }
    }

    fun postComment(commentText: String) {
        if (_isPostingComment.value) return
        val currentSong = playerState.value.currentSong ?: return

        viewModelScope.launch {
            _isPostingComment.value = true
            val success = youTubeRepository.postComment(currentSong.id, commentText)
            if (success) {
                // Refresh comments to show the new one
                fetchComments(currentSong.id)
            }
            _isPostingComment.value = false
        }
    }

    private fun checkAndSyncHistory() {
        if (!isHistorySyncEnabled) return
        val currentSong = playerState.value.currentSong ?: return
        if (currentSong.id == lastSyncedVideoId) return
        
        if (currentSongPlayTime >= HISTORY_SYNC_THRESHOLD_MS) {
            viewModelScope.launch {
                val success = youTubeRepository.syncPlaybackHistory(currentSong.id)
                if (success) {
                    lastSyncedVideoId = currentSong.id
                }
            }
        }
    }

    fun getPlayer() = musicPlayer.getPlayer()

    fun setAudioQuality(quality: com.suvojeet.suvmusic.data.model.AudioQuality) {
        viewModelScope.launch {
            sessionManager.setAudioQuality(quality)
            // Re-apply current song to pick up new quality if needed
            playerState.value.currentSong?.let { song ->
                // If playing from YouTube, we might need to re-fetch the stream URL
                // This is simplified; real implementation depends on how player handles quality changes
            }
        }
    }

    fun getAudioCodec(): String? = musicPlayer.getAudioCodec()
    fun getAudioBitrate(): Int? = musicPlayer.getAudioBitrate()

    fun updateDiscordPresence() {
        val song = playerState.value.currentSong
        val isPlaying = playerState.value.isPlaying
        val position = playerState.value.currentPosition
        val duration = playerState.value.duration
        
        if (song != null) {
            discordManager.updatePresence(song, isPlaying, position, duration)
        } else {
            discordManager.clearPresence()
        }
    }

    fun calibrateAudioAr() {
        audioARManager.recenter()
    }

    fun setPlaybackParameters(speed: Float, pitch: Float) {
        musicPlayer.setPlaybackParameters(speed, pitch)
    }

    fun shareBugReport(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Send Bug Report via…").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "Error sharing bug report", e)
        }
    }
}

private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    value = function(value)
}

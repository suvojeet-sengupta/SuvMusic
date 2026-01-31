package com.suvojeet.suvmusic.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.DownloadState
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.service.MusicPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouteSelector
import com.suvojeet.suvmusic.data.model.DeviceType
import com.suvojeet.suvmusic.data.model.OutputDevice
import com.suvojeet.suvmusic.util.MusicHapticsManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around MediaController connected to MusicPlayerService.
 * This enables media notifications and proper audio focus handling.
 */
@Singleton
@OptIn(UnstableApi::class)
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val sessionManager: SessionManager,
    private val sleepTimerManager: SleepTimerManager,
    private val listeningHistoryRepository: ListeningHistoryRepository,
    private val cache: androidx.media3.datasource.cache.Cache,
    @com.suvojeet.suvmusic.di.PlayerDataSource private val dataSourceFactory: androidx.media3.datasource.DataSource.Factory,
    private val musicHapticsManager: MusicHapticsManager
) {
    
    // ... (existing properties)

    // Caching
    private var cachingJob: Job? = null

    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    private var positionUpdateJob: Job? = null
    
    // Audio Manager for device detection
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Preloading state for gapless playback
    private var preloadedNextSongId: String? = null
    private var preloadedStreamUrl: String? = null
    private var isPreloading = false
    
    // Track manually selected device ID to persist selection across refreshes
    private var manualSelectedDeviceId: String? = null
    
    // Cache for resolved video IDs for non-YouTube songs (SongId -> VideoId)
    // Fix: Unbounded Memory Leak -> Use LruCache with max size 100
    private val resolvedVideoIds = android.util.LruCache<String, String>(100)
    
    // Listening history tracking
    private var currentSongStartTime: Long = 0L
    private var currentSongStartPosition: Long = 0L
    
    private var deviceReceiver: android.content.BroadcastReceiver? = null
    
    init {
        connectToService()
        
        // Setup sleep timer callback
        sleepTimerManager.setOnTimerFinished {
            pause()
        }

        // Initial device scan
        updateAvailableDevices()
        
        // Register receiver for device changes
        registerDeviceReceiver()
    }

    private fun registerDeviceReceiver() {
        val filter = android.content.IntentFilter().apply {
            addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(android.content.Intent.ACTION_HEADSET_PLUG)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        
        deviceReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                // Small delay to allow system to update device list
                scope.launch {
                    delay(1000)
                    updateAvailableDevices()
                }
            }
        }
        
        try {
            deviceReceiver?.let { context.registerReceiver(it, filter) }
        } catch (e: Exception) {
            // Log error
        }
    }

    private fun updateAvailableDevices() {
        refreshDevices()
    }
    
    /**
     * Refresh available audio output devices.
     * Call this when the output device sheet is opened to get latest devices.
     */
    /**
     * Refresh available audio output devices.
     * Call this when the output device sheet is opened to get latest devices.
     */
    fun refreshDevices() {
        val rawDevices = mutableListOf<OutputDevice>()
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        
        // System state for auto-selection
        val isBluetoothActive = audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
        val isWiredHeadsetConnected = audioManager.isWiredHeadsetOn
        val autoSelectPhone = !isBluetoothActive && !isWiredHeadsetConnected

        // 1. Add Phone Speaker
        rawDevices.add(OutputDevice("phone_speaker", "Phone Speaker", DeviceType.PHONE, false))

        // 2. Add other devices
        audioDevices.forEach { device ->
            val type = when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, 
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceType.BLUETOOTH
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, 
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET -> DeviceType.HEADPHONES
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> return@forEach // Already handled
                else -> DeviceType.UNKNOWN
            }
            
            // Avoid duplicates by name
            if (rawDevices.none { it.name == device.productName.toString() }) {
                rawDevices.add(
                    OutputDevice(
                        id = device.id.toString(),
                        name = device.productName.toString().ifBlank { type.name },
                        type = type,
                        isSelected = false
                    )
                )
            }
        }

        // 3. Determine selection
        // Logic: specific manual selection > auto system selection
        
        var devicesWithSelection = rawDevices.map { device ->
            val isSelected = if (manualSelectedDeviceId != null) {
                device.id == manualSelectedDeviceId
            } else {
                when (device.type) {
                    DeviceType.PHONE -> autoSelectPhone
                    DeviceType.BLUETOOTH -> isBluetoothActive
                    DeviceType.HEADPHONES -> isWiredHeadsetConnected
                    else -> false
                }
            }
            device.copy(isSelected = isSelected)
        }
        
        // 4. Validate selection
        // If manual selection is active but the device is no longer available (not found in list),
        // or if no device is selected at all, fallback to auto/default.
        val hasSelection = devicesWithSelection.any { it.isSelected }
        
        if (!hasSelection) {
            // Manual device lost or auto-logic failed -> Reset manual and use auto logic
            if (manualSelectedDeviceId != null) {
                manualSelectedDeviceId = null
                devicesWithSelection = rawDevices.map { device ->
                    val isSelected = when (device.type) {
                        DeviceType.PHONE -> autoSelectPhone
                        DeviceType.BLUETOOTH -> isBluetoothActive
                        DeviceType.HEADPHONES -> isWiredHeadsetConnected
                        else -> false
                    }
                    device.copy(isSelected = isSelected)
                }
            }
            
            // If STILL no selection (edge case), select Phone Speaker (first)
            if (devicesWithSelection.none { it.isSelected }) {
                devicesWithSelection = devicesWithSelection.mapIndexed { index, dev -> 
                    dev.copy(isSelected = index == 0)
                }
            }
        }
        
        val selectedDevice = devicesWithSelection.find { it.isSelected }
        _playerState.update { it.copy(availableDevices = devicesWithSelection, selectedDevice = selectedDevice) }
    }

    fun switchOutputDevice(device: OutputDevice) {
        // Update manual preference
        manualSelectedDeviceId = device.id
        
        // Send command to service to switch output device (ExoPlayer routing)
        val args = android.os.Bundle().apply {
            putString("DEVICE_ID", device.id)
        }
        mediaController?.sendCustomCommand(
            androidx.media3.session.SessionCommand("SET_OUTPUT_DEVICE", android.os.Bundle.EMPTY),
            args
        )
        
        // Update local state immediately to reflect selection
        refreshDevices()
    }

    
    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlayerService::class.java)
        )
        
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                
                // Restore state if player has media
                if (mediaController?.mediaItemCount ?: 0 > 0) {
                    startPositionUpdates()
                }
            } catch (e: Exception) {
                _playerState.update { it.copy(error = "Failed to connect to music service") }
            }
        }, MoreExecutors.directExecutor())
    }
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
            
            // Music Haptics integration
            if (isPlaying) {
                scope.launch {
                    musicHapticsManager.refreshSettings()
                    musicHapticsManager.start()
                }
            } else {
                musicHapticsManager.stop()
            }
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playerState.update { 
                it.copy(
                    isLoading = playbackState == Player.STATE_BUFFERING,
                    error = null
                )
            }
            
            if (playbackState == Player.STATE_READY) {
                startPositionUpdates()
                // Update audio format info when playback is ready
                updateAudioFormatInfo()
            }
        }
        
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            // Update audio format when tracks change
            updateAudioFormatInfo()
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val controller = mediaController ?: return@let
                val index = controller.currentMediaItemIndex
                var song = _playerState.value.queue.getOrNull(index)
                
                // Listen Together Fix:
                // If the mediaItem ID differs from the queue song ID, it means the player 
                // was updated externally (e.g. by ListenTogetherManager). 
                // We should rely on the mediaItem's metadata in this case.
                if (song != null && song.id != item.mediaId) {
                     song = null
                }
                
                // Fallback: If song is null (e.g. Listen Together or external source), create from metadata
                if (song == null && item.mediaMetadata.title != null) {
                    val duration = if (controller.duration != androidx.media3.common.C.TIME_UNSET) controller.duration else 0L
                    song = Song(
                        id = item.mediaId,
                        title = item.mediaMetadata.title.toString(),
                        artist = item.mediaMetadata.artist.toString(),
                        album = item.mediaMetadata.albumTitle?.toString() ?: "",
                        duration = duration,
                        thumbnailUrl = item.mediaMetadata.artworkUri?.toString(),
                        source = SongSource.YOUTUBE // Assume YouTube as default for external
                    )
                }
                
                _playerState.update { 
                    it.copy(
                        currentSong = song,
                        currentIndex = index,
                        currentPosition = 0L,
                        duration = controller.duration.coerceAtLeast(0L),
                        isLiked = false,
                        isDisliked = false,
                        downloadState = DownloadState.NOT_DOWNLOADED,
                        isVideoMode = it.isVideoMode, // Persist video mode across songs
                        videoNotFound = false // Reset error flag on track change
                    )
                }
                
                // Add to recently played and track listening history
                if (song != null) {
                    scope.launch {
                        sessionManager.addToRecentlyPlayed(song)
                        
                        // Track previous song if it was playing
                        val prevSong = _playerState.value.currentSong
                        if (prevSong != null && currentSongStartTime > 0) {
                            val listenDuration = System.currentTimeMillis() - currentSongStartTime
                            val wasSkipped = listenDuration < (prevSong.duration * 0.5) // Skipped if < 50% listened
                            listeningHistoryRepository.recordPlay(prevSong, listenDuration, wasSkipped)
                        }
                        
                        // Start tracking new song
                        currentSongStartTime = System.currentTimeMillis()
                        currentSongStartPosition = controller.currentPosition
                    }
                }
                
                // Handle both AUTO (song ended) and SEEK (notification next/prev) transitions
                val shouldResolve = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || 
                                   reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                
                if (shouldResolve && song != null) {
                    // Check if current item already has a resolved stream URL (from preloading)
                    val currentItem = controller.currentMediaItem
                    val currentUri = currentItem?.localConfiguration?.uri?.toString()
                    
                    // Check if URI needs resolution:
                    // - YouTube placeholders: "https://youtube.com/watch?v=..."
                    // - JioSaavn/empty: null, empty, or doesn't look like a valid stream URL
                    val isYouTubePlaceholder = currentUri != null && (currentUri.contains("youtube.com/watch") || currentUri.contains("youtu.be"))
                    val isEmptyOrInvalid = currentUri.isNullOrBlank()
                    val needsResolution = isYouTubePlaceholder || isEmptyOrInvalid
                    
                    if (!needsResolution && currentUri != null) {
                        // Already has valid stream, just ensure UI state is correct and play
                        _playerState.update { it.copy(isLoading = false) }
                        
                        // Reset preload state as we've seemingly consumed it
                        preloadedNextSongId = null
                        preloadedStreamUrl = null
                        isPreloading = false
                        
                        // Start aggressive caching for this preloaded/resolved song
                        if (song.source != SongSource.LOCAL && song.source != SongSource.DOWNLOADED) {
                            // Cancel previous job first just in case
                            cachingJob?.cancel()
                            startAggressiveCaching(song.id, currentUri)
                        }
                        
                        // Ensure playback continues for SEEK transitions (notification controls)
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                            controller.play()
                        }
                        return@let
                    }
                    
                    // Check sleep timer (only for auto transitions)
                    val timerTriggered = if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        sleepTimerManager.onSongEnded()
                    } else {
                        false
                    }
                    
                    scope.launch {
                        resolveAndPlayCurrentItem(song, index, shouldPlay = !timerTriggered)
                    }
                }
            }
        }
        
        override fun onPlayerError(error: PlaybackException) {
            // Log error
            android.util.Log.e("MusicPlayer", "Playback error: ${error.errorCodeName}", error)
            
            // Check if error is recoverable (e.g. 403/410 HTTP error means URL expired)
            val cause = error.cause
            val isHttpError = cause is androidx.media3.datasource.HttpDataSource.HttpDataSourceException
            val responseCode = (cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode ?: 0
            val isexpiredUrl = (isHttpError && (responseCode == 403 || responseCode == 410))
            val isNetworkError = cause is java.net.UnknownHostException || cause is java.net.SocketTimeoutException
            val isDecoderError = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
            
            if (isexpiredUrl || isNetworkError || isDecoderError) {
                // Try to recover by re-resolving the stream URL
                val currentSong = _playerState.value.currentSong
                
                if (currentSong != null && currentSong.source != SongSource.LOCAL && currentSong.source != SongSource.DOWNLOADED) {
                    android.util.Log.d("MusicPlayer", "Attempting to recover from playback error for: ${currentSong.id}")
                    
                    scope.launch {
                        // Fallback logic: If in Video Mode, switch to Audio Mode first
                        if (_playerState.value.isVideoMode) {
                             android.util.Log.d("MusicPlayer", "Video playback failed, falling back to audio")
                             _playerState.update { 
                                 it.copy(
                                     isVideoMode = false, 
                                     videoNotFound = true,
                                     error = "Video unavailable, switching to audio..."
                                 ) 
                             }
                             // Clear cached video entry if it might be bad
                             resolvedVideoIds.remove(currentSong.id)
                        } else {
                             _playerState.update { it.copy(isLoading = true, error = null) }
                        }

                        // Wait a bit before retrying
                        delay(1000)
                        
                        // Resume from last position
                        val resumePosition = _playerState.value.currentPosition
                        
                        // Re-resolve and play (force resolution)
                        try {
                             resolveAndPlayCurrentItem(currentSong, _playerState.value.currentIndex, shouldPlay = true)
                             
                             // Seek to previous position once ready
                             // We'll trust the player updates to handle seeking, but we can hint it here
                             mediaController?.seekTo(resumePosition)
                        } catch (e: Exception) {
                            // Recovery failed
                             _playerState.update { 
                                it.copy(
                                    error = "Playback failed: ${error.message}",
                                    isLoading = false
                                )
                            }
                        }
                    }
                    return
                }
            }

            _playerState.update { 
                it.copy(
                    error = error.message ?: "Playback error",
                    isLoading = false
                )
            }
        }
    }
    
    private suspend fun resolveAndPlayCurrentItem(song: Song, index: Int, shouldPlay: Boolean = true) {
        try {
            _playerState.update { it.copy(isLoading = true, videoNotFound = false) }
            
            // Check for Developer Mode restriction for JioSaavn
            if (song.source == SongSource.JIOSAAVN && !sessionManager.isDeveloperMode()) {
                 _playerState.update {
                    it.copy(
                        error = "RESTRICTED_HQ_AUDIO",
                        isLoading = false
                    )
                 }
                 return
            }
            
            // Cancel previous caching job
            cachingJob?.cancel()
            
            // Resolve stream URL for the song based on source with timeout protection
            // Added explicit retry here in case the repository layer's retry exhausted or for other issues
            var streamUrl: String? = null
            var attempts = 0
            while (streamUrl == null && attempts < 2) { // Retry 1 time (total 2 attempts)
                streamUrl = kotlinx.coroutines.withTimeoutOrNull(20_000L) { // Increased timeout
                    when (song.source) {
                        SongSource.LOCAL, SongSource.DOWNLOADED -> song.localUri.toString()
                        SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(song.id)
                        else -> {
                            if (_playerState.value.isVideoMode) {
                                // Smart Video Matching
                                val videoId = resolvedVideoIds[song.id] ?: youTubeRepository.getBestVideoId(song).also { 
                                    resolvedVideoIds.put(song.id, it) 
                                }
                                youTubeRepository.getVideoStreamUrl(videoId)
                            } else {
                                youTubeRepository.getStreamUrl(song.id)
                            }
                        }
                    }
                }
                if (streamUrl == null) {
                    attempts++
                    if (attempts < 2) delay(1000)
                }
            }
            
            // Handle null stream URL - show error and clear loading state
            if (streamUrl == null) {
                android.util.Log.e("MusicPlayer", "Failed to resolve stream URL for: ${song.id} after retries")
                
                val isVideoMode = _playerState.value.isVideoMode
                _playerState.update { 
                    it.copy(
                        error = if (!isVideoMode) "Could not load song. Please check your connection." else null,
                        videoNotFound = isVideoMode,
                        isLoading = false
                    )
                }
                return
            }
            
            // Start aggressive caching in background
            if (song.source != SongSource.LOCAL && song.source != SongSource.DOWNLOADED) {
                startAggressiveCaching(song.id, streamUrl)
            }

            val newMediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaId(song.id)
                .setCustomCacheKey(song.id) // CRITICAL: Stable cache key
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()
            
            mediaController?.let { controller ->
                // Verify that the item at this index is still the one we resolved
                // This prevents race conditions where queue changed while we were fetching
                if (index < controller.mediaItemCount) {
                    val currentItem = controller.getMediaItemAt(index)
                    if (currentItem.mediaId == song.id) {
                        // Replace current item with resolved stream and play
                        val oldPos = controller.currentPosition // Remember pos if replacing same item (re-resolve case)
                        
                        controller.replaceMediaItem(index, newMediaItem)
                        
                        // If we are replacing the currently playing item
                        if (index == controller.currentMediaItemIndex) {
                             // If resuming from error/re-resolve, restore position
                             if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                                  controller.prepare()
                                  if (oldPos > 0) controller.seekTo(oldPos)
                             }
                             if (shouldPlay) {
                                  controller.play()
                             }
                        }
                        
                        // Clear loading state after successful resolution
                        _playerState.update { it.copy(isLoading = false, error = null) }
                    } else {
                        // Queue changed, discard this update
                        _playerState.update { it.copy(isLoading = false) }
                    }
                } else {
                    // Index out of bounds - clear loading state
                    _playerState.update { it.copy(isLoading = false) }
                }
            }
        } catch (e: Exception) {
            _playerState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    private fun startAggressiveCaching(contentId: String, streamUrl: String) {
        cachingJob = scope.launch(Dispatchers.IO) {
            try {
                val dataSpec = androidx.media3.datasource.DataSpec.Builder()
                    .setUri(streamUrl)
                    .setKey(contentId) // Must match the player's custom cache key
                    .setFlags(androidx.media3.datasource.DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                    .build()
                
                // Create a temporary CacheDataSource just for this writer
                // We reuse the factory's upstream logic but build a new instance
                val dataSource = dataSourceFactory.createDataSource() as? androidx.media3.datasource.cache.CacheDataSource
                
                if (dataSource != null) {
                    val cacheWriter = androidx.media3.datasource.cache.CacheWriter(
                        dataSource,
                        dataSpec,
                        null // default buffer
                    ) { requestLength, bytesCached, newBytesCached ->
                        // Optional: progress update
                        // val percent = if (requestLength > 0) (bytesCached * 100 / requestLength).toInt() else 0
                    }
                    
                    cacheWriter.cache()
                }
            } catch (e: Exception) {
                // Caching failed or was cancelled - ignore
                if (e !is kotlinx.coroutines.CancellationException) {
                     android.util.Log.e("MusicPlayer", "Aggressive caching failed: ${e.message}")
                }
            }
        }
    }
    
    private var saveCounter = 0
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        saveCounter = 0
        positionUpdateJob = scope.launch {
            while (true) {
                mediaController?.let { controller ->
                    val currentPos = controller.currentPosition.coerceAtLeast(0L)
                    val duration = controller.duration.coerceAtLeast(0L)
                    
                    _playerState.update { 
                        it.copy(
                            currentPosition = currentPos,
                            duration = duration,
                            bufferedPercentage = controller.bufferedPercentage
                        )
                    }
                    
                    // Save playback state every ~5 seconds (20 iterations * 250ms = 5s)
                    saveCounter++
                    if (saveCounter >= 20) {
                        saveCounter = 0
                        saveCurrentPlaybackState()
                    }
                    
                    // Check if we need to preload next song for gapless playback
                    if (sessionManager.isGaplessPlaybackEnabled()) {
                        checkPreloadNextSong(currentPos, duration)
                        
                        // Early transition: If we're in the last 1.5 seconds and next song is preloaded,
                        // trigger transition to prevent any audible gap during the final silence/fade-out
                        if (duration > 0 && currentPos >= duration - 1500 && preloadedNextSongId != null && preloadedStreamUrl != null) {
                            val state = _playerState.value
                            val nextIndex = state.currentIndex + 1
                            if (nextIndex < state.queue.size && state.queue.getOrNull(nextIndex)?.id == preloadedNextSongId) {
                                // Transition to next song immediately
                                controller.seekToNextMediaItem()
                            }
                        }
                    }
                    
                    // Music Haptics - simulate amplitude based on progress
                    // In a real implementation, this would use actual audio analysis
                    if (_playerState.value.isPlaying) {
                        val normalizedPosition = (currentPos % 500) / 500f // Creates a beat-like pattern
                        val simulatedAmplitude = (kotlin.math.sin(normalizedPosition * kotlin.math.PI * 2).toFloat() + 1f) / 2f
                        musicHapticsManager.processAmplitude(simulatedAmplitude)
                    }
                }
                delay(50) // Faster update for haptics
            }
        }
    }
    
    /**
     * Save current playback state for resume functionality.
     */
    private fun saveCurrentPlaybackState() {
        val state = _playerState.value
        val currentSong = state.currentSong ?: return
        val queue = state.queue
        
        if (queue.isEmpty()) return
        
        scope.launch {
            try {
                val queueJson = org.json.JSONArray().apply {
                    queue.forEach { song ->
                        put(org.json.JSONObject().apply {
                            put("id", song.id)
                            put("title", song.title)
                            put("artist", song.artist)
                            put("album", song.album ?: "")
                            put("thumbnailUrl", song.thumbnailUrl ?: "")
                            put("duration", song.duration)
                            put("source", song.source.name)
                        })
                    }
                }.toString()
                
                sessionManager.savePlaybackState(
                    songId = currentSong.id,
                    position = state.currentPosition,
                    queueJson = queueJson,
                    index = state.currentIndex
                )
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }
    
    /**
     * Preload next song's stream URL ahead of time for gapless playback.
     * Starts preloading ~15 seconds before current song ends.
     */
    private fun checkPreloadNextSong(currentPosition: Long, duration: Long) {
        if (isPreloading || duration <= 0) return
        
        val preloadStartMs = duration - 15000L // Start preloading 15 seconds before end
        if (currentPosition < preloadStartMs) return
        
        val state = _playerState.value
        val isVideoMode = state.isVideoMode
        var nextIndex = state.currentIndex + 1
        
        // Handle shuffle mode
        if (state.shuffleEnabled && state.queue.size > 1) {
            // For shuffle, we can't predict the next song, so skip preloading
            return
        }
        
        // Handle repeat/autoplay
        if (nextIndex >= state.queue.size) {
            if (state.repeatMode == RepeatMode.ALL) {
                nextIndex = 0
            } else if (state.isAutoplayEnabled && state.queue.isNotEmpty()) {
                nextIndex = 0 // Autoplay will loop
            } else {
                return // No next song
            }
        }
        
        val nextSong = state.queue.getOrNull(nextIndex) ?: return
        
        // Check if already preloaded
        // Important: check if preloaded type (audio/video) matches current mode? 
        // For simplicity, we just check ID. A mode switch usually clears preload.
        if (preloadedNextSongId == nextSong.id && preloadedStreamUrl != null) {
            return
        }
        
        isPreloading = true
        scope.launch {
            try {
                val streamUrl = when (nextSong.source) {
                    SongSource.LOCAL, SongSource.DOWNLOADED -> nextSong.localUri.toString()
                    SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(nextSong.id)
                    else -> {
                        if (isVideoMode) {
                            // Smart Video Matching for Preload
                            val videoId = resolvedVideoIds[nextSong.id] ?: youTubeRepository.getBestVideoId(nextSong).also { 
                                resolvedVideoIds.put(nextSong.id, it) 
                            }
                            youTubeRepository.getVideoStreamUrl(videoId)
                        } else {
                            youTubeRepository.getStreamUrl(nextSong.id)
                        }
                    }
                }
                
                if (streamUrl != null) {
                    preloadedNextSongId = nextSong.id
                    preloadedStreamUrl = streamUrl
                    
                    // Update the media item in the queue with resolved URL
                    updateNextMediaItemWithPreloadedUrl(nextIndex, nextSong, streamUrl)
                }
            } catch (e: Exception) {
                // Preload failed, will resolve on transition
            } finally {
                isPreloading = false
            }
        }
    }
    
    /**
     * Update the next media item in the player with the preloaded stream URL.
     */
    private fun updateNextMediaItemWithPreloadedUrl(index: Int, song: Song, streamUrl: String) {
        mediaController?.let { controller ->
            if (index < controller.mediaItemCount) {
                val newMediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(song.id)
                    .setCustomCacheKey(song.id) // CRITICAL: Stable cache key
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                            .build()
                    )
                    .build()
                
                // Replace the placeholder media item with resolved one
                try {
                    controller.removeMediaItem(index)
                    controller.addMediaItem(index, newMediaItem)
                } catch (e: Exception) {
                   // Index might have changed or race condition
                }
            }
        }
    }
    
    private var playJob: Job? = null

    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = 0, autoPlay: Boolean = true) {
        // Cancel any pending play request
        playJob?.cancel()
        
        // IMMEDIATELY pause current playback for instant response
        mediaController?.pause()
        
        // Reset preload state
        preloadedNextSongId = null
        preloadedStreamUrl = null
        isPreloading = false
        
        playJob = scope.launch {
            _playerState.update { 
                it.copy(
                    queue = queue,
                    currentIndex = startIndex,
                    currentSong = song,
                    isLoading = true
                )
            }
            
            try {
                _playerState.update { it.copy(isLoading = true) }
                
                val mediaItems = queue.mapIndexed { index, s -> createMediaItem(s, index == startIndex) }
                mediaController?.let { controller ->
                    controller.setMediaItems(mediaItems, startIndex, 0L)
                    controller.prepare()
                    if (autoPlay) {
                        controller.play()
                    }
                } ?: run {
                    _playerState.update { it.copy(error = "Music service not connected", isLoading = false) }
                }
            } catch (e: Exception) {
                // Ignore cancellations
                if (e is kotlinx.coroutines.CancellationException) throw e
                _playerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    private suspend fun createMediaItem(song: Song, resolveStream: Boolean = true): MediaItem {
        val uri = when (song.source) {
            SongSource.LOCAL, SongSource.DOWNLOADED -> song.localUri.toString()
            SongSource.JIOSAAVN -> {
                if (resolveStream) {
                    // Retry once if first attempt fails
                    jioSaavnRepository.getStreamUrl(song.id)
                        ?: run {
                            kotlinx.coroutines.delay(500)
                            jioSaavnRepository.getStreamUrl(song.id)
                        }
                        ?: ""
                } else {
                    song.streamUrl ?: ""
                }
            }
            else -> {
                if (resolveStream) {
                    if (_playerState.value.isVideoMode) {
                        val videoId = resolvedVideoIds[song.id] ?: youTubeRepository.getBestVideoId(song).also { 
                            resolvedVideoIds.put(song.id, it) 
                        }
                        youTubeRepository.getVideoStreamUrl(videoId) ?: ""
                    } else {
                        // Retry once if first attempt fails
                        youTubeRepository.getStreamUrl(song.id)
                            ?: run {
                                kotlinx.coroutines.delay(500)
                                youTubeRepository.getStreamUrl(song.id)
                            }
                            ?: ""
                    }
                } else {
                    "https://youtube.com/watch?v=${song.id}"
                }
            }
        }
        
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
            .setCustomCacheKey(song.id) // CRITICAL: Stable cache key
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
    }
    
    fun play() {
        mediaController?.play()
    }
    
    fun pause() {
        mediaController?.pause()
    }
    
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) pause() else play()
        }
    }
    
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }
    
    fun seekToNext() {
        val state = _playerState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        // Determine next index based on shuffle/repeat/order
        val nextIndex = if (state.shuffleEnabled) {
            // Ensure we don't pick the same song if queue > 1
            if (queue.size > 1) {
                var random = queue.indices.random()
                while (random == state.currentIndex) {
                    random = queue.indices.random()
                }
                random
            } else 0
        } else {
            state.currentIndex + 1
        }
        
        if (nextIndex in queue.indices) {
            playSong(queue[nextIndex], queue, nextIndex)
        } else {
            // End of queue logic
             if (state.repeatMode == RepeatMode.ALL) {
                 playSong(queue[0], queue, 0)
             } else if (state.isAutoplayEnabled) {
                 // Infinite Autoplay: The ViewModel automatically loads more songs when nearing the end.
                 // Check if queue has grown (new songs added by autoplay observer)
                 scope.launch {
                     delay(500) // Brief delay to allow autoplay to add songs
                     val updatedState = _playerState.value
                     val updatedQueue = updatedState.queue
                     val updatedIndex = updatedState.currentIndex
                     
                     // If queue has more songs now, play the next one
                     if (updatedIndex + 1 < updatedQueue.size) {
                         playSong(updatedQueue[updatedIndex + 1], updatedQueue, updatedIndex + 1)
                     } else if (updatedQueue.size > queue.size) {
                         // New songs were added, play the first new one
                         val newSongIndex = queue.size
                         if (newSongIndex < updatedQueue.size) {
                             playSong(updatedQueue[newSongIndex], updatedQueue, newSongIndex)
                         }
                     }
                     // If still no new songs, playback naturally stops (rare edge case)
                 }
             }
             // Else: Stop or do nothing
        }
    }
    
    fun seekToPrevious() {
        val state = _playerState.value
        // If played more than 3 seconds, restart current song
        if (state.currentPosition > 3000) {
            seekTo(0)
            return
        }
        
        val queue = state.queue
        if (queue.isEmpty()) return
        
        val prevIndex = if (state.shuffleEnabled) {
            if (queue.size > 1) {
                 var random = queue.indices.random() // Ideally we'd have a history stack
                 while (random == state.currentIndex) {
                     random = queue.indices.random()
                 }
                 random
             } else 0
        } else {
            state.currentIndex - 1
        }

        if (prevIndex in queue.indices) {
             playSong(queue[prevIndex], queue, prevIndex)
        } else {
            // If at start and repeat all is on, go to end? Or just stop.
            if (state.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
                val lastIndex = queue.lastIndex
                playSong(queue[lastIndex], queue, lastIndex)
            }
        }
    }
    
    fun setRepeatMode(mode: RepeatMode) {
        mediaController?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _playerState.update { it.copy(repeatMode = mode) }
    }
    
    fun toggleShuffle() {
        mediaController?.let { controller ->
            val newShuffleState = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newShuffleState
            _playerState.update { it.copy(shuffleEnabled = newShuffleState) }
        }
    }
    
    fun toggleRepeat() {
        val currentMode = _playerState.value.repeatMode
        val nextMode = when (currentMode) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }
    
    fun updateLikeStatus(isLiked: Boolean) {
        _playerState.update { it.copy(isLiked = isLiked, isDisliked = if (isLiked) false else it.isDisliked) }
    }
    
    fun updateDislikeStatus(isDisliked: Boolean) {
        _playerState.update { it.copy(isDisliked = isDisliked, isLiked = if (isDisliked) false else it.isLiked) }
    }
    
    fun updateDownloadState(state: DownloadState) {
        _playerState.update { it.copy(downloadState = state) }
    }
    
    fun updateDominantColor(color: Int) {
        _playerState.update { it.copy(dominantColor = color) }
    }
    
    fun getPlayer(): Player? = mediaController
    
    fun toggleAutoplay() {
        _playerState.update { it.copy(isAutoplayEnabled = !it.isAutoplayEnabled) }
    }
    
    /**
     * Set playback parameters (speed and pitch).
     */
    fun setPlaybackParameters(speed: Float, pitch: Float) {
        val clampedSpeed = speed.coerceIn(0.1f, 5.0f)
        val clampedPitch = pitch.coerceIn(0.1f, 5.0f)
        
        mediaController?.playbackParameters = androidx.media3.common.PlaybackParameters(clampedSpeed, clampedPitch)
        
        _playerState.update { 
            it.copy(
                playbackSpeed = clampedSpeed,
                pitch = clampedPitch
            ) 
        }
    }
    
    /**
     * Update audio format info (codec and bitrate) from the current track.
     * Called when tracks change or playback becomes ready.
     */
    private fun updateAudioFormatInfo() {
        val player = mediaController ?: return
        val tracks = player.currentTracks
        
        // Find audio track group and extract format
        var audioFormat: androidx.media3.common.Format? = null
        for (group in tracks.groups) {
            if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && group.isSelected) {
                // Get the selected format from this group
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        audioFormat = group.getTrackFormat(i)
                        break
                    }
                }
                if (audioFormat != null) break
            }
        }
        
        if (audioFormat == null) return
        
        // Extract codec from MIME type (e.g., "audio/opus" -> "opus")
        val mimeType = audioFormat.sampleMimeType ?: audioFormat.containerMimeType
        val codec = when {
            mimeType?.contains("opus") == true -> "opus"
            mimeType?.contains("mp4a") == true -> "aac"
            mimeType?.contains("aac") == true -> "aac"
            mimeType?.contains("mp3") == true -> "mp3"
            mimeType?.contains("mpeg") == true -> "mp3"
            mimeType?.contains("flac") == true -> "flac"
            mimeType?.contains("vorbis") == true -> "vorbis"
            mimeType?.contains("wav") == true -> "wav"
            mimeType?.contains("webm") == true -> "webm"
            else -> mimeType?.substringAfter("audio/")?.substringBefore(";")
        }
        
        // Extract bitrate (ExoPlayer provides it in bits per second, convert to kbps)
        val bitrateKbps = if (audioFormat.bitrate > 0) {
            audioFormat.bitrate / 1000
        } else {
            // Fallback: estimate typical bitrates for known codecs
            // YouTube uses VBR for Opus so ExoPlayer doesn't report bitrate
            when (codec) {
                "opus" -> 256  // YouTube Music typically uses ~256kbps Opus
                "aac" -> 256   // Fallback for AAC
                "mp3" -> 320   // Fallback for MP3
                "flac" -> null // Lossless, no bitrate shown
                else -> null
            }
        }
        
        _playerState.update { 
            it.copy(
                audioCodec = codec,
                audioBitrate = bitrateKbps
            )
        }
    }
    
    /**
     * Add songs to the end of the current queue.
     * Used for endless radio mode to continuously add recommendations.
     */
    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        
        scope.launch {
            val currentQueue = _playerState.value.queue.toMutableList()
            currentQueue.addAll(songs)
            
            _playerState.update { it.copy(queue = currentQueue) }
            
            // Add media items to player
            songs.forEach { song ->
                val mediaItem = createMediaItem(song, resolveStream = false)
                mediaController?.addMediaItem(mediaItem)
            }
        }
    }

    /**
     * Move an item within the queue.
     */
    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val queue = _playerState.value.queue.toMutableList()
        if (fromIndex !in queue.indices || toIndex !in queue.indices) return
        
        val item = queue.removeAt(fromIndex)
        queue.add(toIndex, item)
        
        _playerState.update { it.copy(queue = queue) }
        
        mediaController?.moveMediaItem(fromIndex, toIndex)
    }

    /**
     * Remove items from the queue.
     */
    fun removeFromQueue(indices: List<Int>) {
        if (indices.isEmpty()) return
        val sortedIndices = indices.sortedDescending()
        val queue = _playerState.value.queue.toMutableList()
        
        mediaController?.let { controller ->
            sortedIndices.forEach { index ->
                if (index in queue.indices) {
                    queue.removeAt(index)
                    controller.removeMediaItem(index)
                }
            }
            
            _playerState.update { 
                it.copy(
                    queue = queue,
                    currentIndex = controller.currentMediaItemIndex,
                    currentSong = if (controller.currentMediaItemIndex in queue.indices) queue[controller.currentMediaItemIndex] else null
                ) 
            }
        }
    }

    /**
     * Clear the current queue.
     */
    fun clearQueue() {
        mediaController?.clearMediaItems()
        _playerState.update { 
            it.copy(
                queue = emptyList(),
                currentIndex = -1,
                currentSong = null,
                isPlaying = false,
                currentPosition = 0
            ) 
        }
    }

    /**
     * Add songs to be played next (immediately after current song).
     */
    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        
        scope.launch {
            val currentIndex = _playerState.value.currentIndex
            // If nothing playing, just add to end (which is beginning)
            val targetIndex = if (currentIndex < 0) 0 else currentIndex + 1
            
            val currentQueue = _playerState.value.queue.toMutableList()
            // Safety check for index
            val safeIndex = targetIndex.coerceAtMost(currentQueue.size)
            
            currentQueue.addAll(safeIndex, songs)
            
            _playerState.update { it.copy(queue = currentQueue) }
            
            // Add media items to player
            songs.forEachIndexed { i, song ->
                val mediaItem = createMediaItem(song, resolveStream = false)
                mediaController?.addMediaItem(safeIndex + i, mediaItem)
            }
        }
    }
    
    /**
     * Toggle video mode for any song.
     * Searches YouTube for video if the song is not from YouTube.
     * Switches between audio-only and video playback while preserving position.
     */
    fun toggleVideoMode() {
        val state = _playerState.value
        val song = state.currentSong ?: return
        
        val currentPosition = mediaController?.currentPosition ?: 0L
        val wasPlaying = mediaController?.isPlaying == true
        val newVideoMode = !state.isVideoMode
        
        _playerState.update { it.copy(isLoading = true, isVideoMode = newVideoMode, videoNotFound = false) }
        
        scope.launch {
            try {
                val streamUrl = if (newVideoMode) {
                    // Switch to video stream
                    // 1. Determine Video ID
                    // Video ID determination with Smart Match
                    val videoId = if (song.source == SongSource.YOUTUBE) {
                         // Check cache or resolve smart match
                         resolvedVideoIds[song.id] ?: youTubeRepository.getBestVideoId(song).also { 
                             resolvedVideoIds.put(song.id, it) 
                         }
                    } else {
                        // For non-YouTube songs (JioSaavn), we can also try to find a video
                        resolvedVideoIds[song.id] ?: run {
                            // Search for video
                            val query = "${song.title} ${song.artist} official video"
                            try {
                                val results = youTubeRepository.search(query)
                                val bestMatch = results.firstOrNull()
                                bestMatch?.id?.also { resolvedVideoIds.put(song.id, it) }
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    
                    if (videoId != null) {
                        youTubeRepository.getVideoStreamUrl(videoId)
                    } else {
                        null
                    }
                } else {
                    // Switch back to audio stream - use original source logic
                    when (song.source) {
                        SongSource.LOCAL, SongSource.DOWNLOADED -> song.localUri.toString()
                        SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(song.id)
                        else -> youTubeRepository.getStreamUrl(song.id)
                    }
                }
                
                if (streamUrl == null) {
                    // Fallback - revert state
                    _playerState.update { 
                        it.copy(
                            isLoading = false, 
                            isVideoMode = if (newVideoMode) false else !newVideoMode,
                            videoNotFound = newVideoMode 
                        ) 
                    }
                    return@launch
                }
                
                val newMediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(song.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                            .build()
                    )
                    .build()
                
                mediaController?.let { controller ->
                    val currentIndex = controller.currentMediaItemIndex
                    if (currentIndex < controller.mediaItemCount) {
                        controller.replaceMediaItem(currentIndex, newMediaItem)
                        controller.prepare()
                        
                        // Seek to preserved position
                        controller.seekTo(currentPosition)
                        
                        if (wasPlaying) {
                            controller.play()
                        }
                    }
                }
                
                _playerState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("MusicPlayer", "Error toggling video mode", e)
                _playerState.update { it.copy(isLoading = false, isVideoMode = !newVideoMode, error = e.message) }
            }
        }
    }
    
    fun dismissVideoError() {
        _playerState.update { it.copy(videoNotFound = false) }
    }
    
    fun stop() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _playerState.update { 
            it.copy(
                currentSong = null,
                isPlaying = false,
                currentPosition = 0,
                duration = 0,
                queue = emptyList()
            ) 
        }
    }

    fun release() {
        positionUpdateJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        
        // Unregister device receiver
        deviceReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore if already unregistered
            }
            deviceReceiver = null
        }
    }
    
    /**
     * Convert a YouTube thumbnail URL to high resolution for better notification artwork quality.
     * Converts hqdefault, mqdefault, sddefault to maxresdefault format.
     */
    private fun getHighResThumbnail(url: String?): String? {
        return url?.let {
            when {
                it.contains("ytimg.com") -> it
                    .replace("hqdefault", "maxresdefault")
                    .replace("mqdefault", "maxresdefault")
                    .replace("sddefault", "maxresdefault")
                    .replace("default", "maxresdefault")
                    .replace(Regex("w\\d+-h\\d+"), "w544-h544")
                it.contains("lh3.googleusercontent.com") -> 
                    it.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
                      .replace(Regex("=s\\d+"), "=s544")
                else -> it
            }
        }
    }

    /**
     * Optimize bandwidth usage by disabling video tracks when the app is in the background.
     * To be called from MainActivity lifecycle.
     */
    fun optimizeBandwidth(isBackground: Boolean) {
        val player = mediaController ?: return
        val isVideoMode = _playerState.value.isVideoMode

        // Only act if we are in video mode (if audio mode, video track is likely not selected anyway)
        if (isVideoMode) {
            val parameters = player.trackSelectionParameters
            val newParameters = parameters.buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, isBackground)
                .build()
            
            player.trackSelectionParameters = newParameters
        }
    }
}
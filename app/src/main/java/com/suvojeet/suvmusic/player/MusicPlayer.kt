package com.suvojeet.suvmusic.player

import android.content.ComponentName
import android.content.Context
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
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
    private val sleepTimerManager: SleepTimerManager
) {
    
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
    
    init {
        connectToService()
        
        // Setup sleep timer callback
        sleepTimerManager.setOnTimerFinished {
            pause()
        }

        // Initial device scan
        updateAvailableDevices()
    }

    private fun updateAvailableDevices() {
        val devices = mutableListOf<OutputDevice>()
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        
        // Always add phone speaker as primary
        devices.add(OutputDevice("phone_speaker", "Phone Speaker", DeviceType.PHONE, true))

        audioDevices.forEach { device ->
            val type = when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, 
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceType.BLUETOOTH
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, 
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET -> DeviceType.HEADPHONES
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> return@forEach // Already added
                else -> DeviceType.UNKNOWN
            }
            
            // Add if not already present by type (simplified logic)
            if (devices.none { it.type == type && it.name == device.productName.toString() }) {
                devices.add(
                    OutputDevice(
                        id = device.id.toString(),
                        name = device.productName.toString().ifBlank { type.name },
                        type = type,
                        isSelected = false // Will be determined by current active route
                    )
                )
            }
        }

        _playerState.update { it.copy(availableDevices = devices) }
    }

    fun switchOutputDevice(device: OutputDevice) {
        // Switching output device programmatically is limited on Android without system permissions
        // Usually, we can only suggest or open system settings, or use MediaRouter for Cast
        // For local devices, Android handles it automatically when connected.
        // We will show the system output switcher if possible (MediaRouter)
        
        val mediaRouter = MediaRouter.getInstance(context)
        // This is a simplified approach to trigger the system's output switcher
        // In a real app, you might use MediaRouteActionProvider or similar
        
        _playerState.update { state ->
            val updatedDevices = state.availableDevices.map { 
                it.copy(isSelected = it.id == device.id)
            }
            state.copy(
                availableDevices = updatedDevices,
                selectedDevice = device
            )
        }
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
            }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val controller = mediaController ?: return@let
                val index = controller.currentMediaItemIndex
                val song = _playerState.value.queue.getOrNull(index)
                
                _playerState.update { 
                    it.copy(
                        currentSong = song,
                        currentIndex = index,
                        currentPosition = 0L,
                        duration = controller.duration.coerceAtLeast(0L),
                        isLiked = false,
                        downloadState = DownloadState.NOT_DOWNLOADED,
                        isVideoMode = false // Reset video mode on song change
                    )
                }
                
                // Add to recently played
                if (song != null) {
                    scope.launch {
                        sessionManager.addToRecentlyPlayed(song)
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
            _playerState.update { it.copy(isLoading = true) }
            
            // Resolve stream URL for the song based on source
            val streamUrl = when (song.source) {
                SongSource.LOCAL, SongSource.DOWNLOADED -> song.localUri.toString()
                SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(song.id) ?: return
                else -> youTubeRepository.getStreamUrl(song.id) ?: return
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
                // Replace current item with resolved stream and play
                if (index < controller.mediaItemCount) {
                     controller.replaceMediaItem(index, newMediaItem)
                     // If we are replacing the currently playing item that just started (position ~0),
                     // we might need to ensure it plays if it was paused or if replace pauses it.
                     // Usually replaceMediaItem keeps state, but explicit play() checks hurt nothing.
                     if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                         controller.prepare()
                     }
                     if (shouldPlay) {
                         controller.play()
                     }
                }
            }
        } catch (e: Exception) {
            _playerState.update { it.copy(error = e.message, isLoading = false) }
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
                    }
                }
                delay(500)
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
        if (preloadedNextSongId == nextSong.id && preloadedStreamUrl != null) {
            return
        }
        
        isPreloading = true
        scope.launch {
            try {
                val streamUrl = when (nextSong.source) {
                    SongSource.LOCAL, SongSource.DOWNLOADED -> nextSong.localUri.toString()
                    SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(nextSong.id)
                    else -> youTubeRepository.getStreamUrl(nextSong.id)
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
                controller.removeMediaItem(index)
                controller.addMediaItem(index, newMediaItem)
            }
        }
    }
    
    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = 0, autoPlay: Boolean = true) {
        // Reset preload state
        preloadedNextSongId = null
        preloadedStreamUrl = null
        isPreloading = false
        
        scope.launch {
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
                _playerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    private suspend fun createMediaItem(song: Song, resolveStream: Boolean = true): MediaItem {
        val uri = when (song.source) {
            SongSource.LOCAL, SongSource.DOWNLOADED -> song.localUri.toString()
            SongSource.JIOSAAVN -> {
                if (resolveStream) {
                    jioSaavnRepository.getStreamUrl(song.id) ?: ""
                } else {
                    song.streamUrl ?: ""
                }
            }
            else -> {
                if (resolveStream) {
                    youTubeRepository.getStreamUrl(song.id) ?: "https://youtube.com/watch?v=${song.id}"
                } else {
                    "https://youtube.com/watch?v=${song.id}"
                }
            }
        }
        
        return MediaItem.Builder()
            .setUri(uri)
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
                 // Mock Autoplay: Just pick a random song from queue to mimic 'radio'
                 if (queue.isNotEmpty()) {
                     val random = queue.indices.random()
                     playSong(queue[random], queue, random)
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
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }
    
    fun updateLikeStatus(isLiked: Boolean) {
        _playerState.update { it.copy(isLiked = isLiked) }
    }
    
    fun updateDownloadState(state: DownloadState) {
        _playerState.update { it.copy(downloadState = state) }
    }
    
    fun getPlayer(): Player? = mediaController
    
    fun toggleAutoplay() {
        _playerState.update { it.copy(isAutoplayEnabled = !it.isAutoplayEnabled) }
    }
    
    /**
     * Toggle video mode for YouTube songs.
     * Switches between audio-only and video playback while preserving position.
     */
    fun toggleVideoMode() {
        val state = _playerState.value
        val song = state.currentSong ?: return
        
        // Only works for YouTube songs
        if (song.source != SongSource.YOUTUBE) {
            return
        }
        
        val currentPosition = mediaController?.currentPosition ?: 0L
        val wasPlaying = mediaController?.isPlaying == true
        val newVideoMode = !state.isVideoMode
        
        _playerState.update { it.copy(isLoading = true, isVideoMode = newVideoMode) }
        
        scope.launch {
            try {
                val streamUrl = if (newVideoMode) {
                    // Switch to video stream
                    youTubeRepository.getVideoStreamUrl(song.id)
                } else {
                    // Switch back to audio stream
                    youTubeRepository.getStreamUrl(song.id)
                }
                
                if (streamUrl == null) {
                    // Fallback - revert state
                    _playerState.update { it.copy(isLoading = false, isVideoMode = !newVideoMode) }
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
    
    fun release() {
        positionUpdateJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
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
}
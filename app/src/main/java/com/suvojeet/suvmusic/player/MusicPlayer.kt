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
import com.suvojeet.suvmusic.core.model.DownloadState
import com.suvojeet.suvmusic.core.model.PlayerState
import com.suvojeet.suvmusic.core.model.RepeatMode
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.core.model.VideoQuality
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.service.MusicPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouteSelector
import com.suvojeet.suvmusic.core.model.DeviceType
import com.suvojeet.suvmusic.core.model.OutputDevice
import com.suvojeet.suvmusic.util.MusicHapticsManager
import com.suvojeet.suvmusic.util.TTSManager
import androidx.glance.appwidget.updateAll
import com.suvojeet.suvmusic.glance.SuvMusicWidget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around MediaController connected to MusicPlayerService.
 * This enables media notifications and proper audio focus handling.
 */
@Singleton
@OptIn(UnstableApi::class)
class MusicPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val sessionManager: SessionManager,
    private val sleepTimerManager: SleepTimerManager,
    private val listeningHistoryRepository: ListeningHistoryRepository,
    private val cache: androidx.media3.datasource.cache.Cache,
    @com.suvojeet.suvmusic.di.PlayerDataSource private val dataSourceFactory: androidx.media3.datasource.DataSource.Factory,
    private val musicHapticsManager: MusicHapticsManager,
    private val ttsManager: TTSManager,
    private val spatialAudioProcessor: SpatialAudioProcessor,
    private val nativeSpatialAudio: NativeSpatialAudio,
    private val streamingService: com.suvojeet.suvmusic.data.repository.youtube.streaming.YouTubeStreamingService
) {
    
    // ... (existing properties)

    // Caching
    private var cachingJob: Job? = null
    private var currentResolutionJob: Job? = null
    private var preloadJob: Job? = null
    private var ttsVolumeJob: Job? = null

    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val scope = CoroutineScope(
        Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("MusicPlayer", "Uncaught coroutine exception", throwable)
        }
    )
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    private var positionUpdateJob: Job? = null
    
    // Audio Manager for device detection
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Preloading state for gapless playback
    private var preloadedNextSongId: String? = null
    private var lastPreloadAttemptTime: Long = 0L
    private var preloadedStreamUrl: String? = null
    private var preloadedIsVideoMode: Boolean = false  // Track if preloaded URL is video or audio
    private var isPreloading = false
    
    // Track wall-clock time when current song started playing (for gapless guard)
    private var songPlayStartWallTime: Long = 0L
    
    // Track manually selected device ID to persist selection across refreshes
    private var manualSelectedDeviceId: String? = null
    private var manualSelectedDeviceName: String? = null
    private var lastManualSelectionTime: Long = 0L
    
    // Cache for resolved video IDs for non-YouTube songs (SongId -> VideoId)
    // Fix: Unbounded Memory Leak -> Use LruCache with max size 100
    private val resolvedVideoIds = android.util.LruCache<String, String>(100)
    
    // Listening history tracking
    private var currentSongStartTime: Long = 0L
    private var currentSongStartPosition: Long = 0L
    
    private var deviceReceiver: android.content.BroadcastReceiver? = null

    // Debounced device refresh — collapses bursts of Bluetooth/headset events
    // into a single refresh so the Main dispatcher doesn't pile up delay() jobs.
    private var deviceRefreshJob: Job? = null
    private fun scheduleDeviceRefresh(primaryDelayMs: Long = 1000L, secondaryDelayMs: Long = 3000L) {
        deviceRefreshJob?.cancel()
        deviceRefreshJob = scope.launch {
            delay(primaryDelayMs)
            updateAvailableDevices()
            // Second attempt after longer delay for slower devices
            delay(secondaryDelayMs)
            updateAvailableDevices()
        }
    }

    // Modern Audio Device Callback
    private val audioDeviceCallback = object : android.media.AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
            scheduleDeviceRefresh(primaryDelayMs = 1000L, secondaryDelayMs = 2000L)

            val btAdded = addedDevices?.any { d ->
                d.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                d.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (android.os.Build.VERSION.SDK_INT >= 31 && (
                    d.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    d.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    d.type == 30 /* TYPE_BLE_BROADCAST */
                ))
            } == true
            if (btAdded) {
                onBluetoothAudioConnected()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
            scheduleDeviceRefresh(primaryDelayMs = 500L, secondaryDelayMs = 2000L)
        }
    }

    private fun onBluetoothAudioConnected() {
        scope.launch {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val volumePercent = if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f
            if (volumePercent > 0.7f) {
                val targetVolume = (maxVolume * 0.5f).toInt()
                scope.launch {
                    var v = currentVolume
                    while (v > targetVolume) {
                        v--
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
                        delay(50)
                    }
                }
            }

            if (sessionManager.isBluetoothAutoplayEnabled()) {
                // Give the audio route a moment to settle before starting playback
                delay(800)
                val state = _playerState.value
                if (state.queue.isNotEmpty() && !state.isPlaying) {
                    play()
                }
            }
        }
    }
    
    // Error recovery retry tracking to prevent infinite loops
    private var errorRetryCount = 0
    private var errorRetrySongId: String? = null
    
    // Configurable Preloading
    private var nextSongPreloadingEnabled = true
    private var nextSongPreloadDelay = 3

    // Automix master switch (Playback Settings -> Automix). Gates automatic queue
    // extension when autoplay/radio is active. Default matches SessionManager.
    @Volatile private var automixEnabled: Boolean = true

    // Crossfade
    private val crossfadeController = CrossfadeController(scope)
    private var crossfadeMs: Int = 0
    @Volatile private var crossfadeTriggered: Boolean = false

    // Queue mutation lock — serializes toggleShuffle / playSong / radio-append races
    // that desync ExoPlayer internal state (see suvmusic_shuffle*.log).
    private val queueMutex = Mutex()
    
    init {
        // Initialize video quality from settings
        scope.launch {
            val quality = sessionManager.getVideoQuality()
            _playerState.update { it.copy(videoQuality = quality) }
        }

        // Listen for preloading settings
        scope.launch {
            sessionManager.nextSongPreloadingEnabledFlow.collect { nextSongPreloadingEnabled = it }
        }
        scope.launch {
            sessionManager.nextSongPreloadDelayFlow.collect { nextSongPreloadDelay = it }
        }
        scope.launch {
            sessionManager.crossfadeMsFlow.collect { crossfadeMs = it.coerceIn(0, 12000) }
        }
        scope.launch {
            sessionManager.automixEnabledFlow.collect { automixEnabled = it }
        }

        connectToService()
        
        // Setup sleep timer callback
        sleepTimerManager.setOnTimerFinished {
            pause()
        }
        sleepTimerManager.setOnFadeStep {
            scope.launch {
                val controller = mediaController ?: return@launch
                val nextVolume = (controller.volume - 0.05f).coerceIn(0f, 1f)
                controller.volume = nextVolume
            }
        }

        // Initial device scan on background thread
        scope.launch(Dispatchers.Default) {
            updateAvailableDevices()
        }
        
        // Register receiver for device changes
        registerDeviceReceiver()
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        // Update homescreen widget on state changes (filtered to avoid excessive updates from progress)
        scope.launch {
            _playerState.collect { state ->
                // Check if important state changed compared to last update
                // (We use a simple local variable to track the "significant" part of the state)
                val currentSignificantState = state.currentSong?.id to state.isPlaying to state.shuffleEnabled to state.repeatMode
                if (lastSignificantState != currentSignificantState) {
                    lastSignificantState = currentSignificantState
                    try {
                        com.suvojeet.suvmusic.glance.SuvMusicWidget().updateAll(context)
                    } catch (e: Exception) {
                        // Glance widget update can fail when no widget is bound,
                        // when the host process is busy, or on rare runtime errors.
                        // None should crash playback — log so we can spot patterns.
                        android.util.Log.w("MusicPlayer", "Widget updateAll failed: ${e.message}")
                    }
                }
            }
        }
    }

    private var lastSignificantState: Any? = null
    private var playbackSpeedRampJob: Job? = null

    private fun registerDeviceReceiver() {
        val filter = android.content.IntentFilter().apply {
            addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(android.content.Intent.ACTION_HEADSET_PLUG)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        
        deviceReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                // Debounce refresh so rapid connect/disconnect bursts don't stack coroutines.
                // Bluetooth autoplay + safe-volume ducking are handled by the AudioDeviceCallback,
                // which fires reliably without requiring BLUETOOTH_CONNECT permission.
                scheduleDeviceRefresh(primaryDelayMs = 2000L, secondaryDelayMs = 3000L)
            }
        }
        
        try {
            deviceReceiver?.let {
                androidx.core.content.ContextCompat.registerReceiver(
                    context, it, filter,
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayer", "registerReceiver failed", e)
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
        val isBluetoothActive = audioDevices.any { 
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (android.os.Build.VERSION.SDK_INT >= 31 && (
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || 
                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                it.type == 30 // TYPE_BLE_BROADCAST
            ))
        }
        val isWiredHeadsetConnected = audioDevices.any { 
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        val autoSelectPhone = !isBluetoothActive && !isWiredHeadsetConnected

        // 1. Add Phone Speaker as the primary internal output
        rawDevices.add(OutputDevice("phone_speaker", "Phone Speaker", DeviceType.PHONE, false))

        // 2. Add other devices
        audioDevices.forEach { device ->
            // Skip unwanted/internal devices
            // TYPE_REMOTE_SUBMIX is used for recording and shows up as a "device"
            // TYPE_TELEPHONY is for calls
            if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || 
                device.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ||
                device.type == AudioDeviceInfo.TYPE_TELEPHONY ||
                device.type == AudioDeviceInfo.TYPE_FM_TUNER ||
                device.type == AudioDeviceInfo.TYPE_TV_TUNER ||
                device.type == 15 || // TYPE_REMOTE_SUBMIX (API 23+)
                device.type == 18) { // TYPE_TELEPHONY
                return@forEach
            }

            val type = when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, 
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceType.BLUETOOTH
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, 
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY -> DeviceType.HEADPHONES
                AudioDeviceInfo.TYPE_HDMI,
                AudioDeviceInfo.TYPE_HDMI_ARC,
                AudioDeviceInfo.TYPE_HDMI_EARC -> DeviceType.SPEAKER
                AudioDeviceInfo.TYPE_AUX_LINE -> DeviceType.HEADPHONES
                else -> {
                    if (android.os.Build.VERSION.SDK_INT >= 31 && (
                        device.type == AudioDeviceInfo.TYPE_BLE_HEADSET || 
                        device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        device.type == 30 // TYPE_BLE_BROADCAST
                    )) {
                        DeviceType.BLUETOOTH
                    } else {
                        DeviceType.UNKNOWN
                    }
                }
            }
            
            // Skip unknown devices if they don't have a valid name to avoid "null" entries
            val deviceName = device.productName.toString().trim()
            if (type == DeviceType.UNKNOWN && (deviceName.isBlank() || deviceName.lowercase() == "null")) {
                return@forEach
            }

            val finalName = if (deviceName.isBlank() || deviceName.lowercase() == "null") {
                type.name.lowercase().replaceFirstChar { it.uppercase() }
            } else {
                deviceName
            }

            // Avoid duplicates by name (case-insensitive) and ID
            val isDuplicate = rawDevices.any { 
                it.id == device.id.toString() || 
                it.name.equals(finalName, ignoreCase = true)
            }

            if (!isDuplicate) {
                rawDevices.add(
                    OutputDevice(
                        id = device.id.toString(),
                        name = finalName,
                        type = type,
                        isSelected = false
                    )
                )
            }
        }

        // 3. Determine selection
        // Logic: specific manual selection > auto system selection
        
        var devicesWithSelection = rawDevices.map { device ->
            // Try to match by ID first, then by name if it's a recent manual selection
            // (IDs can change on some devices during routing handshakes)
            val isManualMatch = if (manualSelectedDeviceId != null) {
                device.id == manualSelectedDeviceId || 
                (System.currentTimeMillis() - lastManualSelectionTime < 10000 && 
                 device.name == manualSelectedDeviceName)
            } else false

            val isSelected = if (manualSelectedDeviceId != null) {
                isManualMatch
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
        var hasSelection = devicesWithSelection.any { it.isSelected }
        
        // Add a grace period (10 seconds) before clearing manual selection
        // This prevents flickering back to phone speaker while Bluetooth is still handshake-ing
        val isInGracePeriod = manualSelectedDeviceId != null && (System.currentTimeMillis() - lastManualSelectionTime < 10000)

        // If we are in grace period but device isn't in list, MANUALLY add it to the list
        // so the UI stays stable and shows it as "Connecting/Active".
        // Capture to locals to avoid race with concurrent nullification of the fields.
        val capturedId = manualSelectedDeviceId
        val capturedName = manualSelectedDeviceName
        if (!hasSelection && isInGracePeriod && capturedId != null && capturedName != null) {
            val placeholderDevice = OutputDevice(
                id = capturedId,
                name = capturedName,
                type = DeviceType.BLUETOOTH, // Assume Bluetooth for grace period issues
                isSelected = true
            )
            // Add at correct position or replace if same name exists
            val newList = devicesWithSelection.toMutableList()
            newList.add(placeholderDevice)
            devicesWithSelection = newList
            hasSelection = true
        }

        if (!hasSelection && !isInGracePeriod) {
            // Manual device lost or auto-logic failed -> Reset manual and use auto logic
            if (manualSelectedDeviceId != null) {
                manualSelectedDeviceId = null
                manualSelectedDeviceName = null
                
                // Also tell service to reset to default routing since manual device is gone
                mediaController?.sendCustomCommand(
                    androidx.media3.session.SessionCommand("SET_OUTPUT_DEVICE", android.os.Bundle.EMPTY),
                    android.os.Bundle().apply { putString("DEVICE_ID", "default") }
                )
                
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
        
        // If we are in grace period but device isn't in list, keep the "selected" state for the UI
        // so the user doesn't see it jump back immediately
        if (!hasSelection && isInGracePeriod) {
             // Just update available devices but don't clear manualSelectedDeviceId yet
        }

        
        val selectedDevice = devicesWithSelection.find { it.isSelected }
        _playerState.update { it.copy(availableDevices = devicesWithSelection, selectedDevice = selectedDevice) }
    }

    fun switchOutputDevice(device: OutputDevice) {
        // Update manual preference
        manualSelectedDeviceId = device.id
        manualSelectedDeviceName = device.name
        lastManualSelectionTime = System.currentTimeMillis()
        
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
                    // Bug Fix: Only clear errors on STATE_READY, not during STATE_BUFFERING
                    // which would wipe errors set by onPlayerError before user sees them
                    error = if (playbackState == Player.STATE_READY) null else it.error
                )
            }
            
            if (playbackState == Player.STATE_READY) {
                startPositionUpdates()
                // Update audio format info when playback is ready
                updateAudioFormatInfo()
            }
            
            // Bug Fix: Handle STATE_ENDED — ExoPlayer stopped because the current item
            // finished or failed to play (e.g. unresolved placeholder URI).
            // Without this, playback silently dies when songs have placeholder URIs.
            if (playbackState == Player.STATE_ENDED) {
                val controller = mediaController ?: return
                
                // Bug Fix: Only auto-skip if this was a REAL end, not a failed placeholder.
                // If it's a placeholder, the resolution coroutine will handle it.
                val currentUri = controller.currentMediaItem?.localConfiguration?.uri?.toString()
                val isPlaceholder = currentUri.isNullOrBlank() ||
                    currentUri.contains("placeholder.invalid") ||
                    currentUri.contains("youtube.com/watch") ||
                    currentUri.contains("youtu.be/")
                if (isPlaceholder) return

                val state = _playerState.value
                val nextIndex = controller.nextMediaItemIndex
                val queueSize = state.queue.size
                
                if (nextIndex != -1 && nextIndex != androidx.media3.common.C.INDEX_UNSET) {
                    // More songs in queue — player couldn't auto-transition (e.g. bad placeholder URI)
                    val nextSong = state.queue.getOrNull(nextIndex)
                    if (nextSong != null) {
                        scope.launch {
                            controller.seekTo(nextIndex, 0L)
                            resolveAndPlayCurrentItem(nextSong, nextIndex, shouldPlay = true)
                        }
                    }
                } else if (state.repeatMode == RepeatMode.ALL && queueSize > 0) {
                    // Wrap around to beginning of queue
                    val firstSong = state.queue.firstOrNull()
                    if (firstSong != null) {
                        scope.launch {
                            controller.seekTo(0, 0L)
                            resolveAndPlayCurrentItem(firstSong, 0, shouldPlay = true)
                        }
                    }
                } else if ((state.isAutoplayEnabled || state.isRadioMode) && automixEnabled) {
                    // Autoplay/Radio: Queue ended but more songs should be loaded.
                    // Wait for the observer to add more songs, then play the next one.
                    scope.launch {
                        val originalSize = queueSize
                        // Retry up to 6 seconds (12 x 500ms) to allow autoplay to add songs
                        repeat(12) {
                            delay(500)
                            val updatedState = _playerState.value
                            if (updatedState.queue.size > originalSize) {
                                val newIndex = originalSize
                                val newSong = updatedState.queue.getOrNull(newIndex)
                                if (newSong != null) {
                                    // Add new media items to player if needed
                                    val ctrl = mediaController ?: return@launch
                                    if (newIndex < ctrl.mediaItemCount) {
                                        ctrl.seekTo(newIndex, 0L)
                                    }
                                    resolveAndPlayCurrentItem(newSong, newIndex, shouldPlay = true)
                                }
                                return@launch
                            }
                        }
                        // Timeout: no new songs were added — playback stops
                        android.util.Log.w("MusicPlayer", "STATE_ENDED: autoplay timeout, no new songs loaded")
                    }
                }
                // RepeatMode.OFF at end of queue (no autoplay) — playback stops naturally (correct)
            }
        }
        
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            // Update audio format when tracks change
            updateAudioFormatInfo()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mode = when (repeatMode) {
                Player.REPEAT_MODE_OFF -> com.suvojeet.suvmusic.core.model.RepeatMode.OFF
                Player.REPEAT_MODE_ALL -> com.suvojeet.suvmusic.core.model.RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> com.suvojeet.suvmusic.core.model.RepeatMode.ONE
                else -> com.suvojeet.suvmusic.core.model.RepeatMode.OFF
            }
            _playerState.update { it.copy(repeatMode = mode) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playerState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Reset crossfade guard + restore volume for the incoming track.
            // Skip the volume restore while a crossfade is mid fade-in; otherwise we
            // overwrite the ramp and the new track jumps to full volume instantly.
            crossfadeTriggered = false
            if (!crossfadeController.isFadingIn) {
                mediaController?.let { c -> if (c.volume < 1f) c.volume = 1f }
            }

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
                
                // Rebuild accurate queue from player's media items to stay in sync
                // This is especially important when shuffle mode changes externally
                val accurateQueue = mutableListOf<Song>()
                val mediaItemCount = controller.mediaItemCount
                for (i in 0 until mediaItemCount) {
                    val mediaItemAtIndex = controller.getMediaItemAt(i)
                    val videoId = mediaItemAtIndex.mediaId
                    // Try to get song from current state first
                    var queueSong = _playerState.value.queue.firstOrNull { it.id == videoId }
                    if (queueSong == null && mediaItemAtIndex.mediaMetadata.title != null) {
                        // Create from metadata if not found in current state
                        val duration = if (controller.duration != androidx.media3.common.C.TIME_UNSET) controller.duration else 0L
                        queueSong = Song(
                            id = videoId,
                            title = mediaItemAtIndex.mediaMetadata.title.toString(),
                            artist = mediaItemAtIndex.mediaMetadata.artist.toString(),
                            album = mediaItemAtIndex.mediaMetadata.albumTitle?.toString() ?: "",
                            duration = duration,
                            thumbnailUrl = mediaItemAtIndex.mediaMetadata.artworkUri?.toString(),
                            source = SongSource.YOUTUBE // Assume YouTube as default for external
                        )
                    }
                    queueSong?.let { accurateQueue.add(it) }
                }
                
                // Reset error retry state on successful transition
                errorRetryCount = 0
                errorRetrySongId = null
                
                val previousSong = _playerState.value.currentSong
                _playerState.update { 
                    it.copy(
                        currentSong = song,
                        currentIndex = index,
                        currentPosition = 0L,
                        duration = controller.duration.coerceAtLeast(0L),
                        isLiked = false,
                        isDisliked = false,
                        downloadState = DownloadState.NOT_DOWNLOADED,
                        isVideoMode = it.isVideoMode,
                        videoNotFound = false, // Reset error flag on track change
                        queue = accurateQueue // Update with accurate queue from player
                    )
                }
                
                // Add to recently played and track listening history
                if (song != null) {
                    scope.launch {
                        sessionManager.addToRecentlyPlayed(song)
                        
                        // Speak Song Details (TTS) - Only if Bluetooth is connected
                        if (sessionManager.isSpeakSongDetailsEnabled()) {
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
                                // Duck volume
                                ttsVolumeJob?.cancel()
                                ttsVolumeJob = scope.launch {
                                    mediaController?.volume = 0.2f
                                    ttsManager.speak("Now playing ${song.title} by ${song.artist}")
                                    // Restore volume after delay (approx 3s)
                                    delay(3000)
                                    mediaController?.volume = 1.0f
                                }
                            }
                        }
                        
                        // Track previous song if it was playing
                        if (previousSong != null && currentSongStartTime > 0) {
                            val listenDuration = System.currentTimeMillis() - currentSongStartTime
                            val wasSkipped = listenDuration < (previousSong.duration * 0.5) // Skipped if < 50% listened
                            listeningHistoryRepository.recordPlay(previousSong, listenDuration, wasSkipped)
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
                    // - General placeholders: "https://placeholder.invalid/..."
                    // - JioSaavn/empty: null, empty, or doesn't look like a valid stream URL
                    val isYouTubePlaceholder = currentUri != null && (currentUri.contains("youtube.com/watch") || currentUri.contains("youtu.be"))
                    val isInvalidPlaceholder = currentUri != null && currentUri.contains("placeholder.invalid")
                    val isEmptyOrInvalid = currentUri.isNullOrBlank()
                    val needsResolution = isYouTubePlaceholder || isInvalidPlaceholder || isEmptyOrInvalid
                    
                    if (!needsResolution && currentUri != null) {
                        // Check if preloaded content mode matches current video mode
                        val modeMismatch = preloadedNextSongId == song.id &&
                            preloadedIsVideoMode != _playerState.value.isVideoMode
    
                        if (!modeMismatch) {
                            // Already has valid stream, just ensure UI state is correct and play
                            songPlayStartWallTime = System.currentTimeMillis()
                            _playerState.update { it.copy(isLoading = false) }
    
                            // Reset preload state as we've consumed it
                            preloadedNextSongId = null
                            preloadedStreamUrl = null
                            preloadedIsVideoMode = false
                            isPreloading = false
    
                            // Start aggressive caching for this preloaded/resolved song
                            if (song.source != SongSource.LOCAL && song.source != SongSource.DOWNLOADED) {
                                cachingJob?.cancel()
                                startAggressiveCaching(song.id, currentUri)
                            }
    
                            // Ensure playback continues for SEEK transitions (notification controls)
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                                controller.play()
                            }
                            return@let
                        }
                        // Mode mismatch — fall through to re-resolve with correct mode
                        android.util.Log.d("MusicPlayer", "Preloaded mode mismatch: preloaded=$preloadedIsVideoMode, current=${_playerState.value.isVideoMode}")
                    }
                    
                    // Check sleep timer (only for auto transitions)
                    val timerTriggered = if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        sleepTimerManager.onSongEnded()
                    } else {
                        false
                    }
                    
                    // CRITICAL FIX (Shuffle cascade prevention):
                    // When transitioning to an item with an unresolved placeholder URI,
                    // pause the player IMMEDIATELY. Without this, ExoPlayer tries to play
                    // the placeholder → SOURCE_ERROR → auto-advances to the next item
                    // (also a placeholder in shuffle mode) → another SOURCE_ERROR →
                    // cascade through the entire queue in seconds.
                    // Pausing stops ExoPlayer from producing the error while we resolve.
                    if (needsResolution) {
                        controller.pause()
                    }
                    
                    songPlayStartWallTime = System.currentTimeMillis()
                    
                    // Fast path: if we pre-resolved this song's URL during preloading
                    // (in shuffle mode, we cache the URL without calling replaceMediaItem
                    // to avoid disrupting ExoPlayer's internal state). Apply it now.
                    val capturedPreloadUrl = preloadedStreamUrl
                    if (needsResolution && preloadedNextSongId == song.id && capturedPreloadUrl != null) {
                        val cachedUrl = capturedPreloadUrl
                        val cachedIsVideo = preloadedIsVideoMode
                        preloadedNextSongId = null
                        preloadedStreamUrl = null
                        preloadedIsVideoMode = false
                        isPreloading = false
                        
                        currentResolutionJob?.cancel()
                        currentResolutionJob = scope.launch {
                            val cacheKey = if (cachedIsVideo) "${song.id}_${_playerState.value.videoQuality.name}" else song.id
                            val newMediaItem = MediaItem.Builder()
                                .setUri(cachedUrl)
                                .setMediaId(song.id)
                                .setCustomCacheKey(cacheKey)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(song.title)
                                        .setArtist(song.artist)
                                        .setAlbumTitle(song.album)
                                        .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                                        .setIsPlayable(true)
                                        .setIsBrowsable(false)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                        .build()
                                )
                                .build()
                            
                            if (index < controller.mediaItemCount && controller.getMediaItemAt(index).mediaId == song.id) {
                                controller.replaceMediaItem(index, newMediaItem)
                                if (index == controller.currentMediaItemIndex) {
                                    controller.prepare()
                                    if (!timerTriggered) controller.play()
                                }
                                _playerState.update { it.copy(isLoading = false, error = null) }
                                
                                if (song.source != SongSource.LOCAL && song.source != SongSource.DOWNLOADED) {
                                    cachingJob?.cancel()
                                    startAggressiveCaching(cacheKey, cachedUrl)
                                }
                            } else {
                                _playerState.update { it.copy(isLoading = false) }
                            }
                        }
                        return@let
                    }
                    
                    currentResolutionJob?.cancel()
                    currentResolutionJob = scope.launch {
                        // Check if service is already resolving this — avoid double resolution
                        // We check by re-reading the URI after a brief yield
                        if (needsResolution) {
                            kotlinx.coroutines.yield()
                            val freshUri = controller.currentMediaItem?.localConfiguration?.uri?.toString()
                            val stillNeedsResolution = freshUri.isNullOrBlank() ||
                                freshUri.contains("youtube.com/watch") ||
                                freshUri.contains("placeholder.invalid")
                            if (!stillNeedsResolution) {
                                _playerState.update { it.copy(isLoading = false) }
                                return@launch
                            }
                        }
                        
                        resolveAndPlayCurrentItem(song, index, shouldPlay = !timerTriggered)
                    }
                }
            }
        }
        
        override fun onPlayerError(error: PlaybackException) {
            // Log error
            android.util.Log.e("MusicPlayer", "Playback error: ${error.errorCodeName} (${error.errorCode})", error)
            
            // Check if error is recoverable
            val cause = error.cause
            val isHttpError = cause is androidx.media3.datasource.HttpDataSource.HttpDataSourceException
            val responseCode = (cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode ?: 0
            val isExpiredUrl = (isHttpError && (responseCode == 403 || responseCode == 410))
            val isNetworkError = cause is java.net.UnknownHostException || cause is java.net.SocketTimeoutException
            
            // Critical: Audio Sink or Decoder errors often cause the "No Sound" issue
            val isAudioSinkError = error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                                 error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
            val isDecoderError = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                               error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                               error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                               error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED
            // Parse errors are caused by double-resolution race (two replaceMediaItem calls mid-parse).
            // They need a simple re-resolve, NOT a mode-switch reset.
            val isParseError = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                               error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
            
            // Placeholder Check: If current URI is a placeholder, it MUST be resolved
            val currentUri = mediaController?.currentMediaItem?.localConfiguration?.uri?.toString()
            val isYouTubePlaceholder = currentUri != null && (currentUri.contains("youtube.com/watch") || currentUri.contains("youtu.be"))
            val isInvalidPlaceholder = currentUri != null && currentUri.contains("placeholder.invalid")

            if (isExpiredUrl || isNetworkError || isDecoderError || isAudioSinkError || isParseError || isYouTubePlaceholder || isInvalidPlaceholder) {
                // CRITICAL FIX (Shuffle cascade prevention):
                // Pause player immediately on placeholder errors to prevent ExoPlayer
                // from auto-advancing to the next item (which is also a placeholder
                // in shuffle mode), causing a rapid error cascade.
                if (isYouTubePlaceholder || isInvalidPlaceholder) {
                    mediaController?.pause()
                }
                
                // Bug Fix (Shuffle SOURCE_ERROR race condition):
                // When user presses Next with shuffle on, seekToNext() now pre-resolves
                // the placeholder BEFORE seeking. But if something slips through (e.g. pre-resolve
                // timed out), onMediaItemTransition already launched a currentResolutionJob.
                // If that job is still active here, launching ANOTHER recovery job causes a
                // double-resolution race: two coroutines both call replaceMediaItem() on the
                // same index, the second one clobbers the first mid-parse → ERROR_CODE_PARSING_*
                // (which then loops back into this function again — infinite retry spiral).
                // Guard: if resolution is already in-flight, skip launching a second one.
                if ((isYouTubePlaceholder || isInvalidPlaceholder) && currentResolutionJob?.isActive == true) {
                    android.util.Log.d("MusicPlayer", "Placeholder SOURCE_ERROR but resolution already in-flight — skipping duplicate recovery")
                    return
                }

                // Try to recover by re-resolving the stream URL
                val currentSong = _playerState.value.currentSong
                
                if (currentSong != null && currentSong.source != SongSource.LOCAL && currentSong.source != SongSource.DOWNLOADED) {
                    
                    if (currentSong.id == errorRetrySongId) {
                        errorRetryCount++
                    } else {
                        errorRetrySongId = currentSong.id
                        errorRetryCount = 1
                    }
                    
                    if (errorRetryCount > 3) {
                        android.util.Log.w("MusicPlayer", "Max retries reached for ${currentSong.id}, skipping")
                        errorRetryCount = 0
                        errorRetrySongId = null
                        
                        // In shuffle mode, don't blindly seekToNext() — every next item
                        // likely has an unresolved placeholder URI, causing a cascade of
                        // SOURCE_ERROR → skip → SOURCE_ERROR through the entire queue.
                        // Instead, just stop and show an error so the user can manually skip.
                        if (_playerState.value.shuffleEnabled) {
                            android.util.Log.w("MusicPlayer", "Shuffle mode: stopping instead of cascade-skipping")
                            _playerState.update { it.copy(error = "Could not play song. Tap next to skip.", isLoading = false) }
                            mediaController?.pause()
                        } else {
                            _playerState.update { it.copy(error = "Skipping unplayable song", isLoading = false) }
                            seekToNext()
                        }
                        return
                    }
                    
                    android.util.Log.d("MusicPlayer", "Attempting recovery (attempt $errorRetryCount/3) for: ${currentSong.id}")
                    
                    scope.launch {
                        // Improvement (2): Exponential Backoff
                        val backoffDelay = (800L * (1 shl (errorRetryCount - 1))).coerceAtMost(5000L)
                        delay(backoffDelay)
                        
                        // If Audio Sink or Decoder error occurred, switching modes often fixes it (Audio <-> Video)
                        // as it forces a complete reset of the decoder and audio track.
                        // NOTE: isParseError (3003/3004) is excluded — those are race condition artifacts,
                        // not real decoder failures. A simple re-resolve is all that's needed.
                        if (isAudioSinkError || isDecoderError) {
                            android.util.Log.d("MusicPlayer", "Audio/Decoder error detected, performing mode-switch reset")
                            
                            // Reset native audio state to prevent buffer/state corruption in transitions
                            spatialAudioProcessor.resetEqBands()
                            nativeSpatialAudio.reset()
                            
                            val originalMode = _playerState.value.isVideoMode
                            
                            // 1. Temporarily switch mode to force renderer reset
                            _playerState.update { it.copy(isVideoMode = !originalMode, isLoading = true) }
                            delay(500)
                            // 2. Switch back to original mode and re-resolve
                            _playerState.update { it.copy(isVideoMode = originalMode) }
                        } else if (_playerState.value.isVideoMode && (isExpiredUrl || isDecoderError)) {
                             // Fallback logic for video
                             android.util.Log.d("MusicPlayer", "Video playback failed, falling back to audio")
                             _playerState.update { 
                                 it.copy(
                                     isVideoMode = false, 
                                     videoNotFound = true,
                                     error = "Video unavailable, switching to audio..."
                                 ) 
                             }
                             resolvedVideoIds.remove(currentSong.id)
                        } else if (isExpiredUrl && currentSong.source == SongSource.YOUTUBE) {
                             // Improvement (2): 403 Forbidden Search Fallback
                             // Clear cached ID to force a fresh search resolution
                             resolvedVideoIds.remove(currentSong.id)
                             _playerState.update { it.copy(isLoading = true, error = "Stream expired, finding alternative...") }
                        } else {
                             _playerState.update { it.copy(isLoading = true, error = null) }
                        }

                        val resumePosition = _playerState.value.currentPosition
                        
                        try {
                             resolveAndPlayCurrentItem(currentSong, _playerState.value.currentIndex, shouldPlay = true)
                             if (resumePosition > 0) {
                                 delay(500) // Give player a moment to prepare
                                 mediaController?.seekTo(resumePosition)
                             }
                        } catch (e: Exception) {
                             _playerState.update { it.copy(error = "Playback failed: ${error.message}", isLoading = false) }
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

    fun setVideoQuality(quality: VideoQuality) {
        val currentQuality = _playerState.value.videoQuality
        if (currentQuality == quality) return
        
        _playerState.update { it.copy(videoQuality = quality) }
        
        // Update video resolution constraints
        mediaController?.let { player ->
            val maxResolution = quality.maxResolution
            // Constrain video size to the selected quality
            val params = player.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(maxResolution, maxResolution)
                .build()
            player.trackSelectionParameters = params
            android.util.Log.d("MusicPlayer", "Updated track selection: Max video size $maxResolution")
        }
        
        // Save to session and reload if needed
        scope.launch {
            sessionManager.setVideoQuality(quality)
            
            // If in video mode, reload stream to ensure correct quality constraints are applied
            if (_playerState.value.isVideoMode) {
                val state = _playerState.value
                state.currentSong?.let { song ->
                    resolveAndPlayCurrentItem(song, state.currentIndex, shouldPlay = state.isPlaying)
                }
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
            var audioStreamUrl: String? = null  // For dual-stream video (720p/1080p)
            var attempts = 0
            while (streamUrl == null && attempts < 2) { // Retry 1 time (total 2 attempts)
                val result = kotlinx.coroutines.withTimeoutOrNull(20_000L) { // Increased timeout
                    when (song.source) {
                        SongSource.LOCAL, SongSource.DOWNLOADED -> Pair(song.localUri.orEmpty(), null)
                        SongSource.JIOSAAVN -> Pair(jioSaavnRepository.getStreamUrl(song.id), null)
                        else -> {
                            if (_playerState.value.isVideoMode) {
                                // Smart Video Matching with dual-stream support for 720p/1080p
                                val videoId = resolvedVideoIds[song.id] ?: youTubeRepository.getBestVideoId(song).also { 
                                    resolvedVideoIds.put(song.id, it) 
                                }
                                // Use getVideoStreamResult for proper quality
                                val videoResult = youTubeRepository.getVideoStreamResult(videoId, _playerState.value.videoQuality)
                                if (videoResult != null) {
                                    android.util.Log.d("MusicPlayer", "Video stream: ${videoResult.resolution}, has separate audio: ${videoResult.audioUrl != null}")
                                    Pair(videoResult.videoUrl, videoResult.audioUrl)
                                } else {
                                    Pair(null, null)
                                }
                            } else {
                                Pair(youTubeRepository.getStreamUrl(song.id), null)
                            }
                        }
                    }
                }
                streamUrl = result?.first
                audioStreamUrl = result?.second
                if (streamUrl == null) {
                    attempts++
                    if (attempts < 2) delay(1000)
                }
            }

            // --- SEARCH FALLBACK ---
            // If direct resolution failed after retries, try searching for an alternative ID
            // This is a powerful recovery mechanism for "This video is not available" errors
            if (streamUrl == null && song.source == SongSource.YOUTUBE) {
                android.util.Log.d("MusicPlayer", "Direct resolution failed for ${song.id}, attempting search fallback...")
                try {
                    val fallbackId = youTubeRepository.getBestVideoId(song)
                    if (fallbackId != song.id) {
                        android.util.Log.d("MusicPlayer", "Found fallback ID: $fallbackId for original ID: ${song.id}. Resolving...")
                        val fallbackResult = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                            if (_playerState.value.isVideoMode) {
                                val videoResult = youTubeRepository.getVideoStreamResult(fallbackId)
                                Pair(videoResult?.videoUrl, videoResult?.audioUrl)
                            } else {
                                Pair(youTubeRepository.getStreamUrl(fallbackId), null)
                            }
                        }

                        if (fallbackResult?.first != null) {
                            android.util.Log.i("MusicPlayer", "Search fallback successful for ${song.id} -> $fallbackId")
                            streamUrl = fallbackResult.first
                            audioStreamUrl = fallbackResult.second
                            // Update resolved ID cache so we don't search again for this song session
                            resolvedVideoIds.put(song.id, fallbackId)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicPlayer", "Search fallback failed", e)
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
            
            val cacheKey = if (_playerState.value.isVideoMode) "${song.id}_${_playerState.value.videoQuality.name}" else song.id

            // Start aggressive caching in background
            if (song.source != SongSource.LOCAL && song.source != SongSource.DOWNLOADED) {
                startAggressiveCaching(cacheKey, streamUrl)
            }

            // Build MediaItem
            // For dual-stream video (720p/1080p), audio URL is passed via RequestMetadata extras
            // so the service-side DualStreamMediaSourceFactory can create MergingMediaSource.
            val finalUri = streamUrl
            
            android.util.Log.d("MusicPlayer", "Final URI: $finalUri, audioStreamUrl: $audioStreamUrl")
            
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(finalUri)
                .setMediaId(song.id)
                .setCustomCacheKey(cacheKey) // CRITICAL: Stable cache key
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(song.title)
                                            .setArtist(song.artist)
                                            .setAlbumTitle(song.album)
                                            .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                                            .setIsPlayable(true)
                                            .setIsBrowsable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                            .build()
                                    )            
            // Pass audio URL for dual-stream merging (video-only + audio-only)
            if (!audioStreamUrl.isNullOrEmpty()) {
                mediaItemBuilder.setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setExtras(android.os.Bundle().apply {
                            putString("audioStreamUrl", audioStreamUrl)
                        })
                        .build()
                )
            }
            
            val newMediaItem = mediaItemBuilder.build()
            
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
                             controller.prepare()
                             // Always restore position if valid (handles quality switch and error recovery)
                             if (oldPos > 0) controller.seekTo(oldPos)
                             
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
    private var bufferingStartWallTime = 0L
    private val MAX_BUFFERING_DURATION_BEFORE_DOWNSCALE = 3000L // 3 seconds
    private var hasTriedLowQualityForCurrent = false
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        saveCounter = 0
        
        // Optimization 2: Reactive Flow-based ticker.
        // We use a Flow to emit at intervals when the player is active.
        positionUpdateJob = scope.launch {
            kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(Unit)
                    // Adaptive delay: faster when playing for smooth seekbar, slower when paused/buffering
                    val isPlaying = mediaController?.isPlaying == true
                    kotlinx.coroutines.delay(if (isPlaying) 400L else 1000L)
                }
            }.collect {
                mediaController?.let { controller ->
                    val currentPos = controller.currentPosition.coerceAtLeast(0L)
                    val duration = controller.duration.coerceAtLeast(0L)
                    val bufferedPercentage = controller.bufferedPercentage
                    val playbackState = controller.playbackState

                    // Optimization 5: Buffer-aware speed adjustment.
                    // If the buffer is critically low (< 2%), we slow down slightly to avoid a hard pause.
                    if (playbackState == Player.STATE_READY && bufferedPercentage < 2 && controller.isPlaying) {
                        if (controller.playbackParameters.speed == _playerState.value.playbackSpeed) {
                           android.util.Log.d("MusicPlayer", "Buffer low ($bufferedPercentage%), adjusting speed to 0.95x")
                           controller.setPlaybackSpeed(_playerState.value.playbackSpeed * 0.95f)
                        }
                    } else if (controller.playbackParameters.speed != _playerState.value.playbackSpeed) {
                        // Restore speed when buffer recovers
                        controller.setPlaybackSpeed(_playerState.value.playbackSpeed)
                    }

                    if (playbackState == Player.STATE_BUFFERING) {
                        if (bufferingStartWallTime == 0L) bufferingStartWallTime = System.currentTimeMillis()
                        val bufferingDuration = System.currentTimeMillis() - bufferingStartWallTime
                        if (bufferingDuration > MAX_BUFFERING_DURATION_BEFORE_DOWNSCALE && !hasTriedLowQualityForCurrent) {
                            val currentQuality = sessionManager.getAudioQuality()
                            if (currentQuality == com.suvojeet.suvmusic.core.model.AudioQuality.AUTO) {
                                _playerState.value.currentSong?.let { song ->
                                    android.util.Log.i("MusicPlayer", "Buffering too long. Downscaling to LOW quality for ${song.title}")
                                    streamingService.clearCacheFor(song.id)
                                    hasTriedLowQualityForCurrent = true
                                    bufferingStartWallTime = 0L
                                    scope.launch {
                                        playSong(song, _playerState.value.queue, _playerState.value.currentIndex, forceLow = true)
                                    }
                                }
                            }
                        }
                    } else {
                        bufferingStartWallTime = 0L
                    }

                    val currentState = _playerState.value
                    val shouldUpdate = kotlin.math.abs(currentState.currentPosition - currentPos) > 500 ||
                                     currentState.duration != duration ||
                                     currentState.bufferedPercentage != bufferedPercentage

                    if (shouldUpdate) {
                        _playerState.update {
                            it.copy(
                                currentPosition = currentPos,
                                duration = duration,
                                bufferedPercentage = bufferedPercentage
                            )
                        }
                    }

                    // Save playback state periodically
                    saveCounter++
                    if (saveCounter >= 15) { // ~6 seconds at 400ms interval
                        saveCounter = 0
                        saveCurrentPlaybackState()
                    }
                    
                    // Check if we need to preload next song for gapless playback.
                    // Skip the early-transition seek when a crossfade is already in flight
                    // for this track — otherwise the gapless skip races the crossfade and
                    // the new track ends up at full volume before the fade-in plays.
                    if (sessionManager.isGaplessPlaybackEnabled()) {
                        checkPreloadNextSong(currentPos, duration)

                        // Early transition logic
                        val wallPlayTime = System.currentTimeMillis() - songPlayStartWallTime
                        if (!crossfadeTriggered && crossfadeMs == 0 &&
                            duration >= 10_000L && wallPlayTime >= 5_000L &&
                            currentPos >= duration - 1500 &&
                            preloadedNextSongId != null && preloadedStreamUrl != null) {
                            val state = _playerState.value
                            if (state.repeatMode != RepeatMode.ONE) {
                                val nextIndex = controller.nextMediaItemIndex
                                if (nextIndex != -1 && nextIndex != androidx.media3.common.C.INDEX_UNSET) {
                                    val nextMediaId = controller.getMediaItemAt(nextIndex).mediaId
                                    if (nextMediaId == preloadedNextSongId) {
                                        val nextUri = controller.getMediaItemAt(nextIndex).localConfiguration?.uri?.toString()
                                        if (!nextUri.isNullOrBlank() && !nextUri.contains("placeholder.invalid")) {
                                            controller.seekToNextMediaItem()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Crossfade: when within `crossfadeMs` of the end, fade song 1 out then
                    // fade song 2 in. Respects Repeat One, ignores near-zero durations, and
                    // only fires once per track.
                    if (crossfadeMs > 0 && !crossfadeTriggered && duration > (crossfadeMs + 1000)) {
                        val remaining = duration - currentPos
                        val hasNext = controller.hasNextMediaItem()
                        if (hasNext && remaining in 0..crossfadeMs.toLong() &&
                            _playerState.value.repeatMode != RepeatMode.ONE
                        ) {
                            crossfadeTriggered = true
                            crossfadeController.crossfadeTo(controller, crossfadeMs) {
                                mediaController?.seekToNextMediaItem()
                            }
                        }
                    }
                }
            }
        }

        // Separate lightweight haptics coroutine — no StateFlow updates
        scope.launch {
            while (true) {
                val controller = mediaController
                if (controller != null && _playerState.value.isPlaying) {
                    val currentPos = controller.currentPosition.coerceAtLeast(0L)
                    val beatPeriod = 500
                    val timeInBeat = currentPos % beatPeriod
                    val simulatedAmplitude = if (timeInBeat < 100) {
                        // Sharp decay from 1.0 to 0.0 over 100ms
                        1f - (timeInBeat / 100f)
                    } else {
                        0f
                    }
                    musicHapticsManager.processAmplitude(simulatedAmplitude)
                }
                delay(50) // haptics still at 50ms, but no StateFlow involved
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
        
        scope.launch(Dispatchers.IO) {
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
        if (!nextSongPreloadingEnabled || isPreloading || duration <= 0) return
        
        // Start preloading after configured delay (prevents churn during rapid skipping)
        if (currentPosition < (nextSongPreloadDelay * 1000L)) return
        
        // Throttle failed attempts (retry every 3 seconds)
        if (System.currentTimeMillis() - lastPreloadAttemptTime < 3000L) return
        
        val state = _playerState.value
        val isVideoMode = state.isVideoMode
        val controller = mediaController ?: return
        
        // Fix: If Repeat One is active, don't preload next song (we will loop current one)
        if (state.repeatMode == RepeatMode.ONE) {
            return
        }
        
        // Use Media3's nextMediaItemIndex which correctly handles shuffle, repeat, and boundaries.
        // Previous bug: fallback to (currentIndex + 1) was wrong in shuffle mode, and the manual
        // repeat wrap-around could override a valid shuffle index with 0.
        val nextIndex = controller.nextMediaItemIndex
        if (nextIndex == -1 || nextIndex == androidx.media3.common.C.INDEX_UNSET) return
        
        // Safety: verify index is within player's media item count
        if (nextIndex >= controller.mediaItemCount) return
        
        // Get the song by matching the media ID from the player (not from state.queue by index,
        // which may be stale or in a different order during shuffle transitions)
        val nextMediaId = controller.getMediaItemAt(nextIndex).mediaId
        val nextSong = state.queue.firstOrNull { it.id == nextMediaId } ?: return
        
        // Check if already preloaded
        // Important: check if preloaded type (audio/video) matches current mode? 
        // For simplicity, we just check ID. A mode switch usually clears preload.
        if (preloadedNextSongId == nextSong.id && preloadedStreamUrl != null) {
            return
        }
        
        isPreloading = true
        lastPreloadAttemptTime = System.currentTimeMillis()
        preloadJob = scope.launch {
            try {
                val streamUrl = when (nextSong.source) {
                    SongSource.LOCAL, SongSource.DOWNLOADED -> nextSong.localUri.orEmpty()
                    SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(nextSong.id)
                    else -> {
                        if (isVideoMode) {
                            // Smart Video Matching for Preload — use getVideoStreamResult to respect quality settings
                            val videoId = resolvedVideoIds[nextSong.id] ?: youTubeRepository.getBestVideoId(nextSong).also { 
                                resolvedVideoIds.put(nextSong.id, it) 
                            }
                            val videoResult = youTubeRepository.getVideoStreamResult(videoId, _playerState.value.videoQuality)
                            videoResult?.videoUrl ?: youTubeRepository.getVideoStreamUrl(videoId)
                        } else {
                            youTubeRepository.getStreamUrl(nextSong.id)
                        }
                    }
                }
                
                if (streamUrl != null) {
                    // CRITICAL FIX (Shuffle premature transition prevention):
                    // In shuffle mode, calling replaceMediaItem on the next item disrupts
                    // ExoPlayer's internal state, causing it to auto-transition away from
                    // the current song after only ~460ms. Instead, we just cache the URL
                    // and apply it at transition time via the fast-path in onMediaItemTransition.
                    //
                    // In non-shuffle mode, replace the item normally for true gapless playback.
                    if (!state.shuffleEnabled) {
                        updateNextMediaItemWithPreloadedUrl(nextIndex, nextSong, streamUrl)
                    }
                    preloadedNextSongId = nextSong.id
                    preloadedStreamUrl = streamUrl
                    preloadedIsVideoMode = isVideoMode
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
                    .setCustomCacheKey(
                    if (preloadedIsVideoMode) "${song.id}_${_playerState.value.videoQuality.name}" 
                    else song.id
                ) // CRITICAL: Stable cache key matching video/audio mode
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                            .setIsPlayable(true)
                            .setIsBrowsable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
                
                // Bug Fix: Use replaceMediaItem instead of remove+add to avoid
                // triggering a spurious media item transition event
                try {
                    controller.replaceMediaItem(index, newMediaItem)
                } catch (e: Exception) {
                   // Index might have changed or race condition
                }
            }
        }
    }
    
    private var playJob: Job? = null

    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = 0, autoPlay: Boolean = true, forceLow: Boolean = false) {
        // Cancel any pending play request
        playJob?.cancel()
        currentResolutionJob?.cancel()
        
        // Reset downscale tracking for new song (unless we ARE downscaling)
        if (!forceLow) {
            hasTriedLowQualityForCurrent = false
        }
        
        // IMMEDIATELY pause current playback for instant response
        mediaController?.pause()
        
        // Reset preload state
        preloadedNextSongId = null
        preloadedStreamUrl = null
        preloadedIsVideoMode = false
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
                
                // Optimization 1: Parallelize queue resolution.
                // Building the list of MediaItems can be slow for large queues.
                // Only the startIndex item actually performs network resolution here.
                val mediaItems = coroutineScope {
                    queue.mapIndexed { index, s ->
                        async {
                            createMediaItem(s, index == startIndex, forceLow = (index == startIndex && forceLow))
                        }
                    }.awaitAll()
                }

                queueMutex.withLock {
                    mediaController?.let { controller ->
                        // Cancel any in-flight crossfade so the incoming track starts at full volume.
                        crossfadeController.cancel()
                        crossfadeTriggered = false
                        if (controller.volume < 1f) controller.volume = 1f

                        controller.setMediaItems(mediaItems, startIndex, 0L)
                        controller.prepare()
                        if (autoPlay) {
                            controller.play()
                        }
                    } ?: run {
                        _playerState.update { it.copy(error = "Music service not connected", isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                // Ignore cancellations
                if (e is kotlinx.coroutines.CancellationException) throw e
                _playerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    private suspend fun createMediaItem(song: Song, resolveStream: Boolean = true, forceLow: Boolean = false): MediaItem {
        var audioStreamUrl: String? = null
        
        val uri = when (song.source) {
            SongSource.LOCAL, SongSource.DOWNLOADED -> song.localUri.orEmpty()
            SongSource.JIOSAAVN -> {
                if (resolveStream) {
                    // Retry once if first attempt fails
                    jioSaavnRepository.getStreamUrl(song.id)
                        ?: run {
                            kotlinx.coroutines.delay(500)
                            jioSaavnRepository.getStreamUrl(song.id)
                        }
                        ?: "https://placeholder.invalid/${song.id}"
                } else {
                    song.streamUrl ?: "https://placeholder.invalid/${song.id}"
                }
            }
            else -> {
                if (resolveStream) {
                    if (_playerState.value.isVideoMode) {
                        // Optimization 4: Parallelize video ID resolution and stream result fetching.
                        val videoResult = coroutineScope {
                            val videoIdDeferred = async {
                                resolvedVideoIds[song.id] ?: youTubeRepository.getBestVideoId(song).also { 
                                    resolvedVideoIds.put(song.id, it) 
                                }
                            }
                            
                            youTubeRepository.getVideoStreamResult(
                                videoIdDeferred.await(), 
                                _playerState.value.videoQuality, 
                                forceLow = forceLow
                            )
                        }
                        
                        if (videoResult != null) {
                            audioStreamUrl = videoResult.audioUrl
                            videoResult.videoUrl
                        } else {
                            "https://placeholder.invalid/${song.id}"
                        }
                    } else {
                        // Retry once if first attempt fails
                        youTubeRepository.getStreamUrl(song.id, forceLow = forceLow)
                            ?: run {
                                kotlinx.coroutines.delay(500)
                                youTubeRepository.getStreamUrl(song.id, forceLow = forceLow)
                            }
                            ?: "https://placeholder.invalid/${song.id}"
                    }
                } else {
                    "https://youtube.com/watch?v=${song.id}"
                }
            }
        }
        
        // Use video-quality-aware cache key when in video mode (matches resolveAndPlayCurrentItem)
        val cacheKey = if (_playerState.value.isVideoMode && resolveStream) {
            "${song.id}_${_playerState.value.videoQuality.name}"
        } else {
            song.id
        }
        
        val builder = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
            .setCustomCacheKey(cacheKey)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(song.title)
                                        .setArtist(song.artist)
                                        .setAlbumTitle(song.album)
                                        .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                                        .setIsPlayable(true)
                                        .setIsBrowsable(false)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                        .build()
                                )        
        // Pass audio URL for dual-stream merging (video-only + audio-only)
        if (!audioStreamUrl.isNullOrEmpty()) {
            builder.setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setExtras(android.os.Bundle().apply {
                        putString("audioStreamUrl", audioStreamUrl)
                    })
                    .build()
            )
        }
        
        return builder.build()
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
        currentResolutionJob?.cancel()
        // Bug Fix: Cancel any in-flight preload coroutine before we start our own resolution.
        // If preloadJob and our new coroutine both call replaceMediaItem(nextIndex, ...) concurrently,
        // the second one clobbers the first mid-parse → intermittent SOURCE_ERROR in shuffle mode.
        preloadJob?.cancel()
        isPreloading = false
        lastPreloadAttemptTime = 0L
        val state = _playerState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        val controller = mediaController ?: return

        // Use Media3's built-in logic to determine next index for both shuffle/linear/repeat
        val nextIndex = controller.nextMediaItemIndex
        
        if (nextIndex != -1 && nextIndex != androidx.media3.common.C.INDEX_UNSET && nextIndex in queue.indices) {
            // Bug Fix (Shuffle + Next skip flicker):
            // When shuffle is on, the next media item at nextIndex likely has a placeholder URI
            // (unresolved stream URL). Calling seekToNextMediaItem() immediately makes ExoPlayer
            // try to play it right away → STATE_ERROR(7) "Source error" before our resolution
            // coroutine from onMediaItemTransition can even start.
            //
            // Fix: Check if the next item needs resolution. If it does, resolve it FIRST
            // (updating the media item in-place), then seek to it. ExoPlayer will then find
            // a valid stream URL and won't emit a SOURCE_ERROR.
            val nextMediaItem = controller.getMediaItemAt(nextIndex)
            val nextUri = nextMediaItem.localConfiguration?.uri?.toString()
            val nextNeedsResolution = nextUri.isNullOrBlank() ||
                nextUri.contains("placeholder.invalid") ||
                nextUri.contains("youtube.com/watch") ||
                nextUri.contains("youtu.be/")

            if (nextNeedsResolution) {
                val nextSong = queue.firstOrNull { it.id == nextMediaItem.mediaId }
                if (nextSong != null) {
                    // Show loading and resolve BEFORE seeking so ExoPlayer never sees placeholder
                    _playerState.update { it.copy(isLoading = true) }
                    currentResolutionJob = scope.launch {
                        try {
                            val streamUrl = when (nextSong.source) {
                                SongSource.LOCAL, SongSource.DOWNLOADED -> nextSong.localUri.orEmpty()
                                SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(nextSong.id)
                                else -> {
                                    if (_playerState.value.isVideoMode) {
                                        val videoId = resolvedVideoIds[nextSong.id]
                                            ?: youTubeRepository.getBestVideoId(nextSong).also {
                                                resolvedVideoIds.put(nextSong.id, it)
                                            }
                                        val videoResult = youTubeRepository.getVideoStreamResult(videoId, _playerState.value.videoQuality)
                                        videoResult?.videoUrl ?: youTubeRepository.getVideoStreamUrl(videoId)
                                    } else {
                                        youTubeRepository.getStreamUrl(nextSong.id)
                                    }
                                }
                            }
                            if (streamUrl != null) {
                                // Update the media item with resolved URL before seeking.
                                // CRITICAL: We use controller.seekTo(nextIndex, 0L) instead of
                                // seekToNextMediaItem() + delay(80ms) because ExoPlayer processes
                                // its command queue SERIALLY — replaceMediaItem(nextIndex) is
                                // guaranteed to execute before seekTo(nextIndex). No timing guess needed.
                                //
                                // Bug Fix: Set preloaded state AFTER updateNextMediaItemWithPreloadedUrl.
                                // If the update fails, the item still has a placeholder URI — setting
                                // preloadedNextSongId first would fool the gapless trigger into firing.
                                updateNextMediaItemWithPreloadedUrl(nextIndex, nextSong, streamUrl)
                                preloadedNextSongId = nextSong.id
                                preloadedStreamUrl = streamUrl
                                preloadedIsVideoMode = _playerState.value.isVideoMode
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("MusicPlayer", "Pre-resolve for next song failed, falling back: ${e.message}")
                            // Fall through — onMediaItemTransition will handle resolution
                        }
                        // Seek using the captured nextIndex (not seekToNextMediaItem) so ExoPlayer
                        // is guaranteed to go exactly where we put the resolved media item.
                        controller.seekTo(nextIndex, 0L)
                    }
                    return
                }
            }
            // Next item already has a resolved URL — safe to seek immediately
            controller.seekToNextMediaItem()
        } else {
            // End of queue logic
             if (state.repeatMode == RepeatMode.ALL) {
                 playSong(queue[0], queue, 0)
             } else if ((state.isAutoplayEnabled || state.isRadioMode) && automixEnabled) {
                 // Infinite Autoplay/Radio: The ViewModel automatically loads more songs when nearing the end.
                 // Wait with retry loop for new songs to be added by the autoplay observer.
                 val originalQueueSize = queue.size
                 scope.launch {
                     // Retry up to 6 seconds (12 x 500ms) to allow autoplay to load songs
                     repeat(12) {
                         delay(500)
                         val updatedState = _playerState.value
                         val updatedQueue = updatedState.queue
                         
                         if (updatedQueue.size > originalQueueSize) {
                             // New songs were added, play the first new one
                             val newSongIndex = originalQueueSize
                             if (newSongIndex < updatedQueue.size) {
                                 playSong(updatedQueue[newSongIndex], updatedQueue, newSongIndex)
                             }
                             return@launch
                         }
                     }
                     // Timeout: no new songs loaded — playback stops
                     android.util.Log.w("MusicPlayer", "seekToNext: autoplay timeout, no new songs loaded after retries")
                 }
             }
             // Else: Stop or do nothing
        }
    }
    
    fun seekToPrevious() {
        val state = _playerState.value
        // If played more than 3 seconds, restart current song
        if (state.currentPosition > 3000) {
            currentResolutionJob?.cancel()
            seekTo(0)
            return
        }

        val queue = state.queue
        if (queue.isEmpty()) return
        val controller = mediaController ?: return

        // Wait for the in-flight resolution to actually finish cancelling before
        // we trigger seekToPreviousMediaItem — otherwise the still-running
        // resolution and the new transition's resolution race on
        // replaceMediaItem for overlapping indices (same double-resolution
        // class as the prior shuffle bug, but on user-driven Previous).
        scope.launch {
            currentResolutionJob?.cancelAndJoin()
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                val prevIndex = controller.previousMediaItemIndex
                if (prevIndex != -1 && prevIndex != androidx.media3.common.C.INDEX_UNSET && prevIndex in queue.indices) {
                    controller.seekToPreviousMediaItem()
                } else if (state.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
                    controller.seekTo(queue.lastIndex, 0L)
                }
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
        // Serialize against queue mutations (playSong / radio-append) so ExoPlayer's
        // internal timeline isn't reshuffled while another queue change is in-flight.
        scope.launch {
            queueMutex.withLock {
                mediaController?.let { controller ->
                    val newShuffleState = !controller.shuffleModeEnabled
                    // Do NOT seek — shuffle toggle should preserve current playback position.
                    controller.shuffleModeEnabled = newShuffleState
                    _playerState.update { it.copy(shuffleEnabled = newShuffleState) }
                }
            }
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
    
    fun updateRadioMode(isRadioMode: Boolean) {
        _playerState.update { it.copy(isRadioMode = isRadioMode) }
    }
    
    /**
     * Set playback parameters (speed and pitch).
     * Optimization 3: Smoothly ramp playback speed over 300ms to avoid audio artifacts.
     */
    fun setPlaybackParameters(speed: Float, pitch: Float) {
        val clampedSpeed = speed.coerceIn(0.1f, 5.0f)
        val clampedPitch = pitch.coerceIn(0.1f, 5.0f)
        
        playbackSpeedRampJob?.cancel()
        playbackSpeedRampJob = scope.launch {
            val currentSpeed = _playerState.value.playbackSpeed
            val steps = 15
            val duration = 300L
            val stepTime = duration / steps
            
            // Ramp speed smoothly
            for (i in 1..steps) {
                val interpolatedSpeed = currentSpeed + (clampedSpeed - currentSpeed) * (i.toFloat() / steps)
                mediaController?.setPlaybackSpeed(interpolatedSpeed)
                delay(stepTime)
            }
            
            // Final target speed
            mediaController?.playbackParameters = androidx.media3.common.PlaybackParameters(clampedSpeed, 1.0f)
            spatialAudioProcessor.setPlaybackParams(clampedPitch)
            
            _playerState.update { 
                it.copy(
                    playbackSpeed = clampedSpeed,
                    pitch = clampedPitch
                ) 
            }
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
        var bitrateKbps: Int? = if (audioFormat.bitrate > 0) {
            audioFormat.bitrate / 1000
        } else if (audioFormat.peakBitrate > 0) {
            audioFormat.peakBitrate / 1000
        } else if (audioFormat.averageBitrate > 0) {
            audioFormat.averageBitrate / 1000
        } else {
            null
        }

        // If ExoPlayer doesn't report it, try extracting actual quality from URL tags
        if (bitrateKbps == null) {
            val uriString = player.currentMediaItem?.localConfiguration?.uri?.toString() ?: ""
            
            // YouTube Music formats based on itag
            if (uriString.contains("youtube.com") || uriString.contains("googlevideo.com")) {
                val itagMatch = Regex("[?&]itag=(\\d+)").find(uriString)
                if (itagMatch != null) {
                    val itag = itagMatch.groupValues[1]
                    bitrateKbps = when (itag) {
                        "251" -> 160 // Opus High
                        "250" -> 64  // Opus Low
                        "249" -> 48  // Opus Very Low
                        "141", "256", "258" -> 256 // AAC High
                        "140" -> 128 // AAC Medium
                        "139" -> 48  // AAC Low
                        else -> null
                    }
                }
            }
            
            // For JioSaavn or others where Bitrate is often in the streaming URL
            if (bitrateKbps == null && (uriString.contains("jiosaavn.com") || uriString.contains("saavn.com"))) {
                bitrateKbps = when {
                    uriString.contains("320") -> 320
                    uriString.contains("160") -> 160
                    uriString.contains("96") -> 96
                    else -> null
                }
            }
            
            // For Downloaded/Local Files, try to extract from media metadata if available, but usually ExoPlayer handles local files.
        }

        // Final fallback if everything fails
        if (bitrateKbps == null) {
            bitrateKbps = when (codec) {
                "opus" -> 160  // YouTube Music high quality Opus is 160kbps, not 256
                "aac" -> 128   // Typical AAC quality
                "mp3" -> 320   
                "flac" -> null // Lossless, exact bitrate varies
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
            val existingIds = currentQueue.map { it.id }.toSet()
            
            // Final de-duplication check to prevent duplicates from concurrent calls
            val filteredSongs = songs.filter { it.id !in existingIds }
            if (filteredSongs.isEmpty()) return@launch
            
            currentQueue.addAll(filteredSongs)
            _playerState.update { it.copy(queue = currentQueue) }
            
            // Add media items to player
            filteredSongs.forEach { song ->
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
        val controller = mediaController ?: return

        // Capture the current playing index BEFORE mutating, and detect whether
        // any removed index is the one currently playing. After removeMediaItem
        // returns, controller.currentMediaItemIndex may still report the stale
        // pre-removal index (Media3's index update crosses an IPC boundary), so
        // computing currentSong directly off it produced a blank player UI when
        // the user swiped-to-remove the playing track.
        val originalCurrentIndex = controller.currentMediaItemIndex
        val currentWasRemoved = sortedIndices.any { it == originalCurrentIndex }

        sortedIndices.forEach { index ->
            if (index in queue.indices) {
                queue.removeAt(index)
                controller.removeMediaItem(index)
            }
        }

        if (queue.isEmpty()) {
            _playerState.update {
                it.copy(
                    queue = emptyList(),
                    currentIndex = -1,
                    currentSong = null,
                    isPlaying = false,
                )
            }
            return
        }

        // If the playing track was removed, Media3 auto-transitions to the
        // next track but we can't trust controller.currentMediaItemIndex
        // immediately after the IPC removal call. Land on a deterministic
        // "next" index (the same slot, or last if we removed the tail) and
        // let onMediaItemTransition correct us if it disagrees.
        val newIndex = if (currentWasRemoved) {
            originalCurrentIndex.coerceAtMost(queue.size - 1).coerceAtLeast(0)
        } else {
            // Current track survived; its position may have shifted left if any
            // earlier indices were removed.
            val shift = sortedIndices.count { it < originalCurrentIndex }
            (originalCurrentIndex - shift).coerceIn(0, queue.size - 1)
        }

        _playerState.update {
            it.copy(
                queue = queue,
                currentIndex = newIndex,
                currentSong = queue[newIndex],
            )
        }
    }

    /**
     * Clear the current queue.
     */
    fun replaceQueue(songs: List<Song>) {
        val controller = mediaController ?: return
        val currentMediaItem = controller.currentMediaItem ?: return
        val currentIndex = controller.currentMediaItemIndex

        scope.launch {
            // Find the currently playing song in the new ordering.
            // indexOfFirst returns -1 when the user removed/filtered the current
            // track out of the queue. Previously this was coerced to 0, which
            // kept the old current item playing but pinned it at logical index 0
            // of the new queue — the UI then showed song A playing while the
            // queue list said song B was current. Treat -1 as "current is no
            // longer in the queue" and replace the whole timeline.
            val foundIndex = songs.indexOfFirst { it.id == currentMediaItem.mediaId }

            if (foundIndex < 0) {
                // Current song dropped from the new queue. Replace the entire
                // controller timeline and start from the top of the new list.
                if (songs.isEmpty()) {
                    controller.clearMediaItems()
                    _playerState.update {
                        it.copy(
                            queue = emptyList(),
                            currentIndex = -1,
                            currentSong = null,
                            isPlaying = false,
                        )
                    }
                    return@launch
                }
                val newMediaItems = songs.map { createMediaItem(it, resolveStream = false) }
                controller.setMediaItems(newMediaItems, 0, 0L)
                controller.prepare()
                _playerState.update {
                    it.copy(
                        queue = songs,
                        currentIndex = 0,
                        currentSong = songs[0],
                    )
                }
                return@launch
            }

            val newIndexInSongs = foundIndex
            _playerState.update {
                it.copy(
                    queue = songs,
                    currentIndex = newIndexInSongs,
                    currentSong = songs[newIndexInSongs],
                )
            }

            // 1. Clear everything after the current item
            if (controller.mediaItemCount > currentIndex + 1) {
                controller.removeMediaItems(currentIndex + 1, controller.mediaItemCount)
            }

            // 2. Clear everything before the current item
            if (currentIndex > 0) {
                controller.removeMediaItems(0, currentIndex)
            }

            // Now only the current item is in the player at index 0.

            // 3. Add items before the current one in the new queue
            if (newIndexInSongs > 0) {
                val songsBefore = songs.subList(0, newIndexInSongs)
                val mediaItemsBefore = songsBefore.map { createMediaItem(it, resolveStream = false) }
                controller.addMediaItems(0, mediaItemsBefore)
            }

            // 4. Add items after the current one in the new queue
            if (newIndexInSongs < songs.size - 1) {
                val songsAfter = songs.subList(newIndexInSongs + 1, songs.size)
                val mediaItemsAfter = songsAfter.map { createMediaItem(it, resolveStream = false) }
                controller.addMediaItems(newIndexInSongs + 1, mediaItemsAfter)
            }
        }
    }
    
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
                var streamUrl: String? = null
                var audioStreamUrl: String? = null
                
                if (newVideoMode) {
                    // Switch to video stream with quality-aware dual-stream support
                    val videoId = if (song.source == SongSource.YOUTUBE) {
                         resolvedVideoIds[song.id] ?: youTubeRepository.getBestVideoId(song).also { 
                             resolvedVideoIds.put(song.id, it) 
                         }
                    } else {
                        resolvedVideoIds[song.id] ?: run {
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
                        // Use getVideoStreamResult for proper quality + dual-stream
                        val videoResult = youTubeRepository.getVideoStreamResult(videoId, _playerState.value.videoQuality)
                        if (videoResult != null) {
                            streamUrl = videoResult.videoUrl
                            audioStreamUrl = videoResult.audioUrl
                            android.util.Log.d("MusicPlayer", "Toggle video: ${videoResult.resolution}, dual-stream: ${videoResult.audioUrl != null}")
                        }
                    }
                } else {
                    // Switch back to audio stream - use original source logic
                    streamUrl = when (song.source) {
                        SongSource.LOCAL, SongSource.DOWNLOADED -> song.localUri.orEmpty()
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
                
                val cacheKey = if (newVideoMode) {
                    "${song.id}_${_playerState.value.videoQuality.name}"
                } else {
                    song.id
                }
                
                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(song.id)
                    .setCustomCacheKey(cacheKey) // Match cache key pattern from resolveAndPlayCurrentItem
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(getHighResThumbnail(song.thumbnailUrl)?.let { android.net.Uri.parse(it) })
                            .setIsPlayable(true)
                            .setIsBrowsable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                
                // Pass audio URL for dual-stream merging (video-only + audio-only)
                if (!audioStreamUrl.isNullOrEmpty()) {
                    mediaItemBuilder.setRequestMetadata(
                        MediaItem.RequestMetadata.Builder()
                            .setExtras(android.os.Bundle().apply {
                                putString("audioStreamUrl", audioStreamUrl)
                            })
                            .build()
                    )
                }
                
                val newMediaItem = mediaItemBuilder.build()
                
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
        
        // Cancel the entire coroutine scope to stop all launched coroutines
        // (flow collectors, preloading, caching, error-recovery, etc.)
        scope.cancel()
        
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        
        // Unregister modern device callback
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        
        // Unregister device receiver
        deviceReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Already unregistered — release() called twice or never registered.
                android.util.Log.w("MusicPlayer", "deviceReceiver was not registered at release()")
            }
            deviceReceiver = null
        }
    }
    
    /**
     * Convert a YouTube / Google thumbnail URL to high resolution for notification,
     * lock screen, Android Auto, and Wear artwork.
     *
     * Notification panels (especially on tablets) and Android Auto upscale the artwork
     * to ~720–1080px. The previous 544px cap looked soft compared to apps like
     * YouTube Music or Spotify; bumping the request to 1080 gives crisp art on
     * high-DPI panels while still being well under the bitmap-size limit MediaSession
     * accepts (~10MB / 4096px).
     */
    private fun getHighResThumbnail(url: String?): String? {
        return url?.let {
            when {
                it.contains("ytimg.com") || it.contains("youtube.com") -> it
                    .replace("hqdefault", "maxresdefault")
                    .replace("mqdefault", "maxresdefault")
                    .replace("sddefault", "maxresdefault")
                    // Path-aware replace: only upgrade the bare "default" thumbnail
                    // (e.g. "/default.jpg"); bare .replace("default",…) would corrupt
                    // "maxresdefault" → "maxresmaxresdefault".
                    .replace("/default.", "/maxresdefault.")
                    .replace(Regex("w\\d+-h\\d+"), "w1080-h1080")
                it.contains("lh3.googleusercontent.com") || it.contains("yt3.ggpht.com") || it.contains("googleusercontent.com") ->
                    it.replace(Regex("=w\\d+-h\\d+(-[a-z0-9]+)?"), "=w1080-h1080")
                      .replace(Regex("=s\\d+(-[a-z0-9]+)?"), "=s1080")
                      .replace(Regex("=w\\d+(-[a-z0-9]+)?"), "=w1080")
                else -> it.replace(Regex("w\\d+-h\\d+"), "w1080-h1080")
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

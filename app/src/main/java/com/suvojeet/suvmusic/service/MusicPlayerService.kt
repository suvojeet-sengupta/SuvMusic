package com.suvojeet.suvmusic.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.common.collect.ImmutableList
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.MainActivity
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Media3 MediaSessionService for background music playback.
 * Supports gapless playback and automix based on user settings.
 */
@AndroidEntryPoint
class MusicPlayerService : MediaLibraryService() {
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    @Inject
    @com.suvojeet.suvmusic.di.PlayerDataSource
    lateinit var dataSourceFactory: androidx.media3.datasource.DataSource.Factory
    
    @Inject
    lateinit var downloadRepository: DownloadRepository
    
    @Inject
    lateinit var localAudioRepository: LocalAudioRepository
    
    private var mediaLibrarySession: MediaLibrarySession? = null
    
    // Constants for Android Auto browsing
    private val ROOT_ID = "root"
    private val DOWNLOADS_ID = "downloads"
    private val LOCAL_ID = "local"
    
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var dynamicsProcessing: android.media.audiofx.DynamicsProcessing? = null
    
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // Initialize with defaults to avoid blocking main thread
        // Logic for gapless/automix is handled by player configuration and queue management
        
        // Ultra-fast buffer for instant playback
        // Minimum buffering = faster start (may rebuffer on slow networks)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,     // Min buffer: 5 seconds (aggressive)
                30_000,    // Max buffer: 30 seconds  
                500,       // Buffer for playback start: 0.5s (INSTANT!)
                1_500      // Buffer for rebuffer: 1.5 seconds
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
            
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            )
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                // Configure for gapless playback
                // When gapless is enabled, ExoPlayer will seamlessly transition between tracks
                // When disabled, there may be small gaps between tracks
                // ExoPlayer handles gapless automatically when media items are queued
                // Enabling pause at end is DISABLED for gapless playback
                pauseAtEndOfMediaItems = false
                
                // Add listener to attach Audio Normalization when audio session changes
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                            setupAudioNormalization(audioSessionId)
                        }
                    }
                })
            }
        
        // Observe Volume Normalization setting
        serviceScope.launch {
            sessionManager.volumeNormalizationEnabledFlow.collect { enabled ->
                // Apply smooth transition when setting changes
                 updateNormalizationEffect(enabled, animate = true)
            }
        }
        
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands
                    .buildUpon()
                    .add(androidx.media3.session.SessionCommand("SET_OUTPUT_DEVICE", android.os.Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.accept(
                    sessionCommands,
                    connectionResult.availablePlayerCommands
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: androidx.media3.session.SessionCommand,
                args: android.os.Bundle
            ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                if (customCommand.customAction == "SET_OUTPUT_DEVICE") {
                    val deviceId = args.getString("DEVICE_ID")
                    if (deviceId != null) {
                        val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                        val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                        
                        val targetDevice = if (deviceId == "phone_speaker") {
                            devices.find { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        } else {
                            devices.find { it.id.toString() == deviceId }
                        }
                        
                        val player = session.player
                        if (player is ExoPlayer) {
                            // If targetDevice is null, it clears the preference (default routing)
                            player.setPreferredAudioDevice(targetDevice)
                        }
                    }
                    return com.google.common.util.concurrent.Futures.immediateFuture(
                        androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
                    )
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
            
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): com.google.common.util.concurrent.ListenableFuture<LibraryResult<MediaItem>> {
                // The root item of the browser tree
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle("Root")
                            .build()
                    )
                    .build()
                return com.google.common.util.concurrent.Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): com.google.common.util.concurrent.ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                
                serviceScope.launch {
                    try {
                        val children = when (parentId) {
                            ROOT_ID -> {
                                ImmutableList.of(
                                    createBrowsableMediaItem(DOWNLOADS_ID, "Downloads"),
                                    createBrowsableMediaItem(LOCAL_ID, "Local Music")
                                )
                            }
                            DOWNLOADS_ID -> {
                                 // Fetch downloaded songs
                                 val songs = downloadRepository.downloadedSongs.value
                                 songs.map { song -> createPlayableMediaItem(song) }.toImmutableList()
                            }
                            LOCAL_ID -> {
                                // Fetch local songs (Async)
                                val songs = localAudioRepository.getAllLocalSongs()
                                songs.map { song -> createPlayableMediaItem(song) }.toImmutableList()
                            }
                            else -> ImmutableList.of()
                        }
                        future.set(LibraryResult.ofItemList(children, params))
                    } catch (e: Exception) {
                        future.setException(e)
                    }
                }
                return future
            }
            
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): com.google.common.util.concurrent.ListenableFuture<MutableList<MediaItem>> {
                 val updatedMediaItems = mediaItems.map { item ->
                    // If the item has a valid URI, keep it, otherwise try to resolve it
                     if (item.localConfiguration?.uri != null) {
                        item
                    } else {
                        // Logic to resolve MediaItem if needed, but we provide URIs in createPlayableMediaItem
                        // so this should be fine to just return as is or reload from repo if needed.
                        // For now return as is.
                        item
                    }
                }.toMutableList()
                
                return com.google.common.util.concurrent.Futures.immediateFuture(updatedMediaItems)
            }
        })
        .setSessionActivity(sessionActivityPendingIntent)
        .setBitmapLoader(CoilBitmapLoader(this))
        .build()
            
        // Customize the notification provider to ensure seekbar and controls work perfectly
        val notificationProvider = object : androidx.media3.session.DefaultMediaNotificationProvider(this) {
            override fun getMediaButtons(
                session: MediaSession,
                playerCommands: androidx.media3.common.Player.Commands,
                customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                showPauseButton: Boolean
            ): com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton> {
                // Ensure standard transport controls are used
                return super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
            }
        }
        setMediaNotificationProvider(notificationProvider)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        // Fix: Background Playback Termination -> Only stop if NOT playing
        if (player?.playWhenReady == false && player.mediaItemCount == 0) {
            stopSelf()
        }
    }
    
    private  var volumeNormalizationJob: kotlinx.coroutines.Job? = null

    private fun setupAudioNormalization(sessionId: Int) {
        serviceScope.launch {
            if (sessionManager.isVolumeNormalizationEnabled()) {
                // Determine if we need to create/reset effects for the new session
                // For new session, we apply instantly (no fade) to avoid dips at track start
                updateNormalizationEffect(enabled = true, animate = false, forcedSessionId = sessionId)
            }
        }
    }

    private fun updateNormalizationEffect(enabled: Boolean, animate: Boolean, forcedSessionId: Int? = null) {
        volumeNormalizationJob?.cancel()
        volumeNormalizationJob = serviceScope.launch {
            try {
                val player = mediaLibrarySession?.player as? ExoPlayer
                val sessionId = forcedSessionId ?: player?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
                
                if (sessionId == C.AUDIO_SESSION_ID_UNSET) return@launch

                if (enabled) {
                    // Create effects if they don't exist
                    if (loudnessEnhancer == null && dynamicsProcessing == null) {
                        createAudioEffects(sessionId)
                        // If animating, start from 0 gain
                        if (animate) setEffectGain(0f)
                    }

                    if (animate) {
                        // Smooth Ramp Up: 0f -> 1f over 500ms
                        val steps = 20
                        for (i in 0..steps) {
                            val progress = i / steps.toFloat()
                            setEffectGain(progress)
                            kotlinx.coroutines.delay(25) // Total ~500ms
                        }
                    } else {
                        setEffectGain(1f) // Instant
                    }
                } else {
                    // Disable: Smooth Ramp Down -> Release
                    if (loudnessEnhancer != null || dynamicsProcessing != null) {
                        if (animate) {
                            // Smooth Ramp Down: 1f -> 0f
                            val steps = 20
                            for (i in steps downTo 0) {
                                val progress = i / steps.toFloat()
                                setEffectGain(progress)
                                kotlinx.coroutines.delay(25)
                            }
                        }
                        releaseAudioEffects()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicPlayerService", "Error updating audio normalization effect", e)
                e.printStackTrace()
            }
        }
    }

    private fun createAudioEffects(sessionId: Int) {
        releaseAudioEffects() // Ensure clean state
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // API 28+: DynamicsProcessing (Limiter + Pre-Gain)
                val config = android.media.audiofx.DynamicsProcessing.Config.Builder(
                    android.media.audiofx.DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, // Assume stereo
                    false, 0, // preEq
                    false, 0, // mbc
                    false, 0, // postEq
                    true      // limiter
                )
                .setPreferredFrameDuration(10.0f)
                .build()

                dynamicsProcessing = android.media.audiofx.DynamicsProcessing(0, sessionId, config)
                
                dynamicsProcessing?.apply {
                    // Set Limiter Config (Fixed)
                    val limiterConfig = android.media.audiofx.DynamicsProcessing.Limiter(
                        true,   // inUse
                        true,   // enabled
                        0,      // linkGroup
                        10.0f,  // attackTimeMs
                        200.0f, // releaseTimeMs
                        4.0f,   // ratio
                        -3.0f,  // thresholdDb
                        0.0f    // postGainDb
                    )
                    setLimiterAllChannelsTo(limiterConfig)
                    enabled = true
                }
            } else {
                // API < 28: LoudnessEnhancer
                loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
                loudnessEnhancer?.enabled = true
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error creating audio effects", e)
            e.printStackTrace()
        }
    }

    private fun setEffectGain(progress: Float) {
        try {
            // Apply gain based on progress (0.0 -> 1.0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                dynamicsProcessing?.apply {
                    // Target: 5.5dB
                    // We interpolate linearly in dB domain for perceived smoothness
                    val targetDb = 5.5f * progress
                    setInputGainAllChannelsTo(targetDb)
                }
            } else {
                loudnessEnhancer?.apply {
                    // Target: 800mB
                    val targetmB = (800 * progress).toInt()
                    setTargetGain(targetmB)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error setting effect gain", e)
            e.printStackTrace()
        }
    }

    private fun releaseAudioEffects() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
            dynamicsProcessing = null
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error releasing audio effects", e)
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel() // Cancel scope
        releaseAudioEffects()

        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }
    private fun createBrowsableMediaItem(mediaId: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(title)
                    .build()
            )
            .build()
    }

    private fun createPlayableMediaItem(song: com.suvojeet.suvmusic.data.model.Song): MediaItem {
        // Use correct properties based on Song model
        val artworkUri = if (!song.thumbnailUrl.isNullOrEmpty()) {
            Uri.parse(song.thumbnailUrl)
        } else {
            null
        }
        
        val contentUri = song.localUri ?: if (song.streamUrl != null) Uri.parse(song.streamUrl) else Uri.EMPTY

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(artworkUri)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
            
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }
    
    private fun <T> List<T>.toImmutableList(): ImmutableList<T> {
        return ImmutableList.copyOf(this)
    }
}


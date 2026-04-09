package com.suvojeet.suvmusic.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioDeviceInfo
import android.media.AudioManager
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
import org.json.JSONArray
import org.json.JSONObject
import com.google.common.collect.ImmutableList
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.MainActivity
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.SponsorBlockRepository
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.log10

import androidx.media3.session.MediaNotification
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import androidx.media3.common.Player

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
    lateinit var youTubeRepository: com.suvojeet.suvmusic.data.repository.YouTubeRepository

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var localAudioRepository: LocalAudioRepository

    @Inject
    lateinit var sponsorBlockRepository: SponsorBlockRepository

    @Inject
    lateinit var lastFmManager: com.suvojeet.suvmusic.player.LastFmManager

    @Inject
    lateinit var listenTogetherManager: com.suvojeet.suvmusic.shareplay.ListenTogetherManager

    @Inject
    lateinit var audioARManager: com.suvojeet.suvmusic.player.AudioARManager

    @Inject
    lateinit var spatialAudioProcessor: com.suvojeet.suvmusic.player.SpatialAudioProcessor

    @Inject
    lateinit var sleepTimerManager: com.suvojeet.suvmusic.player.SleepTimerManager

    @Inject
    lateinit var listeningHistoryRepository: ListeningHistoryRepository

    private var mediaLibrarySession: MediaLibrarySession? = null
    
    // Custom Command Constants
    private val COMMAND_LIKE = "COMMAND_LIKE"
    private val COMMAND_REPEAT = "COMMAND_REPEAT"
    private val COMMAND_SHUFFLE = "COMMAND_SHUFFLE"
    private val COMMAND_START_RADIO = "COMMAND_START_RADIO"
    private val COMMAND_STOP_RADIO = "COMMAND_STOP_RADIO"
    
    // Constants for Android Auto browsing
    private val ROOT_ID = "root"
    private val HOME_ID = "home"
    private val LIBRARY_ID = "library"
    private val DOWNLOADS_ID = "downloads"
    private val LOCAL_ID = "local_music"
    private val ARTISTS_ID = "artists"
    private val ALBUMS_ID = "albums"
    private val LIKED_SONGS_ID = "liked_songs"
    private val PLAYLISTS_ID = "playlists"
    
    // Cache for Home Sections to handle "SECTION_Index" lookup
    private var cachedHomeSections: List<com.suvojeet.suvmusic.data.model.HomeSection> = emptyList()
    
    // Cache for search results so onSearch & onGetSearchResult stay consistent
    private var cachedSearchQuery: String? = null
    private var cachedSearchResults: List<com.suvojeet.suvmusic.core.model.Song> = emptyList()
    
    // Browse-level song cache: videoId -> Song, populated by createPlayableMediaItem
    private val cachedBrowseSongs: MutableMap<String, com.suvojeet.suvmusic.core.model.Song> = mutableMapOf()

    // Playlist context cache: songId -> ordered list of all songs in its parent playlist/section
    // Used by onAddMediaItems to queue up remaining songs for skip support in Android Auto
    private val playlistContextCache: MutableMap<String, List<com.suvojeet.suvmusic.core.model.Song>> = mutableMapOf()

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("MusicPlayerService", "Coroutine exception", throwable)
    }

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Main + 
        kotlinx.coroutines.SupervisorJob() + 
        exceptionHandler
    )
    
    private val serviceResolutionInProgress = java.util.concurrent.atomic.AtomicBoolean(false)
    
    private var sponsorBlockJob: kotlinx.coroutines.Job? = null
    
    private var audioSinkKickstartDone = false
    
    // Audio AR & Effects state
    private var isSpatialAudioActive = false
    private var isCurrentSongLiked = false

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = android.app.NotificationChannel(
                "media_playback_channel",
                "Media Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private var lastCustomLayout: List<CommandButton>? = null
    private var fadeJob: kotlinx.coroutines.Job? = null
    private val FADE_IN_DURATION_MS = 500L

    private fun updateCustomLayout() {
        val newLayout = getCustomLayout()
        if (lastCustomLayout != newLayout) {
            lastCustomLayout = newLayout
            mediaLibrarySession?.setCustomLayout(newLayout)
        }
    }

    private fun updateLikedState() {
        val currentSongId = mediaLibrarySession?.player?.currentMediaItem?.mediaId ?: return
        serviceScope.launch {
            try {
                // Check both YouTube and local history
                val likedSongs = youTubeRepository.getLikedMusic()
                isCurrentSongLiked = likedSongs.any { it.id == currentSongId }
                
                updateCustomLayout()
            } catch (e: Exception) {
                android.util.Log.e("MusicPlayerService", "Failed to update liked state", e)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        
        // Custom Notification Provider
        setMediaNotificationProvider(CustomNotificationProvider())
        
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000,  // minBufferMs
                50_000,  // maxBufferMs
                2_500,   // bufferForPlaybackMs
                5_000    // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(10_000, true) // Increase back buffer slightly for seeking back
            .build()
            
        val audioSink = androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(this)
            .setAudioProcessors(arrayOf(spatialAudioProcessor))
            .build()

        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink {
                return audioSink
            }
        }
        
        // Custom MediaSource.Factory that handles dual-stream merging (video-only + audio-only)
        // When a MediaItem has an "audioStreamUrl" in its RequestMetadata extras,
        // it creates a MergingMediaSource combining the video and audio sources.
        val defaultMediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
        val dualStreamMediaSourceFactory = object : androidx.media3.exoplayer.source.MediaSource.Factory {
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider): androidx.media3.exoplayer.source.MediaSource.Factory {
                defaultMediaSourceFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                return this
            }
            
            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy): androidx.media3.exoplayer.source.MediaSource.Factory {
                defaultMediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                return this
            }
            
            override fun getSupportedTypes(): IntArray {
                return defaultMediaSourceFactory.supportedTypes
            }
            
            override fun createMediaSource(mediaItem: MediaItem): androidx.media3.exoplayer.source.MediaSource {
                val audioStreamUrl = mediaItem.requestMetadata.extras?.getString("audioStreamUrl")
                
                if (!audioStreamUrl.isNullOrEmpty()) {
                    // Dual-stream: video-only + audio-only → MergingMediaSource
                    android.util.Log.d("MusicPlayerService", "Creating MergingMediaSource: video=${mediaItem.localConfiguration?.uri}, audio=$audioStreamUrl")
                    
                    val videoSource = defaultMediaSourceFactory.createMediaSource(mediaItem)
                    val audioMediaItem = MediaItem.Builder()
                        .setUri(audioStreamUrl)
                        .build()
                    val audioSource = defaultMediaSourceFactory.createMediaSource(audioMediaItem)
                    
                    return androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
                }
                
                // Normal single-stream (muxed or audio-only)
                return defaultMediaSourceFactory.createMediaSource(mediaItem)
            }
        }
        
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(dualStreamMediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Handle audio focus by default
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.apply {
            pauseAtEndOfMediaItems = false
            
            // Apply initial settings asynchronously
            serviceScope.launch {
                try {
                    val isOffloadEnabled = sessionManager.isAudioOffloadEnabled()
                    val isSpatialAudioPreferred = sessionManager.isAudioArEnabled()
                    val ignoreAudioFocus = sessionManager.isIgnoreAudioFocusDuringCallsEnabled()
                    
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (isOffloadEnabled && !isSpatialAudioPreferred && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            trackSelectionParameters = trackSelectionParameters.buildUpon()
                                .setAudioOffloadPreferences(
                                    androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                                        .setAudioOffloadMode(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                                        .setIsGaplessSupportRequired(false)
                                        .build()
                                )
                                .build()
                        }

                        if (ignoreAudioFocus) {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                    .setUsage(C.USAGE_MEDIA)
                                    .build(),
                                false // Don't handle audio focus
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicPlayerService", "Failed to apply initial settings", e)
                }
            }
            
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    updateCustomLayout()
                    mediaItem?.let { item ->
                        val videoId = item.mediaId
                        if (videoId.isNotEmpty()) {
                            sponsorBlockRepository.loadSegments(videoId)
                        }
                        updateLikedState()

                        // Fix for Auto-Advance: Resolve placeholder URIs lazily in the service
                        // This ensures it works even if the MusicPlayer client is not active.
                        val currentUri = item.localConfiguration?.uri?.toString()
                        val isYouTubePlaceholder = currentUri != null && (currentUri.contains("youtube.com/watch") || currentUri.contains("youtu.be"))
                        val isInvalidPlaceholder = currentUri != null && currentUri.contains("placeholder.invalid")
                        val isEmptyOrInvalid = currentUri.isNullOrBlank()
                        val needsResolution = isYouTubePlaceholder || isInvalidPlaceholder || isEmptyOrInvalid

                        if (needsResolution && videoId.isNotEmpty()) {
                            if (serviceResolutionInProgress.compareAndSet(false, true)) {
                                serviceScope.launch {
                                    try {
                                        val streamUrl = resolveStreamUrlWithRetry(videoId)
                                        if (streamUrl != null) {
                                            val updatedItem = item.buildUpon()
                                                .setUri(Uri.parse(streamUrl))
                                                .setMediaMetadata(
                                                    item.mediaMetadata.buildUpon()
                                                        .setIsPlayable(true)
                                                        .setIsBrowsable(false)
                                                        .build()
                                                )
                                                .build()

                                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                val p = mediaLibrarySession?.player ?: return@withContext
                                                val index = p.currentMediaItemIndex
                                                if (index != -1 && p.getMediaItemAt(index).mediaId == videoId) {
                                                    p.replaceMediaItem(index, updatedItem)
                                                    p.prepare()
                                                    if (p.playWhenReady) p.play()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MusicPlayerService", "Transition resolution failed for $videoId", e)
                                    } finally {
                                        serviceResolutionInProgress.set(false)
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    updateCustomLayout()
                    audioARManager.setPlaying(isPlaying)
                    if (isPlaying) {
                        startSponsorBlockMonitoring()
                    } else {
                        sponsorBlockJob?.cancel()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    updateCustomLayout()
                    
                    // Bug Fix: Some devices have a "Silent Handshake" issue where AudioTrack 
                    // is ready but doesn't produce sound until gain is updated.
                    if (playbackState == Player.STATE_READY) {
                        if (!audioSinkKickstartDone) {
                            startFadeIn()
                            audioSinkKickstartDone = true
                        }
                    } else if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE) {
                         audioSinkKickstartDone = false
                    }

                    // Only auto-skip if this was a REAL end, not a failed placeholder
                    // If it's a placeholder, the resolution coroutine will handle it
                    if (playbackState == Player.STATE_ENDED) {
                        val p = mediaLibrarySession?.player ?: return
                        val currentUri = p.currentMediaItem?.localConfiguration?.uri?.toString()
                        val isPlaceholder = currentUri.isNullOrBlank() ||
                            currentUri.contains("placeholder.invalid") ||
                            currentUri.contains("youtube.com/watch") ||
                            currentUri.contains("youtu.be/")

                        if (!isPlaceholder && p.hasNextMediaItem()) {
                            p.seekToNext()
                            p.prepare()
                            p.play()
                        }
                    }
                }

                private fun startFadeIn() {
                    fadeJob?.cancel()
                    fadeJob = serviceScope.launch {
                        val p = mediaLibrarySession?.player ?: return@launch
                        val originalVolume = 1.0f
                        var currentFadeVolume = 0.0f
                        
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            p.volume = 0.0f
                        }
                        
                        val steps = 10
                        val stepDelay = FADE_IN_DURATION_MS / steps
                        val volumeStep = originalVolume / steps
                        
                        for (i in 1..steps) {
                            delay(stepDelay)
                            currentFadeVolume += volumeStep
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                p.volume = currentFadeVolume.coerceAtMost(originalVolume)
                            }
                        }
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            p.volume = originalVolume
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    super.onPlayerError(error)
                    android.util.Log.e("MusicPlayerService", "Playback error: ${error.message}", error)
                    
                    // Recovery for AudioSink issues (common on Bluetooth switches)
                    // Check if the error is related to audio track initialization or sink issues
                    val isAudioSinkError = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                                         error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
                    
                    if (isAudioSinkError) {
                        android.util.Log.i("MusicPlayerService", "Detected AudioSink error, attempting recovery...")
                        serviceScope.launch {
                            delay(500)
                            val p = mediaLibrarySession?.player as? androidx.media3.exoplayer.ExoPlayer ?: return@launch
                            val wasPlaying = p.playWhenReady
                            p.prepare() 
                            if (wasPlaying) p.play()
                        }
                        return
                    }

                    // Only auto-skip on REAL errors, not placeholder URI failures.
                    // Placeholder failures are handled by the resolution coroutine in MusicPlayer.
                    val currentUri = mediaLibrarySession?.player
                        ?.currentMediaItem?.localConfiguration?.uri?.toString()
                    val isPlaceholder = currentUri.isNullOrBlank() ||
                        currentUri.contains("placeholder.invalid") ||
                        currentUri.contains("youtube.com/watch") ||
                        currentUri.contains("youtu.be/")

                    if (isPlaceholder) return // Resolution coroutine handles this

                    // Format/parse errors on a real URL = double-resolution race condition artifact.
                    // MusicPlayer's own recovery coroutine will re-resolve and fix it.
                    // DO NOT skip here — that would cause the auto-slip chain.
                    val isFormatError = error.errorCode ==
                        androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                        error.errorCode ==
                        androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
                    if (isFormatError) return

                    // Basic error recovery: skip to next on failure
                    serviceScope.launch {
                        delay(2000)
                        val p = mediaLibrarySession?.player ?: return@launch
                        if (p.hasNextMediaItem()) {
                            p.seekToNext()
                            p.prepare()
                            p.play()
                        }
                    }
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    super.onPlayWhenReadyChanged(playWhenReady, reason)
                    // If playWhenReady was changed due to audio focus loss/gain
                    if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) {
                        serviceScope.launch {
                            val autoResume = sessionManager.isAutoResumeAfterCallEnabled()
                            android.util.Log.d("MusicPlayerService", "onPlayWhenReadyChanged: playWhenReady=$playWhenReady, autoResume=$autoResume")
                            if (!playWhenReady) {
                                // Focus lost: if auto-resume is disabled, permanently pause so it won't resume later
                                if (!autoResume) {
                                    android.util.Log.d("MusicPlayerService", "Focus lost. Auto-resume is disabled by user. Forcing permanent pause.")
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        mediaLibrarySession?.player?.pause()
                                    }
                                }
                            } else {
                                // Focus regained: Media3 automatically restores playWhenReady to true.
                                android.util.Log.d("MusicPlayerService", "Focus regained. playWhenReady is now true. Auto-resume is $autoResume.")
                            }
                        }
                    }
                }

                override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                    super.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
                    if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                        serviceScope.launch {
                            if (sessionManager.isAutoResumeAfterCallEnabled()) {
                                android.util.Log.d("MusicPlayerService", "Suppression removed. Ensuring playback resumes.")
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    mediaLibrarySession?.player?.play()
                                }
                            }
                        }
                    }
                }
                
                override fun onRepeatModeChanged(repeatMode: Int) {
                    updateCustomLayout()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    updateCustomLayout()
                }
            })
        }
            
        lastFmManager.setPlayer(player)
        listenTogetherManager.setPlayer(player)
        
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                sessionManager.volumeNormalizationEnabledFlow,
                sessionManager.volumeBoostEnabledFlow,
                sessionManager.volumeBoostAmountFlow,
                sessionManager.audioArEnabledFlow
            ) { args: Array<Any?> ->
                AudioEffectsState(
                    normEnabled = args[0] as Boolean,
                    boostEnabled = args[1] as Boolean,
                    boostAmount = args[2] as Int,
                    audioArEnabled = args[3] as Boolean
                )
            }.collect { state ->
                 isSpatialAudioActive = state.audioArEnabled
                 spatialAudioProcessor.setSpatialEnabled(state.audioArEnabled)
                 spatialAudioProcessor.setLimiterConfig(
                     boostEnabled = state.boostEnabled,
                     boostAmount = state.boostAmount,
                     normEnabled = state.normEnabled
                 )
                 
                 // Improvement (4): Dynamic Audio Offload
                 // Offload is incompatible with software processors (Spatial, Limiter, EQ).
                 // Disable it when any effect is active to ensure the processors are used.
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                     val player = mediaLibrarySession?.player as? ExoPlayer
                     if (player != null) {
                         val isAnyEffectActive = state.audioArEnabled || state.boostEnabled || state.normEnabled
                         val offloadMode = if (isAnyEffectActive) {
                             androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                         } else {
                             androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                         }
                         
                         player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                             .setAudioOffloadPreferences(
                                 androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                                     .setAudioOffloadMode(offloadMode)
                                     .setIsGaplessSupportRequired(false)
                                     .build()
                             )
                             .build()
                     }
                 }
            }
        }

        serviceScope.launch {
            sessionManager.eqEnabledFlow.collect { enabled ->
                spatialAudioProcessor.setEqEnabled(enabled)
            }
        }

        serviceScope.launch {
            sessionManager.eqBandsFlow.collect { bands ->
                bands.forEachIndexed { index, gain ->
                    spatialAudioProcessor.setEqBand(index, gain)
                }
            }
        }

        serviceScope.launch {
            sessionManager.eqPreampFlow.collect { gain ->
                spatialAudioProcessor.setEqPreamp(gain)
            }
        }

        serviceScope.launch {
            sessionManager.bassBoostFlow.collect { strength ->
                spatialAudioProcessor.setBassBoost(strength)
            }
        }

        serviceScope.launch {
            sessionManager.virtualizerFlow.collect { strength ->
                spatialAudioProcessor.setVirtualizer(strength)
            }
        }

        serviceScope.launch {
            sessionManager.ignoreAudioFocusDuringCallsFlow.collect { ignoreFocus ->
                val player = mediaLibrarySession?.player as? ExoPlayer ?: return@collect
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    !ignoreFocus
                )
            }
        }
        
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, object : MediaLibrarySession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(SessionCommand(COMMAND_LIKE, android.os.Bundle.EMPTY))
                    .add(SessionCommand(COMMAND_REPEAT, android.os.Bundle.EMPTY))
                    .add(SessionCommand(COMMAND_SHUFFLE, android.os.Bundle.EMPTY))
                    .add(SessionCommand(COMMAND_START_RADIO, android.os.Bundle.EMPTY))
                    .add(SessionCommand(COMMAND_STOP_RADIO, android.os.Bundle.EMPTY))
                    .add(androidx.media3.session.SessionCommand("SET_OUTPUT_DEVICE", android.os.Bundle.EMPTY))
                    .build()
                
                // Fix for Android Auto: Grant all player commands to ensure controls are visible.
                // Media3's default onConnect might be too restrictive for external controllers.
                val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .add(Player.COMMAND_SET_REPEAT_MODE)
                    .add(Player.COMMAND_SET_SHUFFLE_MODE)
                    .add(Player.COMMAND_GET_TIMELINE)
                    .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_GET_METADATA)
                    .add(Player.COMMAND_SET_MEDIA_ITEM)
                    .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                    .add(Player.COMMAND_SET_SPEED_AND_PITCH)
                    .add(Player.COMMAND_GET_AUDIO_ATTRIBUTES)
                    .add(Player.COMMAND_SET_PLAYLIST_METADATA)
                    .add(Player.COMMAND_ADJUST_DEVICE_VOLUME)
                    .add(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)
                    .add(Player.COMMAND_GET_DEVICE_VOLUME)
                    .build()
                
                return MediaSession.ConnectionResult.accept(
                    sessionCommands, 
                    playerCommands
                )
            }

            override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
                super.onPostConnect(session, controller)
                updateCustomLayout()
            }

            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: androidx.media3.session.SessionCommand, args: android.os.Bundle): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                when (customCommand.customAction) {
                    COMMAND_LIKE -> {
                         val currentSongId = session.player.currentMediaItem?.mediaId
                         if (currentSongId != null) {
                             serviceScope.launch {
                                 if (isCurrentSongLiked) {
                                     youTubeRepository.rateSong(currentSongId, "INDIFFERENT")
                                     isCurrentSongLiked = false
                                 } else {
                                     youTubeRepository.rateSong(currentSongId, "LIKE")
                                     isCurrentSongLiked = true
                                 }
                                 updateCustomLayout()
                             }
                         }
                    }
                    COMMAND_REPEAT -> {
                        val currentMode = session.player.repeatMode
                        session.player.repeatMode = when (currentMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                    COMMAND_SHUFFLE -> {
                        session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                    }
                    "SET_OUTPUT_DEVICE" -> {
                       val deviceId = args.getString("DEVICE_ID")
                       val player = session.player as? ExoPlayer
                       if (player != null) {
                           serviceScope.launch {
                                val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)

                                val targetDevice = if (deviceId == null || deviceId == "default") {
                                    null // Clear preference
                                } else if (deviceId == "phone_speaker") {
                                    devices.find { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                                } else {
                                    devices.find { it.id.toString() == deviceId }
                                }

                                android.util.Log.d("MusicPlayerService", "Switching output to: ${targetDevice?.productName ?: "Default"}")

                                val wasPlaying = player.isPlaying

                                // 1. Force Audio System to Normal Mode
                                // (Prevents system from being stuck in "Communication Mode" which blocks some media routing)
                                audioManager.mode = android.media.AudioManager.MODE_NORMAL

                                // 2. Clear current routing
                                player.setPreferredAudioDevice(null)
                                delay(150)

                                // 3. Set the new target device
                                player.setPreferredAudioDevice(targetDevice)

                                // 4. Reset kickstart flag so it runs again for the new device
                                audioSinkKickstartDone = false

                                // For Bluetooth devices, we need a longer delay as the hardware handshake takes time
                                val isBluetooth = targetDevice?.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                                                 targetDevice?.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                                 (android.os.Build.VERSION.SDK_INT >= 31 && (
                                                     targetDevice?.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET || 
                                                     targetDevice?.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER
                                                 ))

                                // 5. Force buffer flush by seeking
                                player.seekTo(player.currentPosition)

                                delay(if (isBluetooth) 1200 else 400) // Give it a moment to switch routing

                                // 6. Forceful wake-up if it was playing
                                if (wasPlaying) {
                                    player.pause()
                                    delay(300)
                                    player.play()
                                }

                                // 7. Multi-step volume nudge sequence
                                if (player.isPlaying || wasPlaying) {
                                    val originalVol = player.volume
                                    val nudgeVol = if (originalVol > 0.1f) originalVol * 0.95f else originalVol + 0.05f

                                    // Immediate nudge
                                    player.volume = nudgeVol
                                    delay(150)
                                    player.volume = originalVol

                                    if (isBluetooth) {
                                        // Secondary nudge after more time
                                        delay(800)
                                        player.volume = nudgeVol
                                        delay(150)
                                        player.volume = originalVol

                                        // Tertiary check: if still silent (or to be safe) toggle play/pause one more time
                                        delay(500)
                                        player.pause()
                                        delay(200)
                                        player.play()
                                    }
                                }
                           }
                       }
                    }
                    COMMAND_START_RADIO -> {
                        val currentVideoId = session.player.currentMediaItem?.mediaId
                        if (!currentVideoId.isNullOrBlank()) {
                            serviceScope.launch {
                                try {
                                    android.util.Log.d("MusicPlayerService", "Start Radio for: $currentVideoId")
                                    // Use getRelatedSongs which already handles /next endpoint internally
                                    val radioSongs = youTubeRepository.getRelatedSongs(currentVideoId)
                                    if (radioSongs.isNotEmpty()) {
                                        val mediaItems = radioSongs.map { createPlayableMediaItem(it) }
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            val player = session.player
                                            val insertAt = player.currentMediaItemIndex + 1
                                            mediaItems.forEachIndexed { i, item ->
                                                player.addMediaItem(insertAt + i, item)
                                            }
                                        }
                                        android.util.Log.d("MusicPlayerService", "Radio: added ${radioSongs.size} songs to queue")
                                    } else {
                                        android.util.Log.w("MusicPlayerService", "Radio: no songs returned for $currentVideoId")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MusicPlayerService", "Start Radio failed", e)
                                }
                            }
                        }
                    }

                    COMMAND_STOP_RADIO -> {
                        // Clear all items after current song
                        serviceScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            val player = session.player
                            val currentIndex = player.currentMediaItemIndex
                            val itemCount = player.mediaItemCount
                            if (itemCount > currentIndex + 1) {
                                player.removeMediaItems(currentIndex + 1, itemCount)
                            }
                        }
                    }
                }
                return com.google.common.util.concurrent.Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
            }

            override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(false)
                            .setIsPlayable(false)
                            .setTitle("SuvMusic")
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build()
                    )
                    .build()
                return com.google.common.util.concurrent.Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): com.google.common.util.concurrent.ListenableFuture<LibraryResult<MediaItem>> {
                // Check browse cache first, then search results
                val song = cachedBrowseSongs[mediaId] ?: cachedSearchResults.find { it.id == mediaId }
                return if (song != null) {
                    com.google.common.util.concurrent.Futures.immediateFuture(LibraryResult.ofItem(createPlayableMediaItem(song), null))
                } else {
                    com.google.common.util.concurrent.Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                }
            }

            override fun onSetMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                // Phone-side items typically already have URIs — pass through immediately
                val allHaveUris = mediaItems.all { it.localConfiguration?.uri != null }
                if (allHaveUris) {
                    return com.google.common.util.concurrent.Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
                    )
                }

                // Android Auto browse items need async URI resolution
                val future = com.google.common.util.concurrent.SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                serviceScope.launch {
                    try {
                        // Fix for Android Auto (No Skip Button): If only one item is being set (from browse tree),
                        // but we have context (playlist/section) for it, return the full list immediately.
                        var finalItems = mediaItems.toMutableList()
                        var finalStartIndex = startIndex

                        if (mediaItems.size == 1) {
                            val videoId = mediaItems[0].mediaId
                            val context = playlistContextCache[videoId]
                            if (context != null) {
                                val contextIndex = context.indexOfFirst { it.id == videoId }
                                if (contextIndex != -1) {
                                    finalItems = context.map { createPlayableMediaItem(it) }.toMutableList()
                                    finalStartIndex = contextIndex
                                    android.util.Log.d("MusicPlayerService", "onSetMediaItems: expanded single item to context of ${finalItems.size} songs")
                                }
                            }
                        }

                        val resolved = finalItems.mapIndexed { index, item ->
                            // Only resolve URI for the START item to keep latency low.
                            // The rest will be resolved lazily by onMediaItemTransition in the service.
                            if (index != finalStartIndex) return@mapIndexed item
                            if (item.localConfiguration?.uri != null) return@mapIndexed item

                            val videoId = item.mediaId
                            val song = cachedBrowseSongs[videoId] ?: cachedSearchResults.find { it.id == videoId }
                            val streamUrl = resolveStreamUrlWithRetry(videoId)

                            if (streamUrl != null) {
                                if (song != null) {
                                    createPlayableMediaItem(song).buildUpon()
                                        .setUri(Uri.parse(streamUrl))
                                        .build()
                                } else {
                                    item.buildUpon()
                                        .setUri(Uri.parse(streamUrl))
                                        .setMediaMetadata(
                                            item.mediaMetadata.buildUpon()
                                                .setIsPlayable(true)
                                                .setIsBrowsable(false)
                                                .build()
                                        )
                                        .build()
                                }
                            } else {
                                item // Keep as placeholder if resolution fails, onPlayerError might handle it
                            }
                        }.toMutableList()

                        if (resolved.isEmpty()) {
                            future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                        } else {
                            val safeIndex = finalStartIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
                            future.set(MediaSession.MediaItemsWithStartPosition(resolved, safeIndex, startPositionMs))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicPlayerService", "onSetMediaItems async resolution failed", e)
                        future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                    }
                }
                return future
            }

            override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                serviceScope.launch {
                    try {
                        val children = mutableListOf<MediaItem>()
                        when (parentId) {
                            ROOT_ID -> {
                                children.add(createBrowsableMediaItem(HOME_ID, "Home", iconResId = com.suvojeet.suvmusic.R.drawable.ic_music_note))
                                children.add(createBrowsableMediaItem(LIBRARY_ID, "Library", iconResId = com.suvojeet.suvmusic.R.drawable.ic_heart_outline))
                                children.add(createBrowsableMediaItem(ARTISTS_ID, "Artists", iconResId = com.suvojeet.suvmusic.R.drawable.ic_music_note, mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS))
                                children.add(createBrowsableMediaItem(ALBUMS_ID, "Albums", iconResId = com.suvojeet.suvmusic.R.drawable.ic_music_note, mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS))
                                children.add(createBrowsableMediaItem(DOWNLOADS_ID, "Downloads", iconResId = com.suvojeet.suvmusic.R.drawable.ic_music_note))
                                children.add(createBrowsableMediaItem(LOCAL_ID, "Local Music", iconResId = com.suvojeet.suvmusic.R.drawable.ic_music_note))
                            }
                            HOME_ID -> {
                                val sections = youTubeRepository.getHomeSections()
                                cachedHomeSections = sections
                                sections.forEachIndexed { index, section ->
                                    children.add(createBrowsableMediaItem("section_$index", section.title, mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED))
                                }
                            }
                            LIBRARY_ID -> {
                                children.add(createBrowsableMediaItem(LIKED_SONGS_ID, "Liked Songs", iconResId = com.suvojeet.suvmusic.R.drawable.ic_heart, mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST))
                                children.add(createBrowsableMediaItem(PLAYLISTS_ID, "Your Playlists", iconResId = com.suvojeet.suvmusic.R.drawable.ic_music_note, mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                            }
                            ARTISTS_ID -> {
                                youTubeRepository.getLibraryArtists().forEach { artist ->
                                    children.add(createBrowsableMediaItem("artist_${artist.id}", artist.name, artist.subscribers, mediaType = MediaMetadata.MEDIA_TYPE_ARTIST))
                                }
                            }
                            ALBUMS_ID -> {
                                youTubeRepository.getLibraryAlbums().forEach { album ->
                                    children.add(createBrowsableMediaItem("album_${album.id}", album.title, album.artist, mediaType = MediaMetadata.MEDIA_TYPE_ALBUM))
                                }
                            }
                            DOWNLOADS_ID -> {
                                val downloaded = downloadRepository.downloadedSongs.first()
                                if (downloaded.isNotEmpty()) {
                                    downloaded.forEach { song ->
                                        playlistContextCache[song.id] = downloaded
                                        children.add(createPlayableMediaItem(song))
                                    }
                                }
                            }
                            LOCAL_ID -> {
                                val locals = localAudioRepository.getAllLocalSongs()
                                if (locals.isNotEmpty()) {
                                    locals.forEach { song ->
                                        playlistContextCache[song.id] = locals
                                        children.add(createPlayableMediaItem(song))
                                    }
                                }
                            }
                            LIKED_SONGS_ID -> {
                                val liked = youTubeRepository.getLikedMusic()
                                if (liked.isNotEmpty()) {
                                    liked.forEach { song ->
                                        playlistContextCache[song.id] = liked
                                        children.add(createPlayableMediaItem(song))
                                    }
                                }
                            }
                            PLAYLISTS_ID -> {
                                youTubeRepository.getUserPlaylists().forEach { playlist ->
                                    children.add(createBrowsableMediaItem("playlist_${playlist.id}", playlist.name, playlist.uploaderName, mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                                }
                            }
                            else -> {
                                if (parentId.startsWith("section_")) {
                                    val index = parentId.removePrefix("section_").toIntOrNull()
                                    if (index != null && index in cachedHomeSections.indices) {
                                        val section = cachedHomeSections[index]

                                        // BUG FIX (Skip): Cache song list for this section so
                                        // onSetMediaItems can populate the queue for skip support
                                        val sectionSongs = section.items
                                            .filterIsInstance<com.suvojeet.suvmusic.data.model.HomeItem.SongItem>()
                                            .map { it.song }
                                        sectionSongs.forEach { song ->
                                            playlistContextCache[song.id] = sectionSongs
                                        }

                                        section.items.forEach { homeItem ->
                                             if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.SongItem) {
                                                 children.add(createPlayableMediaItem(homeItem.song))
                                             } else if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.PlaylistItem) {
                                                 children.add(createBrowsableMediaItem("playlist_${homeItem.playlist.id}", homeItem.playlist.name, homeItem.playlist.uploaderName, mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST))
                                             } else if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.AlbumItem) {
                                                 children.add(createBrowsableMediaItem("album_${homeItem.album.id}", homeItem.album.title, homeItem.album.artist, mediaType = MediaMetadata.MEDIA_TYPE_ALBUM))
                                             }
                                        }
                                    }
                                } else if (parentId.startsWith("playlist_")) {
                                    val playlistId = parentId.removePrefix("playlist_")

                                    // Auto-generated mixes (RD*, RTM) need /next endpoint, not /browse
                                    // getPlaylist() times out on these in Android Auto due to pagination
                                    val isAutoMix = playlistId.startsWith("RD") ||
                                                    playlistId.startsWith("RTM")
                                    val playlist = if (isAutoMix) {
                                        kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                                            youTubeRepository.getAutoMixPlaylist(playlistId)
                                        } ?: run {
                                            android.util.Log.w("MusicPlayerService", "Auto-mix timed out: $playlistId")
                                            com.suvojeet.suvmusic.core.model.Playlist(playlistId, resolveAutoMixTitle(playlistId), "YouTube Music", null, emptyList())
                                        }
                                    } else {
                                        youTubeRepository.getPlaylist(playlistId)
                                    }

                                    // BUG FIX (Skip): Populate context cache so onSetMediaItems
                                    // can append remaining songs for skip support
                                    if (playlist.songs.isNotEmpty()) {
                                        playlist.songs.forEach { song ->
                                            playlistContextCache[song.id] = playlist.songs
                                        }
                                    }

                                    // Add Shuffle item
                                    if (playlist.songs.isNotEmpty()) {
                                        children.add(MediaItem.Builder()
                                            .setMediaId("shuffle_$playlistId")
                                            .setMediaMetadata(MediaMetadata.Builder()
                                                .setTitle("🔀 Shuffle")
                                                .setIsBrowsable(false)
                                                .setIsPlayable(true)
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                                .build())
                                            .build())
                                    }
                                    playlist.songs.forEach { children.add(createPlayableMediaItem(it)) }
                                } else if (parentId.startsWith("album_")) {
                                    val albumId = parentId.removePrefix("album_")
                                    val album = youTubeRepository.getAlbum(albumId)
                                    album?.songs?.let { songs ->
                                        songs.forEach { song ->
                                            playlistContextCache[song.id] = songs
                                            children.add(createPlayableMediaItem(song))
                                        }
                                    }
                                } else if (parentId.startsWith("artist_")) {
                                    val artistId = parentId.removePrefix("artist_")
                                    val artist = youTubeRepository.getArtist(artistId)
                                    artist?.songs?.let { songs ->
                                        songs.forEach { song ->
                                            playlistContextCache[song.id] = songs
                                            children.add(createPlayableMediaItem(song))
                                        }
                                    }
                                    artist?.albums?.forEach { children.add(createBrowsableMediaItem("album_${it.id}", it.title, it.artist, mediaType = MediaMetadata.MEDIA_TYPE_ALBUM)) }
                                }
                            }
                        }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
                    } catch (e: Exception) {
                        android.util.Log.e("MusicPlayerService", "onGetChildren failed for $parentId", e)
                        future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                    }
                }
                return future
            }

            override fun onSearch(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<Void>> {
                val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<Void>>()
                serviceScope.launch {
                    try {
                        val results = youTubeRepository.search(query)
                        cachedSearchQuery = query
                        cachedSearchResults = results
                        // BUG FIX (Skip): Cache results as context for skip support in search
                        results.forEach { song ->
                             playlistContextCache[song.id] = results
                        }
                        mediaLibrarySession?.notifySearchResultChanged(browser, query, results.size, params)
                        future.set(LibraryResult.ofVoid(params))
                    } catch (e: Exception) {
                        android.util.Log.e("MusicPlayerService", "onSearch failed for '$query'", e)
                        future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                    }
                }
                return future
            }
            
            override fun onGetSearchResult(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, page: Int, pageSize: Int, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                 val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                 serviceScope.launch {
                     try {
                         // Reuse cached results if the query matches, otherwise re-search
                         val results = if (query == cachedSearchQuery && cachedSearchResults.isNotEmpty()) {
                             cachedSearchResults
                         } else {
                             youTubeRepository.search(query)
                         }
                         // BUG FIX (Skip): Ensure context cache is populated
                         results.forEach { song ->
                             playlistContextCache[song.id] = results
                         }
                         val mediaItems = results.map { createPlayableMediaItem(it) }
                         future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params))
                     } catch(e: Exception) {
                         android.util.Log.e("MusicPlayerService", "onGetSearchResult failed for '$query'", e)
                         future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                     }
                 }
                 return future
            }

            override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): com.google.common.util.concurrent.ListenableFuture<MutableList<MediaItem>> {
                 val updatedMediaItemsFuture = com.google.common.util.concurrent.SettableFuture.create<MutableList<MediaItem>>()
                 serviceScope.launch {
                     val finalItems = mutableListOf<MediaItem>()
                     for (item in mediaItems) {
                         if (item.mediaId.startsWith("shuffle_")) {
                             val playlistId = item.mediaId.removePrefix("shuffle_")
                             try {
                                 val playlist = youTubeRepository.getPlaylist(playlistId)
                                 val shuffledSongs = playlist.songs.shuffled()
                                 shuffledSongs.forEach { finalItems.add(createPlayableMediaItem(it)) }
                             } catch (e: Exception) {
                                 android.util.Log.e("MusicPlayerService", "Shuffle failed for $playlistId", e)
                             }
                             continue
                         }

                         // Android Auto voice search: "Play <song>" → searchQuery is set
                         val searchQuery = item.requestMetadata.searchQuery
                         val resolvedItem = if (!searchQuery.isNullOrEmpty() && item.localConfiguration?.uri?.toString().isNullOrEmpty()) {
                             try {
                                 val results = youTubeRepository.search(searchQuery)
                                 if (results.isNotEmpty()) {
                                     val song = results.first()
                                     val streamUrl = resolveStreamUrlWithRetry(song.id)
                                     if (streamUrl != null) {
                                         createPlayableMediaItem(song).buildUpon()
                                             .setUri(Uri.parse(streamUrl))
                                             .build()
                                     } else {
                                         createPlayableMediaItem(song)
                                     }
                                 } else {
                                     null
                                 }
                             } catch (e: Exception) {
                                 android.util.Log.e("MusicPlayerService", "Voice search failed for '$searchQuery'", e)
                                 null
                             }
                         } else if (item.localConfiguration?.uri?.toString().isNullOrEmpty()) {
                             // Item from Android Auto browse tree or search results — needs URI resolution.
                             val videoId = item.mediaId
                             try {
                                 // Try to find the full Song object from cached search results
                                 val cachedSong = cachedSearchResults.find { it.id == videoId }

                                 val streamUrl = resolveStreamUrlWithRetry(videoId)
                                 if (streamUrl != null) {
                                     if (cachedSong != null) {
                                         // Rebuild with full metadata from cached Song object
                                         createPlayableMediaItem(cachedSong).buildUpon()
                                             .setUri(Uri.parse(streamUrl))
                                             .build()
                                     } else {
                                         // Rebuild with metadata from the item itself + resolved URI
                                         val metadata = item.mediaMetadata
                                         MediaItem.Builder()
                                             .setMediaId(videoId)
                                             .setUri(Uri.parse(streamUrl))
                                             .setMediaMetadata(
                                                 metadata.buildUpon()
                                                     .setIsPlayable(true)
                                                     .setIsBrowsable(false)
                                                     .build()
                                             )
                                             .build()
                                     }
                                 } else {
                                     android.util.Log.e("MusicPlayerService", "Failed to resolve stream for: $videoId")
                                     null
                                 }
                             } catch (e: Exception) {
                                 android.util.Log.e("MusicPlayerService", "onAddMediaItems resolution failed for: $videoId", e)
                                 null
                             }
                         } else {
                             item
                         }
                         resolvedItem?.let { finalItems.add(it) }
                     }
                     
                     // Fix for Android Auto (No Skip Button): If only one item resolved (from browse/search),
                     // and we have context, return the FULL list here.
                     if (finalItems.size == 1) {
                         val firstId = finalItems.first().mediaId
                         val context = playlistContextCache[firstId]
                         if (context != null) {
                             val startIndex = context.indexOfFirst { it.id == firstId }
                             if (startIndex != -1) {
                                 val allContextItems = context.map { createPlayableMediaItem(it) }.toMutableList()
                                 // Replace the placeholder for current item with the resolved one (with URI)
                                 allContextItems[startIndex] = finalItems[0]
                                 
                                 // Replace finalItems with the full context
                                 finalItems.clear()
                                 finalItems.addAll(allContextItems)
                                 android.util.Log.d("MusicPlayerService", "onAddMediaItems: expanded single item to context of ${finalItems.size} songs")
                             }
                         }
                     }
                     updatedMediaItemsFuture.set(finalItems)
                 }
                 return updatedMediaItemsFuture
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val future = com.google.common.util.concurrent.SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                serviceScope.launch {
                    try {
                        val lastState = sessionManager.getLastPlaybackState()
                        if (lastState != null) {
                            val jsonArray = JSONArray(lastState.queueJson)
                            val mediaItems = mutableListOf<MediaItem>()
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val song = com.suvojeet.suvmusic.core.model.Song(
                                    id = obj.getString("id"),
                                    title = obj.getString("title"),
                                    artist = obj.getString("artist"),
                                    album = obj.optString("album", ""),
                                    thumbnailUrl = obj.optString("thumbnailUrl", ""),
                                    duration = obj.optLong("duration", 0L),
                                    source = try {
                                        com.suvojeet.suvmusic.core.model.SongSource.valueOf(obj.optString("source", "YOUTUBE"))
                                    } catch (e: Exception) {
                                        com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE
                                    }
                                )
                                mediaItems.add(createPlayableMediaItem(song))
                            }
                            if (mediaItems.isNotEmpty()) {
                                val index = lastState.index.coerceIn(0, mediaItems.size - 1)
                                future.set(MediaSession.MediaItemsWithStartPosition(mediaItems, index, lastState.position))
                            } else {
                                // Return empty list so Android Auto shows browse tree instead of error
                                future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                            }
                        } else {
                            // No saved state — return empty so Auto can browse instead of crashing
                            future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicPlayerService", "onPlaybackResumption failed", e)
                        future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                    }
                }
                return future
            }
        })
        .setSessionActivity(sessionActivityPendingIntent)
        .setBitmapLoader(CoilBitmapLoader(this))
        .build()

        registerReceiver(volumeReceiver, android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        serviceScope.launch {
            sessionManager.sponsorBlockEnabledFlow.collect { enabled ->
                sponsorBlockRepository.setEnabled(enabled)
            }
        }
        setupSleepTimerNotification()
    }
    
    private fun getCustomLayout(): List<CommandButton> {
        val player = mediaLibrarySession?.player
        val isShuffleOn = player?.shuffleModeEnabled == true
        val likeIcon = if (isCurrentSongLiked) com.suvojeet.suvmusic.R.drawable.ic_heart else com.suvojeet.suvmusic.R.drawable.ic_heart_outline
        
        val shuffleIcon = if (isShuffleOn) {
            com.suvojeet.suvmusic.R.drawable.ic_shuffle_on
        } else {
            com.suvojeet.suvmusic.R.drawable.ic_shuffle
        }

        val repeatIcon = when (player?.repeatMode) {
            Player.REPEAT_MODE_ALL -> com.suvojeet.suvmusic.R.drawable.ic_repeat_all_on
            Player.REPEAT_MODE_ONE -> com.suvojeet.suvmusic.R.drawable.ic_repeat_one_on
            else -> com.suvojeet.suvmusic.R.drawable.ic_repeat 
        }
        return listOf(
            CommandButton.Builder()
                .setDisplayName(getString(com.suvojeet.suvmusic.R.string.notification_action_shuffle))
                .setSessionCommand(SessionCommand(COMMAND_SHUFFLE, android.os.Bundle.EMPTY))
                .setIconResId(shuffleIcon)
                .build(),
            CommandButton.Builder()
                .setDisplayName(getString(com.suvojeet.suvmusic.R.string.notification_action_like))
                .setSessionCommand(SessionCommand(COMMAND_LIKE, android.os.Bundle.EMPTY))
                .setIconResId(likeIcon)
                .build(),
            CommandButton.Builder()
                .setDisplayName(getString(com.suvojeet.suvmusic.R.string.notification_action_repeat))
                .setSessionCommand(SessionCommand(COMMAND_REPEAT, android.os.Bundle.EMPTY))
                .setIconResId(repeatIcon)
                .build(),
            CommandButton.Builder()
                .setDisplayName("Start Radio")
                .setSessionCommand(SessionCommand(COMMAND_START_RADIO, android.os.Bundle.EMPTY))
                .setIconResId(com.suvojeet.suvmusic.R.drawable.ic_play)
                .build()
        )
    }

    @UnstableApi
    private inner class CustomNotificationProvider : MediaNotification.Provider {
        private val defaultProvider = androidx.media3.session.DefaultMediaNotificationProvider.Builder(this@MusicPlayerService)
            .build()
        override fun createNotification(
            session: MediaSession,
            customLayout: ImmutableList<CommandButton>,
            actionFactory: MediaNotification.ActionFactory,
            onNotificationChangedCallback: MediaNotification.Provider.Callback
        ): MediaNotification {
            val mediaNotification = defaultProvider.createNotification(session, customLayout, actionFactory, onNotificationChangedCallback)
            mediaNotification.notification.flags = mediaNotification.notification.flags or android.app.Notification.FLAG_ONGOING_EVENT
            return mediaNotification
        }

        override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
            return defaultProvider.getNotificationChannelInfo()
        }

        override fun handleCustomCommand(session: MediaSession, action: String, extras: android.os.Bundle): Boolean {
            return when (action) {
                COMMAND_LIKE -> {
                    updateLikedState()
                    true
                }
                COMMAND_SHUFFLE -> {
                    session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                    true
                }
                COMMAND_REPEAT -> {
                    val currentMode = session.player.repeatMode
                    session.player.repeatMode = when (currentMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        serviceScope.launch {
            if (sessionManager.isStopMusicOnTaskClearEnabled()) {
                stopSelf()
            } else {
                val player = mediaLibrarySession?.player
                if (player?.playWhenReady == false && player.mediaItemCount == 0) {
                    stopSelf()
                }
            }
        }
    }
    
    private val volumeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            if ("android.media.VOLUME_CHANGED_ACTION" == intent.action) {
                 val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                 val vol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                 if (vol == 0) {
                     serviceScope.launch {
                         if (sessionManager.isPauseMusicOnMediaMutedEnabled()) {
                             mediaLibrarySession?.player?.pause()
                         }
                     }
                 }
            }
        }
    }

    private fun startSponsorBlockMonitoring() {
        sponsorBlockJob?.cancel()
        sponsorBlockJob = serviceScope.launch {
            while (isActive) {
                val player = mediaLibrarySession?.player
                if (player != null && player.isPlaying) {
                    val currentPos = player.currentPosition / 1000f
                    val skipTo = sponsorBlockRepository.checkSkip(currentPos)
                    if (skipTo != null) {
                        player.seekTo((skipTo * 1000).toLong())
                    } else {
                        // Adaptive polling: 
                        // If next segment is far away, we can sleep longer
                        val nextSegmentStart = sponsorBlockRepository.getNextSegmentStart(currentPos)
                        if (nextSegmentStart != null) {
                            val timeUntilNext = nextSegmentStart - currentPos
                            if (timeUntilNext > 2.0f) {
                                // Sleep for a bit less than time until next segment (max 5s)
                                val sleepMs = (timeUntilNext * 1000 * 0.8f).toLong().coerceIn(200, 5000)
                                delay(sleepMs)
                                continue
                            }
                        }
                    }
                }
                delay(200L)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        sponsorBlockJob?.cancel()
        try { unregisterReceiver(volumeReceiver) } catch (e: Exception) {}
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_SLEEP_TIMER)
        audioARManager.setPlaying(false)
        listenTogetherManager.setPlayer(null)
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    /**
     * Resolve a stream URL with retry logic, matching the phone-side behavior.
     * This ensures Android Auto playback is as reliable as phone-initiated playback.
     */
    private suspend fun resolveStreamUrlWithRetry(videoId: String): String? {
        var streamUrl: String? = null
        var attempts = 0
        while (streamUrl == null && attempts < 2) {
            streamUrl = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                youTubeRepository.getStreamUrl(videoId)
            }
            if (streamUrl == null) {
                attempts++
                if (attempts < 2) delay(1000)
            }
        }
        return streamUrl
    }

    private fun resolveAutoMixTitle(playlistId: String): String = when {
        playlistId.startsWith("RDAMPL") -> "Mixed For You"
        playlistId.startsWith("RDCLAK") -> "Discover Mix"
        playlistId.startsWith("RDGMUK") -> "Replay Mix"
        playlistId.startsWith("RTM") || playlistId.startsWith("RDTMAK") -> "My Supermix"
        else -> "Your Mix"
    }

    private fun createBrowsableMediaItem(
        mediaId: String, 
        title: String, 
        subtitle: String? = null,
        iconResId: Int? = null, 
        mediaType: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
    ): MediaItem {
        val iconUri = iconResId?.let { drawableUri(it) }
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setArtworkUri(iconUri)
                    .setMediaType(mediaType)
                    .build()
            )
            .build()
    }

    private fun drawableUri(id: Int) = Uri.Builder()
        .scheme(android.content.ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(resources.getResourcePackageName(id))
        .appendPath(resources.getResourceTypeName(id))
        .appendPath(resources.getResourceEntryName(id))
        .build()

    private fun createPlayableMediaItem(song: com.suvojeet.suvmusic.core.model.Song): MediaItem {
        // Cache every song encountered during browsing for later URI resolution in onSetMediaItems
        cachedBrowseSongs[song.id] = song

        val artworkUri = if (!song.thumbnailUrl.isNullOrEmpty()) Uri.parse(song.thumbnailUrl) else null
        val contentUri = song.localUri ?: if (song.streamUrl != null) Uri.parse(song.streamUrl) else null
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setDisplayTitle(song.title)
            .setArtist(song.artist)
            .setSubtitle(song.artist)
            .setAlbumTitle(song.album ?: "")
            .setArtworkUri(artworkUri)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setDurationMs(if (song.duration > 0) song.duration else null)
            .build()
        val builder = MediaItem.Builder()
            .setMediaId(song.id)
            .setMediaMetadata(metadata)
        if (contentUri != null) {
            builder.setUri(contentUri)
        }
        return builder.build()
    }

    private data class AudioEffectsState(
        val normEnabled: Boolean,
        val boostEnabled: Boolean,
        val boostAmount: Int,
        val audioArEnabled: Boolean
    )
    
    private fun setupSleepTimerNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "sleep_timer_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Sleep Timer", android.app.NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows active sleep timer countdown"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(sleepTimerManager.isActive, sleepTimerManager.remainingTimeMs) { isActive, remaining -> Pair(isActive, remaining) }.collect { (isActive, remaining) ->
                if (isActive && remaining != null) {
                    val minutes = remaining / 1000 / 60
                    val seconds = (remaining / 1000) % 60
                    val timeString = String.format("%d:%02d", minutes, seconds)
                    val cancelIntent = Intent(this@MusicPlayerService, MusicPlayerService::class.java).apply { action = "ACTION_CANCEL_SLEEP_TIMER" }
                    val pendingCancelIntent = PendingIntent.getService(this@MusicPlayerService, 99, cancelIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    val notification = androidx.core.app.NotificationCompat.Builder(this@MusicPlayerService, channelId)
                        .setSmallIcon(com.suvojeet.suvmusic.R.drawable.ic_music_note)
                        .setContentTitle("Sleep Timer Active")
                        .setContentText("Stopping audio in $timeString")
                        .setOnlyAlertOnce(true)
                        .setOngoing(true)
                        .addAction(com.suvojeet.suvmusic.R.drawable.ic_music_note, "Cancel", pendingCancelIntent)
                        .setColor(androidx.core.content.ContextCompat.getColor(this@MusicPlayerService, com.suvojeet.suvmusic.R.color.black))
                        .build()
                    notificationManager.notify(NOTIFICATION_ID_SLEEP_TIMER, notification)
                } else {
                    notificationManager.cancel(NOTIFICATION_ID_SLEEP_TIMER)
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_CANCEL_SLEEP_TIMER") sleepTimerManager.cancelTimer()
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        private const val NOTIFICATION_ID_SLEEP_TIMER = 1002
    }
}

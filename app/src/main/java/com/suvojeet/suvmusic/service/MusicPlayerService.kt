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
    
    private var sponsorBlockJob: kotlinx.coroutines.Job? = null
    
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

    private fun updateLikedState() {
        val currentSongId = mediaLibrarySession?.player?.currentMediaItem?.mediaId ?: return
        serviceScope.launch {
            try {
                // Check both YouTube and local history
                val likedSongs = youTubeRepository.getLikedMusic()
                isCurrentSongLiked = likedSongs.any { it.id == currentSongId }
                
                mediaLibrarySession?.setCustomLayout(getCustomLayout())
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
                5_000,   // minBufferMs (was 2,000)
                25_000,  // maxBufferMs (was 15_000)
                2_500,   // bufferForPlaybackMs (was 1,500)
                5_000    // bufferForPlaybackAfterRebufferMs (was 2,000)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(5_000, true)
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
                    mediaItem?.mediaId?.let { videoId ->
                        if (videoId.isNotEmpty()) {
                            sponsorBlockRepository.loadSegments(videoId)
                        }
                    }
                    updateLikedState()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    audioARManager.setPlaying(isPlaying)
                    if (isPlaying) {
                        startSponsorBlockMonitoring()
                    } else {
                        sponsorBlockJob?.cancel()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    mediaLibrarySession?.setCustomLayout(getCustomLayout())
                    
                    // Bug Fix: Some devices have a "Silent Handshake" issue where AudioTrack 
                    // is ready but doesn't produce sound until gain is updated.
                    if (playbackState == Player.STATE_READY) {
                        serviceScope.launch {
                            val p = mediaLibrarySession?.player ?: return@launch
                            if (p.playWhenReady) {
                                val currentVol = p.volume
                                // Micro-toggle volume to "kickstart" the AudioSink
                                p.volume = 0.99f * currentVol
                                delay(50)
                                p.volume = currentVol
                            }
                        }
                    }
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    super.onPlayWhenReadyChanged(playWhenReady, reason)
                    // If playWhenReady was changed to false due to audio focus loss
                    // We want to ensure it doesn't automatically resume if it was a transient loss
                    // Media3 by default resumes on transient loss gain.
                    // By checking the reason, we can detect focus loss.
                    if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) {
                        android.util.Log.d("MusicPlayerService", "Paused due to audio focus loss. Manual resume required.")
                        // We don't need to do anything else here because Media3 already set playWhenReady to false.
                        // The default behavior is that when focus is gained back, playWhenReady stays false
                        // UNLESS we are using the default FocusControl which auto-resumes.
                        // However, to be absolutely sure we stop "concurrent" playback, we ensure focus is requested.
                    }
                }
                
                override fun onRepeatModeChanged(repeatMode: Int) {
                    mediaLibrarySession?.setCustomLayout(getCustomLayout())
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    mediaLibrarySession?.setCustomLayout(getCustomLayout())
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
                session.setCustomLayout(getCustomLayout())
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
                                 session.setCustomLayout(getCustomLayout())
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
                        if (deviceId != null && player != null) {
                            serviceScope.launch {
                                 val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                 val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                                 val targetDevice = if (deviceId == "phone_speaker") {
                                     devices.find { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                                 } else {
                                     devices.find { it.id.toString() == deviceId }
                                 }
                                 player.setPreferredAudioDevice(targetDevice)
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
                        val resolved = mediaItems.mapIndexed { index, item ->
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
                                android.util.Log.e("MusicPlayerService", "onSetMediaItems: failed to resolve URI for $videoId")
                                null
                            }
                        }.filterNotNull().toMutableList()

                        if (resolved.isEmpty()) {
                            future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                        } else {
                            val safeIndex = startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
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
                                downloadRepository.downloadedSongs.first().forEach { children.add(createPlayableMediaItem(it)) }
                            }
                            LOCAL_ID -> {
                                localAudioRepository.getAllLocalSongs().forEach { children.add(createPlayableMediaItem(it)) }
                            }
                            LIKED_SONGS_ID -> {
                                youTubeRepository.getLikedMusic().forEach { children.add(createPlayableMediaItem(it)) }
                            }
                            PLAYLISTS_ID -> {
                                youTubeRepository.getUserPlaylists().forEach { playlist ->
                                    children.add(createBrowsableMediaItem("playlist_${playlist.id}", playlist.name, playlist.uploaderName, mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST))
                                }
                            }
                            else -> {
                                if (parentId.startsWith("section_")) {
                                    val index = parentId.removePrefix("section_").toIntOrNull()
                                    if (index != null && index in cachedHomeSections.indices) {
                                        val section = cachedHomeSections[index]

                                        // BUG FIX (Skip): Cache song list for this section so
                                        // onAddMediaItems can populate the queue for skip support
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

                                    // BUG FIX (Skip): Populate context cache so onAddMediaItems
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
                                    youTubeRepository.getAlbum(albumId)?.songs?.forEach { children.add(createPlayableMediaItem(it)) }
                                } else if (parentId.startsWith("artist_")) {
                                    val artistId = parentId.removePrefix("artist_")
                                    val artist = youTubeRepository.getArtist(artistId)
                                    artist?.songs?.forEach { children.add(createPlayableMediaItem(it)) }
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
                     // BUG FIX (Skip): After resolving the tapped song, asynchronously
                     // append the rest of its playlist to the player queue.
                     // This allows AA skip to work. Items have no stream URLs yet —
                     // MusicPlayer's onMediaItemTransition will resolve them lazily.
                     if (finalItems.isNotEmpty()) {
                         val firstId = finalItems.first().mediaId
                         val context = playlistContextCache[firstId]
                         if (context != null) {
                             val startIndex = context.indexOfFirst { it.id == firstId }
                             if (startIndex >= 0 && startIndex < context.size - 1) {
                                 val remaining = context.subList(startIndex + 1, context.size)
                                 serviceScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                     try {
                                         val player = mediaLibrarySession?.player ?: return@launch
                                         remaining.forEach { song ->
                                             player.addMediaItem(createPlayableMediaItem(song))
                                         }
                                         android.util.Log.d("MusicPlayerService", "Skip fix: queued ${remaining.size} songs after ${firstId}")
                                     } catch (e: Exception) {
                                         android.util.Log.e("MusicPlayerService", "Skip fix queue append failed", e)
                                     }
                                 }
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

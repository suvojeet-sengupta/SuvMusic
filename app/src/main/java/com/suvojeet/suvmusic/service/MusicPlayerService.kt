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
    lateinit var listenTogetherManager: com.suvojeet.suvmusic.listentogether.ListenTogetherManager

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
    
    // Constants for Android Auto browsing
    private val ROOT_ID = "root"
    private val HOME_ID = "home"
    private val LIBRARY_ID = "library"
    private val DOWNLOADS_ID = "downloads"
    private val LOCAL_ID = "local_music"
    private val LIKED_SONGS_ID = "liked_songs"
    private val PLAYLISTS_ID = "playlists"
    
    // Cache for Home Sections to handle "SECTION_Index" lookup
    private var cachedHomeSections: List<com.suvojeet.suvmusic.data.model.HomeSection> = emptyList()

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
            .setBufferDurationsMs(2_000, 15_000, 1_500, 2_000)
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
                val isOffloadEnabled = sessionManager.isAudioOffloadEnabled()
                val isSpatialAudioPreferred = sessionManager.isAudioArEnabled()
                val ignoreAudioFocus = sessionManager.isIgnoreAudioFocusDuringCallsEnabled()
                
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
                    .add(Player.COMMAND_SET_REPEAT_MODE)
                    .add(Player.COMMAND_SET_SHUFFLE_MODE)
                    .add(Player.COMMAND_GET_TIMELINE)
                    .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_GET_METADATA)
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
                }
                return com.google.common.util.concurrent.Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
            }

            override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle("SuvMusic")
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build()
                    )
                    .build()
                return com.google.common.util.concurrent.Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                serviceScope.launch {
                    try {
                        val children = mutableListOf<MediaItem>()
                        when (parentId) {
                            ROOT_ID -> {
                                children.add(createBrowsableMediaItem(HOME_ID, "Home"))
                                children.add(createBrowsableMediaItem(LIBRARY_ID, "Library"))
                                children.add(createBrowsableMediaItem(DOWNLOADS_ID, "Downloads"))
                                children.add(createBrowsableMediaItem(LOCAL_ID, "Local Music"))
                            }
                            HOME_ID -> {
                                val sections = youTubeRepository.getHomeSections()
                                cachedHomeSections = sections
                                sections.forEachIndexed { index, section ->
                                    children.add(createBrowsableMediaItem("section_$index", section.title))
                                }
                            }
                            LIBRARY_ID -> {
                                children.add(createBrowsableMediaItem(LIKED_SONGS_ID, "Liked Songs"))
                                children.add(createBrowsableMediaItem(PLAYLISTS_ID, "Your Playlists"))
                            }
                            DOWNLOADS_ID -> {
                                downloadRepository.downloadedSongs.value.forEach { children.add(createPlayableMediaItem(it)) }
                            }
                            LOCAL_ID -> {
                                localAudioRepository.getAllLocalSongs().forEach { children.add(createPlayableMediaItem(it)) }
                            }
                            LIKED_SONGS_ID -> {
                                youTubeRepository.getLikedMusic().forEach { children.add(createPlayableMediaItem(it)) }
                            }
                            PLAYLISTS_ID -> {
                                youTubeRepository.getUserPlaylists().forEach { playlist ->
                                    children.add(createBrowsableMediaItem("playlist_${playlist.id}", playlist.name))
                                }
                            }
                            else -> {
                                if (parentId.startsWith("section_")) {
                                    val index = parentId.removePrefix("section_").toIntOrNull()
                                    if (index != null && index in cachedHomeSections.indices) {
                                        cachedHomeSections[index].items.forEach { homeItem ->
                                             if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.SongItem) {
                                                 children.add(createPlayableMediaItem(homeItem.song))
                                             } else if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.PlaylistItem) {
                                                 children.add(createBrowsableMediaItem("playlist_${homeItem.playlist.id}", homeItem.playlist.name))
                                             } else if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.AlbumItem) {
                                                 children.add(createBrowsableMediaItem("album_${homeItem.album.id}", homeItem.album.title))
                                             }
                                        }
                                    }
                                } else if (parentId.startsWith("playlist_")) {
                                    val playlistId = parentId.removePrefix("playlist_")
                                    youTubeRepository.getPlaylist(playlistId).songs.forEach { children.add(createPlayableMediaItem(it)) }
                                } else if (parentId.startsWith("album_")) {
                                    val albumId = parentId.removePrefix("album_")
                                    youTubeRepository.getAlbum(albumId)?.songs?.forEach { children.add(createPlayableMediaItem(it)) }
                                }
                            }
                        }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
                    } catch (e: Exception) {
                        future.setException(e)
                    }
                }
                return future
            }

            override fun onSearch(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<Void>> {
                val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<Void>>()
                serviceScope.launch {
                    try {
                        val results = youTubeRepository.search(query)
                        mediaLibrarySession?.notifySearchResultChanged(browser, query, results.size, params)
                        future.set(LibraryResult.ofVoid(params))
                    } catch (e: Exception) {
                        future.setException(e)
                    }
                }
                return future
            }
            
            override fun onGetSearchResult(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, page: Int, pageSize: Int, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                 val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                 serviceScope.launch {
                     try {
                         val results = youTubeRepository.search(query)
                         val mediaItems = results.map { createPlayableMediaItem(it) }
                         future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params))
                     } catch(e: Exception) {
                         future.setException(e)
                     }
                 }
                 return future
            }

            override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): com.google.common.util.concurrent.ListenableFuture<MutableList<MediaItem>> {
                 val updatedMediaItemsFuture = com.google.common.util.concurrent.SettableFuture.create<MutableList<MediaItem>>()
                 serviceScope.launch {
                     val updatedList = mediaItems.map { item ->
                        if (item.localConfiguration?.uri?.toString().isNullOrEmpty()) {
                            val videoId = item.mediaId
                            val streamUrl = youTubeRepository.getStreamUrl(videoId)
                            if (streamUrl != null) {
                                item.buildUpon().setUri(Uri.parse(streamUrl)).build()
                            } else {
                                item
                            }
                        } else {
                            item
                        }
                     }.toMutableList()
                     updatedMediaItemsFuture.set(updatedList)
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
                                future.setException(UnsupportedOperationException("No media items found"))
                            }
                        } else {
                            future.setException(UnsupportedOperationException("No last playback state found"))
                        }
                    } catch (e: Exception) {
                        future.setException(e)
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
        val likeIcon = if (isCurrentSongLiked) com.suvojeet.suvmusic.R.drawable.ic_heart else com.suvojeet.suvmusic.R.drawable.ic_heart_outline
        val repeatIcon = when (player?.repeatMode) {
            Player.REPEAT_MODE_ALL -> com.suvojeet.suvmusic.R.drawable.ic_repeat
            Player.REPEAT_MODE_ONE -> com.suvojeet.suvmusic.R.drawable.ic_repeat_one
            else -> com.suvojeet.suvmusic.R.drawable.ic_repeat 
        }
        return listOf(
            CommandButton.Builder()
                .setDisplayName(getString(com.suvojeet.suvmusic.R.string.notification_action_shuffle))
                .setSessionCommand(SessionCommand(COMMAND_SHUFFLE, android.os.Bundle.EMPTY))
                .setIconResId(com.suvojeet.suvmusic.R.drawable.ic_shuffle)
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
        override fun handleCustomCommand(session: MediaSession, action: String, extras: android.os.Bundle): Boolean = false
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

    private fun createBrowsableMediaItem(mediaId: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(title)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()
    }

    private fun createPlayableMediaItem(song: com.suvojeet.suvmusic.core.model.Song): MediaItem {
        val artworkUri = if (!song.thumbnailUrl.isNullOrEmpty()) Uri.parse(song.thumbnailUrl) else null
        val contentUri = song.localUri ?: if (song.streamUrl != null) Uri.parse(song.streamUrl) else Uri.EMPTY
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
            .build()
        return MediaItem.Builder().setMediaId(song.id).setUri(contentUri).setMediaMetadata(metadata).build()
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

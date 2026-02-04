package com.suvojeet.suvmusic.service

import android.app.PendingIntent
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
import com.google.common.collect.ImmutableList
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.MainActivity
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.SponsorBlockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
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

    private var mediaLibrarySession: MediaLibrarySession? = null
    
    // Constants for Android Auto browsing
    private val ROOT_ID = "root"
    private val HOME_ID = "home"
    private val LIBRARY_ID = "library"
    private val DOWNLOADS_ID = "downloads"
    private val LOCAL_ID = "local_music" // Renamed for clarity
    private val LIKED_SONGS_ID = "liked_songs"
    private val PLAYLISTS_ID = "playlists"
    private val SEARCH_PREFIX = "search_"
    
    // Cache for Home Sections to handle "SECTION_Index" lookup
    private var cachedHomeSections: List<com.suvojeet.suvmusic.data.model.HomeSection> = emptyList()

    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var dynamicsProcessing: android.media.audiofx.DynamicsProcessing? = null
    private var sponsorBlockJob: kotlinx.coroutines.Job? = null
    
    // Audio AR & Effects state
    private var currentGlobalGainDb = 0f
    private var currentBalance = 0f

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // Ultra-fast buffer for instant playback
        // Increased min buffer for Android Auto stability
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(5_000, 50_000, 2000, 3_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true)
            .build()
            
        val isOffloadEnabled = kotlinx.coroutines.runBlocking { sessionManager.isAudioOffloadEnabled() }
        val ignoreAudioFocus = kotlinx.coroutines.runBlocking { sessionManager.isIgnoreAudioFocusDuringCallsEnabled() }

        
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                !ignoreAudioFocus
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.apply {
            pauseAtEndOfMediaItems = false
            if (isOffloadEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setAudioOffloadPreferences(
                        androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                            .setIsGaplessSupportRequired(false)
                            .build()
                    )
                    .build()
            }
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                        setupAudioNormalization(audioSessionId)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    mediaItem?.mediaId?.let { videoId ->
                        if (videoId.isNotEmpty()) {
                            sponsorBlockRepository.loadSegments(videoId)
                        }
                    }
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
            })
        }
            
        // Attach Last.fm Manager
        lastFmManager.setPlayer(player)
        
        // Attach Listen Together Manager
        listenTogetherManager.setPlayer(player)
        
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                sessionManager.volumeNormalizationEnabledFlow,
                sessionManager.volumeBoostEnabledFlow,
                sessionManager.volumeBoostAmountFlow,
                sessionManager.audioArEnabledFlow
            ) { norm: Boolean, boost: Boolean, amount: Int, ar: Boolean ->
                AudioEffectsState(norm, boost, amount, ar)
            }.collect { state ->
                 updateAudioEffects(state.normEnabled, state.boostEnabled, state.boostAmount, state.audioArEnabled, animate = true)
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
                    !ignoreFocus // handleAudioFocus is false when ignoreFocus is true
                )
            }
        }
        
        // Audio AR Monitoring
        serviceScope.launch {
            audioARManager.stereoBalance.collect { balance ->
                currentBalance = balance
                applyGains()
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
                    .add(androidx.media3.session.SessionCommand("SET_OUTPUT_DEVICE", android.os.Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.accept(sessionCommands, connectionResult.availablePlayerCommands)
            }

            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: androidx.media3.session.SessionCommand, args: android.os.Bundle): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                if (customCommand.customAction == "SET_OUTPUT_DEVICE") {
                    val deviceId = args.getString("DEVICE_ID")
                    val player = session.player as? ExoPlayer
                    
                    if (deviceId != null && player != null) {
                        serviceScope.launch {
                             val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                             val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                             
                             if (deviceId == "phone_speaker") {
                                 val speaker = devices.find { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                                 if (speaker != null) {
                                     player.setPreferredAudioDevice(speaker)
                                 } else {
                                     player.setPreferredAudioDevice(null)
                                 }
                             } else {
                                 val targetDevice = devices.find { it.id.toString() == deviceId }
                                 if (targetDevice != null) {
                                     player.setPreferredAudioDevice(targetDevice)
                                 } else {
                                     player.setPreferredAudioDevice(null)
                                 }
                             }
                        }
                    }
                    return com.google.common.util.concurrent.Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?): com.google.common.util.concurrent.ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false).setTitle("SuvMusic").build())
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
                                val songs = downloadRepository.downloadedSongs.value
                                children.addAll(songs.map { createPlayableMediaItem(it) })
                            }
                            LOCAL_ID -> {
                                val songs = localAudioRepository.getAllLocalSongs()
                                children.addAll(songs.map { createPlayableMediaItem(it) })
                            }
                            LIKED_SONGS_ID -> {
                                val songs = youTubeRepository.getLikedMusic()
                                children.addAll(songs.map { createPlayableMediaItem(it) })
                            }
                            PLAYLISTS_ID -> {
                                val playlists = youTubeRepository.getUserPlaylists()
                                children.addAll(playlists.map { playlist ->
                                    createBrowsableMediaItem("playlist_${playlist.id}", playlist.name)
                                })
                            }
                            else -> {
                                if (parentId.startsWith("section_")) {
                                    val index = parentId.removePrefix("section_").toIntOrNull()
                                    if (index != null && index in cachedHomeSections.indices) {
                                        val section = cachedHomeSections[index]
                                        section.items.forEach { homeItem ->
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
                                    val playlist = youTubeRepository.getPlaylist(playlistId)
                                    children.addAll(playlist.songs.map { createPlayableMediaItem(it) })
                                } else if (parentId.startsWith("album_")) {
                                    val albumId = parentId.removePrefix("album_")
                                    val album = youTubeRepository.getAlbum(albumId)
                                    if (album != null) {
                                        children.addAll(album.songs.map { createPlayableMediaItem(it) })
                                    }
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
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        serviceScope.launch {
            if (sessionManager.isStopMusicOnTaskClearEnabled()) {
                stopSelf()
            } else {
                val player = mediaLibrarySession?.player
                // Fix: Background Playback Termination -> Only stop if NOT playing
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

    private  var audioEffectsJob: kotlinx.coroutines.Job? = null

    private fun setupAudioNormalization(sessionId: Int) {
        serviceScope.launch {
            val normEnabled = sessionManager.isVolumeNormalizationEnabled()
            val boostEnabled = sessionManager.isVolumeBoostEnabled()
            val boostAmount = sessionManager.getVolumeBoostAmount()
            val audioArEnabled = sessionManager.isAudioArEnabled()

            if (normEnabled || (boostEnabled && boostAmount > 0) || audioArEnabled) {
                // Determine if we need to create/reset effects for the new session
                // For new session, we apply instantly (no fade) to avoid dips at track start
                updateAudioEffects(normEnabled, boostEnabled, boostAmount, audioArEnabled, animate = false, forcedSessionId = sessionId)
            }
        }
    }

    private fun startSponsorBlockMonitoring() {
        sponsorBlockJob?.cancel()
        sponsorBlockJob = serviceScope.launch {
            while (isActive) {
                val player = mediaLibrarySession?.player
                if (player != null && player.isPlaying) {
                    val currentPos = player.currentPosition / 1000f // Convert to seconds
                    val skipTo = sponsorBlockRepository.checkSkip(currentPos)

                    if (skipTo != null) {
                        player.seekTo((skipTo * 1000).toLong())
                        // Note: Toast might not be appropriate from Service, could send custom command to UI
                    }
                }
                delay(200L) // Check every 200ms for better precision
            }
        }
    }

    private fun updateAudioEffects(
        normEnabled: Boolean, 
        boostEnabled: Boolean, 
        boostAmount: Int, 
        audioArEnabled: Boolean,
        animate: Boolean, 
        forcedSessionId: Int? = null
    ) {
        audioEffectsJob?.cancel()
        audioEffectsJob = serviceScope.launch {
            try {
                val player = mediaLibrarySession?.player as? ExoPlayer
                val sessionId = forcedSessionId ?: player?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
                
                if (sessionId == C.AUDIO_SESSION_ID_UNSET) return@launch

                val shouldEnable = normEnabled || (boostEnabled && boostAmount > 0) || audioArEnabled
                
                // Calculate Target Gain in dB
                // Normalization: +5.5 dB
                // Boost: Map 0-100 to 0-15 dB
                var targetGainDb = 0f
                if (normEnabled) targetGainDb += 5.5f
                if (boostEnabled) targetGainDb += (boostAmount / 100f) * 15f

                if (shouldEnable) {
                    // Create effects if they don't exist
                    if (loudnessEnhancer == null && dynamicsProcessing == null) {
                        createAudioEffects(sessionId)
                        // If animating, start from 0 gain
                        if (animate) setEffectGain(0f)
                    }

                    if (animate) {
                        // Smooth Ramp to Target
                        // We need to know current gain? Simplified: just ramp 0 -> Target if starting,
                        // or strict update if just changing value?
                        // For simplicity, we just set the target directly unless it's a fresh enable.
                        // But to be safe on sudden jumps, let's ramp if the jump is large?
                        // Just applying instantly for slider changes is better responsiveness.
                        // Animation is mostly for Enable/Disable Toggle.
                        // Animation is mostly for Enable/Disable Toggle.
                        
                        // Update Global Gain State
                        currentGlobalGainDb = targetGainDb
                        applyGains()
                    } else {
                        currentGlobalGainDb = targetGainDb
                        applyGains()
                    }
                } else {
                    // Disable: Smooth Ramp Down -> Release
                    if (loudnessEnhancer != null || dynamicsProcessing != null) {
                        if (animate) {
                            // Smooth Ramp Down to 0
                            // Note: We don't know current gain cleanly, so maybe just release?
                            // Safest to just release for now to avoid complexity.
                        }
                        releaseAudioEffects()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicPlayerService", "Error updating audio effects", e)
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

    private fun setEffectGain(gainDb: Float) {
        currentGlobalGainDb = gainDb
        applyGains()
    }

    private fun applyGains() {
        try {
            // Calculate Channel Gains
            // Left: 1.0 if balance <= 0, else Fade
            // Right: 1.0 if balance >= 0, else Fade
            
            // Balance -1 (Left) -> Left=1, Right=0
            // Balance 1 (Right) -> Left=0, Right=1
            
            val balance = currentBalance
            
            // Linear factors (0..1)
            val leftFactor = if (balance > 0) 1f - balance else 1f
            val rightFactor = if (balance < 0) 1f + balance else 1f
            
            // Convert attenuation to dB: 20 * log10(factor)
            // Prevent log(0) -> -infinity
            val leftPanDb = if (leftFactor <= 0.001f) -100f else (20 * kotlin.math.log10(leftFactor))
            val rightPanDb = if (rightFactor <= 0.001f) -100f else (20 * kotlin.math.log10(rightFactor))
            
            val finalLeftDb = currentGlobalGainDb + leftPanDb
            val finalRightDb = currentGlobalGainDb + rightPanDb

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                dynamicsProcessing?.apply {
                    // Set Channel 0 (Left)
                    setInputGainbyChannel(0, finalLeftDb)
                    // Set Channel 1 (Right)
                    setInputGainbyChannel(1, finalRightDb)
                }
            } else {
                loudnessEnhancer?.apply {
                    // LoudnessEnhancer is global, can't pan.
                    // Just set global gain.
                    val targetmB = (currentGlobalGainDb * 100).toInt()
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
        sponsorBlockJob?.cancel()
        try {
            unregisterReceiver(volumeReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        releaseAudioEffects()
        
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

    private data class AudioEffectsState(
        val normEnabled: Boolean,
        val boostEnabled: Boolean,
        val boostAmount: Int,
        val audioArEnabled: Boolean
    )
    
    private fun <T> List<T>.toImmutableList(): ImmutableList<T> {
        return ImmutableList.copyOf(this)
    }
}

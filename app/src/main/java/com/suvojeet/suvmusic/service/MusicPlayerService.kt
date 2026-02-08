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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.log10

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

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // Ultra-fast buffer for instant playback
        // Optimized buffer for background stability and lower memory usage
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_000, 15_000, 1_500, 2_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(5_000, true)
            .build()
            
        val isOffloadEnabled = kotlinx.coroutines.runBlocking { sessionManager.isAudioOffloadEnabled() }
        val isSpatialAudioPreferred = kotlinx.coroutines.runBlocking { sessionManager.isAudioArEnabled() }
        val ignoreAudioFocus = kotlinx.coroutines.runBlocking { sessionManager.isIgnoreAudioFocusDuringCallsEnabled() }

        // Configure AudioSink with our native SpatialAudioProcessor
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
        
        val player = ExoPlayer.Builder(this, renderersFactory)
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
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.apply {
            pauseAtEndOfMediaItems = false
            // Offload MUST be disabled for AudioProcessors to work correctly
            if (isOffloadEnabled && !isSpatialAudioPreferred && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setAudioOffloadPreferences(
                        androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                            .setIsGaplessSupportRequired(false)
                            .build()
                    )
                    .build()
            } else {
                 trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setAudioOffloadPreferences(
                        androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                            .build()
                    )
                    .build()
            }
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    // Audio normalization now handled by C++ processor, no session attachment needed
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
        
        // Audio AR Monitoring - Balance handled by SpatialAudioProcessor directly
        
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
                                future.setException(UnsupportedOperationException("No media items found in last state"))
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

    override fun onDestroy() {
        serviceScope.cancel() // Cancel scope
        sponsorBlockJob?.cancel()
        try {
            unregisterReceiver(volumeReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        
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
                    .build()
            )
            .build()
    }

    private fun createPlayableMediaItem(song: com.suvojeet.suvmusic.core.model.Song): MediaItem {
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

    private fun setupSleepTimerNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "sleep_timer_channel"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Sleep Timer",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active sleep timer countdown"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        serviceScope.launch {
            // Combine isActive and remainingTimeMs to manage notification
            kotlinx.coroutines.flow.combine(
                sleepTimerManager.isActive,
                sleepTimerManager.remainingTimeMs
            ) { isActive, remaining ->
                Pair(isActive, remaining)
            }.collect { (isActive, remaining) ->
                if (isActive && remaining != null) {
                    val minutes = remaining / 1000 / 60
                    val seconds = (remaining / 1000) % 60
                    val timeString = String.format("%d:%02d", minutes, seconds)
                    
                    val cancelIntent = android.content.Intent(this@MusicPlayerService, MusicPlayerService::class.java).apply {
                        action = "ACTION_CANCEL_SLEEP_TIMER"
                    }
                    val pendingCancelIntent = android.app.PendingIntent.getService(
                        this@MusicPlayerService, 
                        99, 
                        cancelIntent, 
                        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val notification = androidx.core.app.NotificationCompat.Builder(this@MusicPlayerService, channelId)
                        .setSmallIcon(com.suvojeet.suvmusic.R.drawable.ic_music_note) // Use appropriate icon
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
        if (intent?.action == "ACTION_CANCEL_SLEEP_TIMER") {
            sleepTimerManager.cancelTimer()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        private const val NOTIFICATION_ID_SLEEP_TIMER = 1002
    }
}

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
    lateinit var youTubeRepository: com.suvojeet.suvmusic.data.repository.YouTubeRepository

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var localAudioRepository: LocalAudioRepository

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
    
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // ... (LoadControl and Player setup remains same) ...
        // Ultra-fast buffer for instant playback
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000, 50_000, 250, 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true) // Keep 30s back buffer for rewinding/looping
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
                pauseAtEndOfMediaItems = false
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                            setupAudioNormalization(audioSessionId)
                        }
                    }
                })
            }
        
        serviceScope.launch {
            sessionManager.volumeNormalizationEnabledFlow.collect { enabled ->
                 updateNormalizationEffect(enabled, animate = true)
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
                                // Handle dynamic sections from Home
                                if (parentId.startsWith("section_")) {
                                    val index = parentId.removePrefix("section_").toIntOrNull()
                                    if (index != null && index in cachedHomeSections.indices) {
                                        val section = cachedHomeSections[index]
                                        section.items.forEach { homeItem ->
                                             // Map HomeItem to MediaItem
                                             if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.SongItem) {
                                                 children.add(createPlayableMediaItem(homeItem.song))
                                             } else if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.PlaylistItem) {
                                                 children.add(createBrowsableMediaItem("playlist_${homeItem.playlist.id}", homeItem.playlist.name))
                                             } else if (homeItem is com.suvojeet.suvmusic.data.model.HomeItem.AlbumItem) {
                                                 children.add(createBrowsableMediaItem("album_${homeItem.album.id}", homeItem.album.title))
                                             } else {
                                                 // Skip Artists/Explore for now to keep simple, or map them similarly
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
                        // Signal that search is done. The empty result just confirms receipt.
                        // The actual results are retrieved via onGetSearchResult.
                        // We need to cache these results or just fetch again in onGetSearchResult.
                        // For Media3, we usually notify the session.
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
                         // Potentially redundant network call if not cached, but ensures freshness
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
                            // Needs resolution (likely YouTube song)
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
        
        // Register Volume Receiver
        registerReceiver(volumeReceiver, android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
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
        try {
            unregisterReceiver(volumeReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
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


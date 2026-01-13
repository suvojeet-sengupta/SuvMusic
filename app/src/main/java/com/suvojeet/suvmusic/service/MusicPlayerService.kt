package com.suvojeet.suvmusic.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.suvojeet.suvmusic.MainActivity
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

/**
 * Media3 MediaSessionService for background music playback.
 * Supports gapless playback and automix based on user settings.
 */
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    private var mediaSession: MediaSession? = null

    companion object {
        private var simpleCache: SimpleCache? = null
        private const val CACHE_SIZE_BYTES = 1024 * 1024 * 512L // 512 MB
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val isGaplessEnabled = sessionManager.isGaplessPlaybackEnabled()
        val isAutomixEnabled = sessionManager.isAutomixEnabled()

        // Setup caching
        if (simpleCache == null) {
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
            val databaseProvider = StandaloneDatabaseProvider(this)
            val cacheDir = File(cacheDir, "media_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)
        }

        // Setup sources
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache!!)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Local Source
        val defaultDataSourceFactory = DefaultDataSource.Factory(this)

        // Switch between network and local
        val smartDataSourceFactory = DataSource.Factory {
            val cacheDS = cacheDataSourceFactory.createDataSource()
            val defaultDS = defaultDataSourceFactory.createDataSource()

            object : DataSource {
                private var currentDataSource: DataSource? = null

                override fun addTransferListener(transferListener: TransferListener) {
                    cacheDS.addTransferListener(transferListener)
                    defaultDS.addTransferListener(transferListener)
                }

                override fun open(dataSpec: DataSpec): Long {
                    val uri = dataSpec.uri
                    val scheme = uri.scheme?.lowercase()

                    // Use Cache for HTTP/HTTPS, use Direct for Content or File
                    currentDataSource = if (scheme == "http" || scheme == "https") {
                        cacheDS
                    } else {
                        defaultDS
                    }
                    return currentDataSource!!.open(dataSpec)
                }

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    return currentDataSource?.read(buffer, offset, length) ?: 0
                }

                override fun getUri(): Uri? = currentDataSource?.uri

                override fun getResponseHeaders(): Map<String, List<String>> {
                    return currentDataSource?.responseHeaders ?: emptyMap()
                }

                override fun close() {
                    currentDataSource?.close()
                }
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(smartDataSourceFactory)

        // Buffering
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                30_000,     // Min buffer
                120_000,    // Max buffer
                1_500,      // Buffer for start
                3_000       // Buffer for rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
            
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
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
            }
        
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setBitmapLoader(CoilBitmapLoader(this))
            .build()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            stopSelf()
        }
    }
    
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}


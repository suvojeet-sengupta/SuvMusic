package com.suvojeet.suvmusic.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.datasource.cronet.CronetUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.chromium.net.CronetEngine
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider {
        return StandaloneDatabaseProvider(context)
    }

    @Provides
    @Singleton
    fun provideCronetEngine(@ApplicationContext context: Context): CronetEngine? {
        return try {
            // Build Cronet directly so we can enable QUIC/HTTP3 — CronetUtil's
            // default builder leaves QUIC off. YouTube's googlevideo CDN is a
            // first-class QUIC endpoint (Google designed the protocol), so
            // enabling it typically shaves 150-400ms off the initial buffer
            // fetch via 0-RTT handshakes and no head-of-line blocking on
            // lossy mobile networks. addQuicHint pre-warms the protocol for
            // googlevideo/youtube hosts so the very first request doesn't
            // need an ALPN round-trip to discover QUIC support.
            // Cronet falls back to TCP/HTTP2 automatically on networks that
            // block UDP, so this is safe even on restrictive corporate WiFi.
            CronetEngine.Builder(context)
                .enableQuic(true)
                .enableHttp2(true)
                .addQuicHint("googlevideo.com", 443, 443)
                .addQuicHint("youtube.com", 443, 443)
                .addQuicHint("youtubei.googleapis.com", 443, 443)
                .setUserAgent("SuvMusic-User-Agent")
                .build()
        } catch (e: Exception) {
            // Fallback to CronetUtil if direct construction fails (e.g. when
            // only the Play Services provider is available and rejects custom
            // configs). HTTP/2-only path is still faster than DefaultHttpDataSource.
            try { CronetUtil.buildCronetEngine(context) } catch (_: Exception) { null }
        }
    }

    @Provides
    @Singleton
    @OptIn(UnstableApi::class)
    fun provideCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
        sessionManager: com.suvojeet.suvmusic.data.SessionManager
    ): Cache {
        // Dynamic cache size from settings (synchronous access).
        // Hard-cap at 4 GB even when user picks "unlimited" (-1) to prevent
        // disk-full crashes — LRU eviction is a no-op with Long.MAX_VALUE.
        val limitPreference = sessionManager.getPlayerCacheLimit()
        val hardCap = 4L * 1024 * 1024 * 1024 // 4 GB
        val cacheSize = when {
            limitPreference == -1L -> hardCap
            limitPreference > hardCap -> hardCap
            limitPreference > 0 -> limitPreference
            else -> hardCap
        }

        val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        val cacheDir = File(context.cacheDir, "media_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    @Provides
    @Singleton
    @PlayerDataSource
    @OptIn(UnstableApi::class)
    fun providePlayerDataSourceFactory(
        @ApplicationContext context: Context,
        cache: Cache,
        cronetEngine: CronetEngine?
    ): DataSource.Factory {
        // Base network factory - Prefer Cronet for QUIC/HTTP3 support
        val httpDataSourceFactory: DataSource.Factory = if (cronetEngine != null) {
            CronetDataSource.Factory(cronetEngine, Executors.newSingleThreadExecutor())
                .setUserAgent("SuvMusic-User-Agent")
        } else {
            DefaultHttpDataSource.Factory()
                .setUserAgent("SuvMusic-User-Agent")
                .setAllowCrossProtocolRedirects(true)
        }
        
        // DefaultDataSource handles http/https/file/content/asset/etc.
        val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        // CacheDataSource Factory
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Provides
    @Singleton
    @DownloadDataSource
    @OptIn(UnstableApi::class)
    fun provideDownloadDataSourceFactory(
        cache: Cache,
        cronetEngine: CronetEngine?
    ): DataSource.Factory {
        // Base network factory - Prefer Cronet for QUIC/HTTP3 support
        val httpDataSourceFactory: DataSource.Factory = if (cronetEngine != null) {
            CronetDataSource.Factory(cronetEngine, Executors.newSingleThreadExecutor())
                .setUserAgent("SuvMusic-User-Agent")
        } else {
            DefaultHttpDataSource.Factory()
                .setUserAgent("SuvMusic-User-Agent")
                .setAllowCrossProtocolRedirects(true)
        }
        
        // CacheDataSource Factory
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}

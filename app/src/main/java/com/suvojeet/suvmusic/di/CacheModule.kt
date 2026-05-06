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
            // Use CronetUtil to build an engine from the best available provider
            // (Google Play Services or fallback)
            CronetUtil.buildCronetEngine(context)
        } catch (e: Exception) {
            null
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

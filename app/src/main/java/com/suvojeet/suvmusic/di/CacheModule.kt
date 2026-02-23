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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
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
    @OptIn(UnstableApi::class)
    fun provideCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
        sessionManager: com.suvojeet.suvmusic.data.SessionManager
    ): Cache {
        // Dynamic cache size from settings (synchronous access)
        val limitPreference = sessionManager.getPlayerCacheLimit()
        // If -1, use Long.MAX_VALUE for effectively unlimited
        val cacheSize = if (limitPreference == -1L) Long.MAX_VALUE else limitPreference
        
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
        cache: Cache
    ): DataSource.Factory {
        // Upstream factory for network requests
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SuvMusic-User-Agent")
            .setAllowCrossProtocolRedirects(true)
        
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
        cache: Cache
    ): DataSource.Factory {
        // Upstream factory for network requests
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SuvMusic-User-Agent")
            .setAllowCrossProtocolRedirects(true)
        
        // CacheDataSource Factory
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}

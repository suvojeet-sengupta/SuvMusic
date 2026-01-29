package com.suvojeet.suvmusic.di

import android.content.Context
import com.google.gson.Gson
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.UpdateRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.player.MusicPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): SessionManager {
        return SessionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideYouTubeRepository(
        sessionManager: SessionManager,
        jsonParser: com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeJsonParser,
        apiClient: com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeApiClient,
        streamingService: com.suvojeet.suvmusic.data.repository.youtube.streaming.YouTubeStreamingService,
        searchService: com.suvojeet.suvmusic.data.repository.youtube.search.YouTubeSearchService,
        networkMonitor: com.suvojeet.suvmusic.utils.NetworkMonitor,
        libraryRepository: com.suvojeet.suvmusic.data.repository.LibraryRepository
    ): YouTubeRepository {
        return YouTubeRepository(sessionManager, jsonParser, apiClient, streamingService, searchService, networkMonitor, libraryRepository)
    }
    
    @Provides
    @Singleton
    fun provideLocalAudioRepository(
        @ApplicationContext context: Context
    ): LocalAudioRepository {
        return LocalAudioRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideUpdateRepository(
        @ApplicationContext context: Context
    ): UpdateRepository {
        return UpdateRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    
    @Provides
    @Singleton
    fun provideJioSaavnRepository(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): JioSaavnRepository {
        return JioSaavnRepository(okHttpClient, gson)
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): com.suvojeet.suvmusic.data.local.AppDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            com.suvojeet.suvmusic.data.local.AppDatabase::class.java,
            "suvmusic_database"
        )
        .fallbackToDestructiveMigration() // For now, recreate DB on schema changes
        .build()
    }
    
    @Provides
    @Singleton
    fun provideListeningHistoryDao(
        database: com.suvojeet.suvmusic.data.local.AppDatabase
    ): com.suvojeet.suvmusic.data.local.dao.ListeningHistoryDao {
        return database.listeningHistoryDao()
    }

    @Provides
    @Singleton
    fun provideLibraryDao(
        database: com.suvojeet.suvmusic.data.local.AppDatabase
    ): com.suvojeet.suvmusic.data.local.dao.LibraryDao {
        return database.libraryDao()
    }
    
    @Provides
    @Singleton
    fun provideMusicHapticsManager(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ): com.suvojeet.suvmusic.util.MusicHapticsManager {
        return com.suvojeet.suvmusic.util.MusicHapticsManager(context, sessionManager)
    }
    
    @Provides
    @Singleton
    fun provideMusicPlayer(
        @ApplicationContext context: Context,
        youTubeRepository: YouTubeRepository,
        jioSaavnRepository: JioSaavnRepository,
        sessionManager: SessionManager,
        sleepTimerManager: com.suvojeet.suvmusic.player.SleepTimerManager,
        listeningHistoryRepository: com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository,
        cache: androidx.media3.datasource.cache.Cache,
        @PlayerDataSource dataSourceFactory: androidx.media3.datasource.DataSource.Factory,
        musicHapticsManager: com.suvojeet.suvmusic.util.MusicHapticsManager
    ): MusicPlayer {
        return MusicPlayer(context, youTubeRepository, jioSaavnRepository, sessionManager, sleepTimerManager, listeningHistoryRepository, cache, dataSourceFactory, musicHapticsManager)
    }
    @Provides
    @Singleton
    fun provideLyricsRepository(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        youTubeRepository: YouTubeRepository,
        jioSaavnRepository: JioSaavnRepository,
        betterLyricsProvider: com.suvojeet.suvmusic.providers.lyrics.BetterLyricsProvider,
        simpMusicLyricsProvider: com.suvojeet.suvmusic.simpmusic.SimpMusicLyricsProvider,
        kuGouLyricsProvider: com.suvojeet.suvmusic.kugou.KuGouLyricsProvider,
        lrcLibLyricsProvider: com.suvojeet.suvmusic.lrclib.LrcLibLyricsProvider,
        sessionManager: SessionManager
    ): com.suvojeet.suvmusic.data.repository.LyricsRepository {
        return com.suvojeet.suvmusic.data.repository.LyricsRepository(
            context,
            okHttpClient,
            youTubeRepository,
            jioSaavnRepository,
            betterLyricsProvider,
            simpMusicLyricsProvider,
            kuGouLyricsProvider,
            lrcLibLyricsProvider,
            sessionManager
        )
    }
}

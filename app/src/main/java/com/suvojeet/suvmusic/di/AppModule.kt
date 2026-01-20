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
        sessionManager: SessionManager
    ): YouTubeRepository {
        return YouTubeRepository(sessionManager)
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
    fun provideMusicPlayer(
        @ApplicationContext context: Context,
        youTubeRepository: YouTubeRepository,
        jioSaavnRepository: JioSaavnRepository,
        sessionManager: SessionManager,
        sleepTimerManager: com.suvojeet.suvmusic.player.SleepTimerManager,
        listeningHistoryRepository: com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
    ): MusicPlayer {
        return MusicPlayer(context, youTubeRepository, jioSaavnRepository, sessionManager, sleepTimerManager, listeningHistoryRepository)
    }
    @Provides
    @Singleton
    fun provideLyricsRepository(
        okHttpClient: OkHttpClient,
        youTubeRepository: YouTubeRepository,
        jioSaavnRepository: JioSaavnRepository
    ): com.suvojeet.suvmusic.data.repository.LyricsRepository {
        return com.suvojeet.suvmusic.data.repository.LyricsRepository(okHttpClient, youTubeRepository, jioSaavnRepository)
    }
}

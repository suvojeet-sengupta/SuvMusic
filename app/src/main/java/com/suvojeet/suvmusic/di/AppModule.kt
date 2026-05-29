package com.suvojeet.suvmusic.di

import android.content.Context
import com.google.gson.Gson
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.RemoteAudioRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.player.MusicPlayer
import com.suvojeet.suvmusic.di.ApplicationScope
import com.suvojeet.suvmusic.player.SpatialAudioProcessor
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.data.local.dao.LyricsDao
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
        networkMonitor: com.suvojeet.suvmusic.util.NetworkMonitor,
        libraryRepository: LibraryRepository,
        listeningHistoryRepository: com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository,
        @ApplicationScope externalScope: kotlinx.coroutines.CoroutineScope
    ): YouTubeRepository {
        return YouTubeRepository(sessionManager, jsonParser, apiClient, streamingService, searchService, networkMonitor, libraryRepository, listeningHistoryRepository, externalScope)
    }
    
    @Provides
    @Singleton
    fun provideLocalAudioRepository(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ): LocalAudioRepository {
        return LocalAudioRepository(context, sessionManager)
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Lightweight HTTP-level tracer for RemoteAudio + NewPipe calls. Logs
        // method, URL host/path, response code, and latency at INFO so it
        // survives release builds (Log.d is stripped by proguard).
        val tracer = okhttp3.Interceptor { chain ->
            val req = chain.request()
            val host = req.url.host
            val path = req.url.encodedPath
            // Include the query so we can correlate a failure with the exact search term
            // / song id that triggered it (the bare path is identical for every search).
            val query = req.url.encodedQuery?.let { "?$it" } ?: ""
            val started = System.currentTimeMillis()
            try {
                val resp = chain.proceed(req)
                val ms = System.currentTimeMillis() - started
                when {
                    // 429 is the saavn/sumit.co rate-limit signal behind the v2.5.1.0
                    // offline-fallback crash. Call it out explicitly with Retry-After so
                    // we can see how hard we're being throttled.
                    resp.code == 429 -> {
                        val retryAfter = resp.header("Retry-After") ?: "n/a"
                        android.util.Log.w("HttpTrace", "RATE_LIMITED 429 ${req.method} ${host}${path}${query} retryAfter=${retryAfter}s in ${ms}ms")
                    }
                    resp.code >= 400 -> android.util.Log.w("HttpTrace", "${req.method} ${host}${path}${query} -> ${resp.code} in ${ms}ms")
                    else -> android.util.Log.i("HttpTrace", "${req.method} ${host}${path}${query} -> ${resp.code} in ${ms}ms")
                }
                resp
            } catch (e: java.io.IOException) {
                val ms = System.currentTimeMillis() - started
                android.util.Log.e("HttpTrace", "${req.method} ${host}${path}${query} threw ${e.javaClass.simpleName} after ${ms}ms: ${e.message}")
                throw e
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(5, 5, java.util.concurrent.TimeUnit.MINUTES))
            .protocols(listOf(okhttp3.Protocol.HTTP_3, okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .addInterceptor(tracer)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideRemoteAudioApiService(okHttpClient: OkHttpClient): com.suvojeet.suvmusic.data.repository.remote.RemoteAudioApiService {
        return retrofit2.Retrofit.Builder()
            .baseUrl(com.suvojeet.suvmusic.data.repository.remote.RemoteConstants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.suvojeet.suvmusic.data.repository.remote.RemoteAudioApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideRemoteAudioRepository(
        okHttpClient: OkHttpClient,
        gson: Gson,
        apiService: com.suvojeet.suvmusic.data.repository.remote.RemoteAudioApiService
    ): RemoteAudioRepository {
        return RemoteAudioRepository(okHttpClient, gson, apiService)
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
        remoteAudioRepository: RemoteAudioRepository,
        sessionManager: SessionManager,
        sleepTimerManager: com.suvojeet.suvmusic.player.SleepTimerManager,
        listeningHistoryRepository: com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository,
        cache: androidx.media3.datasource.cache.Cache,
        @PlayerDataSource dataSourceFactory: androidx.media3.datasource.DataSource.Factory,
        musicHapticsManager: com.suvojeet.suvmusic.util.MusicHapticsManager,
        ttsManager: com.suvojeet.suvmusic.util.TTSManager,
        spatialAudioProcessor: SpatialAudioProcessor,
        nativeSpatialAudio: com.suvojeet.suvmusic.player.NativeSpatialAudio,
        streamingService: com.suvojeet.suvmusic.data.repository.youtube.streaming.YouTubeStreamingService,
        loudnessAnalyzer: com.suvojeet.suvmusic.player.LoudnessAnalyzer,
    ): MusicPlayer {
        return MusicPlayer(
            context,
            youTubeRepository,
            remoteAudioRepository,
            sessionManager,
            sleepTimerManager,
            listeningHistoryRepository,
            cache,
            dataSourceFactory,
            musicHapticsManager,
            ttsManager,
            spatialAudioProcessor,
            nativeSpatialAudio,
            streamingService,
            loudnessAnalyzer,
        )
    }
    @Provides
    @Singleton
    fun provideLyricsRepository(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        youTubeRepository: YouTubeRepository,
        remoteAudioRepository: RemoteAudioRepository,
        betterLyricsProvider: com.suvojeet.suvmusic.providers.lyrics.BetterLyricsProvider,
        simpMusicLyricsProvider: com.suvojeet.suvmusic.simpmusic.SimpMusicLyricsProvider,
        kuGouLyricsProvider: com.suvojeet.suvmusic.kugou.KuGouLyricsProvider,
        lrcLibLyricsProvider: com.suvojeet.suvmusic.lrclib.LrcLibLyricsProvider,
        localLyricsProvider: com.suvojeet.suvmusic.providers.lyrics.LocalLyricsProvider,
        sessionManager: SessionManager,
        lyricsDao: LyricsDao
    ): com.suvojeet.suvmusic.data.repository.LyricsRepository {
        return com.suvojeet.suvmusic.data.repository.LyricsRepository(
            context,
            okHttpClient,
            youTubeRepository,
            remoteAudioRepository,
            betterLyricsProvider,
            simpMusicLyricsProvider,
            kuGouLyricsProvider,
            lrcLibLyricsProvider,
            localLyricsProvider,
            sessionManager,
            lyricsDao
        )
    }

    @Provides
    @Singleton
    fun provideListenTogetherClient(
        @ApplicationContext context: Context
    ): com.suvojeet.suvmusic.shareplay.ListenTogetherClient {
        return com.suvojeet.suvmusic.shareplay.ListenTogetherClient(context)
    }

    @Provides
    @Singleton
    fun provideListenTogetherManager(
        client: com.suvojeet.suvmusic.shareplay.ListenTogetherClient,
        youTubeRepository: YouTubeRepository,
        sessionManager: SessionManager
    ): com.suvojeet.suvmusic.shareplay.ListenTogetherManager {
        val manager = com.suvojeet.suvmusic.shareplay.ListenTogetherManager(client, youTubeRepository, sessionManager)
        manager.initialize()
        return manager
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): androidx.work.WorkManager {
        return androidx.work.WorkManager.getInstance(context)
    }
}

package com.suvojeet.suvmusic.di

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.work.WorkManager
import com.google.gson.Gson
import com.suvojeet.suvmusic.core.data.local.AppDatabase
import com.suvojeet.suvmusic.core.data.local.dao.DislikedItemDao
import com.suvojeet.suvmusic.core.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.dao.SongGenreDao
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.LyricsRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeApiClient
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeJsonParser
import com.suvojeet.suvmusic.data.repository.youtube.search.YouTubeSearchService
import com.suvojeet.suvmusic.data.repository.youtube.streaming.YouTubeStreamingService
import com.suvojeet.suvmusic.ai.AIEqualizerService
import com.suvojeet.suvmusic.data.BackupManager
import com.suvojeet.suvmusic.lastfm.LastFmClient
import com.suvojeet.suvmusic.lastfm.LastFmConfig
import com.suvojeet.suvmusic.lastfm.LastFmRepository
import com.suvojeet.suvmusic.player.AudioARManager
import com.suvojeet.suvmusic.player.MusicPlayer
import com.suvojeet.suvmusic.recommendation.RecommendationEngine
import com.suvojeet.suvmusic.recommendation.WrappedGenerator
import com.suvojeet.suvmusic.shareplay.ListenTogetherClient
import com.suvojeet.suvmusic.shareplay.ListenTogetherManager
import com.suvojeet.suvmusic.updater.UpdateChecker
import com.suvojeet.suvmusic.util.MusicHapticsManager
import com.suvojeet.suvmusic.util.NetworkMonitor
import com.suvojeet.suvmusic.util.PlaylistImportHelper
import com.suvojeet.suvmusic.util.RingtoneHelper
import com.suvojeet.suvmusic.util.SpotifyImportHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

/**
 * Hilt -> Koin bridge for the parallel-DI period of phase 1.
 *
 * Why this exists:
 * Hilt and Koin would otherwise each construct their own copy of every
 * singleton, and shared OS resources (SimpleCache file lock, Room DB lock,
 * ExoPlayer instance, MediaSession, audio focus owner) crash or misbehave on
 * the second construction. We saw `IllegalStateException: Another SimpleCache
 * instance uses the folder` during chunk 1c.1.
 *
 * The bridge has Koin's `single { ... }` blocks delegate to Hilt's already-
 * constructed instances via this @EntryPoint. Result: single source of truth
 * for every shared object during the migration.
 *
 * Lifecycle:
 * - Added: chunk 1c (now). Required for any Koin consumer that resolves a
 *   shared singleton.
 * - Removed: chunk 1d, when Hilt itself is removed. At that point Koin's
 *   `single { ... }` blocks reclaim direct construction.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltKoinBridgeEntryPoint {
    // app/di — AppModule
    fun sessionManager(): SessionManager
    fun youTubeRepository(): YouTubeRepository
    fun localAudioRepository(): LocalAudioRepository
    fun okHttpClient(): OkHttpClient
    fun gson(): Gson
    fun jioSaavnRepository(): JioSaavnRepository
    fun musicHapticsManager(): MusicHapticsManager
    fun musicPlayer(): MusicPlayer
    fun lyricsRepository(): LyricsRepository
    fun listenTogetherClient(): ListenTogetherClient
    fun listenTogetherManager(): ListenTogetherManager
    fun workManager(): WorkManager

    // app/di — CacheModule
    fun cache(): Cache

    @PlayerDataSource
    fun playerDataSourceFactory(): DataSource.Factory

    @DownloadDataSource
    fun downloadDataSourceFactory(): DataSource.Factory

    // app — transitive @Inject constructor classes that 1c.1 VMs reach
    fun youTubeJsonParser(): YouTubeJsonParser
    fun youTubeApiClient(): YouTubeApiClient
    fun youTubeStreamingService(): YouTubeStreamingService
    fun youTubeSearchService(): YouTubeSearchService
    fun networkMonitor(): NetworkMonitor
    fun listeningHistoryRepository(): ListeningHistoryRepository
    fun ringtoneHelper(): RingtoneHelper
    fun downloadRepository(): DownloadRepository

    // core/data
    fun appDatabase(): AppDatabase
    fun libraryDao(): LibraryDao
    fun listeningHistoryDao(): ListeningHistoryDao
    fun dislikedItemDao(): DislikedItemDao
    fun songGenreDao(): SongGenreDao
    fun libraryRepository(): LibraryRepository

    // scrobbler
    fun lastFmConfig(): LastFmConfig
    fun lastFmClient(): LastFmClient

    // updater
    fun updateChecker(): UpdateChecker

    // chunk 1c.3 — additional transitive @Inject constructor classes
    fun spotifyImportHelper(): SpotifyImportHelper
    fun playlistImportHelper(): PlaylistImportHelper
    fun recommendationEngine(): RecommendationEngine
    fun lastFmRepository(): LastFmRepository
    fun audioARManager(): AudioARManager
    fun aiEqualizerService(): AIEqualizerService
    fun wrappedGenerator(): WrappedGenerator
    fun backupManager(): BackupManager
}

/** One-call accessor used by Koin module blocks. Resolved against the application Context. */
internal fun bridge(context: Context): HiltKoinBridgeEntryPoint =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        HiltKoinBridgeEntryPoint::class.java,
    )

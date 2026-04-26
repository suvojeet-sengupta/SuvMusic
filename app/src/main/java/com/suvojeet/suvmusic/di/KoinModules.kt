package com.suvojeet.suvmusic.di

import androidx.annotation.OptIn as AndroidxOptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.room.Room
import androidx.work.WorkManager
import com.google.gson.Gson
import com.suvojeet.suvmusic.core.data.local.AppDatabase
import com.suvojeet.suvmusic.core.data.repository.LibraryRepositoryImpl
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.LyricsRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.lastfm.LastFmClient
import com.suvojeet.suvmusic.lastfm.LastFmConfig
import com.suvojeet.suvmusic.lastfm.LastFmConfigImpl
import com.suvojeet.suvmusic.player.MusicPlayer
import com.suvojeet.suvmusic.shareplay.ListenTogetherClient
import com.suvojeet.suvmusic.shareplay.ListenTogetherManager
import com.suvojeet.suvmusic.updater.UpdateChecker
import com.suvojeet.suvmusic.util.MusicHapticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import java.util.concurrent.TimeUnit

// Stable Koin qualifier names mirroring the existing Hilt @Qualifier annotations.
// Kept as strings (not typed qualifiers) to stay decoupled from the annotation
// classes themselves — those get deleted with Hilt in chunk 1d.
internal const val Q_PLAYER_DATA_SOURCE = "PlayerDataSource"
internal const val Q_DOWNLOAD_DATA_SOURCE = "DownloadDataSource"
internal const val Q_APPLICATION_SCOPE = "ApplicationScope"

/**
 * Koin equivalent of [com.suvojeet.suvmusic.di.AppModule]. Translates each
 * @Provides binding 1:1. Transitive @Inject constructor classes are NOT yet
 * declared — they get singleOf(::Class) entries in chunk 1c as each ViewModel
 * is converted and reveals what it actually needs.
 */
private val appModule: Module = module {
    single { Gson() }

    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build()
    }

    single { SessionManager(androidContext()) }
    single { MusicHapticsManager(androidContext(), get()) }
    single { LocalAudioRepository(androidContext(), get()) }
    single { ListenTogetherClient(androidContext()) }
    single { JioSaavnRepository(get(), get()) }
    single { WorkManager.getInstance(androidContext()) }

    single {
        YouTubeRepository(
            get(),  // sessionManager
            get(),  // jsonParser
            get(),  // apiClient
            get(),  // streamingService
            get(),  // searchService
            get(),  // networkMonitor
            get(),  // libraryRepository
            get(),  // listeningHistoryRepository
            get(named(Q_APPLICATION_SCOPE)),
        )
    }

    single {
        ListenTogetherManager(get(), get(), get()).also { it.initialize() }
    }

    single {
        LyricsRepository(
            androidContext(),
            get(),  // okHttpClient
            get(),  // youTubeRepository
            get(),  // jioSaavnRepository
            get(),  // betterLyricsProvider
            get(),  // simpMusicLyricsProvider
            get(),  // kuGouLyricsProvider
            get(),  // lrcLibLyricsProvider
            get(),  // localLyricsProvider
            get(),  // sessionManager
        )
    }

    single {
        MusicPlayer(
            androidContext(),
            get(),  // youTubeRepository
            get(),  // jioSaavnRepository
            get(),  // sessionManager
            get(),  // sleepTimerManager
            get(),  // listeningHistoryRepository
            get(),  // cache
            get(named(Q_PLAYER_DATA_SOURCE)),
            get(),  // musicHapticsManager
            get(),  // ttsManager
            get(),  // spatialAudioProcessor
            get(),  // nativeSpatialAudio
            get(),  // streamingService
        )
    }
}

/**
 * Koin equivalent of [com.suvojeet.suvmusic.di.CacheModule]. Reuses the same
 * dynamic cache-size logic from SessionManager.
 */
@AndroidxOptIn(UnstableApi::class)
private val cacheModule: Module = module {
    single<DatabaseProvider> { StandaloneDatabaseProvider(androidContext()) }

    single<Cache> {
        val sessionManager: SessionManager = get()
        val limitPreference = sessionManager.getPlayerCacheLimit()
        val hardCap = 4L * 1024 * 1024 * 1024 // 4 GB
        val cacheSize = when {
            limitPreference == -1L -> hardCap
            limitPreference > hardCap -> hardCap
            limitPreference > 0 -> limitPreference
            else -> hardCap
        }

        val cacheDir = File(androidContext().cacheDir, "media_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(cacheSize), get())
    }

    single<DataSource.Factory>(named(Q_PLAYER_DATA_SOURCE)) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SuvMusic-User-Agent")
            .setAllowCrossProtocolRedirects(true)

        val upstreamFactory = DefaultDataSource.Factory(androidContext(), httpDataSourceFactory)

        CacheDataSource.Factory()
            .setCache(get())
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    single<DataSource.Factory>(named(Q_DOWNLOAD_DATA_SOURCE)) {
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SuvMusic-User-Agent")
            .setAllowCrossProtocolRedirects(true)

        CacheDataSource.Factory()
            .setCache(get())
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}

/**
 * Koin equivalent of [com.suvojeet.suvmusic.di.CoroutineScopesModule].
 */
private val coroutineScopesModule: Module = module {
    single<CoroutineScope>(named(Q_APPLICATION_SCOPE)) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

/**
 * Koin equivalent of `core.data.di.DatabaseModule` (Hilt). The Room database is
 * a single, the DAOs are factory-derived from it.
 */
private val databaseModule: Module = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "suvmusic_database",
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().libraryDao() }
    single { get<AppDatabase>().listeningHistoryDao() }
    single { get<AppDatabase>().dislikedItemDao() }
    single { get<AppDatabase>().songGenreDao() }
}

/**
 * Koin equivalent of `core.data.di.RepositoryModule` (Hilt @Binds).
 */
private val repositoryModule: Module = module {
    single<LibraryRepository> { LibraryRepositoryImpl(get()) }
}

/**
 * Koin equivalent of `scrobbler.lastfm.di.LastFmModule`.
 */
private val lastFmModule: Module = module {
    single<LastFmConfig> { LastFmConfigImpl() }
    single { LastFmClient(get()) }
}

/**
 * Koin equivalent of `updater.UpdaterModule`.
 */
private val updaterModule: Module = module {
    single { UpdateChecker(get()) }
}

/**
 * Aggregated Koin module list registered with `startKoin` in
 * [com.suvojeet.suvmusic.SuvMusicApplication]. Hilt remains the active DI
 * framework — nothing yet resolves through Koin.
 */
val koinAppModules: List<Module> = listOf(
    appModule,
    cacheModule,
    coroutineScopesModule,
    databaseModule,
    repositoryModule,
    lastFmModule,
    updaterModule,
)

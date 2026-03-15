package com.suvojeet.suvmusic

import android.app.Application
import android.content.Context
import com.suvojeet.suvmusic.BuildConfig
import com.suvojeet.suvmusic.R
import dagger.hilt.android.HiltAndroidApp
import org.acra.ACRA
import org.acra.config.*
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.*
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import okio.Path.Companion.toPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Application class for SuvMusic.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SuvMusicApplication : Application(), SingletonImageLoader.Factory, androidx.work.Configuration.Provider {
    
    @javax.inject.Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @javax.inject.Inject
    lateinit var sessionManager: com.suvojeet.suvmusic.data.SessionManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        try {
            initAcra {
                buildConfigClass = BuildConfig::class.java
                reportFormat = StringFormat.JSON

                // Capture more logs for context (default is 100)
                logcatArguments = listOf("-t", "200", "-v", "time")

                notification {
                    title = base.getString(R.string.acra_crash_title)
                    text = base.getString(R.string.acra_crash_text)
                    channelName = base.getString(R.string.app_name)
                    enabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val applicationScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Initialize logging early
        applicationScope.launch {
            val enabled = sessionManager.isLoggingEnabled()
            withContext(Dispatchers.Main) {
                com.suvojeet.suvmusic.util.AppLog.init(this@SuvMusicApplication, enabled)
                com.suvojeet.suvmusic.util.AppLog.i("SuvMusicApplication") { "App initialization started" }
            }
        }

        // Initialize any app-wide components here on a background thread
        applicationScope.launch {
            com.suvojeet.suvmusic.util.AppLog.d("SuvMusicApplication") { "Setting up workers" }
            setupWorkers()
        }
    }
    
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.30) // Use 30% of available heap for images (was 25%)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(150 * 1024 * 1024) // 150MB disk cache — sensible for album art
                    .build()
            }
            // Aggressive caching for offline support and smoothness
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .build()
    }
    
    private fun setupWorkers() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.suvojeet.suvmusic.workers.NewReleaseWorker>(
            12, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NewReleaseCheck",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

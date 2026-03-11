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

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Application class for SuvMusic.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SuvMusicApplication : Application(), ImageLoaderFactory, androidx.work.Configuration.Provider {
    
    @javax.inject.Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @javax.inject.Inject
    lateinit var sessionManager: com.suvojeet.suvmusic.data.SessionManager

    companion object {
        lateinit var instance: SuvMusicApplication
            private set
    }

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
        instance = this
        
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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // Use 30% of available heap for images (was 25%)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512 * 1024 * 1024) // 512MB dedicated disk cache
                    .build()
            }
            // Aggressive caching for offline support and smoothness
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true) // Ensure hardware bitmaps are used for efficiency
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

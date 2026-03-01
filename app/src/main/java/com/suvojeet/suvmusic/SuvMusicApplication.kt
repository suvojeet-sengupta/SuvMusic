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

/**
 * Application class for SuvMusic.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SuvMusicApplication : Application(), ImageLoaderFactory, androidx.work.Configuration.Provider {
    
    @javax.inject.Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

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

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize any app-wide components here on a background thread
        CoroutineScope(Dispatchers.IO).launch {
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
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Use 2% of disk space (approx 1GB on 50GB phone)
                    .build()
            }
            // Aggressive caching for offline support
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .logger(DebugLogger())
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

package com.suvojeet.suvmusic.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.suvojeet.suvmusic.core.domain.sync.LibrarySyncScheduler
import com.suvojeet.suvmusic.data.worker.LikedSongsSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Android host adapter retaining the existing WorkManager worker implementation. */
@Singleton
class AndroidLibrarySyncScheduler @Inject constructor(
    context: Context,
) : LibrarySyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedulePeriodic(intervalHours: Long) {
        val request = PeriodicWorkRequestBuilder<LikedSongsSyncWorker>(
            intervalHours.coerceAtLeast(1L), TimeUnit.HOURS,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun enqueueNow() {
        workManager.enqueueUniqueWork(
            UNIQUE_NOW,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<LikedSongsSyncWorker>().build(),
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_WORK)
        workManager.cancelUniqueWork(UNIQUE_NOW)
    }

    private companion object {
        const val UNIQUE_WORK = "suvmusic_library_sync"
        const val UNIQUE_NOW = "suvmusic_library_sync_now"
    }
}

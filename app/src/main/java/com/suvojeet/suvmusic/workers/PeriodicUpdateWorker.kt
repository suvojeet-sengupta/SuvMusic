package com.suvojeet.suvmusic.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.updater.UpdateChecker
import com.suvojeet.suvmusic.core.model.UpdateChannel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PeriodicUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateChecker: UpdateChecker,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("PeriodicUpdateWorker", "Checking for updates in background...")

        return try {
            val updateChannel = sessionManager.getUpdateChannel()
            val isNightly = updateChannel == UpdateChannel.NIGHTLY
            val updateInfo = updateChecker.checkForUpdate(isNightly)

            if (updateInfo != null) {
                val currentVersionCode = getVersionCode()
                // Compare as Long so a large versionCode can't overflow when truncated.
                if (updateInfo.versionCode.toLong() > currentVersionCode) {
                    Log.i("PeriodicUpdateWorker", "New update available: ${updateInfo.versionName}")
                    sessionManager.setPendingUpdateInfo(updateInfo.versionCode, updateInfo.versionName)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("PeriodicUpdateWorker", "Update check failed (attempt ${runAttemptCount})", e)
            // Cap retries so a persistently failing check (e.g. offline for hours)
            // doesn't retry unbounded and drain battery/network.
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private fun getVersionCode(): Long {
        return try {
            val pInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}

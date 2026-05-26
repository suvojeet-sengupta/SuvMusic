package com.suvojeet.suvmusic.updater

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.SessionManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import com.suvojeet.suvmusic.core.model.UpdateChannel

class PeriodicUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val updateChecker: UpdateChecker by inject()
    private val sessionManager: SessionManager by inject()

    override suspend fun doWork(): Result {
        Log.d("PeriodicUpdateWorker", "Checking for updates in background...")
        
        try {
            val updateChannel = sessionManager.getUpdateChannel()
            val isNightly = updateChannel == UpdateChannel.NIGHTLY
            val updateInfo = updateChecker.checkForUpdate(isNightly)
            
            if (updateInfo != null) {
                val currentVersionCode = getVersionCode()
                if (updateInfo.versionCode > currentVersionCode) {
                    Log.i("PeriodicUpdateWorker", "New update available: ${updateInfo.versionName}")
                    // Note: We don't show a dialog from a background worker.
                    // Instead, we could post a notification or set a flag.
                    // For now, let's just update a "new update available" flag in session manager
                    // so the UI can show a badge or prompt on next launch.
                    sessionManager.setPendingUpdateInfo(updateInfo.versionCode, updateInfo.versionName)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("PeriodicUpdateWorker", "Update check failed", e)
            return Result.retry()
        }
    }

    private fun getVersionCode(): Int {
        return try {
            val pInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }
}

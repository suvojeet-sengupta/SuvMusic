package com.suvojeet.suvmusic.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.updater.UpdateChecker
import com.suvojeet.suvmusic.core.model.UpdateChannel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

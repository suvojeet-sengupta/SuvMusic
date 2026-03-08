package com.suvojeet.suvmusic.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.suvojeet.suvmusic.data.model.UpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val updateUrl = "https://cdn.jsdelivr.net/gh/suvojeet-sengupta/SuvMusic@main/update.json"

    /**
     * Fetches update JSON from CDN and returns UpdateInfo if a new version is available.
     * Fails silently by returning null on network or parsing errors.
     */
    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(updateUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            val updateInfo = json.decodeFromString<UpdateInfo>(responseBody)

            val currentVersionCode = getAppVersionCode()
            if (updateInfo.versionCode > currentVersionCode) {
                updateInfo
            } else {
                null
            }
        } catch (e: Exception) {
            // Fail silently
            null
        }
    }

    private fun getAppVersionCode(): Long {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
}

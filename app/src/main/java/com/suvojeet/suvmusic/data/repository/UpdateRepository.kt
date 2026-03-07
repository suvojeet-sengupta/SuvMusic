package com.suvojeet.suvmusic.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.AppUpdate
import com.suvojeet.suvmusic.data.model.UpdateState
import com.suvojeet.suvmusic.data.model.UpdateChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for checking and downloading app updates from GitHub Releases.
 */
@Singleton
class UpdateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()
    
    private val gson = Gson()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var downloadedApkFile: File? = null
    
    companion object {
        // GitHub repository info
        private const val GITHUB_OWNER = "suvojeet-sengupta"
        private const val GITHUB_REPO = "SuvMusic"
        
        // Interval for automatic update checks (24 hours)
        private const val AUTO_CHECK_INTERVAL = 24 * 60 * 60 * 1000L
    }
    
    /**
     * Check for updates from GitHub Releases.
     */
    suspend fun checkForUpdate(
        channel: UpdateChannel,
        forceCheck: Boolean = false
    ): Result<AppUpdate?> = withContext(Dispatchers.IO) {
        // Check if we should skip automatic check
        if (!forceCheck) {
            val lastCheckTime = sessionManager.getLastUpdateCheckTime()
            if (System.currentTimeMillis() - lastCheckTime < AUTO_CHECK_INTERVAL) {
                return@withContext Result.success(null)
            }
        }

        _updateState.value = UpdateState.Checking

        try {
            // Fetch releases list
            val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=10"
            
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "SuvMusic-App")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                if (response.code == 403) {
                    val rateLimitError = "GitHub API Rate limit exceeded. Try again later."
                    if (forceCheck) _updateState.value = UpdateState.Error(rateLimitError)
                    return@withContext Result.failure(Exception(rateLimitError))
                }
                val httpError = "Failed to check for updates: HTTP ${response.code}"
                if (forceCheck) _updateState.value = UpdateState.Error(httpError)
                return@withContext Result.failure(Exception(httpError))
            }
            
            val body = response.body.string()
            val releasesArray = gson.fromJson(body, JsonArray::class.java)
            
            if (releasesArray.size() == 0) {
                sessionManager.setLastUpdateCheckTime(System.currentTimeMillis())
                _updateState.value = if (forceCheck) UpdateState.NoUpdate else UpdateState.Idle
                return@withContext Result.success(null)
            }
            
            // Filter releases based on channel
            val selectedRelease = when (channel) {
                UpdateChannel.STABLE -> {
                    var latestStable: JsonObject? = null
                    for (element in releasesArray) {
                        val release = element.asJsonObject
                        if (release.get("prerelease")?.asBoolean == false && release.get("draft")?.asBoolean == false) {
                            latestStable = release
                            break
                        }
                    }
                    latestStable
                }
                UpdateChannel.NIGHTLY -> {
                    var latestNightly: JsonObject? = null
                    for (element in releasesArray) {
                        val release = element.asJsonObject
                        if (release.get("draft")?.asBoolean == false) {
                            latestNightly = release
                            break
                        }
                    }
                    latestNightly
                }
            } 
            
            if (selectedRelease == null) {
                sessionManager.setLastUpdateCheckTime(System.currentTimeMillis())
                _updateState.value = if (forceCheck) UpdateState.NoUpdate else UpdateState.Idle
                return@withContext Result.success(null)
            }

            val tagName = selectedRelease.get("tag_name")?.asString ?: return@withContext Result.failure(Exception("No tag found"))
            val versionName = tagName.removePrefix("v")
            val releaseNotes = selectedRelease.get("body")?.asString ?: "No release notes available"
            val publishedAt = selectedRelease.get("published_at")?.asString ?: ""
            val isPreRelease = selectedRelease.get("prerelease")?.asBoolean ?: false
            
            val assets = selectedRelease.getAsJsonArray("assets")
            var downloadUrl = ""
            val priorityKeywords = listOf("universal", "arm64-v8a", "arm64", "v8a")
            
            for (keyword in priorityKeywords) {
                for (asset in assets) {
                    val assetObj = asset.asJsonObject
                    val name = assetObj.get("name")?.asString?.lowercase() ?: ""
                    if (name.endsWith(".apk") && name.contains(keyword)) {
                        downloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                        break
                    }
                }
                if (downloadUrl.isNotEmpty()) break
            }
            
            if (downloadUrl.isEmpty()) {
                for (asset in assets) {
                    val assetObj = asset.asJsonObject
                    val name = assetObj.get("name")?.asString?.lowercase() ?: ""
                    if (name.endsWith(".apk")) {
                        downloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                        break
                    }
                }
            }
            
            if (downloadUrl.isEmpty()) {
                val noApkError = "No APK found in the latest ${channel.name.lowercase()} release"
                if (forceCheck) _updateState.value = UpdateState.Error(noApkError)
                return@withContext Result.failure(Exception(noApkError))
            }
            
            val currentVersionName = getCurrentVersionName()
            val remoteVersionCode = parseVersionCode(versionName)
            val currentVersionCode = parseVersionCode(currentVersionName)
            
            val update = AppUpdate(
                versionName = versionName,
                versionCode = remoteVersionCode,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                publishedAt = publishedAt,
                isPreRelease = isPreRelease
            )
            
            val isNewer = isVersionNewer(
                currentVersion = currentVersionName,
                remoteVersion = versionName,
                currentCode = currentVersionCode,
                remoteCode = remoteVersionCode,
                currentTimestamp = getLocalLastUpdateTime(),
                remoteTimestamp = parsePublishedDate(publishedAt)
            )

            sessionManager.setLastUpdateCheckTime(System.currentTimeMillis())

            if (isNewer) {
                _updateState.value = UpdateState.UpdateAvailable(update)
                Result.success(update)
            } else {
                _updateState.value = if (forceCheck) UpdateState.NoUpdate else UpdateState.Idle
                Result.success(null)
            }
        } catch (e: Exception) {
            if (forceCheck) _updateState.value = UpdateState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    private fun isVersionNewer(
        currentVersion: String,
        remoteVersion: String,
        currentCode: Int,
        remoteCode: Int,
        currentTimestamp: Long,
        remoteTimestamp: Long
    ): Boolean {
        if (remoteCode > currentCode) return true
        if (remoteCode < currentCode) return false
        if (currentVersion == remoteVersion) return false 
        
        return try {
            compareVersions(remoteVersion, currentVersion) > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val cleanV1 = v1.removePrefix("v")
        val cleanV2 = v2.removePrefix("v")
        val parts1 = cleanV1.split("-", limit = 2)
        val parts2 = cleanV2.split("-", limit = 2)
        val numeric1 = parts1[0].split(".")
        val numeric2 = parts2[0].split(".")
        
        val length = maxOf(numeric1.size, numeric2.size)
        for (i in 0 until length) {
            val n1 = numeric1.getOrNull(i)?.toIntOrNull() ?: 0
            val n2 = numeric2.getOrNull(i)?.toIntOrNull() ?: 0
            if (n1 != n2) return n1.compareTo(n2)
        }
        
        val q1 = parts1.getOrNull(1)
        val q2 = parts2.getOrNull(1)
        if (q1 == q2) return 0
        if (q1 == null) return 1 
        if (q2 == null) return -1 
        return q1.compareTo(q2, ignoreCase = true)
    }

    private fun parsePublishedDate(dateString: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getLocalLastUpdateTime(): Long {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    suspend fun downloadApk(
        downloadUrl: String,
        versionName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Downloading(0)
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val error = "Download failed: ${response.code}"
                _updateState.value = UpdateState.Error(error)
                return@withContext Result.failure(Exception(error))
            }
            
            val body = response.body
            val contentLength = body.contentLength()
            
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()
            updatesDir.listFiles()?.forEach { it.delete() }
            
            val apkFile = File(updatesDir, "SuvMusic-$versionName.apk")
            
            FileOutputStream(apkFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                var read: Int
                
                body.byteStream().use { input ->
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (contentLength > 0) {
                            val progress = ((bytesRead * 100) / contentLength).toInt()
                            _updateState.value = UpdateState.Downloading(progress)
                        }
                    }
                }
            }
            
            downloadedApkFile = apkFile
            _updateState.value = UpdateState.Downloaded
            Result.success(apkFile)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    fun installUpdate() {
        val apkFile = downloadedApkFile ?: return
        if (!apkFile.exists()) {
            _updateState.value = UpdateState.Error("APK file not found")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to install: ${e.message}")
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }
    
    private fun parseVersionCode(versionName: String): Int {
        return try {
            val cleanVersion = versionName.split("-")[0].split("+")[0]
            val parts = cleanVersion.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val build = parts.getOrNull(3)?.toIntOrNull() ?: 0
            // Using base 100 for each part to avoid Int overflow while supporting 4 parts
            // Max value: 21 * 1,000,000 + 99 * 10,000 + 99 * 100 + 99 = 21,999,999 (safe within Int.MAX_VALUE)
            // Actually let's use slightly more space for major
            major * 1000000 + minor * 10000 + patch * 100 + build
        } catch (e: Exception) {
            0
        }
    }
}

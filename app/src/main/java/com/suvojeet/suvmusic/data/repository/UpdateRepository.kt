package com.suvojeet.suvmusic.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.suvojeet.suvmusic.BuildConfig
import com.suvojeet.suvmusic.data.model.AppUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()
    
    private val gson = Gson()
    
    companion object {
        // GitHub repository info - update these with your repo details
        private const val GITHUB_OWNER = "suvojeet-sengupta"
        private const val GITHUB_REPO = "SuvMusic"
        private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }
    
    /**
     * Check for updates from GitHub Releases.
     * Returns AppUpdate if available, null otherwise.
     */
    suspend fun checkForUpdate(channel: com.suvojeet.suvmusic.data.model.UpdateChannel): Result<AppUpdate?> = withContext(Dispatchers.IO) {
        try {
            // Fetch releases list (max 5) to find the latest based on channel
            val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=10"
            
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "SuvMusic-App") // Required by GitHub API
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                if (response.code == 403) {
                    return@withContext Result.failure(Exception("GitHub API Rate limit exceeded. Try again later."))
                }
                return@withContext Result.failure(Exception("Failed to check for updates: HTTP ${response.code}"))
            }
            
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val releasesArray = gson.fromJson(body, com.google.gson.JsonArray::class.java)
            
            if (releasesArray.size() == 0) {
                return@withContext Result.success(null)
            }
            
            // Filter releases based on channel
            val selectedRelease = when (channel) {
                com.suvojeet.suvmusic.data.model.UpdateChannel.STABLE -> {
                    // Find the first release that is NOT a pre-release
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
                com.suvojeet.suvmusic.data.model.UpdateChannel.NIGHTLY -> {
                    // Just pick the absolute latest non-draft release
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
            } ?: return@withContext Result.success(null)

            // Parse version from tag_name (e.g., "v1.2.0-beta.1" -> "1.2.0-beta.1")
            val tagName = selectedRelease.get("tag_name")?.asString ?: return@withContext Result.failure(Exception("No tag found"))
            val versionName = tagName.removePrefix("v")
            
            // Parse release notes, date, and pre-release status
            val releaseNotes = selectedRelease.get("body")?.asString ?: "No release notes available"
            val publishedAt = selectedRelease.get("published_at")?.asString ?: ""
            val isPreRelease = selectedRelease.get("prerelease")?.asBoolean ?: false
            
            // Find APK download URL from assets (prioritize arm64-v8a or universal if possible)
            val assets = selectedRelease.getAsJsonArray("assets")
            var downloadUrl = ""
            
            // Prioritize architectures: Universal > arm64-v8a > Others
            val priorityKeywords = listOf("universal", "arm64-v8a", "arm64", "v8a")
            
            // First pass: try to find priority assets
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
            
            // Second pass: fallback to any .apk if no priority found
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
                return@withContext Result.failure(Exception("No APK found in the latest ${channel.name.lowercase()} release"))
            }
            
            val currentVersionName = getCurrentVersionName()
            val remoteVersionCode = parseVersionCode(versionName)
            val currentVersionCode = getCurrentVersionCode()
            
            val update = AppUpdate(
                versionName = versionName,
                versionCode = remoteVersionCode,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                publishedAt = publishedAt,
                isPreRelease = isPreRelease
            )
            
            // Robust version comparison
            val isNewer = isVersionNewer(
                currentVersion = currentVersionName,
                remoteVersion = versionName,
                currentCode = currentVersionCode,
                remoteCode = remoteVersionCode,
                currentTimestamp = getLocalLastUpdateTime(),
                remoteTimestamp = parsePublishedDate(publishedAt)
            )

            if (isNewer) {
                Result.success(update)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Determines if the remote version is newer than the current version.
     */
    private fun isVersionNewer(
        currentVersion: String,
        remoteVersion: String,
        currentCode: Int,
        remoteCode: Int,
        currentTimestamp: Long,
        remoteTimestamp: Long
    ): Boolean {
        // 1. Primary: Compare version codes
        if (remoteCode > currentCode) return true
        if (remoteCode < currentCode) return false
        
        // 2. Secondary: If codes match, check version name strings (e.g. 1.2.0 vs 1.2.0-beta.1)
        // If they are exactly the same, check timestamp
        if (currentVersion == remoteVersion) {
            // For Nightly, same version might mean a re-published build
            return remoteTimestamp > currentTimestamp + 60000 // 1 minute buffer
        }
        
        // Use a simple semantic-like comparison for version names if codes match
        return try {
            compareVersions(remoteVersion, currentVersion) > 0
        } catch (e: Exception) {
            // Fallback to timestamp if semantic parsing fails
            remoteTimestamp > currentTimestamp
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".", "-", "+")
        val parts2 = v2.split(".", "-", "+")
        val length = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until length) {
            val p1 = parts1.getOrNull(i)
            val p2 = parts2.getOrNull(i)
            
            if (p1 == p2) continue
            if (p1 == null) return -1
            if (p2 == null) return 1
            
            val i1 = p1.toIntOrNull()
            val i2 = p2.toIntOrNull()
            
            if (i1 != null && i2 != null) {
                if (i1 != i2) return i1.compareTo(i2)
            } else {
                val res = p1.compareTo(p2, ignoreCase = true)
                if (res != 0) return res
            }
        }
        return 0
    }

    private fun parsePublishedDate(dateString: String): Long {
        return try {
            // ISO 8601 format: 2024-01-28T10:00:00Z
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
            System.currentTimeMillis() // Fallback to now to avoid unnecessary updates if check fails
        }
    }
    
    /**
     * Download APK file to cache directory.
     * Returns the downloaded file path.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        versionName: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }
            
            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()
            
            // Create updates directory in cache
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }
            
            // Clean old APKs
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
                            onProgress(progress)
                        }
                    }
                }
            }
            
            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current app version code.
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }
    
    /**
     * Get current app version name.
     */
    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }
    
    /**
     * Parse version name to version code.
     * e.g., "1.0.3" -> 10003 (major * 10000 + minor * 100 + patch)
     */
    private fun parseVersionCode(versionName: String): Int {
        return try {
            val parts = versionName.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            0
        }
    }
}

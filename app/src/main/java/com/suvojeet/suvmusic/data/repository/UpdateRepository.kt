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
            val currentVersionCode = parseVersionCode(currentVersionName)
            
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
        // 1. Primary: Compare version codes (generated from version names)
        if (remoteCode > currentCode) return true
        if (remoteCode < currentCode) return false
        
        // 2. Secondary: If codes match, check if they are exactly the same
        if (currentVersion == remoteVersion) {
            // For Nightly, same version might mean a re-published build
            // But only if the remote build is significantly newer than the local install
            // And only if it's a pre-release/nightly build
            // Actually, to avoid annoying users, if version matches exactly, we should NOT update
            // unless we have a specific reason. Most users don't want to re-download the same thing.
            return false 
        }
        
        // 3. Tertiary: Compare suffixes (e.g. 1.3.0.0-nightly vs 1.3.0.0)
        // Usually, 1.3.0.0 is considered NEWER than 1.3.0.0-beta
        return try {
            compareVersions(remoteVersion, currentVersion) > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        // Remove 'v' prefix if present
        val cleanV1 = v1.removePrefix("v")
        val cleanV2 = v2.removePrefix("v")

        // Split into numeric part and qualifier part (e.g. "1.2.0" and "beta.1")
        val parts1 = cleanV1.split("-", limit = 2)
        val parts2 = cleanV2.split("-", limit = 2)
        
        val numeric1 = parts1[0].split(".")
        val numeric2 = parts2[0].split(".")
        
        // Compare numeric parts
        val length = maxOf(numeric1.size, numeric2.size)
        for (i in 0 until length) {
            val n1 = numeric1.getOrNull(i)?.toIntOrNull() ?: 0
            val n2 = numeric2.getOrNull(i)?.toIntOrNull() ?: 0
            if (n1 != n2) return n1.compareTo(n2)
        }
        
        // If numeric parts are identical, check qualifiers
        val q1 = parts1.getOrNull(1)
        val q2 = parts2.getOrNull(1)
        
        if (q1 == q2) return 0
        if (q1 == null) return 1 // v1 has no qualifier, so it's a stable release, newer than v2's pre-release
        if (q2 == null) return -1 // v2 has no qualifier, so it's newer
        
        // Both have qualifiers, compare them lexicographically
        return q1.compareTo(q2, ignoreCase = true)
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
     * Supports both 3-digit (1.0.0) and 4-digit (1.0.0.0) formats.
     * Format: major * 1000000 + minor * 10000 + patch * 100 + build
     */
    private fun parseVersionCode(versionName: String): Int {
        return try {
            // Remove any non-numeric suffixes (like -beta or -nightly) for code calculation
            val cleanVersion = versionName.split("-")[0].split("+")[0]
            val parts = cleanVersion.split(".")
            
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val build = parts.getOrNull(3)?.toIntOrNull() ?: 0
            
            // Using a larger multiplier to accommodate 4 parts safely
            major * 1000000 + minor * 10000 + patch * 100 + build
        } catch (e: Exception) {
            0
        }
    }
}

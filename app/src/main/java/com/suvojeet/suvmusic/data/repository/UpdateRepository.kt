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
    suspend fun checkForUpdate(): Result<AppUpdate?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to check for updates: ${response.code}"))
            }
            
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = gson.fromJson(body, JsonObject::class.java)
            
            // Parse version from tag_name (e.g., "v1.0.3" -> "1.0.3")
            val tagName = json.get("tag_name")?.asString ?: return@withContext Result.failure(Exception("No tag found"))
            val versionName = tagName.removePrefix("v")
            
            // Parse release notes
            val releaseNotes = json.get("body")?.asString ?: "No release notes available"
            
            // Parse published date
            val publishedAt = json.get("published_at")?.asString ?: ""
            
            // Check if pre-release
            val isPreRelease = json.get("prerelease")?.asBoolean ?: false
            
            // Find APK download URL from assets
            val assets = json.getAsJsonArray("assets")
            var downloadUrl = ""
            
            for (asset in assets) {
                val assetObj = asset.asJsonObject
                val name = assetObj.get("name")?.asString ?: ""
                if (name.endsWith(".apk")) {
                    downloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                    break
                }
            }
            
            if (downloadUrl.isEmpty()) {
                return@withContext Result.failure(Exception("No APK found in release"))
            }
            
            // Parse version code from version name (e.g., "1.0.3" -> assume each part)
            val remoteVersionCode = parseVersionCode(versionName)
            val currentVersionCode = parseVersionCode(getCurrentVersionName())
            
            val update = AppUpdate(
                versionName = versionName,
                versionCode = remoteVersionCode,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                publishedAt = publishedAt,
                isPreRelease = isPreRelease
            )
            
            // Check if update is available
            if (remoteVersionCode > currentVersionCode) {
                Result.success(update)
            } else {
                Result.success(null) // No update available
            }
        } catch (e: Exception) {
            Result.failure(e)
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

package com.suvojeet.suvmusic.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

class UpdateChecker(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private val url = "https://raw.githubusercontent.com/suvojeet-sengupta/SuvMusic/main/updater/update.json"
    private val changelogUrl = "https://raw.githubusercontent.com/suvojeet-sengupta/SuvMusic/main/updater/changelog.json"

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                json.decodeFromString<UpdateInfo>(body)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchChangelog(): ChangelogInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(changelogUrl)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                json.decodeFromString<ChangelogInfo>(body)
            }
        } catch (e: Exception) {
            null
        }
    }
}

package com.suvojeet.suvmusic.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

class UpdateChecker(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    // Statically.io is a specialized CDN for GitHub, more robust than jsDelivr for frequently updated files.
    private val baseUrl = "https://cdn.statically.io/gh/suvojeet-sengupta/SuvMusic/main/updater"

    private suspend fun <T> fetchJson(fileName: String, serializer: kotlinx.serialization.KSerializer<T>): T? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/$fileName?t=${System.currentTimeMillis()}")
            .header("User-Agent", "SuvMusic-Updater")
            .header("Accept", "application/json")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                json.decodeFromString(serializer, body)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkForUpdate(): UpdateInfo? = fetchJson("update.json", UpdateInfo.serializer())

    suspend fun fetchChangelog(): ChangelogInfo? = fetchJson("changelog.json", ChangelogInfo.serializer())
}

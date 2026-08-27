package com.suvojeet.suvmusic.updater

import com.suvojeet.suvmusic.providers.lyrics.createLyricsHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Shared release metadata client used by Android and Linux hosts. */
class UpdateChecker(
    private val client: HttpClient = createLyricsHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = "https://raw.githubusercontent.com/suvojeet-sengupta/SuvMusic/main/updater"

    private suspend fun <T> fetchJson(fileName: String, serializer: KSerializer<T>): T? = runCatching {
        val response = client.get("$baseUrl/$fileName") {
            header("User-Agent", "SuvMusic-Updater")
            header("Accept", "application/json")
            header("Cache-Control", "no-cache")
        }
        if (!response.status.isSuccess()) return@runCatching null
        json.decodeFromString(serializer, response.bodyAsText())
    }.getOrNull()

    suspend fun checkForUpdate(isNightly: Boolean = false): UpdateInfo? =
        fetchJson(if (isNightly) "nightly.json" else "update.json", UpdateInfo.serializer())

    suspend fun fetchChangelog(): ChangelogInfo? =
        fetchJson("changelog.json", ChangelogInfo.serializer())
}

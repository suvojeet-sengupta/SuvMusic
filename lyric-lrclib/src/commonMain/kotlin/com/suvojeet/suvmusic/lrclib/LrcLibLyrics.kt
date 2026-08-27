package com.suvojeet.suvmusic.lrclib

import com.suvojeet.suvmusic.providers.lyrics.createLyricsHttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs

/** Shared LRCLIB API client used by Android and the JVM desktop target. */
object LrcLibLyrics {
    private const val BASE_URL = "https://lrclib.net"
    private val json = Json { ignoreUnknownKeys = true }
    private val client by lazy { createLyricsHttpClient() }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = runCatching {
        val cleanTitle = title
            .replace(Regex("\\s*\\(.*?\\)"), "")
            .replace(Regex("\\s*\\[.*?\\]"), "")
            .replace(Regex("\\s*-\\s*.*"), "")
            .trim()
        val cleanArtist = artist.split(",", "&", "feat.", "ft.")
            .firstOrNull()
            ?.trim()
            ?: artist

        val exact = client.get("$BASE_URL/api/get") {
            parameter("track_name", cleanTitle)
            parameter("artist_name", cleanArtist)
            parameter("duration", duration)
            if (!album.isNullOrBlank()) parameter("album_name", album)
            header(HttpHeaders.UserAgent, "SuvMusic/1.0 (https://github.com/suvojeet-sengupta/SuvMusic)")
        }
        val exactBody = if (exact.status.value in 200..299) exact.bodyAsText() else null
        selectLyrics(exactBody)?.let { return@runCatching it }

        val search = client.get("$BASE_URL/api/search") {
            parameter("q", "$cleanTitle $cleanArtist")
            header(HttpHeaders.UserAgent, "SuvMusic/1.0")
        }
        val results = if (search.status.value in 200..299) {
            parseArray(search.bodyAsText())
        } else {
            emptyList()
        }

        var bestMatch: JsonObject? = null
        var bestScore = 0.0
        results.forEach { result ->
            val resultTitle = result.string("trackName").orEmpty()
            val resultArtist = result.string("artistName").orEmpty()
            val resultDuration = result.number("duration") ?: 0.0
            val titleScore = calculateSimilarity(cleanTitle, resultTitle)
            val artistScore = calculateSimilarity(cleanArtist, resultArtist)
            if (titleScore < 0.3 || artistScore < 0.3) return@forEach

            var totalScore = titleScore * 0.6 + artistScore * 0.4
            val durationDiff = abs(duration - resultDuration)
            totalScore += when {
                durationDiff < 5 -> 0.2
                durationDiff > 20 -> -0.3
                else -> -((durationDiff - 5) / 15.0 * 0.3)
            }
            if (totalScore > bestScore) {
                bestScore = totalScore
                bestMatch = result
            }
        }

        if (bestScore > 0.65) {
            selectLyrics(bestMatch?.toString())?.let { return@runCatching it }
        }
        error("Lyrics not found")
    }

    private fun selectLyrics(body: String?): String? {
        if (body.isNullOrBlank() || body == "null") return null
        return runCatching {
            val item = json.parseToJsonElement(body).jsonObject
            item.string("syncedLyrics")?.takeIf { it.isNotBlank() }
                ?: item.string("plainLyrics")?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun parseArray(body: String): List<JsonObject> = runCatching {
        json.parseToJsonElement(body).jsonArray.map { it.jsonObject }
    }.getOrDefault(emptyList())

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.number(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull

    private fun calculateSimilarity(first: String, second: String): Double {
        val left = normalize(first)
        val right = normalize(second)
        if (left.isEmpty() && right.isEmpty()) return 1.0
        if (left.isEmpty() || right.isEmpty()) return 0.0
        if (left == right) return 1.0
        val distance = levenshteinDistance(left, right)
        return 1.0 - distance.toDouble() / maxOf(left.length, right.length)
    }

    private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun levenshteinDistance(first: String, second: String): Int {
        val dp = Array(first.length + 1) { IntArray(second.length + 1) }
        for (i in 0..first.length) dp[i][0] = i
        for (j in 0..second.length) dp[0][j] = j
        for (i in 1..first.length) {
            for (j in 1..second.length) {
                val cost = if (first[i - 1] == second[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost,
                )
            }
        }
        return dp[first.length][second.length]
    }
}

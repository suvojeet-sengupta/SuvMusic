package com.suvojeet.suvmusic.providers.lyrics

import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * BetterLyrics — fetches Apple Music TTML lyrics from the shared provider API.
 * The HTTP engine is supplied by each KMP target.
 */
object BetterLyrics {
    private val client by lazy {
        createLyricsHttpClient().config {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            defaultRequest {
                url("https://lyrics-api.boidu.dev")
            }
            expectSuccess = false
        }
    }

    private suspend fun fetchTTML(
        artist: String,
        title: String,
        duration: Int = -1,
        album: String? = null,
    ): String? = runCatching {
        val response = client.get("/getLyrics") {
            parameter("s", title)
            parameter("a", artist)
            if (duration > 0) parameter("d", duration)
            if (!album.isNullOrBlank()) parameter("al", album)
        }
        if (response.status == HttpStatusCode.OK) response.body<TTMLResponse>().ttml else null
    }.getOrNull()

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = runCatching {
        var ttml = fetchTTML(artist, title, duration, album)
        if (ttml == null) {
            val cleanTitle = title
                .replace(Regex("\\s*\\(.*?\\)"), "")
                .replace(Regex("\\s*\\[.*?\\]"), "")
                .replace(Regex("(?i)\\s*official\\s*video.*"), "")
                .replace(Regex("(?i)\\s*lyric\\s*video.*"), "")
                .replace(Regex("(?i)\\s*audio.*"), "")
                .replace(Regex("\\s*-\\s*.*"), "")
                .trim()
            val cleanArtist = artist.split(",", "&", "feat.", "ft.", "Feat.", "Ft.")
                .firstOrNull()
                ?.trim()
                ?: artist
            if (cleanTitle != title || cleanArtist != artist) {
                ttml = fetchTTML(cleanArtist, cleanTitle)
            }
        }

        val source = ttml ?: error("Lyrics unavailable")
        val parsedLines = TTMLParser.parseTTML(source)
        check(parsedLines.isNotEmpty()) { "Failed to parse lyrics" }
        TTMLParser.toLRC(parsedLines)
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration, album).onSuccess(callback)
    }
}

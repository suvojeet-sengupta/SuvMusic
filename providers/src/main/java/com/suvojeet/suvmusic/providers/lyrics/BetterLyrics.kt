package com.suvojeet.suvmusic.providers.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * BetterLyrics - Fetches Apple Music TTML lyrics.
 * API: https://lyrics-api.boidu.dev
 */
object BetterLyrics {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
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
        album: String? = null
    ): String? = runCatching {
        val response = client.get("/getLyrics") {
            parameter("s", title)
            parameter("a", artist)
            if (duration > 0) {
                parameter("d", duration)
            }
            if (!album.isNullOrBlank()) {
                parameter("al", album)
            }
        }
        
        if (response.status == HttpStatusCode.OK) {
            response.body<TTMLResponse>().ttml
        } else {
            null
        }
    }.getOrNull()

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null
    ) = runCatching {
        // 1. Try exact match first
        var ttml = fetchTTML(artist, title, duration, album)
        
        // 2. If failed, try with cleaned title/artist and relaxed constraints
        if (ttml == null) {
            val cleanTitle = title.replace(Regex("\\s*\\(.*?\\)"), "")
                .replace(Regex("\\s*\\[.*?\\]"), "")
                .replace(Regex("(?i)\\s*official\\s*video.*"), "")
                .replace(Regex("(?i)\\s*lyric\\s*video.*"), "")
                .replace(Regex("(?i)\\s*audio.*"), "")
                .replace(Regex("\\s*-\\s*.*"), "") // Remove after dash (e.g. - Remastered)
                .trim()
            
            val cleanArtist = artist.split(",", "&", "feat.", "ft.", "Feat.", "Ft.").firstOrNull()?.trim() ?: artist
            
            if (cleanTitle != title || cleanArtist != artist) {
                // Retry without duration/album to be more lenient
                ttml = fetchTTML(cleanArtist, cleanTitle, -1, null)
            }
        }

        if (ttml == null) {
            throw IllegalStateException("Lyrics unavailable")
        }
        
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse lyrics")
        }
        
        TTMLParser.toLRC(parsedLines)
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit
    ) {
        getLyrics(title, artist, duration, album)
            .onSuccess { lrcString ->
                callback(lrcString)
            }
    }
}

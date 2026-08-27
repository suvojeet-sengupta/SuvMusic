package com.suvojeet.suvmusic.simpmusic

import com.suvojeet.suvmusic.providers.lyrics.createLyricsHttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.math.abs

/** SimpMusic lyrics API client shared by Android and the JVM desktop host. */
object SimpMusicLyrics {
    private const val BASE_URL = "https://api-lyrics.simpmusic.org/v1/"

    @OptIn(ExperimentalSerializationApi::class)
    private val client by lazy {
        createLyricsHttpClient().config {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            defaultRequest {
                url(BASE_URL)
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "SuvMusic/1.0")
                header(HttpHeaders.ContentType, "application/json")
            }
            expectSuccess = false
        }
    }

    suspend fun getLyricsByVideoId(videoId: String): List<LyricsData> = runCatching {
        val response = client.get(BASE_URL + videoId)
        if (response.status == HttpStatusCode.OK) {
            val apiResponse = response.body<SimpMusicApiResponse>()
            if (apiResponse.success) apiResponse.data else emptyList()
        } else {
            emptyList()
        }
    }.getOrDefault(emptyList())

    suspend fun getLyrics(videoId: String, duration: Int = 0): Result<String> = runCatching {
        val tracks = getLyricsByVideoId(videoId)
        check(tracks.isNotEmpty()) { "Lyrics unavailable" }
        val bestMatch = if (duration > 0 && tracks.size > 1) {
            tracks.minByOrNull { abs((it.duration ?: 0) - duration) }
        } else {
            tracks.firstOrNull()
        }
        pickBest(bestMatch) ?: error("Lyrics unavailable")
    }

    /** Prefers enhanced LRC, then synced LRC, then plain lyrics. */
    private fun pickBest(track: LyricsData?): String? {
        if (track == null) return null
        val rich = track.richSyncLyrics?.trim()
        return if (!rich.isNullOrEmpty() && rich.startsWith("[")) {
            rich
        } else {
            track.syncedLyrics ?: track.plainLyrics
        }
    }

    suspend fun getAllLyrics(
        videoId: String,
        duration: Int = 0,
        callback: (String) -> Unit,
    ) {
        val tracks = getLyricsByVideoId(videoId)
        var count = 0
        var plain = 0
        val sortedTracks = if (duration > 0) {
            tracks.sortedBy { abs((it.duration ?: 0) - duration) }
        } else {
            tracks
        }

        sortedTracks.forEach { track ->
            if (count <= 4) {
                val durationOk = abs((track.duration ?: 0) - duration) <= 5
                val rich = track.richSyncLyrics?.trim()
                if (!rich.isNullOrEmpty() && rich.startsWith("[") && durationOk) {
                    count++
                    callback(rich)
                }
                if (track.syncedLyrics != null && durationOk) {
                    count++
                    callback(track.syncedLyrics)
                }
                if (track.plainLyrics != null && durationOk && plain == 0) {
                    count++
                    plain++
                    callback(track.plainLyrics)
                }
            }
        }
    }
}

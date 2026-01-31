package com.suvojeet.suvmusic.lastfm

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject

interface LastFmConfig {
    val apiKey: String
    val sharedSecret: String
    val userAgent: String get() = "SuvMusic (https://github.com/suvojeet-sengupta/SuvMusic)"
}

class LastFmClient @Inject constructor(
    private val config: LastFmConfig
) {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest { 
            url("https://ws.audioscrobbler.com/2.0/")
            userAgent(config.userAgent)
        }
        expectSuccess = false
    }

    private fun Map<String, String>.apiSig(secret: String): String {
        val sorted = toSortedMap()
        val toHash = sorted.entries.joinToString("") { it.key + it.value } + secret
        val digest = MessageDigest.getInstance("MD5").digest(toHash.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun HttpRequestBuilder.lastfmParams(
        method: String,
        extra: Map<String, String> = emptyMap(),
        sessionKey: String? = null
    ) {
        val paramsForSig = mutableMapOf(
            "method" to method,
            "api_key" to config.apiKey
        ).apply {
            sessionKey?.let { put("sk", it) }
            putAll(extra)
        }
        val apiSig = paramsForSig.apiSig(config.sharedSecret)
        setBody(FormDataContent(Parameters.build {
            paramsForSig.forEach { (k, v) -> append(k, v) }
            append("api_sig", apiSig)
            append("format", "json")
        }))
    }

    suspend fun fetchSession(token: String): Result<Authentication> = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.post {
                lastfmParams(
                    method = "auth.getSession",
                    extra = mapOf("token" to token)
                )
            }
            
            val responseText = response.bodyAsText()
            if (responseText.contains("\"error\"")) {
                val error = json.decodeFromString<LastFmError>(responseText)
                throw Exception(error.message)
            }
            
            json.decodeFromString<Authentication>(responseText)
        }
    }

    suspend fun getMobileSession(username: String, password: String): Result<Authentication> = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.post {
                lastfmParams(
                    method = "auth.getMobileSession",
                    extra = mapOf("username" to username, "password" to password)
                )
            }

            val responseText = response.bodyAsText()
            if (responseText.contains("\"error\"")) {
                val error = json.decodeFromString<LastFmError>(responseText)
                throw Exception(error.message)
            }

            json.decodeFromString<Authentication>(responseText)
        }
    }

    suspend fun updateNowPlaying(sessionKey: String, artist: String, track: String, album: String?, duration: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.post {
                lastfmParams(
                    method = "track.updateNowPlaying",
                    sessionKey = sessionKey,
                    extra = buildMap {
                        put("artist", artist)
                        put("track", track)
                        album?.let { put("album", it) }
                        if (duration > 0) put("duration", (duration / 1000).toString())
                    }
                )
            }
            
            val responseText = response.bodyAsText()
            if (responseText.contains("\"error\"")) {
                val error = json.decodeFromString<LastFmError>(responseText)
                throw Exception(error.message)
            }
        }
    }

    suspend fun scrobble(sessionKey: String, artist: String, track: String, album: String?, duration: Long, timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.post {
                lastfmParams(
                    method = "track.scrobble",
                    sessionKey = sessionKey,
                    extra = buildMap {
                        put("artist[0]", artist)
                        put("track[0]", track)
                        put("timestamp[0]", timestamp.toString())
                        album?.let { put("album[0]", it) }
                        if (duration > 0) put("duration[0]", (duration / 1000).toString())
                    }
                )
            }
            
            val responseText = response.bodyAsText()
            if (responseText.contains("\"error\"")) {
                val error = json.decodeFromString<LastFmError>(responseText)
                throw Exception(error.message)
            }
        }
    }

    suspend fun setLoveStatus(sessionKey: String, artist: String, track: String, love: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val method = if (love) "track.love" else "track.unlove"
            val response = client.post {
                lastfmParams(
                    method = method,
                    sessionKey = sessionKey,
                    extra = mapOf(
                        "artist" to artist,
                        "track" to track
                    )
                )
            }
            
            val responseText = response.bodyAsText()
            if (responseText.contains("\"error\"")) {
                val error = json.decodeFromString<LastFmError>(responseText)
                throw Exception(error.message)
            }
        }
    }

    suspend fun getRecommendedArtists(sessionKey: String, limit: Int = 20, page: Int = 1): Result<RecommendedArtistsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.post {
                lastfmParams(
                    method = "user.getRecommendedArtists",
                    sessionKey = sessionKey,
                    extra = mapOf(
                        "limit" to limit.toString(),
                        "page" to page.toString()
                    )
                )
            }

            val responseText = response.bodyAsText()
            if (responseText.contains("\"error\"")) {
                val error = json.decodeFromString<LastFmError>(responseText)
                throw Exception(error.message)
            }

            json.decodeFromString<RecommendedArtistsResponse>(responseText)
        }
    }

    suspend fun getRecommendedTracks(sessionKey: String, limit: Int = 20, page: Int = 1): Result<RecommendedTracksResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.post {
                lastfmParams(
                    method = "user.getRecommendedTracks",
                    sessionKey = sessionKey,
                    extra = mapOf(
                        "limit" to limit.toString(),
                        "page" to page.toString()
                    )
                )
            }

            val responseText = response.bodyAsText()
            if (responseText.contains("\"error\"")) {
                val error = json.decodeFromString<LastFmError>(responseText)
                throw Exception(error.message)
            }

            json.decodeFromString<RecommendedTracksResponse>(responseText)
        }
    }
    
    fun getAuthUrl(): String {
        val callback = java.net.URLEncoder.encode("suvmusic://lastfm-auth", "UTF-8")
        return "https://www.last.fm/api/auth/?api_key=${config.apiKey}&cb=$callback"
    }
}

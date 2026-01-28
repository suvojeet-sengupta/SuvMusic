package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.BuildConfig
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.lastfm.Authentication
import com.suvojeet.suvmusic.data.model.lastfm.LastFmError
import com.suvojeet.suvmusic.data.model.lastfm.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import javax.inject.Singleton

@Singleton
class LastFmRepository @Inject constructor(
    private val sessionManager: SessionManager
) {

    private val apiKey = BuildConfig.LAST_FM_API_KEY
    private val sharedSecret = BuildConfig.LAST_FM_SHARED_SECRET

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
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
        url("https://ws.audioscrobbler.com/2.0/")
        val paramsForSig = mutableMapOf(
            "method" to method,
            "api_key" to apiKey
        ).apply {
            sessionKey?.let { put("sk", it) }
            putAll(extra)
        }
        val apiSig = paramsForSig.apiSig(sharedSecret)
        setBody(FormDataContent(Parameters.build {
            paramsForSig.forEach { (k, v) -> append(k, v) }
            append("api_sig", apiSig)
            append("format", "json")
        }))
    }

    suspend fun fetchSession(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = client.post {
                lastfmParams(
                    method = "auth.getSession",
                    extra = mapOf("token" to token)
                )
            }
            
            val responseText = response.bodyAsText()
            if (responseText.contains("\"error\"")) {
                val error = json.decodeFromString<LastFmError>(responseText)
                return@withContext Result.failure(Exception(error.message))
            }
            
            val auth = json.decodeFromString<Authentication>(responseText)
            val sessionKey = auth.session.key
            val username = auth.session.name
            
            // Persist session
            sessionManager.setLastFmSession(sessionKey, username)
            
            Result.success(username)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNowPlaying(artist: String, track: String, album: String?, duration: Long) = withContext(Dispatchers.IO) {
        val sessionKey = sessionManager.getLastFmSessionKey() ?: return@withContext
        try {
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
                android.util.Log.e("LastFmRepository", "updateNowPlaying Failed: ${error.message}")
            } else {
                android.util.Log.d("LastFmRepository", "updateNowPlaying Success")
            }
        } catch (e: Exception) {
            android.util.Log.e("LastFmRepository", "updateNowPlaying Exception", e)
        }
    }

    suspend fun scrobble(artist: String, track: String, album: String?, duration: Long, timestamp: Long) = withContext(Dispatchers.IO) {
        val sessionKey = sessionManager.getLastFmSessionKey() ?: return@withContext
        try {
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
                android.util.Log.e("LastFmRepository", "Scrobble Failed: ${error.message}")
            } else {
                android.util.Log.d("LastFmRepository", "Scrobble Success")
            }
        } catch (e: Exception) {
            android.util.Log.e("LastFmRepository", "Scrobble Exception", e)
        }
    }

    suspend fun setLoveStatus(artist: String, track: String, love: Boolean) = withContext(Dispatchers.IO) {
        val sessionKey = sessionManager.getLastFmSessionKey() ?: return@withContext
        try {
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
                android.util.Log.e("LastFmRepository", "setLoveStatus Failed: ${error.message}")
            } else {
                android.util.Log.d("LastFmRepository", "setLoveStatus Success")
            }
        } catch (e: Exception) {
            android.util.Log.e("LastFmRepository", "setLoveStatus Exception", e)
        }
    }
    
    fun getAuthUrl(): String {
        val callback = java.net.URLEncoder.encode("suvmusic://lastfm-auth", "UTF-8")
        return "https://www.last.fm/api/auth/?api_key=$apiKey&cb=$callback"
    }
    
    fun logout() {
        sessionManager.clearLastFmSession()
    }
    
    fun isConnected(): Boolean {
        return !sessionManager.getLastFmSessionKey().isNullOrEmpty()
    }
    
    fun getUsername(): String? {
        return sessionManager.getLastFmUsername()
    }
}

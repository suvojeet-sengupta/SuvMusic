package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.BuildConfig
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.network.LastFmService
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LastFmRepository @Inject constructor(
    private val lastFmService: LastFmService,
    private val sessionManager: SessionManager
) {

    private val apiKey = BuildConfig.LAST_FM_API_KEY
    private val sharedSecret = BuildConfig.LAST_FM_SHARED_SECRET

    suspend fun fetchSession(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val params = sortedMapOf(
                "method" to "auth.getSession",
                "token" to token,
                "api_key" to apiKey
            )
            val signature = generateSignature(params)
            
            val response = lastFmService.getSession(
                token = token,
                apiKey = apiKey,
                apiSig = signature
            )
            
            val sessionKey = response.session.key
            val username = response.session.name
            
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
            val params = sortedMapOf(
                "method" to "track.updateNowPlaying",
                "artist" to artist,
                "track" to track,
                "api_key" to apiKey,
                "sk" to sessionKey
            )
            
            if (!album.isNullOrEmpty()) params["album"] = album
            if (duration > 0) params["duration"] = (duration / 1000).toString()

            val signature = generateSignature(params)

            lastFmService.updateNowPlaying(
                artist = artist,
                track = track,
                album = album,
                duration = if (duration > 0) (duration / 1000).toInt() else null,
                apiKey = apiKey,
                apiSig = signature,
                sessionKey = sessionKey
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun scrobble(artist: String, track: String, album: String?, duration: Long, timestamp: Long) = withContext(Dispatchers.IO) {
        val sessionKey = sessionManager.getLastFmSessionKey() ?: return@withContext
        try {
            val params = sortedMapOf(
                "method" to "track.scrobble",
                "artist" to artist,
                "track" to track,
                "timestamp" to timestamp.toString(),
                "api_key" to apiKey,
                "sk" to sessionKey
            )
            
            if (!album.isNullOrEmpty()) params["album"] = album
            if (duration > 0) params["duration"] = (duration / 1000).toString()

            val signature = generateSignature(params)

            lastFmService.scrobble(
                artist = artist,
                track = track,
                album = album,
                timestamp = timestamp,
                duration = if (duration > 0) (duration / 1000).toInt() else null,
                apiKey = apiKey,
                apiSig = signature,
                sessionKey = sessionKey
            )
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun generateSignature(params: Map<String, String>): String {
        val sortedParams = params.toSortedMap()
        val sb = StringBuilder()
        for ((key, value) in sortedParams) {
            sb.append(key).append(value)
        }
        sb.append(sharedSecret)
        return md5(sb.toString())
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

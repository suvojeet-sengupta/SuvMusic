package com.suvojeet.suvmusic.data.repository.youtube.internal

import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.YouTubeAuthUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles all YouTube Music API communication.
 * Manages authentication, request building, and API calls.
 */
@Singleton
class YouTubeApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager
) {

    /**
     * Fetch authenticated YouTube Music internal API.
     * @param endpoint Either a browseId (e.g., "FEmusic_home") or endpoint path (e.g., "account/account_menu")
     * @param hl Host language (e.g., "en", "hi")
     * @param gl Geolocation (e.g., "US", "IN")
     */
    suspend fun fetchInternalApi(endpoint: String, hl: String = "en", gl: String = "US"): String {
        val cookies = sessionManager.getCookies() ?: return ""
        val isBrowse = !endpoint.contains("/")
        
        val url = if (isBrowse) {
            "https://music.youtube.com/youtubei/v1/browse"
        } else {
            "https://music.youtube.com/youtubei/v1/$endpoint"
        }
        
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
        
        val contextJson = """
            "context": {
                "client": {
                    "clientName": "WEB_REMIX",
                    "clientVersion": "1.20230102.01.00",
                    "hl": "$hl",
                    "gl": "$gl"
                }
            }
        """.trimIndent()

        val jsonBody = if (isBrowse) {
            "{ $contextJson, \"browseId\": \"$endpoint\" }"
        } else {
            "{ $contextJson }"
        }

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Cookie", cookies)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("X-Goog-AuthUser", sessionManager.getAuthUserIndex().toString())
            .build()

        return try {
            okHttpClient.newCall(request).execute().body.string()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Fetch continuation data for paginated results.
     */
    suspend fun fetchInternalApiWithContinuation(continuationToken: String, hl: String = "en", gl: String = "US"): String {
        val cookies = sessionManager.getCookies() ?: return ""
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20230102.01.00",
                        "hl": "$hl",
                        "gl": "$gl"
                    }
                }
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/browse?ctoken=$continuationToken&continuation=$continuationToken")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Cookie", cookies)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("X-Goog-AuthUser", sessionManager.getAuthUserIndex().toString())
            .build()

        return try {
            okHttpClient.newCall(request).execute().body.string()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Fetch with browse parameters (for category browsing).
     */
    suspend fun fetchInternalApiWithParams(browseId: String, params: String, hl: String = "en", gl: String = "US"): String {
        val cookies = sessionManager.getCookies() ?: return ""
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20230102.01.00",
                        "hl": "$hl",
                        "gl": "$gl"
                    }
                },
                "browseId": "$browseId",
                "params": "$params"
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/browse")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Cookie", cookies)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("X-Goog-AuthUser", sessionManager.getAuthUserIndex().toString())
            .build()

        return try {
            okHttpClient.newCall(request).execute().body.string()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Fetch public YouTube Music API without authentication.
     * Used for charts, trending, and public browse content.
     */
    suspend fun fetchPublicApi(browseId: String, hl: String = "en", gl: String = "IN"): String {
        val url = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20240101.01.00",
                        "hl": "$hl",
                        "gl": "$gl"
                    }
                },
                "browseId": "$browseId"
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("Referer", "https://music.youtube.com/")
            .build()

        return try {
            okHttpClient.newCall(request).execute().body.string()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Perform an authenticated action (like, create playlist, etc.).
     * @param endpoint API endpoint path (e.g., "like/like", "playlist/create")
     * @param innerBody JSON body content (without context wrapper)
     */
    suspend fun performAuthenticatedAction(endpoint: String, innerBody: String): Boolean {
        if (!sessionManager.isLoggedIn()) return false
        val cookies = sessionManager.getCookies() ?: return false
        
        val url = "https://music.youtube.com/youtubei/v1/$endpoint"
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: return false

        val cleanedBody = innerBody.trim()
        val processedBody = if (cleanedBody.startsWith("{") && cleanedBody.endsWith("}")) {
            cleanedBody.substring(1, cleanedBody.length - 1)
        } else {
            cleanedBody
        }

        val fullBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20230102.01.00",
                        "hl": "en",
                        "gl": "US"
                    }
                },
                $processedBody
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(fullBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Cookie", cookies)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("X-Goog-AuthUser", sessionManager.getAuthUserIndex().toString())
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body.string()
                android.util.Log.e("YouTubeApiClient", "Action failed: $endpoint. Code: ${response.code}, Body: $errorBody")
                return false
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("YouTubeApiClient", "Action error: $endpoint", e)
            false
        }
    }

    /**
     * Generate a Client Playback Nonce (CPN) - a 16-character alphanumeric string
     * used by YouTube to identify unique playback sessions.
     */
    private fun generateCPN(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        return (1..16).map { chars.random() }.joinToString("")
    }

    /**
     * Report a song playback to YouTube Music history.
     * 
     * This works by:
     * 1. Calling the /player endpoint to get playbackTracking URLs
     * 2. Hitting the videostatsPlaybackUrl (playback start signal)
     * 3. Hitting the videostatsWatchtimeUrl (watch time signal - this is what actually records history)
     *
     * @param videoId The YouTube video ID to report
     * @param durationSeconds Approximate duration of the song in seconds (used in watchtime reporting)
     * @return true if history was successfully reported
     */
    suspend fun reportPlaybackForHistory(videoId: String, durationSeconds: Int = 30): Boolean {
        if (!sessionManager.isLoggedIn()) return false
        val cookies = sessionManager.getCookies() ?: return false
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: return false

        try {
            // Step 1: Call /player to get playbackTracking URLs
            val playerBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("videoId", videoId)
                put("playbackContext", JSONObject().apply {
                    put("contentPlaybackContext", JSONObject().apply {
                        put("signatureTimestamp", System.currentTimeMillis() / 1000)
                    })
                })
            }

            val playerRequest = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/player")
                .post(playerBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Cookie", cookies)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("Referer", "https://music.youtube.com/")
                .addHeader("X-Goog-AuthUser", sessionManager.getAuthUserIndex().toString())
                .build()

            val playerResponse = okHttpClient.newCall(playerRequest).execute()
            val playerResponseBody = playerResponse.body.string()
            
            if (!playerResponse.isSuccessful || playerResponseBody.isNullOrBlank()) {
                android.util.Log.e("YouTubeApiClient", "Player request failed: ${playerResponse.code}")
                return false
            }

            val playerJson = JSONObject(playerResponseBody)
            val playbackTracking = playerJson.optJSONObject("playbackTracking")
            
            if (playbackTracking == null) {
                android.util.Log.e("YouTubeApiClient", "No playbackTracking in player response for $videoId")
                return false
            }

            // Extract tracking URLs
            val playbackUrl = playbackTracking
                .optJSONObject("videostatsPlaybackUrl")
                ?.optString("baseUrl")
            val watchtimeUrl = playbackTracking
                .optJSONObject("videostatsWatchtimeUrl")
                ?.optString("baseUrl")

            if (playbackUrl.isNullOrBlank() || watchtimeUrl.isNullOrBlank()) {
                android.util.Log.e("YouTubeApiClient", "Missing tracking URLs for $videoId")
                return false
            }

            val cpn = generateCPN()

            // Step 2: Hit videostatsPlaybackUrl (signals playback start)
            val playbackTrackingUrl = "$playbackUrl&ver=2&cpn=$cpn&el=detailpage&st=0&et=$durationSeconds"
            val playbackTrackRequest = okhttp3.Request.Builder()
                .url(playbackTrackingUrl)
                .get()
                .addHeader("Cookie", cookies)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("Referer", "https://music.youtube.com/")
                .build()

            val playbackTrackResponse = okHttpClient.newCall(playbackTrackRequest).execute()
            android.util.Log.d("YouTubeApiClient", "Playback tracking response: ${playbackTrackResponse.code}")
            playbackTrackResponse.body.close()

            // Step 3: Hit videostatsWatchtimeUrl (signals watch time - THIS records history)
            val watchtimeTrackingUrl = "$watchtimeUrl&ver=2&cpn=$cpn&el=detailpage&st=0&et=$durationSeconds&len=$durationSeconds&cmt=$durationSeconds"
            val watchtimeTrackRequest = okhttp3.Request.Builder()
                .url(watchtimeTrackingUrl)
                .get()
                .addHeader("Cookie", cookies)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("Referer", "https://music.youtube.com/")
                .build()

            val watchtimeTrackResponse = okHttpClient.newCall(watchtimeTrackRequest).execute()
            android.util.Log.d("YouTubeApiClient", "Watchtime tracking response: ${watchtimeTrackResponse.code}")
            watchtimeTrackResponse.body.close()

            return playbackTrackResponse.isSuccessful && watchtimeTrackResponse.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("YouTubeApiClient", "Error reporting playback history for $videoId", e)
            return false
        }
    }
}

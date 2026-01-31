package com.suvojeet.suvmusic.data.repository.youtube.internal

import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.YouTubeAuthUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
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
     */
    fun fetchInternalApi(endpoint: String): String {
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
                    "hl": "en",
                    "gl": "US"
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
            .addHeader("X-Goog-AuthUser", "0")
            .build()

        return try {
            okHttpClient.newCall(request).execute().body?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Fetch continuation data for paginated results.
     */
    fun fetchInternalApiWithContinuation(continuationToken: String): String {
        val cookies = sessionManager.getCookies() ?: return ""
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20230102.01.00",
                        "hl": "en",
                        "gl": "US"
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
            .addHeader("X-Goog-AuthUser", "0")
            .build()

        return try {
            okHttpClient.newCall(request).execute().body?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Fetch with browse parameters (for category browsing).
     */
    fun fetchInternalApiWithParams(browseId: String, params: String): String {
        val cookies = sessionManager.getCookies() ?: return ""
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20230102.01.00",
                        "hl": "en",
                        "gl": "US"
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
            .addHeader("X-Goog-AuthUser", "0")
            .build()

        return try {
            okHttpClient.newCall(request).execute().body?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Fetch public YouTube Music API without authentication.
     * Used for charts, trending, and public browse content.
     */
    fun fetchPublicApi(browseId: String): String {
        val url = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20240101.01.00",
                        "hl": "en",
                        "gl": "IN"
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
            okHttpClient.newCall(request).execute().body?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Perform an authenticated action (like, create playlist, etc.).
     * @param endpoint API endpoint path (e.g., "like/like", "playlist/create")
     * @param innerBody JSON body content (without context wrapper)
     */
    fun performAuthenticatedAction(endpoint: String, innerBody: String): Boolean {
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
            .addHeader("X-Goog-AuthUser", "0")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                android.util.Log.e("YouTubeApiClient", "Action failed: $endpoint. Code: ${response.code}, Body: $errorBody")
                return false
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("YouTubeApiClient", "Action error: $endpoint", e)
            false
        }
    }
}

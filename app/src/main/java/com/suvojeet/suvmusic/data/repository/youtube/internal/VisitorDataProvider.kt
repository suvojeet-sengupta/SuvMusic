package com.suvojeet.suvmusic.data.repository.youtube.internal

import com.suvojeet.suvmusic.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides YouTube's `visitorData` token, fetched once from
 * `music.youtube.com/sw.js_data` and persisted via [SessionManager].
 *
 * visitorData stabilizes InnerTube requests (sent as the `X-Goog-Visitor-Id`
 * header and `context.client.visitorData`) and is used as the session id the
 * streaming PoToken is bound to. Fetch failures are non-fatal — callers simply
 * proceed without it.
 */
@Singleton
class VisitorDataProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
) {
    /** Cached value if available, otherwise fetch + persist. Null on failure. */
    suspend fun get(): String? {
        sessionManager.getVisitorData()?.takeIf { it.isNotBlank() }?.let { return it }
        val fetched = fetch() ?: return null
        sessionManager.saveVisitorData(fetched)
        return fetched
    }

    private suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://music.youtube.com/sw.js_data")
                .addHeader("User-Agent", YouTubeConfig.USER_AGENT)
                .addHeader("Origin", YouTubeConfig.ORIGIN)
                .addHeader("Referer", "https://music.youtube.com/")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                parseVisitorData(body)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * `sw.js_data` is a JSON document prefixed with the `)]}'` XSSI guard. The
     * visitorData token is a string beginning with `Cgt`/`Cgs` buried in nested
     * arrays, so we strip the guard and recursively scan for the first match.
     */
    private fun parseVisitorData(raw: String): String? {
        return try {
            val trimmed = if (raw.length > 5) raw.substring(5) else raw
            val root = Json.parseToJsonElement(trimmed)
            val regex = Regex("^Cg[ts]")
            var found: String? = null
            fun scan(element: JsonElement) {
                if (found != null) return
                when (element) {
                    is JsonArray -> element.forEach { scan(it) }
                    is JsonObject -> element.values.forEach { scan(it) }
                    is JsonPrimitive ->
                        if (element.isString && regex.containsMatchIn(element.content)) {
                            found = element.content
                        }
                    else -> {}
                }
            }
            scan(root)
            found
        } catch (e: Exception) {
            null
        }
    }
}

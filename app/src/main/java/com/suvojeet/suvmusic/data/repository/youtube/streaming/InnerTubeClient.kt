package com.suvojeet.suvmusic.data.repository.youtube.streaming

import com.google.gson.JsonParser
import com.suvojeet.suvmusic.core.model.AudioQuality
import com.suvojeet.suvmusic.data.repository.youtube.internal.VisitorDataProvider
import com.suvojeet.suvmusic.data.repository.youtube.streaming.potoken.PoTokenGenerator
import com.suvojeet.suvmusic.data.repository.youtube.streaming.potoken.PoTokenResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback audio resolver that talks to YouTube's private InnerTube
 * `/youtubei/v1/player` endpoint directly.
 *
 * Purpose: the NewPipe extractor is the primary path, but YouTube periodically
 * changes its web player which breaks NewPipe until it ships an update. This
 * client is the next link in the "one fails, the next tries" chain: it asks the
 * player API with several client identities (IOS → ANDROID_VR → TV) in turn.
 *
 * Hard constraint: we only accept formats that come back with a direct `url`.
 * The IOS / ANDROID_VR clients return un-ciphered URLs; formats carrying only a
 * `signatureCipher` are skipped, because descrambling the signature requires
 * executing YouTube's JS player — too fragile to maintain in-app.
 *
 * Caveats (inherent to this approach, not bugs):
 *  - May return UNPLAYABLE on datacenter IPs (works on normal mobile/Wi-Fi IPs).
 *  - Some clients/regions now require a PoToken; this is best-effort.
 *
 * PoToken: a WEB_REMIX client carrying a WebView-minted PoToken is appended as
 * the LAST attempt. The earlier clients (IOS/ANDROID_VR/TV) return un-ciphered
 * URLs without a PoToken and remain the fast path; the WEB attempt only pays the
 * PoToken cold-start when everything else has already failed, and falls through
 * (fail-safe) if generation fails or the formats are ciphered-only. visitorData
 * is attached only to the WEB client (for the PoToken binding); the other
 * clients are left byte-identical to their proven, working form.
 */
@Singleton
class InnerTubeClient @Inject constructor(
    okHttpClient: OkHttpClient,
    private val poTokenGenerator: PoTokenGenerator,
    private val visitorDataProvider: VisitorDataProvider
) {
    // Per-call timeout so a single slow client identity can't hang the whole
    // resolve. Clients are tried sequentially (IOS → ANDROID_VR → TV); without a
    // cap, three stalled calls at the default ~30s each could block playback ~90s.
    private val httpClient: OkHttpClient = okHttpClient.newBuilder()
        .callTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private data class ClientProfile(
        val clientName: String,
        val clientVersion: String,
        val userAgent: String,
        /** Extra JSON fields injected inside the "client" object (must end with a comma). */
        val extraContext: String = "",
        /**
         * When true, a WebView-minted PoToken is attached to this client's
         * request (`serviceIntegrityDimensions.poToken` + `pot=` on the stream
         * URL). Only the WEB family accepts the web PoToken; the others use a
         * different attestation and must NOT carry it.
         */
        val useWebPoTokens: Boolean = false
    )

    // Order matters: try the clients most likely to return direct (un-ciphered)
    // URLs without a PoToken first. The WEB_REMIX + PoToken client is LAST so its
    // cold-start cost is only paid as a last resort.
    private val clients = listOf(
        ClientProfile(
            clientName = "IOS",
            clientVersion = "20.10.4",
            userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            extraContext = "\"deviceMake\":\"Apple\",\"deviceModel\":\"iPhone16,2\",\"osName\":\"iPhone\",\"osVersion\":\"18.3.2.22D82\","
        ),
        ClientProfile(
            clientName = "ANDROID_VR",
            clientVersion = "1.62.27",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.62.27 (Linux; U; Android 12; GB) gzip",
            extraContext = "\"deviceMake\":\"Oculus\",\"deviceModel\":\"Quest 3\",\"androidSdkVersion\":32,\"osName\":\"Android\",\"osVersion\":\"12\","
        ),
        ClientProfile(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 (KHTML, like Gecko)"
        ),
        ClientProfile(
            clientName = "WEB_REMIX",
            clientVersion = "1.20240101.01.00",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            useWebPoTokens = true
        )
    )

    private val jsonMedia = "application/json".toMediaType()

    /**
     * Resolve a direct, playable audio URL for [videoId], or null if every
     * client identity fails. Honors the user's [audioQuality] preference when
     * choosing among the available audio bitrates.
     */
    suspend fun resolveAudioUrl(videoId: String, audioQuality: AudioQuality): String? {
        // Fetched once; only attached to the WEB client (which needs it for the
        // PoToken binding). The IOS/ANDROID_VR/TV requests are left byte-identical
        // to before so the proven fast path can't regress.
        val visitorData = try { visitorDataProvider.get() } catch (e: Exception) { null }
        for (client in clients) {
            try {
                // The WEB client needs a PoToken to be accepted; mint it lazily,
                // bound to the videoId (player) + visitorData (streaming session).
                // Any failure leaves poToken null and the request still goes out.
                val poToken = if (client.useWebPoTokens && !visitorData.isNullOrBlank()) {
                    try {
                        // getWebClientPoToken uses runBlocking internally, so it must
                        // never run on the main thread (it would deadlock against the
                        // WebView's withContext(Main)).
                        withContext(Dispatchers.IO) {
                            poTokenGenerator.getWebClientPoToken(videoId, visitorData)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("InnerTube", "poToken gen failed for $videoId: ${e.message}")
                        null
                    }
                } else null

                val clientVisitorData = if (client.useWebPoTokens) visitorData else null
                val url = tryClient(videoId, client, audioQuality, clientVisitorData, poToken)
                if (!url.isNullOrBlank()) {
                    android.util.Log.i("InnerTube", "resolved $videoId via ${client.clientName}")
                    return url
                }
            } catch (e: Exception) {
                android.util.Log.w("InnerTube", "client ${client.clientName} failed for $videoId: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        android.util.Log.w("InnerTube", "all clients failed for $videoId")
        return null
    }

    private fun tryClient(
        videoId: String,
        client: ClientProfile,
        audioQuality: AudioQuality,
        visitorData: String?,
        poToken: PoTokenResult?
    ): String? {
        val visitorDataField = if (!visitorData.isNullOrBlank()) "\"visitorData\":\"$visitorData\"," else ""
        val serviceIntegrity = if (poToken != null) {
            ",\"serviceIntegrityDimensions\":{\"poToken\":\"${poToken.playerRequestPoToken}\"}"
        } else ""
        val payload =
            "{\"context\":{\"client\":{\"clientName\":\"${client.clientName}\",\"clientVersion\":\"${client.clientVersion}\"," +
                "${client.extraContext}${visitorDataField}\"hl\":\"en\",\"gl\":\"US\",\"utcOffsetMinutes\":0}}," +
                "\"videoId\":\"$videoId\"," +
                "\"playbackContext\":{\"contentPlaybackContext\":{\"html5Preference\":\"HTML5_PREF_WANTS\"}}," +
                "\"contentCheckOk\":true,\"racyCheckOk\":true${serviceIntegrity}}"

        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            .addHeader("User-Agent", client.userAgent)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Format-Version", "2")
            .apply { if (!visitorData.isNullOrBlank()) addHeader("X-Goog-Visitor-Id", visitorData) }
            .post(payload.toRequestBody(jsonMedia))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                android.util.Log.w("InnerTube", "${client.clientName} HTTP ${response.code} for $videoId")
                return null
            }
            val body = response.body.string()
            if (body.isBlank()) return null
            val root = JsonParser.parseString(body).asJsonObject

            val status = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            if (status != "OK") {
                android.util.Log.w("InnerTube", "${client.clientName} playabilityStatus=$status for $videoId")
                return null
            }

            val streamingData = root.getAsJsonObject("streamingData") ?: return null
            val formats = streamingData.getAsJsonArray("adaptiveFormats") ?: return null

            // Keep only audio-only formats that expose a direct URL (skip ciphered).
            var bestUrl: String? = null
            var bestBitrate = -1
            var fallbackUrl: String? = null
            var fallbackBitrate = -1

            // InnerTube bitrate is in bits-per-second.
            val targetBitrate = when (audioQuality) {
                AudioQuality.LOW -> 70_000
                AudioQuality.MEDIUM -> 160_000
                AudioQuality.HIGH -> 512_000
                AudioQuality.AUTO -> 160_000
            }

            for (el in formats) {
                val f = el.asJsonObject
                val mime = f.get("mimeType")?.asString ?: continue
                if (!mime.startsWith("audio")) continue
                val streamUrl = f.get("url")?.asString ?: continue // ciphered-only → skip
                val bitrate = f.get("bitrate")?.asInt ?: 0

                // Track the overall highest as a fallback if nothing fits the target.
                if (bitrate > fallbackBitrate) {
                    fallbackBitrate = bitrate
                    fallbackUrl = streamUrl
                }
                // Prefer the highest bitrate at or below the target quality.
                if (bitrate <= targetBitrate && bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = streamUrl
                }
            }

            val resolvedUrl = bestUrl ?: fallbackUrl
            // The streaming PoToken must ride along on the stream URL as `pot=` for
            // the WEB client, or the CDN rejects the request.
            if (resolvedUrl != null && client.useWebPoTokens && poToken != null) {
                val separator = if (resolvedUrl.contains("?")) "&" else "?"
                return "$resolvedUrl${separator}pot=${URLEncoder.encode(poToken.streamingDataPoToken, "UTF-8")}"
            }
            return resolvedUrl
        }
    }
}

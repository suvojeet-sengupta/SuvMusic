package com.suvojeet.suvmusic.data.repository.youtube.streaming

import android.util.LruCache
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.AppError
import com.suvojeet.suvmusic.data.error.toAppError
import com.suvojeet.suvmusic.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles YouTube stream URL fetching and caching.
 * Manages audio/video streams for playback and downloads.
 */
@Suppress("DEPRECATION")
@Singleton
class YouTubeStreamingService @Inject constructor(
    private val sessionManager: SessionManager,
    private val networkMonitor: com.suvojeet.suvmusic.util.NetworkMonitor,
    private val innerTubeClient: InnerTubeClient
) {
    private val ytService: org.schabi.newpipe.extractor.StreamingService by lazy {
        ServiceList.all().find { it.serviceInfo.name == "YouTube" }
            ?: throw IllegalStateException("YouTube service not found")
    }

    // Cache for stream URLs to avoid re-fetching (max 50 entries).
    // Backstop TTL only — the real gate is the URL's own `expire` param (see
    // cachedUrlStillValid). googlevideo links are signed and die well before any
    // fixed TTL, so a long TTL was serving dead URLs that failed with 403 mid-song.
    private data class CachedStream(val url: String, val extension: String, val timestamp: Long)
    private val streamCache = LruCache<String, CachedStream>(50)
    private val CACHE_EXPIRY_MS = 60 * 60 * 1000L // 1 hour backstop

    /**
     * A cached stream URL is valid only if it's within the backstop TTL AND the
     * signed URL hasn't reached (or is about to reach) its own expiry. googlevideo
     * URLs carry `expire=<epochSeconds>`; we refresh 5 minutes early so playback
     * never starts on a URL that dies seconds later with a 403.
     */
    private fun cachedUrlStillValid(cached: CachedStream): Boolean {
        val now = System.currentTimeMillis()
        if (now - cached.timestamp >= CACHE_EXPIRY_MS) return false
        val expireSeconds = Regex("[?&]expire=(\\d+)").find(cached.url)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (expireSeconds == null) {
            // No parseable expire param. Either YouTube changed the URL format or this
            // isn't a googlevideo URL — log it so silent serving of dead URLs is visible.
            android.util.Log.d("YouTubeStreaming", "cached URL has no parseable expire= param; relying on backstop TTL only")
        } else if (now >= expireSeconds * 1000L - 5 * 60_000L) {
            return false
        }
        return true
    }

    // In-flight request dedup: if two callers ask for the same videoId concurrently,
    // they share one network fetch instead of racing duplicate requests.
    // Why: prior logs showed two CACHE_MISS for the same id within 1.5s, both burning a
    // full NewPipe round-trip. Map is cleaned up in finally so failed fetches don't get stuck.
    private val inFlightAudio = ConcurrentHashMap<String, Deferred<String?>>()
    private val inFlightVideo = ConcurrentHashMap<String, Deferred<VideoStreamResult?>>()
    private val dedupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Helper to retry operations with exponential backoff.
     */
    private suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 500, // 0.5 sec
        maxDelay: Long = 2000,    // 2 sec
        factor: Double = 2.0,
        opTag: String = "op",
        block: suspend () -> T?
    ): T? {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                val result = block()
                if (result != null) return result
                android.util.Log.w(
                    "YouTubeStreaming",
                    "$opTag attempt ${attempt + 1}/$times returned null (no exception); retrying in ${currentDelay}ms",
                )
            } catch (e: Exception) {
                android.util.Log.w(
                    "YouTubeStreaming",
                    "$opTag attempt ${attempt + 1}/$times threw ${e.javaClass.simpleName}: ${e.message}; retrying in ${currentDelay}ms",
                    e,
                )
                if (e is org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException) {
                    android.util.Log.e(
                        "YouTubeStreaming",
                        "$opTag aborted — ContentNotAvailableException is permanent (no retry)",
                    )
                    return null
                }
            }
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return try {
            val finalResult = block()
            if (finalResult == null) {
                android.util.Log.e("YouTubeStreaming", "$opTag exhausted all $times attempts — returning null")
            }
            finalResult
        } catch (e: Exception) {
            android.util.Log.e(
                "YouTubeStreaming",
                "$opTag final attempt threw ${e.javaClass.simpleName}: ${e.message}",
                e,
            )
            null
        }
    }

    /**
     * Get audio stream URL for playback.
     * Uses user's audio quality preference and caches the result.
     */
    suspend fun getStreamUrl(videoId: String, forceLow: Boolean = false): String? = withContext(Dispatchers.IO) {
        android.util.Log.i("YouTubeStreaming", ">> getStreamUrl ENTER vid=$videoId forceLow=$forceLow")
        val cacheKey = "audio_$videoId"
        if (!forceLow) {
            streamCache.get(cacheKey)?.let { cached ->
                val ageMs = System.currentTimeMillis() - cached.timestamp
                if (cachedUrlStillValid(cached)) {
                    android.util.Log.i("YouTubeStreaming", "CACHE_HIT audio $videoId (age=${ageMs}ms)")
                    return@withContext cached.url
                }
                android.util.Log.i("YouTubeStreaming", "CACHE_STALE audio $videoId (age=${ageMs}ms, expired/expiring) — evicting")
                streamCache.remove(cacheKey)
            } ?: android.util.Log.i("YouTubeStreaming", "CACHE_MISS audio $videoId")
        } else {
            android.util.Log.i("YouTubeStreaming", "CACHE_BYPASS audio $videoId (forceLow=true)")
            streamCache.remove(cacheKey)
        }

        // In-flight dedup: piggyback on an existing fetch for the same videoId.
        // Key is videoId+forceLow so a high-quality fetch doesn't deliver a low-quality URL.
        // computeIfAbsent is atomic — closes the check-then-put race window.
        val inflightKey = "$videoId|$forceLow"
        var piggybacked = true
        val deferred = inFlightAudio.computeIfAbsent(inflightKey) {
            piggybacked = false
            dedupScope.async {
                try {
                    android.util.Log.i("YouTubeStreaming", "RESOLVE audio $videoId primary=www.youtube.com forceLow=$forceLow")
                    val primaryResult = resolveStreamWithUrl("https://www.youtube.com/watch?v=$videoId", videoId, forceLow)
                    if (primaryResult != null) {
                        android.util.Log.i("YouTubeStreaming", "RESOLVE audio $videoId primary OK")
                        return@async primaryResult
                    }
                    android.util.Log.w("YouTubeStreaming", "RESOLVE audio $videoId primary returned null; falling back to music.youtube.com")
                    val fallback = resolveStreamWithUrl("https://music.youtube.com/watch?v=$videoId", videoId, forceLow)
                    if (fallback != null) {
                        android.util.Log.i("YouTubeStreaming", "RESOLVE audio $videoId music fallback succeeded")
                        return@async fallback
                    }

                    // Final fallback: NewPipe failed to extract on both hosts (YouTube
                    // likely changed its web player). Try the InnerTube player API directly.
                    android.util.Log.w("YouTubeStreaming", "RESOLVE audio $videoId NewPipe failed on both hosts; trying InnerTube")
                    val itQuality = if (forceLow) {
                        com.suvojeet.suvmusic.core.model.AudioQuality.LOW
                    } else {
                        sessionManager.getAudioQuality()
                    }
                    val innerTubeUrl = innerTubeClient.resolveAudioUrl(videoId, itQuality)
                    if (innerTubeUrl != null) {
                        android.util.Log.i("YouTubeStreaming", "RESOLVE audio $videoId InnerTube OK")
                        streamCache.put(cacheKey, CachedStream(innerTubeUrl, "m4a", System.currentTimeMillis()))
                    } else {
                        android.util.Log.e("YouTubeStreaming", "RESOLVE audio $videoId ALL methods failed (NewPipe + InnerTube)")
                        // Definitive resolution failure with no exception — NewPipe parsed
                        // the page but found no usable stream (classic extractor break).
                        Telemetry.report("stream.resolve", "youtube", AppError.Upstream("newpipe+innertube returned null"), mapOf("id" to videoId))
                    }
                    innerTubeUrl
                } catch (t: Throwable) {
                    android.util.Log.e("YouTubeStreaming", "RESOLVE audio $videoId async threw ${t.javaClass.simpleName}: ${t.message}", t)
                    Telemetry.report("stream.resolve", "youtube", t.toAppError(), mapOf("id" to videoId))
                    throw t
                } finally {
                    inFlightAudio.remove(inflightKey)
                }
            }
        }
        if (piggybacked) {
            android.util.Log.i("YouTubeStreaming", "INFLIGHT_PIGGYBACK audio $videoId — waiting on existing resolve")
        }
        val result = try {
            deferred.await()
        } catch (t: Throwable) {
            android.util.Log.w("YouTubeStreaming", "<< getStreamUrl AWAIT_FAIL vid=$videoId ${t.javaClass.simpleName}: ${t.message}")
            throw t
        }
        android.util.Log.i("YouTubeStreaming", "<< getStreamUrl EXIT vid=$videoId result=${if (result == null) "NULL" else "ok(${result.take(60)})"}")
        result
    }

    private suspend fun resolveStreamWithUrl(streamUrl: String, videoId: String, forceLow: Boolean = false): String? {
        val cacheKey = "audio_$videoId"
        val host = streamUrl.substringAfter("//").substringBefore("/")
        return retryWithBackoff(opTag = "audio[$videoId@$host]") {
            val startTime = System.currentTimeMillis()
            var audioQuality = if (forceLow) {
                com.suvojeet.suvmusic.core.model.AudioQuality.LOW
            } else {
                sessionManager.getAudioQuality()
            }

            // Adaptive logic for AUTO quality
            if (audioQuality == com.suvojeet.suvmusic.core.model.AudioQuality.AUTO) {
                audioQuality = if (networkMonitor.isOnWifi()) {
                    com.suvojeet.suvmusic.core.model.AudioQuality.MEDIUM
                } else {
                    com.suvojeet.suvmusic.core.model.AudioQuality.LOW
                }
                android.util.Log.d("YouTubeStreaming", "audio[$videoId@$host] AUTO resolved to $audioQuality (wifi=${networkMonitor.isOnWifi()})")
            }

            android.util.Log.d("YouTubeStreaming", "audio[$videoId@$host] getStreamExtractor()")
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            val fetchStart = System.currentTimeMillis()
            try {
                streamExtractor.fetchPage()
            } catch (e: Exception) {
                android.util.Log.e(
                    "YouTubeStreaming",
                    "audio[$videoId@$host] fetchPage() threw ${e.javaClass.simpleName} after ${System.currentTimeMillis() - fetchStart}ms: ${e.message}",
                    e,
                )
                throw e
            }
            android.util.Log.d("YouTubeStreaming", "audio[$videoId@$host] fetchPage() ok in ${System.currentTimeMillis() - fetchStart}ms")

            val audioStreams = try {
                streamExtractor.audioStreams
            } catch (e: Exception) {
                android.util.Log.e(
                    "YouTubeStreaming",
                    "audio[$videoId@$host] audioStreams accessor threw ${e.javaClass.simpleName}: ${e.message}",
                    e,
                )
                throw e
            }
            if (audioStreams.isEmpty()) {
                android.util.Log.w("YouTubeStreaming", "audio[$videoId@$host] audioStreams EMPTY — NewPipe parsed page but found no streams (likely extractor break)")
                return@retryWithBackoff null
            }
            android.util.Log.d(
                "YouTubeStreaming",
                "audio[$videoId@$host] found ${audioStreams.size} streams: ${audioStreams.joinToString { "${it.averageBitrate}kbps/${it.format?.name}" }}",
            )

            val targetBitrate = when (audioQuality) {
                com.suvojeet.suvmusic.core.model.AudioQuality.LOW -> 70
                com.suvojeet.suvmusic.core.model.AudioQuality.MEDIUM -> 160
                com.suvojeet.suvmusic.core.model.AudioQuality.HIGH -> 512
                com.suvojeet.suvmusic.core.model.AudioQuality.AUTO -> 160
            }

            val bestAudioStream = audioStreams
                .filter { it.averageBitrate <= targetBitrate }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }

            val latency = System.currentTimeMillis() - startTime
            val extension = when (bestAudioStream?.format?.name?.uppercase()) {
                "M4A", "AAC" -> "m4a"
                "WEBM", "OPUS" -> "opus"
                else -> "m4a"
            }
            if (bestAudioStream == null) {
                android.util.Log.w("YouTubeStreaming", "audio[$videoId@$host] no bestStream picked from ${audioStreams.size} candidates (target=${targetBitrate}kbps)")
                return@retryWithBackoff null
            }
            if (bestAudioStream.content.isNullOrBlank()) {
                android.util.Log.w("YouTubeStreaming", "audio[$videoId@$host] bestStream picked (${bestAudioStream.averageBitrate}kbps) but content URL is blank")
                return@retryWithBackoff null
            }
            android.util.Log.d(
                "YouTubeStreaming",
                "audio[$videoId@$host] picked ${bestAudioStream.averageBitrate}kbps/$extension in ${latency}ms (target=${targetBitrate}kbps, quality=$audioQuality)",
            )

            bestAudioStream.content.also { url ->
                streamCache.put(cacheKey, CachedStream(url, extension, System.currentTimeMillis()))
            }
        }
    }

    /**
     * Data class to hold video stream result with optional separate audio URL.
     */
    data class VideoStreamResult(
        val videoUrl: String,
        val audioUrl: String? = null,
        val resolution: String? = null
    )
    
    suspend fun getVideoStreamUrl(videoId: String, quality: com.suvojeet.suvmusic.core.model.VideoQuality? = null, forceLow: Boolean = false): String? = withContext(Dispatchers.IO) {
        getVideoStreamResult(videoId, quality, forceLow)?.videoUrl
    }
    
    suspend fun getVideoStreamResult(videoId: String, quality: com.suvojeet.suvmusic.core.model.VideoQuality? = null, forceLow: Boolean = false): VideoStreamResult? = withContext(Dispatchers.IO) {
        val targetQuality = if (forceLow) {
            com.suvojeet.suvmusic.core.model.VideoQuality.LOW
        } else {
            quality ?: sessionManager.getVideoQuality()
        }

        val videoCacheKey = "video_${videoId}_${targetQuality.name}"
        val audioCacheKey = "video_audio_${videoId}_${targetQuality.name}"

        if (forceLow) {
            android.util.Log.d("YouTubeStreaming", "CACHE_BYPASS video $videoId/$targetQuality (forceLow=true)")
            streamCache.remove(videoCacheKey)
            streamCache.remove(audioCacheKey)
        } else {
            val cachedVideo = streamCache.get(videoCacheKey)
            val cachedAudio = streamCache.get(audioCacheKey)

            if (cachedVideo != null && cachedUrlStillValid(cachedVideo) && (cachedAudio == null || cachedUrlStillValid(cachedAudio))) {
                android.util.Log.d("YouTubeStreaming", "CACHE_HIT video $videoId/$targetQuality (age=${System.currentTimeMillis() - cachedVideo.timestamp}ms)")
                return@withContext VideoStreamResult(
                    videoUrl = cachedVideo.url,
                    audioUrl = cachedAudio?.url
                )
            }
            if (cachedVideo != null) {
                streamCache.remove(videoCacheKey)
                streamCache.remove(audioCacheKey)
            }
            android.util.Log.d("YouTubeStreaming", "CACHE_MISS video $videoId/$targetQuality")
        }

        val inflightKey = "$videoId|${targetQuality.name}|$forceLow"
        var piggybacked = true
        val deferred = inFlightVideo.computeIfAbsent(inflightKey) {
            piggybacked = false
            dedupScope.async {
                try {
                    android.util.Log.d("YouTubeStreaming", "RESOLVE video $videoId primary=www.youtube.com quality=$targetQuality forceLow=$forceLow")
                    val primaryResult = resolveVideoWithUrl("https://www.youtube.com/watch?v=$videoId", videoId, targetQuality)
                    if (primaryResult != null) {
                        return@async primaryResult
                    }
                    android.util.Log.w("YouTubeStreaming", "RESOLVE video $videoId primary returned null; falling back to music.youtube.com")
                    val fallback = resolveVideoWithUrl("https://music.youtube.com/watch?v=$videoId", videoId, targetQuality)
                    if (fallback == null) {
                        android.util.Log.e("YouTubeStreaming", "RESOLVE video $videoId BOTH primary and music fallback failed")
                    } else {
                        android.util.Log.d("YouTubeStreaming", "RESOLVE video $videoId music fallback succeeded")
                    }
                    fallback
                } finally {
                    inFlightVideo.remove(inflightKey)
                }
            }
        }
        if (piggybacked) {
            android.util.Log.d("YouTubeStreaming", "INFLIGHT_PIGGYBACK video $videoId/$targetQuality")
        }
        deferred.await()
    }

    private suspend fun resolveVideoWithUrl(
        streamUrl: String,
        videoId: String,
        targetQuality: com.suvojeet.suvmusic.core.model.VideoQuality
    ): VideoStreamResult? {
        val videoCacheKey = "video_${videoId}_${targetQuality.name}"
        val audioCacheKey = "video_audio_${videoId}_${targetQuality.name}"
        val host = streamUrl.substringAfter("//").substringBefore("/")

        return retryWithBackoff(opTag = "video[$videoId@$host/$targetQuality]") {
            var quality = targetQuality

            // Adaptive logic for AUTO quality
            if (quality == com.suvojeet.suvmusic.core.model.VideoQuality.AUTO) {
                quality = if (networkMonitor.isOnWifi()) {
                    com.suvojeet.suvmusic.core.model.VideoQuality.MEDIUM
                } else {
                    com.suvojeet.suvmusic.core.model.VideoQuality.LOW
                }
                android.util.Log.d("YouTubeStreaming", "video[$videoId@$host] AUTO resolved to $quality (wifi=${networkMonitor.isOnWifi()})")
            }

            android.util.Log.d("YouTubeStreaming", "video[$videoId@$host] getStreamExtractor()")
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            val fetchStart = System.currentTimeMillis()
            try {
                streamExtractor.fetchPage()
            } catch (e: Exception) {
                android.util.Log.e(
                    "YouTubeStreaming",
                    "video[$videoId@$host] fetchPage() threw ${e.javaClass.simpleName} after ${System.currentTimeMillis() - fetchStart}ms: ${e.message}",
                    e,
                )
                throw e
            }
            android.util.Log.d("YouTubeStreaming", "video[$videoId@$host] fetchPage() ok in ${System.currentTimeMillis() - fetchStart}ms")
            
            val targetResolution = quality.maxResolution
            var result: VideoStreamResult? = null
            
            if (targetResolution >= 720) {
                try {
                    val videoOnlyStreams = streamExtractor.videoOnlyStreams
                    val audioStreams = streamExtractor.audioStreams
                    
                    val bestVideoStream = videoOnlyStreams
                        .filter { stream ->
                            val height = stream.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                            height in 1..targetResolution
                        }
                        .maxByOrNull { stream ->
                            stream.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                        }
                    
                    val bestAudioStream = audioStreams.maxByOrNull { it.averageBitrate }
                    
                    if (bestVideoStream != null && bestAudioStream != null) {
                        val videoUrl = bestVideoStream.content
                        val audioUrl = bestAudioStream.content
                        
                        if (videoUrl != null && audioUrl != null) {
                            val videoExtension = if (bestVideoStream.format?.name?.uppercase()?.contains("WEBM") == true) "webm" else "mp4"
                            val audioExtension = if (bestAudioStream.format?.name?.uppercase()?.contains("OPUS") == true) "opus" else "m4a"
                            
                            streamCache.put(videoCacheKey, CachedStream(videoUrl, videoExtension, System.currentTimeMillis()))
                            streamCache.put(audioCacheKey, CachedStream(audioUrl, audioExtension, System.currentTimeMillis()))
                            
                            result = VideoStreamResult(videoUrl, audioUrl, bestVideoStream.resolution)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("YouTubeStreaming", "Failed to get video-only streams for $videoId")
                }
            }
            
            if (result == null) {
                val muxedStreams = streamExtractor.videoStreams
                val bestMuxedStream = muxedStreams
                    .filter { stream ->
                        val height = stream.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                        height <= targetResolution && height > 0
                    }
                    .maxByOrNull { stream ->
                        stream.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                    } ?: muxedStreams.maxByOrNull { 
                        it.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                    }

                bestMuxedStream?.content?.let { videoUrl ->
                    val extension = if (bestMuxedStream.format?.name?.uppercase()?.contains("WEBM") == true) "webm" else "mp4"
                    streamCache.put(videoCacheKey, CachedStream(videoUrl, extension, System.currentTimeMillis()))
                    result = VideoStreamResult(videoUrl, null, bestMuxedStream.resolution)
                }
            }
            
            result
        }
    }

    suspend fun getStreamUrlForDownload(videoId: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val cacheKey = "audio_$videoId"
        streamCache.get(cacheKey)?.let { cached ->
            if (cachedUrlStillValid(cached)) {
                return@withContext cached.url to cached.extension
            }
            streamCache.remove(cacheKey)
        }

        retryWithBackoff {
            val downloadQuality = sessionManager.getDownloadQuality()
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            val targetBitrate = downloadQuality.maxBitrate
            
            val bestAudioStream = audioStreams
                .filter { it.averageBitrate <= targetBitrate }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }
            
            val extension = if (bestAudioStream?.format?.name?.uppercase()?.contains("OPUS") == true) "opus" else "m4a"

            bestAudioStream?.content?.let { url ->
                streamCache.put(cacheKey, CachedStream(url, extension, System.currentTimeMillis()))
                url to extension
            }
        }
    }

    suspend fun getMuxedVideoStreamUrlForDownload(
        videoId: String,
        maxResolution: Int = 720
    ): String? = withContext(Dispatchers.IO) {
        retryWithBackoff {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()

            val videoStreams = streamExtractor.videoStreams
            val best = videoStreams
                .filter { stream ->
                    val h = stream.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                    h in 1..maxResolution
                }
                .maxByOrNull { stream ->
                    stream.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                } ?: videoStreams.maxByOrNull { 
                    it.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                }

            best?.content
        }
    }

    suspend fun getSongDetails(videoId: String): Song? = withContext(Dispatchers.IO) {
        retryWithBackoff {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            Song.fromYouTube(
                videoId = videoId,
                title = streamExtractor.name ?: "Unknown",
                artist = streamExtractor.uploaderName ?: "Unknown Artist",
                album = "",
                thumbnailUrl = streamExtractor.thumbnails.maxByOrNull { it.width * it.height }?.url,
                duration = streamExtractor.length * 1000,
                releaseDate = streamExtractor.textualUploadDate
            )
        }
    }

    suspend fun getRelatedItems(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        retryWithBackoff {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()

            val results = mutableListOf<Song>()
            val itemsPage = streamExtractor.relatedItems
            
            // Fetch related items from the current page
            if (itemsPage != null) {
                for (item in itemsPage.items) {
                    if (item is StreamInfoItem) {
                        mapToSong(item)?.let { results.add(it) }
                    }
                }
            }
            results
        } ?: emptyList()
    }

    private fun mapToSong(item: StreamInfoItem): Song? {
        val id = item.url?.substringAfter("v=")?.substringBefore("&")
            ?: item.url?.substringAfter("youtu.be/")?.substringBefore("?")
            ?: return null

        return Song.fromYouTube(
            videoId = id,
            title = item.name ?: "Unknown",
            artist = item.uploaderName ?: "Unknown Artist",
            album = "",
            duration = item.duration * 1000L,
            thumbnailUrl = item.thumbnails.lastOrNull()?.url
        )
    }

    fun clearCacheFor(videoId: String) {
        streamCache.remove("audio_$videoId")
        com.suvojeet.suvmusic.core.model.VideoQuality.entries.forEach { 
            streamCache.remove("video_${videoId}_${it.name}")
            streamCache.remove("video_audio_${videoId}_${it.name}")
        }
    }
}

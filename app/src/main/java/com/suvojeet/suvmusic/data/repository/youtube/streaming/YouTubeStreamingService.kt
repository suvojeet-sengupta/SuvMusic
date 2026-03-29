package com.suvojeet.suvmusic.data.repository.youtube.streaming

import android.util.LruCache
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
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
    private val networkMonitor: com.suvojeet.suvmusic.util.NetworkMonitor
) {
    private val ytService: org.schabi.newpipe.extractor.StreamingService by lazy {
        ServiceList.all().find { it.serviceInfo.name == "YouTube" }
            ?: throw IllegalStateException("YouTube service not found")
    }

    // Cache for stream URLs to avoid re-fetching (max 50 entries, 3 hours expiry)
    private data class CachedStream(val url: String, val extension: String, val timestamp: Long)
    private val streamCache = LruCache<String, CachedStream>(50)
    private val CACHE_EXPIRY_MS = 3 * 60 * 60 * 1000L // 3 hours

    /**
     * Helper to retry operations with exponential backoff.
     */
    private suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 500, // 0.5 sec
        maxDelay: Long = 2000,    // 2 sec
        factor: Double = 2.0,
        block: suspend () -> T?
    ): T? {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                val result = block()
                if (result != null) return result
            } catch (e: Exception) {
                android.util.Log.w("YouTubeStreaming", "Operation failed, retrying in ${currentDelay}ms", e)
                if (e is org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException) {
                    return null // Don't retry for "not available"
                }
            }
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return try {
            block() // Final attempt
        } catch (e: Exception) {
            android.util.Log.e("YouTubeStreaming", "Operation failed after multiple retries", e)
            null
        }
    }

    /**
     * Get audio stream URL for playback.
     * Uses user's audio quality preference and caches the result.
     */
    suspend fun getStreamUrl(videoId: String, forceLow: Boolean = false): String? = withContext(Dispatchers.IO) {
        val cacheKey = "audio_$videoId"
        if (!forceLow) {
            streamCache.get(cacheKey)?.let { cached ->
                if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRY_MS) {
                    return@withContext cached.url
                }
            }
        } else {
            streamCache.remove(cacheKey)
        }
        
        // Try primary URL
        val primaryResult = resolveStreamWithUrl("https://www.youtube.com/watch?v=$videoId", videoId, forceLow)
        if (primaryResult != null) return@withContext primaryResult
        
        // Try music fallback
        android.util.Log.d("YouTubeStreaming", "Primary resolution failed for $videoId, trying music fallback")
        resolveStreamWithUrl("https://music.youtube.com/watch?v=$videoId", videoId, forceLow)
    }

    private suspend fun resolveStreamWithUrl(streamUrl: String, videoId: String, forceLow: Boolean = false): String? {
        val cacheKey = "audio_$videoId"
        return retryWithBackoff {
            val startTime = System.currentTimeMillis()
            var audioQuality = if (forceLow) {
                com.suvojeet.suvmusic.data.model.AudioQuality.LOW
            } else {
                sessionManager.getAudioQuality()
            }
            
            // Adaptive logic for AUTO quality
            if (audioQuality == com.suvojeet.suvmusic.data.model.AudioQuality.AUTO) {
                audioQuality = if (networkMonitor.isOnWifi()) {
                    com.suvojeet.suvmusic.data.model.AudioQuality.MEDIUM
                } else {
                    com.suvojeet.suvmusic.data.model.AudioQuality.LOW
                }
            }
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            if (audioStreams.isEmpty()) return@retryWithBackoff null

            val targetBitrate = when (audioQuality) {
                com.suvojeet.suvmusic.data.model.AudioQuality.LOW -> 70
                com.suvojeet.suvmusic.data.model.AudioQuality.MEDIUM -> 160
                com.suvojeet.suvmusic.data.model.AudioQuality.HIGH -> 512
                com.suvojeet.suvmusic.data.model.AudioQuality.AUTO -> 160
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
            android.util.Log.d("YouTubeStreaming", "Audio fetched in ${latency}ms for $videoId. Bitrate: ${bestAudioStream?.averageBitrate}kbps")

            bestAudioStream?.content?.also { url ->
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
    
    suspend fun getVideoStreamUrl(videoId: String, quality: com.suvojeet.suvmusic.data.model.VideoQuality? = null, forceLow: Boolean = false): String? = withContext(Dispatchers.IO) {
        getVideoStreamResult(videoId, quality, forceLow)?.videoUrl
    }
    
    suspend fun getVideoStreamResult(videoId: String, quality: com.suvojeet.suvmusic.data.model.VideoQuality? = null, forceLow: Boolean = false): VideoStreamResult? = withContext(Dispatchers.IO) {
        val targetQuality = if (forceLow) {
            com.suvojeet.suvmusic.data.model.VideoQuality.LOW
        } else {
            quality ?: sessionManager.getVideoQuality()
        }
        
        val videoCacheKey = "video_${videoId}_${targetQuality.name}"
        val audioCacheKey = "video_audio_${videoId}_${targetQuality.name}"
        
        if (forceLow) {
            streamCache.remove(videoCacheKey)
            streamCache.remove(audioCacheKey)
        } else {
            val cachedVideo = streamCache.get(videoCacheKey)
            val cachedAudio = streamCache.get(audioCacheKey)
            
            if (cachedVideo != null && System.currentTimeMillis() - cachedVideo.timestamp < CACHE_EXPIRY_MS) {
                return@withContext VideoStreamResult(
                    videoUrl = cachedVideo.url,
                    audioUrl = cachedAudio?.url
                )
            }
        }

        val primaryResult = resolveVideoWithUrl("https://www.youtube.com/watch?v=$videoId", videoId, targetQuality)
        if (primaryResult != null) return@withContext primaryResult

        android.util.Log.d("YouTubeStreaming", "Primary video resolution failed for $videoId, trying music fallback")
        resolveVideoWithUrl("https://music.youtube.com/watch?v=$videoId", videoId, targetQuality)
    }

    private suspend fun resolveVideoWithUrl(
        streamUrl: String,
        videoId: String,
        targetQuality: com.suvojeet.suvmusic.data.model.VideoQuality
    ): VideoStreamResult? {
        val videoCacheKey = "video_${videoId}_${targetQuality.name}"
        val audioCacheKey = "video_audio_${videoId}_${targetQuality.name}"

        return retryWithBackoff {
            var quality = targetQuality
            
            // Adaptive logic for AUTO quality
            if (quality == com.suvojeet.suvmusic.data.model.VideoQuality.AUTO) {
                quality = if (networkMonitor.isOnWifi()) {
                    com.suvojeet.suvmusic.data.model.VideoQuality.MEDIUM
                } else {
                    com.suvojeet.suvmusic.data.model.VideoQuality.LOW
                }
            }
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
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
            if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRY_MS) {
                return@withContext cached.url to cached.extension
            }
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
            
            Song(
                id = videoId,
                title = streamExtractor.name ?: "Unknown",
                artist = streamExtractor.uploaderName ?: "Unknown Artist",
                album = "",
                thumbnailUrl = streamExtractor.thumbnails.maxByOrNull { it.width * it.height }?.url,
                duration = streamExtractor.length * 1000,
                source = com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
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
            
            if (itemsPage != null) {
                for (item in itemsPage.items) {
                    if (item is StreamInfoItem) {
                        try {
                            val id = item.url?.substringAfter("v=")?.substringBefore("&") 
                                ?: item.url?.substringAfter("youtu.be/")?.substringBefore("?")
                            
                            if (id != null) {
                                Song.fromYouTube(
                                    videoId = id,
                                    title = item.name ?: "Unknown",
                                    artist = item.uploaderName ?: "Unknown Artist",
                                    album = "",
                                    duration = item.duration * 1000L,
                                    thumbnailUrl = item.thumbnails.lastOrNull()?.url
                                )?.let { results.add(it) }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            results
        } ?: emptyList()
    }

    fun clearCacheFor(videoId: String) {
        streamCache.remove("audio_$videoId")
        com.suvojeet.suvmusic.data.model.VideoQuality.entries.forEach { 
            streamCache.remove("video_${videoId}_${it.name}")
            streamCache.remove("video_audio_${videoId}_${it.name}")
        }
    }
}

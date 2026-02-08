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
@Singleton
class YouTubeStreamingService @Inject constructor(
    private val sessionManager: SessionManager
) {
    // Cache for stream URLs to avoid re-fetching (max 50 entries, 30 min expiry)
    private data class CachedStream(val url: String, val timestamp: Long)
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
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                // Log and retry
                android.util.Log.w("YouTubeStreaming", "Operation failed, retrying in ${currentDelay}ms", e)
                kotlinx.coroutines.delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
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
    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // Check cache first for fast playback
        val cacheKey = "audio_$videoId"
        streamCache.get(cacheKey)?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRY_MS) {
                android.util.Log.d("YouTubeStreaming", "Stream URL from cache: $videoId")
                return@withContext cached.url
            }
        }
        
        retryWithBackoff {
            val startTime = System.currentTimeMillis()
            val audioQuality = sessionManager.getAudioQuality()
            android.util.Log.d("YouTubeStreaming", "Fetching audio stream for $videoId. Target Quality: $audioQuality")

            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: throw IllegalStateException("YouTube service not found")
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            val targetBitrate = when (audioQuality) {
                com.suvojeet.suvmusic.data.model.AudioQuality.LOW -> 64
                com.suvojeet.suvmusic.data.model.AudioQuality.MEDIUM -> 128
                com.suvojeet.suvmusic.data.model.AudioQuality.HIGH -> 256
            }
            
            val bestAudioStream = audioStreams
                .filter { it.averageBitrate <= targetBitrate }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }
            
            val latency = System.currentTimeMillis() - startTime
            android.util.Log.d("YouTubeStreaming", "Audio stream fetched in ${latency}ms. Selected bitrate: ${bestAudioStream?.averageBitrate}kbps (Target: $targetBitrate)")

            bestAudioStream?.content?.also { url ->
                // Cache the result
                streamCache.put(cacheKey, CachedStream(url, System.currentTimeMillis()))
            }
        }
    }

    /**
     * Data class to hold video stream result with optional separate audio URL.
     * When videoUrl and audioUrl are both present, they need to be merged for playback.
     * When only videoUrl is present, it's a muxed stream (has audio included).
     */
    data class VideoStreamResult(
        val videoUrl: String,
        val audioUrl: String? = null,  // null means video has audio included (muxed stream)
        val resolution: String? = null
    )
    
    /**
     * Get video stream URLs for video playback mode.
     * For 720p+ quality, uses video-only streams with separate audio for best quality.
     * For lower quality, uses muxed streams (video+audio combined).
     */
    suspend fun getVideoStreamUrl(videoId: String, quality: com.suvojeet.suvmusic.data.model.VideoQuality? = null): String? = withContext(Dispatchers.IO) {
        getVideoStreamResult(videoId, quality)?.videoUrl
    }
    
    /**
     * Get video stream result with both video and audio URLs for high-quality playback.
     * This is the preferred method for video playback - it returns separate streams for 720p+.
     */
    suspend fun getVideoStreamResult(videoId: String, quality: com.suvojeet.suvmusic.data.model.VideoQuality? = null): VideoStreamResult? = withContext(Dispatchers.IO) {
        val targetQuality = quality ?: sessionManager.getVideoQuality()
        
        // Check cache first for fast playback
        // Include quality in cache key to separate different resolutions
        val videoCacheKey = "video_${videoId}_${targetQuality.name}"
        val audioCacheKey = "video_audio_${videoId}_${targetQuality.name}"
        
        val cachedVideo = streamCache.get(videoCacheKey)
        val cachedAudio = streamCache.get(audioCacheKey)
        
        if (cachedVideo != null && System.currentTimeMillis() - cachedVideo.timestamp < CACHE_EXPIRY_MS) {
            android.util.Log.d("YouTubeStreaming", "Video stream URL from cache: $videoId (${targetQuality.name})")
            return@withContext VideoStreamResult(
                videoUrl = cachedVideo.url,
                audioUrl = cachedAudio?.url  // May be null for muxed streams
            )
        }
        
        retryWithBackoff {
            android.util.Log.d("YouTubeStreaming", "Fetching video stream for $videoId. Quality: $targetQuality (Max: ${targetQuality.maxResolution}p)")

            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: throw IllegalStateException("YouTube service not found")
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val targetResolution = targetQuality.maxResolution
            
            // Strategy:
            // 1. For higher quality (720p, 1080p), use videoOnlyStreams + separate audio
            //    These are typically higher quality but require merging
            // 2. For lower quality (360p) or as fallback, use muxed videoStreams
            
            var videoResult: VideoStreamResult? = null
            
            // Try DASH stream first (Native adaptive streaming for 720p/1080p)
            if (targetResolution >= 720) {
                try {
                    val dashUrl = streamExtractor.dashMpdUrl
                    if (!dashUrl.isNullOrEmpty()) {
                        android.util.Log.d("YouTubeStreaming", "Using DASH manifest: $dashUrl")
                        videoResult = VideoStreamResult(
                            videoUrl = dashUrl,
                            audioUrl = null, // DASH handles audio internally
                            resolution = "Auto (DASH)"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("YouTubeStreaming", "Failed to get DASH URL", e)
                }
            }
            
            // If DASH not available, try finding best muxed stream (unlikely for high res, but fallback)
            
            // Fallback to muxed streams (video + audio combined) for lower quality or if video-only failed
            if (videoResult == null) {
                val videoStreams = streamExtractor.videoStreams
                android.util.Log.d("YouTubeStreaming", "Available muxed streams: ${videoStreams.map { it.resolution }}")
                
                val bestMuxedStream = videoStreams
                    .filter { stream ->
                        val resolutionString = stream.resolution ?: return@filter false
                        val height = resolutionString.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                        height <= targetResolution && height > 0
                    }
                    .maxByOrNull { stream ->
                        val resolutionString = stream.resolution ?: "0"
                        resolutionString.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    }
                    ?: videoStreams.maxByOrNull { stream ->
                        val resolutionString = stream.resolution ?: "0"
                        resolutionString.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    }
                
                if (bestMuxedStream != null) {
                    android.util.Log.d("YouTubeStreaming", "Using muxed stream: ${bestMuxedStream.resolution}")
                    
                    bestMuxedStream.content?.let { url ->
                        streamCache.put(videoCacheKey, CachedStream(url, System.currentTimeMillis()))
                        videoResult = VideoStreamResult(
                            videoUrl = url,
                            audioUrl = null,  // Muxed stream - no separate audio needed
                            resolution = bestMuxedStream.resolution
                        )
                    }
                }
            }
            
            videoResult
        }
    }

    /**
     * Get stream URL for downloading with the user's download quality preference.
     */
    /**
     * Get stream URL for downloading with the user's download quality preference.
     * Prioritizes cached playback URL if available (to ensure download success if song is playing).
     */
    /**
     * Get stream URL for downloading with the user's download quality preference.
     * Prioritizes cached playback URL if available (to ensure download success if song is playing).
     */
    suspend fun getStreamUrlForDownload(videoId: String): String? = withContext(Dispatchers.IO) {
        // 1. Check cache first (audio_ cache from playback)
        // If the user is listening to it, we know this URL works.
        val cacheKey = "audio_$videoId"
        streamCache.get(cacheKey)?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRY_MS) {
                android.util.Log.d("YouTubeStreaming", "Download using cached playback URL: $videoId")
                return@withContext cached.url
            }
        }
        
        retryWithBackoff {
            val startTime = System.currentTimeMillis()
            val downloadQuality = sessionManager.getDownloadQuality()
            android.util.Log.d("YouTubeStreaming", "Fetching download stream for $videoId. Quality: $downloadQuality (Max Bitrate: ${downloadQuality.maxBitrate})")
            
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: throw IllegalStateException("YouTube service not found")
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            val targetBitrate = downloadQuality.maxBitrate
            
            val bestAudioStream = audioStreams
                .filter { it.averageBitrate <= targetBitrate }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }
            
            val latency = System.currentTimeMillis() - startTime
            android.util.Log.d("YouTubeStreaming", "Download stream fetched in ${latency}ms. Selected bitrate: ${bestAudioStream?.averageBitrate}kbps")

            // Cache this result too, so subsequent playback uses it
            bestAudioStream?.content?.also { url ->
                streamCache.put(cacheKey, CachedStream(url, System.currentTimeMillis()))
            }
            
            bestAudioStream?.content
        }
    }

    /**
     * Get song details from a video ID.
     * Used for deep linking to play songs from YouTube/YouTube Music URLs.
     */
    suspend fun getSongDetails(videoId: String): Song? = withContext(Dispatchers.IO) {
        retryWithBackoff {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: throw IllegalStateException("YouTube service not found")
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val title = streamExtractor.name ?: "Unknown Title"
            val artist = streamExtractor.uploaderName ?: "Unknown Artist"
            val thumbnailUrl = streamExtractor.thumbnails.maxByOrNull { it.width * it.height }?.url

            val duration = streamExtractor.length * 1000 // Convert to milliseconds
            
            Song(
                id = videoId,
                title = title,
                artist = artist,
                album = "", // Not available from stream extractor
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                source = com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE
            )
        }
    }

    /**
     * Get related songs from the extractor (NewPipe).
     * This provides the "Up Next" or related videos logic directly from the video page.
     */
    suspend fun getRelatedItems(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        retryWithBackoff {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" }
                ?: throw IllegalStateException("YouTube service not found")

            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()

            val results = mutableListOf<Song>()
            val items: List<*>? = streamExtractor.relatedItems as? List<*>
            
            if (items != null) {
                for (item in items) {
                    if (item is StreamInfoItem) {
                        try {
                            // Extract video ID from URL
                            val id = item.url?.substringAfter("v=")?.substringBefore("&") 
                                ?: item.url?.substringAfter("youtu.be/")?.substringBefore("?")
                            
                            if (id != null) {
                                Song.fromYouTube(
                                    videoId = id,
                                    title = item.name ?: "Unknown",
                                    artist = item.uploaderName ?: "Unknown Artist",
                                    album = "",
                                    duration = item.duration * 1000L,
                                    thumbnailUrl = item.thumbnails?.lastOrNull()?.url
                                )?.let { results.add(it) }
                            }
                        } catch (e: Exception) {
                            // Ignore error for single item
                        }
                    }
                }
            }
            results
        } ?: emptyList()
    }
}

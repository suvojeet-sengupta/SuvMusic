package com.suvojeet.suvmusic.data.repository.youtube.streaming

import android.util.LruCache
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
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
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: throw IllegalStateException("YouTube service not found")
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            val targetBitrate = when (sessionManager.getAudioQuality()) {
                com.suvojeet.suvmusic.data.model.AudioQuality.LOW -> 64
                com.suvojeet.suvmusic.data.model.AudioQuality.MEDIUM -> 128
                com.suvojeet.suvmusic.data.model.AudioQuality.HIGH -> 256
            }
            
            val bestAudioStream = audioStreams
                .filter { it.averageBitrate <= targetBitrate }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }
            
            bestAudioStream?.content?.also { url ->
                // Cache the result
                streamCache.put(cacheKey, CachedStream(url, System.currentTimeMillis()))
            }
        }
    }

    /**
     * Get video stream URL for video playback mode.
     * Returns the best quality video stream that includes audio (for combined playback).
     */
    suspend fun getVideoStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // Check cache first for fast playback
        val cacheKey = "video_$videoId"
        streamCache.get(cacheKey)?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRY_MS) {
                android.util.Log.d("YouTubeStreaming", "Video stream URL from cache: $videoId")
                return@withContext cached.url
            }
        }
        
        retryWithBackoff {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: throw IllegalStateException("YouTube service not found")
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            // Get video streams (these include audio in the stream)
            val videoStreams = streamExtractor.videoStreams
            
            // Filter for streams with resolution <= 720p to reduce bandwidth
            // and sort by resolution to get best quality
            val bestVideoStream = videoStreams
                .filter { 
                    val height = it.resolution?.replace("p", "")?.toIntOrNull() ?: 0
                    height <= 720 && height > 0
                }
                .maxByOrNull { 
                    it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 
                }
                ?: videoStreams.firstOrNull() // Fallback to any available stream
            
            android.util.Log.d("YouTubeStreaming", "Video stream: ${bestVideoStream?.resolution}")
            
            bestVideoStream?.content?.also { url ->
                // Cache the result
                streamCache.put(cacheKey, CachedStream(url, System.currentTimeMillis()))
            }
        }
    }

    /**
     * Get stream URL for downloading with the user's download quality preference.
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
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: throw IllegalStateException("YouTube service not found")
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            val targetBitrate = sessionManager.getDownloadQuality().maxBitrate
            
            val bestAudioStream = audioStreams
                .filter { it.averageBitrate <= targetBitrate }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }
            
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
                source = com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE
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

            streamExtractor.relatedItems?.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()?.mapNotNull { item ->
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
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } ?: emptyList()
    }
}

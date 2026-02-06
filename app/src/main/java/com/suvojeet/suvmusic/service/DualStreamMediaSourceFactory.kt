package com.suvojeet.suvmusic.service

import android.net.Uri
import android.util.Base64
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource

/**
 * Custom MediaSourceFactory that handles dual-stream video playback.
 * 
 * For 720p/1080p video quality, YouTube provides separate video-only and audio streams.
 * This factory detects the "dualstream://" URI scheme and creates a MergingMediaSource
 * to combine both streams for playback.
 * 
 * URI Format: dualstream://base64(videoUrl)|base64(audioUrl)
 * 
 * For muxed streams (360p or fallback), it delegates to the default factory.
 */
@OptIn(UnstableApi::class)
class DualStreamMediaSourceFactory(
    private val dataSourceFactory: DataSource.Factory,
    private val defaultFactory: MediaSource.Factory
) : MediaSource.Factory {

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider): MediaSource.Factory {
        defaultFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy): MediaSource.Factory {
        defaultFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        return this
    }

    override fun getSupportedTypes(): IntArray {
        return defaultFactory.supportedTypes
    }

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val uri = mediaItem.localConfiguration?.uri?.toString() ?: return defaultFactory.createMediaSource(mediaItem)
        
        // Check for dual-stream URI scheme
        if (uri.startsWith("dualstream://")) {
            try {
                android.util.Log.d("DualStreamFactory", "Detected dual-stream URI, parsing video and audio URLs")
                
                // Parse: "dualstream://base64video|base64audio"
                val encodedPart = uri.removePrefix("dualstream://")
                val parts = encodedPart.split("|")
                
                if (parts.size == 2) {
                    val videoUrl = String(Base64.decode(parts[0], Base64.NO_WRAP))
                    val audioUrl = String(Base64.decode(parts[1], Base64.NO_WRAP))
                    
                    android.util.Log.d("DualStreamFactory", "Creating MergingMediaSource: video=${videoUrl.take(50)}..., audio=${audioUrl.take(50)}...")
                    
                    // Create video-only source
                    val videoMediaItem = mediaItem.buildUpon()
                        .setUri(Uri.parse(videoUrl))
                        .build()
                    val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(videoMediaItem)
                    
                    // Create audio source
                    val audioMediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(audioUrl))
                        .build()
                    val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(audioMediaItem)
                    
                    // Merge video and audio streams
                    return MergingMediaSource(videoSource, audioSource)
                }
            } catch (e: Exception) {
                android.util.Log.e("DualStreamFactory", "Failed to parse dual-stream URI", e)
            }
        }
        
        // Fallback to default factory for muxed streams
        return defaultFactory.createMediaSource(mediaItem)
    }
}


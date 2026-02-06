package com.suvojeet.suvmusic.service

import android.net.Uri
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
 * This factory detects the "AUDIO_STREAM_URL" extra in MediaItem metadata and creates
 * a MergingMediaSource to combine both streams for playback.
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
        // Check for separate audio URL in extras
        val audioStreamUrl = mediaItem.mediaMetadata.extras?.getString("AUDIO_STREAM_URL")
        
        if (audioStreamUrl != null) {
            android.util.Log.d("DualStreamFactory", "Creating MergingMediaSource for dual-stream video")
            
            // Create video source from the main URI
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            
            // Create audio source from the extras URL
            val audioMediaItem = MediaItem.Builder()
                .setUri(Uri.parse(audioStreamUrl))
                .build()
            val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(audioMediaItem)
            
            // Merge video and audio streams
            return MergingMediaSource(videoSource, audioSource)
        }
        
        // Fallback to default factory for muxed streams
        return defaultFactory.createMediaSource(mediaItem)
    }
}

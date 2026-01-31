package com.suvojeet.suvmusic.data.model

/**
 * Video quality settings for streaming.
 */
enum class VideoQuality(val label: String, val maxResolution: Int) {
    LOW("Low (360p)", 360),
    MEDIUM("Medium (720p)", 720),
    HIGH("High (1080p)", 1080);
    
    companion object {
        fun fromResolution(resolution: Int): VideoQuality {
            return entries.find { resolution <= it.maxResolution } ?: HIGH
        }
    }
}

package com.suvojeet.suvmusic.core.model

/**
 * Video quality settings for streaming.
 */
enum class VideoQuality(val label: String, val maxResolution: Int) {
    AUTO("Auto (Adaptive)", 720),
    LOW("Low (360p)", 360),
    MEDIUM("Medium (720p)", 720),
    HIGH("High (1080p)", 1080),
    QHD("QHD (1440p)", 1440),
    UHD("4K (2160p)", 2160);

    companion object {
        fun fromResolution(resolution: Int): VideoQuality {
            return entries.find { resolution <= it.maxResolution } ?: UHD
        }
    }
}

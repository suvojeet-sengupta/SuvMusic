package com.suvojeet.suvmusic.data.model

/**
 * Download quality settings for audio.
 * Lower quality = smaller file size, saves storage space.
 */
enum class DownloadQuality(val label: String, val maxBitrate: Int) {
    LOW("Low (64 kbps) • Saves data", 64),
    MEDIUM("Medium (128 kbps)", 128),
    HIGH("High (256 kbps)", 256);
}

/**
 * Video download quality settings.
 * Determines the maximum resolution when downloading a video.
 */
enum class VideoDownloadQuality(val label: String, val maxResolution: Int) {
    LOW("360p • Saves storage", 360),
    MEDIUM("720p HD", 720),
    HIGH("1080p Full HD", 1080);
}

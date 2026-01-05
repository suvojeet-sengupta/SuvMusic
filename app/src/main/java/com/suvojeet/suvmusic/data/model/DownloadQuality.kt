package com.suvojeet.suvmusic.data.model

/**
 * Download quality settings.
 * Lower quality = smaller file size, saves storage space.
 */
enum class DownloadQuality(val label: String, val maxBitrate: Int) {
    LOW("Low (64 kbps) • Saves data", 64),
    MEDIUM("Medium (128 kbps)", 128),
    HIGH("High (256 kbps)", 256),
    BEST("Best available", Int.MAX_VALUE);
}

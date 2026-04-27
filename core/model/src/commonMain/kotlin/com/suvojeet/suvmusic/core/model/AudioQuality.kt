package com.suvojeet.suvmusic.core.model

/**
 * Audio quality settings for streaming.
 */
enum class AudioQuality(val label: String, val bitrateRange: IntRange) {
    AUTO("Auto (Adaptive)", 0..160),
    LOW("Low (48-64 kbps)", 0..70),
    MEDIUM("Normal (128 kbps)", 71..160),
    HIGH("Always High (256 kbps)", 161..512);

    companion object {
        fun fromBitrate(bitrate: Int): AudioQuality {
            // Check based on standard YouTube DASH itag bitrates
            return entries.find { bitrate in it.bitrateRange } ?: MEDIUM
        }
    }
}

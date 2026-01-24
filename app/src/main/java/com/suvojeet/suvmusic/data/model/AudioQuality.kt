package com.suvojeet.suvmusic.data.model

/**
 * Audio quality settings for streaming.
 */
enum class AudioQuality(val label: String, val bitrateRange: IntRange) {
    LOW("Low (64 kbps)", 0..64),
    MEDIUM("Medium (128 kbps)", 65..128),
    HIGH("High (256 kbps)", 129..256);
    
    companion object {
        fun fromBitrate(bitrate: Int): AudioQuality {
            return entries.find { bitrate in it.bitrateRange } ?: MEDIUM
        }
    }
}

package com.suvojeet.suvmusic.data.model

/**
 * Player state for UI updates.
 */
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPercentage: Int = 0,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val videoQuality: VideoQuality = VideoQuality.HIGH,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    val isAutoplayEnabled: Boolean = false,
    val isVideoMode: Boolean = false, // Video playback mode for YouTube songs
    val availableDevices: List<OutputDevice> = emptyList(),
    val selectedDevice: OutputDevice? = null,
    val playbackSpeed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val audioCodec: String? = null, // e.g., "opus", "aac", "mp3"
    val audioBitrate: Int? = null, // in kbps, e.g., 256
    val dominantColor: Int = -16777216, // Black/Dark default
    val videoNotFound: Boolean = false, // Flag for video stream failures
    val isRadioMode: Boolean = false // Radio mode flag
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    val hasNext: Boolean
        get() = currentIndex < queue.size - 1 || repeatMode == RepeatMode.ALL
    
    val hasPrevious: Boolean
        get() = currentIndex > 0 || repeatMode == RepeatMode.ALL
    
    /** Returns formatted audio format string like "Opus • 256kbps" */
    val audioFormatDisplay: String
        get() {
            val codec = audioCodec?.uppercase() ?: return "Unknown"
            val bitrate = audioBitrate?.let { "${it}kbps" } ?: ""
            return if (bitrate.isNotEmpty()) "$codec • $bitrate" else codec
        }
}

enum class DownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

/**
 * Repeat mode for playback.
 */
enum class RepeatMode {
    OFF,
    ALL,
    ONE
}
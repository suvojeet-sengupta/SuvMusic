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
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLiked: Boolean = false,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    val isAutoplayEnabled: Boolean = true,
    val isVideoMode: Boolean = false, // Video playback mode for YouTube songs
    val availableDevices: List<OutputDevice> = emptyList(),
    val selectedDevice: OutputDevice? = null
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    val hasNext: Boolean
        get() = currentIndex < queue.size - 1 || repeatMode == RepeatMode.ALL
    
    val hasPrevious: Boolean
        get() = currentIndex > 0 || repeatMode == RepeatMode.ALL
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
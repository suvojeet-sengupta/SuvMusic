package com.suvojeet.suvmusic.core.domain.player

import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Phase 4.0 STUB. Holds in-memory state, prints every method call,
 * does NOT delegate to VLCJ yet. The real Desktop player using VLCJ
 * lands in Phase 4.2.
 *
 * For now this exists so Desktop builds compile when commonMain code
 * starts referencing the MusicPlayer abstraction.
 */
actual class MusicPlayer {
    private val _currentSong = MutableStateFlow<Song?>(null)
    actual val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    actual val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    actual val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    actual val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    actual val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    actual val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    actual fun setQueue(songs: List<Song>, startIndex: Int) {
        log("setQueue size=${songs.size} startIndex=$startIndex")
        _currentSong.value = songs.getOrNull(startIndex)
    }

    actual fun play() {
        log("play()")
        _isPlaying.value = true
    }

    actual fun pause() {
        log("pause()")
        _isPlaying.value = false
    }

    actual fun togglePlayPause() {
        log("togglePlayPause() current=${_isPlaying.value}")
        _isPlaying.update { !it }
    }

    actual fun next() {
        log("next()")
    }

    actual fun previous() {
        log("previous()")
    }

    actual fun seekTo(positionMs: Long) {
        log("seekTo($positionMs)")
        _positionMs.value = positionMs
    }

    actual fun setRepeatMode(mode: RepeatMode) {
        log("setRepeatMode($mode)")
        _repeatMode.value = mode
    }

    actual fun setShuffleEnabled(enabled: Boolean) {
        log("setShuffleEnabled($enabled)")
        _shuffleEnabled.value = enabled
    }

    actual fun release() {
        log("release()")
    }

    private fun log(message: String) {
        println("[MusicPlayer.stub.desktop] $message")
    }
}

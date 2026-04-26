package com.suvojeet.suvmusic.core.domain.player

import android.util.Log
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Phase 4.0 STUB. Holds in-memory state, logs every method call, but
 * does NOT delegate to Media3/ExoPlayer yet. The existing rich Android
 * player at `:app/.../player/MusicPlayer.kt` continues to own all real
 * playback for now.
 *
 * Phase 4.1 will replace these stubs with delegation to the existing
 * MusicPlayer class. The wrapper pattern (rather than rewriting Media3
 * code from scratch) preserves all the spatial-audio / BT-autoplay /
 * MediaSession / audio-focus work already proven in production.
 *
 * To detect whether anything in the codebase accidentally consumes this
 * stub, every method logs to logcat with tag "MusicPlayer.stub". If you
 * see those logs while playback works fine on Android, that's the
 * existing :app player still doing the real work — the stub was called
 * but ignored.
 */
actual class MusicPlayer {
    actual val isAvailable: Boolean = true

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
        Log.d(TAG, "setQueue size=${songs.size} startIndex=$startIndex")
        _currentSong.value = songs.getOrNull(startIndex)
    }

    actual fun play() {
        Log.d(TAG, "play()")
        _isPlaying.value = true
    }

    actual fun pause() {
        Log.d(TAG, "pause()")
        _isPlaying.value = false
    }

    actual fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause() current=${_isPlaying.value}")
        _isPlaying.update { !it }
    }

    actual fun next() {
        Log.d(TAG, "next()")
    }

    actual fun previous() {
        Log.d(TAG, "previous()")
    }

    actual fun seekTo(positionMs: Long) {
        Log.d(TAG, "seekTo($positionMs)")
        _positionMs.value = positionMs
    }

    actual fun setRepeatMode(mode: RepeatMode) {
        Log.d(TAG, "setRepeatMode($mode)")
        _repeatMode.value = mode
    }

    actual fun setShuffleEnabled(enabled: Boolean) {
        Log.d(TAG, "setShuffleEnabled($enabled)")
        _shuffleEnabled.value = enabled
    }

    actual fun release() {
        Log.d(TAG, "release()")
    }

    private companion object {
        const val TAG = "MusicPlayer.stub"
    }
}

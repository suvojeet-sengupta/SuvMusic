package com.suvojeet.suvmusic.core.domain.player

import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-neutral music player abstraction. CommonMain code (UI screens
 * in Phase 5, Desktop entry point in Phase 4.x) controls playback through
 * this surface; the platform actual decides whether to delegate to
 * Media3 (Android) or VLCJ (Desktop).
 *
 * Phase 4.0 STATUS: this is the scaffolding interface only. Actual
 * implementations are stubs — no audio plays through these yet. Real
 * Android delegation lands in Phase 4.1 (wraps existing `:app/.../player/
 * MusicPlayer.kt`); Desktop VLCJ implementation lands in Phase 4.2.
 *
 * Design notes:
 * - State is exposed via [StateFlow]s so commonMain composables can
 *   observe changes idiomatically (`collectAsState()`).
 * - The interface is intentionally narrow — only the methods commonMain
 *   UI actually needs. The richer Android-specific API (spatial audio,
 *   audio focus, MediaSession, BT autoplay) stays in androidMain on the
 *   existing MusicPlayer class and is not exposed here.
 * - Repeat/shuffle modes are simple enums to avoid pulling Media3's
 *   constants into common code.
 */
expect class MusicPlayer {
    /**
     * Whether the underlying playback engine is functional. False on
     * Desktop when LibVLC isn't discoverable (no VLC installed on the
     * host); always true on Android. UI surfaces this so a missing-VLC
     * setup shows an install hint instead of a silent dead Play button.
     */
    val isAvailable: Boolean

    /** Currently loaded song, or null if the queue is empty. */
    val currentSong: StateFlow<Song?>

    /** True while audio is actively playing (not paused, not buffering). */
    val isPlaying: StateFlow<Boolean>

    /** Current playback position in milliseconds; updates ~10 Hz while playing. */
    val positionMs: StateFlow<Long>

    /** Reported duration of the current track in milliseconds, or 0 if unknown. */
    val durationMs: StateFlow<Long>

    /** Repeat behaviour for the current queue. */
    val repeatMode: StateFlow<RepeatMode>

    /** Whether the queue plays in shuffled order. */
    val shuffleEnabled: StateFlow<Boolean>

    fun setQueue(songs: List<Song>, startIndex: Int = 0)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleEnabled(enabled: Boolean)
    fun release()
}

enum class RepeatMode { OFF, ONE, ALL }

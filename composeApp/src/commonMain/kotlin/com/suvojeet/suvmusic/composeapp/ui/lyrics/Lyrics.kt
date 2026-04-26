package com.suvojeet.suvmusic.composeapp.ui.lyrics

/**
 * Lyrics data model — commonMain port of `app/.../providers/lyrics/Lyrics.kt`.
 *
 * Represents a song's lyrics in either synced (timestamps per line) or
 * unsynced (plain text) form. The Android side has a richer model with
 * provider attribution, error states, and word-level timings; this
 * commonMain shape covers the rendering surface — provider chain ports
 * will land in a follow-up round.
 *
 * @property lines synced lyric lines in chronological order. Empty when
 *   only [plain] text is available.
 * @property plain unsynced fallback text. Used when no provider returned
 *   timed lyrics.
 * @property source short label for the provider that produced this set.
 *   Shown in the lyrics tab footer when set.
 */
data class Lyrics(
    val lines: List<LyricLine> = emptyList(),
    val plain: String? = null,
    val source: String? = null,
) {
    /** Convenience — true when at least one timed line is available. */
    val isSynced: Boolean get() = lines.isNotEmpty()
}

/**
 * One timed lyric line. [timeMs] is the timestamp at which [text] should
 * become "current" during playback.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String,
)

/**
 * Where the lyrics text aligns inside the viewport.
 */
enum class LyricsTextPosition { LEFT, CENTER, RIGHT }

/**
 * Highlight animation style. Mirrors `app/.../providers/lyrics/LyricsAnimationType.kt`.
 */
enum class LyricsAnimationType { LINE, WORD, NONE }

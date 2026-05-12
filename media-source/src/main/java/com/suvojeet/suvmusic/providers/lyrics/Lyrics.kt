package com.suvojeet.suvmusic.providers.lyrics

data class Lyrics(
    val lines: List<LyricsLine>,
    val sourceCredit: String?,
    val isSynced: Boolean = false,
    val provider: LyricsProviderType = LyricsProviderType.AUTO
)

enum class LyricsProviderType(val displayName: String) {
    AUTO("Auto (Best Match)"),
    LRCLIB("LRCLIB (Synced)"),
    JIOSAAVN("JioSaavn"),
    YOUTUBE("YouTube (Captions)"),
    BETTER_LYRICS("Better Lyrics (Apple Music)"),
    SIMP_MUSIC("SimpMusic"),
    KUGOU("Kugou"),
    LOCAL("Local File")
}

data class LyricsLine(
    val text: String,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val isHeader: Boolean = false,
    val words: List<LyricsWord>? = null,
    // Optional translation/secondary line for bilingual lyrics (e.g. EN + JP).
    // Populated by providers that supply it, or auto-detected from LRC files
    // where two consecutive lines share the exact same start timestamp.
    val translation: String? = null
)

data class LyricsWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

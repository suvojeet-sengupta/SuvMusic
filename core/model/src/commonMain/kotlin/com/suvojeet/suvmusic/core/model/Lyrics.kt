package com.suvojeet.suvmusic.core.model

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
    val words: List<LyricsWord>? = null
)

data class LyricsWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

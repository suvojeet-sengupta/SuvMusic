package com.suvojeet.suvmusic.data.model

data class Lyrics(
    val lines: List<LyricsLine>,
    val sourceCredit: String?,
    val isSynced: Boolean = false
)

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

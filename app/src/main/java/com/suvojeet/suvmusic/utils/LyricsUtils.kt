package com.suvojeet.suvmusic.utils

import com.suvojeet.suvmusic.providers.lyrics.LyricsLine
import java.util.regex.Pattern

object LyricsUtils {
    // Regex matches [mm:ss.xx] or [mm:ss.xxx] followed by text
    private val LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")
    // Regex for BetterLyrics rich sync data: <word:start:end|...>
    private val RICH_SYNC_PATTERN = Pattern.compile("<(.*)>")

    fun parseLyrics(lrcContent: String): List<LyricsLine> {
        val lines = mutableListOf<LyricsLine>()
        if (lrcContent.isBlank()) return lines

        val lrcLines = lrcContent.split("\n")
        
        for (line in lrcLines) {
            val trimmedLine = line.trim()
            val matcher = LRC_PATTERN.matcher(trimmedLine)
            val richSyncMatcher = RICH_SYNC_PATTERN.matcher(trimmedLine)

            if (matcher.find()) {
                // It's a synced line
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0
                val fractionStr = matcher.group(3)
                val milliseconds = if (fractionStr != null) {
                    if (fractionStr.length == 2) fractionStr.toLong() * 10 else fractionStr.toLong()
                } else 0
                
                val text = matcher.group(4)?.trim() ?: ""
                val startTime = (minutes * 60 * 1000) + (seconds * 1000) + milliseconds
                
                // Create LyricsLine using the data class constructor
                lines.add(LyricsLine(text = text, startTimeMs = startTime))
            } else if (richSyncMatcher.find()) {
                // It's rich sync metadata for the previous line
                val content = richSyncMatcher.group(1)
                if (content != null && lines.isNotEmpty()) {
                    val lastLine = lines.removeAt(lines.lastIndex)
                    val words = parseRichSyncContent(content)
                    lines.add(lastLine.copy(words = words))
                }
            } else if (trimmedLine.isNotEmpty()) {
                // Header or plain line (metadata like [ar:Artist])
                // Filter out standard metadata tags
                if (!trimmedLine.startsWith("[") && !trimmedLine.endsWith("]")) {
                    lines.add(LyricsLine(text = trimmedLine))
                }
            }
        }
        
        return lines.sortedBy { it.startTimeMs }
    }

    private fun parseRichSyncContent(content: String): List<com.suvojeet.suvmusic.providers.lyrics.LyricsWord> {
        return try {
            content.split("|").mapNotNull { wordData ->
                val parts = wordData.split(":")
                if (parts.size >= 3) {
                    val text = parts[0]
                    val startSec = parts[1].toDoubleOrNull() ?: 0.0
                    val endSec = parts[2].toDoubleOrNull() ?: 0.0
                    com.suvojeet.suvmusic.providers.lyrics.LyricsWord(
                        text = text,
                        startTimeMs = (startSec * 1000).toLong(),
                        endTimeMs = (endSec * 1000).toLong()
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

package com.suvojeet.suvmusic.util

import com.suvojeet.suvmusic.providers.lyrics.LyricsLine
import com.suvojeet.suvmusic.providers.lyrics.LyricsWord
import java.util.regex.Pattern

/**
 * Robust parser for LRC v1 and Enhanced LRC ("LRC v2" / A2 extension).
 *
 * Supports:
 *  - Standard line timestamps: [mm:ss.xx] or [mm:ss.xxx] (centi- or milli-seconds)
 *  - Multi-timestamp prefix lines: [00:12.00][00:36.00]Chorus
 *  - Enhanced inline word timestamps: [00:12.00]<00:12.00>Hello <00:12.45>world
 *  - Legacy BetterLyrics sidecar lines: <word:start:end|word:start:end|...>
 *  - ID-tag metadata skipping: [ar:], [ti:], [length:], [offset:], …
 *  - Plain (unsynced) lyrics
 */
object LyricsUtils {

    // [mm:ss], [mm:ss.xx], [mm:ss.xxx], [mm:ss:xx]  — allow optional fractional part with . or : separator
    private val LINE_TIME_PATTERN: Pattern = Pattern.compile(
        "\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]"
    )

    // <mm:ss>, <mm:ss.xx>, <mm:ss.xxx> — inline word-start timestamps (enhanced LRC)
    private val WORD_TIME_PATTERN: Pattern = Pattern.compile(
        "<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>"
    )

    // Legacy BetterLyrics custom sidecar: entire line is <word:start:end|word:start:end|...>
    private val LEGACY_SIDECAR_PATTERN: Pattern = Pattern.compile(
        "^<([^<>]*:[0-9]+(?:\\.[0-9]+)?:[0-9]+(?:\\.[0-9]+)?(?:\\|[^<>]*:[0-9]+(?:\\.[0-9]+)?:[0-9]+(?:\\.[0-9]+)?)*)>$"
    )

    // Standard ID-tag metadata (anchored, full-line)
    private val ID_TAG_PATTERN: Pattern = Pattern.compile(
        "^\\[(ar|ti|al|au|by|length|offset|re|ve|tool|encoding|lang|t_time|id):[^]]*]$",
        Pattern.CASE_INSENSITIVE
    )

    fun parseLyrics(lrcContent: String): List<LyricsLine> {
        if (lrcContent.isBlank()) return emptyList()
        val out = mutableListOf<LyricsLine>()

        for (rawLine in lrcContent.split('\n')) {
            val line = rawLine.trim().trimStart('﻿')
            if (line.isEmpty()) continue

            if (ID_TAG_PATTERN.matcher(line).matches()) continue

            // Legacy custom rich-sync sidecar — attach words to previous line
            val sidecar = LEGACY_SIDECAR_PATTERN.matcher(line)
            if (sidecar.matches() && out.isNotEmpty()) {
                val payload = sidecar.group(1) ?: continue
                val words = parseLegacyRichSync(payload)
                if (words.isNotEmpty()) {
                    val last = out.removeAt(out.lastIndex)
                    out.add(last.copy(words = words))
                }
                continue
            }

            // Extract one or more leading [mm:ss.xx] line-start tags
            val starts = mutableListOf<Long>()
            val timeMatcher = LINE_TIME_PATTERN.matcher(line)
            var cursor = 0
            while (timeMatcher.find(cursor) && timeMatcher.start() == cursor) {
                starts += timeToMs(timeMatcher.group(1), timeMatcher.group(2), timeMatcher.group(3))
                cursor = timeMatcher.end()
            }

            if (starts.isEmpty()) {
                // No timestamps and not metadata — plain (unsynced) text line
                out.add(LyricsLine(text = line))
                continue
            }

            val body = line.substring(cursor)
            val anchor = starts.first()
            val (wordsAtAnchor, plainText) = parseInlineWords(body, anchor)

            for (start in starts) {
                if (wordsAtAnchor != null) {
                    val shifted = if (start == anchor) wordsAtAnchor
                    else {
                        val delta = start - anchor
                        wordsAtAnchor.map {
                            it.copy(
                                startTimeMs = it.startTimeMs + delta,
                                endTimeMs = it.endTimeMs + delta
                            )
                        }
                    }
                    out.add(LyricsLine(text = plainText, startTimeMs = start, words = shifted))
                } else {
                    out.add(LyricsLine(text = plainText, startTimeMs = start))
                }
            }
        }

        return finalize(out.sortedBy { it.startTimeMs })
    }

    /**
     * Parse the body of one synced line for inline <mm:ss.xx> word timestamps.
     * Returns (words, plainText). `words` is null when the body has no inline tags.
     * Any prose appearing before the first <ts> is treated as a leading word anchored at [lineStart].
     */
    private fun parseInlineWords(body: String, lineStart: Long): Pair<List<LyricsWord>?, String> {
        val matcher = WORD_TIME_PATTERN.matcher(body)
        if (!matcher.find()) return null to body.trim()

        matcher.reset()
        val words = mutableListOf<LyricsWord>()
        val plain = StringBuilder()
        var cursor = 0
        var pendingStart: Long = lineStart

        while (matcher.find()) {
            val chunk = body.substring(cursor, matcher.start())
            val nextStart = timeToMs(matcher.group(1), matcher.group(2), matcher.group(3))
            if (chunk.isNotEmpty()) {
                words.add(
                    LyricsWord(
                        text = chunk,
                        startTimeMs = pendingStart,
                        endTimeMs = nextStart
                    )
                )
                plain.append(chunk)
            }
            pendingStart = nextStart
            cursor = matcher.end()
        }
        val tail = body.substring(cursor)
        if (tail.isNotEmpty()) {
            words.add(
                LyricsWord(
                    text = tail,
                    startTimeMs = pendingStart,
                    endTimeMs = pendingStart
                )
            )
            plain.append(tail)
        }

        return (if (words.isEmpty()) null else words) to plain.toString().trim()
    }

    /**
     * Backfill missing end times so the UI never sees zero-length words/lines.
     */
    private fun finalize(lines: List<LyricsLine>): List<LyricsLine> {
        if (lines.isEmpty()) return lines
        val out = ArrayList<LyricsLine>(lines.size)
        for (i in lines.indices) {
            val cur = lines[i]
            val nextStart = if (i + 1 < lines.size) lines[i + 1].startTimeMs
                            else cur.startTimeMs + 5_000L
            val lineEnd = if (cur.endTimeMs > 0L) cur.endTimeMs else nextStart
            val fixedWords = cur.words?.let { ws ->
                ws.mapIndexed { idx, w ->
                    val end = when {
                        w.endTimeMs > w.startTimeMs -> w.endTimeMs
                        idx + 1 < ws.size -> ws[idx + 1].startTimeMs
                        else -> lineEnd
                    }
                    w.copy(endTimeMs = end)
                }
            }
            out.add(cur.copy(endTimeMs = lineEnd, words = fixedWords))
        }
        return out
    }

    private fun parseLegacyRichSync(content: String): List<LyricsWord> = try {
        content.split('|').mapNotNull { token ->
            // Tolerate ':' inside word text by splitting from the right
            val lastColon = token.lastIndexOf(':')
            if (lastColon <= 0) return@mapNotNull null
            val secondLastColon = token.lastIndexOf(':', lastColon - 1)
            if (secondLastColon <= 0) return@mapNotNull null
            val text = token.substring(0, secondLastColon)
            val startSec = token.substring(secondLastColon + 1, lastColon).toDoubleOrNull()
                ?: return@mapNotNull null
            val endSec = token.substring(lastColon + 1).toDoubleOrNull()
                ?: return@mapNotNull null
            LyricsWord(
                text = text,
                startTimeMs = (startSec * 1000).toLong(),
                endTimeMs = (endSec * 1000).toLong()
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun timeToMs(minStr: String?, secStr: String?, fracStr: String?): Long {
        val minutes = minStr?.toLongOrNull() ?: 0L
        val seconds = secStr?.toLongOrNull() ?: 0L
        val frac = if (!fracStr.isNullOrEmpty()) {
            when (fracStr.length) {
                1 -> fracStr.toLong() * 100
                2 -> fracStr.toLong() * 10
                else -> fracStr.take(3).toLong()
            }
        } else 0L
        return minutes * 60_000L + seconds * 1_000L + frac
    }

    /**
     * Serialize parsed lyrics back to canonical Enhanced LRC ("LRC v2") so the app
     * can export/share lyrics to SimpMusic, BetterMusic, and other compatible apps.
     *
     * When [enhanced] is true and a line has word-level timing, inline <mm:ss.xx>
     * tags are emitted per word and a trailing <end> marker closes the line.
     */
    fun serialize(lines: List<LyricsLine>, enhanced: Boolean = true): String {
        if (lines.isEmpty()) return ""
        val sb = StringBuilder()
        for (line in lines) {
            if (line.startTimeMs <= 0L && line.text.isBlank() && line.words.isNullOrEmpty()) continue
            sb.append('[').append(formatTime(line.startTimeMs)).append(']')
            val words = line.words
            if (enhanced && !words.isNullOrEmpty()) {
                for ((idx, w) in words.withIndex()) {
                    if (idx > 0 && !w.text.startsWith(' ')) sb.append(' ')
                    sb.append('<').append(formatTime(w.startTimeMs)).append('>').append(w.text.trim())
                }
                val tail = if (line.endTimeMs > 0L) line.endTimeMs
                           else words.last().endTimeMs
                if (tail > 0L) sb.append('<').append(formatTime(tail)).append('>')
            } else {
                sb.append(line.text)
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun formatTime(ms: Long): String {
        val safe = if (ms < 0L) 0L else ms
        val totalCs = safe / 10L
        val cs = totalCs % 100L
        val totalSec = totalCs / 100L
        val sec = totalSec % 60L
        val min = totalSec / 60L
        return "%02d:%02d.%02d".format(min, sec, cs)
    }
}

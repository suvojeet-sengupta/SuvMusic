package com.suvojeet.suvmusic.providers.lyrics

/**
 * Small dependency-free TTML parser shared by Android and the JVM desktop host.
 * It intentionally accepts the provider's media-time TTML subset and degrades
 * to an empty result for malformed input instead of crashing playback.
 */
object TTMLParser {
    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>,
    )

    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
    )

    private data class SpanInfo(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val boundaryAfter: Boolean,
    )

    private val paragraphRegex = Regex(
        "<p\\b([^>]*)>(.*?)</p\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val spanRegex = Regex(
        "<span\\b([^>]*)>(.*?)</span\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val attributeRegex = Regex("([A-Za-z_:][A-Za-z0-9_.:-]*)\\s*=\\s*\\\"([^\\\"]*)\\\"")

    fun parseTTML(ttml: String): List<ParsedLine> = runCatching {
        paragraphRegex.findAll(ttml).mapNotNull { paragraph ->
            val paragraphAttributes = attributesOf(paragraph.groupValues[1])
            val begin = paragraphAttributes["begin"].orEmpty()
            if (begin.isEmpty()) return@mapNotNull null

            val content = paragraph.groupValues[2]
            val timedSpans = spanRegex.findAll(content).mapNotNull { span ->
                val attrs = attributesOf(span.groupValues[1])
                val wordBegin = attrs["begin"].orEmpty()
                val wordEnd = attrs["end"].orEmpty()
                if (wordBegin.isEmpty() || wordEnd.isEmpty()) return@mapNotNull null

                val after = content.substring(span.range.last + 1)
                val nextSpan = spanRegex.find(after)
                val between = nextSpan?.let { after.substring(0, it.range.first) } ?: after
                SpanInfo(
                    text = decodeEntities(stripTags(span.groupValues[2])),
                    startTime = parseTime(wordBegin),
                    endTime = parseTime(wordEnd),
                    boundaryAfter = between.any(Char::isWhitespace) ||
                        (nextSpan != null && stripTags(nextSpan.groupValues[2]).isBlank()),
                )
            }.toList()

            val words = mergeSpansIntoWords(timedSpans)
            val fallbackText = decodeEntities(stripTags(content)).trim().replace(Regex("\\s+"), " ")
            val lineText = if (words.isEmpty()) fallbackText else words.joinToString(" ") { it.text }
            if (lineText.isEmpty()) null else ParsedLine(lineText, parseTime(begin), words)
        }.toList()
    }.getOrDefault(emptyList())

    private fun mergeSpansIntoWords(spans: List<SpanInfo>): List<ParsedWord> {
        if (spans.isEmpty()) return emptyList()
        val words = mutableListOf<ParsedWord>()
        var text = buildString { append(spans.first().text) }
        var start = spans.first().startTime
        var end = spans.first().endTime

        spans.drop(1).forEachIndexed { index, span ->
            val previous = spans[index]
            if (previous.boundaryAfter) {
                words += ParsedWord(text.trim(), start, end)
                text = span.text
                start = span.startTime
            } else {
                text += span.text
            }
            end = span.endTime
        }
        if (text.isNotBlank()) words += ParsedWord(text.trim(), start, end)
        return words
    }

    fun toLRC(lines: List<ParsedLine>): String = buildString {
        lines.forEach { line ->
            val timeMs = (line.startTime * 1000).toLong()
            val minutes = timeMs / 60000
            val seconds = (timeMs % 60000) / 1000
            val centiseconds = (timeMs % 1000) / 10
            append('[')
            append(minutes.toString().padStart(2, '0'))
            append(':')
            append(seconds.toString().padStart(2, '0'))
            append('.')
            append(centiseconds.toString().padStart(2, '0'))
            append(']')
            appendLine(line.text)
            if (line.words.isNotEmpty()) {
                appendLine(line.words.joinToString("|") { "${it.text}:${it.startTime}:${it.endTime}" }.let { "<$it>" })
            }
        }
    }

    private fun attributesOf(raw: String): Map<String, String> =
        attributeRegex.findAll(raw).associate { it.groupValues[1].lowercase() to it.groupValues[2] }

    private fun parseTime(value: String): Double = runCatching {
        val parts = value.split(':')
        when (parts.size) {
            2 -> parts[0].toDouble() * 60 + parts[1].toDouble()
            3 -> parts[0].toDouble() * 3600 + parts[1].toDouble() * 60 + parts[2].toDouble()
            else -> value.toDouble()
        }
    }.getOrDefault(0.0)

    private fun stripTags(value: String): String = value.replace(Regex("<[^>]+>"), "")

    private fun decodeEntities(value: String): String = value
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")

}

package com.suvojeet.suvmusic.util

/**
 * Cleans raw YouTube video titles into music-app friendly song titles and
 * provides a normalized fingerprint so different uploads of the same song
 * (e.g. "Ghar Kab Aaoge | BORDER 2 | Full Video" vs "Ghar Kab Aaoge")
 * can be recognized as duplicates.
 */
object TitleSanitizer {

    // Bracketed/parenthesized qualifiers that are upload noise, not part of the song name.
    private val NOISE_BRACKETS = Regex(
        "[(\\[][^)\\]]*(official|full|lyrical|lyrics?|video|audio|visualizer|visualiser|hd|4k|remaster(ed)?|" +
            "color coded|colour coded|mv|m/v|teaser|trailer|promo|out now|new song|latest|version)[^)\\]]*[)\\]]",
        RegexOption.IGNORE_CASE
    )

    // Trailing qualifiers after the song name separated by |, ||, - etc.
    private val NOISE_SEGMENT = Regex(
        "^(official|full|lyrical|lyrics?|video|audio|song|new|latest|hd|4k|out now|" +
            "official (music )?video|official audio|full (video|audio|song)|video song|audio song|title track).*$",
        RegexOption.IGNORE_CASE
    )

    private val SEPARATORS = Regex("\\s*(\\|\\||\\||•|–|—)\\s*")
    private val WHITESPACE = Regex("\\s+")
    private val FINGERPRINT_STRIP = Regex("[^a-z0-9\\u0900-\\u097F\\u0980-\\u09FF]")

    /**
     * Returns a display-friendly title: strips bracketed upload noise and drops
     * pipe-separated segments that are pure qualifiers ("Full Video", "Official Audio")
     * or that just repeat the artist/movie name. Never returns an empty string —
     * falls back to the original title if cleaning removes everything.
     */
    fun clean(rawTitle: String, artist: String? = null): String {
        if (rawTitle.isBlank()) return rawTitle

        var title = rawTitle.replace(NOISE_BRACKETS, " ")

        val segments = title.split(SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.size > 1) {
            val artistKey = artist?.trim()?.lowercase()
            val kept = segments.filterIndexed { index, segment ->
                if (index == 0) return@filterIndexed true // first segment is the song name
                val lower = segment.lowercase()
                !NOISE_SEGMENT.matches(segment) &&
                    (artistKey == null || lower != artistKey) &&
                    !lower.contains("official") && !lower.contains("video") && !lower.contains("audio")
            }
            // Keep at most the song name + one contextual segment (e.g. movie name)
            title = kept.take(2).joinToString(" · ")
        }

        title = title.replace(WHITESPACE, " ").trim().trim('-', '•', '·', ',').trim()
        return title.ifBlank { rawTitle.trim() }
    }

    /**
     * Normalized fingerprint of the *song name only* (first segment, noise stripped,
     * Latin + Devanagari + Bengali letters kept) — used for deduplication across uploads.
     */
    fun fingerprint(rawTitle: String, artist: String = ""): String {
        val core = clean(rawTitle).split(" · ").first().lowercase().replace(FINGERPRINT_STRIP, "")
        val artistKey = artist.lowercase().replace(FINGERPRINT_STRIP, "")
        return "$core|$artistKey"
    }
}

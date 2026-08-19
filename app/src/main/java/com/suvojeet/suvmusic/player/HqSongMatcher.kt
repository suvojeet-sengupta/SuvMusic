package com.suvojeet.suvmusic.player

import com.suvojeet.suvmusic.core.model.Song
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Picks the HQ Audio catalogue entry that is the *same recording* as a YouTube song.
 *
 * The catalogue routinely holds a dozen or more songs with an identical title (film
 * originals, covers, single releases, female/male versions), so a title match on its own
 * is the easiest way to play the wrong song. Every candidate has to be corroborated by
 * something beyond the title — the credited artists, the album/film, or the duration —
 * and the rules for which corroboration is enough depend on how trustworthy the YouTube
 * metadata is (song entries carry real artists; video entries carry channel names).
 *
 * Tokens are compared with a transliteration-tolerant equality so the many romanisation
 * variants of Indic titles ("Kichhu"/"Kichu", "Kotha"/"Katha", "Ghoshal"/"Ghosal") count
 * as the same word.
 */
internal object HqSongMatcher {

    /** Both durations known and further apart than this → different cut. */
    const val DURATION_WINDOW_MS = 15_000L

    /** Duration this close is treated as "the same recording" when other evidence is thin. */
    private const val TIGHT_DURATION_MS = 5_000L

    private const val TITLE_FLOOR = 0.6
    private const val ARTIST_WEIGHT = 0.5
    private const val ALBUM_WEIGHT = 0.3
    private const val LABEL_WEIGHT = 0.1
    private const val DURATION_WEIGHT = 0.15
    private const val POPULARITY_WEIGHT = 0.1
    private const val SOFT_TAG_PENALTY = 0.25

    data class Verdict(val song: Song, val score: Double, val reason: String)

    fun pickBest(target: Song, candidates: List<Song>): Song? =
        rank(target, candidates).firstOrNull()?.song

    fun rank(target: Song, candidates: List<Song>): List<Verdict> {
        if (candidates.isEmpty()) return emptyList()
        val t = TargetProfile.of(target)
        if (t.titleTokens.isEmpty()) return emptyList()
        return candidates.mapNotNull { evaluate(t, it) }.sortedByDescending { it.score }
    }

    /** One-line explanation of why [candidate] was or wasn't accepted for [target]. */
    fun explain(target: Song, candidate: Song): String {
        val t = TargetProfile.of(target)
        if (t.titleTokens.isEmpty()) return "target title empty"
        return evaluate(t, candidate)?.reason ?: rejectionReason(t, candidate)
    }

    // ── Target profile ─────────────────────────────────────────────────────────────

    private class TargetProfile(
        val titleTokens: Set<String>,
        val hardTags: Set<String>,
        val softTags: Set<String>,
        val artistNames: List<Set<String>>,
        val labelTokens: Set<String>,
        val albumTokens: Set<String>,
        val durationMs: Long,
        val isVideo: Boolean
    ) {
        /** True when the YouTube artist field holds real artist names (not a label/channel). */
        val artistKnown: Boolean get() = artistNames.isNotEmpty()

        companion object {
            fun of(song: Song): TargetProfile {
                val (mainTitle, hints) = splitTitleSegments(song.title)
                val titleTokens = titleTokensOf(mainTitle)
                val albumTokens = informativeAlbumTokens(song.album, hints, song.title, titleTokens)
                val artistRaw = cleanArtistString(song.artist)
                val artistIsLabel = looksLikeLabel(artistRaw)
                return TargetProfile(
                    titleTokens = titleTokens,
                    hardTags = hardTagsOf(song.title),
                    softTags = softTagsOf(song.title),
                    artistNames = if (artistIsLabel) emptyList() else artistNamesOf(artistRaw),
                    labelTokens = if (artistIsLabel) labelTokensOf(artistRaw) else emptySet(),
                    albumTokens = albumTokens,
                    durationMs = song.duration,
                    isVideo = song.isVideo
                )
            }
        }
    }

    // ── Evaluation ─────────────────────────────────────────────────────────────────

    private fun evaluate(t: TargetProfile, c: Song): Verdict? {
        val (cMainTitle, cHints) = splitTitleSegments(c.title)
        val cTitle = titleTokensOf(cMainTitle)
        if (cTitle.isEmpty()) return null

        val title = fuzzySetScore(t.titleTokens, cTitle)
        if (title.recall < 0.5 || title.precision < 0.5 || title.f1 < TITLE_FLOOR) return null

        // A remix / lofi / instrumental / cover / other-language cut on either side that
        // the other lacks is a different recording no matter how well the rest lines up.
        if (hardTagsOf(c.title) != t.hardTags) return null

        var durationKnown = false
        var durationDiff = Long.MAX_VALUE
        var durBonus = 0.0
        if (t.durationMs > 0 && c.duration > 0) {
            durationKnown = true
            durationDiff = abs(t.durationMs - c.duration)
            if (durationDiff > DURATION_WINDOW_MS) return null
            durBonus = (1.0 - durationDiff.toDouble() / DURATION_WINDOW_MS) * DURATION_WEIGHT
        }

        val artistScore = if (t.artistKnown) artistOverlap(t.artistNames, creditedNames(c)) else 0.0
        val labelMatch = t.labelTokens.isNotEmpty() &&
            fuzzySetScore(t.labelTokens, labelTokensOf(c.remoteAudioMetadata?.label.orEmpty())).recall >= 0.5

        val cAlbum = informativeAlbumTokens(c.album, cHints, c.title, cTitle)
        val albumScore = if (t.albumTokens.isNotEmpty() && cAlbum.isNotEmpty()) {
            fuzzySetScore(t.albumTokens, cAlbum).let { max(it.recall, it.precision) }.takeIf { it >= 0.5 } ?: 0.0
        } else 0.0
        val albumMatch = albumScore >= 0.5
        val tightDuration = durationKnown && durationDiff <= TIGHT_DURATION_MS

        val corroborated = when {
            // Real artist in common. Without a duration to confirm, demand a tighter title so
            // two different songs by one artist with overlapping names don't merge.
            artistScore > 0.0 -> durationKnown || title.f1 >= 0.8
            // Real artist on both sides and nothing shared → almost always a cover or a
            // different rendition. Only the album/film naming the same release overrides it,
            // or — for a video whose artist field is often a channel — an exact title
            // with a near-identical duration.
            t.artistKnown -> (albumMatch && title.f1 >= 0.8) ||
                (t.isVideo && tightDuration && title.f1 >= 0.9)
            // Artist unknown or a label/channel: the title alone is never enough.
            else -> (durationKnown && (title.f1 >= 0.9 || ((albumMatch || labelMatch) && title.f1 >= 0.75))) ||
                (albumMatch && title.f1 >= 0.9)
        }
        if (!corroborated) return null

        // Female/male/duet/reprise style tags are applied inconsistently between catalogues,
        // so a mismatch is only a penalty — and none at all when the artist and a tight
        // duration say it is the same take.
        val softMismatch = softTagsOf(c.title) != t.softTags
        val softPenalty = when {
            !softMismatch -> 0.0
            artistScore >= 0.5 && tightDuration -> 0.0
            else -> SOFT_TAG_PENALTY
        }

        val popBonus = popularityBonus(c.remoteAudioMetadata?.playCount)
        val score = title.f1 +
            ARTIST_WEIGHT * artistScore +
            ALBUM_WEIGHT * albumScore +
            (if (labelMatch) LABEL_WEIGHT else 0.0) +
            durBonus + popBonus - softPenalty
        if (score < TITLE_FLOOR) return null

        val reason = buildString {
            append("title=").append(fmt(title.f1))
            append(" artist=").append(fmt(artistScore))
            append(" album=").append(fmt(albumScore))
            if (labelMatch) append(" label")
            append(" dur=").append(if (durationKnown) "${durationDiff / 1000}s" else "?")
            append(" pop=").append(fmt(popBonus))
            if (softPenalty > 0) append(" softTag-").append(fmt(softPenalty))
            append(" → ").append(fmt(score))
        }
        return Verdict(c, score, reason)
    }

    private fun rejectionReason(t: TargetProfile, c: Song): String {
        val cTitle = titleTokensOf(splitTitleSegments(c.title).first)
        if (cTitle.isEmpty()) return "candidate title empty"
        val title = fuzzySetScore(t.titleTokens, cTitle)
        if (title.recall < 0.5 || title.precision < 0.5 || title.f1 < TITLE_FLOOR) return "title ${fmt(title.f1)}"
        if (hardTagsOf(c.title) != t.hardTags) return "variant tags ${t.hardTags} vs ${hardTagsOf(c.title)}"
        if (t.durationMs > 0 && c.duration > 0 && abs(t.durationMs - c.duration) > DURATION_WINDOW_MS) {
            return "duration ${abs(t.durationMs - c.duration) / 1000}s apart"
        }
        return "no corroboration (artist/album/duration)"
    }

    private fun fmt(d: Double) = ((d * 100).toInt() / 100.0).toString()

    // ── Artist handling ────────────────────────────────────────────────────────────

    /** Every name the catalogue credits on [c]: the primary string plus the full credit list. */
    private fun creditedNames(c: Song): List<Set<String>> {
        val names = ArrayList<Set<String>>()
        names += artistNamesOf(c.artist)
        c.remoteAudioMetadata?.artists?.forEach { credit ->
            if (!credit.role.equals("starring", ignoreCase = true)) names += artistNamesOf(credit.name)
        }
        return names.filter { it.isNotEmpty() }.distinct()
    }

    /**
     * Fraction of the target's artist names that appear among [credited]. Two names match
     * when their tokens largely agree *and* share something more specific than a common
     * surname — "Arijit Singh" must not match "Jaspinder Singh".
     */
    private fun artistOverlap(targetNames: List<Set<String>>, credited: List<Set<String>>): Double {
        if (targetNames.isEmpty() || credited.isEmpty()) return 0.0
        val matched = targetNames.count { name -> credited.any { namesMatch(name, it) } }
        return matched.toDouble() / targetNames.size
    }

    private fun namesMatch(a: Set<String>, b: Set<String>): Boolean {
        if (a == b) return true
        val s = fuzzySetScore(a, b)
        if (s.f1 < 0.6) return false
        return s.pairs.any { (x, y) -> x !in COMMON_SURNAMES && y !in COMMON_SURNAMES }
    }

    private val ARTIST_SPLIT = Regex(
        "(?i)\\s*(?:,|&|/|;|\\+|\\bx\\b|\\bfeat\\.?|\\bft\\.?|\\bfeaturing\\b|\\bwith\\b|\\band\\b)\\s*"
    )

    private fun artistNamesOf(raw: String): List<Set<String>> =
        raw.split(ARTIST_SPLIT)
            .map { wordTokens(it) - GENERIC_ARTIST_WORDS }
            .filter { it.isNotEmpty() }

    private fun cleanArtistString(artist: String): String =
        artist.replace(Regex("(?i)\\s*-\\s*topic\\b"), " ")
            .replace(Regex("\\(.*?\\)|\\[.*?]|\\{.*?}"), " ")
            .trim()

    /** "Various Artists", "T-Series", "Zee Music Company", "SVF" — not a person. */
    private fun looksLikeLabel(artist: String): Boolean {
        val tokens = wordTokens(artist) - GENERIC_ARTIST_WORDS
        if (tokens.isEmpty()) return true
        return tokens.any { it in LABEL_WORDS }
    }

    private fun labelTokensOf(label: String): Set<String> =
        wordTokens(label) - GENERIC_ARTIST_WORDS - LABEL_GENERIC_WORDS

    // ── Album handling ─────────────────────────────────────────────────────────────

    /**
     * Album tokens worth comparing. A single whose "album" is just the song's own title
     * says nothing about which recording it is, so those collapse to empty.
     */
    private fun informativeAlbumTokens(
        album: String?,
        titleHints: List<String>,
        rawTitle: String,
        titleTokens: Set<String>
    ): Set<String> {
        val tokens = albumTokensOf(album) +
            titleHints.flatMap { albumTokensOf(it) } +
            (fromAlbumHint(rawTitle)?.let { albumTokensOf(it) } ?: emptySet())
        if (tokens.isEmpty()) return emptySet()
        val distinct = tokens.filter { tok -> titleTokens.none { tokenSimilarity(tok, it) > 0.0 } }
        return if (distinct.isEmpty()) emptySet() else tokens
    }

    private fun albumTokensOf(album: String?): Set<String> {
        if (album.isNullOrBlank()) return emptySet()
        val stripped = album.replace(Regex("\\(.*?\\)|\\[.*?]"), " ")
        return wordTokens(stripped) - ALBUM_NOISE
    }

    /** `Song (From "Film")` / `Song [from Film]` → `Film`. */
    private fun fromAlbumHint(title: String): String? {
        val m = Regex("(?i)[(\\[]\\s*from\\s+[\"“]?([^)\\]\"”]+)[\"”]?\\s*[)\\]]").find(title) ?: return null
        return m.groupValues[1].trim().ifBlank { null }
    }

    // ── Title handling ─────────────────────────────────────────────────────────────

    private val SEGMENT_SPLIT = Regex("\\s+(?:·|\\|\\||\\||•|–|—|-|/)\\s+")

    /** First segment is the song name; the rest ("· Movie", "| Film") are album hints. */
    private fun splitTitleSegments(title: String): Pair<String, List<String>> {
        val parts = title.split(SEGMENT_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return title to emptyList()
        return parts.first() to parts.drop(1)
    }

    private fun titleTokensOf(title: String): Set<String> {
        val stripped = title.replace(Regex("\\(.*?\\)|\\[.*?]|\\{.*?}"), " ")
        return wordTokens(stripped) - TITLE_NOISE
    }

    private fun tagTokens(title: String): Set<String> =
        wordTokens(title.replace(Regex("(?i)\\blo[\\s-]?fi\\b"), "lofi").replace(Regex("(?i)\\bre-?mix\\b"), "remix"))

    private fun hardTagsOf(title: String): Set<String> = tagTokens(title).mapNotNull { HARD_TAGS[it] }.toSet()

    private fun softTagsOf(title: String): Set<String> = tagTokens(title).mapNotNull { SOFT_TAGS[it] }.toSet()

    // ── Tokens & fuzzy comparison ──────────────────────────────────────────────────

    private fun wordTokens(s: String): Set<String> =
        s.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && (it.length > 1 || it.any { ch -> ch.isDigit() }) }
            .toSet()

    private class SetScore(val recall: Double, val precision: Double, val pairs: List<Pair<String, String>>) {
        val f1: Double get() = if (recall + precision == 0.0) 0.0 else 2 * recall * precision / (recall + precision)
    }

    /** One-to-one greedy assignment of fuzzy-equal tokens between two sets. */
    private fun fuzzySetScore(a: Set<String>, b: Set<String>): SetScore {
        if (a.isEmpty() || b.isEmpty()) return SetScore(0.0, 0.0, emptyList())
        val pairs = ArrayList<Triple<String, String, Double>>()
        for (x in a) for (y in b) {
            val sim = tokenSimilarity(x, y)
            if (sim > 0.0) pairs += Triple(x, y, sim)
        }
        pairs.sortByDescending { it.third }
        val usedA = HashSet<String>()
        val usedB = HashSet<String>()
        var total = 0.0
        val matched = ArrayList<Pair<String, String>>()
        for ((x, y, sim) in pairs) {
            if (x in usedA || y in usedB) continue
            usedA += x; usedB += y
            total += sim
            matched += x to y
        }
        return SetScore(total / a.size, total / b.size, matched)
    }

    /** 1.0 exact, 0.95 same after transliteration folding, 0.85 near-identical spelling, else 0. */
    internal fun tokenSimilarity(x: String, y: String): Double {
        if (x == y) return 1.0
        val px = phonetic(x)
        val py = phonetic(y)
        if (px == py) return 0.95
        if (min(x.length, y.length) < 3) return 0.0
        val phoneticSim = 1.0 - levenshtein(px, py).toDouble() / max(px.length, py.length)
        if (phoneticSim >= 0.75) return 0.85
        val longest = max(x.length, y.length)
        return if (longest >= 6 && 1.0 - levenshtein(x, y).toDouble() / longest >= 0.8) 0.85 else 0.0
    }

    /** Folds the usual romanisation choices for Indic words onto one spelling. */
    internal fun phonetic(t: String): String {
        var s = t
        s = s.replace("chh", "ch").replace("kh", "k").replace("gh", "g").replace("th", "t")
            .replace("dh", "d").replace("bh", "b").replace("ph", "f").replace("sh", "s")
            .replace("jh", "j").replace("zh", "j").replace("ck", "k")
        s = s.replace("aa", "a").replace("ee", "i").replace("ii", "i").replace("oo", "u").replace("uu", "u")
        s = s.replace('w', 'v').replace('z', 'j').replace('q', 'k')
        s = s.replace(Regex("(.)\\1+"), "$1")
        return s
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[b.length]
    }

    private fun popularityBonus(playCount: Long?): Double {
        if (playCount == null || playCount <= 0) return 0.0
        return min(POPULARITY_WEIGHT, log10(playCount.toDouble() + 1) / 70.0)
    }

    // ── Vocabulary ─────────────────────────────────────────────────────────────────

    private val TITLE_NOISE = setOf(
        "official", "video", "audio", "lyrics", "lyric", "lyrical", "full", "song", "songs",
        "hd", "4k", "mv", "feat", "ft", "with", "the", "remastered", "version",
        "original", "soundtrack", "ost", "from", "movie", "film", "title", "track",
        "latest", "hits", "visualizer", "visualiser", "presents"
    )

    private val ALBUM_NOISE = setOf(
        "original", "motion", "picture", "soundtrack", "ost", "from", "the", "album",
        "single", "ep", "deluxe", "edition", "various", "artists", "hits", "best", "of",
        "vol", "volume", "remastered", "version", "songs", "song", "movie", "film",
        "music", "official", "video", "audio", "presents", "collection", "greatest",
        "top", "superhit", "superhits", "jukebox", "mix", "playlist", "series"
    )

    private val GENERIC_ARTIST_WORDS = setOf(
        "various", "artists", "artist", "unknown", "feat", "ft", "featuring", "topic",
        "official", "vevo", "and", "the", "with", "presents"
    )

    /** Words that only ever appear in the name of a label, channel or company. */
    private val LABEL_WORDS = setOf(
        "records", "record", "music", "films", "film", "entertainment", "series", "company",
        "productions", "production", "studio", "studios", "digital", "media", "network",
        "movies", "pictures", "tv", "label", "channel", "cassettes", "cassette", "industries",
        "gaana", "saregama", "tips", "zee", "sony", "universal", "warner", "eros", "venus",
        "svf", "eskay", "jalsha", "shemaroo", "ultra", "yrf", "musicals", "creations", "beats"
    )

    /** Dropped before comparing two label names so "Times Music" ≠ "Zee Music". */
    private val LABEL_GENERIC_WORDS = setOf(
        "music", "records", "record", "company", "entertainment", "india", "ltd", "limited",
        "pvt", "private", "digital", "media", "official", "label", "the", "group", "inc"
    )

    private val COMMON_SURNAMES = setOf(
        "singh", "kumar", "khan", "roy", "das", "sen", "ghosh", "sharma", "chakraborty",
        "dutta", "mukherjee", "banerjee", "bhattacharya", "chatterjee", "sarkar", "biswas",
        "mondal", "mandal", "saha", "paul", "pal", "dey", "de", "bose", "basu", "nath",
        "ali", "ahmed", "hossain", "islam", "rahman", "chowdhury", "choudhury", "verma",
        "gupta", "yadav", "mishra", "shukla", "pandey", "tiwari", "jha", "rao", "reddy",
        "nair", "menon", "iyer", "pillai", "patel", "shah", "mehta", "kapoor", "khanna",
        "malhotra", "kaur", "gill", "sandhu", "dhillon", "bajwa", "sidhu", "brar", "grewal",
        "mr", "ms", "dr", "sir", "singer", "band", "group", "boys", "girls"
    )

    /** Tags that mark a different recording; a mismatch rejects outright. */
    private val HARD_TAGS: Map<String, String> = mapOf(
        "remix" to "remix", "remixed" to "remix", "mix" to "remix", "dj" to "remix", "club" to "remix",
        "jhankar" to "remix", "dholki" to "remix", "trap" to "remix", "edm" to "remix",
        "lofi" to "lofi",
        "slowed" to "slowed", "reverb" to "slowed",
        "sped" to "sped", "nightcore" to "sped",
        "instrumental" to "instrumental", "karaoke" to "instrumental", "bgm" to "instrumental",
        "cover" to "cover", "covered" to "cover",
        "unplugged" to "unplugged", "acoustic" to "unplugged",
        "mashup" to "mashup", "medley" to "mashup",
        "live" to "live", "concert" to "live",
        "8d" to "8d", "3d" to "8d",
        "extended" to "extended", "edit" to "extended",
        "arabic" to "lang", "tamil" to "lang", "telugu" to "lang", "hindi" to "lang",
        "bengali" to "lang", "bangla" to "lang", "punjabi" to "lang", "marathi" to "lang",
        "malayalam" to "lang", "kannada" to "lang", "gujarati" to "lang", "english" to "lang"
    )

    /** Tags that catalogues apply inconsistently; a mismatch is only penalised. */
    private val SOFT_TAGS: Map<String, String> = mapOf(
        "female" to "female", "male" to "male", "duet" to "duet",
        "reprise" to "reprise", "revisited" to "reprise", "redux" to "reprise", "encore" to "reprise",
        "sad" to "sad", "solo" to "solo"
    )
}

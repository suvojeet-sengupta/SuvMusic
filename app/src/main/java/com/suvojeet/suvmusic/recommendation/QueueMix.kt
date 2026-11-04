package com.suvojeet.suvmusic.recommendation

import com.suvojeet.suvmusic.core.model.Song

enum class QueueMix(val label: String) {
    ALL("All"),
    FAMILIAR("Familiar"),
    POPULAR("Popular"),
    DISCOVER("Discover"),
    DEEP_CUTS("Deep cuts"),
}

internal data class QueueMixSources(
    val seedArtist: String,
    val related: List<Song> = emptyList(),
    val artistTop: List<Song> = emptyList(),
    val artistCatalog: List<Song> = emptyList(),
    val history: List<Song> = emptyList(),
    val playedIds: Set<String> = emptySet(),
    val knownArtists: Set<String> = emptySet(),
)

internal object QueueMixPolicy {
    /** The part of a credit like "A, B & C" that names the lead artist. */
    fun primaryArtist(artist: String): String =
        artist.split(",", "&", " x ", " feat", " ft.").first().trim()

    /** Candidates in priority order. [QueueMix.ALL] has none: it means the untuned queue. */
    fun select(mix: QueueMix, sources: QueueMixSources): List<Song> = with(sources) {
        val isKnown = { song: Song -> normalized(song.artist) in knownArtists || normalized(primaryArtist(song.artist)) in knownArtists }
        when (mix) {
            QueueMix.ALL -> emptyList()
            QueueMix.FAMILIAR -> related.filter { it.id in playedIds || isKnown(it) } + history
            QueueMix.POPULAR -> artistTop + related
            QueueMix.DISCOVER -> {
                val unplayed = related.filter { it.id !in playedIds }
                unplayed.filterNot(isKnown).ifEmpty { unplayed }
            }
            QueueMix.DEEP_CUTS -> {
                val hits = artistTop.mapTo(HashSet()) { it.id }
                artistCatalog.filter {
                    it.id !in hits && it.id !in playedIds && it.artist.contains(seedArtist, ignoreCase = true)
                }
            }
        }
    }

    fun normalized(artist: String): String = artist.trim().lowercase()
}

package com.suvojeet.suvmusic.recommendation

import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Spotify-Wrapped-style statistics generator.
 *
 * Aggregates [ListeningHistoryDao] over a requested time window into a self-contained
 * [WrappedReport] suitable for story-card rendering. No network calls.
 */
@Singleton
class WrappedGenerator @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao
) {

    suspend fun generate(window: WrappedWindow = WrappedWindow.LAST_365_DAYS): WrappedReport =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val startMs = when (window) {
                WrappedWindow.LAST_30_DAYS -> now - 30L * 24 * 60 * 60 * 1000
                WrappedWindow.LAST_365_DAYS -> now - 365L * 24 * 60 * 60 * 1000
                WrappedWindow.THIS_YEAR -> {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.DAY_OF_YEAR, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    cal.timeInMillis
                }
            }

            val allHistory = listeningHistoryDao.getAllHistory()
            val relevant = allHistory.filter { it.lastPlayed >= startMs }

            if (relevant.isEmpty()) {
                return@withContext WrappedReport.empty(window)
            }

            val topSongs = relevant
                .groupBy { "${it.songTitle.trim().lowercase()}|${primaryArtist(it.artist).lowercase()}" }
                .map { (_, dupes) ->
                    val best = dupes.maxBy { it.playCount }
                    best.copy(
                        playCount = dupes.sumOf { it.playCount },
                        totalDurationMs = dupes.sumOf { it.totalDurationMs },
                        thumbnailUrl = dupes.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
                    )
                }
                .sortedWith(compareByDescending<ListeningHistory> { it.playCount }.thenByDescending { it.totalDurationMs })
                .take(5)
            val topArtists = relevant
                .filter { it.artist.isNotBlank() && !it.artist.equals("Unknown Artist", ignoreCase = true) }
                .groupBy { primaryArtist(it.artist).lowercase() }
                .map { (_, plays) ->
                    TopArtist(
                        name = plays.groupingBy { primaryArtist(it.artist) }.eachCount().maxBy { it.value }.key,
                        totalPlays = plays.sumOf { it.playCount },
                        thumbnailUrl = plays.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
                    )
                }
                .sortedByDescending { it.totalPlays }
                .take(5)

            val totalMinutes = relevant.sumOf { it.totalDurationMs } / 60_000L
            val totalSongs = relevant.size
            val totalPlays = relevant.sumOf { it.playCount }

            val mostSkipped = relevant
                .filter { it.skipCount >= 2 }
                .maxByOrNull { it.skipCount }

            // Top genre derived from inferred vectors of each song (aggregated).
            val genreTally = FloatArray(GenreTaxonomy.GENRE_COUNT)
            relevant.forEach { h ->
                val v = GenreTaxonomy.inferGenreVector(h.songTitle, h.artist)
                for (i in v.indices) genreTally[i] += v[i] * max(1, h.playCount)
            }
            val topGenre = GenreTaxonomy.topGenres(genreTally, 1).firstOrNull()?.first ?: "eclectic"

            WrappedReport(
                window = window,
                generatedAt = now,
                totalSongs = totalSongs,
                totalPlays = totalPlays,
                totalMinutesListened = totalMinutes,
                topGenre = topGenre,
                topSongs = topSongs.map { it.toCardSong() },
                topArtists = topArtists,
                mostSkipped = mostSkipped?.toCardSong(),
                firstEverTrack = listeningHistoryDao.getFirstEverTrack()?.toCardSong()
            )
        }
}

private val artistSeparators = Regex("\\s*(,|\\bfeat\\.?|\\bft\\.)\\s*", RegexOption.IGNORE_CASE)

private fun primaryArtist(artist: String): String =
    artist.split(artistSeparators).firstOrNull { it.isNotBlank() }?.trim() ?: artist.trim()

enum class WrappedWindow { LAST_30_DAYS, LAST_365_DAYS, THIS_YEAR }

data class WrappedReport(
    val window: WrappedWindow,
    val generatedAt: Long,
    val totalSongs: Int,
    val totalPlays: Int,
    val totalMinutesListened: Long,
    val topGenre: String,
    val topSongs: List<WrappedCardSong>,
    val topArtists: List<TopArtist>,
    val mostSkipped: WrappedCardSong?,
    val firstEverTrack: WrappedCardSong?
) {
    val hasData: Boolean get() = totalSongs > 0

    companion object {
        fun empty(window: WrappedWindow) = WrappedReport(
            window = window,
            generatedAt = System.currentTimeMillis(),
            totalSongs = 0,
            totalPlays = 0,
            totalMinutesListened = 0L,
            topGenre = "—",
            topSongs = emptyList(),
            topArtists = emptyList(),
            mostSkipped = null,
            firstEverTrack = null
        )
    }
}

data class WrappedCardSong(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val playCount: Int
)

data class TopArtist(
    val name: String,
    val totalPlays: Int,
    val thumbnailUrl: String?
)

private fun ListeningHistory.toCardSong() = WrappedCardSong(
    id = songId,
    title = songTitle,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    playCount = playCount
)

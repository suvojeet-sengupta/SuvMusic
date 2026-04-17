package com.suvojeet.suvmusic.recommendation

import android.util.Log
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spotify-style weekly personalized playlists:
 *   • Discover Weekly  — 30 unheard songs tailored to your taste, refreshed every Monday
 *   • Release Radar    — recent releases from your top artists, refreshed every Friday
 *
 * Uses the existing [TasteProfileBuilder] + [YouTubeRepository] + [NativeRecommendationScorer]
 * so no new ML model or DB migration is needed. Results are cached in [RecommendationCache]
 * with a 7-day TTL — the weekly cadence is simulated by the TTL.
 */
@Singleton
class WeeklyRecommendations @Inject constructor(
    private val tasteProfileBuilder: TasteProfileBuilder,
    private val youTubeRepository: YouTubeRepository,
    private val cache: RecommendationCache
) {

    companion object {
        private const val TAG = "WeeklyRecs"
        private const val WEEKLY_TTL_MS = 7L * 24 * 60 * 60 * 1000
        private const val KEY_DISCOVER_WEEKLY = "discover_weekly_v1"
        private const val KEY_RELEASE_RADAR = "release_radar_v1"
        const val DISCOVER_WEEKLY_SIZE = 30
        const val RELEASE_RADAR_SIZE = 25
        private const val RELEASE_WINDOW_DAYS = 21
    }

    suspend fun getDiscoverWeekly(limit: Int = DISCOVER_WEEKLY_SIZE): List<Song> {
        cache.getSongs(KEY_DISCOVER_WEEKLY)?.let { return it.take(limit) }
        val generated = buildDiscoverWeekly()
        if (generated.isNotEmpty()) cache.putSongs(KEY_DISCOVER_WEEKLY, generated, WEEKLY_TTL_MS)
        return generated.take(limit)
    }

    suspend fun getReleaseRadar(limit: Int = RELEASE_RADAR_SIZE): List<Song> {
        cache.getSongs(KEY_RELEASE_RADAR)?.let { return it.take(limit) }
        val generated = buildReleaseRadar()
        if (generated.isNotEmpty()) cache.putSongs(KEY_RELEASE_RADAR, generated, WEEKLY_TTL_MS)
        return generated.take(limit)
    }

    fun invalidate() {
        cache.putSongs(KEY_DISCOVER_WEEKLY, emptyList(), 1L)
        cache.putSongs(KEY_RELEASE_RADAR, emptyList(), 1L)
    }

    private suspend fun buildDiscoverWeekly(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val profile = tasteProfileBuilder.getProfile()
            if (!profile.hasEnoughData) return@withContext emptyList()

            val topGenres = GenreTaxonomy.topGenres(profile.genreAffinityVector, 3).map { it.first }
            val topArtists = profile.artistAffinities.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key }

            val heardFingerprints = profile.recentSongIds.toMutableSet().apply {
                addAll(profile.topPlayedSongIds)
            }

            val candidates = mutableListOf<Song>()

            coroutineScope {
                // Seed from genres — tap into YT Music search for each.
                val genreJobs = topGenres.map { genre ->
                    async {
                        try {
                            youTubeRepository.search("$genre music mix", YouTubeRepository.FILTER_SONGS)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                // Seed from artist stations — getRelatedSongs on a known good song per artist is expensive
                // without a seed song, so we search the artist name to grab their catalog then crawl related.
                val artistJobs = topArtists.map { artist ->
                    async {
                        try {
                            val seedSongs = youTubeRepository.search(artist, YouTubeRepository.FILTER_SONGS).take(3)
                            val relatedForEach = seedSongs.flatMap { seed ->
                                try {
                                    youTubeRepository.getRelatedSongs(seed.id).take(10)
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                            relatedForEach
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }

                candidates.addAll(genreJobs.awaitAll().flatten())
                candidates.addAll(artistJobs.awaitAll().flatten())
            }

            // Filter out songs the user has already heard (by id and by fingerprint).
            val seenFingerprints = mutableSetOf<String>()
            val filtered = candidates
                .asSequence()
                .filter { song ->
                    if (song.id in heardFingerprints) return@filter false
                    if (song.id in profile.dislikedSongIds) return@filter false
                    if (song.artist.lowercase() in profile.dislikedArtists) return@filter false
                    val fp = "${song.title.lowercase()}|${song.artist.lowercase()}"
                    seenFingerprints.add(fp)
                }
                .distinctBy { it.id }
                .toList()

            // Shuffle (stable per week) and take.
            val weekSeed = weekOfYearSeed()
            filtered.shuffled(java.util.Random(weekSeed.toLong())).take(DISCOVER_WEEKLY_SIZE)
        } catch (e: Exception) {
            Log.e(TAG, "buildDiscoverWeekly failed", e)
            emptyList()
        }
    }

    private suspend fun buildReleaseRadar(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val profile = tasteProfileBuilder.getProfile()
            if (!profile.hasEnoughData) return@withContext emptyList()

            val topArtistIds = profile.artistAffinities.entries
                .sortedByDescending { it.value }
                .take(50)
                .map { it.key }

            val candidates = mutableListOf<Song>()
            coroutineScope {
                val jobs = topArtistIds.map { artistName ->
                    async {
                        try {
                            // `artistAffinities` keys are names, not channel IDs. Resolve via search.
                            val results = youTubeRepository.search(artistName, YouTubeRepository.FILTER_SONGS)
                            results.take(10)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                candidates.addAll(jobs.awaitAll().flatten())
            }

            val cutoff = java.time.LocalDate.now().minusDays(RELEASE_WINDOW_DAYS.toLong())
            val fresh = candidates
                .asSequence()
                .distinctBy { it.id }
                .filter { song ->
                    // Keep songs whose releaseDate is within the window, OR have no releaseDate
                    // (don't filter those out since YT Music often omits it).
                    val release = song.releaseDate?.let { parseIsoLikeDate(it) }
                    release == null || !release.isBefore(cutoff)
                }
                .filter { it.id !in profile.dislikedSongIds }
                .filter { it.artist.lowercase() !in profile.dislikedArtists }
                .filter { it.id !in profile.recentSongIds }
                .take(RELEASE_RADAR_SIZE * 2)
                .toList()

            fresh.take(RELEASE_RADAR_SIZE)
        } catch (e: Exception) {
            Log.e(TAG, "buildReleaseRadar failed", e)
            emptyList()
        }
    }

    private fun parseIsoLikeDate(s: String): java.time.LocalDate? = try {
        java.time.LocalDate.parse(s.take(10))
    } catch (e: Exception) {
        null
    }

    private fun weekOfYearSeed(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR)
    }
}

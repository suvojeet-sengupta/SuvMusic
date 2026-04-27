package com.suvojeet.suvmusic.recommendation

import android.util.Log
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.core.model.HomeItem
import com.suvojeet.suvmusic.core.model.HomeSection
import com.suvojeet.suvmusic.core.model.HomeSectionType
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spotify-style "Daily Mix 1…6" generator.
 *
 * Strategy:
 *   1. Take top 8 most-played artists from listening history.
 *   2. Cluster them by genre-vector cosine similarity into up to 6 groups.
 *   3. Each group becomes a Daily Mix: ~60% familiar songs from history, ~40% fresh pulled
 *      via [YouTubeRepository.getRelatedSongs].
 *   4. Caches the mixes with a 24-hour TTL, invalidated on significant taste shifts
 *      (e.g., 3+ new likes since last build — caller can force-invalidate).
 */
@Singleton
class DailyMixGenerator @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val youTubeRepository: YouTubeRepository,
    private val tasteProfileBuilder: TasteProfileBuilder,
    private val cache: RecommendationCache
) {

    companion object {
        private const val TAG = "DailyMixGenerator"
        private const val KEY_PREFIX = "daily_mix_v1_"
        private const val DAILY_TTL_MS = 24L * 60 * 60 * 1000
        const val MAX_MIXES = 6
        const val SONGS_PER_MIX = 40
        private const val ARTIST_SEED_POOL = 8
    }

    suspend fun getMixes(): List<HomeSection> = withContext(Dispatchers.IO) {
        // Try cache: stored as a single composite under key "daily_mix_v1_all".
        cache.getSections(KEY_PREFIX + "all")?.let { return@withContext it }

        val mixes = buildMixes()
        if (mixes.isNotEmpty()) cache.putSections(KEY_PREFIX + "all", mixes, DAILY_TTL_MS)
        mixes
    }

    fun invalidate() {
        cache.putSections(KEY_PREFIX + "all", emptyList(), 1L)
    }

    private suspend fun buildMixes(): List<HomeSection> = coroutineScope {
        try {
            val profile = tasteProfileBuilder.getProfile()
            if (!profile.hasEnoughData) return@coroutineScope emptyList()

            val topArtists = profile.artistAffinities.entries
                .sortedByDescending { it.value }
                .take(ARTIST_SEED_POOL)
                .map { it.key }
            if (topArtists.isEmpty()) return@coroutineScope emptyList()

            // Build a genre vector per artist (from history), then cluster.
            val artistVectors = topArtists.associateWith { artist ->
                GenreTaxonomy.inferGenreVector(title = "", artist = artist)
            }
            val clusters = clusterArtists(artistVectors)

            val allHistory = listeningHistoryDao.getAllHistory()
            val mixes = mutableListOf<HomeSection>()

            // Expand each cluster into a Daily Mix SEQUENTIALLY to save memory.
            clusters.take(MAX_MIXES).forEachIndexed { index, cluster ->
                try {
                    buildMixForCluster(index + 1, cluster, allHistory)?.let {
                        mixes.add(it)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to build Daily Mix for cluster $index", e)
                }
            }
            mixes
        } catch (e: Exception) {
            Log.e(TAG, "buildMixes failed", e)
            emptyList()
        }
    }

    private suspend fun buildMixForCluster(
        index: Int,
        artists: List<String>,
        allHistory: List<com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory>
    ): HomeSection? = withContext(Dispatchers.IO) {
        // 60% familiar — favorite songs from these artists that the user has played repeatedly.
        val artistLower = artists.map { it.lowercase() }.toSet()
        val familiar = allHistory
            .filter { it.artist.lowercase() in artistLower && it.playCount >= 2 }
            .sortedByDescending { it.playCount }
            .take((SONGS_PER_MIX * 0.6).toInt())
            .map { it.toSong() }

        // 40% fresh — pull related songs from seed artists.
        val freshTarget = SONGS_PER_MIX - familiar.size
        val freshRaw = mutableListOf<Song>()
        
        // Fetch fresh songs sequentially per artist in the cluster
        artists.forEach { artist ->
            try {
                val results = youTubeRepository.search(artist, YouTubeRepository.FILTER_SONGS)
                val seed = results.firstOrNull() ?: return@forEach
                val related = youTubeRepository.getRelatedSongs(seed.id).take(10)
                freshRaw.addAll(related)
            } catch (e: Exception) {
                // Ignore individual artist failures
            }
        }
        
        val familiarIds = familiar.map { it.id }.toSet()
        val fresh = freshRaw
            .distinctBy { it.id }
            .filter { it.id !in familiarIds }
            .shuffled()
            .take(freshTarget)

        val combined = (familiar + fresh).distinctBy { it.id }.take(SONGS_PER_MIX)
        if (combined.isEmpty()) return@withContext null

        val title = buildString {
            append("Daily Mix $index: ")
            append(artists.take(2).joinToString(", ") { it.replaceFirstChar { c -> c.titlecase() } })
            if (artists.size > 2) append(" & more")
        }

        HomeSection(
            title = title,
            items = combined.map { HomeItem.SongItem(it) },
            type = HomeSectionType.HorizontalCarousel
        )
    }

    /**
     * Greedy single-link clustering of artists by genre-vector cosine similarity.
     * Returns up to [MAX_MIXES] clusters. Each cluster has ≥1 artist.
     */
    private fun clusterArtists(vectors: Map<String, FloatArray>): List<List<String>> {
        val artists = vectors.keys.toList()
        if (artists.size <= MAX_MIXES) return artists.map { listOf(it) }

        val used = mutableSetOf<String>()
        val clusters = mutableListOf<MutableList<String>>()

        for (artist in artists) {
            if (artist in used) continue
            val cluster = mutableListOf(artist)
            used.add(artist)
            val seedVec = vectors[artist] ?: continue

            // Attach up to 2 similar artists.
            val similar = artists
                .filter { it !in used }
                .mapNotNull { cand ->
                    val v = vectors[cand] ?: return@mapNotNull null
                    val sim = cosine(seedVec, v)
                    if (sim > 0.35f) cand to sim else null
                }
                .sortedByDescending { it.second }
                .take(2)
                .map { it.first }
            similar.forEach {
                cluster.add(it)
                used.add(it)
            }
            clusters.add(cluster)
            if (clusters.size >= MAX_MIXES) break
        }
        return clusters
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var na = 0f
        var nb = 0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = kotlin.math.sqrt(na) * kotlin.math.sqrt(nb)
        return if (denom == 0f) 0f else (dot / denom).coerceIn(0f, 1f)
    }

    private fun com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory.toSong(): Song = Song(
        id = songId,
        title = songTitle,
        artist = artist,
        album = album,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        source = try {
            SongSource.valueOf(source)
        } catch (e: Exception) {
            SongSource.YOUTUBE
        },
        localUri = null,
        artistId = artistId
    )
}

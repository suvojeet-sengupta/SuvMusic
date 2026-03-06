package com.suvojeet.suvmusic.recommendation

import com.suvojeet.suvmusic.core.data.local.dao.DislikedItemDao
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.dao.SongGenreDao
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.core.data.local.entity.SongGenre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Builds and maintains a [UserTasteProfile] from the local listening history database.
 * The profile is used by [RecommendationEngine] to score and rank song candidates.
 *
 * **Genre vectors** are built from song title/artist keyword matching via [GenreTaxonomy]
 * and cached in Room via [SongGenreDao] to avoid redundant inference.
 */
@Singleton
class TasteProfileBuilder @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val dislikedItemDao: DislikedItemDao,
    private val songGenreDao: SongGenreDao
) {
    /** Lock to ensure only one buildProfile happens at a time */
    private val buildMutex = Mutex()

    /** Cached profile + build timestamp — atomic pair to avoid stale reads */
    @Volatile
    private var profileSnapshot: Pair<UserTasteProfile?, Long> = Pair(null, 0L)

    /**
     * Counter for play events since last invalidation.
     * Only invalidate on every Nth play to reduce rebuild frequency.
     */
    private val playEventCounter = AtomicInteger(0)

    companion object {
        /** Rebuild profile if older than 10 minutes */
        private const val PROFILE_TTL_MS = 10 * 60 * 1000L
        /** Minimum songs needed for meaningful personalization */
        private const val MIN_SONGS_FOR_PERSONALIZATION = 5
        /** How many recent songs to consider */
        private const val RECENT_SONGS_LIMIT = 50
        /** How many top songs/artists to track */
        private const val TOP_LIMIT = 30
        /** How many recent songs to use for the session genre vector */
        private const val SESSION_GENRE_WINDOW = 10
        /** Only invalidate on every Nth play event to reduce rebuild frequency */
        private const val INVALIDATE_EVERY_N_PLAYS = 5
    }

    /**
     * Get the current taste profile, rebuilding if stale.
     */
    suspend fun getProfile(forceRefresh: Boolean = false): UserTasteProfile {
        // Fast path: double-checked locking style check without lock
        val now = System.currentTimeMillis()
        val (existing, buildTime) = profileSnapshot
        if (!forceRefresh && existing != null && (now - buildTime) < PROFILE_TTL_MS) {
            return existing
        }

        // Slow path: obtain lock and rebuild if still needed
        return buildMutex.withLock {
            val (existingInner, buildTimeInner) = profileSnapshot
            if (!forceRefresh && existingInner != null && (System.currentTimeMillis() - buildTimeInner) < PROFILE_TTL_MS) {
                return@withLock existingInner
            }

            val newProfile = buildProfile()
            profileSnapshot = Pair(newProfile, System.currentTimeMillis())
            newProfile
        }
    }

    /**
     * Invalidate the cached profile (e.g., after a new song play).
     * Atomic: resets both profile reference and timestamp in a single write.
     */
    fun invalidate() {
        profileSnapshot = Pair(null, 0L)
    }

    /**
     * Called on every song play event. Only triggers a full invalidation
     * every [INVALIDATE_EVERY_N_PLAYS] plays to avoid excessive rebuilds
     * while still keeping the profile reasonably fresh.
     */
    fun onSongPlayed() {
        val count = playEventCounter.incrementAndGet()
        if (count % INVALIDATE_EVERY_N_PLAYS == 0) {
            invalidate()
        }
    }

    /**
     * Immediate invalidation for significant events (like/unlike, dislike).
     * These are rare and semantically important, so bypass the counter.
     */
    fun invalidateImmediate() {
        playEventCounter.set(0)
        invalidate()
    }

    private suspend fun buildProfile(): UserTasteProfile = withContext(Dispatchers.IO) {
        val allHistory = listeningHistoryDao.getAllHistory()
        val totalSongs = allHistory.size

        if (totalSongs == 0) {
            return@withContext UserTasteProfile()
        }

        // --- Artist Affinities ---
        // Weighted by play count * completion rate, with recency decay
        val now = System.currentTimeMillis()
        val artistScores = mutableMapOf<String, Float>()
        allHistory.forEach { entry ->
            val recencyDays = ((now - entry.lastPlayed) / (1000 * 60 * 60 * 24)).toFloat().coerceAtLeast(1f)
            val recencyWeight = 1f / (1f + (recencyDays / 30f)) // Decay over ~1 month
            val completionWeight = (entry.completionRate / 100f).coerceIn(0.1f, 1f)
            val score = entry.playCount * completionWeight * recencyWeight
            
            val artist = entry.artist.trim().lowercase()
            if (artist.isNotBlank()) {
                artistScores[artist] = (artistScores[artist] ?: 0f) + score
            }
        }
        // Normalize to 0..1
        val maxArtistScore = artistScores.values.maxOrNull() ?: 1f
        val normalizedArtists = artistScores.mapValues { (it.value / maxArtistScore).coerceIn(0f, 1f) }
            .toList()
            .sortedByDescending { it.second }
            .take(TOP_LIMIT)
            .toMap()

        // --- Time-of-Day Weights ---
        val hourCounts = mutableMapOf<Int, Int>()
        allHistory.forEach { entry ->
            val cal = Calendar.getInstance().apply { timeInMillis = entry.lastPlayed }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour] = (hourCounts[hour] ?: 0) + entry.playCount
        }
        val maxHourCount = hourCounts.values.maxOrNull()?.toFloat() ?: 1f
        val timeWeights = hourCounts.mapValues { (it.value / maxHourCount).coerceIn(0f, 1f) }

        // --- Completion Rate ---
        val avgCompletion = allHistory.map { it.completionRate }.average().toFloat()

        // --- Frequently Skipped ---
        val skippedIds = allHistory
            .filter { it.skipCount > it.playCount / 2 && it.playCount >= 3 }
            .map { it.songId }
            .toSet()

        // --- Liked Songs ---
        val likedIds = allHistory.filter { it.isLiked }.map { it.songId }.toSet()

        // --- Liked Artists (artists of liked songs) ---
        val likedArtistSet = allHistory
            .filter { it.isLiked }
            .map { it.artist.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        // --- Disliked Artists (artists with high skip rate across all their songs) ---
        val artistSkipRatios = allHistory
            .groupBy { it.artist.trim().lowercase() }
            .filter { it.key.isNotBlank() && it.value.size >= 2 }
            .mapValues { (_, songs) ->
                val totalPlays = songs.sumOf { it.playCount }.toFloat()
                val totalSkips = songs.sumOf { it.skipCount }.toFloat()
                if (totalPlays > 0) totalSkips / totalPlays else 0f
            }
        val dislikedArtistSet = artistSkipRatios
            .filter { it.value > 0.6f } // Artist skipped more than 60% of the time
            .keys

        // --- Disliked Songs (from persistent DB) ---
        val dislikedSongIdSet = try {
            dislikedItemDao.getAllDislikedSongIds().toSet()
        } catch (e: Exception) {
            emptySet()
        }

        // --- Recent Songs ---
        val recentSongs = try {
            listeningHistoryDao.getRecentlyPlayed(RECENT_SONGS_LIMIT).first()
                .map { it.songId }
        } catch (e: Exception) {
            emptyList()
        }

        // --- Top Played ---
        val topPlayedIds = try {
            listeningHistoryDao.getTopSongs(TOP_LIMIT).first()
                .map { it.songId }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }

        // --- Source Distribution ---
        val sourceCounts = allHistory.groupBy { it.source }
            .mapValues { it.value.sumOf { e -> e.playCount }.toFloat() }
        val totalPlays = sourceCounts.values.sum().coerceAtLeast(1f)
        val sourceDistribution = sourceCounts.mapValues { it.value / totalPlays }

        // --- Genre Vectors ---
        val genreVectors = buildGenreVectors(allHistory, skippedIds, recentSongs)

        UserTasteProfile(
            artistAffinities = normalizedArtists,
            timeOfDayWeights = timeWeights,
            avgCompletionRate = avgCompletion,
            frequentlySkippedIds = skippedIds,
            likedSongIds = likedIds,
            dislikedSongIds = dislikedSongIdSet,
            likedArtists = likedArtistSet,
            dislikedArtists = dislikedArtistSet,
            recentSongIds = recentSongs,
            topPlayedSongIds = topPlayedIds,
            sourceDistribution = sourceDistribution,
            totalSongsInHistory = totalSongs,
            hasEnoughData = totalSongs >= MIN_SONGS_FOR_PERSONALIZATION,
            genreAffinityVector = genreVectors.first,
            recentGenreVector = genreVectors.second,
            skipGenreVector = genreVectors.third
        )
    }

    /**
     * Build three genre vectors:
     * 1. Full affinity vector (weighted by play count across all history)
     * 2. Recent session vector (last 10 songs — mood/session recency)
     * 3. Skip genre vector (genres the user frequently skips)
     *
     * Uses [GenreTaxonomy.inferGenreVector] for keyword-based inference
     * and caches results in Room via [SongGenreDao].
     */
    private suspend fun buildGenreVectors(
        allHistory: List<ListeningHistory>,
        skippedIds: Set<String>,
        recentSongIds: List<String>
    ): Triple<FloatArray, FloatArray, FloatArray> {
        val genreCount = GenreTaxonomy.GENRE_COUNT

        // Batch-fetch cached genre vectors from Room
        val allSongIds = allHistory.map { it.songId }
        val cachedGenres = songGenreDao.getGenres(allSongIds).associateBy { it.songId }

        // Infer missing genres and batch-insert into Room cache
        val newGenres = mutableListOf<SongGenre>()
        val songGenreMap = mutableMapOf<String, FloatArray>()

        for (entry in allHistory) {
            val cached = cachedGenres[entry.songId]
            if (cached != null) {
                songGenreMap[entry.songId] = cached.toFloatArray()
            } else {
                val inferred = GenreTaxonomy.inferGenreVector(entry.songTitle, entry.artist)
                songGenreMap[entry.songId] = inferred
                if (GenreTaxonomy.isNonZero(inferred)) {
                    newGenres.add(SongGenre.fromFloatArray(entry.songId, inferred))
                }
            }
        }

        // Persist newly inferred genres
        if (newGenres.isNotEmpty()) {
            try {
                songGenreDao.insertGenres(newGenres)
            } catch (_: Exception) {
                // Non-critical — cache miss won't break scoring
            }
        }

        // 1. Full genre affinity: weighted sum of all genres by play count
        val fullVector = FloatArray(genreCount)
        var totalWeight = 0f
        for (entry in allHistory) {
            val genreVec = songGenreMap[entry.songId] ?: continue
            val weight = entry.playCount.toFloat()
            for (i in 0 until genreCount) {
                fullVector[i] += genreVec[i] * weight
            }
            totalWeight += weight
        }
        // Normalize
        if (totalWeight > 0f) {
            val maxVal = fullVector.max().coerceAtLeast(1f)
            for (i in fullVector.indices) {
                fullVector[i] = (fullVector[i] / maxVal).coerceIn(0f, 1f)
            }
        }

        // 2. Recent session genre vector (last SESSION_GENRE_WINDOW songs)
        val recentVector = FloatArray(genreCount)
        val recentWindow = recentSongIds.take(SESSION_GENRE_WINDOW)
        for (songId in recentWindow) {
            val genreVec = songGenreMap[songId] ?: continue
            for (i in 0 until genreCount) {
                recentVector[i] += genreVec[i]
            }
        }
        if (recentWindow.isNotEmpty()) {
            val maxVal = recentVector.max().coerceAtLeast(1f)
            for (i in recentVector.indices) {
                recentVector[i] = (recentVector[i] / maxVal).coerceIn(0f, 1f)
            }
        }

        // 3. Skip genre vector (from frequently skipped songs)
        val skipVector = FloatArray(genreCount)
        for (songId in skippedIds) {
            val genreVec = songGenreMap[songId] ?: continue
            for (i in 0 until genreCount) {
                skipVector[i] += genreVec[i]
            }
        }
        if (skippedIds.isNotEmpty()) {
            val maxVal = skipVector.max().coerceAtLeast(1f)
            for (i in skipVector.indices) {
                skipVector[i] = (skipVector[i] / maxVal).coerceIn(0f, 1f)
            }
        }

        return Triple(fullVector, recentVector, skipVector)
    }
}

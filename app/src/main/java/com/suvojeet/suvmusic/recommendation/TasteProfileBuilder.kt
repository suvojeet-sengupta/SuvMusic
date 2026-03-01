package com.suvojeet.suvmusic.recommendation

import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and maintains a [UserTasteProfile] from the local listening history database.
 * The profile is used by [RecommendationEngine] to score and rank song candidates.
 */
@Singleton
class TasteProfileBuilder @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao
) {
    /** Cached profile — rebuilt when stale */
    @Volatile
    private var cachedProfile: UserTasteProfile? = null
    private var lastBuildTime: Long = 0L

    companion object {
        /** Rebuild profile if older than 10 minutes */
        private const val PROFILE_TTL_MS = 10 * 60 * 1000L
        /** Minimum songs needed for meaningful personalization */
        private const val MIN_SONGS_FOR_PERSONALIZATION = 5
        /** How many recent songs to consider */
        private const val RECENT_SONGS_LIMIT = 50
        /** How many top songs/artists to track */
        private const val TOP_LIMIT = 30
    }

    /**
     * Get the current taste profile, rebuilding if stale.
     */
    suspend fun getProfile(forceRefresh: Boolean = false): UserTasteProfile {
        val now = System.currentTimeMillis()
        val existing = cachedProfile
        if (!forceRefresh && existing != null && (now - lastBuildTime) < PROFILE_TTL_MS) {
            return existing
        }
        return buildProfile().also {
            cachedProfile = it
            lastBuildTime = now
        }
    }

    /**
     * Invalidate the cached profile (e.g., after a new song play).
     */
    fun invalidate() {
        cachedProfile = null
        lastBuildTime = 0L
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

        UserTasteProfile(
            artistAffinities = normalizedArtists,
            timeOfDayWeights = timeWeights,
            avgCompletionRate = avgCompletion,
            frequentlySkippedIds = skippedIds,
            likedSongIds = likedIds,
            dislikedSongIds = emptySet(), // Tracked in-memory by RecommendationEngine
            likedArtists = likedArtistSet,
            dislikedArtists = dislikedArtistSet,
            recentSongIds = recentSongs,
            topPlayedSongIds = topPlayedIds,
            sourceDistribution = sourceDistribution,
            totalSongsInHistory = totalSongs,
            hasEnoughData = totalSongs >= MIN_SONGS_FOR_PERSONALIZATION
        )
    }
}

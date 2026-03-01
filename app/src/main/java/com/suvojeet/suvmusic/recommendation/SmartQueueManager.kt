package com.suvojeet.suvmusic.recommendation

import android.util.Log
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent queue manager that works with [RecommendationEngine] to ensure
 * the playback queue always has great next songs, tightly coupled with
 * YouTube Music's recommendation signals.
 *
 * Features:
 * - Pre-fetches "up next" songs before the queue runs dry
 * - Uses multi-seed recommendations (current song + recent plays + taste profile)
 * - Deduplicates against the current queue
 * - Avoids frequently skipped songs
 * - Adapts to context (radio mode vs. autoplay vs. manual queue)
 */
@Singleton
class SmartQueueManager @Inject constructor(
    private val recommendationEngine: RecommendationEngine
) {
    companion object {
        private const val TAG = "SmartQueueManager"

        /** Minimum songs to keep ahead in the queue */
        const val MIN_LOOKAHEAD = 3

        /** How many songs to fetch per batch */
        const val BATCH_SIZE = 10

        /** Maximum retries for finding new songs */
        private const val MAX_SEED_RETRIES = 3
    }

    /** Last seed used for radio/autoplay fetching */
    @Volatile
    var lastSeedId: String? = null
        private set

    /**
     * Check if the queue needs more songs and fetch them if so.
     *
     * @param currentSong Currently playing song
     * @param currentIndex Current position in queue
     * @param queue The full current queue
     * @param isRadioMode Whether radio mode is active
     * @param isAutoplayEnabled Whether autoplay is enabled
     * @return List of new songs to append to the queue, or empty if queue is sufficient
     */
    suspend fun ensureQueueHealth(
        currentSong: Song,
        currentIndex: Int,
        queue: List<Song>,
        isRadioMode: Boolean,
        isAutoplayEnabled: Boolean
    ): List<Song> = withContext(Dispatchers.IO) {
        if (!isRadioMode && !isAutoplayEnabled) return@withContext emptyList()
        
        val remainingSongs = queue.size - currentIndex - 1
        if (remainingSongs >= MIN_LOOKAHEAD) return@withContext emptyList()

        Log.d(TAG, "Queue health check: $remainingSongs songs remaining, fetching more...")

        val existingIds = queue.map { it.id }.toSet()
        var seedId = lastSeedId ?: currentSong.id
        var attempt = 0
        var newSongs = emptyList<Song>()

        while (newSongs.isEmpty() && attempt < MAX_SEED_RETRIES) {
            attempt++

            // Use RecommendationEngine's smart up-next logic
            val candidates = if (isRadioMode) {
                recommendationEngine.getMoreForRadio(seedId, existingIds, BATCH_SIZE)
            } else {
                recommendationEngine.getUpNext(currentSong, queue, BATCH_SIZE)
            }

            newSongs = candidates.filter { it.id !in existingIds }

            if (newSongs.isEmpty()) {
                // Try different seed — use a random song from the middle of the queue
                val midIndex = (queue.size / 2).coerceIn(0, queue.size - 1)
                seedId = queue.getOrNull(midIndex)?.id ?: currentSong.id
                Log.d(TAG, "No new songs from seed, trying alternative seed (attempt $attempt)")
            }
        }

        if (newSongs.isNotEmpty()) {
            lastSeedId = newSongs.last().id
            Log.d(TAG, "Fetched ${newSongs.size} new songs for queue")
        } else {
            Log.w(TAG, "Could not find new songs after $MAX_SEED_RETRIES attempts")
        }

        newSongs
    }

    /**
     * Build the initial radio queue for a seed song.
     * Fetches a mix of related songs and personalized recommendations.
     *
     * @param seedSong The song to start radio from
     * @param initialQueue Optional songs already in the queue (e.g., from search results)
     * @return Songs to add to the queue after the seed
     */
    suspend fun buildRadioQueue(
        seedSong: Song,
        initialQueue: List<Song> = emptyList()
    ): List<Song> = withContext(Dispatchers.IO) {
        lastSeedId = seedSong.id
        val existingIds = mutableSetOf(seedSong.id)
        initialQueue.forEach { existingIds.add(it.id) }

        val songs = recommendationEngine.getUpNext(seedSong, initialQueue, 30)
        val filtered = songs.filter { it.id !in existingIds }

        if (filtered.isNotEmpty()) {
            lastSeedId = filtered.last().id
        }

        Log.d(TAG, "Built radio queue: ${filtered.size} songs for seed '${seedSong.title}'")
        filtered
    }

    /**
     * Reset the queue manager state (e.g., when radio is stopped).
     */
    fun reset() {
        lastSeedId = null
    }
}

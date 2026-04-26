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
        const val MIN_LOOKAHEAD = 15

        /** How many songs to fetch per batch */
        const val BATCH_SIZE = 25

        /** Maximum retries for finding new songs */
        private const val MAX_SEED_RETRIES = 3

        /** Dedup window — last N songs served by this manager in the current session. */
        private const val RECENTLY_SERVED_CAPACITY = 80

        /** Early-warning threshold: refill the queue when this many songs or fewer remain. */
        const val EARLY_WARNING_LOOKAHEAD = 8
    }

    /** Last seed used for radio/autoplay fetching */
    @Volatile
    var lastSeedId: String? = null
        private set

    /**
     * Ring buffer of recently-served song IDs (both appended to queue and played-from-queue).
     * Used to prevent the radio/autoplay loop from pushing the same songs back within a session.
     */
    private val recentlyServed = java.util.ArrayDeque<String>(RECENTLY_SERVED_CAPACITY)
    private val recentlyServedLock = Any()

    private fun markServed(ids: Collection<String>) {
        if (ids.isEmpty()) return
        synchronized(recentlyServedLock) {
            for (id in ids) {
                if (recentlyServed.size >= RECENTLY_SERVED_CAPACITY) recentlyServed.pollFirst()
                recentlyServed.addLast(id)
            }
        }
    }

    private fun recentlyServedSnapshot(): Set<String> {
        synchronized(recentlyServedLock) { return recentlyServed.toSet() }
    }

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
        // Early-warning trigger — refill well before the queue drains.
        if (remainingSongs > EARLY_WARNING_LOOKAHEAD && remainingSongs >= MIN_LOOKAHEAD) {
            return@withContext emptyList()
        }

        Log.d(TAG, "Queue health check: $remainingSongs songs remaining, fetching more...")

        val existingIds = queue.map { it.id }.toSet()
        val recentlyServedIds = recentlyServedSnapshot()
        // Any song we've served this session OR that's already in the queue is blocked.
        val blockedIds = existingIds + recentlyServedIds

        var seedId = lastSeedId ?: currentSong.id
        var attempt = 0
        val allNewSongs = mutableListOf<Song>()

        while (allNewSongs.size < BATCH_SIZE && attempt < MAX_SEED_RETRIES) {
            attempt++

            val candidates = if (isRadioMode) {
                recommendationEngine.getMoreForRadio(seedId, blockedIds, BATCH_SIZE)
            } else {
                recommendationEngine.getUpNext(currentSong, queue, BATCH_SIZE)
            }

            val addedIds = allNewSongs.map { it.id }.toSet()
            val filtered = candidates.filter { it.id !in blockedIds && it.id !in addedIds }
            allNewSongs.addAll(filtered)

            if (filtered.isEmpty()) {
                val midIndex = (queue.size / 2).coerceIn(0, queue.size - 1)
                seedId = queue.getOrNull(midIndex)?.id ?: currentSong.id
                Log.d(TAG, "No new songs from seed, trying alternative seed (attempt $attempt)")
            } else {
                seedId = filtered.last().id
            }
        }

        if (allNewSongs.isNotEmpty()) {
            lastSeedId = allNewSongs.last().id
            markServed(allNewSongs.map { it.id })
            Log.d(TAG, "Fetched ${allNewSongs.size} new songs for queue")
        } else {
            Log.w(TAG, "Could not find new songs after $MAX_SEED_RETRIES attempts")
        }

        allNewSongs
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

        // Fetch up to 50 songs for initial queue
        val songs = recommendationEngine.getUpNext(seedSong, initialQueue, 50)
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
        synchronized(recentlyServedLock) { recentlyServed.clear() }
    }
}

package com.suvojeet.suvmusic.recommendation

import android.util.Log
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.data.model.HomeSectionType
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.suvojeet.suvmusic.core.data.local.dao.DislikedItemDao
import com.suvojeet.suvmusic.core.data.local.entity.DislikedArtist as DislikedArtistEntity
import com.suvojeet.suvmusic.core.data.local.entity.DislikedSong as DislikedSongEntity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Advanced recommendation engine deeply integrated with YouTube Music's recommendation system.
 *
 * **Architecture:**
 * 1. Uses YouTube Music's personalized API (FEmusic_home, related, radio, mixes) as the PRIMARY signal
 * 2. Builds a local [UserTasteProfile] from listening history for scoring/ranking
 * 3. Blends YT Music recommendations with local taste analysis for optimal personalization
 * 4. Provides all home screen sections, "up next" queue, and discovery features
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val dislikedItemDao: DislikedItemDao,
    private val youTubeRepository: YouTubeRepository,
    private val tasteProfileBuilder: TasteProfileBuilder,
    private val cache: RecommendationCache
) {
    companion object {
        private const val TAG = "RecommendationEngine"
    }

    private val scope = MainScope()

    /**
     * In-memory set of disliked song IDs (synced with DB).
     */
    private val dislikedSongIds = mutableSetOf<String>()

    /**
     * In-memory set of disliked artists (synced with DB).
     */
    private val dislikedArtists = mutableSetOf<String>()

    init {
        // Load persistent dislikes on initialization
        scope.launch(Dispatchers.IO) {
            try {
                dislikedSongIds.addAll(dislikedItemDao.getAllDislikedSongIds())
                dislikedArtists.addAll(dislikedItemDao.getAllDislikedArtistNames())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load persistent dislikes", e)
            }
        }
    }

    // ============================================================================================
    // UTILITIES — Deduplication & Filtering
    // ============================================================================================

    /**
     * Generate a unique fingerprint for a song to catch duplicates with different IDs.
     * Normalized "Title | Artist"
     */
    private fun getSongFingerprint(song: Song): String {
        val title = song.title.lowercase().replace(Regex("[^a-z0-9]"), "")
        val artist = song.artist.lowercase().replace(Regex("[^a-z0-9]"), "")
        return "$title|$artist"
    }

    /**
     * Filter and deduplicate a list of songs using both ID and Title/Artist fingerprint.
     */
    private fun deduplicate(songs: List<Song>, seenIds: MutableSet<String>, seenFingerprints: MutableSet<String>): List<Song> {
        return songs.filter { song ->
            val fingerprint = getSongFingerprint(song)
            val isNew = seenIds.add(song.id) && seenFingerprints.add(fingerprint)
            isNew && song.id !in dislikedSongIds && song.artist.lowercase().trim() !in dislikedArtists
        }
    }

    // ============================================================================================
    // PUBLIC API — Home Screen
    // ============================================================================================

    /**
     * Generate the complete set of personalized home sections, tightly coupled with YT Music.
     */
    suspend fun getPersonalizedHomeSections(): List<HomeSection> = coroutineScope {
        val cached = cache.getSections(RecommendationCache.Keys.HOME_SECTIONS)
        if (cached != null) return@coroutineScope cached

        val profile = tasteProfileBuilder.getProfile()
        val sections = mutableListOf<HomeSection>()
        val seenSongIds = mutableSetOf<String>()
        val seenFingerprints = mutableSetOf<String>()

        // Launch multiple recommendation sources in parallel
        val quickPicksDeferred = async { fetchQuickPicks(profile, seenSongIds, seenFingerprints) }
        val recentBasedDeferred = async { fetchRecentBased(profile, seenSongIds, seenFingerprints) }
        val artistMixDeferred = async { fetchArtistMixes(profile) }
        val discoveryDeferred = async { fetchDiscoveryMix(profile, seenSongIds, seenFingerprints) }
        val forgottenDeferred = async { fetchForgottenFavorites(profile) }
        val timeBasedDeferred = async { fetchTimeBasedRecommendations(profile, seenSongIds, seenFingerprints) }

        // Collect results
        val quickPicks = quickPicksDeferred.await()
        val recentBased = recentBasedDeferred.await()
        val artistMixes = artistMixDeferred.await()
        val discovery = discoveryDeferred.await()
        val forgotten = forgottenDeferred.await()
        val timeBased = timeBasedDeferred.await()

        // Assemble sections in priority order
        if (quickPicks.isNotEmpty()) {
            sections.add(HomeSection(
                title = "Quick picks",
                items = quickPicks.map { HomeItem.SongItem(it) },
                type = HomeSectionType.QuickPicks
            ))
        }

        if (timeBased.isNotEmpty()) {
            val greeting = getTimeBasedGreeting()
            sections.add(HomeSection(
                title = greeting,
                items = timeBased.map { HomeItem.SongItem(it) },
                type = HomeSectionType.HorizontalCarousel
            ))
        }

        if (recentBased.isNotEmpty()) {
            sections.add(HomeSection(
                title = "Based on recent listening",
                items = recentBased.map { HomeItem.SongItem(it) },
                type = HomeSectionType.HorizontalCarousel
            ))
        }

        // Artist mixes (each is its own section)
        sections.addAll(artistMixes)

        if (discovery.isNotEmpty()) {
            sections.add(HomeSection(
                title = "Discover new music",
                items = discovery.map { HomeItem.SongItem(it) },
                type = HomeSectionType.HorizontalCarousel
            ))
        }

        if (forgotten.isNotEmpty()) {
            sections.add(HomeSection(
                title = "Forgotten favorites",
                items = forgotten.map { HomeItem.SongItem(it) },
                type = HomeSectionType.HorizontalCarousel
            ))
        }

        cache.putSections(RecommendationCache.Keys.HOME_SECTIONS, sections)
        sections
    }

    // ============================================================================================
    // PUBLIC API — Quick Picks (Primary Recommendations)
    // ============================================================================================

    /**
     * Generate personalized song recommendations blending YT Music signals with local taste.
     */
    suspend fun getPersonalizedRecommendations(limit: Int = 20): List<Song> {
        val cached = cache.getSongs(RecommendationCache.Keys.QUICK_PICKS)
        if (cached != null) return cached.take(limit)

        val profile = tasteProfileBuilder.getProfile()
        val candidates = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()
        val seenFingerprints = mutableSetOf<String>()

        // --- Priority 1: YT Music's official personalized recommendations ---
        if (youTubeRepository.isLoggedIn()) {
            try {
                val ytRecs = youTubeRepository.getRecommendations()
                candidates.addAll(deduplicate(ytRecs, seenIds, seenFingerprints))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch YT Music recommendations", e)
            }
        }

        // --- Priority 2: Related songs from multiple recent plays (multi-seed) ---
        if (candidates.size < limit) {
            try {
                val recentIds = profile.recentSongIds.take(5)
                if (recentIds.isNotEmpty()) {
                    coroutineScope {
                        val relatedJobs = recentIds.map { songId ->
                            async(Dispatchers.IO) {
                                try {
                                    youTubeRepository.getRelatedSongs(songId)
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        }
                        val results = relatedJobs.awaitAll().flatten()
                        candidates.addAll(deduplicate(results, seenIds, seenFingerprints))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch multi-seed related songs", e)
            }
        }

        // --- Priority 3: "My Supermix" ---
        if (candidates.size < limit) {
            try {
                val supermix = youTubeRepository.getPlaylist("RTM")
                candidates.addAll(deduplicate(supermix.songs, seenIds, seenFingerprints))
            } catch (e: Exception) {
                // Ignore
            }
        }

        // --- Priority 4: Trending fallback ---
        if (candidates.isEmpty()) {
            try {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val trending = youTubeRepository.search("top hits $year", YouTubeRepository.FILTER_SONGS)
                candidates.addAll(deduplicate(trending, seenIds, seenFingerprints))
            } catch (e: Exception) {
                // Final fallback
            }
        }

        // Score and rank using taste profile
        val scored = scoreAndRank(candidates, profile)
        val result = scored.take(limit)

        cache.putSongs(RecommendationCache.Keys.QUICK_PICKS, scored) // Cache full ranked list
        return result
    }

    // ============================================================================================
    // PUBLIC API — Up Next / Queue Intelligence
    // ============================================================================================

    /**
     * Get the best "up next" songs to play after the current song.
     */
    suspend fun getUpNext(
        currentSong: Song,
        currentQueue: List<Song> = emptyList(),
        count: Int = 15
    ): List<Song> = withContext(Dispatchers.IO) {
        val profile = tasteProfileBuilder.getProfile()
        val seenIds = currentQueue.map { it.id }.toMutableSet()
        seenIds.add(currentSong.id)
        val seenFingerprints = currentQueue.map { getSongFingerprint(it) }.toMutableSet()
        seenFingerprints.add(getSongFingerprint(currentSong))
        
        val candidates = mutableListOf<Song>()

        // 1. YT Music related songs
        try {
            val cached = cache.getRelatedSongs(currentSong.id)
            val related = cached ?: youTubeRepository.getRelatedSongs(currentSong.id).also {
                cache.putRelatedSongs(currentSong.id, it)
            }
            candidates.addAll(deduplicate(related, seenIds, seenFingerprints))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get related songs for up-next", e)
        }

        // 2. Supplement with personalized recs
        if (candidates.size < count && youTubeRepository.isLoggedIn()) {
            try {
                val personalRecs = youTubeRepository.getRecommendations()
                candidates.addAll(deduplicate(personalRecs, seenIds, seenFingerprints))
            } catch (e: Exception) {
                // Continue
            }
        }

        // 3. Score, filter skips, and rank
        val scored = scoreAndRank(candidates, profile)
        scored.take(count)
    }

    /**
     * Continuously generate "more like this" songs for infinite autoplay/radio.
     */
    suspend fun getMoreForRadio(
        seedSongId: String,
        excludeIds: Set<String>,
        count: Int = 10
    ): List<Song> = withContext(Dispatchers.IO) {
        val profile = tasteProfileBuilder.getProfile()
        val candidates = mutableListOf<Song>()
        val seenIds = excludeIds.toMutableSet()
        val seenFingerprints = mutableSetOf<String>()

        // 1. Related songs from the seed
        try {
            val related = youTubeRepository.getRelatedSongs(seedSongId)
            candidates.addAll(deduplicate(related, seenIds, seenFingerprints))
        } catch (e: Exception) {
            Log.w(TAG, "Radio: failed to get related for seed $seedSongId", e)
        }

        // 2. Personalized supplement
        if (candidates.size < count) {
            try {
                val personalRecs = getPersonalizedRecommendations(count * 2)
                candidates.addAll(deduplicate(personalRecs, seenIds, seenFingerprints))
            } catch (e: Exception) {
                // Continue
            }
        }

        // Score and return
        val scored = scoreAndRank(candidates, profile)
        scored.take(count)
    }

    // ============================================================================================
    // Signal: Notify the engine of user actions
    // ============================================================================================

    fun onSongPlayed(song: Song) {
        tasteProfileBuilder.invalidate()
        cache.invalidateUpNext()
    }

    fun onSongSkipped(song: Song) {
        tasteProfileBuilder.invalidate()
    }

    fun onSongLikeChanged(song: Song, isLiked: Boolean) {
        tasteProfileBuilder.invalidate()
        cache.invalidateRecommendations()
    }

    fun onSongDisliked(song: Song, isDisliked: Boolean) {
        val artistKey = song.artist.trim().lowercase()
        scope.launch(Dispatchers.IO) {
            try {
                if (isDisliked) {
                    dislikedSongIds.add(song.id)
                    dislikedItemDao.insertDislikedSong(DislikedSongEntity(song.id, song.title, song.artist))
                    
                    if (artistKey.isNotBlank()) {
                        dislikedArtists.add(artistKey)
                        dislikedItemDao.insertDislikedArtist(DislikedArtistEntity(artistKey))
                    }
                } else {
                    dislikedSongIds.remove(song.id)
                    dislikedItemDao.removeDislikedSong(song.id)
                    // We don't remove the artist dislike here as it might have been from another song
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update persistent dislikes", e)
            }
        }
        tasteProfileBuilder.invalidate()
        cache.invalidateRecommendations()
        cache.invalidateUpNext()
    }

    fun onAuthStateChanged() {
        scope.launch(Dispatchers.IO) {
            dislikedItemDao.clearAllDislikedSongs()
            dislikedItemDao.clearAllDislikedArtists()
            dislikedSongIds.clear()
            dislikedArtists.clear()
        }
        tasteProfileBuilder.invalidate()
        cache.invalidateAll()
    }

    // ============================================================================================
    // PRIVATE — Scoring & Ranking
    // ============================================================================================

    private fun scoreAndRank(candidates: List<Song>, profile: UserTasteProfile): List<Song> {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val artistCounts = mutableMapOf<String, Int>()

        data class ScoredSong(val song: Song, val score: Float)

        val scored = candidates
            .filter { it.id !in dislikedSongIds && it.artist.lowercase().trim() !in dislikedArtists }
            .map { song ->
                var score = 0.5f

                val artistKey = song.artist.trim().lowercase()

                // --- Artist Affinity ---
                val artistAffinity = profile.artistAffinities[artistKey] ?: 0f
                score += artistAffinity * 0.30f

                // --- Freshness ---
                val isRecent = song.id in profile.recentSongIds
                if (!isRecent) score += 0.15f

                // --- Skip Avoidance ---
                if (song.id in profile.frequentlySkippedIds) score -= 0.15f

                // --- Liked Boost ---
                if (song.id in profile.likedSongIds) score += 0.15f
                if (artistKey in profile.likedArtists) score += 0.10f

                // --- Time-of-Day Relevance ---
                val timeWeight = profile.timeOfDayWeights[currentHour] ?: 0.5f
                score += timeWeight * 0.10f

                // --- Variety Penalty ---
                val artistCount = artistCounts.getOrDefault(artistKey, 0)
                artistCounts[artistKey] = artistCount + 1
                if (artistCount > 2) score -= 0.12f * (artistCount - 2)

                ScoredSong(song, score.coerceIn(0f, 1f))
            }

        return scored.sortedByDescending { it.score }.map { it.song }
    }

    // ============================================================================================
    // PRIVATE — Section Generators
    // ============================================================================================

    private suspend fun fetchQuickPicks(
        profile: UserTasteProfile,
        seenIds: MutableSet<String>,
        seenFingerprints: MutableSet<String>
    ): List<Song> {
        val songs = getPersonalizedRecommendations(25)
        return deduplicate(songs, seenIds, seenFingerprints).take(20)
    }

    private suspend fun fetchRecentBased(
        profile: UserTasteProfile,
        seenIds: MutableSet<String>,
        seenFingerprints: MutableSet<String>
    ): List<Song> {
        val songs = getRecentBasedSuggestions(20)
        return deduplicate(songs, seenIds, seenFingerprints).take(15)
    }

    private suspend fun fetchDiscoveryMix(
        profile: UserTasteProfile,
        seenIds: MutableSet<String>,
        seenFingerprints: MutableSet<String>
    ): List<Song> {
        val cached = cache.getSongs(RecommendationCache.Keys.DISCOVERY_MIX)
        if (cached != null) return deduplicate(cached, seenIds, seenFingerprints).take(15)

        val knownArtists = profile.artistAffinities.keys
        val candidates = mutableListOf<Song>()

        try {
            // Enhanced Discovery: Use varied queries + related songs from obscure artists
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val discoveryQueries = listOf(
                "emerging artists $year",
                "underground music",
                "new music friday",
                "indie gems"
            )
            
            coroutineScope {
                val jobs = discoveryQueries.map { query ->
                    async {
                        try {
                            youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                val results = jobs.awaitAll().flatten().shuffled()
                candidates.addAll(results.filter { song ->
                    song.artist.lowercase().trim() !in knownArtists
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build discovery mix", e)
        }

        val filtered = deduplicate(candidates, seenIds, seenFingerprints)
        cache.putSongs(RecommendationCache.Keys.DISCOVERY_MIX, filtered)
        return filtered.take(15)
    }

    private suspend fun fetchTimeBasedRecommendations(
        profile: UserTasteProfile,
        seenIds: MutableSet<String>,
        seenFingerprints: MutableSet<String>
    ): List<Song> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // Refined granular ranges
        val query = when (hour) {
            in 4..6 -> "pre-dawn peaceful music"
            in 7..9 -> "energizing morning commute"
            in 10..12 -> "deep focus work music"
            in 13..15 -> "afternoon energy boost"
            in 16..18 -> "sunset lounge chill"
            in 19..21 -> "evening dinner jazz"
            in 22..23 -> "late night lo-fi"
            else -> "deep sleep ambient"
        }

        return try {
            val songs = youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS)
            val scored = scoreAndRank(songs, tasteProfileBuilder.getProfile())
            deduplicate(scored, seenIds, seenFingerprints).take(12)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "Start your morning right"
            in 11..13 -> "Midday melodies"
            in 14..17 -> "Afternoon flow"
            in 18..21 -> "Evening relaxation"
            in 22..23 -> "Night owl session"
            else -> "Into the deep night"
        }
    }

    // (Rest of the methods like getRecentBasedSuggestions, getArtistMixRecommendations, etc. remain with similar enhancements)

}

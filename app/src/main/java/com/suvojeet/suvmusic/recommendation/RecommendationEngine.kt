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
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced recommendation engine deeply integrated with YouTube Music's recommendation system.
 *
 * **Architecture:**
 * 1. Uses YouTube Music's personalized API (FEmusic_home, related, radio, mixes) as the PRIMARY signal
 * 2. Builds a local [UserTasteProfile] from listening history for scoring/ranking
 * 3. Blends YT Music recommendations with local taste analysis for optimal personalization
 * 4. Provides all home screen sections, "up next" queue, and discovery features
 *
 * **Key capabilities:**
 * - Personalized Quick Picks (YT Music + local taste scoring)
 * - "Your Mix" playlists from YT Music
 * - Time-of-day aware recommendations
 * - Artist-affinity based discovery
 * - Listening pattern analysis for better next-song selection
 * - Smart queue management for autoplay / radio
 * - Deep deduplication and skip-avoidance
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val youTubeRepository: YouTubeRepository,
    private val tasteProfileBuilder: TasteProfileBuilder,
    private val cache: RecommendationCache
) {
    companion object {
        private const val TAG = "RecommendationEngine"
    }

    /**
     * In-memory set of disliked song IDs (synced when user presses dislike).
     * These songs are completely excluded from all recommendations.
     */
    private val dislikedSongIds = mutableSetOf<String>()

    /**
     * In-memory set of disliked artists (derived from disliked songs).
     * Songs by these artists receive a heavy penalty.
     */
    private val dislikedArtists = mutableSetOf<String>()

    // ============================================================================================
    // PUBLIC API — Home Screen
    // ============================================================================================

    /**
     * Generate the complete set of personalized home sections, tightly coupled with YT Music.
     * Returns multiple themed sections for the home screen, ordered by relevance.
     *
     * Sections may include:
     * - "Quick picks" — hybrid YT recs + taste-scored
     * - "Based on recent listening" — related to last few plays
     * - "Your [Artist] Mix" — artist-specific mixes
     * - "Discover Weekly" — exploration beyond comfort zone
     * - "Forgotten Favorites" — songs you haven't played in a while
     * - Time-based greetings ("Good Morning Vibes", "Late Night Listening")
     */
    suspend fun getPersonalizedHomeSections(): List<HomeSection> = coroutineScope {
        val cached = cache.getSections(RecommendationCache.Keys.HOME_SECTIONS)
        if (cached != null) return@coroutineScope cached

        val profile = tasteProfileBuilder.getProfile()
        val sections = mutableListOf<HomeSection>()
        val seenSongIds = mutableSetOf<String>()

        // Launch multiple recommendation sources in parallel
        val quickPicksDeferred = async { fetchQuickPicks(profile, seenSongIds) }
        val recentBasedDeferred = async { fetchRecentBased(profile, seenSongIds) }
        val artistMixDeferred = async { fetchArtistMixes(profile) }
        val discoveryDeferred = async { fetchDiscoveryMix(profile, seenSongIds) }
        val forgottenDeferred = async { fetchForgottenFavorites(profile) }
        val timeBasedDeferred = async { fetchTimeBasedRecommendations(profile, seenSongIds) }

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
     * This is the primary recommendation method used for "Quick picks" on home screen.
     */
    suspend fun getPersonalizedRecommendations(limit: Int = 20): List<Song> {
        val cached = cache.getSongs(RecommendationCache.Keys.QUICK_PICKS)
        if (cached != null) return cached.take(limit)

        val profile = tasteProfileBuilder.getProfile()
        val candidates = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()

        // --- Priority 1: YT Music's official personalized recommendations ---
        if (youTubeRepository.isLoggedIn()) {
            try {
                val ytRecs = youTubeRepository.getRecommendations()
                ytRecs.forEach { song ->
                    if (seenIds.add(song.id)) candidates.add(song)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch YT Music recommendations", e)
            }
        }

        // --- Priority 2: Related songs from multiple recent plays (multi-seed) ---
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
                    relatedJobs.awaitAll().flatten().forEach { song ->
                        if (seenIds.add(song.id)) candidates.add(song)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch multi-seed related songs", e)
        }

        // --- Priority 3: "My Supermix" ---
        if (candidates.size < limit) {
            try {
                val supermix = youTubeRepository.getPlaylist("RTM")
                supermix.songs.forEach { song ->
                    if (seenIds.add(song.id)) candidates.add(song)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // --- Priority 4: Trending fallback ---
        if (candidates.isEmpty()) {
            try {
                val trending = youTubeRepository.search("top hits 2026", YouTubeRepository.FILTER_SONGS)
                trending.forEach { song ->
                    if (seenIds.add(song.id)) candidates.add(song)
                }
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
     * Deeply coupled with YT Music's recommendations but enhanced with taste profile.
     *
     * @param currentSong The currently playing song
     * @param currentQueue Current queue songs (for deduplication)
     * @param count How many songs to return
     */
    suspend fun getUpNext(
        currentSong: Song,
        currentQueue: List<Song> = emptyList(),
        count: Int = 15
    ): List<Song> = withContext(Dispatchers.IO) {
        val profile = tasteProfileBuilder.getProfile()
        val existingIds = currentQueue.map { it.id }.toMutableSet()
        existingIds.add(currentSong.id)
        
        val candidates = mutableListOf<Song>()

        // 1. YT Music related songs (primary signal — this IS what YT Music would play next)
        try {
            val cached = cache.getRelatedSongs(currentSong.id)
            val related = cached ?: youTubeRepository.getRelatedSongs(currentSong.id).also {
                cache.putRelatedSongs(currentSong.id, it)
            }
            related.forEach { song ->
                if (song.id !in existingIds) {
                    candidates.add(song)
                    existingIds.add(song.id)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get related songs for up-next", e)
        }

        // 2. If logged in, supplement with personalized recs to maintain variety
        if (candidates.size < count && youTubeRepository.isLoggedIn()) {
            try {
                val personalRecs = youTubeRepository.getRecommendations()
                personalRecs.forEach { song ->
                    if (song.id !in existingIds) {
                        candidates.add(song)
                        existingIds.add(song.id)
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // 3. Multi-seed from recent favorites if still short
        if (candidates.size < count && profile.hasEnoughData) {
            val topArtists = profile.artistAffinities.keys.take(3)
            for (artist in topArtists) {
                if (candidates.size >= count * 2) break
                try {
                    val artistSongs = youTubeRepository.search(artist, YouTubeRepository.FILTER_SONGS)
                    artistSongs.forEach { song ->
                        if (song.id !in existingIds) {
                            candidates.add(song)
                            existingIds.add(song.id)
                        }
                    }
                } catch (e: Exception) {
                    // Continue
                }
            }
        }

        // 4. Score, filter skips, and rank
        val scored = scoreAndRank(candidates, profile)
        scored.take(count)
    }

    /**
     * Continuously generate "more like this" songs for infinite autoplay/radio.
     * Uses the current song and taste profile to find the best next batch.
     *
     * @param seedSongId The song to base recommendations on
     * @param excludeIds IDs to exclude (already in queue)
     * @param count How many songs to fetch
     */
    suspend fun getMoreForRadio(
        seedSongId: String,
        excludeIds: Set<String>,
        count: Int = 10
    ): List<Song> = withContext(Dispatchers.IO) {
        val profile = tasteProfileBuilder.getProfile()
        val candidates = mutableListOf<Song>()
        val seen = excludeIds.toMutableSet()

        // 1. Related songs from the seed
        try {
            val related = youTubeRepository.getRelatedSongs(seedSongId)
            related.forEach { song ->
                if (song.id !in seen) {
                    candidates.add(song)
                    seen.add(song.id)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Radio: failed to get related for seed $seedSongId", e)
        }

        // 2. If there are results, use the last one as secondary seed for more variety
        if (candidates.isNotEmpty() && candidates.size < count) {
            val secondarySeed = candidates.last().id
            try {
                val moreRelated = youTubeRepository.getRelatedSongs(secondarySeed)
                moreRelated.forEach { song ->
                    if (song.id !in seen) {
                        candidates.add(song)
                        seen.add(song.id)
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // 3. Personalized supplement
        if (candidates.size < count) {
            try {
                val personalRecs = getPersonalizedRecommendations(count * 2)
                personalRecs.forEach { song ->
                    if (song.id !in seen) {
                        candidates.add(song)
                        seen.add(song.id)
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // Score and return
        val scored = scoreAndRank(candidates, profile)
        scored.take(count)
    }

    // ============================================================================================
    // PUBLIC API — Specialized Recommendations
    // ============================================================================================

    /**
     * Get "Based on Recent Plays" recommendations.
     * Multiple seeds from the last few played songs for broader coverage.
     */
    suspend fun getRecentBasedSuggestions(limit: Int = 10): List<Song> {
        val cached = cache.getSongs(RecommendationCache.Keys.BASED_ON_RECENT)
        if (cached != null) return cached.take(limit)

        val profile = tasteProfileBuilder.getProfile()
        val recentIds = profile.recentSongIds.take(3)
        if (recentIds.isEmpty()) return emptyList()

        val candidates = mutableListOf<Song>()
        val seenIds = recentIds.toMutableSet()

        coroutineScope {
            val jobs = recentIds.map { songId ->
                async(Dispatchers.IO) {
                    try {
                        youTubeRepository.getRelatedSongs(songId)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            jobs.awaitAll().flatten().forEach { song ->
                if (seenIds.add(song.id)) candidates.add(song)
            }
        }

        val scored = scoreAndRank(candidates, profile)
        cache.putSongs(RecommendationCache.Keys.BASED_ON_RECENT, scored)
        return scored.take(limit)
    }

    /**
     * Get artist-specific mix recommendations.
     * Fetches songs related to the user's top artists from YouTube Music.
     */
    suspend fun getArtistMixRecommendations(artist: String, limit: Int = 20): List<Song> {
        val cacheKey = "${RecommendationCache.Keys.ARTIST_MIX}_${artist.lowercase().hashCode()}"
        val cached = cache.getSongs(cacheKey)
        if (cached != null) return cached.take(limit)

        val candidates = mutableListOf<Song>()
        try {
            val results = youTubeRepository.search("$artist mix", YouTubeRepository.FILTER_SONGS)
            candidates.addAll(results)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get artist mix for $artist", e)
        }

        if (candidates.size < limit / 2) {
            try {
                val more = youTubeRepository.search(artist, YouTubeRepository.FILTER_SONGS)
                val existingIds = candidates.map { it.id }.toSet()
                more.filter { it.id !in existingIds }.let { candidates.addAll(it) }
            } catch (e: Exception) {
                // Continue
            }
        }

        val profile = tasteProfileBuilder.getProfile()
        val scored = scoreAndRank(candidates, profile)
        cache.putSongs(cacheKey, scored)
        return scored.take(limit)
    }

    /**
     * Get mood-based recommendations that combine YT Music mood data with personal taste.
     */
    suspend fun getMoodRecommendations(mood: String, limit: Int = 20): List<Song> {
        val cacheKey = "${RecommendationCache.Keys.MOOD_PREFIX}${mood.lowercase().hashCode()}"
        val cached = cache.getSongs(cacheKey)
        if (cached != null) return cached.take(limit)

        val candidates = mutableListOf<Song>()
        try {
            val moodSongs = youTubeRepository.search("$mood music", YouTubeRepository.FILTER_SONGS)
            candidates.addAll(moodSongs)
        } catch (e: Exception) {
            // Fallback
        }

        val profile = tasteProfileBuilder.getProfile()
        val scored = scoreAndRank(candidates, profile)
        cache.putSongs(cacheKey, scored)
        return scored.take(limit)
    }

    // ============================================================================================
    // Signal: Notify the engine of user actions
    // ============================================================================================

    /**
     * Call when a song starts playing — invalidates stale caches and updates taste profile.
     */
    fun onSongPlayed(song: Song) {
        tasteProfileBuilder.invalidate()
        cache.invalidateUpNext()
    }

    /**
     * Call when the user skips a song — helps improve future recommendations quality.
     */
    fun onSongSkipped(song: Song) {
        tasteProfileBuilder.invalidate()
    }

    /**
     * Call when the user likes/unlikes a song.
     * Liked songs get a boost; unliked songs lose the boost.
     */
    fun onSongLikeChanged(song: Song, isLiked: Boolean) {
        tasteProfileBuilder.invalidate()
        cache.invalidateRecommendations()
        Log.d(TAG, "Like changed: '${song.title}' by ${song.artist} -> liked=$isLiked")
    }

    /**
     * Call when the user dislikes/un-dislikes a song.
     * Disliked songs are excluded from all recommendations.
     * Also penalizes the artist in future scoring.
     */
    fun onSongDisliked(song: Song, isDisliked: Boolean) {
        if (isDisliked) {
            dislikedSongIds.add(song.id)
            song.artist.trim().lowercase().let { if (it.isNotBlank()) dislikedArtists.add(it) }
        } else {
            dislikedSongIds.remove(song.id)
            // Don't remove artist — one un-dislike shouldn't undo full artist penalty
        }
        tasteProfileBuilder.invalidate()
        cache.invalidateRecommendations()
        cache.invalidateUpNext()
        Log.d(TAG, "Dislike changed: '${song.title}' by ${song.artist} -> disliked=$isDisliked")
    }

    /**
     * Call when the user logs in or out — full cache reset.
     */
    fun onAuthStateChanged() {
        tasteProfileBuilder.invalidate()
        cache.invalidateAll()
        dislikedSongIds.clear()
        dislikedArtists.clear()
    }

    // ============================================================================================
    // PRIVATE — Scoring & Ranking
    // ============================================================================================

    /**
     * Score and rank song candidates using the taste profile.
     *
     * Scoring factors:
     * - Artist affinity: +0.30 weight ("I listen to this artist a lot")
     * - Freshness: +0.15 weight (not recently played = more interesting)
     * - Skip avoidance: -0.15 penalty (frequently skipped songs ranked lower)
     * - Dislike penalty: -HARD EXCLUDE or -0.25 (user explicitly disliked)
     * - Liked boost: +0.10 weight (user explicitly liked this song)
     * - Liked artist boost: +0.10 weight (user liked other songs by this artist)
     * - Time-of-day relevance: +0.10 weight (songs associated with current time)
     * - Variety: -0.10 penalty (over-represented artists in results)
     */
    private fun scoreAndRank(candidates: List<Song>, profile: UserTasteProfile): List<Song> {
        if (!profile.hasEnoughData || candidates.isEmpty()) {
            // Not enough data to personalize — filter disliked, return in YT Music's order
            return candidates.filter { it.id !in dislikedSongIds }
        }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val artistCounts = mutableMapOf<String, Int>() // Track artist frequency for variety

        data class ScoredSong(val song: Song, val score: Float)

        val scored = candidates
            .filter { it.id !in dislikedSongIds } // Hard exclude disliked songs
            .map { song ->
                var score = 0.5f // Base score

                val artistKey = song.artist.trim().lowercase()

                // --- Artist Affinity (0.30) ---
                val artistAffinity = profile.artistAffinities[artistKey] ?: 0f
                score += artistAffinity * 0.30f

                // --- Freshness (0.15) — prefer songs NOT recently played
                val isRecent = song.id in profile.recentSongIds
                if (!isRecent) {
                    score += 0.15f
                } else {
                    val recencyIndex = profile.recentSongIds.indexOf(song.id)
                    score -= 0.08f * (1f - recencyIndex.toFloat() / profile.recentSongIds.size.coerceAtLeast(1))
                }

                // --- Skip Avoidance (-0.15) --- penalize frequently skipped
                if (song.id in profile.frequentlySkippedIds) {
                    score -= 0.15f
                }

                // --- Disliked Artist Penalty (-0.25) ---
                if (artistKey in dislikedArtists || artistKey in profile.dislikedArtists) {
                    score -= 0.25f
                }

                // --- Liked Boost (0.10) --- if this exact song is liked
                if (song.id in profile.likedSongIds) {
                    score += 0.10f
                }

                // --- Liked Artist Boost (0.10) --- if user liked other songs by this artist
                if (artistKey in profile.likedArtists) {
                    score += 0.10f
                }

                // --- Time-of-Day Relevance (0.10) ---
                val timeWeight = profile.timeOfDayWeights[currentHour] ?: 0.5f
                score += timeWeight * 0.10f

                // --- Variety (-0.10) --- penalize over-represented artists in results
                val artistCount = artistCounts.getOrDefault(artistKey, 0)
                artistCounts[artistKey] = artistCount + 1
                if (artistCount > 2) {
                    score -= 0.10f * (artistCount - 2)
                }

                ScoredSong(song, score.coerceIn(0f, 1f))
            }

        return scored.sortedByDescending { it.score }.map { it.song }
    }

    // ============================================================================================
    // PRIVATE — Section Generators
    // ============================================================================================

    private suspend fun fetchQuickPicks(
        profile: UserTasteProfile,
        seenIds: MutableSet<String>
    ): List<Song> {
        val songs = getPersonalizedRecommendations(20)
        songs.forEach { seenIds.add(it.id) }
        return songs
    }

    private suspend fun fetchRecentBased(
        profile: UserTasteProfile,
        seenIds: MutableSet<String>
    ): List<Song> {
        val songs = getRecentBasedSuggestions(15)
        return songs.filter { seenIds.add(it.id) }
    }

    private suspend fun fetchArtistMixes(profile: UserTasteProfile): List<HomeSection> {
        if (!profile.hasEnoughData) return emptyList()

        val topArtists = profile.artistAffinities.keys.take(3)
        val sections = mutableListOf<HomeSection>()

        coroutineScope {
            val jobs = topArtists.map { artist ->
                async(Dispatchers.IO) {
                    try {
                        val songs = getArtistMixRecommendations(artist, 15)
                        if (songs.isNotEmpty()) {
                            val displayName = artist.replaceFirstChar { it.titlecase() }
                            HomeSection(
                                title = "Your $displayName Mix",
                                items = songs.map { HomeItem.SongItem(it) },
                                type = HomeSectionType.HorizontalCarousel
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            jobs.awaitAll().filterNotNull().let { sections.addAll(it) }
        }

        return sections
    }

    private suspend fun fetchDiscoveryMix(
        profile: UserTasteProfile,
        seenIds: MutableSet<String>
    ): List<Song> {
        val cached = cache.getSongs(RecommendationCache.Keys.DISCOVERY_MIX)
        if (cached != null) return cached.filter { seenIds.add(it.id) }

        // Discovery: find songs from artists NOT in the user's top list
        // Use YT Music's own recommendations but filter for unfamiliar artists
        val knownArtists = profile.artistAffinities.keys
        val candidates = mutableListOf<Song>()

        try {
            // Search for discovery-oriented content
            val discoveryQueries = listOf(
                "new music this week",
                "undiscovered gems",
                "rising artists"
            )
            for (query in discoveryQueries) {
                if (candidates.size >= 20) break
                try {
                    val results = youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS)
                    results.forEach { song ->
                        val artistKey = song.artist.trim().lowercase()
                        // Prefer songs from artists the user doesn't already listen to
                        if (artistKey !in knownArtists && seenIds.add(song.id)) {
                            candidates.add(song)
                        }
                    }
                } catch (e: Exception) {
                    // Continue
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build discovery mix", e)
        }

        cache.putSongs(RecommendationCache.Keys.DISCOVERY_MIX, candidates)
        return candidates.take(15)
    }

    private suspend fun fetchForgottenFavorites(profile: UserTasteProfile): List<Song> {
        if (!profile.hasEnoughData) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                // Get songs played often but not recently (> 30 days ago)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                val allHistory = listeningHistoryDao.getAllHistory()
                val forgotten = allHistory
                    .filter { it.playCount >= 3 && it.lastPlayed < thirtyDaysAgo && it.completionRate > 50f }
                    .sortedByDescending { it.playCount }
                    .take(10)

                // Convert to Song objects (reconstruct from history data)
                forgotten.mapNotNull { history ->
                    try {
                        Song.fromYouTube(
                            videoId = history.songId,
                            title = history.songTitle,
                            artist = history.artist,
                            album = history.album,
                            duration = history.duration,
                            thumbnailUrl = history.thumbnailUrl
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun fetchTimeBasedRecommendations(
        profile: UserTasteProfile,
        seenIds: MutableSet<String>
    ): List<Song> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val query = when (hour) {
            in 5..8 -> "morning chill music"
            in 9..11 -> "upbeat morning music"
            in 12..14 -> "afternoon vibes music"
            in 15..17 -> "evening drive music"
            in 18..20 -> "evening relaxing music"
            in 21..23 -> "late night music"
            else -> "late night chill music"
        }

        return try {
            val songs = youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS)
            val profile2 = tasteProfileBuilder.getProfile()
            val scored = scoreAndRank(songs, profile2)
            scored.filter { seenIds.add(it.id) }.take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ============================================================================================
    // PRIVATE — Utilities
    // ============================================================================================

    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning vibes"
            in 12..16 -> "Afternoon picks"
            in 17..20 -> "Evening mood"
            else -> "Late night listening"
        }
    }
}

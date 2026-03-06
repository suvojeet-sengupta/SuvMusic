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
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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
    private val cache: RecommendationCache,
    private val nativeScorer: NativeRecommendationScorer,
    private val songGenreDao: com.suvojeet.suvmusic.core.data.local.dao.SongGenreDao
) {
    companion object {
        private const val TAG = "RecommendationEngine"
        /** Max concurrent YouTube API calls to prevent throttling */
        private const val MAX_CONCURRENT_API_CALLS = 3
    }

    /** Application-scoped coroutine scope with SupervisorJob — survives child failures */
    private val scope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    /** Semaphore to rate-limit parallel YouTube API calls */
    private val apiSemaphore = Semaphore(MAX_CONCURRENT_API_CALLS)

    /**
     * In-memory set of disliked song IDs (synced with DB).
     * Thread-safe: uses ConcurrentHashMap-backed set.
     */
    private val dislikedSongIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * In-memory set of disliked artists (synced with DB).
     * Thread-safe: uses ConcurrentHashMap-backed set.
     */
    private val dislikedArtists: MutableSet<String> = ConcurrentHashMap.newKeySet()

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
     * Generate genre-based discovery sections for the home screen.
     * Creates "Because you like [genre]" sections for the user's top 3 genres.
     * Uses genre affinity vector from the taste profile for accurate genre identification.
     */
    suspend fun getGenreBasedSections(): List<HomeSection> = coroutineScope {
        val cached = cache.getSections(RecommendationCache.Keys.HOME_SECTIONS + "_genre")
        if (cached != null) return@coroutineScope cached

        val profile = tasteProfileBuilder.getProfile()
        if (!profile.hasEnoughData) return@coroutineScope emptyList()

        val topGenres = GenreTaxonomy.topGenres(profile.genreAffinityVector, n = 3)
        if (topGenres.isEmpty()) return@coroutineScope emptyList()

        val sections = mutableListOf<HomeSection>()
        val seenIds = mutableSetOf<String>()
        val seenFingerprints = mutableSetOf<String>()

        val deferredSections = topGenres.map { (genreName, _) ->
            async(Dispatchers.IO) {
                apiSemaphore.withPermit {
                    try {
                        val songs = youTubeRepository.search(
                            "$genreName music mix",
                            YouTubeRepository.FILTER_SONGS
                        )
                        val scored = scoreAndRank(songs, profile)
                        val unique = deduplicate(scored, seenIds, seenFingerprints).take(12)
                        if (unique.isNotEmpty()) {
                            HomeSection(
                                title = "Because you like $genreName",
                                items = unique.map { HomeItem.SongItem(it) },
                                type = HomeSectionType.HorizontalCarousel
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        deferredSections.awaitAll().filterNotNull().let { sections.addAll(it) }
        cache.putSections(RecommendationCache.Keys.HOME_SECTIONS + "_genre", sections)
        sections
    }

    /**
     * Generate context-aware sections based on time-of-day, day-of-week, and listening patterns.
     * Creates sections like "Friday Night Energy", "Weekend Chill", "Your Late Night Mix".
     */
    suspend fun getContextAwareSections(): List<HomeSection> = coroutineScope {
        val cached = cache.getSections(RecommendationCache.Keys.HOME_SECTIONS + "_context")
        if (cached != null) return@coroutineScope cached

        val profile = tasteProfileBuilder.getProfile()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        val sections = mutableListOf<HomeSection>()
        val seenIds = mutableSetOf<String>()
        val seenFingerprints = mutableSetOf<String>()

        // Context query pairs: (title, searchQuery)
        val contextQueries = mutableListOf<Pair<String, String>>()

        // Time-of-day aware sections
        when (hour) {
            in 5..8 -> contextQueries.add("Morning Kickstart" to "upbeat morning motivation music")
            in 9..12 -> contextQueries.add("Focus Flow" to "deep focus concentration music")
            in 13..16 -> contextQueries.add("Afternoon Boost" to "afternoon energy upbeat")
            in 17..20 -> contextQueries.add("Evening Unwind" to "evening relaxing atmospheric music")
            in 21..23 -> contextQueries.add("Night Session" to "late night mellow music")
            else -> contextQueries.add("Late Night Wandering" to "ambient late night calm")
        }

        // Weekend/weekday specific
        if (isWeekend) {
            contextQueries.add("Weekend Vibes" to "weekend party feel good music")
        } else {
            contextQueries.add("Workday Groove" to "productive work music beats")
        }

        val deferredSections = contextQueries.map { (title, query) ->
            async(Dispatchers.IO) {
                apiSemaphore.withPermit {
                    try {
                        val songs = youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS)
                        val scored = scoreAndRank(songs, profile)
                        val unique = deduplicate(scored, seenIds, seenFingerprints).take(12)
                        if (unique.isNotEmpty()) {
                            HomeSection(
                                title = title,
                                items = unique.map { HomeItem.SongItem(it) },
                                type = HomeSectionType.HorizontalCarousel
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        deferredSections.awaitAll().filterNotNull().let { sections.addAll(it) }
        cache.putSections(RecommendationCache.Keys.HOME_SECTIONS + "_context", sections)
        sections
    }

    /**
     * Detect the user's current mood based on recent listening patterns.
     * Analyzes the genre distribution of the last 5-10 plays to infer mood.
     *
     * @return A mood string like "Chill", "Energize", "Sad", "Party", or null if uncertain.
     */
    suspend fun detectCurrentMood(): String? = withContext(Dispatchers.IO) {
        val profile = tasteProfileBuilder.getProfile()
        if (!profile.hasEnoughData) return@withContext null

        val vec = profile.recentGenreVector
        if (!GenreTaxonomy.isNonZero(vec)) return@withContext null

        val topGenres = GenreTaxonomy.topGenres(vec, n = 2)
        if (topGenres.isEmpty()) return@withContext null

        val primary = topGenres[0].first.lowercase()

        // Map genre to mood
        return@withContext when (primary) {
            "lo-fi", "ambient", "jazz", "classical" -> "Chill"
            "r&b", "soul" -> "Romance"
            "edm", "hip-hop", "latin" -> "Party"
            "rock", "metal", "punk" -> "Energize"
            "folk", "country", "indie" -> "Relax"
            "blues" -> "Sad"
            "pop" -> if (topGenres.size > 1 && topGenres[1].first.lowercase() in listOf("edm", "hip-hop", "latin")) "Feel Good" else null
            else -> null
        }
    }

    /**
     * Generate additional sections for scroll-to-load.
     * Each page uses a DIFFERENT strategy and visual style, all taste-profile-driven.
     * Returns nearby-relevant content — songs the user will actually want to listen to.
     *
     * Pages 1-6 produce 2-3 sections each (total ~15 extra sections). Empty after page 6.
     *
     * @param page The page number (1-based)
     * @param existingTitles Titles already shown to avoid duplicates
     */
    suspend fun getMoreSections(
        page: Int,
        existingTitles: Set<String>
    ): List<HomeSection> = coroutineScope {
        if (page > 6) return@coroutineScope emptyList()

        val profile = tasteProfileBuilder.getProfile()
        val seenIds = mutableSetOf<String>()
        val seenFingerprints = mutableSetOf<String>()

        // Gather taste signals for query generation
        val topArtists = profile.artistAffinities.keys.toList()
        val topGenres = GenreTaxonomy.topGenres(profile.genreAffinityVector, n = 5).map { it.first }
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)

        // Each page: list of Triple(title, searchQuery, sectionType)
        val pageContent: List<Triple<String, String, HomeSectionType>> = when (page) {
            1 -> {
                // Page 1 — artist deep-dives from user's top artists
                val artistQueries = mutableListOf<Triple<String, String, HomeSectionType>>()
                topArtists.getOrNull(0)?.let { a ->
                    val name = a.replaceFirstChar { it.titlecase() }
                    artistQueries.add(Triple("Because you listen to $name", "$a best songs", HomeSectionType.QuickPicks))
                }
                topArtists.getOrNull(1)?.let { a ->
                    val name = a.replaceFirstChar { it.titlecase() }
                    artistQueries.add(Triple("$name Radio", "$a similar artists songs", HomeSectionType.HorizontalCarousel))
                }
                // Genre-aligned pick if available
                topGenres.getOrNull(0)?.let { g ->
                    artistQueries.add(Triple("Your $g Favourites", "$g hits best $year", HomeSectionType.Grid))
                }
                artistQueries.ifEmpty {
                    listOf(Triple("Today's Hits", "top hits $year", HomeSectionType.HorizontalCarousel))
                }
            }
            2 -> {
                // Page 2 — deeper genre exploration, different visual styles
                val items = mutableListOf<Triple<String, String, HomeSectionType>>()
                topGenres.getOrNull(1)?.let { g ->
                    items.add(Triple("$g Deep Cuts", "$g deep cuts underrated", HomeSectionType.VerticalList))
                }
                topGenres.getOrNull(2)?.let { g ->
                    items.add(Triple("Fresh $g", "new $g $year releases", HomeSectionType.LargeCardWithList))
                }
                topArtists.getOrNull(2)?.let { a ->
                    val name = a.replaceFirstChar { it.titlecase() }
                    items.add(Triple("More like $name", "$a similar songs", HomeSectionType.HorizontalCarousel))
                }
                items.ifEmpty {
                    listOf(Triple("Chill Vibes", "chill relaxing music", HomeSectionType.VerticalList))
                }
            }
            3 -> {
                // Page 3 — nostalgia + related artists
                val items = mutableListOf<Triple<String, String, HomeSectionType>>()
                topArtists.getOrNull(3)?.let { a ->
                    val name = a.replaceFirstChar { it.titlecase() }
                    items.add(Triple("$name Essentials", "$a top songs all time", HomeSectionType.QuickPicks))
                }
                topGenres.getOrNull(0)?.let { g ->
                    items.add(Triple("Classic $g", "classic $g songs all time best", HomeSectionType.Grid))
                }
                items.add(Triple("Throwback Favourites", buildString {
                    // Use top artist for relevance
                    append(topArtists.take(2).joinToString(" "))
                    append(" throwback songs")
                }, HomeSectionType.HorizontalCarousel))
                items
            }
            4 -> {
                // Page 4 — mood & context, acoustic/live versions
                val items = mutableListOf<Triple<String, String, HomeSectionType>>()
                topArtists.getOrNull(0)?.let { a ->
                    items.add(Triple("${a.replaceFirstChar { it.titlecase() }} — Acoustic", "$a acoustic live", HomeSectionType.VerticalList))
                }
                topGenres.getOrNull(0)?.let { g ->
                    items.add(Triple("$g for the Mood", "$g mood playlist", HomeSectionType.LargeCardWithList))
                }
                items.ifEmpty {
                    listOf(Triple("Acoustic Sessions", "acoustic covers popular songs", HomeSectionType.VerticalList))
                }
            }
            5 -> {
                // Page 5 — sibling artists & blended genres
                val items = mutableListOf<Triple<String, String, HomeSectionType>>()
                topArtists.getOrNull(4)?.let { a ->
                    val name = a.replaceFirstChar { it.titlecase() }
                    items.add(Triple("Fans also like $name", "$a fans also like", HomeSectionType.HorizontalCarousel))
                }
                if (topGenres.size >= 2) {
                    val g1 = topGenres[0]; val g2 = topGenres[1]
                    items.add(Triple("$g1 × $g2 Blend", "$g1 $g2 blend crossover songs", HomeSectionType.Grid))
                }
                topGenres.getOrNull(3)?.let { g ->
                    items.add(Triple("Discover $g", "$g new artists $year", HomeSectionType.QuickPicks))
                }
                items.ifEmpty {
                    listOf(Triple("Genre Blend", "crossover fusion music", HomeSectionType.Grid))
                }
            }
            6 -> {
                // Page 6 — final batch: underrated, long-play, "one last thing"
                val items = mutableListOf<Triple<String, String, HomeSectionType>>()
                topGenres.getOrNull(0)?.let { g ->
                    items.add(Triple("Hidden Gems: $g", "$g hidden gems underrated $year", HomeSectionType.VerticalList))
                }
                items.add(Triple("Long Listens", buildString {
                    append(topGenres.take(2).joinToString(" "))
                    append(" full album long playlist")
                }, HomeSectionType.LargeCardWithList))
                items
            }
            else -> emptyList()
        }

        if (pageContent.isEmpty()) return@coroutineScope emptyList()

        val deferredSections = pageContent
            .filter { (title, _, _) -> title !in existingTitles }
            .map { (title, query, type) ->
                async(Dispatchers.IO) {
                    apiSemaphore.withPermit {
                        try {
                            val songs = youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS)
                            val scored = scoreAndRank(songs, profile)
                            val unique = deduplicate(scored, seenIds, seenFingerprints).take(12)
                            if (unique.isNotEmpty()) {
                                HomeSection(title = title, items = unique.map { HomeItem.SongItem(it) }, type = type)
                            } else null
                        } catch (e: Exception) { null }
                    }
                }
            }

        deferredSections.awaitAll().filterNotNull()
    }

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
                                apiSemaphore.withPermit {
                                    try {
                                        youTubeRepository.getRelatedSongs(songId)
                                    } catch (e: Exception) {
                                        emptyList()
                                    }
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
        
        // 3. Fallback to recent-based suggestions if needed
        if (candidates.size < count) {
            try {
                val recentSuggestions = getRecentBasedSuggestions(count)
                candidates.addAll(deduplicate(recentSuggestions, seenIds, seenFingerprints))
            } catch (e: Exception) {}
        }
        
        // 4. Inject familiarity: random top artist
        if (profile.hasEnoughData && candidates.size < count * 2) {
            try {
                profile.artistAffinities.keys.shuffled().firstOrNull()?.let { artist ->
                    val songs = youTubeRepository.search("$artist best hits", YouTubeRepository.FILTER_SONGS)
                    candidates.addAll(deduplicate(songs, seenIds, seenFingerprints))
                }
            } catch (e: Exception) {}
        }

        // 5. Score, filter skips, and rank
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
        if (candidates.size < count * 2) {
            try {
                val personalRecs = getPersonalizedRecommendations(count * 2)
                candidates.addAll(deduplicate(personalRecs, seenIds, seenFingerprints))
            } catch (e: Exception) {
                Log.w(TAG, "Radio: failed to get personalized recs", e)
            }
        }
        
        // 3. Recent-based suggestions (if still needing more candidates)
        if (candidates.size < count * 2) {
            try {
                val recentSuggestions = getRecentBasedSuggestions(count)
                candidates.addAll(deduplicate(recentSuggestions, seenIds, seenFingerprints))
            } catch (e: Exception) {
                Log.w(TAG, "Radio: failed to get recent based suggestions", e)
            }
        }
        
        // 4. Inject familiarity: random top artist
        if (profile.hasEnoughData && candidates.size < count * 3) {
            try {
                profile.artistAffinities.keys.shuffled().firstOrNull()?.let { artist ->
                    val songs = youTubeRepository.search("$artist best hits", YouTubeRepository.FILTER_SONGS)
                    candidates.addAll(deduplicate(songs, seenIds, seenFingerprints))
                }
            } catch (e: Exception) {}
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
        val seenFingerprints = mutableSetOf<String>()

        coroutineScope {
            val jobs = recentIds.map { songId ->
                async(Dispatchers.IO) {
                    apiSemaphore.withPermit {
                        try {
                            youTubeRepository.getRelatedSongs(songId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
            }
            val results = jobs.awaitAll().flatten()
            candidates.addAll(deduplicate(results, seenIds, seenFingerprints))
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
                val seenIds = candidates.map { it.id }.toMutableSet()
                val seenFingerprints = candidates.map { getSongFingerprint(it) }.toMutableSet()
                candidates.addAll(deduplicate(more, seenIds, seenFingerprints))
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

    fun onSongPlayed(song: Song) {
        tasteProfileBuilder.onSongPlayed() // Rate-limited invalidation (every 5th play)
        cache.invalidateUpNext()
    }

    fun onSongSkipped(song: Song) {
        tasteProfileBuilder.onSongPlayed() // Skips also count toward the play-event counter
    }

    fun onSongLikeChanged(song: Song, isLiked: Boolean) {
        tasteProfileBuilder.invalidateImmediate() // Likes are significant — immediate rebuild
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
        tasteProfileBuilder.invalidateImmediate()
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
        tasteProfileBuilder.invalidateImmediate()
        cache.invalidateAll()
    }

    // ============================================================================================
    // PRIVATE — Scoring & Ranking
    // ============================================================================================

    /**
     * Rebalanced scoring weights (v2, with genre signals).
     *
     * | Signal               | Weight |
     * |----------------------|--------|
     * | Base                 | 0.50   |
     * | Artist Affinity      | 0.22   |
     * | Freshness            | 0.12   |
     * | Skip Avoidance       | 0.12   |
     * | Liked Song           | 0.12   |
     * | Liked Artist         | 0.08   |
     * | Time-of-Day          | 0.08   |
     * | Variety Penalty      | 0.10   |
     * | Genre Similarity     | 0.20   |
     * | Session Genre Sim    | 0.08   |
     * | Skip Genre Penalty   | 0.08   |
     */
    private val scoringWeights = floatArrayOf(
        0.50f,  // [0] base
        0.22f,  // [1] artistAffinity
        0.12f,  // [2] freshness
        0.12f,  // [3] skipPenalty
        0.12f,  // [4] likedSong
        0.08f,  // [5] likedArtist
        0.08f,  // [6] timeOfDay
        0.10f,  // [7] varietyPenalty
        0.20f,  // [8] genreSimilarity
        0.08f,  // [9] recentGenreSimilarity
        0.08f   // [10] skipGenrePenalty
    )

    /**
     * Score and rank candidates using the native SIMD engine (with Kotlin fallback).
     *
     * Pipeline:
     * 1. Filter disliked songs/artists
     * 2. Infer genre vectors for each candidate (cached in Room)
     * 3. Marshal features into SoA FloatArray
     * 4. Single JNI call to native scorer
     * 5. Unpack top-K indices back to Song list
     *
     * Falls back to pure Kotlin scoring if native library is unavailable.
     */
    private suspend fun scoreAndRank(candidates: List<Song>, profile: UserTasteProfile): List<Song> {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Step 1: Filter disliked
        val filtered = candidates.filter {
            it.id !in dislikedSongIds && it.artist.lowercase().trim() !in dislikedArtists
        }

        if (filtered.isEmpty()) return emptyList()

        val N = filtered.size

        // Step 2: Compute variety penalties (order-dependent, must be sequential)
        val artistCounts = mutableMapOf<String, Int>()
        val varietyPenalties = FloatArray(N)
        for (i in 0 until N) {
            val artistKey = filtered[i].artist.trim().lowercase()
            val count = artistCounts.getOrDefault(artistKey, 0)
            artistCounts[artistKey] = count + 1
            varietyPenalties[i] = if (count > 2) (count - 2).toFloat() else 0f
        }

        // Step 3: Compute genre similarities
        val genreSimilarities = computeGenreSimilarities(filtered, profile.genreAffinityVector)
        val recentGenreSimilarities = computeGenreSimilarities(filtered, profile.recentGenreVector)
        val skipGenreSimilarities = computeGenreSimilarities(filtered, profile.skipGenreVector)

        // Step 4: Try native scoring
        if (nativeScorer.isNativeAvailable()) {
            val result = scoreWithNative(
                filtered, profile, currentHour, varietyPenalties,
                genreSimilarities, recentGenreSimilarities, skipGenreSimilarities
            )
            if (result != null) return result
        }

        // Step 5: Kotlin fallback
        return scoreWithKotlin(
            filtered, profile, currentHour, varietyPenalties,
            genreSimilarities, recentGenreSimilarities, skipGenreSimilarities
        )
    }

    /**
     * Native SIMD scoring path. Marshals features into SoA layout and calls C++.
     */
    private fun scoreWithNative(
        candidates: List<Song>,
        profile: UserTasteProfile,
        currentHour: Int,
        varietyPenalties: FloatArray,
        genreSims: FloatArray,
        recentGenreSims: FloatArray,
        skipGenreSims: FloatArray
    ): List<Song>? {
        val N = candidates.size
        val numFeatures = NativeRecommendationScorer.NUM_FEATURES

        // Marshal features into SoA (column-major) FloatArray
        val features = FloatArray(numFeatures * N)

        for (i in 0 until N) {
            val song = candidates[i]
            val artistKey = song.artist.trim().lowercase()

            // Feature 0: Artist affinity
            features[0 * N + i] = profile.artistAffinities[artistKey] ?: 0f
            // Feature 1: Freshness (1 = not recently played)
            features[1 * N + i] = if (song.id in profile.recentSongIds) 0f else 1f
            // Feature 2: Skip flag
            features[2 * N + i] = if (song.id in profile.frequentlySkippedIds) 1f else 0f
            // Feature 3: Liked song
            features[3 * N + i] = if (song.id in profile.likedSongIds) 1f else 0f
            // Feature 4: Liked artist
            features[4 * N + i] = if (artistKey in profile.likedArtists) 1f else 0f
            // Feature 5: Time-of-day weight
            features[5 * N + i] = profile.timeOfDayWeights[currentHour] ?: 0.5f
            // Feature 6: Variety penalty
            features[6 * N + i] = varietyPenalties[i]
            // Feature 7: Genre similarity
            features[7 * N + i] = genreSims[i]
            // Feature 8: Recent genre similarity
            features[8 * N + i] = recentGenreSims[i]
            // Feature 9: Skip genre penalty
            features[9 * N + i] = skipGenreSims[i]
            // Feature 10: Reserved
            features[10 * N + i] = 0f
        }

        val topIndices = nativeScorer.scoreCandidates(
            features = features,
            numCandidates = N,
            weights = scoringWeights,
            topK = N // Return all, sorted
        ) ?: return null

        return topIndices.map { candidates[it] }
    }

    /**
     * Kotlin fallback scoring — identical algorithm to native, used when JNI fails.
     */
    private fun scoreWithKotlin(
        candidates: List<Song>,
        profile: UserTasteProfile,
        currentHour: Int,
        varietyPenalties: FloatArray,
        genreSims: FloatArray,
        recentGenreSims: FloatArray,
        skipGenreSims: FloatArray
    ): List<Song> {
        data class ScoredSong(val song: Song, val score: Float)

        val w = scoringWeights

        val scored = candidates.mapIndexed { i, song ->
            val artistKey = song.artist.trim().lowercase()

            var score = w[0] // base
            score += (profile.artistAffinities[artistKey] ?: 0f) * w[1]
            score += (if (song.id in profile.recentSongIds) 0f else 1f) * w[2]
            score -= (if (song.id in profile.frequentlySkippedIds) 1f else 0f) * w[3]
            score += (if (song.id in profile.likedSongIds) 1f else 0f) * w[4]
            score += (if (artistKey in profile.likedArtists) 1f else 0f) * w[5]
            score += (profile.timeOfDayWeights[currentHour] ?: 0.5f) * w[6]
            score -= varietyPenalties[i] * w[7]
            score += genreSims[i] * w[8]
            score += recentGenreSims[i] * w[9]
            score -= skipGenreSims[i] * w[10]

            ScoredSong(song, score.coerceIn(0f, 1f))
        }

        return scored.sortedByDescending { it.score }.map { it.song }
    }

    /**
     * Compute genre cosine similarities between each candidate and a target genre vector.
     * Uses cached genre vectors from Room + keyword-based inference via GenreTaxonomy.
     */
    private suspend fun computeGenreSimilarities(
        candidates: List<Song>,
        targetVector: FloatArray
    ): FloatArray {
        if (!GenreTaxonomy.isNonZero(targetVector)) {
            return FloatArray(candidates.size) // All zeros — no genre data
        }

        val N = candidates.size
        val dim = GenreTaxonomy.GENRE_COUNT

        // Batch fetch cached genre vectors from Room
        val songIds = candidates.map { it.id }
        val cachedGenres = try {
            songGenreDao.getGenres(songIds).associateBy { it.songId }
        } catch (e: Exception) {
            emptyMap()
        }

        // Build candidate genre vectors (infer missing, cache them)
        val candidateVectors = FloatArray(N * dim)
        val newGenres = mutableListOf<com.suvojeet.suvmusic.core.data.local.entity.SongGenre>()

        for (i in 0 until N) {
            val song = candidates[i]
            val cached = cachedGenres[song.id]
            val genreVec = if (cached != null) {
                cached.toFloatArray()
            } else {
                val inferred = GenreTaxonomy.inferGenreVector(song.title, song.artist)
                if (GenreTaxonomy.isNonZero(inferred)) {
                    newGenres.add(com.suvojeet.suvmusic.core.data.local.entity.SongGenre.fromFloatArray(song.id, inferred))
                }
                inferred
            }
            // Pack into contiguous array for batch operation
            System.arraycopy(genreVec, 0, candidateVectors, i * dim, dim.coerceAtMost(genreVec.size))
        }

        // Persist newly inferred genres
        if (newGenres.isNotEmpty()) {
            try { songGenreDao.insertGenres(newGenres) } catch (_: Exception) { }
        }

        // Try native batch cosine similarity
        val nativeSims = nativeScorer.batchCosineSimilarity(targetVector, candidateVectors, N)
        if (nativeSims != null && nativeSims.size == N) {
            return nativeSims
        }

        // Kotlin fallback: compute individually
        val sims = FloatArray(N)
        for (i in 0 until N) {
            val startIdx = i * dim
            val vec = candidateVectors.copyOfRange(startIdx, startIdx + dim)
            sims[i] = nativeScorer.cosineSimilarity(targetVector, vec)
        }
        return sims
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

    private suspend fun fetchArtistMixes(profile: UserTasteProfile): List<HomeSection> {
        if (!profile.hasEnoughData) return emptyList()

        val topArtists = profile.artistAffinities.keys.take(3)
        val sections = mutableListOf<HomeSection>()

        coroutineScope {
            val jobs = topArtists.map { artist ->
                async(Dispatchers.IO) {
                    apiSemaphore.withPermit {
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
            }
            jobs.awaitAll().filterNotNull().let { sections.addAll(it) }
        }

        return sections
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
                        apiSemaphore.withPermit {
                            try {
                                youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS)
                            } catch (e: Exception) {
                                emptyList()
                            }
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
}

package com.suvojeet.suvmusic.data.repository.youtube.browse

import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.BrowseCategory
import com.suvojeet.suvmusic.core.model.HomeItem
import com.suvojeet.suvmusic.core.model.HomeSection
import com.suvojeet.suvmusic.core.model.HomeSectionType
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeApiClient
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeContentParser
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeJsonParser
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeLocale
import com.suvojeet.suvmusic.data.repository.youtube.playlist.YouTubePlaylistService
import com.suvojeet.suvmusic.data.repository.youtube.search.YouTubeSearchService
import com.suvojeet.suvmusic.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The home screen, mood/genre browsing and recommendation feeds.
 *
 * Logged-in users get YouTube's own personalised shelves; logged-out users get public
 * charts, and failing that a set of search-built shelves picked from their language
 * preferences.
 */
@Singleton
class YouTubeBrowseService @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiClient: YouTubeApiClient,
    private val parser: YouTubeContentParser,
    private val jsonParser: YouTubeJsonParser,
    private val searchService: YouTubeSearchService,
    private val playlistService: YouTubePlaylistService,
    private val networkMonitor: NetworkMonitor,
    private val libraryRepository: LibraryRepository
) {

    suspend fun getRecommendations(): List<Song> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext emptyList()
        if (!sessionManager.isLoggedIn()) return@withContext trendingSearch()

        try {
            val hl = YouTubeLocale.hl(sessionManager)
            val response = apiClient.fetchInternalApi("FEmusic_home", hl = hl)
            val sections = parser.parseHomeSections(response)

            val quickPicks = sections.find { section ->
                QUICK_PICK_TITLES.any { section.title.contains(it, ignoreCase = true) }
            }
            quickPicks?.songs()?.takeIf { it.isNotEmpty() }?.let { return@withContext it }

            sections.firstNotNullOfOrNull { it.songs().takeIf { songs -> songs.isNotEmpty() } }
                ?.let { return@withContext it }

            parser.parseSongs(response).takeIf { it.isNotEmpty() }
                ?.let { return@withContext it }

            playlistService.getLikedMusic()
        } catch (e: Exception) {
            // Last resort: seed from an artist the user follows, then generic trending.
            val followedArtists = libraryRepository.getSavedArtists().first()
            if (followedArtists.isNotEmpty()) {
                try {
                    val artistSongs = searchService.search(followedArtists.random().title, YouTubeSearchService.FILTER_SONGS).take(5)
                    if (artistSongs.isNotEmpty()) return@withContext artistSongs
                } catch (e: Exception) { }
            }
            trendingSearch()
        }
    }

    suspend fun getHomeSections(): List<HomeSection> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext emptyList()
        if (!sessionManager.isLoggedIn()) return@withContext loggedOutHomeSections()

        try {
            val sections = mutableListOf<HomeSection>()
            val hl = YouTubeLocale.hl(sessionManager)

            try {
                sections.addAll(parser.parseChartsSections(apiClient.fetchInternalApi("FEmusic_charts", hl = hl)))
            } catch (e: Exception) {
                // Charts are optional garnish — home itself still has to load.
            }

            val homeResponse = apiClient.fetchInternalApi("FEmusic_home", hl = hl)
            sections.addAll(parser.parseHomeSections(homeResponse))

            var currentJson = JSONObject(homeResponse)
            var continuationToken = jsonParser.extractContinuationToken(currentJson)
            var attempts = 0
            while (continuationToken != null && attempts < 3) {
                try {
                    val continuationResponse = apiClient.fetchInternalApiWithContinuation(continuationToken, hl = hl)
                    if (continuationResponse.isEmpty()) break
                    sections.addAll(parser.parseHomeSections(continuationResponse))
                    currentJson = JSONObject(continuationResponse)
                    continuationToken = jsonParser.extractContinuationToken(currentJson)
                } catch (e: Exception) {
                    break
                }
                attempts++
            }

            sections.add(exploreSection())
            sections.distinctBy { it.title }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun loggedOutHomeSections(): List<HomeSection> {
        val sections = mutableListOf<HomeSection>()
        val preferredLanguages = sessionManager.getPreferredLanguages()
        val hl = YouTubeLocale.hl(sessionManager)
        val gl = java.util.Locale.getDefault().country.ifBlank { DEFAULT_PUBLIC_GL }
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        // Charts and home are independent round-trips; running them sequentially doubles
        // the cold-start wait before anything is on screen.
        coroutineScope {
            val chartsDeferred = async(Dispatchers.IO) {
                runCatching { parser.parseChartsSections(apiClient.fetchPublicApi("FEmusic_charts", hl = hl, gl = gl)) }
                    .getOrDefault(emptyList())
            }
            val homeDeferred = async(Dispatchers.IO) {
                runCatching { parser.parseHomeSections(apiClient.fetchPublicApi("FEmusic_home", hl = hl, gl = gl)) }
                    .getOrDefault(emptyList())
            }
            sections.addAll(chartsDeferred.await())
            sections.addAll(homeDeferred.await())
        }

        if (sections.isNotEmpty()) {
            // Public feeds ignore language taste, so lead with shelves for preferred languages.
            val languageQueries = fallbackShelves(year)
                .filter { (_, _, languages) -> languages.size == 1 && languages.first() in preferredLanguages }
                .take(MAX_LANGUAGE_SHELVES)
                .map { it.first to it.second }
            return (searchShelves(languageQueries) + sections).distinctBy { it.title }
        }

        // Nothing public came back — build shelves out of searches instead, filtered to the
        // languages this user cares about.
        val sectionQueries = if (preferredLanguages.isEmpty()) {
            fallbackShelves(year).take(10).map { it.first to it.second }
        } else {
            fallbackShelves(year)
                .filter { (_, _, languages) -> languages.any { it in preferredLanguages } || languages.size > 2 }
                .map { it.first to it.second }
        }

        sections.addAll(searchShelves(sectionQueries))
        return sections
    }

    // Up to 14 sequential searches used to run before the screen had anything on it.
    private suspend fun searchShelves(queries: List<Pair<String, String>>): List<HomeSection> = coroutineScope {
        queries.map { (title, query) ->
            async(Dispatchers.IO) {
                try {
                    val songs = searchService.search(query, YouTubeSearchService.FILTER_SONGS)
                        .take(10)
                        .map { HomeItem.SongItem(it) }
                    if (songs.isEmpty()) null
                    else HomeSection(title, songs, parser.homeSectionTypeFor(title))
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getBrowseSections(browseId: String): List<HomeSection> = withContext(Dispatchers.IO) {
        try {
            parser.parseHomeSections(apiClient.fetchInternalApi(browseId))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getHomeSectionsForMood(moodTitle: String): List<HomeSection> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext emptyList()
        try {
            val categories = getMoodsAndGenres()
            val targetVariations = MOOD_SYNONYMS[moodTitle.lowercase()] ?: listOf(moodTitle)

            val category = categories.find { cat ->
                targetVariations.any { cat.title.equals(it, ignoreCase = true) }
            } ?: categories.find { cat ->
                targetVariations.any { cat.title.contains(it, ignoreCase = true) }
            }

            if (category != null) {
                if (category.browseId.startsWith(SEARCH_BROWSE_PREFIX)) {
                    val songs = searchService.search(
                        category.browseId.removePrefix(SEARCH_BROWSE_PREFIX),
                        YouTubeSearchService.FILTER_SONGS
                    )
                    if (songs.isNotEmpty()) {
                        return@withContext listOf(
                            HomeSection(
                                title = category.title,
                                items = songs.map { HomeItem.SongItem(it) },
                                type = HomeSectionType.HorizontalCarousel
                            )
                        )
                    }
                }

                val response = apiClient.fetchInternalApi(category.browseId, params = category.params)
                val sections = parser.parseHomeSections(response)
                if (sections.isNotEmpty()) return@withContext sections
            }

            // Playlists make a better fallback than loose songs — they read as curation.
            val playlistResults = searchService.searchPlaylists("$moodTitle music").take(10)
            if (playlistResults.isNotEmpty()) {
                return@withContext listOf(
                    HomeSection(
                        title = "$moodTitle Playlists",
                        items = playlistResults.map {
                            HomeItem.PlaylistItem(
                                PlaylistDisplayItem(
                                    id = it.id,
                                    name = it.title,
                                    url = "https://music.youtube.com/playlist?list=${it.id}",
                                    uploaderName = it.author,
                                    thumbnailUrl = it.thumbnailUrl,
                                    songCount = it.songs.size
                                )
                            )
                        },
                        type = HomeSectionType.HorizontalCarousel
                    )
                )
            }

            val songs = searchService.search("$moodTitle music", YouTubeSearchService.FILTER_SONGS)
            if (songs.isNotEmpty()) {
                return@withContext listOf(
                    HomeSection(
                        title = "$moodTitle Songs",
                        items = songs.map { HomeItem.SongItem(it) },
                        type = HomeSectionType.VerticalList
                    )
                )
            }
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMoodsAndGenres(): List<BrowseCategory> = withContext(Dispatchers.IO) {
        try {
            if (!networkMonitor.isCurrentlyConnected()) return@withContext emptyList()

            val hl = YouTubeLocale.hl(sessionManager)
            val categories = mutableListOf<BrowseCategory>()

            if (sessionManager.isLoggedIn()) {
                val internalJson = apiClient.fetchInternalApi("FEmusic_moods_and_genres", hl = hl)
                if (internalJson.isNotBlank()) categories.addAll(parser.parseMoodsAndGenres(internalJson))
            }

            if (categories.isEmpty()) {
                val publicJson = apiClient.fetchPublicApi("FEmusic_moods_and_genres", hl = hl, gl = DEFAULT_PUBLIC_GL)
                if (publicJson.isNotBlank()) categories.addAll(parser.parseMoodsAndGenres(publicJson))
            }

            if (categories.isEmpty()) return@withContext fallbackMoodCategories()

            categories
                .distinctBy { "${it.browseId}:${it.params ?: ""}:${it.title.lowercase()}" }
                .filter { it.title.isNotBlank() && it.browseId.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMoodCategories()
        }
    }

    suspend fun getCategoryContent(
        browseId: String,
        params: String? = null,
        title: String? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        val fallbackQuery = title?.takeIf { it.isNotBlank() } ?: "latest music"
        try {
            if (!networkMonitor.isCurrentlyConnected()) return@withContext emptyList()

            val hl = YouTubeLocale.hl(sessionManager)

            if (browseId.startsWith(SEARCH_BROWSE_PREFIX)) {
                val query = browseId.removePrefix(SEARCH_BROWSE_PREFIX).ifBlank { title ?: "music" }
                return@withContext searchService.search(query, YouTubeSearchService.FILTER_SONGS)
            }

            var songs = emptyList<Song>()

            if (sessionManager.isLoggedIn()) {
                val internalJson = apiClient.fetchInternalApi(browseId, hl = hl, params = params?.takeIf { it.isNotBlank() })
                if (internalJson.isNotBlank()) songs = parser.parseSongs(internalJson)
            }

            if (songs.isEmpty()) {
                val publicJson = apiClient.fetchPublicApi(browseId, hl = hl, gl = DEFAULT_PUBLIC_GL, params = params?.takeIf { it.isNotBlank() })
                if (publicJson.isNotBlank()) songs = parser.parseSongs(publicJson)
            }

            songs.ifEmpty { searchService.search(fallbackQuery, YouTubeSearchService.FILTER_SONGS) }
        } catch (e: Exception) {
            e.printStackTrace()
            searchService.search(fallbackQuery, YouTubeSearchService.FILTER_SONGS)
        }
    }

    private suspend fun trendingSearch(): List<Song> {
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return searchService.search("trending music $year", YouTubeSearchService.FILTER_SONGS)
    }

    private fun HomeSection.songs(): List<Song> =
        items.filterIsInstance<HomeItem.SongItem>().map { it.song }

    private fun exploreSection(): HomeSection = HomeSection(
        "Explore",
        listOf(
            HomeItem.ExploreItem("New releases", com.suvojeet.suvmusic.R.drawable.ic_music_note, "FEmusic_new_releases"),
            HomeItem.ExploreItem("Charts", com.suvojeet.suvmusic.R.drawable.ic_waveform, "FEmusic_charts"),
            HomeItem.ExploreItem("Moods and genres", com.suvojeet.suvmusic.R.drawable.ic_play, "FEmusic_moods_and_genres"),
            HomeItem.ExploreItem("Podcasts", com.suvojeet.suvmusic.R.drawable.ic_launcher_monochrome, "FEmusic_podcasts")
        ),
        HomeSectionType.ExploreGrid
    )

    private fun fallbackShelves(year: Int): List<Triple<String, String, Set<String>>> = listOf(
        Triple("Trending Now", "trending music $year", setOf("English")),
        Triple("Top Hits", "top hits $year", setOf("English")),
        Triple("Bollywood Hits", "bollywood hits latest", setOf("Hindi")),
        Triple("Bengali Hits", "latest bengali hits", setOf("Bengali")),
        Triple("Pop Hits", "pop hits $year", setOf("English")),
        Triple("Hip Hop & Rap", "hip hop hits $year", setOf("English")),
        Triple("Chill Vibes", "chill lofi beats", setOf("English", "Hindi", "Punjabi", "Spanish")),
        Triple("Punjabi Hits", "punjabi hits latest", setOf("Punjabi")),
        Triple("90s Nostalgia", "90s bollywood hits", setOf("Hindi")),
        Triple("Tamil Hits", "latest tamil hits", setOf("Tamil")),
        Triple("Telugu Hits", "latest telugu hits", setOf("Telugu")),
        Triple("Malayalam Hits", "latest malayalam hits", setOf("Malayalam")),
        Triple("Workout Energy", "workout music energy", setOf("English", "Hindi", "Punjabi", "Spanish")),
        Triple("Romance", "romantic songs love", setOf("English", "Hindi", "Punjabi", "Spanish"))
    )

    private fun fallbackMoodCategories(): List<BrowseCategory> = listOf(
        BrowseCategory(title = "Chill", browseId = "${SEARCH_BROWSE_PREFIX}chill music mix", params = null),
        BrowseCategory(title = "Sleep", browseId = "${SEARCH_BROWSE_PREFIX}sleep music ambient", params = null),
        BrowseCategory(title = "Focus", browseId = "${SEARCH_BROWSE_PREFIX}focus instrumental music", params = null),
        BrowseCategory(title = "Workout", browseId = "${SEARCH_BROWSE_PREFIX}workout motivation songs", params = null),
        BrowseCategory(title = "Party", browseId = "${SEARCH_BROWSE_PREFIX}party hits", params = null),
        BrowseCategory(title = "Romance", browseId = "${SEARCH_BROWSE_PREFIX}romantic songs", params = null),
        BrowseCategory(title = "Lo-fi", browseId = "${SEARCH_BROWSE_PREFIX}lofi beats", params = null),
        BrowseCategory(title = "Trending", browseId = "${SEARCH_BROWSE_PREFIX}trending music", params = null)
    )

    companion object {
        const val SEARCH_BROWSE_PREFIX = "SEARCH::"

        private const val DEFAULT_PUBLIC_GL = "US"
        private const val MAX_LANGUAGE_SHELVES = 3

        private val QUICK_PICK_TITLES = listOf(
            "Quick picks", "Listen again", "Your favorites", "Your top", "Mixed for you", "Made for you"
        )

        /** Our mood chips don't match YouTube's category names one-for-one. */
        private val MOOD_SYNONYMS = mapOf(
            "relax" to listOf("Relax", "Relaxing", "Chill", "Chill out", "Calm"),
            "sad" to listOf("Sad", "Sadness", "Cry", "Blue", "Melancholy"),
            "romance" to listOf("Romance", "Romantic", "Love", "Date night"),
            "party" to listOf("Party", "Partying", "Dance", "Club"),
            "focus" to listOf("Focus", "Concentration", "Study", "Work"),
            "sleep" to listOf("Sleep", "Bedtime", "Dream"),
            "energize" to listOf("Energy", "Energize", "Motivation", "Boost"),
            "feel good" to listOf("Feel Good", "Happy", "Happiness", "Good vibes", "Mood booster"),
            "workout" to listOf("Workout", "Gym", "Fitness", "Sport"),
            "commute" to listOf("Commute", "Driving", "Travel", "On the go")
        )
    }
}

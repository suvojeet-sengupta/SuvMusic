package com.suvojeet.suvmusic.data.repository

import android.util.LruCache
import com.suvojeet.suvmusic.newpipe.NewPipeDownloaderImpl
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.SessionManager.StoredAccount
import com.suvojeet.suvmusic.data.YouTubeAuthUtils
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.ArtistPreview
import com.suvojeet.suvmusic.data.model.BrowseCategory
import com.suvojeet.suvmusic.data.model.Comment
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.data.model.HomeSectionType
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeApiClient
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeJsonParser
import com.suvojeet.suvmusic.data.repository.youtube.search.YouTubeSearchService
import com.suvojeet.suvmusic.data.repository.youtube.streaming.YouTubeStreamingService
import com.suvojeet.suvmusic.util.NetworkMonitor
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching data from YouTube Music.
 * Uses NewPipeExtractor for streams and search, and internal API for browsing/library.
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val jsonParser: YouTubeJsonParser,
    private val apiClient: YouTubeApiClient,
    private val streamingService: YouTubeStreamingService,
    private val searchService: YouTubeSearchService,
    private val networkMonitor: NetworkMonitor,
    private val libraryRepository: LibraryRepository
) {
    companion object {
        private var isInitialized = false
        
        const val FILTER_SONGS = "music_songs"
        const val FILTER_VIDEOS = "music_videos"
        const val FILTER_ALBUMS = "music_albums"
        const val FILTER_PLAYLISTS = "music_playlists"
        const val FILTER_ARTISTS = "music_artists"
    }

    fun isOnline(): Boolean = networkMonitor.isCurrentlyConnected()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    


    // Comments Pagination State
    private var currentCommentsExtractor: org.schabi.newpipe.extractor.comments.CommentsExtractor? = null
    private var currentCommentsPage: org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage<*>? = null
    private var currentVideoIdForComments: String? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeNewPipe()
        }
    }

    private fun initializeNewPipe() {
        if (!isInitialized) {
            try {
                NewPipe.init(NewPipeDownloaderImpl(okHttpClient) { sessionManager.getCookies() ?: "" })
                isInitialized = true
            } catch (e: Exception) {
                isInitialized = true
            }
        }
    }

    /**
     * Fetch user account info (Name, Email, Avatar) from account menu.
     */
    suspend fun fetchAccountInfo(): StoredAccount? = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext null
        try {
            if (!sessionManager.isLoggedIn()) return@withContext null
            val cookies = sessionManager.getCookies() ?: return@withContext null
            
            // Use account_menu endpoint
            val jsonResponse = fetchInternalApi("account/account_menu")
            if (jsonResponse.isBlank()) return@withContext null
            
            val root = JSONObject(jsonResponse)
            
            // Recursive search for activeAccountHeaderRenderer as structure can vary
            val accountHeader = findActiveAccountHeader(root)
            
            if (accountHeader != null) {
                val name = getRunText(accountHeader.optJSONObject("accountName")) ?: "User"
                val email = getRunText(accountHeader.optJSONObject("email")) ?: ""
                
                val thumbnails = accountHeader.optJSONObject("accountPhoto")
                    ?.optJSONArray("thumbnails")
                val avatarUrl = thumbnails?.let { 
                    it.optJSONObject(it.length() - 1)?.optString("url") 
                } ?: ""
                
                return@withContext StoredAccount(
                    name = name,
                    email = email,
                    avatarUrl = avatarUrl,
                    cookies = cookies
                )
            }
            
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findActiveAccountHeader(node: JSONObject): JSONObject? {
        val keys = node.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = node.opt(key)
            
            if (key == "activeAccountHeaderRenderer" && value is JSONObject) {
                return value
            }
            
            if (value is JSONObject) {
                val found = findActiveAccountHeader(value)
                if (found != null) return found
            } else if (value is JSONArray) {
                for (i in 0 until value.length()) {
                    val item = value.optJSONObject(i) ?: continue
                    val found = findActiveAccountHeader(item)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    // ============================================================================================
    // Search & Stream (NewPipe)
    // ============================================================================================

    suspend fun search(query: String, filter: String = FILTER_SONGS): List<Song> {
        if (!networkMonitor.isCurrentlyConnected()) return emptyList()
        return searchService.search(query, filter)
    }

    suspend fun searchArtists(query: String): List<Artist> {
        if (!networkMonitor.isCurrentlyConnected()) return emptyList()
        return searchService.searchArtists(query)
    }

    suspend fun searchPlaylists(query: String): List<Playlist> {
        if (!networkMonitor.isCurrentlyConnected()) return emptyList()
        return searchService.searchPlaylists(query)
    }

    suspend fun searchAlbums(query: String): List<Album> {
        if (!networkMonitor.isCurrentlyConnected()) return emptyList()
        return searchService.searchAlbums(query)
    }

    suspend fun getSearchSuggestions(query: String): List<String> {
        if (!networkMonitor.isCurrentlyConnected()) return emptyList()
        return searchService.getSearchSuggestions(query)
    }

    suspend fun getStreamUrl(videoId: String): String? {
        if (!networkMonitor.isCurrentlyConnected()) return null
        return streamingService.getStreamUrl(videoId)
    }

    suspend fun getVideoStreamUrl(videoId: String, quality: com.suvojeet.suvmusic.data.model.VideoQuality? = null): String? = streamingService.getVideoStreamUrl(videoId, quality)

    suspend fun getVideoStreamResult(videoId: String, quality: com.suvojeet.suvmusic.data.model.VideoQuality? = null) = streamingService.getVideoStreamResult(videoId, quality)

    suspend fun getStreamUrlForDownload(videoId: String): String? = streamingService.getStreamUrlForDownload(videoId)

    suspend fun getSongDetails(videoId: String): Song? = streamingService.getSongDetails(videoId)

    suspend fun getRelatedSongs(videoId: String): List<Song> {
        // Try internal API first (official Up Next/Radio)
        val internalResults = searchService.getRelatedSongs(videoId)
        if (internalResults.isNotEmpty()) {
            return internalResults
        }
        
        // Fallback to extractor related items (NewPipe)
        return streamingService.getRelatedItems(videoId)
    }

    /**
     * Tries to find the official music video ID for a given song.
     * Use this when switching to Video Mode to play the actual video instead of static art track.
     */
    suspend fun getBestVideoId(song: Song): String = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext song.id
        
        // If it's already likely a video (not from "Topic" channel), keep it
        // Note: NewPipe extractor might settle on "Unknown Artist" if not detailed, 
        // but typically Topic channels have " - Topic" suffix or we can check simple heuristics.
        
        // Heuristic: If title contains "Official Video" or "Music Video", trust it.
        if (song.title.contains("Official Video", ignoreCase = true) || 
            song.title.contains("Music Video", ignoreCase = true)) {
            return@withContext song.id
        }
        
        // If it seems to be an audio track, try to find the video
        // We search for "Song Title Artist Name Official Video"
        try {
            val query = "${song.title} ${song.artist} Official Video"
            val results = search(query, FILTER_VIDEOS)
            
            // Return the first video result if available
            return@withContext results.firstOrNull()?.id ?: song.id
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext song.id
        }
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    // ============================================================================================
    // Browsing (Internal API)
    // ============================================================================================

    suspend fun getRecommendations(): List<Song> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext emptyList()
        if (!sessionManager.isLoggedIn()) {
            return@withContext search("trending music 2024", FILTER_SONGS)
        }
        try {
            val jsonResponse = fetchInternalApi("FEmusic_home")
            
            // Try to find "Quick picks" or "Listen again" section specifically for the home grid
            val sections = parseHomeSectionsFromInternalJson(jsonResponse)
            val quickPicks = sections.find { 
                it.title.contains("Quick picks", ignoreCase = true) || 
                it.title.contains("Listen again", ignoreCase = true) ||
                it.title.contains("Your favorites", ignoreCase = true) ||
                it.title.contains("Your top", ignoreCase = true) ||
                it.title.contains("Mixed for you", ignoreCase = true) ||
                it.title.contains("Made for you", ignoreCase = true)
            }
            
            if (quickPicks != null) {
                val songs = quickPicks.items.filterIsInstance<HomeItem.SongItem>().map { it.song }
                if (songs.isNotEmpty()) return@withContext songs
            }
            
            // Fallback to any section that has songs
            val firstSongSection = sections.find { it.items.any { item -> item is HomeItem.SongItem } }
            if (firstSongSection != null) {
                val songs = firstSongSection.items.filterIsInstance<HomeItem.SongItem>().map { it.song }
                if (songs.isNotEmpty()) return@withContext songs
            }

            // Fallback to generic parsing if sections didn't work
            val items = parseSongsFromInternalJson(jsonResponse)
            if (items.isNotEmpty()) return@withContext items
            
            getLikedMusic()
        } catch (e: Exception) {
            // Fallback: Mix functionality
            // Try to get some recommendations based on followed artists
            val followedArtists = libraryRepository.getSavedArtists().first()
            if (followedArtists.isNotEmpty()) {
                val randomArtist = followedArtists.random()
                try {
                    val artistSongs = search(randomArtist.title, FILTER_SONGS).take(5)
                    if (artistSongs.isNotEmpty()) return@withContext artistSongs
                } catch (e: Exception) {
                    // Ignore
                }
            }
            search("trending music 2024", FILTER_SONGS)
        }
    }

    suspend fun getHomeSections(): List<HomeSection> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext emptyList()
        if (!sessionManager.isLoggedIn()) {
            // Try to fetch public YouTube Music content without auth
            val sections = mutableListOf<HomeSection>()
            
            // 1. Try fetching public trending/charts (works without auth)
            try {
                val chartsResponse = fetchPublicApi("FEmusic_charts")
                if (chartsResponse.isNotEmpty()) {
                    sections.addAll(parseChartsSectionsFromJson(chartsResponse))
                }
            } catch (e: Exception) {
                // Charts failed, continue
            }
            
            // 2. Try fetching public home browse (may work without auth for some content)
            try {
                val homeResponse = fetchPublicApi("FEmusic_home")
                if (homeResponse.isNotEmpty()) {
                    val homeSections = parseHomeSectionsFromInternalJson(homeResponse)
                    sections.addAll(homeSections)
                }
            } catch (e: Exception) {
                // Home failed, continue
            }
            
            // 3. If we got sections from public API, return them
            if (sections.isNotEmpty()) {
                return@withContext sections.distinctBy { it.title }
            }
            
            // 4. Fallback: Rich content via search for non-logged in users
            val preferredLanguages = sessionManager.getPreferredLanguages()
            
            // Define all available sections with their associated languages
            val allSections = listOf(
                Triple("Trending Now", "trending music 2024", setOf("English")),
                Triple("Top Hits", "top hits 2024", setOf("English")),
                Triple("Bollywood Hits", "bollywood hits latest", setOf("Hindi")),
                Triple("Bengali Hits", "latest bengali hits", setOf("Bengali")),
                Triple("Pop Hits", "pop hits 2024", setOf("English")),
                Triple("Hip Hop & Rap", "hip hop hits 2026", setOf("English")),
                Triple("Chill Vibes", "chill lofi beats", setOf("English", "Hindi", "Punjabi", "Spanish")), // Universal
                Triple("Punjabi Hits", "punjabi hits latest", setOf("Punjabi")),
                Triple("90s Nostalgia", "90s bollywood hits", setOf("Hindi")),
                Triple("Tamil Hits", "latest tamil hits", setOf("Tamil")),
                Triple("Telugu Hits", "latest telugu hits", setOf("Telugu")),
                Triple("Malayalam Hits", "latest malayalam hits", setOf("Malayalam")),
                Triple("Workout Energy", "workout music energy", setOf("English", "Hindi", "Punjabi", "Spanish")),
                Triple("Romance", "romantic songs love", setOf("English", "Hindi", "Punjabi", "Spanish"))
            )
            
            // Filter sections based on preference
            val sectionQueries = if (preferredLanguages.isEmpty()) {
                // Default mix if no preference
                allSections.take(10).map { it.first to it.second }
            } else {
                allSections.filter { (_, _, languages) ->
                    // Include if section matches ANY of the preferred languages
                    languages.any { it in preferredLanguages } || languages.size > 2 // Keep universal sections
                }.map { it.first to it.second }
            }
            
            for ((title, query) in sectionQueries) {
                try {
                    val songs = search(query, FILTER_SONGS).take(10)
                        .map { HomeItem.SongItem(it) }
                    if (songs.isNotEmpty()) {
                        val type = when {
                            title.contains("Quick picks", ignoreCase = true) -> HomeSectionType.VerticalList
                            title.contains("Fresh finds", ignoreCase = true) -> HomeSectionType.Grid
                            title.contains("Community", ignoreCase = true) -> HomeSectionType.LargeCardWithList
                            title.contains("Trending", ignoreCase = true) -> HomeSectionType.HorizontalCarousel
                             else -> HomeSectionType.HorizontalCarousel
                        }
                        sections.add(HomeSection(title, songs, type))
                    }
                } catch (e: Exception) {
                    // Skip failed section
                }
            }
            
            return@withContext sections
        }

        try {
            val sections = mutableListOf<HomeSection>()
            
            // 1. Fetch Trending/Charts first to put at top
            try {
                val chartsResponse = fetchInternalApi("FEmusic_charts")
                sections.addAll(parseChartsSectionsFromJson(chartsResponse))
            } catch (e: Exception) {
                // Charts failed, continue to home
            }

            // 2. Fetch Home personalized sections
            val homeResponse = fetchInternalApi("FEmusic_home")
            sections.addAll(parseHomeSectionsFromInternalJson(homeResponse))
            
            // 3. Handle Pagination (Continuations) to get MORE sections
            var currentJson = JSONObject(homeResponse)
            var continuationToken = extractContinuationToken(currentJson)
            var attempts = 0
            
            while (continuationToken != null && attempts < 3) {
                try {
                    val continuationResponse = fetchInternalApiWithContinuation(continuationToken)
                    if (continuationResponse.isNotEmpty()) {
                        val newSections = parseHomeSectionsFromInternalJson(continuationResponse)
                        sections.addAll(newSections)
                        
                        currentJson = JSONObject(continuationResponse)
                        continuationToken = extractContinuationToken(currentJson)
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    break
                }
                attempts++
            }
            
            // Append Explore Section at the end
            sections.add(getExploreSection())

            return@withContext sections.distinctBy { it.title }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getHomeSectionsForMood(moodTitle: String): List<HomeSection> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext emptyList()
        try {
            // 1. Get all moods categories
            val categories = getMoodsAndGenres()
            
            // 2. Find matching category with improved mapping
            // Map our specific chips to potential YouTube Music category titles
            val moodMap = mapOf(
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

            // Get variations for the requested mood
            val targetVariations = moodMap[moodTitle.lowercase()] ?: listOf(moodTitle)
            
            // Try to find a category that matches any of the variations
            var category = categories.find { cat -> 
                targetVariations.any { variation -> 
                    cat.title.equals(variation, ignoreCase = true) 
                }
            }
            
            // Debug/Fallback: if still null, try partial match for "Sad" (e.g. "Sad songs")
            if (category == null) {
                 category = categories.find { cat -> 
                    targetVariations.any { variation -> 
                        cat.title.contains(variation, ignoreCase = true)
                    }
                }
            }
            
            if (category != null) {
                // 3. Fetch specific mood page
                val json = if (category.params != null) {
                    fetchInternalApiWithParams(category.browseId, category.params)
                } else {
                    fetchInternalApi(category.browseId)
                }
                return@withContext parseHomeSectionsFromInternalJson(json)
            }
            
            // 4. Fallback: Search (High accuracy fallback)
            // Use "Playlist" filter to get collections if direct category fails, often better than songs
             val playlistResults = searchPlaylists("$moodTitle music").take(10)
             if (playlistResults.isNotEmpty()) {
                 return@withContext listOf(
                     HomeSection(
                         title = "$moodTitle Playlists",
                         items = playlistResults.map { HomeItem.PlaylistItem(PlaylistDisplayItem(
                             id = it.id,
                             name = it.title,
                             url = "https://music.youtube.com/playlist?list=${it.id}",
                             uploaderName = it.author,
                             thumbnailUrl = it.thumbnailUrl,
                             songCount = it.songs.size
                         )) },
                         type = HomeSectionType.HorizontalCarousel
                     )
                 )
             }

             // Last resort: Songs
             val songs = search("$moodTitle music", FILTER_SONGS)
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

    private fun extractContinuationToken(json: JSONObject): String? = jsonParser.extractContinuationToken(json)

    private fun fetchInternalApiWithContinuation(continuationToken: String): String =
        apiClient.fetchInternalApiWithContinuation(continuationToken)

    private fun parseChartsSectionsFromJson(json: String): List<HomeSection> {
        val sections = mutableListOf<HomeSection>()
        try {
            val root = JSONObject(json)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val sectionObj = contents.optJSONObject(i)
                    
                    // Charts can have musicCarouselShelfRenderer or musicShelfRenderer (for lists)
                    val carouselShelf = sectionObj?.optJSONObject("musicCarouselShelfRenderer")
                    if (carouselShelf != null) {
                        val title = getRunText(carouselShelf.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")?.optJSONObject("title")) ?: ""
                        
                        // We only want "Trending" or "Top songs" for songs
                        if (title.contains("Trending", ignoreCase = true) || title.contains("Top songs", ignoreCase = true)) {
                            val itemsArray = carouselShelf.optJSONArray("contents")
                            val items = mutableListOf<HomeItem>()

                            if (itemsArray != null) {
                                for (j in 0 until itemsArray.length()) {
                                    val itemObj = itemsArray.optJSONObject(j)
                                    parseHomeItem(itemObj)?.let { items.add(it) }
                                }
                            }

                            if (items.isNotEmpty()) {
                                val type = when {
                                    title.contains("Quick picks", ignoreCase = true) -> HomeSectionType.VerticalList
                                    title.contains("Fresh finds", ignoreCase = true) -> HomeSectionType.Grid
                                    title.contains("Community", ignoreCase = true) -> HomeSectionType.LargeCardWithList
                                    else -> HomeSectionType.HorizontalCarousel
                                }
                                sections.add(HomeSection(title, items, type))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sections
    }

    suspend fun getBrowseSections(browseId: String): List<HomeSection> = withContext(Dispatchers.IO) {
        try {
            val jsonResponse = fetchInternalApi(browseId)
            parseHomeSectionsFromInternalJson(jsonResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getUserPlaylists(): List<PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            return@withContext sessionManager.getCachedLibraryPlaylistsSync()
        }

        val playlists = mutableListOf<PlaylistDisplayItem>()

        if (sessionManager.isLoggedIn()) {
            try {
                val jsonResponse = fetchInternalApi("FEmusic_liked_playlists")
                val ytPlaylists = parsePlaylistsFromInternalJson(jsonResponse)
                playlists.addAll(ytPlaylists)
                
                // Cache the playlists for offline use
                if (ytPlaylists.isNotEmpty()) {
                    sessionManager.saveLibraryPlaylistsCache(ytPlaylists)
                    
                    // Also save to Room for persistent storage
                    ytPlaylists.forEach { item ->
                        libraryRepository.savePlaylist(
                            Playlist(
                                id = item.id,
                                title = item.name,
                                author = item.uploaderName,
                                thumbnailUrl = item.thumbnailUrl,
                                songs = emptyList() // We don't have songs here, but savePlaylist will save metadata
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        playlists
    }

    suspend fun getLikedMusic(fetchAll: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        // Offline-First: Always try to return local data first, unless it's empty
        val localSongs = libraryRepository.getCachedPlaylistSongs("LM")
        if (localSongs.isNotEmpty()) {
            return@withContext localSongs
        }

        // If no local data and we are online, try to sync
        if (networkMonitor.isCurrentlyConnected() && sessionManager.isLoggedIn()) {
            syncLikedSongs(fetchAll)
            return@withContext libraryRepository.getCachedPlaylistSongs("LM")
        }
        
        return@withContext emptyList()
    }

    suspend fun syncLikedSongs(fetchAll: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected() || !sessionManager.isLoggedIn()) return@withContext false
        
        try {
            // Incremental Sync Strategy:
            // 1. Clear old data to avoid duplicates/stale data
            // 2. Fetch pages and append immediately to DB -> Triggers UI Flow updates
            
            // Clear existing Liked Songs
            libraryRepository.removePlaylist("LM")
            
            val initialResponse = fetchInternalApi("FEmusic_liked_videos")
            val initialSongs = parseSongsFromInternalJson(initialResponse)
            var totalSongsAdded = 0
            
            if (initialSongs.isNotEmpty()) {
                // Save Playlist Metadata first so the card appears/updates
                libraryRepository.savePlaylist(
                    Playlist(
                        id = "LM",
                        title = "Your Likes",
                        author = "You",
                        thumbnailUrl = initialSongs.first().thumbnailUrl,
                        songs = emptyList() // Metadata only
                    )
                )
                
                // Append first batch
                libraryRepository.appendPlaylistSongs("LM", initialSongs, totalSongsAdded)
                totalSongsAdded += initialSongs.size
                
                if (fetchAll) {
                    var currentJson = JSONObject(initialResponse)
                    var continuationToken = extractContinuationToken(currentJson)
                    var pageCount = 0
                    val maxPages = 500 // Limit to ~50k songs

                    while (continuationToken != null && pageCount < maxPages) {
                        try {
                            val continuationResponse = fetchInternalApiWithContinuation(continuationToken)
                            if (continuationResponse.isNotEmpty()) {
                                val newSongs = parseSongsFromInternalJson(continuationResponse)
                                if (newSongs.isEmpty()) break
                                
                                // Append immediately
                                libraryRepository.appendPlaylistSongs("LM", newSongs, totalSongsAdded)
                                totalSongsAdded += newSongs.size
                                
                                currentJson = JSONObject(continuationResponse)
                                continuationToken = extractContinuationToken(currentJson)
                                pageCount++
                            } else {
                                break
                            }
                        } catch (e: Exception) {
                            break
                        }
                    }
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun removeFromLikedCache(songId: String) {
        libraryRepository.removeSongFromPlaylist("LM", songId)
    }
    
    suspend fun getPlaylist(playlistId: String): Playlist = withContext(Dispatchers.IO) {
        // Fallback to cache if offline
        if (!networkMonitor.isCurrentlyConnected()) {
            val cachedSongs = libraryRepository.getCachedPlaylistSongs(playlistId)
            if (cachedSongs.isNotEmpty()) {
                val cachedPlaylist = libraryRepository.getPlaylistById(playlistId)
                return@withContext Playlist(
                    id = playlistId,
                    title = cachedPlaylist?.title ?: "Offline Playlist",
                    author = cachedPlaylist?.subtitle ?: "",
                    thumbnailUrl = cachedPlaylist?.thumbnailUrl,
                    songs = cachedSongs
                )
            }
        }

        // Handle special playlists that require authentication
        if (playlistId == "LM" || playlistId == "VLLM") {
            // Use internal API for Liked Music if logged in
            if (sessionManager.isLoggedIn()) {
                try {
                    val songs = mutableListOf<Song>()
                    var json = fetchInternalApi("FEmusic_liked_videos")
                    songs.addAll(parseSongsFromInternalJson(json))
                    
                    // Add Pagination for your likes
                    var currentJson = JSONObject(json)
                    var continuationToken = extractContinuationToken(currentJson)
                    var pageCount = 0
                    while (continuationToken != null && pageCount < 200) { // Limit to 20000 songs
                        val continuationResponse = fetchInternalApiWithContinuation(continuationToken)
                        if (continuationResponse.isEmpty()) break
                        val newSongs = parseSongsFromInternalJson(continuationResponse)
                        if (newSongs.isEmpty()) break
                        songs.addAll(newSongs)
                        currentJson = JSONObject(continuationResponse)
                        continuationToken = extractContinuationToken(currentJson)
                        pageCount++
                    }

                    if (songs.isNotEmpty()) {
                        val playlist = Playlist(
                            id = playlistId,
                            title = "Your Likes",
                            author = "You",
                            thumbnailUrl = songs.firstOrNull()?.thumbnailUrl,
                            songs = songs
                        )
                        libraryRepository.savePlaylist(playlist)
                        return@withContext playlist
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Return empty playlist if not logged in or API fails
            return@withContext Playlist(
                id = playlistId,
                title = "Your Likes",
                author = "You",
                thumbnailUrl = null,
                songs = emptyList()
            )
        }
        
        // Handle My Supermix (auto-generated radio)
        if (playlistId == "RTM" || playlistId == "RDTMAK") {
            if (sessionManager.isLoggedIn()) {
                try {
                    // Supermix is based on recommendations
                    val json = fetchInternalApi("FEmusic_home")
                    val songs = parseSongsFromInternalJson(json)
                    if (songs.isNotEmpty()) {
                        val playlist = Playlist(
                            id = playlistId,
                            title = "My Supermix",
                            author = "YouTube Music",
                            thumbnailUrl = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked_music_@576.png",
                            songs = songs.take(50) // Limit to 50 songs
                        )
                        // Don't auto-save Supermix
                        // libraryRepository.savePlaylist(playlist) 
                        return@withContext playlist
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return@withContext Playlist(
                id = playlistId,
                title = "My Supermix",
                author = "YouTube Music",
                thumbnailUrl = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked_music_@576.png",
                songs = emptyList()
            )
        }

        try {
            // Try internal API first for better metadata
            val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
            val json = fetchInternalApi(browseId)
            val playlist = parsePlaylistFromInternalJson(json, playlistId)
            
            // Add Pagination for playlists
            val songs = playlist.songs.toMutableList()
            var currentJson = JSONObject(json)
            var continuationToken = extractContinuationToken(currentJson)
            var pageCount = 0
            while (continuationToken != null && pageCount < 100) { // Limit to 10000 songs
                val continuationResponse = fetchInternalApiWithContinuation(continuationToken)
                if (continuationResponse.isEmpty()) break
                val newSongs = parseSongsFromInternalJson(continuationResponse)
                if (newSongs.isEmpty()) break
                songs.addAll(newSongs)
                currentJson = JSONObject(continuationResponse)
                continuationToken = extractContinuationToken(currentJson)
                pageCount++
            }

            if (songs.isNotEmpty()) {
                val finalPlaylist = playlist.copy(songs = songs.distinctBy { it.id })
                // libraryRepository.savePlaylist(finalPlaylist)
                return@withContext finalPlaylist
            }
        } catch(e: Exception) { }

        // Fallback to NewPipe for public playlists
        try {
            val urlId = if (playlistId.startsWith("VL")) playlistId.removePrefix("VL") else playlistId
            val playlistUrl = "https://www.youtube.com/playlist?list=$urlId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext Playlist(playlistId, "Error", "", null, emptyList())
            
            val playlistExtractor = ytService.getPlaylistExtractor(playlistUrl)
            playlistExtractor.fetchPage()
            
            // Get playlist metadata with proper method calls
            val playlistName = try { playlistExtractor.getName() } catch (e: Exception) { null }
            val uploaderName = try { playlistExtractor.getUploaderName() } catch (e: Exception) { null }
            val description = try { (playlistExtractor as? org.schabi.newpipe.extractor.playlist.PlaylistExtractor)?.description?.content } catch (e: Exception) { null }
            val thumbnailUrl = try { 
                playlistExtractor.thumbnails?.lastOrNull()?.url 
            } catch (e: Exception) { null }
            
            val songs = mutableListOf<Song>()
            var page: org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage<StreamInfoItem>? = playlistExtractor.initialPage
            
            while (page != null) {
                val pageSongs = page.items
                    .filterIsInstance<StreamInfoItem>()
                    .mapNotNull { item ->
                        val videoId = extractVideoId(item.url)
                        val itemThumbnail = item.thumbnails?.lastOrNull()?.url
                            ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                        
                        Song.fromYouTube(
                            videoId = videoId,
                            title = item.name ?: "Unknown",
                            artist = item.uploaderName ?: "Unknown Artist",
                            album = playlistName ?: "",
                            duration = item.duration * 1000L,
                            thumbnailUrl = itemThumbnail,
                            isMembersOnly = false
                        )
                    }
                songs.addAll(pageSongs)
                
                if (page.hasNextPage()) {
                    page = playlistExtractor.getPage(page.nextPage)
                } else {
                    page = null
                }
                
                // Safety break for extremely large playlists in fallback mode
                if (songs.size > 5000) break
            }
            
            if (songs.isNotEmpty()) {
                val playlist = Playlist(
                    id = playlistId,
                    title = playlistName ?: songs.firstOrNull()?.album?.takeIf { it.isNotBlank() } ?: "Playlist",
                    author = uploaderName?.takeIf { it.isNotBlank() } ?: "",
                    thumbnailUrl = thumbnailUrl ?: songs.firstOrNull()?.thumbnailUrl,
                    songs = songs,
                    description = description
                )
                // libraryRepository.savePlaylist(playlist)
                return@withContext playlist
            }
            return@withContext Playlist(
                id = playlistId,
                title = playlistName ?: "Playlist",
                author = uploaderName ?: "",
                thumbnailUrl = thumbnailUrl,
                songs = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
             // Final fallback: try cache one last time
            val cachedSongs = libraryRepository.getCachedPlaylistSongs(playlistId)
            if (cachedSongs.isNotEmpty()) {
                return@withContext Playlist(playlistId, "Cached Playlist", "", null, cachedSongs)
            }
            Playlist(playlistId, "Error loading playlist", "", null, emptyList())
        }
    }

    suspend fun getArtist(browseId: String): Artist? = withContext(Dispatchers.IO) {
        try {
            val json = fetchInternalApi(browseId)
            val artist = parseArtistFromInternalJson(json, browseId)
            
            // Check local subscription state to merge with remote state
            val isLocalSubscribed = libraryRepository.isArtistSaved(browseId).first()
            
            artist.copy(isSubscribed = artist.isSubscribed || isLocalSubscribed)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAlbum(browseId: String): Album? = withContext(Dispatchers.IO) {
        try {
            // Album IDs from search are in OLAK format (e.g., OLAK5uy...), which needs VL prefix
            // Album IDs from home/browse are in MPRE format (e.g., MPREb...)
            val effectiveBrowseId = when {
                browseId.startsWith("OLAK") -> "VL$browseId"
                browseId.startsWith("MPRE") -> browseId
                browseId.startsWith("VL") -> browseId
                else -> browseId
            }
            
            val json = fetchInternalApi(effectiveBrowseId)
            val album = parseAlbumFromInternalJson(json, browseId)
            
            // If parsing returned empty songs and we used VL prefix, album might work differently
            // Return the album regardless - UI will show empty state if needed
            album
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: try with original ID if the modified one failed
            try {
                val json = fetchInternalApi(browseId)
                parseAlbumFromInternalJson(json, browseId)
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }

    /**
     * Get moods and genres (browse categories) from YouTube Music.
     * Used for the Apple Music-style search browse grid.
     */
    suspend fun getMoodsAndGenres(): List<BrowseCategory> = withContext(Dispatchers.IO) {
        try {
            val json = fetchInternalApi("FEmusic_moods_and_genres")
            parseMoodsAndGenresFromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get songs/content for a specific mood/genre category.
     */
    suspend fun getCategoryContent(browseId: String, params: String? = null): List<Song> = withContext(Dispatchers.IO) {
        try {
            val json = if (params != null) {
                fetchInternalApiWithParams(browseId, params)
            } else {
                fetchInternalApi(browseId)
            }
            parseSongsFromInternalJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getLibraryArtists(): List<Artist> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) return@withContext emptyList()
        try {
            // FEmusic_library_corpus_track_artists contains artists from songs in your library
            val json = fetchInternalApi("FEmusic_library_corpus_track_artists")
            parseLibraryArtistsFromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseLibraryArtistsFromJson(json: String): List<Artist> {
        val artists = mutableListOf<Artist>()
        try {
            val root = JSONObject(json)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val item = contents.optJSONObject(i)
                    val gridItems = item?.optJSONObject("gridRenderer")?.optJSONArray("items")
                        ?: item?.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")

                    if (gridItems != null) {
                        for (j in 0 until gridItems.length()) {
                            val gridItem = gridItems.optJSONObject(j)
                            val musicStatsRenderer = gridItem?.optJSONObject("musicTwoRowItemRenderer")
                            
                            if (musicStatsRenderer != null) {
                                val titleObj = musicStatsRenderer.optJSONObject("title")
                                val artistName = getRunText(titleObj) ?: continue
                                
                                val navEndpoint = musicStatsRenderer.optJSONObject("navigationEndpoint")
                                val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId") ?: ""
                                
                                val thumbnailRenderer = musicStatsRenderer.optJSONObject("thumbnailRenderer")
                                val thumbnails = thumbnailRenderer?.optJSONObject("musicThumbnailRenderer")
                                    ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                
                                val thumbnailUrl = thumbnails?.let { 
                                    it.optJSONObject(it.length() - 1)?.optString("url") 
                                }

                                val subtitleObj = musicStatsRenderer.optJSONObject("subtitle")
                                val subscriberCount = getRunText(subtitleObj)

                                if (browseId.isNotEmpty()) {
                                    artists.add(Artist(
                                        id = browseId,
                                        name = artistName,
                                        thumbnailUrl = thumbnailUrl,
                                        subscribers = subscriberCount
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return artists
    }

    /**
     * Get albums from user's library.
     */
    suspend fun getLibraryAlbums(): List<Album> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) return@withContext emptyList()
        try {
            val json = fetchInternalApi("FEmusic_library_corpus_track_albums")
            parseLibraryAlbumsFromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseLibraryAlbumsFromJson(json: String): List<Album> {
        val albums = mutableListOf<Album>()
        try {
            val root = JSONObject(json)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val item = contents.optJSONObject(i)
                    val gridItems = item?.optJSONObject("gridRenderer")?.optJSONArray("items")
                        ?: item?.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")

                    if (gridItems != null) {
                        for (j in 0 until gridItems.length()) {
                            val gridItem = gridItems.optJSONObject(j)
                            val musicStatsRenderer = gridItem?.optJSONObject("musicTwoRowItemRenderer")
                            
                            if (musicStatsRenderer != null) {
                                val titleObj = musicStatsRenderer.optJSONObject("title")
                                val albumName = getRunText(titleObj) ?: continue
                                
                                val navEndpoint = musicStatsRenderer.optJSONObject("navigationEndpoint")
                                val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId") ?: ""
                                
                                val thumbnailRenderer = musicStatsRenderer.optJSONObject("thumbnailRenderer")
                                val thumbnails = thumbnailRenderer?.optJSONObject("musicThumbnailRenderer")
                                    ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                
                                val thumbnailUrl = thumbnails?.let { 
                                    it.optJSONObject(it.length() - 1)?.optString("url") 
                                }

                                val subtitleObj = musicStatsRenderer.optJSONObject("subtitle")
                                val subtitle = getRunText(subtitleObj) ?: ""
                                
                                // Subtitle often contains Artist Name • Year
                                val parts = subtitle.split("•").map { it.trim() }
                                val artistName = parts.firstOrNull() ?: ""
                                val year = parts.getOrNull(1) ?: ""

                                if (browseId.isNotEmpty()) {
                                    albums.add(Album(
                                        id = browseId,
                                        title = albumName,
                                        artist = artistName,
                                        thumbnailUrl = thumbnailUrl,
                                        year = year,
                                        songs = emptyList() // Details fetched separately
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return albums
    }


    private fun fetchInternalApiWithParams(browseId: String, params: String): String =
        apiClient.fetchInternalApiWithParams(browseId, params)

    private fun parseMoodsAndGenresFromJson(json: String): List<com.suvojeet.suvmusic.data.model.BrowseCategory> {
        val categories = mutableListOf<com.suvojeet.suvmusic.data.model.BrowseCategory>()
        try {
            val root = JSONObject(json)
            
            // Navigate to the grid items
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            
            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val section = contents.optJSONObject(i)
                    
                    // Look for grid renderer
                    val gridItems = section?.optJSONObject("gridRenderer")?.optJSONArray("items")
                    
                    if (gridItems != null) {
                        for (j in 0 until gridItems.length()) {
                            val item = gridItems.optJSONObject(j)
                            val categoryRenderer = item?.optJSONObject("musicNavigationButtonRenderer")
                            
                            if (categoryRenderer != null) {
                                val title = getRunText(categoryRenderer.optJSONObject("buttonText"))
                                    ?: continue
                                
                                val clickEndpoint = categoryRenderer.optJSONObject("clickCommand")
                                    ?.optJSONObject("browseEndpoint")
                                
                                val browseId = clickEndpoint?.optString("browseId") ?: continue
                                val params = clickEndpoint.optString("params").takeIf { it.isNotEmpty() }
                                
                                // Extract color from solid background
                                val colorValue = categoryRenderer.optJSONObject("solid")
                                    ?.optJSONObject("leftStripeColor")
                                    ?.optLong("value")
                                
                                categories.add(
                                    com.suvojeet.suvmusic.data.model.BrowseCategory(
                                        title = title,
                                        browseId = browseId,
                                        params = params,
                                        thumbnailUrl = null,
                                        color = colorValue
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return categories
    }

    // ============================================================================================
    // Mutating Actions (Library Management)
    // ============================================================================================

    suspend fun rateSong(videoId: String, rating: String): Boolean = withContext(Dispatchers.IO) {
        // rating: LIKE, DISLIKE, INDIFFERENT
        try {
            val endpoint = when(rating) {
                "LIKE" -> "like/like"
                "DISLIKE" -> "like/dislike"
                else -> "like/removelike"
            }
            // For like/removelike, the body structure is mostly the same
            val body = """
                {
                    "target": {
                        "videoId": "$videoId"
                    }
                }
            """.trimIndent()
            
            performAuthenticatedAction(endpoint, body)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun ratePlaylist(playlistId: String, rating: String): Boolean = withContext(Dispatchers.IO) {
        // rating: LIKE, DISLIKE, INDIFFERENT
        try {
            val endpoint = when(rating) {
                "LIKE" -> "like/like"
                "DISLIKE" -> "like/dislike"
                else -> "like/removelike"
            }
            
            val body = """
                {
                    "target": {
                        "playlistId": "$playlistId"
                    }
                }
            """.trimIndent()
            
            performAuthenticatedAction(endpoint, body)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun subscribe(channelId: String, isSubscribe: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
             // 1. Toggle local persistence (Priority for this feature)
             if (isSubscribe) {
                 // We need to fetch the artist to save it if we don't have it. 
                 // This method only has ID. Fetching it might be expensive if we just want to toggle.
                 // However, to save to DB we need the Artist object.
                 // Logic: If fetching fails, we can't save locally, so return false? 
                 // Or we save a minimal entry? LibraryEntity needs title/thumb.
                 // Better to fetch.
                 val artist = getArtist(channelId)
                 if (artist != null) {
                     libraryRepository.saveArtist(artist)
                 }
             } else {
                 libraryRepository.removeArtist(channelId)
             }

             // 2. Try upstream YouTube subscribe if logged in (Best effort)
             if (sessionManager.isLoggedIn()) {
                 val endpoint = if (isSubscribe) "subscription/subscribe" else "subscription/unsubscribe"
                 val body = """
                     {
                         "channelIds": ["$channelId"]
                     }
                 """.trimIndent()
                 performAuthenticatedAction(endpoint, body)
             }
             
             true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ============================================================================================
    // Playlist Management
    // ============================================================================================

    /**
     * Create a new playlist on YouTube Music.
     * @param title The title of the playlist
     * @param description Optional description for the playlist
     * @param privacyStatus Privacy status - PRIVATE, UNLISTED, or PUBLIC
     * @return The playlist ID if successful, null otherwise
     */
    suspend fun createPlaylist(
        title: String,
        description: String = "",
        privacyStatus: String = "PRIVATE"
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext null
            
            val cookies = sessionManager.getCookies() ?: return@withContext null
            val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
            
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("title", title)
                put("description", description)
                put("privacyStatus", privacyStatus)
            }
            
            val request = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/playlist/create")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Cookie", cookies)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("X-Goog-AuthUser", "0")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            
            // Parse the response to get playlist ID
            val jsonResponse = JSONObject(responseBody)
            jsonResponse.optString("playlistId").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Add a song to an existing YouTube Music playlist.
     * @param playlistId The ID of the playlist
     * @param videoId The video ID to add
     * @return True if successful
     */
    suspend fun addSongToPlaylist(playlistId: String, videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext false
            
            val cookies = sessionManager.getCookies() ?: return@withContext false
            val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
            
            // Strip "VL" prefix if present, as edit_playlist expects the raw playlist ID
            val realPlaylistId = if (playlistId.startsWith("VL")) playlistId.substring(2) else playlistId
            
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("playlistId", realPlaylistId)
                put("actions", JSONArray().apply {
                    put(JSONObject().apply {
                        put("action", "ACTION_ADD_VIDEO")
                        put("addedVideoId", videoId)
                    })
                })
            }
            
            val request = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse/edit_playlist")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Cookie", cookies)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("X-Goog-AuthUser", "0")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    /**
     * Add multiple songs to an existing YouTube Music playlist.
     * @param playlistId The ID of the playlist
     * @param videoIds List of video IDs to add
     * @return True if successful
     */
    suspend fun addSongsToPlaylist(playlistId: String, videoIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext false
            if (videoIds.isEmpty()) return@withContext true
            
            val cookies = sessionManager.getCookies() ?: return@withContext false
            val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
            
            val realPlaylistId = if (playlistId.startsWith("VL")) playlistId.substring(2) else playlistId
            
            // Chunk requests to avoid hitting limits (50 per request is safe)
            var allSuccess = true
            
            videoIds.chunked(50).forEach { chunk ->
                val jsonBody = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20230102.01.00")
                            put("hl", "en")
                            put("gl", "US")
                        })
                    })
                    put("playlistId", realPlaylistId)
                    put("actions", JSONArray().apply {
                        chunk.forEach { videoId ->
                            put(JSONObject().apply {
                                put("action", "ACTION_ADD_VIDEO")
                                put("addedVideoId", videoId)
                            })
                        }
                    })
                }
                
                val request = okhttp3.Request.Builder()
                    .url("https://music.youtube.com/youtubei/v1/browse/edit_playlist")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Cookie", cookies)
                    .addHeader("Authorization", authHeader)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Origin", "https://music.youtube.com")
                    .addHeader("X-Goog-AuthUser", "0")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) allSuccess = false
                response.close()
            }
            
            allSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Rename a playlist.
     * @param playlistId The ID of the playlist
     * @param newTitle The new title
     * @param newDescription The new description (optional)
     * @return True if successful
     */
    suspend fun renamePlaylist(playlistId: String, newTitle: String, newDescription: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext false
            
            val cookies = sessionManager.getCookies() ?: return@withContext false
            val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
            
            val realPlaylistId = if (playlistId.startsWith("VL")) playlistId.substring(2) else playlistId
            
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("playlistId", realPlaylistId)
                put("actions", JSONArray().apply {
                    put(JSONObject().apply {
                        put("action", "ACTION_SET_PLAYLIST_NAME")
                        put("playlistName", newTitle)
                    })
                    if (newDescription != null) {
                        put(JSONObject().apply {
                            put("action", "ACTION_SET_PLAYLIST_DESCRIPTION")
                            put("playlistDescription", newDescription)
                        })
                    }
                })
            }
            
            val request = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse/edit_playlist")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Cookie", cookies)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("X-Goog-AuthUser", "0")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a playlist.
     * @param playlistId The ID of the playlist
     * @return True if successful
     */
    suspend fun deletePlaylist(playlistId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext false
            
            val cookies = sessionManager.getCookies() ?: return@withContext false
            val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
            
            val realPlaylistId = if (playlistId.startsWith("VL")) playlistId.substring(2) else playlistId
            
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("playlistId", realPlaylistId)
            }
            
            val request = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/playlist/delete")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Cookie", cookies)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("X-Goog-AuthUser", "0")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch lyrics for a song.
     * Tries to find time-synced lyrics, falls back to plain text.
     */
    suspend fun getLyrics(videoId: String): com.suvojeet.suvmusic.providers.lyrics.Lyrics? = withContext(Dispatchers.IO) {
        try {
            // 1. Get the "Next" response to find the Lyrics browse ID
            val cookies = sessionManager.getCookies()
            // Even if not logged in, we can try without auth, or use minimal auth
            val authHeader = if (cookies != null) YouTubeAuthUtils.getAuthorizationHeader(cookies) else ""
            
            val nextBody = JSONObject().apply {
                 put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("videoId", videoId)
            }
            
            val nextRequest = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/next")
                .post(nextBody.toString().toRequestBody("application/json".toMediaType()))
                .apply {
                    if (cookies != null) addHeader("Cookie", cookies)
                    if (authHeader != null) addHeader("Authorization", authHeader)
                    addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    addHeader("Origin", "https://music.youtube.com")
                    addHeader("X-Goog-AuthUser", "0") 
                }
                .build()
                
            val nextResponse = okHttpClient.newCall(nextRequest).execute()
            if (!nextResponse.isSuccessful) return@withContext null
            
            val nextJson = JSONObject(nextResponse.body?.string() ?: return@withContext null)
            val lyricsBrowseId = extractLyricsBrowseId(nextJson) ?: return@withContext null
            
            // 2. Fetch the Lyrics using the browse ID
            val browseBody = JSONObject().apply {
                 put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("browseId", lyricsBrowseId)
            }
            
            val browseRequest = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse")
                .post(browseBody.toString().toRequestBody("application/json".toMediaType()))
                .apply {
                    if (cookies != null) addHeader("Cookie", cookies)
                    if (authHeader != null) addHeader("Authorization", authHeader)
                    addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    addHeader("Origin", "https://music.youtube.com")
                    addHeader("X-Goog-AuthUser", "0")
                }
                .build()
                
            val browseResponse = okHttpClient.newCall(browseRequest).execute()
            if (!browseResponse.isSuccessful) return@withContext null
             
            val browseJson = JSONObject(browseResponse.body?.string() ?: return@withContext null)
            parseLyricsFromBrowse(browseJson)
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch comments for a video using NewPipe extractor.
     */
    suspend fun getComments(videoId: String): List<com.suvojeet.suvmusic.data.model.Comment> = withContext(Dispatchers.IO) {
        try {
            currentVideoIdForComments = videoId
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext emptyList()
            
            currentCommentsExtractor = ytService.getCommentsExtractor("https://www.youtube.com/watch?v=$videoId")
            currentCommentsExtractor?.fetchPage()
            currentCommentsPage = currentCommentsExtractor?.initialPage
            
            currentCommentsPage?.items?.filterIsInstance<CommentsInfoItem>()?.map { item ->
                Comment(
                    id = item.url ?: java.util.UUID.randomUUID().toString(),
                    authorName = item.uploaderName ?: "Unknown",
                    authorThumbnailUrl = item.uploaderAvatars?.firstOrNull()?.url,
                    text = item.commentText?.content ?: "",
                    timestamp = item.textualUploadDate ?: "",
                    likeCount = if (item.likeCount > 0) item.likeCount.toString() else "",
                    replyCount = 0
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch more comments for the current video.
     */
    suspend fun getMoreComments(videoId: String): List<com.suvojeet.suvmusic.data.model.Comment> = withContext(Dispatchers.IO) {
        if (videoId != currentVideoIdForComments || currentCommentsExtractor == null || currentCommentsPage == null || !currentCommentsPage!!.hasNextPage()) {
            return@withContext emptyList()
        }

        try {
            currentCommentsPage = currentCommentsExtractor!!.getPage(currentCommentsPage!!.nextPage)
            
            currentCommentsPage?.items?.filterIsInstance<CommentsInfoItem>()?.map { item ->
                Comment(
                    id = item.url ?: java.util.UUID.randomUUID().toString(),
                    authorName = item.uploaderName ?: "Unknown",
                    authorThumbnailUrl = item.uploaderAvatars?.firstOrNull()?.url,
                    text = item.commentText?.content ?: "",
                    timestamp = item.textualUploadDate ?: "",
                    likeCount = if (item.likeCount > 0) item.likeCount.toString() else "",
                    replyCount = 0
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Post a comment on a YouTube video.
     * Requires user to be logged in.
     */
    suspend fun postComment(videoId: String, commentText: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext false
            if (commentText.isBlank()) return@withContext false
            
            val cookies = sessionManager.getCookies() ?: return@withContext false
            val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
            
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.00.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("createCommentParams", JSONObject().apply {
                    put("videoId", videoId)
                    put("text", commentText)
                })
            }
            
            val request = okhttp3.Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/comment/create_comment")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Cookie", cookies)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://www.youtube.com")
                .addHeader("X-Goog-AuthUser", "0")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun extractLyricsBrowseId(nextJson: JSONObject): String? {
        val tabs = nextJson.optJSONObject("contents")
            ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
            ?.optJSONObject("tabbedRenderer")
            ?.optJSONObject("watchNextTabbedResultsRenderer")
            ?.optJSONArray("tabs")
            
        if (tabs != null) {
            for (i in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(i)?.optJSONObject("tabRenderer") ?: continue
                val title = tab.optString("title") // Often "Lyrics"
                val endpoint = tab.optJSONObject("endpoint")
                val browseId = endpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
                
                // Confirm it's lyrics - sometimes id is "MPLYt..."
                if (browseId != null && (title.equals("Lyrics", ignoreCase = true) || browseId.startsWith("MPLY"))) {
                    return browseId
                }
            }
        }
        return null
    }
    
    private fun parseLyricsFromBrowse(json: JSONObject): com.suvojeet.suvmusic.providers.lyrics.Lyrics? {
        val contents = json.optJSONObject("contents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
        if (contents != null) {
            var timedLyrics: JSONObject? = null
            var descriptionShelf: JSONObject? = null
            
            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i)
                if (timedLyrics == null) {
                    timedLyrics = item.optJSONObject("musicTimedLyricsRenderer")
                }
                if (descriptionShelf == null) {
                    descriptionShelf = item.optJSONObject("musicDescriptionShelfRenderer")
                }
            }
            
            // 1. Try Synced Lyrics
            if (timedLyrics != null) {
                val lyricData = timedLyrics.optJSONArray("timedLyricsData")
                if (lyricData != null) {
                    val lines = mutableListOf<com.suvojeet.suvmusic.providers.lyrics.LyricsLine>()
                    for (i in 0 until lyricData.length()) {
                        val lineObj = lyricData.optJSONObject(i)
                        val text = lineObj?.optString("lyricLine") ?: ""
                        val startTime = lineObj?.optLong("cueRangeStartMillis") ?: 0L
                        
                        if (text.isNotBlank()) {
                            lines.add(com.suvojeet.suvmusic.providers.lyrics.LyricsLine(
                                text = text,
                                startTimeMs = startTime
                            ))
                        }
                    }
                    
                    val footer = getRunText(timedLyrics.optJSONObject("footer")) 
                        ?: getRunText(descriptionShelf?.optJSONObject("footer"))
                    
                    if (lines.isNotEmpty()) {
                        return com.suvojeet.suvmusic.providers.lyrics.Lyrics(lines, footer, true)
                    }
                }
            }
            
            // 2. Fallback to Plain Text
            if (descriptionShelf != null) {
                val description = getRunText(descriptionShelf.optJSONObject("description"))
                val footer = getRunText(descriptionShelf.optJSONObject("footer"))
                
                if (description != null) {
                    val lines = description.split("\r\n", "\n").map { com.suvojeet.suvmusic.providers.lyrics.LyricsLine(it) }
                    return com.suvojeet.suvmusic.providers.lyrics.Lyrics(lines, footer, false)
                }
            }
        }
        
        return null
    }

    /**
     * Move a song within a playlist.
     * @param playlistId The ID of the playlist
     * @param setVideoId The unique ID of the song instance in the playlist
     * @param predecessorSetVideoId The setVideoId of the song that should come BEFORE the moved song. 
     *                              If moving to the top, pass null or specific sentinel if required. 
     *                              (Note: YT Music API usually moves 'movedSetVideoId' to follow 'predecessorSetVideoId')
     * @return True if successful
     */
    suspend fun moveSongInPlaylist(
        playlistId: String, 
        setVideoId: String, 
        predecessorSetVideoId: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext false
            
            val cookies = sessionManager.getCookies() ?: return@withContext false
            val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
            
            val realPlaylistId = if (playlistId.startsWith("VL")) playlistId.substring(2) else playlistId
            
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("playlistId", realPlaylistId)
                put("actions", JSONArray().apply {
                    put(JSONObject().apply {
                        put("action", "ACTION_MOVE_VIDEO_AFTER")
                        put("setVideoId", setVideoId)
                        if (predecessorSetVideoId != null) {
                            put("movedSetVideoIdPredecessor", predecessorSetVideoId)
                        }
                    })
                })
            }
            
            val request = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse/edit_playlist")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Cookie", cookies)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("X-Goog-AuthUser", "0")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get user's playlists for adding songs to.
     * Returns only user-created playlists, not auto-generated ones.
     */
    suspend fun getUserEditablePlaylists(): List<PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext emptyList()
            
            val response = fetchInternalApi("FEmusic_liked_playlists")
            if (response.isEmpty()) return@withContext emptyList()
            
            val json = JSONObject(response)
            val playlists = mutableListOf<PlaylistDisplayItem>()
            
            // Parse the response to get user playlists
            val contents = json.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            
            contents?.let { contentArray ->
                for (i in 0 until contentArray.length()) {
                    val section = contentArray.optJSONObject(i)
                    val gridRenderer = section?.optJSONObject("gridRenderer")
                        ?: section?.optJSONObject("musicShelfRenderer")
                    
                    val items = gridRenderer?.optJSONArray("items")
                        ?: gridRenderer?.optJSONArray("contents")
                    
                    items?.let { itemArray ->
                        for (j in 0 until itemArray.length()) {
                            val item = itemArray.optJSONObject(j)
                            val musicItem = item?.optJSONObject("musicTwoRowItemRenderer")
                                ?: item?.optJSONObject("musicResponsiveListItemRenderer")
                            
                            val playlist = musicItem?.let { parsePlaylistItem(it) }
                            if (playlist != null) {
                                // Filter out auto-generated playlists
                                val playlistId = playlist.id
                                if (!playlistId.startsWith("RDAMPL") && 
                                    !playlistId.startsWith("LM") &&
                                    playlistId.isNotEmpty()) {
                                    playlists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
            
            playlists
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun extractSongCount(subtitle: String): Int {
        // Try to find "X songs"
        val songCountRegex = Regex("(\\d+)\\s*song")
        songCountRegex.find(subtitle)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        
        // Sometimes just digits if it's a list item validation? No, usually valid text.
        // Try finding any large number if "song" is missing but it looks like a stat? 
        // Maybe unsafe. Stick to "songs" for now.
        
        // Handle "100+ songs" case
        val plusRegex = Regex("(\\d+)\\+\\s*song")
        plusRegex.find(subtitle)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        return 0
    }

    private fun parsePlaylistItem(item: JSONObject): PlaylistDisplayItem? {
        val title = extractTitle(item)
        val thumbnail = extractThumbnail(item)
        
        val navigationEndpoint = item.optJSONObject("navigationEndpoint")
            ?: item.optJSONObject("serviceEndpoint")
            
        val browseId = navigationEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
        
        if (browseId.isNullOrEmpty()) return null

        // Subtitle often contains "Author • Song count"
        val subtitle = extractFullSubtitle(item)
        
        // Simple heuristic for song count and uploader
        val parts = subtitle.split("•").map { it.trim() }
        
        val uploader = parts.firstOrNull { !it.contains("song", ignoreCase = true) } ?: "YouTube User"

        return PlaylistDisplayItem(
            id = browseId,
            name = title,
            url = "https://music.youtube.com/playlist?list=$browseId",
            uploaderName = uploader,
            thumbnailUrl = thumbnail,
            songCount = extractSongCount(subtitle)
        )
    }

    // ============================================================================================
    // Internal API Helpers (Delegated to YouTubeApiClient)
    // ============================================================================================

    private fun fetchInternalApi(endpoint: String): String = apiClient.fetchInternalApi(endpoint)

    private fun fetchPublicApi(browseId: String): String = apiClient.fetchPublicApi(browseId)

    private fun performAuthenticatedAction(endpoint: String, innerBody: String): Boolean =
        apiClient.performAuthenticatedAction(endpoint, innerBody)

    // ============================================================================================
    // Parsers
    // ============================================================================================

    private fun parsePlaylistFromInternalJson(json: String, playlistId: String): Playlist {
        val root = JSONObject(json)
        
        // Try multiple header renderer types
        val header = root.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")
            ?: root.optJSONObject("header")?.optJSONObject("musicResponsiveHeaderRenderer")
            ?: root.optJSONObject("header")?.optJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                ?.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")
            ?: root.optJSONObject("header")?.optJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                ?.optJSONObject("header")?.optJSONObject("musicResponsiveHeaderRenderer")
        
        // Extract title from header
        val title = getRunText(header?.optJSONObject("title")) 
            ?: header?.optJSONObject("title")?.optString("simpleText")
            ?: "Unknown Playlist"
        
        // Extract author/subtitle
        val subtitle = getRunText(header?.optJSONObject("subtitle"))
            ?: getRunText(header?.optJSONObject("straplineTextOne"))
        val author = subtitle?.split("•")?.firstOrNull()
            ?.replace("Playlist", "")
            ?.replace("Auto playlist", "YouTube Music")
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.lowercase() != "unknown" }
            ?: ""

        // Extract description
        val description = getRunText(header?.optJSONObject("description"))
            ?: getRunText(header?.optJSONObject("descriptionText"))
        
        // Extract thumbnail from multiple possible locations
        var thumbnailUrl: String? = null
        
        // Try direct thumbnail in header
        thumbnailUrl = extractHeaderThumbnail(header)
        
        // Try from thumbnail renderer
        if (thumbnailUrl == null) {
            thumbnailUrl = header?.optJSONObject("thumbnail")
                ?.optJSONObject("croppedSquareThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.let { arr -> arr.optJSONObject(arr.length() - 1)?.optString("url") }
        }
        
        // Try from foreground thumbnail
        if (thumbnailUrl == null) {
            thumbnailUrl = header?.optJSONObject("foregroundThumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.let { arr -> arr.optJSONObject(arr.length() - 1)?.optString("url") }
        }
        
        val songs = parseSongsFromInternalJson(json)
        
        // Fallback to first song's thumbnail if playlist thumbnail is missing
        if (thumbnailUrl == null && songs.isNotEmpty()) {
            thumbnailUrl = songs.firstOrNull()?.thumbnailUrl
        }
        
        return Playlist(
            id = playlistId,
            title = title,
            author = author,
            thumbnailUrl = thumbnailUrl,
            songs = songs,
            description = description
        )
    }
    
    private fun extractHeaderThumbnail(header: JSONObject?): String? = 
        jsonParser.extractHeaderThumbnail(header)

    private fun parseSongsFromInternalJson(json: String): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val root = JSONObject(json)
            val items = mutableListOf<JSONObject>()
            findAllObjects(root, "musicResponsiveListItemRenderer", items)
            findAllObjects(root, "musicTwoRowItemRenderer", items)

            items.forEach { item ->
                try {
                    // Try to get videoId from standard runs/endpoints first
                    var videoId = extractValueFromRuns(item, "videoId") 
                        ?: item.optString("videoId").takeIf { it.isNotEmpty() }
                    
                    // Fallback to playlistItemData (common in playlists)
                    if (videoId == null) {
                        videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
                    }
                    
                    if (videoId != null) {
                        val title = extractTitle(item)
                        val artist = extractArtist(item)
                        val thumbnailUrl = extractThumbnail(item)
                        val setVideoId = item.optJSONObject("playlistItemData")?.optString("videoId")
                        
                        Song.fromYouTube(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            album = "",
                            duration = extractDuration(item),
                            thumbnailUrl = thumbnailUrl,
                            setVideoId = setVideoId
                        )?.let { songs.add(it) }
                    }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }
        return songs.distinctBy { it.id }
    }
    
    private fun parsePlaylistsFromInternalJson(json: String): List<PlaylistDisplayItem> {
        val playlists = mutableListOf<PlaylistDisplayItem>()
        try {
            val root = JSONObject(json)
            val items = mutableListOf<JSONObject>()
            findAllObjects(root, "musicTwoRowItemRenderer", items)
            
            items.forEach { item ->
                try {
                    val navigationEndpoint = item.optJSONObject("navigationEndpoint")
                    val browseId = navigationEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
                     
                    if (browseId != null && (browseId.startsWith("VL") || browseId.startsWith("PL"))) {
                        val cleanId = browseId.removePrefix("VL")
                        val title = getRunText(item.optJSONObject("title")) ?: "Unknown Playlist"
                        val subtitle = getRunText(item.optJSONObject("subtitle")) ?: "Unknown"
                        
                        // Parse song count more robustly
                        val parts = subtitle.split("•").map { it.trim() }
                        
                        val uploaderName = parts.firstOrNull { !it.contains("song", ignoreCase = true) } ?: subtitle
                        
                        val thumbnailUrl = extractThumbnail(item)

                        playlists.add(PlaylistDisplayItem(
                            id = cleanId,
                            name = title,
                            url = "https://music.youtube.com/playlist?list=$cleanId",
                            uploaderName = uploaderName,
                            thumbnailUrl = thumbnailUrl,
                            songCount = extractSongCount(subtitle)
                        ))
                    }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }
        return playlists 
    }

    private fun parseArtistFromInternalJson(json: String, artistId: String): Artist {
        val root = JSONObject(json)
        val header = root.optJSONObject("header")?.optJSONObject("musicImmersiveHeaderRenderer")
            ?: root.optJSONObject("header")?.optJSONObject("musicVisualHeaderRenderer")
        
        val name = getRunText(header?.optJSONObject("title")) ?: "Unknown Artist"
        val description = getRunText(header?.optJSONObject("description"))
        
        // Artist thumbnail is in header.thumbnail.thumbnails or header.foregroundThumbnail.thumbnails
        val thumbnailUrl = run {
            val thumbObj = header?.optJSONObject("thumbnail") 
                ?: header?.optJSONObject("foregroundThumbnail")
            val thumbnails = thumbObj?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?: thumbObj?.optJSONArray("thumbnails")
            // Get largest thumbnail
            thumbnails?.let { arr ->
                if (arr.length() > 0) arr.optJSONObject(arr.length() - 1)?.optString("url") else null
            }
        }
        
        val subscriptionButton = header?.optJSONObject("subscriptionButton")?.optJSONObject("subscribeButtonRenderer")
    val subscribers = getRunText(subscriptionButton?.optJSONObject("subscriberCountText"))
    val isSubscribed = subscriptionButton?.optBoolean("subscribed") ?: false
    val channelId = subscriptionButton?.optString("channelId") ?: artistId

    val songs = mutableListOf<Song>()
    val albums = mutableListOf<Album>()
    val singles = mutableListOf<Album>()
    val videos = mutableListOf<Song>()
    val relatedArtists = mutableListOf<ArtistPreview>()
    val featuredPlaylists = mutableListOf<Playlist>()

        // Find sections
        val sections = mutableListOf<JSONObject>()
        findAllObjects(root, "musicShelfRenderer", sections)
        findAllObjects(root, "musicCarouselShelfRenderer", sections)

        sections.forEach { section ->
            val title = getRunText(section.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")?.optJSONObject("title"))
                ?: getRunText(section.optJSONObject("title"))
            
            val contents = section.optJSONArray("contents") ?: return@forEach

            if (title?.contains("Songs", ignoreCase = true) == true || title?.contains("Top songs", ignoreCase = true) == true) {
                 for (i in 0 until contents.length()) {
                     val item = contents.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                     val videoId = extractValueFromRuns(item, "videoId")
                     if (videoId != null) {
                         Song.fromYouTube(
                             videoId = videoId,
                             title = extractTitle(item),
                             artist = extractArtist(item),
                             album = "",
                             duration = extractDuration(item),
                             thumbnailUrl = extractThumbnail(item)
                         )?.let { songs.add(it) }
                     }
                 }
            } else if (title?.contains("Albums", ignoreCase = true) == true) {
                parseAlbums(contents, albums, name)
            } else if (title?.contains("Singles", ignoreCase = true) == true) {
                parseAlbums(contents, singles, name)
            } else if (title?.contains("Videos", ignoreCase = true) == true) {
                 for (i in 0 until contents.length()) {
                     val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                     val videoId = item.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                     if (videoId != null) {
                         Song.fromYouTube(
                             videoId = videoId,
                            title = getRunText(item.optJSONObject("title")) ?: "Unknown",
                            artist = name,
                            album = "",
                            duration = 0L, // Duration might not be readily available in video cards
                             thumbnailUrl = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                                 ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let { arr ->
                                     if (arr.length() > 0) arr.optJSONObject(arr.length() - 1)?.optString("url") else null
                                 }
                         )?.let { videos.add(it) }
                     }
                 }
            } else if (title?.contains("Fans might also like", ignoreCase = true) == true || title?.contains("Related", ignoreCase = true) == true) {
                 parseRelatedArtists(contents, relatedArtists)
            } else if (title?.contains("Featured on", ignoreCase = true) == true) {
                 parseFeaturedPlaylists(contents, featuredPlaylists)
            }
        }

        return Artist(
            id = artistId,
            name = name,
            thumbnailUrl = thumbnailUrl,
            description = description,
            subscribers = subscribers,
            songs = songs,
            albums = albums,
            singles = singles,
            isSubscribed = isSubscribed,
            channelId = channelId,
            videos = videos,
            relatedArtists = relatedArtists,
            featuredPlaylists = featuredPlaylists
        )
    }

    private fun parseAlbums(contents: JSONArray, targetList: MutableList<Album>, artistName: String) {
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
            val browseId = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
            
            if (browseId != null) {
                val title = getRunText(item.optJSONObject("title")) ?: "Unknown Album"
                val year = getRunText(item.optJSONObject("subtitle")) // Often contains Year • Type
                val thumbnailUrl = extractThumbnail(item)
                
                targetList.add(Album(
                    id = browseId,
                    title = title,
                    artist = artistName,
                    year = year,
                    thumbnailUrl = thumbnailUrl
                ))
            }
        }
    }

    private fun parseAlbumFromInternalJson(json: String, albumId: String): Album {
         val root = JSONObject(json)
         
         // Header extraction - similar to playlist parsing to cover all cases
         val header = root.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")
             ?: root.optJSONObject("header")?.optJSONObject("musicResponsiveHeaderRenderer")
             ?: root.optJSONObject("header")?.optJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                ?.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")
             ?: root.optJSONObject("header")?.optJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                ?.optJSONObject("header")?.optJSONObject("musicResponsiveHeaderRenderer")
         
         // Title
         val title = getRunText(header?.optJSONObject("title")) 
             ?: header?.optJSONObject("title")?.optString("simpleText")
             ?: "Unknown Album"
             
         // Subtitle (Artist • Year)
         val subtitle = getRunText(header?.optJSONObject("subtitle")) 
             ?: getRunText(header?.optJSONObject("straplineTextOne"))
             
         // Description
         val description = getRunText(header?.optJSONObject("description"))
             ?: getRunText(header?.optJSONObject("descriptionText"))
             ?: getRunText(header?.optJSONObject("secondSubtitle"))
             
         // Thumbnail
         var thumbnailUrl = extractHeaderThumbnail(header)
         
         // Fallback thumbnail extraction
         if (thumbnailUrl == null) {
             thumbnailUrl = header?.optJSONObject("thumbnail")
                 ?.optJSONObject("croppedSquareThumbnailRenderer")
                 ?.optJSONObject("thumbnail")
                 ?.optJSONArray("thumbnails")
                 ?.let { arr -> arr.optJSONObject(arr.length() - 1)?.optString("url") }
         }
         
         val songs = parseSongsFromInternalJson(json)
         
         // Use first song's thumbnail if album art is missing
         if (thumbnailUrl == null && songs.isNotEmpty()) {
             thumbnailUrl = songs.first().thumbnailUrl
         }
         
         // Artist extraction
         val artist = subtitle?.split("•")?.firstOrNull()?.trim() 
             ?: songs.firstOrNull()?.artist 
             ?: "Unknown Artist"
             
         // Year extraction
         val year = subtitle?.split("•")?.find { it.trim().matches(Regex("\\d{4}")) } ?: subtitle

         return Album(
             id = albumId,
             title = title,
             artist = artist,
             year = year,
             thumbnailUrl = thumbnailUrl,
             description = description,
             songs = songs
         )
    }

    private fun parseHomeSectionsFromInternalJson(json: String): List<com.suvojeet.suvmusic.data.model.HomeSection> {
        val sections = mutableListOf<com.suvojeet.suvmusic.data.model.HomeSection>()
        try {
            val root = JSONObject(json)
            
            // Try standard browse response path
            var contents = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            
            // If null, try continuation response path
            if (contents == null) {
                contents = root.optJSONObject("continuationContents")
                    ?.optJSONObject("sectionListContinuation")
                    ?.optJSONArray("contents")
            }

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val sectionObj = contents.optJSONObject(i)
                    
                    // 1. Carousels (Horizontal Scroll)
                    val carouselShelf = sectionObj?.optJSONObject("musicCarouselShelfRenderer")
                        ?: sectionObj?.optJSONObject("musicImmersiveCarouselShelfRenderer")

                    if (carouselShelf != null) {
                        val title = getRunText(carouselShelf.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")?.optJSONObject("title"))
                            ?: getRunText(carouselShelf.optJSONObject("header")?.optJSONObject("musicImmersiveCarouselShelfBasicHeaderRenderer")?.optJSONObject("title"))
                            ?: ""

                        val itemsArray = carouselShelf.optJSONArray("contents")
                        val items = mutableListOf<com.suvojeet.suvmusic.data.model.HomeItem>()

                        if (itemsArray != null) {
                            for (j in 0 until itemsArray.length()) {
                                val itemObj = itemsArray.optJSONObject(j)
                                parseHomeItem(itemObj)?.let { items.add(it) }
                            }
                        }

                        if (items.isNotEmpty() && title.isNotEmpty()) {
                            val type = when {
                                title.contains("Quick picks", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.VerticalList
                                title.contains("Fresh finds", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.Grid
                                title.contains("Community", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.CommunityCarousel
                                else -> com.suvojeet.suvmusic.data.model.HomeSectionType.HorizontalCarousel
                            }
                            sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, items, type))
                        }
                    }
                    
                    // 2. Shelves (Vertical List - e.g. "Your Likes" often appears as a list)
                    val shelf = sectionObj?.optJSONObject("musicShelfRenderer")
                    if (shelf != null) {
                         val title = getRunText(shelf.optJSONObject("title")) ?: ""
                         val itemsArray = shelf.optJSONArray("contents")
                         val items = mutableListOf<com.suvojeet.suvmusic.data.model.HomeItem>()
                         
                         if (itemsArray != null) {
                             for (j in 0 until itemsArray.length()) {
                                 val itemObj = itemsArray.optJSONObject(j)
                                 parseHomeItem(itemObj)?.let { items.add(it) }
                             }
                         }
                         
                         if (items.isNotEmpty() && title.isNotEmpty()) {
                            val type = when {
                                title.contains("Quick picks", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.VerticalList
                                title.contains("Fresh finds", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.Grid
                                title.contains("Community", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.CommunityCarousel
                                else -> com.suvojeet.suvmusic.data.model.HomeSectionType.HorizontalCarousel
                            }
                            sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, items, type))
                        }
                    }
                    
                    // 3. Grids (e.g. "Listen Again" sometimes)
                    val grid = sectionObj?.optJSONObject("gridRenderer")
                    if (grid != null) {
                        val title = getRunText(grid.optJSONObject("header")?.optJSONObject("gridHeaderRenderer")?.optJSONObject("title")) ?: ""
                        val itemsArray = grid.optJSONArray("items")
                        val items = mutableListOf<com.suvojeet.suvmusic.data.model.HomeItem>()
                         
                         if (itemsArray != null) {
                             for (j in 0 until itemsArray.length()) {
                                 val itemObj = itemsArray.optJSONObject(j)
                                 parseHomeItem(itemObj)?.let { items.add(it) }
                             }
                         }
                         
                         if (items.isNotEmpty() && title.isNotEmpty()) {
                            val type = when {
                                title.contains("Quick picks", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.VerticalList
                                title.contains("Fresh finds", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.Grid
                                title.contains("Community", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.LargeCardWithList
                                else -> com.suvojeet.suvmusic.data.model.HomeSectionType.Grid
                            }
                            sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, items, type))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sections
    }

    private fun getExploreSection(): com.suvojeet.suvmusic.data.model.HomeSection {
        val exploreItems = listOf(
            com.suvojeet.suvmusic.data.model.HomeItem.ExploreItem("New releases", com.suvojeet.suvmusic.R.drawable.ic_music_note, "FEmusic_new_releases"),
            com.suvojeet.suvmusic.data.model.HomeItem.ExploreItem("Charts", com.suvojeet.suvmusic.R.drawable.ic_waveform, "FEmusic_charts"),
            com.suvojeet.suvmusic.data.model.HomeItem.ExploreItem("Moods and genres", com.suvojeet.suvmusic.R.drawable.ic_play, "FEmusic_moods_and_genres"),
            com.suvojeet.suvmusic.data.model.HomeItem.ExploreItem("Podcasts", com.suvojeet.suvmusic.R.drawable.ic_launcher_monochrome, "FEmusic_podcasts")
        )
        return com.suvojeet.suvmusic.data.model.HomeSection("Explore", exploreItems, com.suvojeet.suvmusic.data.model.HomeSectionType.ExploreGrid)
    }

    private fun parseHomeItem(itemObj: JSONObject?): com.suvojeet.suvmusic.data.model.HomeItem? {
        if (itemObj == null) return null

        val responsiveItem = itemObj.optJSONObject("musicResponsiveListItemRenderer")
        if (responsiveItem != null) {
            // Usually a song or video
            val videoId = extractValueFromRuns(responsiveItem, "videoId") ?: responsiveItem.optString("videoId")
            if (videoId.isNotEmpty()) {
                val title = extractTitle(responsiveItem)
                val artist = extractArtist(responsiveItem)
                val thumbnail = extractThumbnail(responsiveItem)
                
                // Check if it's a playlist or something else based on navigation endpoint
                val navEndpoint = responsiveItem.optJSONObject("navigationEndpoint")
                val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
                
                if (browseId != null && (browseId.startsWith("VL") || browseId.startsWith("PL"))) {
                     // It's a playlist masquerading as a list item? Rare but possible.
                     // Treat as song for now as responsive items are usually tracks.
                }

                val song = Song.fromYouTube(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    album = "",
                    duration = extractDuration(responsiveItem),
                    thumbnailUrl = thumbnail
                )
                return song?.let { com.suvojeet.suvmusic.data.model.HomeItem.SongItem(it) }
            }
        }

        val twoRowItem = itemObj.optJSONObject("musicTwoRowItemRenderer")
        if (twoRowItem != null) {
            val title = getRunText(twoRowItem.optJSONObject("title")) ?: "Unknown"
            val subtitle = getRunText(twoRowItem.optJSONObject("subtitle")) ?: ""
            val thumbnail = extractThumbnail(twoRowItem)
            
            val navEndpoint = twoRowItem.optJSONObject("navigationEndpoint")
            val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
            val watchId = navEndpoint?.optJSONObject("watchEndpoint")?.optString("videoId")

            if (browseId != null) {
                if (browseId.startsWith("VL") || browseId.startsWith("PL") || 
                    browseId.startsWith("RD") || browseId.startsWith("RTM") || browseId == "LM") {
                    // Playlist or Mix
                    val cleanId = if (browseId.startsWith("VL")) browseId.removePrefix("VL") else browseId
                    val playlist = PlaylistDisplayItem(
                        id = cleanId,
                        name = title,
                        url = "https://music.youtube.com/playlist?list=$cleanId",
                        uploaderName = subtitle,
                        thumbnailUrl = thumbnail
                    )
                    return com.suvojeet.suvmusic.data.model.HomeItem.PlaylistItem(playlist)
                } else if (browseId.startsWith("MPRE") || browseId.startsWith("OLAK")) {
                    // Album
                    val album = Album(
                        id = browseId,
                        title = title,
                        artist = subtitle, // Usually "Artist • Year" or just Artist
                        thumbnailUrl = thumbnail
                    )
                    return com.suvojeet.suvmusic.data.model.HomeItem.AlbumItem(album)
                } else if (browseId.startsWith("UC")) {
                    // Artist
                     val artist = Artist(
                        id = browseId,
                        name = title,
                        thumbnailUrl = thumbnail,
                        description = null,
                        subscribers = subtitle
                    )
                    return com.suvojeet.suvmusic.data.model.HomeItem.ArtistItem(artist)
                }
            } else if (watchId != null) {
                 // It's a video/song but in a card format
                 val song = Song.fromYouTube(
                    videoId = watchId,
                    title = title,
                    artist = subtitle,
                    album = "",
                    duration = extractDuration(twoRowItem),
                    thumbnailUrl = thumbnail
                )
                return song?.let { com.suvojeet.suvmusic.data.model.HomeItem.SongItem(it) }
            }
        }
        
        return null
    }

    // --- JSON Helpers (Delegated to YouTubeJsonParser) ---

    private fun findAllObjects(node: Any, key: String, results: MutableList<JSONObject>) = 
        jsonParser.findAllObjects(node, key, results)

    private fun getRunText(formattedString: JSONObject?): String? = 
        jsonParser.getRunText(formattedString)

    private fun extractValueFromRuns(item: JSONObject, key: String): String? = 
        jsonParser.extractValueFromRuns(item, key)

    private fun extractFullSubtitle(item: JSONObject): String = 
        jsonParser.extractFullSubtitle(item)
    
    private fun extractTitle(item: JSONObject): String = 
        jsonParser.extractTitle(item)
    
    private fun extractArtist(item: JSONObject): String = 
        jsonParser.extractArtist(item)
    
    private fun extractThumbnail(item: JSONObject?): String? = 
        jsonParser.extractThumbnail(item)
    
    private fun extractDuration(item: JSONObject): Long = 
        jsonParser.extractDuration(item)
    
    private fun parseDurationText(text: String): Long = 
        jsonParser.parseDurationText(text)

    private fun extractVideoId(url: String): String = 
        jsonParser.extractVideoId(url)

    private fun parseRelatedArtists(contents: JSONArray, targetList: MutableList<ArtistPreview>) {
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
            val browseEndpoint = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val browseId = browseEndpoint?.optString("browseId") ?: continue
            
            // Skip if not artist
            if (browseEndpoint.optString("browseEndpointContextSupportedConfigs")?.contains("MUSIC_PAGE_TYPE_ARTIST") != true &&
                !browseId.startsWith("UC")) continue

            val name = getRunText(item.optJSONObject("title")) ?: "Unknown"
            val subtitle = getRunText(item.optJSONObject("subtitle")) // Often contains "Subscriber count"
            
            val thumbnailUrl = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let { arr ->
                    if (arr.length() > 0) arr.optJSONObject(arr.length() - 1)?.optString("url") else null
                }
                
            targetList.add(ArtistPreview(browseId, name, thumbnailUrl, subtitle))
        }
    }

    private fun parseFeaturedPlaylists(contents: JSONArray, targetList: MutableList<Playlist>) {
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
            val browseId = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId") ?: continue
            
            if (!browseId.startsWith("VL")) continue // Only playlists
            
            val playlistId = browseId.removePrefix("VL")
            val title = getRunText(item.optJSONObject("title")) ?: "Unknown"
            val author = getRunText(item.optJSONObject("subtitle")) ?: "Unknown"
            
            val thumbnailUrl = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let { arr ->
                    if (arr.length() > 0) arr.optJSONObject(arr.length() - 1)?.optString("url") else null
                }
                
            targetList.add(Playlist(playlistId, title, author, thumbnailUrl, emptyList()))
        }
    }

    suspend fun getArtistRadioId(artistId: String): String? = withContext(Dispatchers.IO) {
        try {
            val json = fetchInternalApi(artistId)
            val root = JSONObject(json)
            val header = root.optJSONObject("header")?.optJSONObject("musicImmersiveHeaderRenderer")
                ?: root.optJSONObject("header")?.optJSONObject("musicVisualHeaderRenderer")
                
            // Search in buttons for radio
            val buttons = header?.optJSONObject("startRadioButton")?.optJSONObject("buttonRenderer")
            
            val navigationEndpoint = buttons?.optJSONObject("navigationEndpoint")
            val watchEndpoint = navigationEndpoint?.optJSONObject("watchEndpoint")
            
            watchEndpoint?.optString("playlistId")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Mark a song as watched in YouTube Music history.
     * This uses the internal player endpoint to simulate a playback event.
     */
    suspend fun markAsWatched(videoId: String) = withContext(Dispatchers.IO) {
        try {
            // Check if logged in first
            if (!sessionManager.isLoggedIn()) return@withContext

            val payload = """
                {
                    "videoId": "$videoId",
                    "playbackContext": {
                        "contentPlaybackContext": {
                            "signatureTimestamp": ${System.currentTimeMillis() / 1000}
                        }
                    }
                }
            """.trimIndent()

            val success = apiClient.performAuthenticatedAction("player", payload)
            if (success) {
                android.util.Log.d("YouTubeRepository", "Marked as watched: $videoId")
            } else {
                android.util.Log.w("YouTubeRepository", "Failed to mark as watched: $videoId")
            }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeRepository", "Error marking as watched", e)
        }
    }
}
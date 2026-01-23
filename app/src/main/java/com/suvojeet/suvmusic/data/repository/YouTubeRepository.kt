package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.data.NewPipeDownloaderImpl
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.YouTubeAuthUtils
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.ArtistPreview
import com.suvojeet.suvmusic.data.model.Playlist
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import android.util.LruCache

/**
 * Repository for fetching data from YouTube Music.
 * Uses NewPipeExtractor for streams and search, and internal API for browsing/library.
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val jsonParser: com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeJsonParser,
    private val apiClient: com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeApiClient,
    private val streamingService: com.suvojeet.suvmusic.data.repository.youtube.streaming.YouTubeStreamingService,
    private val searchService: com.suvojeet.suvmusic.data.repository.youtube.search.YouTubeSearchService
) {
    companion object {
        private var isInitialized = false
        
        const val FILTER_SONGS = "music_songs"
        const val FILTER_VIDEOS = "music_videos"
        const val FILTER_ALBUMS = "music_albums"
        const val FILTER_PLAYLISTS = "music_playlists"
        const val FILTER_ARTISTS = "music_artists"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    


    // Comments Pagination State
    private var currentCommentsExtractor: org.schabi.newpipe.extractor.comments.CommentsExtractor? = null
    private var currentCommentsPage: org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage<*>? = null
    private var currentVideoIdForComments: String? = null

    init {
        initializeNewPipe()
    }

    private fun initializeNewPipe() {
        if (!isInitialized) {
            try {
                NewPipe.init(NewPipeDownloaderImpl(okHttpClient, sessionManager))
                isInitialized = true
            } catch (e: Exception) {
                isInitialized = true
            }
        }
    }

    /**
     * Fetch user account info (Name, Email, Avatar) from account menu.
     */
    suspend fun fetchAccountInfo(): com.suvojeet.suvmusic.data.SessionManager.StoredAccount? = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext null
            val cookies = sessionManager.getCookies() ?: return@withContext null
            
            // Use account_menu endpoint
            val jsonResponse = fetchInternalApi("account/account_menu")
            if (jsonResponse.isBlank()) return@withContext null
            
            val root = JSONObject(jsonResponse)
            
            // Parse response to find account info
            // Structure: actions -> openPopupAction -> popup -> multiPageMenuRenderer -> header -> activeAccountHeaderRenderer
            
            val actions = root.optJSONArray("actions")
            var accountHeader: JSONObject? = null
            
            if (actions != null) {
                for (i in 0 until actions.length()) {
                    val action = actions.optJSONObject(i)
                    val header = action?.optJSONObject("openPopupAction")
                        ?.optJSONObject("popup")
                        ?.optJSONObject("multiPageMenuRenderer")
                        ?.optJSONObject("header")
                        ?.optJSONObject("activeAccountHeaderRenderer")
                    
                    if (header != null) {
                        accountHeader = header
                        break
                    }
                }
            }
            
            if (accountHeader != null) {
                val name = getRunText(accountHeader.optJSONObject("accountName")) ?: "User"
                val email = getRunText(accountHeader.optJSONObject("email")) ?: ""
                
                val thumbnails = accountHeader.optJSONObject("avatar")
                    ?.optJSONArray("thumbnails")
                val avatarUrl = thumbnails?.let { 
                    it.optJSONObject(it.length() - 1)?.optString("url") 
                } ?: ""
                
                return@withContext com.suvojeet.suvmusic.data.SessionManager.StoredAccount(
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

    // ============================================================================================
    // Search & Stream (NewPipe)
    // ============================================================================================

    suspend fun search(query: String, filter: String = FILTER_SONGS): List<Song> = 
        searchService.search(query, filter)

    suspend fun searchArtists(query: String): List<Artist> = 
        searchService.searchArtists(query)

    suspend fun searchPlaylists(query: String): List<Playlist> = 
        searchService.searchPlaylists(query)

    suspend fun getSearchSuggestions(query: String): List<String> = 
        searchService.getSearchSuggestions(query)

    suspend fun getStreamUrl(videoId: String): String? = streamingService.getStreamUrl(videoId)

    suspend fun getVideoStreamUrl(videoId: String): String? = streamingService.getVideoStreamUrl(videoId)

    suspend fun getStreamUrlForDownload(videoId: String): String? = streamingService.getStreamUrlForDownload(videoId)

    suspend fun getSongDetails(videoId: String): Song? = streamingService.getSongDetails(videoId)

    suspend fun getRelatedSongs(videoId: String): List<Song> = 
        searchService.getRelatedSongs(videoId)

    // ============================================================================================
    // Browsing (Internal API)
    // ============================================================================================

    suspend fun getRecommendations(): List<Song> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) {
            return@withContext search("trending music 2024", FILTER_SONGS)
        }
        try {
            val jsonResponse = fetchInternalApi("FEmusic_home")
            val items = parseSongsFromInternalJson(jsonResponse)
            if (items.isNotEmpty()) return@withContext items
            getLikedMusic()
        } catch (e: Exception) {
            search("trending music 2024", FILTER_SONGS)
        }
    }

    suspend fun getHomeSections(): List<com.suvojeet.suvmusic.data.model.HomeSection> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) {
            // Try to fetch public YouTube Music content without auth
            val sections = mutableListOf<com.suvojeet.suvmusic.data.model.HomeSection>()
            
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
            val sectionQueries = listOf(
                "Trending Now" to "trending music 2024",
                "Top Hits" to "top hits 2024",
                "Bollywood Hits" to "bollywood hits latest",
                "Pop Hits" to "pop hits 2024",
                "Hip Hop & Rap" to "hip hop hits 2026",
                "Chill Vibes" to "chill lofi beats",
                "Punjabi Hits" to "punjabi hits latest",
                "90s Nostalgia" to "90s bollywood hits",
                "Workout Energy" to "workout music energy",
                "Romance" to "romantic songs love"
            )
            
            for ((title, query) in sectionQueries) {
                try {
                    val songs = search(query, FILTER_SONGS).take(10)
                        .map { com.suvojeet.suvmusic.data.model.HomeItem.SongItem(it) }
                    if (songs.isNotEmpty()) {
                        val type = when {
                            title.contains("Quick picks", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.VerticalList
                            title.contains("Fresh finds", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.Grid
                            title.contains("Community", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.LargeCardWithList
                            title.contains("Trending", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.HorizontalCarousel
                             else -> com.suvojeet.suvmusic.data.model.HomeSectionType.HorizontalCarousel
                        }
                        sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, songs, type))
                    }
                } catch (e: Exception) {
                    // Skip failed section
                }
            }
            
            return@withContext sections
        }

        try {
            val sections = mutableListOf<com.suvojeet.suvmusic.data.model.HomeSection>()
            
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

    private fun extractContinuationToken(json: JSONObject): String? = jsonParser.extractContinuationToken(json)

    private fun fetchInternalApiWithContinuation(continuationToken: String): String =
        apiClient.fetchInternalApiWithContinuation(continuationToken)

    private fun parseChartsSectionsFromJson(json: String): List<com.suvojeet.suvmusic.data.model.HomeSection> {
        val sections = mutableListOf<com.suvojeet.suvmusic.data.model.HomeSection>()
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
                            val items = mutableListOf<com.suvojeet.suvmusic.data.model.HomeItem>()

                            if (itemsArray != null) {
                                for (j in 0 until itemsArray.length()) {
                                    val itemObj = itemsArray.optJSONObject(j)
                                    parseHomeItem(itemObj)?.let { items.add(it) }
                                }
                            }

                            if (items.isNotEmpty()) {
                                val type = when {
                                    title.contains("Quick picks", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.VerticalList
                                    title.contains("Fresh finds", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.Grid
                                    title.contains("Community", ignoreCase = true) -> com.suvojeet.suvmusic.data.model.HomeSectionType.LargeCardWithList
                                    else -> com.suvojeet.suvmusic.data.model.HomeSectionType.HorizontalCarousel
                                }
                                sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, items, type))
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

    suspend fun getUserPlaylists(): List<PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<PlaylistDisplayItem>()

        if (sessionManager.isLoggedIn()) {
            try {
                val jsonResponse = fetchInternalApi("FEmusic_liked_playlists")
                playlists.addAll(parsePlaylistsFromInternalJson(jsonResponse))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        playlists
    }

    suspend fun getLikedMusic(fetchAll: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        if (sessionManager.isLoggedIn()) {
            try {
                val songs = mutableListOf<Song>()
                var jsonResponse = fetchInternalApi("FEmusic_liked_videos")
                songs.addAll(parseSongsFromInternalJson(jsonResponse))
                
                if (fetchAll) {
                    var currentJson = JSONObject(jsonResponse)
                    var continuationToken = extractContinuationToken(currentJson)
                    var pageCount = 0
                    val maxPages = 100 // Limit to ~10000 songs

                    while (continuationToken != null && pageCount < maxPages) {
                         try {
                            val continuationResponse = fetchInternalApiWithContinuation(continuationToken)
                            if (continuationResponse.isNotEmpty()) {
                                val newSongs = parseSongsFromInternalJson(continuationResponse)
                                if (newSongs.isEmpty()) break
                                
                                songs.addAll(newSongs)
                                
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
                
                if (songs.isNotEmpty()) return@withContext songs.distinctBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        getPlaylist("LM").songs 
    }
    
    suspend fun getPlaylist(playlistId: String): Playlist = withContext(Dispatchers.IO) {
        // Handle special playlists that require authentication
        if (playlistId == "LM" || playlistId == "VLLM") {
            // Use internal API for Liked Music if logged in
            if (sessionManager.isLoggedIn()) {
                try {
                    val json = fetchInternalApi("FEmusic_liked_videos")
                    val songs = parseSongsFromInternalJson(json)
                    if (songs.isNotEmpty()) {
                        return@withContext Playlist(
                            id = playlistId,
                            title = "Your Likes",
                            author = "You",
                            thumbnailUrl = songs.firstOrNull()?.thumbnailUrl,
                            songs = songs
                        )
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
                        return@withContext Playlist(
                            id = playlistId,
                            title = "My Supermix",
                            author = "YouTube Music",
                            thumbnailUrl = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked_music_@576.png",
                            songs = songs.take(50) // Limit to 50 songs
                        )
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
            if (playlist.songs.isNotEmpty()) return@withContext playlist
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
            val thumbnailUrl = try { 
                playlistExtractor.thumbnails?.lastOrNull()?.url 
            } catch (e: Exception) { null }
            
            val songs = playlistExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .mapNotNull { item ->
                    val videoId = extractVideoId(item.url)
                    // Get thumbnail from NewPipe, fallback to YouTube standard thumbnail URL
                    val itemThumbnail = item.thumbnails?.lastOrNull()?.url
                        ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                    
                    Song.fromYouTube(
                        videoId = videoId,
                        title = item.name ?: "Unknown",
                        artist = item.uploaderName ?: "Unknown Artist",
                        album = playlistName ?: "",
                        duration = item.duration * 1000L,
                        thumbnailUrl = itemThumbnail
                    )
                }
            
            Playlist(
                id = playlistId,
                title = playlistName ?: songs.firstOrNull()?.album?.takeIf { it.isNotBlank() } ?: "Playlist",
                author = uploaderName?.takeIf { it.isNotBlank() } ?: "",
                thumbnailUrl = thumbnailUrl ?: songs.firstOrNull()?.thumbnailUrl,
                songs = songs
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Playlist(playlistId, "Error loading playlist", "", null, emptyList())
        }
    }

    suspend fun getArtist(browseId: String): Artist? = withContext(Dispatchers.IO) {
        try {
            val json = fetchInternalApi(browseId)
            parseArtistFromInternalJson(json, browseId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAlbum(browseId: String): Album? = withContext(Dispatchers.IO) {
        try {
            val json = fetchInternalApi(browseId)
            parseAlbumFromInternalJson(json, browseId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get moods and genres (browse categories) from YouTube Music.
     * Used for the Apple Music-style search browse grid.
     */
    suspend fun getMoodsAndGenres(): List<com.suvojeet.suvmusic.data.model.BrowseCategory> = withContext(Dispatchers.IO) {
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

    suspend fun subscribe(channelId: String, isSubscribe: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
             val endpoint = if (isSubscribe) "subscription/subscribe" else "subscription/unsubscribe"
             val body = """
                 {
                     "channelIds": ["$channelId"]
                 }
             """.trimIndent()
             performAuthenticatedAction(endpoint, body)
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
    suspend fun getLyrics(videoId: String): com.suvojeet.suvmusic.data.model.Lyrics? = withContext(Dispatchers.IO) {
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
                com.suvojeet.suvmusic.data.model.Comment(
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
                com.suvojeet.suvmusic.data.model.Comment(
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
    
    private fun parseLyricsFromBrowse(json: JSONObject): com.suvojeet.suvmusic.data.model.Lyrics? {
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
                    val lines = mutableListOf<com.suvojeet.suvmusic.data.model.LyricsLine>()
                    for (i in 0 until lyricData.length()) {
                        val lineObj = lyricData.optJSONObject(i)
                        val text = lineObj?.optString("lyricLine") ?: ""
                        val startTime = lineObj?.optLong("cueRangeStartMillis") ?: 0L
                        
                        if (text.isNotBlank()) {
                            lines.add(com.suvojeet.suvmusic.data.model.LyricsLine(
                                text = text,
                                startTimeMs = startTime
                            ))
                        }
                    }
                    
                    val footer = getRunText(timedLyrics.optJSONObject("footer")) 
                        ?: getRunText(descriptionShelf?.optJSONObject("footer"))
                    
                    if (lines.isNotEmpty()) {
                        return com.suvojeet.suvmusic.data.model.Lyrics(lines, footer, true)
                    }
                }
            }
            
            // 2. Fallback to Plain Text
            if (descriptionShelf != null) {
                val description = getRunText(descriptionShelf.optJSONObject("description"))
                val footer = getRunText(descriptionShelf.optJSONObject("footer"))
                
                if (description != null) {
                    val lines = description.split("\r\n", "\n").map { com.suvojeet.suvmusic.data.model.LyricsLine(it) }
                    return com.suvojeet.suvmusic.data.model.Lyrics(lines, footer, false)
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
        val songCountStr = parts.find { it.contains("song", ignoreCase = true) }
        val songCount = songCountStr?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        
        val uploader = parts.firstOrNull { !it.contains("song", ignoreCase = true) } ?: "YouTube User"

        return PlaylistDisplayItem(
            id = browseId,
            name = title,
            url = "https://music.youtube.com/playlist?list=$browseId",
            uploaderName = uploader,
            thumbnailUrl = thumbnail,
            songCount = songCount
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
            songs = songs
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
                    val videoId = extractValueFromRuns(item, "videoId") ?: item.optString("videoId").takeIf { it.isNotEmpty() }
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
                        
                        // Parse song count from subtitle (e.g., "Suvojeet • 50 songs")
                        val parts = subtitle.split("•").map { it.trim() }
                        val songCountStr = parts.find { it.contains("song", ignoreCase = true) }
                        val songCount = songCountStr?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                        
                        // Uploader is usually the first part if it's not the song count
                        val uploaderName = parts.firstOrNull { !it.contains("song", ignoreCase = true) } ?: subtitle
                        
                        val thumbnailUrl = extractThumbnail(item)

                        playlists.add(PlaylistDisplayItem(
                            id = cleanId,
                            name = title,
                            url = "https://music.youtube.com/playlist?list=$cleanId",
                            uploaderName = uploaderName,
                            thumbnailUrl = thumbnailUrl,
                            songCount = songCount
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
         // Header
         val header = root.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")
             ?: root.optJSONObject("header")?.optJSONObject("musicResponsiveHeaderRenderer")
         
         val title = getRunText(header?.optJSONObject("title")) ?: "Unknown Album"
         val subtitle = getRunText(header?.optJSONObject("subtitle")) 
             ?: getRunText(header?.optJSONObject("straplineTextOne")) // Artist • Year • ...
             
         val description = getRunText(header?.optJSONObject("description"))
             ?: getRunText(header?.optJSONObject("secondSubtitle"))
             
         val thumbnailUrl = extractHeaderThumbnail(header)
         
         val songs = parseSongsFromInternalJson(json)
         
         return Album(
             id = albumId,
             title = title,
             artist = subtitle?.split("•")?.firstOrNull()?.trim() ?: "Unknown",
             year = subtitle,
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
            com.suvojeet.suvmusic.data.model.HomeItem.ExploreItem("New releases", com.suvojeet.suvmusic.R.drawable.ic_music_note), // Placeholder ID
            com.suvojeet.suvmusic.data.model.HomeItem.ExploreItem("Charts", com.suvojeet.suvmusic.R.drawable.ic_waveform), // Placeholder ID
            com.suvojeet.suvmusic.data.model.HomeItem.ExploreItem("Moods and genres", com.suvojeet.suvmusic.R.drawable.ic_play), // Placeholder ID
            com.suvojeet.suvmusic.data.model.HomeItem.ExploreItem("Podcasts", com.suvojeet.suvmusic.R.drawable.ic_launcher_monochrome) // Placeholder ID
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
}
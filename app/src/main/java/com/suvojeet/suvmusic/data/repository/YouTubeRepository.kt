package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.data.NewPipeDownloaderImpl
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.YouTubeAuthUtils
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Artist
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching data from YouTube Music.
 * Uses NewPipeExtractor for streams and search, and internal API for browsing/library.
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val sessionManager: SessionManager
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

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

    // ============================================================================================
    // Search & Stream (NewPipe)
    // ============================================================================================

    suspend fun search(query: String, filter: String = FILTER_SONGS): List<Song> = withContext(Dispatchers.IO) {
        try {
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext emptyList()
            
            val searchExtractor = ytService.getSearchExtractor(query, listOf(filter), "")
            searchExtractor.fetchPage()
            
            searchExtractor.initialPage.items.filterIsInstance<StreamInfoItem>().mapNotNull { item ->
                try {
                    Song.fromYouTube(
                        videoId = extractVideoId(item.url),
                        title = item.name ?: "Unknown",
                        artist = item.uploaderName ?: "Unknown Artist",
                        album = "",
                        duration = item.duration * 1000L,
                        thumbnailUrl = item.thumbnails?.firstOrNull()?.url
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get search suggestions for autocomplete.
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        
        try {
            val url = "https://suggestqueries-clients6.youtube.com/complete/search?client=youtube&ds=yt&q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            
            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            
            // Response format: window.google.ac.h(["query",[["suggestion1",0],["suggestion2",0],...]])
            val jsonStart = body.indexOf("[[")
            val jsonEnd = body.lastIndexOf("]]") + 2
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) return@withContext emptyList()
            
            val suggestionsArray = JSONArray(body.substring(jsonStart, jsonEnd))
            val suggestions = mutableListOf<String>()
            
            for (i in 0 until suggestionsArray.length()) {
                val suggestionItem = suggestionsArray.optJSONArray(i)
                if (suggestionItem != null && suggestionItem.length() > 0) {
                    val text = suggestionItem.optString(0)
                    if (text.isNotBlank()) {
                        suggestions.add(text)
                    }
                }
            }
            
            suggestions.take(8) // Limit to 8 suggestions
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext null
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            val targetBitrate = when (sessionManager.getAudioQuality()) {
                com.suvojeet.suvmusic.data.model.AudioQuality.LOW -> 64
                com.suvojeet.suvmusic.data.model.AudioQuality.MEDIUM -> 128
                com.suvojeet.suvmusic.data.model.AudioQuality.HIGH -> 256
                com.suvojeet.suvmusic.data.model.AudioQuality.BEST -> Int.MAX_VALUE
            }
            
            val bestAudioStream = audioStreams
                .filter { it.averageBitrate <= targetBitrate || targetBitrate == Int.MAX_VALUE }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }
            
            bestAudioStream?.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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

    suspend fun getLikedMusic(): List<Song> = withContext(Dispatchers.IO) {
        if (sessionManager.isLoggedIn()) {
            try {
                val json = fetchInternalApi("FEmusic_liked_videos")
                val songs = parseSongsFromInternalJson(json)
                if (songs.isNotEmpty()) return@withContext songs
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
                    Song.fromYouTube(
                        videoId = extractVideoId(item.url),
                        title = item.name ?: "Unknown",
                        artist = item.uploaderName ?: "Unknown Artist",
                        album = playlistName ?: "",
                        duration = item.duration * 1000L,
                        thumbnailUrl = item.thumbnails?.lastOrNull()?.url // Use last (highest quality)
                    )
                }
            
            Playlist(
                id = playlistId,
                title = playlistName ?: songs.firstOrNull()?.album?.takeIf { it.isNotBlank() } ?: "Playlist",
                author = uploaderName ?: "Unknown",
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
            
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("playlistId", playlistId)
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
        val subtitle = extractArtist(item) 
        
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
    // Internal API Helpers
    // ============================================================================================

    private fun fetchInternalApi(endpoint: String): String {
        val cookies = sessionManager.getCookies() ?: return ""
        val isBrowse = !endpoint.contains("/")
        
        val url = if (isBrowse) {
            "https://music.youtube.com/youtubei/v1/browse"
        } else {
            "https://music.youtube.com/youtubei/v1/$endpoint"
        }
        
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
        
        val contextJson = """
            "context": {
                "client": {
                    "clientName": "WEB_REMIX",
                    "clientVersion": "1.20230102.01.00",
                    "hl": "en",
                    "gl": "US"
                }
            }
        """.trimIndent()

        val jsonBody = if (isBrowse) {
            "{ $contextJson, \"browseId\": \"$endpoint\" }"
        } else {
            "{ $contextJson }"
        }

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Cookie", cookies)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("X-Goog-AuthUser", "0")
            .build()

        return try {
            okHttpClient.newCall(request).execute().body?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun performAuthenticatedAction(endpoint: String, innerBody: String): Boolean {
        if (!sessionManager.isLoggedIn()) return false
        val cookies = sessionManager.getCookies() ?: return false
        
        val url = "https://music.youtube.com/youtubei/v1/$endpoint"
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: return false

        val fullBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20230102.01.00",
                        "hl": "en",
                        "gl": "US"
                    }
                },
                ${innerBody.removePrefix("{").removeSuffix("}")}
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(fullBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Cookie", cookies)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("X-Goog-AuthUser", "0")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

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
            ?: "Unknown"
        
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
    
    private fun extractHeaderThumbnail(header: JSONObject?): String? {
        if (header == null) return null
        
        // Standard musicThumbnailRenderer path
        val thumbnails = header.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        
        if (thumbnails != null && thumbnails.length() > 0) {
            return thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
        }
        
        // Alternative path for some playlists
        val altThumbnails = header.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        
        if (altThumbnails != null && altThumbnails.length() > 0) {
            return altThumbnails.optJSONObject(altThumbnails.length() - 1)?.optString("url")
        }
        
        return null
    }

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
                        
                        Song.fromYouTube(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            album = "",
                            duration = 0L,
                            thumbnailUrl = thumbnailUrl
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
                        val thumbnailUrl = extractThumbnail(item)

                        playlists.add(PlaylistDisplayItem(
                            id = cleanId,
                            name = title,
                            url = "https://music.youtube.com/playlist?list=$cleanId",
                            uploaderName = subtitle,
                            thumbnailUrl = thumbnailUrl
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
        val thumbnailUrl = extractThumbnail(header?.optJSONObject("thumbnail") ?: header?.optJSONObject("foregroundThumbnail")) // check foreground for immersive
        val subscribers = getRunText(header?.optJSONObject("subscriptionButton")?.optJSONObject("subscribeButtonRenderer")?.optJSONObject("subscriberCountText"))

        val songs = mutableListOf<Song>()
        val albums = mutableListOf<Album>()
        val singles = mutableListOf<Album>()

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
                             duration = 0L,
                             thumbnailUrl = extractThumbnail(item)
                         )?.let { songs.add(it) }
                     }
                 }
            } else if (title?.contains("Albums", ignoreCase = true) == true) {
                parseAlbums(contents, albums, name)
            } else if (title?.contains("Singles", ignoreCase = true) == true) {
                parseAlbums(contents, singles, name)
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
            singles = singles
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
         val title = getRunText(header?.optJSONObject("title")) ?: "Unknown Album"
         val subtitle = getRunText(header?.optJSONObject("subtitle")) // Artist • Year • ...
         val description = getRunText(header?.optJSONObject("description"))
         val thumbnailUrl = extractThumbnail(header?.optJSONObject("thumbnail"))
         
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

    // --- JSON Helpers ---

    private fun findAllObjects(node: Any, key: String, results: MutableList<JSONObject>) {
        if (node is JSONObject) {
            if (node.has(key)) {
                results.add(node.getJSONObject(key))
            }
            val keys = node.keys()
            while (keys.hasNext()) {
                val nextKey = keys.next()
                findAllObjects(node.get(nextKey), key, results)
            }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) {
                findAllObjects(node.get(i), key, results)
            }
        }
    }

    private fun getRunText(formattedString: JSONObject?): String? {
        if (formattedString == null) return null
        if (formattedString.has("simpleText")) {
            return formattedString.optString("simpleText")
        }
        val runs = formattedString.optJSONArray("runs") ?: return null
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text") ?: "")
        }
        return sb.toString()
    }

    private fun extractValueFromRuns(item: JSONObject, key: String): String? {
        val endpoints = mutableListOf<JSONObject>()
        findAllObjects(item, "watchEndpoint", endpoints)
        return endpoints.firstOrNull()?.optString("videoId")
    }
    
    private fun extractTitle(item: JSONObject): String {
        val flexColumns = item.optJSONArray("flexColumns")
        val titleFormatted = flexColumns?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
        return getRunText(titleFormatted) ?: getRunText(item.optJSONObject("title")) ?: "Unknown"
    }
    
    private fun extractArtist(item: JSONObject): String {
        val flexColumns = item.optJSONArray("flexColumns")
        val subtitleFormatted = flexColumns?.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
        val subtitleRuns = subtitleFormatted?.optJSONArray("runs")
            ?: item.optJSONObject("subtitle")?.optJSONArray("runs")
        return subtitleRuns?.optJSONObject(0)?.optString("text") ?: "Unknown Artist"
    }
    
    private fun extractThumbnail(item: JSONObject?): String? {
        if (item == null) return null
        val thumbnails = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: item.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            ?: item.optJSONArray("thumbnails") // For header thumbnail
        
        return thumbnails?.let { it.optJSONObject(it.length() - 1)?.optString("url") }
    }

    private fun extractVideoId(url: String): String {
        val patterns = listOf(
            Regex("watch\\?v=([a-zA-Z0-9_-]+)"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),
            Regex("music\\.youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)")
        )
        
        for (pattern in patterns) {
            pattern.find(url)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return url
    }
}
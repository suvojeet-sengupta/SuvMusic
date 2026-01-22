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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    // Cache for stream URLs to avoid re-fetching (max 50 entries, 30 min expiry)
    private data class CachedStream(val url: String, val timestamp: Long)
    private val streamCache = LruCache<String, CachedStream>(50)
    private val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes

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
                    // Extract artist ID from uploader URL (format: youtube.com/channel/UC...)
                    val artistId = item.uploaderUrl?.let { url ->
                        when {
                            url.contains("/channel/") -> url.substringAfter("/channel/").substringBefore("/").substringBefore("?")
                            url.contains("/@") -> null // Handle URLs don't have direct channel IDs
                            else -> null
                        }
                    }
                    
                    Song.fromYouTube(
                        videoId = extractVideoId(item.url),
                        title = item.name ?: "Unknown",
                        artist = item.uploaderName ?: "Unknown Artist",
                        album = "",
                        duration = item.duration * 1000L,
                        thumbnailUrl = item.thumbnails?.maxByOrNull { it.width * it.height }?.url,
                        artistId = artistId,
                        isVideo = filter == FILTER_VIDEOS
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
     * Search for artists/channels on YouTube Music.
     * Returns a list of Artist objects with basic info (id, name, thumbnail, subscribers).
     */
    suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        try {
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext emptyList()
            
            val searchExtractor = ytService.getSearchExtractor(query, listOf("channels"), "")
            searchExtractor.fetchPage()
            
            searchExtractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.channel.ChannelInfoItem>().take(3).mapNotNull { item ->
                try {
                    val channelId = item.url?.substringAfter("/channel/")?.substringBefore("/")?.substringBefore("?")
                    if (channelId.isNullOrBlank()) return@mapNotNull null
                    
                    Artist(
                        id = channelId,
                        name = item.name ?: "Unknown Artist",
                        thumbnailUrl = item.thumbnails?.lastOrNull()?.url,
                        subscribers = item.subscriberCount?.let { 
                            if (it >= 1_000_000) "${it / 1_000_000}M subscribers"
                            else if (it >= 1_000) "${it / 1_000}K subscribers"
                            else "$it subscribers"
                        }
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
     * Search for playlists on YouTube Music.
     * Returns a list of Playlist objects with basic info (id, title, author, thumbnail).
     */
    suspend fun searchPlaylists(query: String): List<Playlist> = withContext(Dispatchers.IO) {
        try {
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext emptyList()
            
            val searchExtractor = ytService.getSearchExtractor(query, listOf(FILTER_PLAYLISTS), "")
            searchExtractor.fetchPage()
            
            searchExtractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.playlist.PlaylistInfoItem>().take(5).mapNotNull { item ->
                try {
                    val playlistId = item.url?.substringAfter("list=")?.substringBefore("&")
                    if (playlistId.isNullOrBlank()) return@mapNotNull null
                    
                    Playlist(
                        id = playlistId,
                        title = item.name ?: "Unknown Playlist",
                        author = item.uploaderName ?: "",
                        thumbnailUrl = item.thumbnails?.lastOrNull()?.url,
                        songs = emptyList() // Will be loaded when clicked
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
        // Check cache first for fast playback
        val cacheKey = "audio_$videoId"
        streamCache.get(cacheKey)?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRY_MS) {
                android.util.Log.d("YouTubeRepo", "Stream URL from cache: $videoId")
                return@withContext cached.url
            }
        }
        
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
            
            bestAudioStream?.content?.also { url ->
                // Cache the result
                streamCache.put(cacheKey, CachedStream(url, System.currentTimeMillis()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get video stream URL for video playback mode.
     * Returns the best quality video stream that includes audio (for combined playback).
     */
    suspend fun getVideoStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // Check cache first for fast playback
        val cacheKey = "video_$videoId"
        streamCache.get(cacheKey)?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRY_MS) {
                android.util.Log.d("YouTubeRepo", "Video stream URL from cache: $videoId")
                return@withContext cached.url
            }
        }
        
        try {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext null
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            // Get video streams (these include audio in the stream)
            val videoStreams = streamExtractor.videoStreams
            
            // Filter for streams with resolution <= 720p to reduce bandwidth
            // and sort by resolution to get best quality
            val bestVideoStream = videoStreams
                .filter { 
                    val height = it.resolution?.replace("p", "")?.toIntOrNull() ?: 0
                    height <= 720 && height > 0
                }
                .maxByOrNull { 
                    it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 
                }
                ?: videoStreams.firstOrNull() // Fallback to any available stream
            
            android.util.Log.d("YouTubeRepo", "Video stream: ${bestVideoStream?.resolution}")
            
            bestVideoStream?.content?.also { url ->
                // Cache the result
                streamCache.put(cacheKey, CachedStream(url, System.currentTimeMillis()))
            }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeRepo", "Error getting video stream", e)
            null
        }
    }

    /**
     * Get stream URL for downloading with the user's download quality preference.
     */
    suspend fun getStreamUrlForDownload(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext null
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val audioStreams = streamExtractor.audioStreams
            val targetBitrate = sessionManager.getDownloadQuality().maxBitrate
            
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

    /**
     * Get song details from a video ID.
     * Used for deep linking to play songs from YouTube/YouTube Music URLs.
     */
    suspend fun getSongDetails(videoId: String): Song? = withContext(Dispatchers.IO) {
        try {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext null
            
            val streamExtractor = ytService.getStreamExtractor(streamUrl)
            streamExtractor.fetchPage()
            
            val title = streamExtractor.name ?: "Unknown Title"
            val artist = streamExtractor.uploaderName ?: "Unknown Artist"
            val thumbnailUrl = streamExtractor.thumbnails.maxByOrNull { it.width * it.height }?.url

            val duration = streamExtractor.length * 1000 // Convert to milliseconds
            
            Song(
                id = videoId,
                title = title,
                artist = artist,
                album = "", // Not available from stream extractor
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                source = com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get related songs (Up Next / Radio) for a specific video.
     * Uses YouTube Music's "next" endpoint which provides the official recommendations.
     */
    suspend fun getRelatedSongs(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val cookies = sessionManager.getCookies()
            val authHeader = if (cookies != null) YouTubeAuthUtils.getAuthorizationHeader(cookies) else ""

            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230102.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("videoId", videoId)
                put("enablePersistentPlaylistPanel", true)
                put("isAudioOnly", true)
            }

            val request = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/next")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .apply {
                    if (cookies != null) addHeader("Cookie", cookies)
                    if (authHeader != null && authHeader.isNotEmpty()) addHeader("Authorization", authHeader)
                    addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    addHeader("Origin", "https://music.youtube.com")
                    addHeader("X-Goog-AuthUser", "0")
                }
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            
            parseSongsFromNextResponse(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseSongsFromNextResponse(json: String): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val root = JSONObject(json)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicQueueRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("playlistPanelRenderer")
                ?.optJSONArray("contents")

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val item = contents.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer")
                    if (item != null) {
                        val videoId = item.optString("videoId")
                        val title = getRunText(item.optJSONObject("title")) ?: "Unknown"
                        val longByline = getRunText(item.optJSONObject("longBylineText")) ?: ""
                        
                        // longByline is typically "Artist • Album" or just "Artist"
                        val artist = longByline.split("•").firstOrNull()?.trim() ?: "Unknown Artist"
                        val album = if (longByline.contains("•")) longByline.split("•").lastOrNull()?.trim() ?: "" else ""
                        
                        val lengthText = getRunText(item.optJSONObject("lengthText")) ?: ""
                        val duration = parseDurationText(lengthText)
                        
                        val thumbnail = extractThumbnail(item)
                        
                        // setVideoId is used for moving/removing items in the specific queue instance
                        val setVideoId = item.optString("setVideoId")

                        Song.fromYouTube(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            thumbnailUrl = thumbnail,
                            setVideoId = setVideoId
                        )?.let { songs.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songs
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
                        sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, songs))
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
            
            return@withContext sections.distinctBy { it.title }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun extractContinuationToken(json: JSONObject): String? {
        try {
            // 1. Get the sectionListRenderer (common root)
            val sectionListRenderer = json.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?: json.optJSONObject("continuationContents")
                ?.optJSONObject("sectionListContinuation")
            
            // 2. Check for direct section continuations (infinite scroll on home/mixed lists)
            val sectionContinuations = sectionListRenderer?.optJSONArray("continuations")
            if (sectionContinuations != null) {
                return sectionContinuations.optJSONObject(0)
                    ?.optJSONObject("nextContinuationData")
                    ?.optString("continuation")
            }

            // 3. Look inside contents for shelf continuations (Playlists, Liked Songs, Shelves)
            val contents = sectionListRenderer?.optJSONArray("contents") 
                ?: json.optJSONObject("contents")?.optJSONObject("sectionListRenderer")?.optJSONArray("contents") // Fallback
            
            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val item = contents.optJSONObject(i)
                    
                    // Try MusicPlaylistShelfRenderer (Liked Songs, Playlists)
                    val playlistShelf = item?.optJSONObject("musicPlaylistShelfRenderer")
                        ?: json.optJSONObject("continuationContents")?.optJSONObject("musicPlaylistShelfContinuation")
                        
                    var continuations = playlistShelf?.optJSONArray("continuations")
                    if (continuations != null) {
                        return continuations.optJSONObject(0)
                            ?.optJSONObject("nextContinuationData")
                            ?.optString("continuation")
                    }
                    
                    // Try MusicShelfRenderer (Category lists, some playlists)
                    val musicShelf = item?.optJSONObject("musicShelfRenderer")
                         ?: json.optJSONObject("continuationContents")?.optJSONObject("musicShelfContinuation")

                    continuations = musicShelf?.optJSONArray("continuations")
                    if (continuations != null) {
                        return continuations.optJSONObject(0)
                            ?.optJSONObject("nextContinuationData")
                            ?.optString("continuation")
                    }
                }
            }
            
            // 4. Fallback: check root continuationContents directly (standard for next pages)
            val rootContinuation = json.optJSONObject("continuationContents")
            if (rootContinuation != null) {
                 val playlistContinuation = rootContinuation.optJSONObject("musicPlaylistShelfContinuation")
                 val shelfContinuation = rootContinuation.optJSONObject("musicShelfContinuation")
                 
                 val target = playlistContinuation ?: shelfContinuation
                 val continuations = target?.optJSONArray("continuations")
                 if (continuations != null) {
                        return continuations.optJSONObject(0)
                            ?.optJSONObject("nextContinuationData")
                            ?.optString("continuation")
                 }
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }

    private fun fetchInternalApiWithContinuation(continuationToken: String): String {
        val cookies = sessionManager.getCookies() ?: return ""
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20230102.01.00",
                        "hl": "en",
                        "gl": "US"
                    }
                }
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/browse?ctoken=$continuationToken&continuation=$continuationToken")
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
                                sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, items))
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

    private fun fetchInternalApiWithParams(browseId: String, params: String): String {
        val cookies = sessionManager.getCookies() ?: return ""
        val authHeader = YouTubeAuthUtils.getAuthorizationHeader(cookies) ?: ""
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20230102.01.00",
                        "hl": "en",
                        "gl": "US"
                    }
                },
                "browseId": "$browseId",
                "params": "$params"
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/browse")
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

    /**
     * Fetch public YouTube Music API without authentication.
     * Used for charts, trending, and public browse content.
     */
    private fun fetchPublicApi(browseId: String): String {
        val url = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
        
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20240101.01.00",
                        "hl": "en",
                        "gl": "IN"
                    }
                },
                "browseId": "$browseId"
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("Referer", "https://music.youtube.com/")
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
                             duration = extractDuration(item),
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
                            sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, items))
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
                            sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, items))
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
                            sections.add(com.suvojeet.suvmusic.data.model.HomeSection(title, items))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sections
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

    private fun extractFullSubtitle(item: JSONObject): String {
        val flexColumns = item.optJSONArray("flexColumns")
        if (flexColumns != null) {
            val subtitleFormatted = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
            return getRunText(subtitleFormatted) ?: ""
        }
        return getRunText(item.optJSONObject("subtitle")) ?: ""
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
    
    private fun extractDuration(item: JSONObject): Long {
        // Try fixedColumns (most common for list items)
        val fixedColumns = item.optJSONArray("fixedColumns")
        if (fixedColumns != null) {
            for (i in 0 until fixedColumns.length()) {
                val col = fixedColumns.optJSONObject(i)
                    ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                val text = getRunText(col?.optJSONObject("text"))
                if (text != null) {
                    val duration = parseDurationText(text)
                    if (duration > 0) return duration
                }
            }
        }
        
        // Try overlay for two-row items
        val overlayText = item.optJSONObject("overlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("accessibilityPlayData")
            ?.optJSONObject("accessibilityData")
            ?.optString("label")
        if (overlayText != null) {
            // Try to parse duration from accessibility text like "Play Song Name - 3 minutes, 45 seconds"
            val durationMatch = Regex("(\\d+)\\s*minutes?,?\\s*(\\d+)?\\s*seconds?").find(overlayText)
            if (durationMatch != null) {
                val minutes = durationMatch.groupValues[1].toLongOrNull() ?: 0L
                val seconds = durationMatch.groupValues.getOrNull(2)?.toLongOrNull() ?: 0L
                return (minutes * 60 + seconds) * 1000L
            }
        }
        
        // Try subtitle runs for duration text
        val subtitleRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
        if (subtitleRuns != null) {
            for (i in 0 until subtitleRuns.length()) {
                val text = subtitleRuns.optJSONObject(i)?.optString("text") ?: continue
                val duration = parseDurationText(text)
                if (duration > 0) return duration
            }
        }
        
        return 0L
    }
    
    private fun parseDurationText(text: String): Long {
        // Handle formats like "3:45", "1:23:45", "45"
        val parts = text.trim().split(":")
        return when (parts.size) {
            3 -> {
                // H:MM:SS
                val hours = parts[0].toLongOrNull() ?: return 0L
                val minutes = parts[1].toLongOrNull() ?: return 0L
                val seconds = parts[2].toLongOrNull() ?: return 0L
                (hours * 3600 + minutes * 60 + seconds) * 1000L
            }
            2 -> {
                // M:SS
                val minutes = parts[0].toLongOrNull() ?: return 0L
                val seconds = parts[1].toLongOrNull() ?: return 0L
                (minutes * 60 + seconds) * 1000L
            }
            1 -> {
                // Just seconds
                val seconds = parts[0].toLongOrNull() ?: return 0L
                seconds * 1000L
            }
            else -> 0L
        }
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
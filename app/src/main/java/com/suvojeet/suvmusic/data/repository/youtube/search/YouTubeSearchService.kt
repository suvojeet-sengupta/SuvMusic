package com.suvojeet.suvmusic.data.repository.youtube.search

import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.YouTubeAuthUtils
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles all YouTube Music search functionality.
 * Includes song search, artist search, playlist search, suggestions, and recommendations.
 */
@Singleton
class YouTubeSearchService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
    private val jsonParser: YouTubeJsonParser
) {

    companion object {
        const val FILTER_SONGS = "music_songs"
        const val FILTER_VIDEOS = "music_videos"
        const val FILTER_ALBUMS = "music_albums"
        const val FILTER_PLAYLISTS = "music_playlists"
        const val FILTER_ARTISTS = "music_artists"
    }

    /**
     * Search for songs/videos on YouTube Music.
     */
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
                        videoId = jsonParser.extractVideoId(item.url),
                        title = item.name ?: "Unknown",
                        artist = item.uploaderName ?: "Unknown Artist",
                        album = "",
                        duration = item.duration * 1000L,
                        thumbnailUrl = item.thumbnails.maxByOrNull { it.width * it.height }?.url,
                        artistId = artistId,
                        isVideo = filter == FILTER_VIDEOS,
                        isMembersOnly = false // Default to false until we verify the field name
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
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                        subscribers = item.subscriberCount.let { 
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
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url,
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
     * Search for albums on YouTube Music.
     */
    suspend fun searchAlbums(query: String): List<com.suvojeet.suvmusic.core.model.Album> = withContext(Dispatchers.IO) {
        try {
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" } 
                ?: return@withContext emptyList()
            
            val searchExtractor = ytService.getSearchExtractor(query, listOf(FILTER_ALBUMS), "")
            searchExtractor.fetchPage()
            
            searchExtractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.playlist.PlaylistInfoItem>().mapNotNull { item ->
                try {
                    val albumId = item.url?.substringAfter("list=")?.substringBefore("&")
                    if (albumId.isNullOrBlank()) return@mapNotNull null
                    
                    com.suvojeet.suvmusic.core.model.Album(
                        id = albumId,
                        title = item.name ?: "Unknown Album",
                        artist = item.uploaderName ?: "Unknown Artist",
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                        year = "" // NewPipe often doesn't give year in search results
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
            val body = response.body.string()
            
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
                // RDAMVM + videoId is the standard radio playlist for a song on YT Music
                put("playlistId", "RDAMVM$videoId")
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
            val responseBody = response.body.string()
            
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
            
            // Use a more robust deep search for the playlist items
            val playlistItems = mutableListOf<JSONObject>()
            jsonParser.findAllObjects(root, "playlistPanelVideoRenderer", playlistItems)
            
            if (playlistItems.isNotEmpty()) {
                for (item in playlistItems) {
                    val videoId = item.optString("videoId")
                    if (videoId.isNullOrBlank()) continue
                    
                    val title = jsonParser.getRunText(item.optJSONObject("title")) ?: "Unknown"
                    
                    // Extract artist and album from byline or longByline
                    val bylineObj = item.optJSONObject("longBylineText") ?: item.optJSONObject("shortBylineText")
                    val fullByline = jsonParser.getRunText(bylineObj) ?: ""
                    
                    val parts = fullByline.split(" • ").map { it.trim() }
                    val artist = parts.firstOrNull() ?: "Unknown Artist"
                    val album = parts.getOrNull(1) ?: ""
                    
                    val lengthText = jsonParser.getRunText(item.optJSONObject("lengthText")) ?: ""
                    val duration = jsonParser.parseDurationText(lengthText)
                    
                    val thumbnail = jsonParser.extractThumbnail(item)
                    val setVideoId = item.optString("setVideoId")

                    Song.fromYouTube(
                        videoId = videoId,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        thumbnailUrl = thumbnail,
                        setVideoId = setVideoId,
                        isMembersOnly = false
                    )?.let { songs.add(it) }
                }
            } else {
                // Fallback to older navigation if deep search yielded nothing
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
                            val title = jsonParser.getRunText(item.optJSONObject("title")) ?: "Unknown"
                            val longByline = jsonParser.getRunText(item.optJSONObject("longBylineText")) ?: ""
                            
                            val artist = longByline.split("•").firstOrNull()?.trim() ?: "Unknown Artist"
                            val album = if (longByline.contains("•")) longByline.split("•").lastOrNull()?.trim() ?: "" else ""
                            
                            val lengthText = jsonParser.getRunText(item.optJSONObject("lengthText")) ?: ""
                            val duration = jsonParser.parseDurationText(lengthText)
                            
                            val thumbnail = jsonParser.extractThumbnail(item)
                            val setVideoId = item.optString("setVideoId")

                            Song.fromYouTube(
                                videoId = videoId,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                thumbnailUrl = thumbnail,
                                setVideoId = setVideoId,
                                isMembersOnly = false
                            )?.let { songs.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songs.distinctBy { it.id }
    }
}

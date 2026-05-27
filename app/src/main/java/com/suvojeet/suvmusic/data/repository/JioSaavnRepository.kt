package com.suvojeet.suvmusic.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64
import com.suvojeet.suvmusic.util.encodeUrl
import com.suvojeet.suvmusic.util.decodeHtml
import com.suvojeet.suvmusic.util.toHighResImage

/**
 * Repository for fetching music from JioSaavn.
 * Uses JioSaavn's public (unofficial) internal API endpoints.
 * Supports 320kbps high-quality audio.
 */
@Singleton
class JioSaavnRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    // In-memory caches to reduce server load and improve performance
    private val searchCache = mutableMapOf<String, List<Song>>()
    private val songDetailsCache = mutableMapOf<String, Song>()
    private val streamUrlCache = mutableMapOf<String, String>()
    private val playlistCache = mutableMapOf<String, Playlist>()

    companion object {
        // JioSaavn internal API endpoint.
        private const val BASE_URL = "https://www.jiosaavn.com/api.php"

        // Public DES key used by JioSaavn to encrypt media URLs (DES/ECB/PKCS5).
        private const val DES_KEY = "38346591"

        // Quality suffixes for stream URLs
        private const val QUALITY_96 = "_96.mp4"
        private const val QUALITY_160 = "_160.mp4"
        private const val QUALITY_320 = "_320.mp4"
    }
    
    /**
     * Search for songs on JioSaavn.
     */
    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val cacheKey = query.trim().lowercase()
        if (searchCache.containsKey(cacheKey)) {
            return@withContext searchCache[cacheKey] ?: emptyList()
        }

        try {
            val url = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&n=20&q=${query.encodeUrl()}"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            val results = json.getAsJsonArray("results") ?: return@withContext emptyList()
            
            val songs = results.mapNotNull { element ->
                val song = element.asJsonObject
                parseSong(song)
            }
            
            if (songs.isNotEmpty()) {
                searchCache[cacheKey] = songs
                // Also cache individual song details
                songs.forEach { song -> songDetailsCache[song.id] = song }
            } else {
                android.util.Log.w("JioSaavn", "search('$query') parsed 0 songs from response")
            }
            songs
        } catch (e: Exception) {
            android.util.Log.e("JioSaavn", "search('$query') failed: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Search for artists on JioSaavn.
     */
    suspend fun searchArtists(query: String): List<com.suvojeet.suvmusic.core.model.Artist> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?__call=search.getArtistResults&_format=json&n=5&q=${query.encodeUrl()}"
            val response = makeRequest(url)
            val json = JsonParser.parseString(response).asJsonObject
            val results = json.getAsJsonArray("results") ?: return@withContext emptyList()
            
            results.mapNotNull { element ->
                val artist = element.asJsonObject
                val id = artist.get("id")?.asString ?: return@mapNotNull null
                val name = artist.get("name")?.asString ?: artist.get("title")?.asString ?: ""
                val image = artist.get("image")?.asString?.toHighResImage()
                
                com.suvojeet.suvmusic.core.model.Artist(
                    id = id,
                    name = name.decodeHtml(),
                    thumbnailUrl = image
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Get artist details by ID.
     */
    suspend fun getArtist(artistId: String): com.suvojeet.suvmusic.core.model.Artist? = withContext(Dispatchers.IO) {
        try {
            // Try fetching artist details using webapi.get
            // JioSaavn uses a "token" (which is the ID we get from search) or a permalink
            // Usually the ID from search results is the one we need.
            val url = "$BASE_URL?__call=webapi.get&token=$artistId&type=artist&p=1&n_song=20&n_album=20&sub_type=songs&category=&sort_order=&includeMetaTags=0&ctx=web6dot0&api_version=4&_format=json&_marker=0"
            val response = makeRequest(url)
            val json = JsonParser.parseString(response).asJsonObject
            
            val name = json.get("name")?.asString ?: "Unknown Artist"
            val image = json.get("image")?.asString?.toHighResImage()
            val fans = json.get("fan_count")?.asString
            val description = json.get("wiki")?.asString?.decodeHtml()
            
            // Songs
            val topSongs = json.getAsJsonArray("topSongs")?.mapNotNull { parseSong(it.asJsonObject) } ?: emptyList()
            
            // Albums
            val topAlbums = json.getAsJsonArray("topAlbums")?.mapNotNull { element ->
                val obj = element.asJsonObject
                val albumId = obj.get("id")?.asString ?: return@mapNotNull null
                val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                val albumImage = obj.get("image")?.asString?.toHighResImage()
                val year = obj.get("year")?.asString
                
                com.suvojeet.suvmusic.core.model.Album(albumId, title.decodeHtml(), name.decodeHtml(), albumImage, year)
            } ?: emptyList()
            
            com.suvojeet.suvmusic.core.model.Artist(
                id = artistId,
                name = name.decodeHtml(),
                thumbnailUrl = image,
                description = description,
                subscribers = if (fans != null) "$fans Fans" else null,
                songs = topSongs,
                albums = topAlbums
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get song details by ID.
     */
    suspend fun getSongDetails(songId: String): Song? = withContext(Dispatchers.IO) {
        if (songDetailsCache.containsKey(songId)) {
            return@withContext songDetailsCache[songId]
        }

        try {
            val url = "$BASE_URL?__call=song.getDetails&_format=json&pids=$songId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            val songs = json.getAsJsonObject("songs") ?: json.getAsJsonObject(songId)
            
            val result = if (songs != null && songs.has(songId)) {
                parseSong(songs.getAsJsonObject(songId))
            } else {
                // Try parsing as direct song object
                parseSong(json)
            }
            
            result?.let { songDetailsCache[songId] = it }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get stream URL for a song (320kbps).
     * JioSaavn returns encrypted URLs that need to be decrypted.
     */
    suspend fun getStreamUrl(songId: String, quality: Int = 320): String? = withContext(Dispatchers.IO) {
        val cacheKey = "${songId}_$quality"
        if (streamUrlCache.containsKey(cacheKey)) {
            return@withContext streamUrlCache[cacheKey]
        }

        try {
            val url = "$BASE_URL?__call=song.getDetails&_format=json&pids=$songId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            
            // Find the song in response
            val songJson = if (json.has(songId)) {
                json.getAsJsonObject(songId)
            } else if (json.has("songs")) {
                val songs = json.getAsJsonObject("songs")
                if (songs.has(songId)) songs.getAsJsonObject(songId) else null
            } else {
                json
            }
            
            val streamUrl = songJson?.let { song ->
                val encryptedUrl = song.get("encrypted_media_url")?.asString
                    ?: song.get("more_info")?.asJsonObject?.get("encrypted_media_url")?.asString
                
                encryptedUrl?.let { encrypted ->
                    decryptUrl(encrypted)?.let { decrypted ->
                        // Replace quality suffix based on requested quality
                        when (quality) {
                            320 -> decrypted.replace(QUALITY_96, QUALITY_320).replace(QUALITY_160, QUALITY_320)
                            160 -> decrypted.replace(QUALITY_96, QUALITY_160).replace(QUALITY_320, QUALITY_160)
                            else -> decrypted
                        }
                    }
                }
            }
            
            if (streamUrl == null) {
                android.util.Log.w("JioSaavn", "getStreamUrl($songId): could not resolve a stream URL (no/failed encrypted_media_url)")
            }
            streamUrl?.let { streamUrlCache[cacheKey] = it }
            streamUrl
        } catch (e: Exception) {
            android.util.Log.e("JioSaavn", "getStreamUrl($songId) failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    // ==================== Radio / Related / Recommendations ====================

    private val relatedCache = mutableMapOf<String, List<Song>>()

    /**
     * Get songs related to [songId] using JioSaavn's recommendation endpoint.
     * This powers native autoplay/radio when JioSaavn is the active source.
     */
    suspend fun getRelatedSongs(songId: String): List<Song> = withContext(Dispatchers.IO) {
        relatedCache[songId]?.let { return@withContext it }

        try {
            val url = "$BASE_URL?__call=reco.getreco&_format=json&_marker=0&api_version=4&ctx=android&pid=$songId"
            val response = makeRequest(url)
            val parsed = JsonParser.parseString(response)

            // reco.getreco usually returns a JSON array of song objects, but some
            // variants wrap them in an object — tolerate both shapes.
            val array = when {
                parsed.isJsonArray -> parsed.asJsonArray
                parsed.isJsonObject && parsed.asJsonObject.has("data") -> parsed.asJsonObject.getAsJsonArray("data")
                parsed.isJsonObject && parsed.asJsonObject.has("songs") -> parsed.asJsonObject.getAsJsonArray("songs")
                else -> null
            }

            val songs = array?.mapNotNull { parseSong(it.asJsonObject) } ?: emptyList()
            if (songs.isNotEmpty()) {
                relatedCache[songId] = songs
                songs.forEach { songDetailsCache[it.id] = it }
            } else {
                android.util.Log.w("JioSaavn", "getRelatedSongs($songId) parsed 0 songs")
            }
            songs
        } catch (e: Exception) {
            android.util.Log.e("JioSaavn", "getRelatedSongs($songId) failed: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }

    /**
     * Build a pool of recommended songs seeded from the user's recent JioSaavn plays.
     * Falls back to trending/home content when no seeds are available, so the
     * home screen and queue always have JioSaavn material to work with.
     */
    suspend fun getRecommendations(seedSongIds: List<String>): List<Song> = withContext(Dispatchers.IO) {
        val pool = mutableListOf<Song>()
        try {
            for (seed in seedSongIds.take(5)) {
                pool.addAll(getRelatedSongs(seed))
                if (pool.size >= 40) break
            }
        } catch (e: Exception) {
            android.util.Log.w("JioSaavn", "getRecommendations seed pass failed: ${e.message}")
        }

        // Fallback: pull songs out of the home/discover sections if seeds were dry.
        if (pool.isEmpty()) {
            try {
                getHomeSections().forEach { section ->
                    section.items.forEach { item ->
                        if (item is com.suvojeet.suvmusic.core.model.HomeItem.SongItem) pool.add(item.song)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("JioSaavn", "getRecommendations home fallback failed: ${e.message}")
            }
        }

        pool.distinctBy { it.id }
    }

    /**
     * Get top songs for an artist (used for artist radio on JioSaavn).
     */
    suspend fun getArtistTopSongs(artistId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            getArtist(artistId)?.songs ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("JioSaavn", "getArtistTopSongs($artistId) failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get playlist details with all songs.
     */
    suspend fun getPlaylist(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        if (playlistCache.containsKey(playlistId)) {
            return@withContext playlistCache[playlistId]
        }

        try {
            val url = "$BASE_URL?__call=playlist.getDetails&_format=json&listid=$playlistId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            
            val title = json.get("listname")?.asString ?: json.get("title")?.asString ?: "Playlist"
            val image = json.get("image")?.asString?.toHighResImage()
            val songs = json.getAsJsonArray("songs")?.mapNotNull { 
                parseSong(it.asJsonObject) 
            } ?: emptyList()
            
            val playlist = Playlist(
                id = playlistId,
                title = title,
                author = json.get("firstname")?.asString ?: "",
                thumbnailUrl = image,
                songs = songs
            )
            
            playlistCache[playlistId] = playlist
            playlist
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get album details with all songs.
     */
    suspend fun getAlbum(albumId: String): Playlist? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?__call=content.getAlbumDetails&_format=json&albumid=$albumId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            
            val title = json.get("title")?.asString ?: json.get("name")?.asString ?: "Album"
            val image = json.get("image")?.asString?.toHighResImage()
            val artist = json.get("primary_artists")?.asString ?: ""
            val songs = json.getAsJsonArray("songs")?.mapNotNull { 
                parseSong(it.asJsonObject) 
            } ?: emptyList()
            
            Playlist(
                id = albumId,
                title = title,
                author = artist,
                thumbnailUrl = image,
                songs = songs
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get plain lyrics from JioSaavn (internal fallback).
     */
    /**
     * Get plain lyrics from JioSaavn (internal fallback).
     */
    suspend fun getLyricsFromJioSaavn(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?__call=lyrics.getLyrics&_format=json&lyrics_id=$songId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            json.get("lyrics")?.asString
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get lyrics for a song (legacy function - now returns plain text).
     */
    suspend fun getLyrics(songId: String): String? = getLyricsFromJioSaavn(songId)
    
    /**
     * Get home sections with dynamic content from JioSaavn Launch Data.
     * This provides a "For You" experience with Trending, Charts, New Releases, and customized modules.
     */
    suspend fun getHomeSections(): List<com.suvojeet.suvmusic.core.model.HomeSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<com.suvojeet.suvmusic.core.model.HomeSection>()
        
        try {
            // Use the main launch data endpoint for dynamic homepage structure
            val launchUrl = "$BASE_URL?__call=webapi.getLaunchData&api_version=4&_format=json&_marker=0"
            val response = makeRequest(launchUrl)
            val json = JsonParser.parseString(response).asJsonObject
            
            // Helper to parse a list of items based on their type
            fun parseHomeItems(jsonArray: com.google.gson.JsonArray): List<com.suvojeet.suvmusic.core.model.HomeItem> {
                return jsonArray.mapNotNull { element ->
                    val obj = element.asJsonObject
                    val type = obj.get("type")?.asString ?: ""
                    
                    when (type) {
                        "song" -> {
                            parseSong(obj)?.let { com.suvojeet.suvmusic.core.model.HomeItem.SongItem(it) }
                        }
                        "album" -> {
                            val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                            val image = obj.get("image")?.asString?.toHighResImage()
                            val artist = obj.get("primary_artists")?.asString ?: obj.get("music")?.asString ?: ""
                            val year = obj.get("year")?.asString
                            
                            com.suvojeet.suvmusic.core.model.HomeItem.AlbumItem(
                                com.suvojeet.suvmusic.core.model.Album(id, title.decodeHtml(), artist.decodeHtml(), image, year)
                            )
                        }
                        "playlist" -> {
                            val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                            val image = obj.get("image")?.asString?.toHighResImage()
                            val count = obj.get("song_count")?.asInt ?: obj.get("count")?.asInt ?: 0
                            
                            com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                                com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(id, title.decodeHtml(), "", "JioSaavn", image, count)
                            )
                        }
                        "chart" -> {
                            val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: "Chart"
                            val image = obj.get("image")?.asString?.toHighResImage()
                            val count = obj.get("count")?.asInt ?: 0
                            
                            com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                                com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(id, title.decodeHtml(), "", "JioSaavn Chart", image, count)
                            )
                        }
                        "radio_station" -> {
                            val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: "Radio"
                            val image = obj.get("image")?.asString?.toHighResImage()
                            
                            com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                                com.suvojeet.suvmusic.core.model.PlaylistDisplayItem("radio_$id", title.decodeHtml(), "", "Radio", image, 0)
                            )
                        }
                        "artist" -> {
                             val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                            val image = obj.get("image")?.asString?.toHighResImage()
                             com.suvojeet.suvmusic.core.model.HomeItem.ArtistItem(
                                com.suvojeet.suvmusic.core.model.Artist(id, title.decodeHtml(), image)
                            )
                        }
                        else -> null
                    }
                }
            }

            // 1. New Trending
            if (json.has("new_trending")) {
                val trendingList = json.getAsJsonArray("new_trending")
                val items = parseHomeItems(trendingList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Trending Now 🔥", items))
                }
            }
            
            // 2. Top Charts
            if (json.has("charts")) {
                val chartsList = json.getAsJsonArray("charts")
                val items = parseHomeItems(chartsList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Top Charts 📊", items))
                }
            }
            
            // 3. New Albums
            if (json.has("new_albums")) {
                val albumsList = json.getAsJsonArray("new_albums")
                val items = parseHomeItems(albumsList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("New Releases 🆕", items))
                }
            }
            
            // 4. Browse Discover (Dynamic Sections)
            if (json.has("browse_discover")) {
                val discoverList = json.getAsJsonArray("browse_discover")
                discoverList.forEach { element ->
                    val sectionObj = element.asJsonObject
                    val title = sectionObj.get("title")?.asString
                    val data = sectionObj.getAsJsonArray("data")
                    
                    if (!title.isNullOrBlank() && data != null && data.size() > 0) {
                        val items = parseHomeItems(data)
                        if (items.isNotEmpty()) {
                            sections.add(com.suvojeet.suvmusic.core.model.HomeSection(title.decodeHtml(), items))
                        }
                    }
                }
            }
            
            // 5. Radio
            if (json.has("radio")) {
                val radioList = json.getAsJsonArray("radio")
                val items = parseHomeItems(radioList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Radio Stations 📻", items))
                }
            }
            
            // 6. Top Playlists (Global)
            if (json.has("top_playlists")) {
                val plList = json.getAsJsonArray("top_playlists")
                val items = parseHomeItems(plList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Featured Playlists 🎧", items))
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("JioSaavn", "Error fetching dynamic home sections", e)
        }

        // Always merge the static catalogue on top of whatever dynamic gave us.
        // Launch data is region-dependent and often returns only 1–2 sections,
        // which leaves the home screen feeling empty. Dedupe by title so a
        // section appearing in both sources isn't shown twice.
        try {
            val staticSections = fetchStaticHomeSections()
            if (staticSections.isNotEmpty()) {
                val existingTitles = sections.map { it.title }.toMutableSet()
                for (s in staticSections) {
                    if (existingTitles.add(s.title)) sections.add(s)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("JioSaavn", "Static home merge failed: ${e.message}")
        }

        sections
    }

    /**
     * Fallback: Get home sections with manual fetches if Launch Data fails.
     */
    private suspend fun fetchStaticHomeSections(): List<com.suvojeet.suvmusic.core.model.HomeSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<com.suvojeet.suvmusic.core.model.HomeSection>()
        
        try {
            // 1. Top Charts / Trending - Using content.getCharts
            try {
                val chartsUrl = "$BASE_URL?__call=content.getCharts&_format=json"
                val chartsResponse = makeRequest(chartsUrl)
                val chartsJson = JsonParser.parseString(chartsResponse)
                
                if (chartsJson.isJsonArray && chartsJson.asJsonArray.size() > 0) {
                    val chartItems = chartsJson.asJsonArray.take(12).mapNotNull { element ->
                        val chartObj = element.asJsonObject
                        val chartId = chartObj.get("id")?.asString 
                            ?: chartObj.get("listid")?.asString ?: return@mapNotNull null
                        val title = chartObj.get("title")?.asString 
                            ?: chartObj.get("listname")?.asString ?: "Chart"
                        val image = chartObj.get("image")?.asString?.toHighResImage()
                        val songCount = chartObj.get("count")?.asInt ?: chartObj.get("songs_count")?.asInt ?: 0
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                            com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                                id = chartId,
                                name = title.decodeHtml(),
                                url = "",
                                uploaderName = "JioSaavn Charts",
                                thumbnailUrl = image,
                                songCount = songCount
                            )
                        )
                    }
                    if (chartItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Top Charts 📊", chartItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("JioSaavn", "Charts fetch error", e) }
            
            // 2. New Releases - Using content.getAlbums with filter
            try {
                val newReleasesUrl = "$BASE_URL?__call=content.getAlbums&_format=json&n=20&p=1&type=latest"
                val releaseResponse = makeRequest(newReleasesUrl)
                val releaseJson = JsonParser.parseString(releaseResponse)
                
                val albumsList = if (releaseJson.isJsonObject && releaseJson.asJsonObject.has("data")) {
                    releaseJson.asJsonObject.getAsJsonArray("data")
                } else if (releaseJson.isJsonArray) {
                    releaseJson.asJsonArray
                } else {
                    null
                }
                
                if (albumsList != null && albumsList.size() > 0) {
                    val albumItems = albumsList.take(12).mapNotNull { albumElement ->
                        val albumObj = albumElement.asJsonObject
                        val albumId = albumObj.get("id")?.asString 
                            ?: albumObj.get("albumid")?.asString ?: return@mapNotNull null
                        val title = albumObj.get("title")?.asString 
                            ?: albumObj.get("name")?.asString ?: "Album"
                        val artist = albumObj.get("primary_artists")?.asString 
                            ?: albumObj.get("music")?.asString ?: ""
                        val image = albumObj.get("image")?.asString?.toHighResImage()
                        val year = albumObj.get("year")?.asString
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.AlbumItem(
                            com.suvojeet.suvmusic.core.model.Album(
                                id = albumId,
                                title = title.decodeHtml(),
                                artist = artist.decodeHtml(),
                                thumbnailUrl = image,
                                year = year
                            )
                        )
                    }
                    if (albumItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("New Releases 🆕", albumItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("JioSaavn", "New releases fetch error", e) }
            
            // 3. Trending Songs - Using content.getTrending
            try {
                val trendingUrl = "$BASE_URL?__call=content.getTrending&type=song&_format=json&n=20"
                val trendingResponse = makeRequest(trendingUrl)
                val trendingJson = JsonParser.parseString(trendingResponse)
                
                val songsList = if (trendingJson.isJsonObject && trendingJson.asJsonObject.has("data")) {
                    trendingJson.asJsonObject.getAsJsonArray("data")
                } else if (trendingJson.isJsonArray) {
                    trendingJson.asJsonArray
                } else {
                    null
                }
                
                if (songsList != null && songsList.size() > 0) {
                    val songItems = songsList.take(12).mapNotNull { songElement ->
                        val song = parseSong(songElement.asJsonObject) ?: return@mapNotNull null
                        com.suvojeet.suvmusic.core.model.HomeItem.SongItem(song)
                    }
                    if (songItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Trending Now 🔥", songItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("JioSaavn", "Trending songs fetch error", e) }
            
            // 4. Top Playlists - Using content.getFeaturedPlaylists
            try {
                val playlistsUrl = "$BASE_URL?__call=content.getFeaturedPlaylists&_format=json&n=20&p=1"
                val playlistsResponse = makeRequest(playlistsUrl)
                val playlistsJson = JsonParser.parseString(playlistsResponse)
                
                val plList = if (playlistsJson.isJsonObject && playlistsJson.asJsonObject.has("data")) {
                    playlistsJson.asJsonObject.getAsJsonArray("data")
                } else if (playlistsJson.isJsonObject && playlistsJson.asJsonObject.has("featured_playlists")) {
                    playlistsJson.asJsonObject.getAsJsonArray("featured_playlists")
                } else if (playlistsJson.isJsonArray) {
                    playlistsJson.asJsonArray
                } else {
                    null
                }
                
                if (plList != null && plList.size() > 0) {
                    val playlistItems = plList.take(12).mapNotNull { plElement ->
                        val plObj = plElement.asJsonObject
                        val plId = plObj.get("id")?.asString 
                            ?: plObj.get("listid")?.asString ?: return@mapNotNull null
                        val name = plObj.get("title")?.asString 
                            ?: plObj.get("listname")?.asString ?: "Playlist"
                        val image = plObj.get("image")?.asString?.toHighResImage()
                        val songCount = plObj.get("count")?.asInt ?: plObj.get("songs_count")?.asInt ?: 0
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                            com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                                id = plId,
                                name = name.decodeHtml(),
                                url = "",
                                uploaderName = "JioSaavn",
                                thumbnailUrl = image,
                                songCount = songCount
                            )
                        )
                    }
                    if (playlistItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Featured Playlists 🎧", playlistItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("JioSaavn", "Featured playlists fetch error", e) }
            
            // 5. Top Artists - Using content.getArtists (trending artists)
            try {
                val artistsUrl = "$BASE_URL?__call=search.getArtistResults&_format=json&q=top+indian+artists&n=15"
                val artistsResponse = makeRequest(artistsUrl)
                val artistsJson = JsonParser.parseString(artistsResponse)
                
                val artistResults = if (artistsJson.isJsonObject && artistsJson.asJsonObject.has("results")) {
                    artistsJson.asJsonObject.getAsJsonArray("results")
                } else {
                    null
                }
                
                if (artistResults != null && artistResults.size() > 0) {
                    val artistItems = artistResults.take(10).mapNotNull { artistElement ->
                        val artistObj = artistElement.asJsonObject
                        val artistId = artistObj.get("id")?.asString ?: return@mapNotNull null
                        val name = artistObj.get("name")?.asString 
                            ?: artistObj.get("title")?.asString ?: "Artist"
                        val image = artistObj.get("image")?.asString?.toHighResImage()
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.ArtistItem(
                            com.suvojeet.suvmusic.core.model.Artist(
                                id = artistId,
                                name = name.decodeHtml(),
                                thumbnailUrl = image
                            )
                        )
                    }
                    if (artistItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Top Artists 🎤", artistItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("JioSaavn", "Artists fetch error", e) }
            
            // 6. Trending Albums - Using content.getTrending with type=album
            try {
                val trendingAlbumsUrl = "$BASE_URL?__call=content.getTrending&type=album&_format=json&n=15"
                val albumsResponse = makeRequest(trendingAlbumsUrl)
                val albumsJson = JsonParser.parseString(albumsResponse)
                
                val albumArray = if (albumsJson.isJsonObject && albumsJson.asJsonObject.has("data")) {
                    albumsJson.asJsonObject.getAsJsonArray("data")
                } else if (albumsJson.isJsonArray) {
                    albumsJson.asJsonArray
                } else {
                    null
                }
                
                if (albumArray != null && albumArray.size() > 0) {
                    val albumItems = albumArray.take(10).mapNotNull { albumElement ->
                        val albumObj = albumElement.asJsonObject
                        val albumId = albumObj.get("id")?.asString 
                            ?: albumObj.get("albumid")?.asString ?: return@mapNotNull null
                        val title = albumObj.get("title")?.asString 
                            ?: albumObj.get("name")?.asString ?: "Album"
                        val artist = albumObj.get("primary_artists")?.asString 
                            ?: albumObj.get("music")?.asString ?: ""
                        val image = albumObj.get("image")?.asString?.toHighResImage()
                        val year = albumObj.get("year")?.asString
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.AlbumItem(
                            com.suvojeet.suvmusic.core.model.Album(
                                id = albumId,
                                title = title.decodeHtml(),
                                artist = artist.decodeHtml(),
                                thumbnailUrl = image,
                                year = year
                            )
                        )
                    }
                    if (albumItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Popular Albums 💿", albumItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("JioSaavn", "Trending albums fetch error", e) }
            
            // 7. Editorial Picks / Radio Stations - Using content.getRadioStations
            try {
                val radioUrl = "$BASE_URL?__call=webradio.getFeaturedStations&_format=json&n=15"
                val radioResponse = makeRequest(radioUrl)
                val radioJson = JsonParser.parseString(radioResponse)
                
                val radioList = if (radioJson.isJsonObject && radioJson.asJsonObject.has("data")) {
                    radioJson.asJsonObject.getAsJsonArray("data")
                } else if (radioJson.isJsonArray) {
                    radioJson.asJsonArray
                } else {
                    null
                }
                
                if (radioList != null && radioList.size() > 0) {
                    val radioItems = radioList.take(8).mapNotNull { radioElement ->
                        val radioObj = radioElement.asJsonObject
                        val radioId = (radioObj.get("id")?.asString 
                            ?: radioObj.get("stationid")?.asString)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val name = radioObj.get("name")?.asString 
                            ?: radioObj.get("title")?.asString ?: "Radio"
                        val image = radioObj.get("image")?.asString?.toHighResImage()
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                            com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                                id = "radio_$radioId",
                                name = name.decodeHtml(),
                                url = "",
                                uploaderName = "JioSaavn Radio",
                                thumbnailUrl = image,
                                songCount = 0
                            )
                        )
                    }
                    if (radioItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Radio Stations 📻", radioItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("JioSaavn", "Radio stations fetch error", e) }
            
        } catch (e: Exception) {
            android.util.Log.e("JioSaavn", "Home sections error", e)
        }
        
        sections
    }
    
    /**
     * Get featured/trending playlists from JioSaavn for the library.
     */
    suspend fun getFeaturedPlaylists(): List<com.suvojeet.suvmusic.core.model.PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<com.suvojeet.suvmusic.core.model.PlaylistDisplayItem>()
        
        try {
            // Fetch top charts/featured playlists
            val playlistsUrl = "$BASE_URL?__call=content.getCharts&_format=json&n=20"
            val response = makeRequest(playlistsUrl)
            
            // Note: getCharts returns an array directly or inside "results" depending on endpoint version
            // But usually content.getCharts returns a list of playlists
            val json = JsonParser.parseString(response)
            
            val results = if (json.isJsonArray) {
                json.asJsonArray
            } else if (json.isJsonObject && json.asJsonObject.has("results")) {
                json.asJsonObject.getAsJsonArray("results")
            } else {
                null
            }
            
            results?.forEach { element ->
                val plObj = element.asJsonObject
                val plId = plObj.get("id")?.asString ?: plObj.get("listid")?.asString
                val name = plObj.get("title")?.asString ?: plObj.get("listname")?.asString ?: "Playlist"
                val image = plObj.get("image")?.asString?.toHighResImage()
                val songCount = plObj.get("count")?.asInt ?: 0
                
                if (plId != null) {
                    playlists.add(
                        com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                            id = plId,
                            name = name.decodeHtml(),
                            url = "",
                            uploaderName = "JioSaavn",
                            thumbnailUrl = image,
                            songCount = songCount
                        )
                    )
                }
            }
            
            // Also add some specific search results if charts are empty or few
            if (playlists.size < 5) {
                val searchUrl = "$BASE_URL?__call=search.getPlaylistResults&_format=json&q=top+hits&n=10"
                val searchResponse = makeRequest(searchUrl)
                val searchJson = JsonParser.parseString(searchResponse).asJsonObject
                val searchResults = searchJson.getAsJsonArray("results")
                
                searchResults?.forEach { element ->
                    val plObj = element.asJsonObject
                    val plId = plObj.get("id")?.asString ?: return@forEach
                    // Avoid duplicates
                    if (playlists.none { it.id == plId }) {
                        val name = plObj.get("title")?.asString ?: "Playlist"
                        val image = plObj.get("image")?.asString?.toHighResImage()
                        val songCount = plObj.get("count")?.asInt ?: 0
                        
                        playlists.add(
                            com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                                id = plId,
                                name = name.decodeHtml(),
                                url = "",
                                uploaderName = "JioSaavn",
                                thumbnailUrl = image,
                                songCount = songCount
                            )
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        playlists
    }
    
    // ==================== Private Helpers ====================
    
    private fun makeRequest(url: String): String {
        // Derive referer dynamically
        val referer = BASE_URL.substringBefore("/api")  + "/"
        val origin = referer.dropLast(1)

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36")
            .addHeader("Referer", referer)
            .addHeader("Origin", origin)
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                android.util.Log.e("JioSaavn", "HTTP ${response.code} for ${request.url.host}${request.url.encodedPath} (body ${body.length} chars)")
            } else {
                android.util.Log.d("JioSaavn", "HTTP ${response.code} ${request.url.host} — ${body.length} chars")
            }
            body
        }
    }
    
    private fun parseSong(json: JsonObject): Song? {
        return try {
            val id = (json.get("id")?.asString 
                ?: json.get("perma_url")?.asString?.substringAfterLast("/"))
                ?.takeIf { it.isNotBlank() }
                ?: return null
            
            // Check for more_info object
            val moreInfo = if (json.has("more_info") && json.get("more_info").isJsonObject) {
                json.getAsJsonObject("more_info")
            } else {
                null
            }

            val title = json.get("song")?.asString 
                ?: json.get("title")?.asString 
                ?: json.get("name")?.asString 
                ?: "Unknown"
            
            val artistsJson = moreInfo?.get("primary_artists")?.asString
                ?: moreInfo?.get("singers")?.asString
                ?: moreInfo?.get("music")?.asString
                ?: json.get("primary_artists")?.asString 
                ?: json.get("singers")?.asString 
                ?: json.get("music")?.asString 
                ?: "Unknown Artist"
            
            val album = moreInfo?.get("album")?.asString
                ?: json.get("album")?.asString ?: ""
            
            val durationStr = moreInfo?.get("duration")?.asString
                ?: json.get("duration")?.asString ?: "0"
            val duration = (durationStr.toLongOrNull() ?: 0L) * 1000 // Convert to milliseconds
            
            val image = json.get("image")?.asString?.toHighResImage()

            val releaseDate = json.get("release_date")?.asString
                ?: moreInfo?.get("release_date")?.asString
                ?: json.get("year")?.asString
                ?: moreInfo?.get("year")?.asString

            Song.fromJioSaavn(
                songId = id,
                title = title.decodeHtml(),
                artist = artistsJson.decodeHtml(),
                album = album.decodeHtml(),
                duration = duration,
                thumbnailUrl = image,
                releaseDate = releaseDate
            )
            } catch (e: Exception) {            e.printStackTrace()
            null
        }
    }
    
    /**
     * Decrypt JioSaavn's encrypted media URL.
     *
     * Note: the algorithm (DES-ECB) is dictated by JioSaavn's server format and
     * is not something we control. The defensive posture here is to fail hard
     * on decrypt errors and return null, rather than silently returning the
     * encrypted blob which the caller would then attempt to use as a URL.
     */
    private fun decryptUrl(encryptedUrl: String): String? {
        return try {
            val keySpec = DESKeySpec(DES_KEY.toByteArray())
            val keyFactory = SecretKeyFactory.getInstance("DES")
            val secretKey = keyFactory.generateSecret(keySpec)

            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decoded = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)

            String(decrypted)
        } catch (e: Exception) {
            android.util.Log.w("JioSaavnRepository", "decryptUrl failed: ${e.javaClass.simpleName}")
            null
        }
    }
    
    // Extension functions moved to util/Extensions.kt
    
    /**
     * Comprehensive search for songs, albums, artists, and playlists.
     * Uses autocomplete endpoint for best "instant search" results.
     */
    suspend fun searchAll(query: String): SearchResults = withContext(Dispatchers.IO) {
        // Helper: safely pull the "data" array out of a category object, tolerating
        // shape differences (missing key, object instead of array, etc.).
        fun categoryData(json: JsonObject, key: String): com.google.gson.JsonArray? {
            return try {
                json.getAsJsonObject(key)?.getAsJsonArray("data")
                    ?: json.getAsJsonArray(key)
            } catch (e: Exception) {
                null
            }
        }

        try {
            val url = "$BASE_URL?__call=autocomplete.get&_format=json&cc=in&includeMetaTags=1&query=${query.encodeUrl()}"
            val response = makeRequest(url)
            val json = JsonParser.parseString(response).asJsonObject

            // Parse Songs — fall back to the canonical search.getResults endpoint
            // if autocomplete returns no songs (its shape changes occasionally).
            var songs = categoryData(json, "songs")?.mapNotNull { parseSong(it.asJsonObject) } ?: emptyList()
            if (songs.isEmpty()) {
                android.util.Log.w("JioSaavn", "searchAll('$query'): autocomplete returned no songs, falling back to search.getResults")
                songs = search(query)
            }

            // Parse Albums
            val albums = if (json.has("albums")) {
                (categoryData(json, "albums") ?: com.google.gson.JsonArray()).mapNotNull { element ->
                    val obj = element.asJsonObject
                    val id = obj.get("id")?.asString ?: return@mapNotNull null
                    val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                    val image = obj.get("image")?.asString?.toHighResImage()
                    val artist = obj.get("music")?.asString ?: "" // 'music' key usually holds artist in autocomplete
                    val year = obj.get("year")?.asString
                    
                    com.suvojeet.suvmusic.core.model.Album(id, title.decodeHtml(), artist.decodeHtml(), image, year)
                }
            } else emptyList()
            
            // Parse Artists
            val artists = if (json.has("artists")) {
                (categoryData(json, "artists") ?: com.google.gson.JsonArray()).mapNotNull { element ->
                    val obj = element.asJsonObject
                    val id = obj.get("id")?.asString ?: return@mapNotNull null
                    val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                    val image = obj.get("image")?.asString?.toHighResImage()

                    com.suvojeet.suvmusic.core.model.Artist(id, title.decodeHtml(), image)
                }
            } else emptyList()

            // Parse Playlists
            val playlists = if (json.has("playlists")) {
                (categoryData(json, "playlists") ?: com.google.gson.JsonArray()).mapNotNull { element ->
                    val obj = element.asJsonObject
                    val id = obj.get("id")?.asString ?: return@mapNotNull null
                    val title = obj.get("title")?.asString ?: ""
                    val image = obj.get("image")?.asString?.toHighResImage()
                    
                    // Create lightweight Playlist object
                    Playlist(
                        id = id,
                        title = title.decodeHtml(),
                        author = "JioSaavn",
                        thumbnailUrl = image,
                        songs = emptyList()
                    )
                }
            } else emptyList()
            
            return@withContext SearchResults(songs, albums, artists, playlists)

        } catch (e: Exception) {
            android.util.Log.e("JioSaavn", "searchAll('$query') failed: ${e.javaClass.simpleName}: ${e.message} — falling back to search.getResults")
            // Even if autocomplete fails entirely, still try to return songs so the tab isn't empty.
            return@withContext SearchResults(songs = search(query))
        }
    }
}

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<com.suvojeet.suvmusic.core.model.Album> = emptyList(),
    val artists: List<com.suvojeet.suvmusic.core.model.Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

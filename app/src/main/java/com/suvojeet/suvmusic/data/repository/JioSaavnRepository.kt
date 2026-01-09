package com.suvojeet.suvmusic.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.suvojeet.suvmusic.data.model.Playlist
import com.suvojeet.suvmusic.data.model.Song
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

/**
 * Repository for fetching music from JioSaavn.
 * Uses JioSaavn's internal API endpoints (unofficial).
 * Supports 320kbps high-quality audio.
 */
@Singleton
class JioSaavnRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val BASE_URL = "https://www.jiosaavn.com/api.php"
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
        try {
            val url = "$BASE_URL?__call=search.getResults&_format=json&_marker=0&n=20&q=${query.encodeUrl()}"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            val results = json.getAsJsonArray("results") ?: return@withContext emptyList()
            
            results.mapNotNull { element ->
                val song = element.asJsonObject
                parseSong(song)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Search for artists on JioSaavn.
     */
    suspend fun searchArtists(query: String): List<com.suvojeet.suvmusic.data.model.Artist> = withContext(Dispatchers.IO) {
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
                
                com.suvojeet.suvmusic.data.model.Artist(
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
     * Get song details by ID.
     */
    suspend fun getSongDetails(songId: String): Song? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?__call=song.getDetails&_format=json&pids=$songId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            val songs = json.getAsJsonObject("songs") ?: json.getAsJsonObject(songId)
            
            if (songs != null && songs.has(songId)) {
                parseSong(songs.getAsJsonObject(songId))
            } else {
                // Try parsing as direct song object
                parseSong(json)
            }
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
            
            songJson?.let { song ->
                val encryptedUrl = song.get("encrypted_media_url")?.asString
                    ?: song.get("more_info")?.asJsonObject?.get("encrypted_media_url")?.asString
                
                encryptedUrl?.let { encrypted ->
                    val decrypted = decryptUrl(encrypted)
                    // Replace quality suffix based on requested quality
                    when (quality) {
                        320 -> decrypted.replace(QUALITY_96, QUALITY_320).replace(QUALITY_160, QUALITY_320)
                        160 -> decrypted.replace(QUALITY_96, QUALITY_160).replace(QUALITY_320, QUALITY_160)
                        else -> decrypted
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get playlist details with all songs.
     */
    suspend fun getPlaylist(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?__call=playlist.getDetails&_format=json&listid=$playlistId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            
            val title = json.get("listname")?.asString ?: json.get("title")?.asString ?: "Playlist"
            val image = json.get("image")?.asString?.toHighResImage()
            val songs = json.getAsJsonArray("songs")?.mapNotNull { 
                parseSong(it.asJsonObject) 
            } ?: emptyList()
            
            Playlist(
                id = playlistId,
                title = title,
                author = json.get("firstname")?.asString ?: "",
                thumbnailUrl = image,
                songs = songs
            )
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
     * Get lyrics for a song.
     */
    suspend fun getLyrics(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?__call=lyrics.getLyrics&_format=json&lyrics_id=$songId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            json.get("lyrics")?.asString
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get home sections with trending content from JioSaavn.
     * Returns sections similar to YouTube Music home page.
     * Includes songs, albums, and featured playlists.
     */
    suspend fun getHomeSections(): List<com.suvojeet.suvmusic.data.model.HomeSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<com.suvojeet.suvmusic.data.model.HomeSection>()
        
        try {
            // 1. Trending Now - Top hits 2026
            val trendingSongs = mutableListOf<Song>()
            listOf("trending 2026", "new hindi songs 2026", "latest bollywood").forEach { query ->
                try {
                    val results = search(query)
                    if (results.isNotEmpty()) {
                        trendingSongs.addAll(results.take(6))
                    }
                } catch (e: Exception) { }
            }
            
            if (trendingSongs.isNotEmpty()) {
                val items = trendingSongs.distinctBy { it.id }.take(12).map { song ->
                    com.suvojeet.suvmusic.data.model.HomeItem.SongItem(song)
                }
                sections.add(com.suvojeet.suvmusic.data.model.HomeSection("Trending Now 🔥", items))
            }
            
            // 2. New Albums - Fetch latest albums via search
            try {
                val albumsUrl = "$BASE_URL?__call=search.getAlbumResults&_format=json&q=new+album+2026&n=15"
                val albumsResponse = makeRequest(albumsUrl)
                val albumsJson = JsonParser.parseString(albumsResponse)
                
                if (albumsJson.isJsonObject) {
                    val results = albumsJson.asJsonObject.getAsJsonArray("results")
                    if (results != null && results.size() > 0) {
                        val albumItems = results.take(10).mapNotNull { albumElement ->
                            val albumObj = albumElement.asJsonObject
                            val albumId = albumObj.get("id")?.asString ?: return@mapNotNull null
                            val title = albumObj.get("title")?.asString ?: "Album"
                            val artist = albumObj.get("music")?.asString 
                                ?: albumObj.get("primary_artists")?.asString ?: ""
                            val image = albumObj.get("image")?.asString?.toHighResImage()
                            val year = albumObj.get("year")?.asString
                            
                            com.suvojeet.suvmusic.data.model.HomeItem.AlbumItem(
                                com.suvojeet.suvmusic.data.model.Album(
                                    id = albumId,
                                    title = title.decodeHtml(),
                                    artist = artist.decodeHtml(),
                                    thumbnailUrl = image,
                                    year = year
                                )
                            )
                        }
                        
                        if (albumItems.isNotEmpty()) {
                            sections.add(com.suvojeet.suvmusic.data.model.HomeSection("New Albums 💿", albumItems))
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            // 3. Featured Playlists
            try {
                val playlistsUrl = "$BASE_URL?__call=search.getPlaylistResults&_format=json&q=top+hits&n=15"
                val playlistsResponse = makeRequest(playlistsUrl)
                val playlistsJson = JsonParser.parseString(playlistsResponse)
                
                if (playlistsJson.isJsonObject) {
                    val results = playlistsJson.asJsonObject.getAsJsonArray("results")
                    if (results != null && results.size() > 0) {
                        val playlistItems = results.take(10).mapNotNull { plElement ->
                            val plObj = plElement.asJsonObject
                            val plId = plObj.get("id")?.asString ?: return@mapNotNull null
                            val name = plObj.get("title")?.asString ?: "Playlist"
                            val image = plObj.get("image")?.asString?.toHighResImage()
                            val songCount = plObj.get("count")?.asInt ?: 0
                            
                            com.suvojeet.suvmusic.data.model.HomeItem.PlaylistItem(
                                com.suvojeet.suvmusic.data.model.PlaylistDisplayItem(
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
                            sections.add(com.suvojeet.suvmusic.data.model.HomeSection("Featured Playlists 🎧", playlistItems))
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            // 4. Category-based song sections
            val categories = listOf(
                "Bollywood Hits" to "bollywood hits 2026",
                "Romantic" to "romantic love songs hindi",
                "Punjabi Beats" to "punjabi songs 2026",
                "Arijit Singh" to "arijit singh latest",
                "Party Mix" to "party dance songs",
                "90s Retro" to "90s hindi evergreen"
            )
            
            for ((sectionName, searchQuery) in categories) {
                if (sections.size >= 8) break // Limit total sections
                
                try {
                    val results = search(searchQuery)
                    if (results.isNotEmpty()) {
                        val items = results.take(10).map { song ->
                            com.suvojeet.suvmusic.data.model.HomeItem.SongItem(song)
                        }
                        sections.add(com.suvojeet.suvmusic.data.model.HomeSection(sectionName, items))
                    }
                } catch (e: Exception) { }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        sections
    }
    
    // ==================== Private Helpers ====================
    
    private fun makeRequest(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        
        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }
    
    private fun parseSong(json: JsonObject): Song? {
        return try {
            val id = json.get("id")?.asString 
                ?: json.get("perma_url")?.asString?.substringAfterLast("/")
                ?: return null
            
            val title = json.get("song")?.asString 
                ?: json.get("title")?.asString 
                ?: json.get("name")?.asString 
                ?: "Unknown"
            
            val artistsJson = json.get("primary_artists")?.asString 
                ?: json.get("singers")?.asString 
                ?: json.get("music")?.asString 
                ?: "Unknown Artist"
            
            val album = json.get("album")?.asString ?: ""
            
            val durationStr = json.get("duration")?.asString ?: "0"
            val duration = (durationStr.toLongOrNull() ?: 0L) * 1000 // Convert to milliseconds
            
            val image = json.get("image")?.asString?.toHighResImage()
            
            Song.fromJioSaavn(
                songId = id,
                title = title.decodeHtml(),
                artist = artistsJson.decodeHtml(),
                album = album.decodeHtml(),
                duration = duration,
                thumbnailUrl = image
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Decrypt JioSaavn's encrypted media URL using DES.
     */
    private fun decryptUrl(encryptedUrl: String): String {
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
            e.printStackTrace()
            encryptedUrl
        }
    }
    
    // Extension functions
    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
    
    private fun String.toHighResImage(): String {
        // JioSaavn images: replace resolution suffix for higher quality
        return this.replace("150x150", "500x500")
            .replace("50x50", "500x500")
    }
    
    private fun String.decodeHtml(): String {
        return this.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
    }
}

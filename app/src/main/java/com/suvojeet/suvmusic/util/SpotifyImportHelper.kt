package com.suvojeet.suvmusic.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyImportHelper @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    /**
     * Data class for Spotify Track info.
     */
    data class SpotifyTrack(
        val title: String,
        val artist: String,
        val durationMs: Long = 0
    )

    /**
     * Extracts song titles, artists, and durations from a Spotify URL (Playlist, Album, or Artist).
     * Returns a Pair of (Name, List of SpotifyTrack).
     */
    suspend fun getPlaylistSongs(url: String): Pair<String, List<SpotifyTrack>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<SpotifyTrack>()
        var playlistName = "Spotify Import ${System.currentTimeMillis() / 1000}"

        try {
            // Resolve shortened links
            val finalUrl = if (url.contains("spotify.link") || (url.contains("open.spotify.com") && !url.contains("/playlist/") && !url.contains("/album/") && !url.contains("/artist/"))) {
                resolveShortenedUrl(url) ?: url
            } else url

            val (type, id) = extractTypeAndId(finalUrl)
            
            if (id != null && type != null) {
                val accessToken = getSpotifyAccessToken(finalUrl)
                if (accessToken != null) {
                    try {
                        val (name, apiTracks) = fetchSpotifyTracksWithApi(id, type, accessToken)
                        if (name.isNotEmpty()) playlistName = name
                        if (apiTracks.isNotEmpty()) {
                            Log.i("SpotifyImportHelper", "API method fetched ${apiTracks.size} tracks from $type")
                            return@withContext playlistName to apiTracks
                        }
                    } catch (e: Exception) {
                        Log.w("SpotifyImportHelper", "Spotify API method failed for $type", e)
                    }
                }
            }

            // Fallback to embed (limited info) for playlists
            if (type == "playlist" && id != null) {
                try {
                    val embedUrl = "https://open.spotify.com/embed/playlist/$id"
                    val doc = Jsoup.connect(embedUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .get()

                    val nextDataScript = doc.select("script#__NEXT_DATA__").first()
                    if (nextDataScript != null) {
                        val json = nextDataScript.html()
                        val jsonObject = gson.fromJson(json, JsonObject::class.java)

                        val entity = jsonObject.getAsJsonObject("props")
                            ?.getAsJsonObject("pageProps")
                            ?.getAsJsonObject("state")
                            ?.getAsJsonObject("data")
                            ?.getAsJsonObject("entity")

                        if (entity != null) {
                            if (entity.has("name")) {
                                playlistName = entity.get("name").asString
                            } else if (entity.has("title")) {
                                playlistName = entity.get("title").asString
                            }

                            val trackList = entity.getAsJsonArray("trackList")
                            if (trackList != null) {
                                for (element in trackList) {
                                    val trackObj = element.asJsonObject
                                    val title = trackObj.get("title").asString
                                    val subtitle = trackObj.get("subtitle").asString
                                    val duration = trackObj.get("duration")?.asLong ?: 0L
                                    if (title.isNotBlank()) {
                                        tracks.add(SpotifyTrack(title, subtitle, duration))
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SpotifyImportHelper", "Spotify embed fallback failed", e)
                }
            }

            // Final fallback: Scrape original URL (minimal info)
            if (tracks.isEmpty()) {
                val doc = Jsoup.connect(finalUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get()
                
                val docTitle = doc.title()
                if (docTitle.contains("Spotify")) {
                    playlistName = docTitle.split("- Playlist by").firstOrNull()?.trim() 
                        ?: docTitle.split("|").firstOrNull()?.trim() 
                        ?: docTitle
                }

                val trackNames = doc.select("span.track-name, div.track-name, .track-name")
                val artistNames = doc.select("span.artist-name, div.artist-name, .artist-name")

                if (trackNames.isNotEmpty() && trackNames.size == artistNames.size) {
                    for (i in trackNames.indices) {
                        tracks.add(SpotifyTrack(trackNames[i].text(), artistNames[i].text()))
                    }
                }
            }

        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Spotify operation failed", e)
        }
        playlistName to tracks
    }

    private suspend fun resolveShortenedUrl(shortUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(shortUrl)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                val finalUrl = response.request.url.toString()
                response.close()
                Log.i("SpotifyImportHelper", "Resolved $shortUrl to $finalUrl")
                finalUrl
            } catch (e: Exception) {
                Log.w("SpotifyImportHelper", "Failed to resolve short URL: $shortUrl", e)
                null
            }
        }
    }

    private fun extractTypeAndId(url: String): Pair<String?, String?> {
        val types = listOf("playlist", "album", "artist", "track")
        for (type in types) {
            val pattern = "$type/([a-zA-Z0-9]+)".toRegex()
            val match = pattern.find(url)
            if (match != null) return type to match.groupValues[1]
        }
        return null to null
    }

    private fun extractPlaylistId(url: String): String? {
        return extractTypeAndId(url).second
    }

    private suspend fun getSpotifyAccessToken(url: String): String? {
        try {
            val token = fetchAnonymousSpotifyToken()
            if (token != null) return token
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Anonymous token fetch failed", e)
        }

        try {
            val token = extractAccessTokenFromPage(url)
            if (token != null) return token
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Page token extraction failed", e)
        }

        val playlistId = extractPlaylistId(url)
        if (playlistId != null) {
            try {
                val token = extractAccessTokenFromEmbed(playlistId)
                if (token != null) return token
            } catch (e: Exception) {
                Log.w("SpotifyImportHelper", "Embed token extraction failed", e)
            }
        }
        return null
    }

    private fun fetchAnonymousSpotifyToken(): String? {
        val request = Request.Builder()
            .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Accept", "application/json")
            .addHeader("Referer", "https://open.spotify.com/")
            .addHeader("Origin", "https://open.spotify.com")
            .build()

        val response = okHttpClient.newCall(request).execute()
        return try {
            if (response.isSuccessful) {
                val json = response.body.string()
                val jsonObj = gson.fromJson(json, JsonObject::class.java)
                if (jsonObj.has("accessToken")) jsonObj.get("accessToken").asString else null
            } else null
        } finally {
            response.close()
        }
    }

    private fun extractAccessTokenFromPage(url: String): String? {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()

        val nextDataScript = doc.select("script#__NEXT_DATA__").first() ?: return null
        val json = nextDataScript.html()
        val jsonObject = gson.fromJson(json, JsonObject::class.java)
        return findAccessTokenRecursive(jsonObject, 0)
    }

    private fun extractAccessTokenFromEmbed(playlistId: String): String? {
        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
        val doc = Jsoup.connect(embedUrl)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()

        val nextDataScript = doc.select("script#__NEXT_DATA__").first() ?: return null
        val json = nextDataScript.html()
        val jsonObject = gson.fromJson(json, JsonObject::class.java)
        return findAccessTokenRecursive(jsonObject, 0)
    }

    private fun findAccessTokenRecursive(jsonObject: JsonObject, depth: Int): String? {
        if (depth > 8) return null
        if (jsonObject.has("accessToken")) {
            val token = jsonObject.get("accessToken")
            if (token.isJsonPrimitive && token.asString.isNotBlank()) return token.asString
        }
        for ((_, value) in jsonObject.entrySet()) {
            if (value.isJsonObject) {
                val found = findAccessTokenRecursive(value.asJsonObject, depth + 1)
                if (found != null) return found
            }
        }
        return null
    }

    private suspend fun fetchSpotifyTracksWithApi(id: String, type: String, accessToken: String): Pair<String, List<SpotifyTrack>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<SpotifyTrack>()
        var name = ""
        
        // Construct URL based on type
        val baseUrl = when (type) {
            "playlist" -> "https://api.spotify.com/v1/playlists/$id"
            "album" -> "https://api.spotify.com/v1/albums/$id"
            "artist" -> "https://api.spotify.com/v1/artists/$id/top-tracks?market=from_token"
            "track" -> "https://api.spotify.com/v1/tracks/$id"
            else -> return@withContext "" to emptyList<SpotifyTrack>()
        }

        try {
            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body.string()
                val jsonObj = gson.fromJson(json, JsonObject::class.java)
                name = jsonObj.get("name")?.asString ?: ""
                
                if (type == "track") {
                    val title = jsonObj.get("name").asString
                    val duration = jsonObj.get("duration_ms").asLong
                    val artists = jsonObj.getAsJsonArray("artists").map { it.asJsonObject.get("name").asString }.joinToString(", ")
                    return@withContext name to listOf(SpotifyTrack(title, artists, duration))
                }
                
                if (type == "artist") {
                    val tracksArray = jsonObj.getAsJsonArray("tracks")
                    tracksArray?.forEach { 
                        val t = it.asJsonObject
                        val title = t.get("name").asString
                        val duration = t.get("duration_ms").asLong
                        val artists = t.getAsJsonArray("artists").map { a -> a.asJsonObject.get("name").asString }.joinToString(", ")
                        tracks.add(SpotifyTrack(title, artists, duration))
                    }
                    return@withContext "$name - Top Tracks" to tracks
                }
            }
            response.close()
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Failed to fetch $type details", e)
        }

        var nextUrl: String? = when (type) {
            "playlist" -> "https://api.spotify.com/v1/playlists/$id/tracks?offset=0&limit=100&fields=items(track(name,artists(name),duration_ms)),next"
            "album" -> "https://api.spotify.com/v1/albums/$id/tracks?offset=0&limit=50"
            else -> null
        }

        var pageCount = 0
        var consecutiveErrors = 0
        val maxSongs = 3000

        while (nextUrl != null && tracks.size < maxSongs) {
            val currentNextUrl: String = nextUrl!!
            try {
                val request = Request.Builder()
                    .url(currentNextUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    if (code == 429 && consecutiveErrors < 3) {
                        consecutiveErrors++
                        delay(consecutiveErrors * 2000L)
                        continue
                    }
                    break
                }
                
                consecutiveErrors = 0
                val json = response.body.string()
                val jsonObj = gson.fromJson(json, JsonObject::class.java)
                val items = jsonObj.getAsJsonArray("items")
                
                if (items != null) {
                    for (item in items) {
                        try {
                            val trackObj = if (type == "playlist") {
                                item.asJsonObject.getAsJsonObject("track")
                            } else {
                                item.asJsonObject
                            } ?: continue
                            
                            if (trackObj.get("name")?.isJsonNull != false) continue
                            
                            val title = trackObj.get("name").asString
                            val duration = trackObj.get("duration_ms")?.asLong ?: 0L
                            val artists = trackObj.getAsJsonArray("artists")?.mapNotNull { 
                                it.asJsonObject.get("name")?.asString 
                            }?.joinToString(", ") ?: ""
                            
                            if (title.isNotBlank()) {
                                tracks.add(SpotifyTrack(title, artists, duration))
                            }
                        } catch (e: Exception) {
                            Log.w("SpotifyImportHelper", "Skipping track", e)
                        }
                    }
                }
                
                pageCount++
                nextUrl = if (jsonObj.has("next") && !jsonObj.get("next").isJsonNull) jsonObj.get("next").asString else null
                response.close()
                if (nextUrl != null) delay(100)
                
            } catch (e: Exception) {
                if (consecutiveErrors < 3) {
                    consecutiveErrors++
                    delay(consecutiveErrors * 1500L)
                    continue
                }
                break
            }
        }
        name to tracks
    }

    /**
     * Finds a single match for a song on YouTube Music with improved logic.
     */
    suspend fun findMatch(title: String, artist: String, durationMs: Long = 0): Song? {
        return try {
            val query = "$title $artist"
            val searchResults = youTubeRepository.search(query)
            
            if (searchResults.isEmpty()) return null

            // 1. Try to find an exact title match (ignoring case) with duration check
            var bestMatch = searchResults.find { result ->
                val titleMatch = result.title.equals(title, ignoreCase = true) ||
                               result.title.contains(title, ignoreCase = true) ||
                               title.contains(result.title, ignoreCase = true)
                
                val durationMatch = if (durationMs > 0 && result.duration > 0) {
                    kotlin.math.abs(result.duration - durationMs) < 10000 // within 10 seconds
                } else true
                
                titleMatch && durationMatch
            }

            // 2. Fallback to duration-only check among top results if title match is loose
            if (bestMatch == null && durationMs > 0) {
                bestMatch = searchResults.take(3).find { result ->
                    kotlin.math.abs(result.duration - durationMs) < 5000 // within 5 seconds
                }
            }
            
            bestMatch ?: searchResults.firstOrNull()
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Match failed", e)
            null
        }
    }
}

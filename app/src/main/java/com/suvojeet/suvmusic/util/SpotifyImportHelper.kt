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
     * Extracts song titles, artists, and durations from a Spotify URL.
     * onTrackFetch is called periodically with the count of tracks fetched so far.
     */
    suspend fun getPlaylistSongs(
        url: String,
        onTrackFetch: (Int) -> Unit = {}
    ): Pair<String, List<SpotifyTrack>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<SpotifyTrack>()
        var playlistName = "Spotify Import ${System.currentTimeMillis() / 1000}"

        try {
            val finalUrl = if (url.contains("spotify.link") || (url.contains("open.spotify.com") && !url.contains("/playlist/") && !url.contains("/album/") && !url.contains("/artist/"))) {
                resolveShortenedUrl(url) ?: url
            } else url

            val (type, id) = extractTypeAndId(finalUrl)
            
            if (id != null && type != null) {
                val accessToken = getSpotifyAccessToken(finalUrl)
                if (accessToken != null) {
                    try {
                        val (name, apiTracks) = fetchSpotifyTracksWithApi(id, type, accessToken, onTrackFetch)
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

            // Fallback to embed (only for playlists, limited to 100 tracks)
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
                        val entity = jsonObject.getAsJsonObject("props")?.getAsJsonObject("pageProps")
                            ?.getAsJsonObject("state")?.getAsJsonObject("data")?.getAsJsonObject("entity")

                        if (entity != null) {
                            playlistName = entity.get("name")?.asString ?: entity.get("title")?.asString ?: playlistName
                            val trackList = entity.getAsJsonArray("trackList")
                            if (trackList != null) {
                                for (element in trackList) {
                                    val trackObj = element.asJsonObject
                                    tracks.add(SpotifyTrack(
                                        trackObj.get("title").asString,
                                        trackObj.get("subtitle").asString,
                                        trackObj.get("duration")?.asLong ?: 0L
                                    ))
                                }
                                onTrackFetch(tracks.size)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SpotifyImportHelper", "Spotify embed fallback failed", e)
                }
            }

            // Scrape original URL (minimal info)
            if (tracks.isEmpty()) {
                val doc = Jsoup.connect(finalUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get()
                
                val docTitle = doc.title()
                if (docTitle.contains("Spotify")) {
                    playlistName = docTitle.split("- Playlist by").firstOrNull()?.trim() ?: docTitle
                }

                val trackNames = doc.select("span.track-name, div.track-name, .track-name")
                val artistNames = doc.select("span.artist-name, div.artist-name, .artist-name")

                if (trackNames.isNotEmpty() && trackNames.size == artistNames.size) {
                    for (i in trackNames.indices) {
                        tracks.add(SpotifyTrack(trackNames[i].text(), artistNames[i].text()))
                    }
                    onTrackFetch(tracks.size)
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
                val request = Request.Builder().url(shortUrl).build()
                val response = okHttpClient.newCall(request).execute()
                val finalUrl = response.request.url.toString()
                response.close()
                finalUrl
            } catch (e: Exception) {
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
        } catch (e: Exception) {}

        try {
            val token = extractAccessTokenFromPage(url)
            if (token != null) return token
        } catch (e: Exception) {}

        val playlistId = extractPlaylistId(url)
        if (playlistId != null) {
            try {
                val token = extractAccessTokenFromEmbed(playlistId)
                if (token != null) return token
            } catch (e: Exception) {}
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
                val jsonObj = gson.fromJson(response.body.string(), JsonObject::class.java)
                jsonObj.get("accessToken")?.asString
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
        return findAccessTokenRecursive(gson.fromJson(nextDataScript.html(), JsonObject::class.java), 0)
    }

    private fun extractAccessTokenFromEmbed(playlistId: String): String? {
        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
        val doc = Jsoup.connect(embedUrl)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()
        val nextDataScript = doc.select("script#__NEXT_DATA__").first() ?: return null
        return findAccessTokenRecursive(gson.fromJson(nextDataScript.html(), JsonObject::class.java), 0)
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

    private suspend fun fetchSpotifyTracksWithApi(
        id: String,
        type: String,
        accessToken: String,
        onTrackFetch: (Int) -> Unit
    ): Pair<String, List<SpotifyTrack>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<SpotifyTrack>()
        var name = ""
        
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
                val jsonObj = gson.fromJson(response.body.string(), JsonObject::class.java)
                name = jsonObj.get("name")?.asString ?: ""
                
                if (type == "track") {
                    val artists = jsonObj.getAsJsonArray("artists").map { it.asJsonObject.get("name").asString }.joinToString(", ")
                    return@withContext name to listOf(SpotifyTrack(name, artists, jsonObj.get("duration_ms").asLong))
                }
                
                if (type == "artist") {
                    val tracksArray = jsonObj.getAsJsonArray("tracks")
                    tracksArray?.forEach { 
                        val t = it.asJsonObject
                        val artists = t.getAsJsonArray("artists").map { a -> a.asJsonObject.get("name").asString }.joinToString(", ")
                        tracks.add(SpotifyTrack(t.get("name").asString, artists, t.get("duration_ms").asLong))
                    }
                    onTrackFetch(tracks.size)
                    return@withContext "$name - Top Tracks" to tracks
                }
            }
            response.close()
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Failed to fetch $type details", e)
        }

        var nextUrl: String? = when (type) {
            "playlist" -> "https://api.spotify.com/v1/playlists/$id/tracks?limit=100"
            "album" -> "https://api.spotify.com/v1/albums/$id/tracks?limit=50"
            else -> null
        }

        var pageCount = 0
        val maxSongs = 3000

        while (nextUrl != null && tracks.size < maxSongs) {
            try {
                val request = Request.Builder()
                    .url(nextUrl!!)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    break
                }
                
                val jsonObj = gson.fromJson(response.body.string(), JsonObject::class.java)
                val items = jsonObj.getAsJsonArray("items")
                
                if (items != null) {
                    for (item in items) {
                        try {
                            val trackObj = if (type == "playlist") item.asJsonObject.getAsJsonObject("track") else item.asJsonObject
                            if (trackObj == null || trackObj.get("name")?.isJsonNull != false) continue
                            
                            val artists = trackObj.getAsJsonArray("artists")?.mapNotNull { it.asJsonObject.get("name")?.asString }?.joinToString(", ") ?: ""
                            tracks.add(SpotifyTrack(trackObj.get("name").asString, artists, trackObj.get("duration_ms")?.asLong ?: 0L))
                        } catch (e: Exception) {}
                    }
                }
                
                pageCount++
                Log.i("SpotifyImportHelper", "Fetched page $pageCount, tracks: ${tracks.size}")
                onTrackFetch(tracks.size)
                
                nextUrl = if (jsonObj.has("next") && !jsonObj.get("next").isJsonNull) jsonObj.get("next").asString else null
                response.close()
                if (nextUrl != null) delay(100)
                
            } catch (e: Exception) {
                break
            }
        }
        name to tracks
    }

    suspend fun findMatch(title: String, artist: String, durationMs: Long = 0): Song? {
        return try {
            val query = "$title $artist"
            val searchResults = youTubeRepository.search(query)
            if (searchResults.isEmpty()) return null
            
            var bestMatch = searchResults.find { result ->
                val titleMatch = result.title.contains(title, ignoreCase = true) || title.contains(result.title, ignoreCase = true)
                val durationMatch = if (durationMs > 0 && result.duration > 0) kotlin.math.abs(result.duration - durationMs) < 10000 else true
                titleMatch && durationMatch
            }

            if (bestMatch == null && durationMs > 0) {
                bestMatch = searchResults.take(3).find { result -> kotlin.math.abs(result.duration - durationMs) < 5000 }
            }
            bestMatch ?: searchResults.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

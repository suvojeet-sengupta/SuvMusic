package com.suvojeet.suvmusic.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.suvojeet.suvmusic.data.model.ImportResult
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
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
     * Extracts song titles and artists from a Spotify playlist URL.
     * Uses Spotify's internal API with pagination to fetch ALL tracks.
     * Returns a Pair of (Playlist Name, List of (Song Title, Artist)).
     */
    suspend fun getPlaylistSongs(url: String): Pair<String, List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Pair<String, String>>()
        var playlistName = "Spotify Import ${System.currentTimeMillis() / 1000}"

        try {
            val playlistId = extractPlaylistId(url)
            
            // 1. Try API Method (Best for large playlists — handles pagination for 3500+ songs)
            if (playlistId != null) {
                // Try multiple token strategies in order of reliability
                val accessToken = getSpotifyAccessToken(url)
                if (accessToken != null) {
                    try {
                        val (name, apiSongs) = fetchSpotifyTracksWithApi(playlistId, accessToken)
                        if (name.isNotEmpty()) playlistName = name
                        if (apiSongs.isNotEmpty()) {
                            Log.i("SpotifyImportHelper", "API method fetched ${apiSongs.size} songs")
                            return@withContext playlistName to apiSongs
                        }
                    } catch (e: Exception) {
                        Log.w("SpotifyImportHelper", "Spotify API method failed, falling back to embed", e)
                    }
                } else {
                    Log.w("SpotifyImportHelper", "Could not obtain Spotify access token")
                }
            }

            // 2. Try Embed Method fallback (Reliable for < 100 songs)
            if (playlistId != null) {
                try {
                    val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
                    val doc = Jsoup.connect(embedUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .get()

                    val nextDataScript = doc.select("script#__NEXT_DATA__").first()
                    if (nextDataScript != null) {
                        val json = nextDataScript.html()
                        val jsonObject = gson.fromJson(json, JsonObject::class.java)

                        // Navigate JSON: props -> pageProps -> state -> data -> entity
                        val entity = jsonObject.getAsJsonObject("props")
                            ?.getAsJsonObject("pageProps")
                            ?.getAsJsonObject("state")
                            ?.getAsJsonObject("data")
                            ?.getAsJsonObject("entity")

                        if (entity != null) {
                            // Extract Name
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
                                    val subtitle = trackObj.get("subtitle").asString // Artists
                                    if (title.isNotBlank()) {
                                        songs.add(title to subtitle)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SpotifyImportHelper", "Spotify operation failed", e)
                }
            }

            // 3. Final Fallback: Scrape original URL
            if (songs.isEmpty()) {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get()
                
                // Try to get title from document
                val docTitle = doc.title()
                if (docTitle.contains("Spotify")) {
                    // "My Playlist - Playlist by User | Spotify" -> "My Playlist"
                    playlistName = docTitle.split("- Playlist by").firstOrNull()?.trim() 
                        ?: docTitle.split("|").firstOrNull()?.trim() 
                        ?: docTitle
                } else if (docTitle.isNotBlank()) {
                    playlistName = docTitle
                }

                // Try h1 for title
                val h1Title = doc.select("h1").text()
                if (h1Title.isNotBlank()) {
                    playlistName = h1Title
                }

                val trackNames = doc.select("span.track-name, div.track-name, .track-name")
                val artistNames = doc.select("span.artist-name, div.artist-name, .artist-name")

                if (trackNames.isNotEmpty() && trackNames.size == artistNames.size) {
                    for (i in trackNames.indices) {
                        songs.add(trackNames[i].text() to artistNames[i].text())
                    }
                }

                // If above fails, try searching for the "og:description" which sometimes lists tracks
                if (songs.isEmpty()) {
                    val ogDesc = doc.select("meta[property=og:description]").attr("content")
                    if (ogDesc.isNotBlank() && ogDesc.contains("Â·")) {
                        // Format: "Song Name Â· Artist Name, Song 2 Â· Artist 2..."
                        val parts = ogDesc.split(",")
                        for (part in parts) {
                            val trackInfo = part.split("Â·")
                            if (trackInfo.size >= 2) {
                                songs.add(trackInfo[0].trim() to trackInfo[1].trim())
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Spotify operation failed", e)
        }
        playlistName to songs
    }

    private fun extractPlaylistId(url: String): String? {
        val pattern = "playlist/([a-zA-Z0-9]+)".toRegex()
        val match = pattern.find(url)
        return match?.groupValues?.get(1)
    }

    /**
     * Tries multiple strategies to obtain a Spotify access token.
     * Strategy 1: Fetch anonymous token from Spotify's internal token endpoint
     * Strategy 2: Extract from playlist page's __NEXT_DATA__
     * Strategy 3: Extract from embed page
     */
    private suspend fun getSpotifyAccessToken(url: String): String? {
        // Strategy 1: Anonymous token (most reliable for public playlists)
        try {
            val token = fetchAnonymousSpotifyToken()
            if (token != null) {
                Log.i("SpotifyImportHelper", "Got anonymous Spotify token")
                return token
            }
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Anonymous token fetch failed", e)
        }

        // Strategy 2: Extract from playlist page HTML
        try {
            val token = extractAccessTokenFromPage(url)
            if (token != null) {
                Log.i("SpotifyImportHelper", "Got token from page HTML")
                return token
            }
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Page token extraction failed", e)
        }

        // Strategy 3: Extract from embed page
        val playlistId = extractPlaylistId(url)
        if (playlistId != null) {
            try {
                val token = extractAccessTokenFromEmbed(playlistId)
                if (token != null) {
                    Log.i("SpotifyImportHelper", "Got token from embed page")
                    return token
                }
            } catch (e: Exception) {
                Log.w("SpotifyImportHelper", "Embed token extraction failed", e)
            }
        }

        return null
    }

    /**
     * Fetch an anonymous access token from Spotify's internal token endpoint.
     * This works without any user login for public playlists.
     */
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
                if (jsonObj.has("accessToken")) {
                    jsonObj.get("accessToken").asString
                } else null
            } else null
        } finally {
            response.close()
        }
    }

    /**
     * Extract access token from the playlist page's __NEXT_DATA__ JSON.
     */
    private fun extractAccessTokenFromPage(url: String): String? {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()

        val nextDataScript = doc.select("script#__NEXT_DATA__").first() ?: return null
        val json = nextDataScript.html()
        val jsonObject = gson.fromJson(json, JsonObject::class.java)

        val pageProps = jsonObject.getAsJsonObject("props")?.getAsJsonObject("pageProps")
        val session = pageProps?.getAsJsonObject("session")
        
        if (session != null) {
            if (session.has("accessToken")) {
                return session.get("accessToken").asString
            }
            if (session.has("data")) {
                val data = session.getAsJsonObject("data")
                if (data?.has("accessToken") == true) {
                    return data.get("accessToken").asString
                }
            }
        }
        
        // Try alternative path: session might be at a different location
        val buildId = jsonObject.get("buildId")
        if (buildId != null) {
            // Some versions put the token in runtimeConfig or directly in props
            val token = findAccessTokenRecursive(jsonObject, 0)
            if (token != null) return token
        }
        
        return null
    }

    /**
     * Extract access token from the embed page.
     */
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

    /**
     * Recursively search a JSON object for an "accessToken" field (max depth 6).
     */
    private fun findAccessTokenRecursive(jsonObject: JsonObject, depth: Int): String? {
        if (depth > 6) return null
        if (jsonObject.has("accessToken")) {
            val token = jsonObject.get("accessToken")
            if (token.isJsonPrimitive && token.asString.isNotBlank()) {
                return token.asString
            }
        }
        for ((_, value) in jsonObject.entrySet()) {
            if (value.isJsonObject) {
                val found = findAccessTokenRecursive(value.asJsonObject, depth + 1)
                if (found != null) return found
            }
        }
        return null
    }

    private fun fetchSpotifyTracksWithApi(playlistId: String, accessToken: String): Pair<String, List<Pair<String, String>>> {
        val songs = mutableListOf<Pair<String, String>>()
        var playlistName = ""
        var totalExpected = 0
        // Use fields parameter to reduce payload size — only fetch what we need
        var nextUrl: String? = "https://api.spotify.com/v1/playlists/$playlistId/tracks?offset=0&limit=100&fields=items(track(name,artists(name))),next,total"

        // First, fetch playlist details to get the name and total track count
        try {
            val request = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId?fields=name,tracks.total")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body.string()
                if (json != null) {
                    val jsonObj = gson.fromJson(json, JsonObject::class.java)
                    if (jsonObj.has("name")) {
                        playlistName = jsonObj.get("name").asString
                    }
                    val tracks = jsonObj.getAsJsonObject("tracks")
                    if (tracks?.has("total") == true) {
                        totalExpected = tracks.get("total").asInt
                        Log.i("SpotifyImportHelper", "Playlist '$playlistName' has $totalExpected tracks")
                    }
                }
            }
            response.close()
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Failed to fetch playlist details", e)
        }

        // Pagination Loop — fetch ALL pages
        var pageCount = 0
        var consecutiveErrors = 0
        val maxConsecutiveErrors = 3

        while (nextUrl != null) {
            val currentNextUrl: String = nextUrl
            try {
                val request = Request.Builder()
                    .url(currentNextUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    
                    // If rate limited (429), wait and retry
                    if (code == 429) {
                        consecutiveErrors++
                        if (consecutiveErrors <= maxConsecutiveErrors) {
                            Log.w("SpotifyImportHelper", "Rate limited, waiting ${consecutiveErrors * 2}s before retry...")
                            Thread.sleep(consecutiveErrors * 2000L)
                            continue // Retry same URL
                        }
                    }
                    
                    // If token expired (401), don't retry
                    if (code == 401) {
                        Log.e("SpotifyImportHelper", "Token expired after fetching ${songs.size} songs")
                    }
                    break
                }
                
                consecutiveErrors = 0 // Reset on success

                val json = response.body.string()
                val jsonObj = gson.fromJson(json, JsonObject::class.java)

                // Get items
                val items = jsonObj.getAsJsonArray("items")
                if (items != null) {
                    for (item in items) {
                        try {
                            val itemObj = item.asJsonObject
                            val trackObj = itemObj.getAsJsonObject("track") ?: continue
                            // Skip null/local tracks
                            if (!trackObj.has("name") || trackObj.get("name").isJsonNull) continue
                            val title = trackObj.get("name").asString
                            
                            val artistsArray = trackObj.getAsJsonArray("artists")
                            val artistList = mutableListOf<String>()
                            if (artistsArray != null) {
                                for (artist in artistsArray) {
                                    val artistObj = artist.asJsonObject
                                    if (artistObj.has("name") && !artistObj.get("name").isJsonNull) {
                                        artistList.add(artistObj.get("name").asString)
                                    }
                                }
                            }
                            val artist = artistList.joinToString(", ")
                            
                            if (title.isNotBlank()) {
                                songs.add(title to artist)
                            }
                        } catch (e: Exception) {
                            // Skip malformed individual tracks, don't break the whole loop
                            Log.w("SpotifyImportHelper", "Skipping malformed track", e)
                        }
                    }
                }
                
                pageCount++
                Log.i("SpotifyImportHelper", "Fetched page $pageCount — ${songs.size}/$totalExpected tracks so far")

                // Check for next page
                nextUrl = if (jsonObj.has("next") && !jsonObj.get("next").isJsonNull) {
                    jsonObj.get("next").asString
                } else {
                    null
                }
                
                response.close()
                
                // Small delay between pages to avoid rate limiting on large playlists
                if (nextUrl != null) {
                    Thread.sleep(100)
                }
                
            } catch (e: Exception) {
                Log.w("SpotifyImportHelper", "Error fetching page ${pageCount + 1}, songs so far: ${songs.size}", e)
                consecutiveErrors++
                if (consecutiveErrors <= maxConsecutiveErrors) {
                    // Retry same URL with increasing delay
                    Thread.sleep(consecutiveErrors * 1500L)
                    continue
                }
                break
            }
        }

        Log.i("SpotifyImportHelper", "Finished fetching: ${songs.size} songs out of $totalExpected expected")
        return playlistName to songs
    }

    /**
     * Matches Spotify songs to YouTube Music songs (Batch).
     */
    suspend fun matchSongsOnYouTube(spotifySongs: List<Pair<String, String>>, onProgress: (Int, Int) -> Unit): List<ImportResult> {
        val results = mutableListOf<ImportResult>()
        spotifySongs.forEachIndexed { index, (title, artist) ->
            val match = findMatch(title, artist)
            results.add(ImportResult(title, artist, match))
            onProgress(index + 1, spotifySongs.size)
        }
        return results
    }

    /**
     * Finds a single match for a song on YouTube Music.
     */
    suspend fun findMatch(title: String, artist: String): Song? {
        return try {
            val query = "$title $artist"
            val searchResults = youTubeRepository.search(query)
            searchResults.firstOrNull { 
                it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
            } ?: searchResults.firstOrNull()
        } catch (e: Exception) {
            Log.w("SpotifyImportHelper", "Spotify operation failed", e)
            null
        }
    }
}

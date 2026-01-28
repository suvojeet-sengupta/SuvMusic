package com.suvojeet.suvmusic.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.suvojeet.suvmusic.data.model.ImportResult
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyImportHelper @Inject constructor(
    private val youTubeRepository: YouTubeRepository
) {
    /**
     * Extracts song titles and artists from a Spotify playlist URL.
     * Note: This uses Jsoup to scrape the public Spotify playlist page.
     */
    /**
     * Extracts song titles and artists from a Spotify playlist URL.
     * Note: This uses Jsoup to scrape the public Spotify playlist page.
     * Returns a Pair of (Playlist Name, List of (Song Title, Artist)).
     */
    suspend fun getPlaylistSongs(url: String): Pair<String, List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Pair<String, String>>()
        var playlistName = "Spotify Import ${System.currentTimeMillis() / 1000}"

        try {
            // 1. Try to extract playlist ID and use Embed method (most reliable)
            val playlistId = extractPlaylistId(url)
            if (playlistId != null) {
                try {
                    val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
                    val doc = Jsoup.connect(embedUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .get()

                    val nextDataScript = doc.select("script#__NEXT_DATA__").first()
                    if (nextDataScript != null) {
                        val json = nextDataScript.html()
                        val gson = Gson()
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
                    e.printStackTrace()
                }
            }

            // 2. Fallback to scraping the original URL if embed failed or no songs found
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
                    if (ogDesc.isNotBlank() && ogDesc.contains("·")) {
                        // Format: "Song Name · Artist Name, Song 2 · Artist 2..."
                        val parts = ogDesc.split(",")
                        for (part in parts) {
                            val trackInfo = part.split("·")
                            if (trackInfo.size >= 2) {
                                songs.add(trackInfo[0].trim() to trackInfo[1].trim())
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        playlistName to songs
    }

    private fun extractPlaylistId(url: String): String? {
        val pattern = "playlist/([a-zA-Z0-9]+)".toRegex()
        val match = pattern.find(url)
        return match?.groupValues?.get(1)
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
            e.printStackTrace()
            null
        }
    }
}

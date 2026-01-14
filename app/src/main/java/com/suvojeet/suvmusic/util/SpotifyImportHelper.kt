package com.suvojeet.suvmusic.util

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
    suspend fun getPlaylistSongs(url: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Pair<String, String>>()
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .get()

            // Spotify's public page structure often changes, but meta tags or specific classes usually contain song info.
            // For the public non-logged in view, they often use meta tags or specific item classes.
            
            // Try meta tags first (OpenGraph) - though this only gives the first few or general info.
            
            // Modern Spotify web player uses a more complex structure, but we can try to find the track names.
            // A more robust way without official API is to look for specific patterns in the HTML.
            
            // Let's look for track names in the embedded JSON if available, or list items.
            val scriptTags = doc.select("script")
            for (script in scriptTags) {
                val content = script.html()
                if (content.contains("Spotify.Entity")) {
                    // This often contains track list in JSON format
                    // Parsing this would be ideal but complex.
                }
            }

            // Fallback to searching for specific elements that contain track info
            // Track names are often in elements with specific data-attributes or classes
            val trackElements = doc.select("meta[property=music:song]")
            if (trackElements.isNotEmpty()) {
                // Some pages might have these meta tags
            }

            // Most common structure for public playlists as of late 2024/2025:
            // Track names are in spans or divs with specific roles or classes.
            // Since we can't reliably guess the exact class names (they are often obfuscated), 
            // we will look for the track name and artist patterns.
            
            // Alternatively, Spotify provides a "meta" view for some links.
            // Let's try to extract from the most likely places.
            
            val trackRows = doc.select("div[role=gridcell]") // Common in modern web apps
            
            // If scraping fails due to heavy JS, we might need a different approach or 
            // inform the user. But Jsoup can often see the SSR content.
            
            // Re-evaluating: Spotify's public playlist page usually has a "playlist-track-name" 
            // or similar in the SSR HTML for SEO.
            
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

        } catch (e: Exception) {
            e.printStackTrace()
        }
        songs
    }

    /**
     * Matches Spotify songs to YouTube Music songs.
     */
    suspend fun matchSongsOnYouTube(spotifySongs: List<Pair<String, String>>, onProgress: (Int, Int) -> Unit): List<Song> {
        val matchedSongs = mutableListOf<Song>()
        spotifySongs.forEachIndexed { index, (title, artist) ->
            try {
                val query = "$title $artist"
                val searchResults = youTubeRepository.search(query)
                val bestMatch = searchResults.firstOrNull { 
                    it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                } ?: searchResults.firstOrNull()
                
                bestMatch?.let { matchedSongs.add(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onProgress(index + 1, spotifySongs.size)
        }
        return matchedSongs
    }
}

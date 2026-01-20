package com.suvojeet.suvmusic.data.repository

import com.google.gson.JsonParser
import com.suvojeet.suvmusic.data.model.Lyrics
import com.suvojeet.suvmusic.data.model.LyricsLine
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import com.suvojeet.suvmusic.util.encodeUrl

/**
 * Repository for fetching lyrics from multiple sources.
 * Prioritizes:
 * 1. LRCLIB (Synced)
 * 2. Origin Platform (YouTube/JioSaavn)
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository
) {

    suspend fun getLyrics(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        // 1. Try LRCLIB for synced lyrics (Best experience)
        val lrcLibLyrics = getSyncedLyricsFromLrcLib(song.title, song.artist, song.duration)
        
        // If we found synced lyrics, return immediately
        if (lrcLibLyrics != null && lrcLibLyrics.isSynced) {
            return@withContext lrcLibLyrics
        }
        
        // 2. Fallback: Get lyrics from the original source
        // If we got non-synced lyrics from LRCLIB, we can still check the source for better ones?
        // Or just use LRCLIB's plain text? 
        // Strategy: If source is YTM, YTM lyrics are often just text/static too unless synced.
        // Let's prefer Source > LRCLIB Plain if source has something? 
        // Actually, LRCLIB plain might be better than no lyrics.
        
        val sourceLyrics = when (song.source) {
            SongSource.JIOSAAVN -> {
                jioSaavnRepository.getLyricsFromJioSaavn(song.id)?.let { text ->
                    val lines = text.split("\n").map { LyricsLine(text = it.trim()) }
                    Lyrics(lines = lines, sourceCredit = "Lyrics from JioSaavn", isSynced = false)
                }
            }
            SongSource.YOUTUBE, SongSource.DOWNLOADED, SongSource.LOCAL -> {
                // For YouTube, it might return synced lyrics too!
                try {
                     youTubeRepository.getLyrics(song.id)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
        
        if (sourceLyrics != null) {
            return@withContext sourceLyrics
        }
        
        // 3. Last resort: Return LRCLIB plain lyrics if we found them earlier
        if (lrcLibLyrics != null) {
            return@withContext lrcLibLyrics
        }
        
        null
    }

    private suspend fun getSyncedLyricsFromLrcLib(title: String, artist: String, duration: Long): Lyrics? {
        return try {
            // Clean up title and artist for better matching
            val cleanTitle = title.replace(Regex("\\s*\\(.*?\\)"), "") // Remove parentheses content
                .replace(Regex("\\s*\\[.*?\\]"), "") // Remove brackets content
                .replace(Regex("\\s*-\\s*.*"), "") // Remove after dash
                .trim()
            val cleanArtist = artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: artist
            val durationSeconds = (duration / 1000).toInt()
            
            // Try LRCLIB API for synced lyrics
            val lrcLibUrl = "https://lrclib.net/api/get?" +
                "track_name=${cleanTitle.encodeUrl()}" +
                "&artist_name=${cleanArtist.encodeUrl()}" +
                "&duration=$durationSeconds"
            
            android.util.Log.d("LyricsRepo", "Fetching synced lyrics from: $lrcLibUrl")
            
            val request = Request.Builder()
                .url(lrcLibUrl)
                .addHeader("User-Agent", "SuvMusic/1.0 (https://github.com/suvojeet-sengupta/SuvMusic)")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrBlank() && responseBody != "null") {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    
                    // Try to get synced lyrics first
                    val syncedLyrics = json.get("syncedLyrics")?.asString
                    if (!syncedLyrics.isNullOrBlank()) {
                        val lines = parseLrcLyrics(syncedLyrics)
                        if (lines.isNotEmpty()) {
                            android.util.Log.d("LyricsRepo", "Found synced lyrics with ${lines.size} lines")
                            return Lyrics(
                                lines = lines,
                                sourceCredit = "Lyrics from LRCLIB",
                                isSynced = true
                            )
                        }
                    }
                    
                    // Return plain lyrics as fallback option (but don't return yet in main flow)
                    val plainLyrics = json.get("plainLyrics")?.asString
                    if (!plainLyrics.isNullOrBlank()) {
                         val lines = plainLyrics.split("\n").map { line ->
                            LyricsLine(text = line.trim())
                        }
                        return Lyrics(
                            lines = lines,
                            sourceCredit = "Lyrics from LRCLIB",
                            isSynced = false
                        )
                    }
                }
            }
            response.close()
            
            // Fallback: Search LRCLIB if exact match failed
            val searchUrl = "https://lrclib.net/api/search?q=${(cleanTitle + " " + cleanArtist).encodeUrl()}"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("User-Agent", "SuvMusic/1.0")
                .build()
            
            val searchResponse = okHttpClient.newCall(searchRequest).execute()
            if (searchResponse.isSuccessful) {
                val searchBody = searchResponse.body?.string()
                if (!searchBody.isNullOrBlank() && searchBody != "[]") {
                    val results = JsonParser.parseString(searchBody).asJsonArray
                    if (results.size() > 0) {
                        val firstResult = results[0].asJsonObject
                        val syncedLyrics = firstResult.get("syncedLyrics")?.asString
                        if (!syncedLyrics.isNullOrBlank()) {
                            val lines = parseLrcLyrics(syncedLyrics)
                            if (lines.isNotEmpty()) {
                                android.util.Log.d("LyricsRepo", "Found synced lyrics via search")
                                return Lyrics(
                                    lines = lines,
                                    sourceCredit = "Lyrics from LRCLIB",
                                    isSynced = true
                                )
                            }
                        }
                        
                         val plainLyrics = firstResult.get("plainLyrics")?.asString
                        if (!plainLyrics.isNullOrBlank()) {
                            val lines = plainLyrics.split("\n").map { line ->
                                LyricsLine(text = line.trim())
                            }
                            return Lyrics(
                                lines = lines,
                                sourceCredit = "Lyrics from LRCLIB",
                                isSynced = false
                            )
                        }
                    }
                }
            }
            searchResponse.close()
            
            null
        } catch (e: Exception) {
            android.util.Log.e("LyricsRepo", "Error fetching synced lyrics", e)
            null
        }
    }

    private fun parseLrcLyrics(lrcContent: String): List<LyricsLine> {
        val lines = mutableListOf<LyricsLine>()
        val lrcPattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)") // [mm:ss.xx]text
        
        lrcContent.split("\n").forEach { line ->
            val match = lrcPattern.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val millisPart = match.groupValues[3]
                val millis = if (millisPart.length == 2) {
                    (millisPart.toLongOrNull() ?: 0L) * 10
                } else {
                    millisPart.toLongOrNull() ?: 0L
                }
                val text = match.groupValues[4].trim()
                
                val startTimeMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                
                if (text.isNotBlank()) {
                    lines.add(
                        LyricsLine(
                            text = text,
                            startTimeMs = startTimeMs
                        )
                    )
                }
            }
        }
        
        // Calculate end times based on next line's start time
        for (i in lines.indices) {
            if (i < lines.lastIndex) {
                lines[i] = lines[i].copy(endTimeMs = lines[i + 1].startTimeMs)
            } else {
                // Last line - add 5 seconds as default duration
                lines[i] = lines[i].copy(endTimeMs = lines[i].startTimeMs + 5000)
            }
        }
        
        return lines
    }
}

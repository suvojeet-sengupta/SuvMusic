package com.suvojeet.suvmusic.lrclib

import com.google.gson.JsonParser
import com.suvojeet.suvmusic.providers.lyrics.LyricsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.math.min

class LrcLibLyricsProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : LyricsProvider {

    override val name = "LRCLIB"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Clean up title and artist for better matching
            val cleanTitle = title.replace(Regex("\\s*\\(.*?\\)"), "") // Remove parentheses content
                .replace(Regex("\\s*\\[.*?\\]"), "") // Remove brackets content
                .replace(Regex("\\s*-\\s*.*"), "") // Remove after dash
                .trim()
            val cleanArtist = artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: artist
            
            // Try LRCLIB API for synced lyrics
            val lrcLibUrl = "https://lrclib.net/api/get?" +
                "track_name=${encodeUrl(cleanTitle)}" +
                "&artist_name=${encodeUrl(cleanArtist)}" +
                "&duration=$duration"
            
            val request = Request.Builder()
                .url(lrcLibUrl)
                .addHeader("User-Agent", "SuvMusic/1.0 (https://github.com/suvojeet-sengupta/SuvMusic)")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrBlank() && responseBody != "null") {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    
                    val syncedLyrics = json.get("syncedLyrics")?.asString
                    if (!syncedLyrics.isNullOrBlank()) {
                        return@withContext Result.success(syncedLyrics)
                    }
                    
                    val plainLyrics = json.get("plainLyrics")?.asString
                    if (!plainLyrics.isNullOrBlank()) {
                        return@withContext Result.success(plainLyrics)
                    }
                }
            }
            response.close()
            
            // Fallback: Search LRCLIB if exact match failed
            val searchUrl = "https://lrclib.net/api/search?q=${encodeUrl("$cleanTitle $cleanArtist")}"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("User-Agent", "SuvMusic/1.0")
                .build()
            
            val searchResponse = okHttpClient.newCall(searchRequest).execute()
            if (searchResponse.isSuccessful) {
                val searchBody = searchResponse.body?.string()
                if (!searchBody.isNullOrBlank() && searchBody != "[]") {
                    val results = JsonParser.parseString(searchBody).asJsonArray
                    
                    // Iterate through all results and find the best match
                    var bestMatch: com.google.gson.JsonObject? = null
                    var bestScore = 0.0
                    
                    for (i in 0 until results.size()) {
                        val result = results[i].asJsonObject
                        val resultTitle = result.get("trackName")?.asString ?: ""
                        val resultArtist = result.get("artistName")?.asString ?: ""
                        val resultDuration = result.get("duration")?.asDouble ?: 0.0
                        
                        // Calculate score
                        val titleScore = calculateSimilarity(cleanTitle, resultTitle)
                        val artistScore = calculateSimilarity(cleanArtist, resultArtist)
                        
                        // critical check: if major mismatch in title or artist, skip
                        if (titleScore < 0.3 || artistScore < 0.3) continue
                        
                        var totalScore = (titleScore * 0.6) + (artistScore * 0.4)
                        
                        // Duration penalty
                        val durationDiff = kotlin.math.abs(duration - resultDuration)
                        if (durationDiff < 5) {
                            totalScore += 0.2 // Bonus for exact duration match
                        } else if (durationDiff > 20) {
                            totalScore -= 0.3 // Heavy penalty for large duration mismatch
                        } else {
                            // Linear penalty for difference between 5s and 20s
                            val penalty = (durationDiff - 5) / 15.0 * 0.3
                            totalScore -= penalty
                        }
                        
                        if (totalScore > bestScore) {
                            bestScore = totalScore
                            bestMatch = result
                        }
                    }
                    
                    // Threshold for accepting a match
                    if (bestMatch != null && bestScore > 0.65) {
                         val syncedLyrics = bestMatch.get("syncedLyrics")?.asString
                        if (!syncedLyrics.isNullOrBlank()) {
                            return@withContext Result.success(syncedLyrics)
                        }
                        
                         val plainLyrics = bestMatch.get("plainLyrics")?.asString
                        if (!plainLyrics.isNullOrBlank()) {
                             return@withContext Result.success(plainLyrics)
                        }
                    }
                }
            }
            searchResponse.close()
            
            return@withContext Result.failure(Exception("Lyrics not found"))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    private fun encodeUrl(url: String): String {
        return URLEncoder.encode(url, "UTF-8")
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val n1 = normalizeString(s1)
        val n2 = normalizeString(s2)
        
        if (n1.isEmpty() && n2.isEmpty()) return 1.0
        if (n1.isEmpty() || n2.isEmpty()) return 0.0
        if (n1 == n2) return 1.0
        
        val distance = levenshteinDistance(n1, n2)
        val maxLength = kotlin.math.max(n1.length, n2.length)
        
        return 1.0 - (distance.toDouble() / maxLength)
    }
    
    private fun normalizeString(s: String): String {
        return s.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1,       // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
}

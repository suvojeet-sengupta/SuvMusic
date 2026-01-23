package com.suvojeet.suvmusic.data.repository

import android.content.Context
import android.util.LruCache
import com.google.gson.JsonParser
import com.suvojeet.suvmusic.data.model.Lyrics
import com.suvojeet.suvmusic.data.model.LyricsLine
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.repository.lyrics.BetterLyricsProvider
import com.suvojeet.suvmusic.data.repository.lyrics.LyricsProvider
import com.suvojeet.suvmusic.data.repository.lyrics.SimpMusicLyricsProvider
import com.suvojeet.suvmusic.data.model.LyricsProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import com.suvojeet.suvmusic.util.encodeUrl

/**
 * Repository for fetching lyrics from multiple providers.
 * Priority: BetterLyrics → SimpMusic → LRCLIB → YouTube/JioSaavn
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val betterLyricsProvider: BetterLyricsProvider,
    private val simpMusicLyricsProvider: SimpMusicLyricsProvider
) {
    private val cache = LruCache<String, Lyrics>(MAX_CACHE_SIZE)

    /**
     * Ordered list of lyrics providers
     */
    private fun getLyricsProviders(): List<LyricsProvider> {
        return buildList {
            // BetterLyrics (Apple Music TTML) - highest quality
            if (betterLyricsProvider.isEnabled(context)) {
                add(betterLyricsProvider)
            }
            
            // SimpMusic
            if (simpMusicLyricsProvider.isEnabled(context)) {
                add(simpMusicLyricsProvider)
            }
        }
    }

    suspend fun getLyrics(song: Song, providerType: LyricsProviderType = LyricsProviderType.AUTO): Lyrics? = withContext(Dispatchers.IO) {
        // If specific provider requested, bypass cache for that provider
        if (providerType != LyricsProviderType.AUTO) {
            return@withContext fetchFromProvider(song, providerType)
        }
        
        // Check cache first for AUTO mode
        val cached = cache.get(song.id)
        if (cached != null) {
            return@withContext cached
        }
        
        // AUTO Mode: Priority Order
        
        // 1. Try external providers (BetterLyrics, SimpMusic)
        for (provider in getLyricsProviders()) {
            try {
                provider.getLyrics(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    duration = (song.duration / 1000).toInt(),
                    album = song.album
                ).onSuccess { lrcText ->
                    val parsed = parseLrcLyrics(lrcText)
                    if (parsed.isNotEmpty()) {
                        val providerEnum = if (provider == betterLyricsProvider) LyricsProviderType.BETTER_LYRICS else LyricsProviderType.SIMP_MUSIC
                        val lyrics = Lyrics(
                            lines = parsed,
                            sourceCredit = "Lyrics from ${provider.name}",
                            isSynced = true,
                            provider = providerEnum
                        )
                        cache.put(song.id, lyrics)
                        return@withContext lyrics
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }
        
        // 2. Try LRCLIB for synced lyrics
        val lrcLibLyrics = getSyncedLyricsFromLrcLib(song.title, song.artist, song.duration)
        if (lrcLibLyrics != null && lrcLibLyrics.isSynced) {
            cache.put(song.id, lrcLibLyrics)
            return@withContext lrcLibLyrics
        }
        
        // 3. Fallback: Get lyrics from the original source (JioSaavn/YouTube)
        val sourceLyrics = fetchFromSource(song)
        
        if (sourceLyrics != null) {
            cache.put(song.id, sourceLyrics)
            return@withContext sourceLyrics
        }
        
        // 4. Last resort: Return LRCLIB plain lyrics if we found them earlier
        if (lrcLibLyrics != null) {
            cache.put(song.id, lrcLibLyrics)
            return@withContext lrcLibLyrics
        }
        
        null
    }
    
    // Fetch from a specific provider
    private suspend fun fetchFromProvider(song: Song, providerType: LyricsProviderType): Lyrics? {
        return when (providerType) {
            LyricsProviderType.BETTER_LYRICS -> {
                if (betterLyricsProvider.isEnabled(context)) {
                    fetchExternalLyrics(betterLyricsProvider, song, LyricsProviderType.BETTER_LYRICS)
                } else null
            }
            LyricsProviderType.SIMP_MUSIC -> {
                if (simpMusicLyricsProvider.isEnabled(context)) {
                    fetchExternalLyrics(simpMusicLyricsProvider, song, LyricsProviderType.SIMP_MUSIC)
                } else null
            }
            LyricsProviderType.LRCLIB -> {
                getSyncedLyricsFromLrcLib(song.title, song.artist, song.duration)
            }
            LyricsProviderType.JIOSAAVN -> {
                jioSaavnRepository.getLyricsFromJioSaavn(song.id)?.let { text ->
                    val lines = text.split("\n").map { LyricsLine(text = it.trim()) }
                    Lyrics(lines = lines, sourceCredit = "Lyrics from JioSaavn", isSynced = false, provider = LyricsProviderType.JIOSAAVN)
                }
            }
            LyricsProviderType.YOUTUBE -> {
                try {
                     // Force YouTube fetch even if source is different (search by ID might fail if ID is not YT, handle carefully)
                     // If song source is NOT YT, we might not have a valid YT ID. 
                     // But typically this is called with a YT ID for YT/YTMusic songs.
                     if (song.source == SongSource.YOUTUBE || song.source == SongSource.DOWNLOADED) {
                        youTubeRepository.getLyrics(song.id)?.copy(provider = LyricsProviderType.YOUTUBE)
                     } else null
                } catch (e: Exception) { null }
            }
            LyricsProviderType.AUTO -> getLyrics(song, LyricsProviderType.AUTO)
        }
    }

    private suspend fun fetchExternalLyrics(
        provider: LyricsProvider, 
        song: Song, 
        type: LyricsProviderType
    ): Lyrics? {
        return try {
            var result: Lyrics? = null
            provider.getLyrics(
                id = song.id,
                title = song.title,
                artist = song.artist,
                duration = (song.duration / 1000).toInt(),
                album = song.album
            ).onSuccess { lrcText ->
                val parsed = parseLrcLyrics(lrcText)
                if (parsed.isNotEmpty()) {
                    result = Lyrics(
                        lines = parsed,
                        sourceCredit = "Lyrics from ${provider.name}",
                        isSynced = true,
                        provider = type
                    )
                }
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchFromSource(song: Song): Lyrics? {
        return when (song.source) {
            SongSource.JIOSAAVN -> {
                jioSaavnRepository.getLyricsFromJioSaavn(song.id)?.let { text ->
                    val lines = text.split("\n").map { LyricsLine(text = it.trim()) }
                    Lyrics(lines = lines, sourceCredit = "Lyrics from JioSaavn", isSynced = false, provider = LyricsProviderType.JIOSAAVN)
                }
            }
            SongSource.YOUTUBE, SongSource.DOWNLOADED, SongSource.LOCAL -> {
                try {
                     youTubeRepository.getLyrics(song.id)?.copy(provider = LyricsProviderType.YOUTUBE)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
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
                                isSynced = true,
                                provider = LyricsProviderType.LRCLIB
                            )
                        }
                    }
                    
                    // Return plain lyrics as fallback option
                    val plainLyrics = json.get("plainLyrics")?.asString
                    if (!plainLyrics.isNullOrBlank()) {
                         val lines = plainLyrics.split("\n").map { line ->
                            LyricsLine(text = line.trim())
                        }
                        return Lyrics(
                            lines = lines,
                            sourceCredit = "Lyrics from LRCLIB",
                            isSynced = false,
                            provider = LyricsProviderType.LRCLIB
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
                        val durationDiff = kotlin.math.abs(durationSeconds - resultDuration)
                        if (durationDiff < 5) {
                            totalScore += 0.2 // Bonus for exact duration match
                        } else if (durationDiff > 20) {
                            totalScore -= 0.3 // Heavy penalty for large duration mismatch
                        } else {
                            // Linear penalty for difference between 5s and 20s
                            val penalty = (durationDiff - 5) / 15.0 * 0.3
                            totalScore -= penalty
                        }
                        
                        android.util.Log.d("LyricsRepo", "Match candidate: $resultTitle by $resultArtist, score: $totalScore")
                        
                        if (totalScore > bestScore) {
                            bestScore = totalScore
                            bestMatch = result
                        }
                    }
                    
                    // Threshold for accepting a match
                    if (bestMatch != null && bestScore > 0.65) {
                         android.util.Log.d("LyricsRepo", "Selected best match with score $bestScore: ${bestMatch.get("trackName")}")
                         
                         val syncedLyrics = bestMatch.get("syncedLyrics")?.asString
                        if (!syncedLyrics.isNullOrBlank()) {
                            val lines = parseLrcLyrics(syncedLyrics)
                            if (lines.isNotEmpty()) {
                                return Lyrics(
                                    lines = lines,
                                    sourceCredit = "Lyrics from LRCLIB (Best Match)",
                                    isSynced = true,
                                    provider = LyricsProviderType.LRCLIB
                                )
                            }
                        }
                        
                         val plainLyrics = bestMatch.get("plainLyrics")?.asString
                        if (!plainLyrics.isNullOrBlank()) {
                            val lines = plainLyrics.split("\n").map { line ->
                                LyricsLine(text = line.trim())
                            }
                            return Lyrics(
                                lines = lines,
                                sourceCredit = "Lyrics from LRCLIB (Best Match)",
                                isSynced = false,
                                provider = LyricsProviderType.LRCLIB
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
        val lrcPattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
        val wordTimingPattern = Regex("<(.*)>")

        val rawLines = lrcContent.split("\n")
        var i = 0
        while (i < rawLines.size) {
            val line = rawLines[i]
            val lrcMatch = lrcPattern.find(line)
            
            if (lrcMatch != null) {
                // Parse Standard Line
                val minutes = lrcMatch.groupValues[1].toLongOrNull() ?: 0L
                val seconds = lrcMatch.groupValues[2].toLongOrNull() ?: 0L
                val millisPart = lrcMatch.groupValues[3]
                val millis = if (millisPart.length == 2) {
                    (millisPart.toLongOrNull() ?: 0L) * 10
                } else {
                    millisPart.toLongOrNull() ?: 0L
                }
                val text = lrcMatch.groupValues[4].trim()
                
                val startTimeMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                
                // Check if NEXT line is word timing metadata
                var words: List<com.suvojeet.suvmusic.data.model.LyricsWord>? = null
                if (i + 1 < rawLines.size) {
                    val nextLine = rawLines[i + 1].trim()
                    val wordMatch = wordTimingPattern.find(nextLine)
                    if (wordMatch != null) {
                        try {
                            val content = wordMatch.groupValues[1]
                            val wordParts = content.split("|")
                            words = wordParts.mapNotNull { part ->
                                val p = part.split(":")
                                if (p.size >= 3) {
                                    val wText = p[0]
                                    val wStart = (p[1].toDoubleOrNull() ?: 0.0) * 1000
                                    val wEnd = (p[2].toDoubleOrNull() ?: 0.0) * 1000
                                    com.suvojeet.suvmusic.data.model.LyricsWord(
                                        text = wText,
                                        startTimeMs = wStart.toLong(),
                                        endTimeMs = wEnd.toLong()
                                    )
                                } else null
                            }
                            i++ // Skip the timing line since we consumed it
                        } catch (e: Exception) {
                            // Ignore parsing errors for words
                        }
                    }
                }

                if (text.isNotBlank()) {
                    lines.add(
                        LyricsLine(
                            text = text,
                            startTimeMs = startTimeMs,
                            words = words
                        )
                    )
                }
            }
            i++
        }
        
        // Calculate end times based on next line's start time
        for (j in lines.indices) {
            if (j < lines.lastIndex) {
                lines[j] = lines[j].copy(endTimeMs = lines[j + 1].startTimeMs)
            } else {
                lines[j] = lines[j].copy(endTimeMs = lines[j].startTimeMs + 5000)
            }
        }
        
        return lines
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
    
    companion object {
        private const val MAX_CACHE_SIZE = 50
    }
}

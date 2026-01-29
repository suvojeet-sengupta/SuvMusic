package com.suvojeet.suvmusic.data.repository

import android.content.Context
import android.util.LruCache
import com.google.gson.JsonParser
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import com.suvojeet.suvmusic.providers.lyrics.LyricsLine
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.providers.lyrics.BetterLyricsProvider
import com.suvojeet.suvmusic.providers.lyrics.LyricsProvider
import com.suvojeet.suvmusic.simpmusic.SimpMusicLyricsProvider
import com.suvojeet.suvmusic.kugou.KuGouLyricsProvider
import com.suvojeet.suvmusic.lrclib.LrcLibLyricsProvider
import com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import com.suvojeet.suvmusic.util.encodeUrl
import com.suvojeet.suvmusic.data.SessionManager

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
    private val simpMusicLyricsProvider: SimpMusicLyricsProvider,
    private val kuGouLyricsProvider: KuGouLyricsProvider,
    private val lrcLibLyricsProvider: LrcLibLyricsProvider,
    private val sessionManager: SessionManager
) {
    private val cache = LruCache<String, Lyrics>(MAX_CACHE_SIZE)

    /**
     * Ordered list of lyrics providers
     */
    private suspend fun getLyricsProviders(): List<LyricsProvider> {
        val preferred = sessionManager.getPreferredLyricsProvider()
        val providers = mutableListOf<LyricsProvider>()
        
        if (sessionManager.doesEnableBetterLyrics()) providers.add(betterLyricsProvider)
        if (sessionManager.doesEnableSimpMusic()) providers.add(simpMusicLyricsProvider)
        if (sessionManager.doesEnableKuGou()) providers.add(kuGouLyricsProvider)
        
        // Reorder: put preferred at top
        val preferredProvider = providers.find { 
            when (it) {
                betterLyricsProvider -> preferred == "BetterLyrics"
                simpMusicLyricsProvider -> preferred == "SimpMusic"
                kuGouLyricsProvider -> preferred == "Kugou"
                else -> false
            }
        }
        
        if (preferredProvider != null) {
            providers.remove(preferredProvider)
            providers.add(0, preferredProvider)
        }
        
        return providers
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
                        val providerEnum = when (provider) {
                            betterLyricsProvider -> LyricsProviderType.BETTER_LYRICS
                            kuGouLyricsProvider -> LyricsProviderType.KUGOU
                            simpMusicLyricsProvider -> LyricsProviderType.SIMP_MUSIC
                            else -> LyricsProviderType.AUTO
                        }
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
        val lrcLibLyrics = fetchExternalLyrics(lrcLibLyricsProvider, song, LyricsProviderType.LRCLIB)
        if (lrcLibLyrics != null) {
            // Check if it is synced or plain
             // fetchExternalLyrics assumes synced if parseLrcLyrics succeeds. 
             // If parseLrcLyrics fails or empty, it returns null.
             // But lrcLib might return plain text which parseLrcLyrics might treat as empty or single line?
             // parseLrcLyrics looks for timestamps.
             
             // If we got a result from fetchExternalLyrics, it means it parsed as LRC (synced).
             cache.put(song.id, lrcLibLyrics)
             return@withContext lrcLibLyrics
        }
        
        // If fetchExternalLyrics returned null, maybe it was plain text?
        // Let's try to get plain text from LRCLIB manually if needed, 
        // OR modify fetchExternalLyrics to handle plain text.
        // For now, let's assume if LrcLib returns something, we want it.
        
        // 3. Fallback: Get lyrics from the original source (JioSaavn/YouTube)
        val sourceLyrics = fetchFromSource(song)
        
        if (sourceLyrics != null) {
            cache.put(song.id, sourceLyrics)
            return@withContext sourceLyrics
        }
        
        // 4. Last resort: Try LRCLIB plain lyrics
        // Since step 2 only accepted synced, we can try to fetch again and accept plain?
        // Actually, fetchExternalLyrics implementation below only accepts parsed LRC.
        // Let's manually call provider and handle plain text.
        try {
            val result = lrcLibLyricsProvider.getLyrics(
                id = song.id,
                title = song.title,
                artist = song.artist,
                duration = (song.duration / 1000).toInt(),
                album = song.album
            )
            val text = result.getOrNull()
            if (!text.isNullOrBlank()) {
                 val lines = text.split("\n").map { LyricsLine(text = it.trim()) }
                 val lyrics = Lyrics(
                    lines = lines,
                    sourceCredit = "Lyrics from LRCLIB",
                    isSynced = false,
                    provider = LyricsProviderType.LRCLIB
                )
                cache.put(song.id, lyrics)
                return@withContext lyrics
            }
        } catch (e: Exception) { } // Ignore exceptions here, as it's a last resort
        
        null
    }
    
    // Fetch from a specific provider
    private suspend fun fetchFromProvider(song: Song, providerType: LyricsProviderType): Lyrics? {
        return when (providerType) {
            LyricsProviderType.BETTER_LYRICS -> {
                if (sessionManager.doesEnableBetterLyrics()) {
                    fetchExternalLyrics(betterLyricsProvider, song, LyricsProviderType.BETTER_LYRICS)
                } else null
            }
            LyricsProviderType.SIMP_MUSIC -> {
                if (sessionManager.doesEnableSimpMusic()) {
                    fetchExternalLyrics(simpMusicLyricsProvider, song, LyricsProviderType.SIMP_MUSIC)
                } else null
            }
            LyricsProviderType.KUGOU -> {
                if (sessionManager.doesEnableKuGou()) {
                    fetchExternalLyrics(kuGouLyricsProvider, song, LyricsProviderType.KUGOU)
                } else null
            }
            LyricsProviderType.LRCLIB -> {
                // Try synced first
                fetchExternalLyrics(lrcLibLyricsProvider, song, LyricsProviderType.LRCLIB) ?: run {
                    // Fallback to plain if synced failed but text exists
                    lrcLibLyricsProvider.getLyrics(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        duration = (song.duration / 1000).toInt(),
                        album = song.album
                    ).map { text ->
                        val lines = text.split("\n").map { LyricsLine(text = it.trim()) }
                        Lyrics(lines = lines, sourceCredit = "Lyrics from LRCLIB", isSynced = false, provider = LyricsProviderType.LRCLIB)
                    }.getOrNull()
                }
            }
            LyricsProviderType.JIOSAAVN -> {
                jioSaavnRepository.getLyricsFromJioSaavn(song.id)?.let { text ->
                    val lines = text.split("\n").map { LyricsLine(text = it.trim()) }
                    Lyrics(lines = lines, sourceCredit = "Lyrics from JioSaavn", isSynced = false, provider = LyricsProviderType.JIOSAAVN)
                }
            }
            LyricsProviderType.YOUTUBE -> {
                try {
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
                var words: List<com.suvojeet.suvmusic.providers.lyrics.LyricsWord>? = null
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
                                    com.suvojeet.suvmusic.providers.lyrics.LyricsWord(
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
    
    companion object {
        private const val MAX_CACHE_SIZE = 50
    }
}
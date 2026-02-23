package com.suvojeet.suvmusic.data.repository

import android.content.Context
import android.util.LruCache
import com.google.gson.JsonParser
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import com.suvojeet.suvmusic.providers.lyrics.LyricsLine
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
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
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val betterLyricsProvider: BetterLyricsProvider,
    private val simpMusicLyricsProvider: SimpMusicLyricsProvider,
    private val kuGouLyricsProvider: KuGouLyricsProvider,
    private val lrcLibLyricsProvider: LrcLibLyricsProvider,
    private val localLyricsProvider: com.suvojeet.suvmusic.providers.lyrics.LocalLyricsProvider,
    private val sessionManager: SessionManager
) {
    private val cache = LruCache<String, Lyrics>(MAX_CACHE_SIZE)

    /**
     * Cache key helper
     */
    private fun getCacheKey(songId: String, provider: LyricsProviderType): String {
        return "${songId}_${provider.name}"
    }

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
        val cacheKey = getCacheKey(song.id, providerType)
        
        // Check cache first
        val cached = cache.get(cacheKey)
        if (cached != null) {
            return@withContext cached
        }
        
        // If specific provider requested, fetch directly
        if (providerType != LyricsProviderType.AUTO) {
            return@withContext fetchFromProvider(song, providerType)
        }
        
        // AUTO Mode: Priority Order
        
        // 0. Check Local Lyrics (Highest Priority)
        val localLyricsText = localLyricsProvider.getLyrics(song)
        if (localLyricsText != null) {
            val lines = parseLrcLyrics(localLyricsText)
            val lyrics = Lyrics(
                lines = lines,
                sourceCredit = "Local File",
                isSynced = lines.any { it.startTimeMs > 0 },
                provider = LyricsProviderType.LOCAL
            )
            cache.put(getCacheKey(song.id, LyricsProviderType.AUTO), lyrics)
            cache.put(getCacheKey(song.id, LyricsProviderType.LOCAL), lyrics)
            return@withContext lyrics
        }
        
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
                        // Cache for AUTO and specific provider
                        cache.put(getCacheKey(song.id, LyricsProviderType.AUTO), lyrics)
                        cache.put(getCacheKey(song.id, providerEnum), lyrics)
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
             cache.put(getCacheKey(song.id, LyricsProviderType.AUTO), lrcLibLyrics)
             cache.put(getCacheKey(song.id, LyricsProviderType.LRCLIB), lrcLibLyrics)
             return@withContext lrcLibLyrics
        }
        
        // 3. Fallback: Get lyrics from the original source (JioSaavn/YouTube)
        val sourceLyrics = fetchFromSource(song)
        if (sourceLyrics != null) {
            cache.put(getCacheKey(song.id, LyricsProviderType.AUTO), sourceLyrics)
            cache.put(getCacheKey(song.id, sourceLyrics.provider), sourceLyrics)
            return@withContext sourceLyrics
        }
        
        // 4. Last resort: Try LRCLIB plain lyrics
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
                cache.put(getCacheKey(song.id, LyricsProviderType.AUTO), lyrics)
                cache.put(getCacheKey(song.id, LyricsProviderType.LRCLIB), lyrics)
                return@withContext lyrics
            }
        } catch (e: Exception) { }
        
        null
    }
    
    // Fetch from a specific provider
    private suspend fun fetchFromProvider(song: Song, providerType: LyricsProviderType): Lyrics? {
        val result = when (providerType) {
            LyricsProviderType.LOCAL -> {
                localLyricsProvider.getLyrics(song)?.let { text ->
                    val lines = parseLrcLyrics(text)
                    Lyrics(
                        lines = lines,
                        sourceCredit = "Local File",
                        isSynced = lines.any { it.startTimeMs > 0 },
                        provider = LyricsProviderType.LOCAL
                    )
                }
            }
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
        
        if (result != null) {
            cache.put(getCacheKey(song.id, providerType), result)
        }
        
        return result
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
        return com.suvojeet.suvmusic.util.LyricsUtils.parseLyrics(lrcContent)
    }

    companion object {
        private const val MAX_CACHE_SIZE = 50
    }
}
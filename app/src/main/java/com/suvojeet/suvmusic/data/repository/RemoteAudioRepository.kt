package com.suvojeet.suvmusic.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64
import com.suvojeet.suvmusic.util.encodeUrl
import com.suvojeet.suvmusic.util.decodeHtml
import com.suvojeet.suvmusic.util.toHighResImage
import com.suvojeet.suvmusic.data.repository.remote.RemoteConstants
import com.suvojeet.suvmusic.core.model.AppResult
import com.suvojeet.suvmusic.core.model.getOrDefault
import com.suvojeet.suvmusic.data.error.toAppError
import com.suvojeet.suvmusic.telemetry.Telemetry
import com.suvojeet.suvmusic.cache.OfflineCache

/** Repository for fetching tracks from the remote HQ audio backend. */
@Singleton
class RemoteAudioRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val apiService: com.suvojeet.suvmusic.data.repository.remote.RemoteAudioApiService
) {
    // In-memory caches to reduce server load and improve performance.
    // Bounded (LRU) so a long session can't leak memory by growing forever; the
    // least-recently-used entry is evicted past the cap. Thread-safe because these
    // are touched from multiple Dispatchers.IO coroutines concurrently.
    private fun <V> boundedCache(maxSize: Int): MutableMap<String, V> =
        java.util.Collections.synchronizedMap(
            object : LinkedHashMap<String, V>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, V>): Boolean = size > maxSize
            }
        )

    private val searchCache = boundedCache<List<Song>>(80)
    private val songDetailsCache = boundedCache<Song>(300)
    private val streamUrlCache = boundedCache<String>(300)
    private val playlistCache = boundedCache<Playlist>(60)

    // Per-call timeout for raw HTTP requests. Shares the connection pool/dispatcher
    // with the injected client (newBuilder is cheap) but caps total call time so a
    // stalled legacy endpoint can't hang a resolve/prefetch indefinitely.
    private val boundedHttpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .callTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /**
     * Drop any cached stream URL / song details for [songId] so the next resolve
     * re-fetches from the backend. Called by the player when a stream URL dies
     * (HTTP 403/410) mid-playback — without this the cache would keep handing back
     * the same dead URL and playback could never recover.
     */
    fun invalidate(songId: String) {
        // Both caches dropped under the same lock so a concurrent reader can't
        // observe a half-invalidated state (stream URL gone but stale details, or
        // vice versa).
        synchronized(streamUrlCache) {
            streamUrlCache.keys
                .filter { it == songId || it.startsWith("${songId}_") }
                .toList()
                .forEach { streamUrlCache.remove(it) }
            songDetailsCache.remove(songId)
        }
    }

    // --- 429 rate-limit backoff (shared across ALL RemoteAudio network calls) ---
    // The backend rate-limits aggressively, and the app used to fire many calls at once
    // (and retry failed ones every few seconds), which only deepened the throttle. When
    // we see a 429 we pause every RemoteAudio *network* call for a growing cooldown;
    // reads still hit the in-memory / disk caches. One 429 → all callers back off
    // together instead of hammering.
    @Volatile private var rateLimitedUntilMs = 0L
    @Volatile private var rateLimitStreak = 0

    private fun isRateLimited(): Boolean = System.currentTimeMillis() < rateLimitedUntilMs

    @Synchronized
    private fun noteRateLimited() {
        rateLimitStreak = (rateLimitStreak + 1).coerceAtMost(6)
        // 15s, 30s, 60s, 120s, 240s … capped at 5 min
        val backoff = (15_000L shl (rateLimitStreak - 1)).coerceAtMost(5 * 60_000L)
        rateLimitedUntilMs = System.currentTimeMillis() + backoff
        android.util.Log.w("RemoteAudio", "BACKOFF: 429 — pausing all RemoteAudio network for ${backoff / 1000}s (streak=$rateLimitStreak)")
    }

    @Synchronized
    private fun noteSuccess() {
        if (rateLimitStreak != 0 || rateLimitedUntilMs != 0L) {
            rateLimitStreak = 0
            rateLimitedUntilMs = 0L
        }
    }

    /** True when [e] is a Retrofit HTTP 429 (rate limited). */
    private fun is429(e: Throwable): Boolean =
        e::class.qualifiedName == "retrofit2.HttpException" &&
            runCatching { e.javaClass.getMethod("code").invoke(e) as Int }.getOrNull() == 429

    companion object {
        private val API_BASE_URL get() = RemoteConstants.API_BASE_URL
        private val BASE_URL get() = RemoteConstants.LEGACY_BASE_URL
        private val DES_KEY get() = RemoteConstants.DES_KEY

        private const val QUALITY_96 = "_96.mp4"
        private const val QUALITY_160 = "_160.mp4"
        private const val QUALITY_320 = "_320.mp4"
    }
    
    /**
     * Search for songs on RemoteAudio.
     *
     * Legacy nullable-free entry point kept for existing callers; it collapses a typed
     * failure back to an empty list. Prefer [searchResult] in new code so the UI can tell
     * "no results" apart from "offline"/"upstream error".
     */
    suspend fun search(query: String): List<Song> = searchResult(query).getOrDefault(emptyList())

    /**
     * Typed variant of [search]: returns [AppResult.Success] (possibly with an empty list
     * when the backend genuinely had nothing) or [AppResult.Failure] with a classified
     * [com.suvojeet.suvmusic.core.model.AppError]. Failures are also reported to telemetry.
     */
    suspend fun searchResult(query: String): AppResult<List<Song>> = withContext(Dispatchers.IO) {
        val cacheKey = query.trim().lowercase()
        searchCache[cacheKey]?.let { cached ->
            android.util.Log.i("RemoteAudio", "search('$query') CACHE_HIT n=${cached.size}")
            return@withContext AppResult.Success(cached)
        }

        // Don't touch the network while backing off from a 429 — serve disk cache if any.
        if (isRateLimited()) {
            val stale = OfflineCache.getSearch("ra:$cacheKey")
            android.util.Log.w("RemoteAudio", "search('$query') SKIP backoff active; ${if (stale != null) "serving ${stale.size} cached" else "no cache"}")
            return@withContext if (stale != null) {
                searchCache[cacheKey] = stale
                AppResult.Success(stale)
            } else {
                AppResult.Failure(com.suvojeet.suvmusic.core.model.AppError.RateLimited("backoff"))
            }
        }

        val started = System.currentTimeMillis()
        try {
            android.util.Log.i("RemoteAudio", "search('$query') api=searchSongs start")
            val response = apiService.searchSongs(query)
            noteSuccess()
            // `/search/songs` returns a flat `data.results`; the global `/search`
            // endpoint nests them under `data.songs.results`. Accept either shape.
            val rawResults = response.data?.results ?: response.data?.songs?.results
            val rawCount = rawResults?.size ?: 0
            val songs = rawResults?.mapNotNull { parseSongDto(it) } ?: emptyList()
            val ms = System.currentTimeMillis() - started

            android.util.Log.i("RemoteAudio", "search('$query') OK in ${ms}ms raw=$rawCount parsed=${songs.size}")
            if (rawCount > 0 && songs.isEmpty()) {
                android.util.Log.w("RemoteAudio", "search('$query') had $rawCount raw results but all failed parseSongDto — backend schema may have shifted")
                // Raw rows arrived but none parsed → the backend schema likely shifted.
                Telemetry.report("search", "remoteaudio", com.suvojeet.suvmusic.core.model.AppError.Parse("$rawCount raw, 0 parsed"))
            }

            if (songs.isNotEmpty()) {
                searchCache[cacheKey] = songs
                // Also cache individual song details
                songs.forEach { song: Song -> songDetailsCache[song.id] = song }
                // Persist to disk so a cold start / offline launch can still show results.
                OfflineCache.putSearch("ra:$cacheKey", songs)
            }
            AppResult.Success(songs)
        } catch (e: Exception) {
            val ms = System.currentTimeMillis() - started
            val error = e.toAppError()
            if (error is com.suvojeet.suvmusic.core.model.AppError.RateLimited) noteRateLimited()
            // Name the classified error (RateLimited/Timeout/NoNetwork/…) so a 429 storm
            // is obvious in logcat and distinguishable from a parse or network failure.
            android.util.Log.e("RemoteAudio", "search('$query') FAIL in ${ms}ms ${e.javaClass.simpleName}: ${e.message} classified=${error::class.simpleName}", e)
            Telemetry.report("search", "remoteaudio", error, mapOf("qlen" to query.length.toString()))
            // Offline-first fallback: serve last-known results from disk rather than a
            // blank screen. The failure is still recorded above for telemetry.
            val stale = OfflineCache.getSearch("ra:$cacheKey")
            if (stale != null) {
                android.util.Log.i("RemoteAudio", "search('$query') FALLBACK serving ${stale.size} STALE cached results (after ${error::class.simpleName})")
                searchCache[cacheKey] = stale
                AppResult.Success(stale)
            } else {
                android.util.Log.w("RemoteAudio", "search('$query') FALLBACK no cached results -> returning Failure(${error::class.simpleName})")
                AppResult.Failure(error)
            }
        }
    }
    
    /**
     * Search for artists on RemoteAudio (Legacy fallback for rich profiles).
     */
    suspend fun searchArtists(query: String): List<com.suvojeet.suvmusic.core.model.Artist> = withContext(Dispatchers.IO) {
        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", "searchArtists('$query') SKIP backoff active")
            return@withContext emptyList()
        }
        try {
            val url = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_SEARCH_ARTIST}&_format=json&n=5&q=${query.encodeUrl()}"
            val response = makeRequest(url)

            val json = JsonParser.parseString(response).asJsonObject
            val results = json.getAsJsonArray("results") ?: return@withContext emptyList()

            results.mapNotNull { element ->
                val artist = element.asJsonObject
                val id = artist.get("id")?.asString ?: return@mapNotNull null
                val name = artist.get("name")?.asString ?: artist.get("title")?.asString ?: ""
                val image = artist.get("image")?.asString?.toHighResImage()
                
                com.suvojeet.suvmusic.core.model.Artist(
                    id = id,
                    name = name.decodeHtml(),
                    thumbnailUrl = image
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("RemoteAudio", "searchArtists('$query') failed: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get detailed artist profile (Legacy internal API).
     */
    suspend fun getArtist(artistId: String): com.suvojeet.suvmusic.core.model.Artist? = withContext(Dispatchers.IO) {
        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", "getArtist($artistId) SKIP backoff active")
            return@withContext null
        }
        try {
            val url = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_WEBAPI_GET}&token=$artistId&type=artist&p=1&n_song=20&n_album=20&sub_type=songs&category=&sort_order=&includeMetaTags=0&ctx=web6dot0&api_version=4&_format=json&_marker=0"
            val response = makeRequest(url)
            val json = JsonParser.parseString(response).asJsonObject
            
            val name = json.get("name")?.asString ?: "Unknown Artist"
            val image = json.get("image")?.asString?.toHighResImage()
            val fans = json.get("fan_count")?.asString
            val description = json.get("wiki")?.asString?.decodeHtml()
            
            // Songs
            val topSongs = json.getAsJsonArray("topSongs")?.mapNotNull { parseSong(it.asJsonObject) } ?: emptyList()
            
            // Albums
            val topAlbums = json.getAsJsonArray("topAlbums")?.mapNotNull { element ->
                val obj = element.asJsonObject
                val albumId = obj.get("id")?.asString ?: return@mapNotNull null
                val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                val albumImage = obj.get("image")?.asString?.toHighResImage()
                val year = obj.get("year")?.asString
                
                com.suvojeet.suvmusic.core.model.Album(albumId, title.decodeHtml(), name.decodeHtml(), albumImage, year)
            } ?: emptyList()
            
            com.suvojeet.suvmusic.core.model.Artist(
                id = artistId,
                name = name.decodeHtml(),
                thumbnailUrl = image,
                description = description,
                subscribers = if (fans != null) "$fans Fans" else null,
                songs = topSongs,
                albums = topAlbums
            )
        } catch (e: Exception) {
            android.util.Log.e("RemoteAudio", "getArtist($artistId) failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
    
    /**
     * Get song details by ID.
     */
    suspend fun getSongDetails(songId: String): Song? = withContext(Dispatchers.IO) {
        if (songDetailsCache.containsKey(songId)) {
            android.util.Log.i("RemoteAudio", "getSongDetails($songId) CACHE_HIT")
            return@withContext songDetailsCache[songId]
        }

        // Skip while backing off — prevents the player from re-hitting a 429 every few
        // seconds while resolving a stream URL.
        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", "getSongDetails($songId) SKIP backoff active")
            return@withContext null
        }

        val started = System.currentTimeMillis()
        try {
            android.util.Log.i("RemoteAudio", "getSongDetails($songId) api=getSongDetails start")
            val response = apiService.getSongDetails(songId)
            noteSuccess()
            val rawCount = response.data?.size ?: 0
            val dto = response.data?.firstOrNull()
            val downloadCount = dto?.downloadUrl?.size ?: 0
            val result = dto?.let { parseSongDto(it) }
            val ms = System.currentTimeMillis() - started

            if (result == null) {
                android.util.Log.e(
                    "RemoteAudio",
                    "getSongDetails($songId) FAIL_PARSE in ${ms}ms success=${response.success} raw=$rawCount downloadUrls=$downloadCount",
                )
            } else {
                android.util.Log.i(
                    "RemoteAudio",
                    "getSongDetails($songId) OK in ${ms}ms title='${result.title}' downloadUrls=$downloadCount streamUrl=${if (result.streamUrl.isNullOrBlank()) "BLANK" else "ok"}",
                )
            }

            result?.let { songDetailsCache[songId] = it }
            result
        } catch (e: Exception) {
            if (is429(e)) noteRateLimited()
            val ms = System.currentTimeMillis() - started
            android.util.Log.e("RemoteAudio", "getSongDetails($songId) FAIL in ${ms}ms ${e.javaClass.simpleName}: ${e.message}", e)
            Telemetry.report("song.details", "remoteaudio", e.toAppError(), mapOf("id" to songId))
            null
        }
    }
    
    /**
     * Get stream URL for a song (320kbps).
     */
    suspend fun getStreamUrl(songId: String, quality: Int = 320): String? = withContext(Dispatchers.IO) {
        val cacheKey = "${songId}_$quality"
        streamUrlCache[cacheKey]?.let {
            android.util.Log.i("RemoteAudio", ">> getStreamUrl ENTER vid=$songId q=$quality CACHE_HIT")
            return@withContext it
        }

        // Don't even attempt while backing off from a 429 — the player would just
        // re-trigger the throttle and stall waiting for a URL that won't come.
        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", ">> getStreamUrl SKIP vid=$songId q=$quality backoff active")
            return@withContext null
        }

        android.util.Log.i("RemoteAudio", ">> getStreamUrl ENTER vid=$songId q=$quality")
        // Retry transient failures (network blip / null) up to 2 attempts so a single
        // hiccup doesn't stop playback. Parity with the YouTube path, which already retries.
        val maxAttempts = 2
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            val started = System.currentTimeMillis()
            try {
                val song = getSongDetails(songId)
                val streamUrl = song?.streamUrl
                val ms = System.currentTimeMillis() - started

                if (streamUrl.isNullOrBlank()) {
                    android.util.Log.e("RemoteAudio", "<< getStreamUrl vid=$songId NULL (attempt ${attempt + 1}/$maxAttempts, details=${if (song == null) "null" else "ok but no streamUrl"}) in ${ms}ms")
                    // A genuinely blank stream URL won't change on retry — bail out.
                    if (song != null) return@withContext null
                } else {
                    android.util.Log.i("RemoteAudio", "<< getStreamUrl EXIT vid=$songId ok(${streamUrl.take(80)}) in ${ms}ms")
                    streamUrlCache[cacheKey] = streamUrl
                    return@withContext streamUrl
                }
            } catch (e: Exception) {
                lastError = e
                if (is429(e)) { noteRateLimited(); return@withContext null }
                val ms = System.currentTimeMillis() - started
                android.util.Log.e("RemoteAudio", "<< getStreamUrl vid=$songId FAIL (attempt ${attempt + 1}/$maxAttempts) in ${ms}ms ${e.javaClass.simpleName}: ${e.message}")
            }
            if (attempt < maxAttempts - 1) kotlinx.coroutines.delay(800)
        }
        lastError?.let { Telemetry.report("stream.resolve", "remoteaudio", it.toAppError(), mapOf("id" to songId)) }
        null
    }

    // ==================== Radio / Related / Recommendations ====================

    private val relatedCache = boundedCache<List<Song>>(80)

    /**
     * Get songs related to [songId] using RemoteAudio's recommendation endpoint.
     * This powers native autoplay/radio when RemoteAudio is the active source.
     */
    suspend fun getRelatedSongs(songId: String): List<Song> = withContext(Dispatchers.IO) {
        relatedCache[songId]?.let { return@withContext it }

        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", "getRelatedSongs($songId) SKIP backoff active")
            return@withContext emptyList()
        }

        try {
            val response = apiService.getSongSuggestions(songId)
            noteSuccess()
            val songs = response.data?.mapNotNull { parseSongDto(it) } ?: emptyList()

            if (songs.isNotEmpty()) {
                relatedCache[songId] = songs
                songs.forEach { songDetailsCache[it.id] = it }
            }
            songs
        } catch (e: Exception) {
            if (is429(e)) noteRateLimited()
            android.util.Log.e("RemoteAudio", "getRelatedSongs($songId) failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Build a pool of recommended songs seeded from the user's recent RemoteAudio plays.
     * Falls back to trending/home content when no seeds are available, so the
     * home screen and queue always have RemoteAudio material to work with.
     */
    suspend fun getRecommendations(seedSongIds: List<String>): List<Song> = withContext(Dispatchers.IO) {
        val pool = mutableListOf<Song>()
        try {
            for (seed in seedSongIds.take(5)) {
                pool.addAll(getRelatedSongs(seed))
                if (pool.size >= 40) break
            }
        } catch (e: Exception) {
            android.util.Log.w("RemoteAudio", "getRecommendations seed pass failed: ${e.message}")
        }

        // Fallback: pull songs out of the home/discover sections if seeds were dry.
        if (pool.isEmpty()) {
            try {
                getHomeSections().forEach { section ->
                    section.items.forEach { item ->
                        if (item is com.suvojeet.suvmusic.core.model.HomeItem.SongItem) pool.add(item.song)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("RemoteAudio", "getRecommendations home fallback failed: ${e.message}")
            }
        }

        pool.distinctBy { it.id }
    }

    /**
     * Get top songs for an artist (used for artist radio on RemoteAudio).
     */
    suspend fun getArtistTopSongs(artistId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            getArtist(artistId)?.songs ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("RemoteAudio", "getArtistTopSongs($artistId) failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get playlist details with all songs.
     */
    suspend fun getPlaylist(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        playlistCache[playlistId]?.let { return@withContext it }

        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", "getPlaylist($playlistId) SKIP backoff active")
            return@withContext null
        }

        try {
            val response = apiService.getPlaylist(playlistId)
            noteSuccess()
            val data = response.data

            // In some wrapper versions, playlist info might be in 'playlists' or direct 'name/id' fields
            val playlistObj = data?.playlists?.results?.firstOrNull()
            val title = playlistObj?.name ?: "" 
            val image = playlistObj?.image?.lastOrNull()?.url
            
            // Surface schema drift: empty-string/empty-list defaults above otherwise
            // mask a backend response shape change as a benign "empty playlist".
            if (data?.songs?.results == null) {
                android.util.Log.w("RemoteAudio", "getPlaylist($playlistId): no songs.results in response — possible schema drift")
            }

            val songs = data?.songs?.results?.mapNotNull { parseSongDto(it) } ?: emptyList()

            if (songs.isEmpty()) return@withContext null

            val playlist = Playlist(
                id = playlistId,
                title = title.decodeHtml().ifBlank { "Playlist" },
                author = "",
                thumbnailUrl = image,
                songs = songs
            )

            playlistCache[playlistId] = playlist
            playlist
        } catch (e: Exception) {
            if (is429(e)) noteRateLimited()
            android.util.Log.e("RemoteAudio", "getPlaylist($playlistId) failed: ${e.message}")
            null
        }
    }

    /**
     * Get album details with all songs.
     */
    suspend fun getAlbum(albumId: String): Playlist? = withContext(Dispatchers.IO) {
        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", "getAlbum($albumId) SKIP backoff active")
            return@withContext null
        }
        try {
            val response = apiService.getAlbumDetails(albumId)
            noteSuccess()
            val songs = response.data?.mapNotNull { parseSongDto(it) } ?: emptyList()
            
            if (songs.isEmpty()) return@withContext null
            
            val firstSong = response.data?.firstOrNull() ?: return@withContext null
            val albumName = firstSong.album?.name ?: "Album"
            val image = firstSong.image?.lastOrNull()?.url
            val artist = firstSong.artists?.primary?.joinToString { it.name ?: "" } ?: ""

            Playlist(
                id = albumId,
                title = albumName.decodeHtml(),
                author = artist.decodeHtml(),
                thumbnailUrl = image,
                songs = songs
            )
        } catch (e: Exception) {
            if (is429(e)) noteRateLimited()
            android.util.Log.e("RemoteAudio", "getAlbum($albumId) failed: ${e.message}")
            null
        }
    }
    /**
     * Get plain lyrics from RemoteAudio (internal fallback).
     */
    /**
     * Get plain lyrics from RemoteAudio (internal fallback).
     */
    suspend fun getLyricsFromRemote(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_LYRICS}&_format=json&lyrics_id=$songId"
            val response = makeRequest(url)
            
            val json = JsonParser.parseString(response).asJsonObject
            json.get("lyrics")?.asString
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get lyrics for a song (legacy function - now returns plain text).
     */
    suspend fun getLyrics(songId: String): String? = getLyricsFromRemote(songId)
    
    /**
     * Get home sections with dynamic content from RemoteAudio Launch Data.
     * This provides a "For You" experience with Trending, Charts, New Releases, and customized modules.
     */
    suspend fun getHomeSections(): List<com.suvojeet.suvmusic.core.model.HomeSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<com.suvojeet.suvmusic.core.model.HomeSection>()
        
        try {
            // Use the main launch data endpoint for dynamic homepage structure
            val launchUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_LAUNCH_DATA}&api_version=4&_format=json&_marker=0"
            val response = makeRequest(launchUrl)
            val json = JsonParser.parseString(response).asJsonObject
            
            // Helper to parse a list of items based on their type
            fun parseHomeItems(jsonArray: com.google.gson.JsonArray): List<com.suvojeet.suvmusic.core.model.HomeItem> {
                return jsonArray.mapNotNull { element ->
                    val obj = element.asJsonObject
                    val type = obj.get("type")?.asString ?: ""
                    
                    when (type) {
                        "song" -> {
                            parseSong(obj)?.let { com.suvojeet.suvmusic.core.model.HomeItem.SongItem(it) }
                        }
                        "album" -> {
                            val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                            val image = obj.get("image")?.asString?.toHighResImage()
                            val artist = obj.get("primary_artists")?.asString ?: obj.get("music")?.asString ?: ""
                            val year = obj.get("year")?.asString
                            
                            com.suvojeet.suvmusic.core.model.HomeItem.AlbumItem(
                                com.suvojeet.suvmusic.core.model.Album(id, title.decodeHtml(), artist.decodeHtml(), image, year)
                            )
                        }
                        "playlist" -> {
                            val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                            val image = obj.get("image")?.asString?.toHighResImage()
                            val count = obj.get("song_count")?.asInt ?: obj.get("count")?.asInt ?: 0
                            
                            com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                                com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(id, title.decodeHtml(), "", "Featured", image, count)
                            )
                        }
                        "chart" -> {
                            val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: "Chart"
                            val image = obj.get("image")?.asString?.toHighResImage()
                            val count = obj.get("count")?.asInt ?: 0
                            
                            com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                                com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(id, title.decodeHtml(), "", "Top Charts", image, count)
                            )
                        }
                        "radio_station" -> {
                            val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: "Radio"
                            val image = obj.get("image")?.asString?.toHighResImage()
                            
                            com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                                com.suvojeet.suvmusic.core.model.PlaylistDisplayItem("radio_$id", title.decodeHtml(), "", "Radio", image, 0)
                            )
                        }
                        "artist" -> {
                             val id = obj.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                            val image = obj.get("image")?.asString?.toHighResImage()
                             com.suvojeet.suvmusic.core.model.HomeItem.ArtistItem(
                                com.suvojeet.suvmusic.core.model.Artist(id, title.decodeHtml(), image)
                            )
                        }
                        else -> null
                    }
                }
            }

            // 1. New Trending
            if (json.has("new_trending")) {
                val trendingList = json.getAsJsonArray("new_trending")
                val items = parseHomeItems(trendingList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Trending Now 🔥", items))
                }
            }
            
            // 2. Top Charts
            if (json.has("charts")) {
                val chartsList = json.getAsJsonArray("charts")
                val items = parseHomeItems(chartsList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Top Charts 📊", items))
                }
            }
            
            // 3. New Albums
            if (json.has("new_albums")) {
                val albumsList = json.getAsJsonArray("new_albums")
                val items = parseHomeItems(albumsList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("New Releases 🆕", items))
                }
            }
            
            // 4. Browse Discover (Dynamic Sections)
            if (json.has("browse_discover")) {
                val discoverList = json.getAsJsonArray("browse_discover")
                discoverList.forEach { element ->
                    val sectionObj = element.asJsonObject
                    val title = sectionObj.get("title")?.asString
                    val data = sectionObj.getAsJsonArray("data")
                    
                    if (!title.isNullOrBlank() && data != null && data.size() > 0) {
                        val items = parseHomeItems(data)
                        if (items.isNotEmpty()) {
                            sections.add(com.suvojeet.suvmusic.core.model.HomeSection(title.decodeHtml(), items))
                        }
                    }
                }
            }
            
            // 5. Radio
            if (json.has("radio")) {
                val radioList = json.getAsJsonArray("radio")
                val items = parseHomeItems(radioList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Radio Stations 📻", items))
                }
            }
            
            // 6. Top Playlists (Global)
            if (json.has("top_playlists")) {
                val plList = json.getAsJsonArray("top_playlists")
                val items = parseHomeItems(plList)
                if (items.isNotEmpty()) {
                    sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Featured Playlists 🎧", items))
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("RemoteAudio", "Error fetching dynamic home sections", e)
        }

        // Always merge the static catalogue on top of whatever dynamic gave us.
        // Launch data is region-dependent and often returns only 1–2 sections,
        // which leaves the home screen feeling empty. Dedupe by title so a
        // section appearing in both sources isn't shown twice.
        try {
            val staticSections = fetchStaticHomeSections()
            if (staticSections.isNotEmpty()) {
                val existingTitles = sections.map { it.title }.toMutableSet()
                for (s in staticSections) {
                    if (existingTitles.add(s.title)) sections.add(s)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("RemoteAudio", "Static home merge failed: ${e.message}")
        }

        sections
    }

    /**
     * Fallback: Get home sections with manual fetches if Launch Data fails.
     */
    private suspend fun fetchStaticHomeSections(): List<com.suvojeet.suvmusic.core.model.HomeSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<com.suvojeet.suvmusic.core.model.HomeSection>()
        
        try {
            // 1. Top Charts / Trending - Using content.getCharts
            try {
                val chartsUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_CHARTS}&_format=json"
                val chartsResponse = makeRequest(chartsUrl)
                val chartsJson = JsonParser.parseString(chartsResponse)
                
                if (chartsJson.isJsonArray && chartsJson.asJsonArray.size() > 0) {
                    val chartItems = chartsJson.asJsonArray.take(12).mapNotNull { element ->
                        val chartObj = element.asJsonObject
                        val chartId = chartObj.get("id")?.asString 
                            ?: chartObj.get("listid")?.asString ?: return@mapNotNull null
                        val title = chartObj.get("title")?.asString 
                            ?: chartObj.get("listname")?.asString ?: "Chart"
                        val image = chartObj.get("image")?.asString?.toHighResImage()
                        val songCount = chartObj.get("count")?.asInt ?: chartObj.get("songs_count")?.asInt ?: 0
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                            com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                                id = chartId,
                                name = title.decodeHtml(),
                                url = "",
                                uploaderName = "Top Charts",
                                thumbnailUrl = image,
                                songCount = songCount
                            )
                        )
                    }
                    if (chartItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Top Charts 📊", chartItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("RemoteAudio", "Charts fetch error", e) }
            
            // 2. New Releases - Using content.getAlbums with filter
            try {
                val newReleasesUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_ALBUMS}&_format=json&n=20&p=1&type=latest"
                val releaseResponse = makeRequest(newReleasesUrl)
                val releaseJson = JsonParser.parseString(releaseResponse)
                
                val albumsList = if (releaseJson.isJsonObject && releaseJson.asJsonObject.has("data")) {
                    releaseJson.asJsonObject.getAsJsonArray("data")
                } else if (releaseJson.isJsonArray) {
                    releaseJson.asJsonArray
                } else {
                    null
                }
                
                if (albumsList != null && albumsList.size() > 0) {
                    val albumItems = albumsList.take(12).mapNotNull { albumElement ->
                        val albumObj = albumElement.asJsonObject
                        val albumId = albumObj.get("id")?.asString 
                            ?: albumObj.get("albumid")?.asString ?: return@mapNotNull null
                        val title = albumObj.get("title")?.asString 
                            ?: albumObj.get("name")?.asString ?: "Album"
                        val artist = albumObj.get("primary_artists")?.asString 
                            ?: albumObj.get("music")?.asString ?: ""
                        val image = albumObj.get("image")?.asString?.toHighResImage()
                        val year = albumObj.get("year")?.asString
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.AlbumItem(
                            com.suvojeet.suvmusic.core.model.Album(
                                id = albumId,
                                title = title.decodeHtml(),
                                artist = artist.decodeHtml(),
                                thumbnailUrl = image,
                                year = year
                            )
                        )
                    }
                    if (albumItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("New Releases 🆕", albumItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("RemoteAudio", "New releases fetch error", e) }
            
            // 3. Trending Songs - Using content.getTrending
            try {
                val trendingUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_TRENDING}&type=song&_format=json&n=20"
                val trendingResponse = makeRequest(trendingUrl)
                val trendingJson = JsonParser.parseString(trendingResponse)
                
                val songsList = if (trendingJson.isJsonObject && trendingJson.asJsonObject.has("data")) {
                    trendingJson.asJsonObject.getAsJsonArray("data")
                } else if (trendingJson.isJsonArray) {
                    trendingJson.asJsonArray
                } else {
                    null
                }
                
                if (songsList != null && songsList.size() > 0) {
                    val songItems = songsList.take(12).mapNotNull { songElement ->
                        val song = parseSong(songElement.asJsonObject) ?: return@mapNotNull null
                        com.suvojeet.suvmusic.core.model.HomeItem.SongItem(song)
                    }
                    if (songItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Trending Now 🔥", songItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("RemoteAudio", "Trending songs fetch error", e) }
            
            // 4. Top Playlists - Using content.getFeaturedPlaylists
            try {
                val playlistsUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_FEATURED_PL}&_format=json&n=20&p=1"
                val playlistsResponse = makeRequest(playlistsUrl)
                val playlistsJson = JsonParser.parseString(playlistsResponse)
                
                val plList = if (playlistsJson.isJsonObject && playlistsJson.asJsonObject.has("data")) {
                    playlistsJson.asJsonObject.getAsJsonArray("data")
                } else if (playlistsJson.isJsonObject && playlistsJson.asJsonObject.has("featured_playlists")) {
                    playlistsJson.asJsonObject.getAsJsonArray("featured_playlists")
                } else if (playlistsJson.isJsonArray) {
                    playlistsJson.asJsonArray
                } else {
                    null
                }
                
                if (plList != null && plList.size() > 0) {
                    val playlistItems = plList.take(12).mapNotNull { plElement ->
                        val plObj = plElement.asJsonObject
                        val plId = plObj.get("id")?.asString 
                            ?: plObj.get("listid")?.asString ?: return@mapNotNull null
                        val name = plObj.get("title")?.asString 
                            ?: plObj.get("listname")?.asString ?: "Playlist"
                        val image = plObj.get("image")?.asString?.toHighResImage()
                        val songCount = plObj.get("count")?.asInt ?: plObj.get("songs_count")?.asInt ?: 0
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                            com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                                id = plId,
                                name = name.decodeHtml(),
                                url = "",
                                uploaderName = "Featured",
                                thumbnailUrl = image,
                                songCount = songCount
                            )
                        )
                    }
                    if (playlistItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Featured Playlists 🎧", playlistItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("RemoteAudio", "Featured playlists fetch error", e) }
            
            // 5. Top Artists - Using content.getArtists (trending artists)
            try {
                val artistsUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_SEARCH_ARTIST}&_format=json&q=top+indian+artists&n=15"
                val artistsResponse = makeRequest(artistsUrl)
                val artistsJson = JsonParser.parseString(artistsResponse)
                
                val artistResults = if (artistsJson.isJsonObject && artistsJson.asJsonObject.has("results")) {
                    artistsJson.asJsonObject.getAsJsonArray("results")
                } else {
                    null
                }
                
                if (artistResults != null && artistResults.size() > 0) {
                    val artistItems = artistResults.take(10).mapNotNull { artistElement ->
                        val artistObj = artistElement.asJsonObject
                        val artistId = artistObj.get("id")?.asString ?: return@mapNotNull null
                        val name = artistObj.get("name")?.asString 
                            ?: artistObj.get("title")?.asString ?: "Artist"
                        val image = artistObj.get("image")?.asString?.toHighResImage()
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.ArtistItem(
                            com.suvojeet.suvmusic.core.model.Artist(
                                id = artistId,
                                name = name.decodeHtml(),
                                thumbnailUrl = image
                            )
                        )
                    }
                    if (artistItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Top Artists 🎤", artistItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("RemoteAudio", "Artists fetch error", e) }
            
            // 6. Trending Albums - Using content.getTrending with type=album
            try {
                val trendingAlbumsUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_TRENDING}&type=album&_format=json&n=15"
                val albumsResponse = makeRequest(trendingAlbumsUrl)
                val albumsJson = JsonParser.parseString(albumsResponse)
                
                val albumArray = if (albumsJson.isJsonObject && albumsJson.asJsonObject.has("data")) {
                    albumsJson.asJsonObject.getAsJsonArray("data")
                } else if (albumsJson.isJsonArray) {
                    albumsJson.asJsonArray
                } else {
                    null
                }
                
                if (albumArray != null && albumArray.size() > 0) {
                    val albumItems = albumArray.take(10).mapNotNull { albumElement ->
                        val albumObj = albumElement.asJsonObject
                        val albumId = albumObj.get("id")?.asString 
                            ?: albumObj.get("albumid")?.asString ?: return@mapNotNull null
                        val title = albumObj.get("title")?.asString 
                            ?: albumObj.get("name")?.asString ?: "Album"
                        val artist = albumObj.get("primary_artists")?.asString 
                            ?: albumObj.get("music")?.asString ?: ""
                        val image = albumObj.get("image")?.asString?.toHighResImage()
                        val year = albumObj.get("year")?.asString
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.AlbumItem(
                            com.suvojeet.suvmusic.core.model.Album(
                                id = albumId,
                                title = title.decodeHtml(),
                                artist = artist.decodeHtml(),
                                thumbnailUrl = image,
                                year = year
                            )
                        )
                    }
                    if (albumItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Popular Albums 💿", albumItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("RemoteAudio", "Trending albums fetch error", e) }
            
            // 7. Editorial Picks / Radio Stations - Using content.getRadioStations
            try {
                val radioUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_RADIO_STATIONS}&_format=json&n=15"
                val radioResponse = makeRequest(radioUrl)
                val radioJson = JsonParser.parseString(radioResponse)
                
                val radioList = if (radioJson.isJsonObject && radioJson.asJsonObject.has("data")) {
                    radioJson.asJsonObject.getAsJsonArray("data")
                } else if (radioJson.isJsonArray) {
                    radioJson.asJsonArray
                } else {
                    null
                }
                
                if (radioList != null && radioList.size() > 0) {
                    val radioItems = radioList.take(8).mapNotNull { radioElement ->
                        val radioObj = radioElement.asJsonObject
                        val radioId = (radioObj.get("id")?.asString 
                            ?: radioObj.get("stationid")?.asString)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val name = radioObj.get("name")?.asString 
                            ?: radioObj.get("title")?.asString ?: "Radio"
                        val image = radioObj.get("image")?.asString?.toHighResImage()
                        
                        com.suvojeet.suvmusic.core.model.HomeItem.PlaylistItem(
                            com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                                id = "radio_$radioId",
                                name = name.decodeHtml(),
                                url = "",
                                uploaderName = "Radio",
                                thumbnailUrl = image,
                                songCount = 0
                            )
                        )
                    }
                    if (radioItems.isNotEmpty()) {
                        sections.add(com.suvojeet.suvmusic.core.model.HomeSection("Radio Stations 📻", radioItems))
                    }
                }
            } catch (e: Exception) { android.util.Log.e("RemoteAudio", "Radio stations fetch error", e) }
            
        } catch (e: Exception) {
            android.util.Log.e("RemoteAudio", "Home sections error", e)
        }
        
        sections
    }
    
    /**
     * Get featured/trending playlists from RemoteAudio for the library.
     */
    suspend fun getFeaturedPlaylists(): List<com.suvojeet.suvmusic.core.model.PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<com.suvojeet.suvmusic.core.model.PlaylistDisplayItem>()

        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", "getFeaturedPlaylists SKIP backoff active")
            return@withContext playlists
        }

        try {
            // Fetch top charts/featured playlists
            val playlistsUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_CHARTS}&_format=json&n=20"
            val response = makeRequest(playlistsUrl)
            
            // Note: getCharts returns an array directly or inside "results" depending on endpoint version
            // But usually content.getCharts returns a list of playlists
            val json = JsonParser.parseString(response)
            
            val results = if (json.isJsonArray) {
                json.asJsonArray
            } else if (json.isJsonObject && json.asJsonObject.has("results")) {
                json.asJsonObject.getAsJsonArray("results")
            } else {
                null
            }
            
            results?.forEach { element ->
                val plObj = element.asJsonObject
                val plId = plObj.get("id")?.asString ?: plObj.get("listid")?.asString
                val name = plObj.get("title")?.asString ?: plObj.get("listname")?.asString ?: "Playlist"
                val image = plObj.get("image")?.asString?.toHighResImage()
                val songCount = plObj.get("count")?.asInt ?: 0
                
                if (plId != null) {
                    playlists.add(
                        com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                            id = plId,
                            name = name.decodeHtml(),
                            url = "",
                            uploaderName = "Featured",
                            thumbnailUrl = image,
                            songCount = songCount
                        )
                    )
                }
            }
            
            // Also add some specific search results if charts are empty or few
            if (playlists.size < 5) {
                val searchUrl = "$BASE_URL?${RemoteConstants.PARAM_CALL}${RemoteConstants.EP_SEARCH_PL}&_format=json&q=top+hits&n=10"
                val searchResponse = makeRequest(searchUrl)
                val searchJson = JsonParser.parseString(searchResponse).asJsonObject
                val searchResults = searchJson.getAsJsonArray("results")
                
                searchResults?.forEach { element ->
                    val plObj = element.asJsonObject
                    val plId = plObj.get("id")?.asString ?: return@forEach
                    // Avoid duplicates
                    if (playlists.none { it.id == plId }) {
                        val name = plObj.get("title")?.asString ?: "Playlist"
                        val image = plObj.get("image")?.asString?.toHighResImage()
                        val songCount = plObj.get("count")?.asInt ?: 0
                        
                        playlists.add(
                            com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                                id = plId,
                                name = name.decodeHtml(),
                                url = "",
                                uploaderName = "Featured",
                                thumbnailUrl = image,
                                songCount = songCount
                            )
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("RemoteAudio", "getFeaturedPlaylists failed: ${e.javaClass.simpleName}: ${e.message}")
        }

        playlists
    }
    
    // ==================== Private Helpers ====================
    
    private fun makeRequest(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36")
            .addHeader("Referer", RemoteConstants.REFERER)
            .addHeader("Origin", RemoteConstants.ORIGIN)
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .build()

        return boundedHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                // Legacy endpoints go through this raw path (not Retrofit), so a 429 here
                // never reached the typed is429() check — arm the shared backoff directly
                // so these callers stop hammering a rate-limited backend too.
                if (response.code == 429) noteRateLimited()
                android.util.Log.e("RemoteAudio", "HTTP ${response.code} for ${request.url.host}${request.url.encodedPath} (body ${body.length} chars)")
            } else {
                noteSuccess()
                android.util.Log.d("RemoteAudio", "HTTP ${response.code} ${request.url.host} — ${body.length} chars")
            }
            body
        }
    }
    
    private fun parseSongDto(dto: com.suvojeet.suvmusic.data.repository.remote.RemoteAudioSongDto): Song? {
        return try {
            val downloadUrls = dto.downloadUrl ?: emptyList()
            val streamUrl = downloadUrls
                .firstOrNull { it.quality == "320kbps" }?.url
                ?: downloadUrls.lastOrNull()?.url

            val metadata = com.suvojeet.suvmusic.core.model.RemoteAudioMetadata(
                label = dto.label?.decodeHtml(),
                playCount = dto.playCount,
                language = dto.language,
                explicitContent = dto.explicitContent,
                copyright = dto.copyright?.decodeHtml(),
                hasLyrics = dto.hasLyrics,
                year = dto.year,
                releaseDate = dto.releaseDate,
                artists = dto.artists?.all?.mapNotNull { artist ->
                    val name = artist.name ?: return@mapNotNull null
                    com.suvojeet.suvmusic.core.model.ArtistCreditInfo(
                        name = name.decodeHtml(),
                        role = (artist.role ?: "Artist").replace("_", " ").split(" ").joinToString(" ") { it.capitalize() },
                        thumbnailUrl = artist.image?.lastOrNull()?.url,
                        artistId = artist.id
                    )
                } ?: emptyList()
            )

            Song.fromRemoteAudio(
                songId = dto.id ?: return null,
                title = (dto.name ?: "Unknown").decodeHtml(),
                artist = dto.artists?.primary?.joinToString { it.name ?: "" }?.decodeHtml() ?: "Unknown Artist",
                album = dto.album?.name?.decodeHtml() ?: "",
                duration = (dto.duration ?: 0L) * 1000,
                thumbnailUrl = dto.image?.lastOrNull()?.url,
                streamUrl = streamUrl,
                releaseDate = dto.releaseDate,
                remoteAudioMetadata = metadata
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSong(json: JsonObject): Song? {
        return try {
            // Check if it's the new wrapper format (has 'success' and 'data')
            if (json.has("success") && json.has("data")) {
                val data = json.get("data")
                return if (data.isJsonArray) {
                    val first = data.asJsonArray.firstOrNull()?.asJsonObject
                    first?.let { parseSongJsonObject(it) }
                } else if (data.isJsonObject) {
                    parseSongJsonObject(data.asJsonObject)
                } else null
            }

            // Fallback for internal API format
            parseSongInternal(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSongJsonObject(json: JsonObject): Song? {
        return try {
            val id = json.get("id")?.asString ?: return null
            val title = json.get("name")?.asString ?: json.get("song")?.asString ?: "Unknown"
            
            val albumObj = if (json.has("album") && json.get("album").isJsonObject) {
                json.getAsJsonObject("album")
            } else null
            val albumName = albumObj?.get("name")?.asString ?: json.get("album")?.asString ?: ""
            
            val duration = (json.get("duration")?.asLong ?: 0L) * 1000
            
            val images = json.getAsJsonArray("image")
            val thumbnailUrl = if (images != null && images.size() > 0) {
                images.last().asJsonObject.get("url")?.asString
            } else json.get("image")?.asString?.toHighResImage()
            
            val releaseDate = json.get("releaseDate")?.asString ?: json.get("release_date")?.asString
            
            val artistsObj = if (json.has("artists") && json.get("artists").isJsonObject) {
                json.getAsJsonObject("artists")
            } else null
            
            val primaryArtists = artistsObj?.getAsJsonArray("primary")?.map { it.asJsonObject.get("name")?.asString ?: "" }
                ?: json.get("primary_artists")?.asString?.split(",")?.map { it.trim() }
                ?: emptyList()
            
            val artistName = primaryArtists.joinToString(", ").ifBlank { "Unknown Artist" }
            
            val allArtists = artistsObj?.getAsJsonArray("all")?.mapNotNull { element ->
                val obj = element.asJsonObject
                val name = obj.get("name")?.asString ?: return@mapNotNull null
                val role = obj.get("role")?.asString ?: "Artist"
                val artistId = obj.get("id")?.asString
                val artistImage = obj.getAsJsonArray("image")?.lastOrNull()?.asJsonObject?.get("url")?.asString
                
                com.suvojeet.suvmusic.core.model.ArtistCreditInfo(
                    name = name.decodeHtml(),
                    role = role.replace("_", " ").split(" ").joinToString(" ") { it.capitalize() },
                    thumbnailUrl = artistImage,
                    artistId = artistId
                )
            } ?: emptyList()

            val streamUrl = json.getAsJsonArray("downloadUrl")?.lastOrNull { 
                it.asJsonObject.get("quality")?.asString == "320kbps" 
            }?.asJsonObject?.get("url")?.asString ?: json.getAsJsonArray("downloadUrl")?.lastOrNull()?.asJsonObject?.get("url")?.asString

            val metadata = com.suvojeet.suvmusic.core.model.RemoteAudioMetadata(
                label = json.get("label")?.asString?.decodeHtml(),
                playCount = json.get("playCount")?.asLong,
                language = json.get("language")?.asString,
                explicitContent = json.get("explicitContent")?.asBoolean,
                copyright = json.get("copyright")?.asString?.decodeHtml(),
                hasLyrics = json.get("hasLyrics")?.asBoolean,
                year = json.get("year")?.asString,
                releaseDate = releaseDate,
                artists = allArtists
            )

            Song.fromRemoteAudio(
                songId = id,
                title = title.decodeHtml(),
                artist = artistName.decodeHtml(),
                album = albumName.decodeHtml(),
                duration = duration,
                thumbnailUrl = thumbnailUrl,
                streamUrl = streamUrl,
                releaseDate = releaseDate,
                remoteAudioMetadata = metadata
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSongInternal(json: JsonObject): Song? {
        return try {
            val id = (json.get("id")?.asString 
                ?: json.get("perma_url")?.asString?.substringAfterLast("/"))
                ?.takeIf { it.isNotBlank() }
                ?: return null
            
            val moreInfo = if (json.has("more_info") && json.get("more_info").isJsonObject) {
                json.getAsJsonObject("more_info")
            } else null

            val title = json.get("song")?.asString ?: json.get("title")?.asString ?: json.get("name")?.asString ?: "Unknown"
            val artistsJson = moreInfo?.get("primary_artists")?.asString ?: json.get("primary_artists")?.asString ?: "Unknown Artist"
            val album = moreInfo?.get("album")?.asString ?: json.get("album")?.asString ?: ""
            val duration = (moreInfo?.get("duration")?.asString?.toLongOrNull() ?: json.get("duration")?.asString?.toLongOrNull() ?: 0L) * 1000
            val image = json.get("image")?.asString?.toHighResImage()
            val releaseDate = json.get("release_date")?.asString ?: moreInfo?.get("release_date")?.asString ?: json.get("year")?.asString

            Song.fromRemoteAudio(
                songId = id,
                title = title.decodeHtml(),
                artist = artistsJson.decodeHtml(),
                album = album.decodeHtml(),
                duration = duration,
                thumbnailUrl = image,
                releaseDate = releaseDate
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    
    /**
     * Decrypt RemoteAudio's encrypted media URL.
     *
     * Note: the algorithm (DES-ECB) is dictated by RemoteAudio's server format and
     * is not something we control. The defensive posture here is to fail hard
     * on decrypt errors and return null, rather than silently returning the
     * encrypted blob which the caller would then attempt to use as a URL.
     */
    private fun decryptUrl(encryptedUrl: String): String? {
        return try {
            val keySpec = DESKeySpec(DES_KEY.toByteArray())
            val keyFactory = SecretKeyFactory.getInstance("DES")
            val secretKey = keyFactory.generateSecret(keySpec)

            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decoded = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)

            String(decrypted)
        } catch (e: Exception) {
            android.util.Log.w("RemoteAudioRepository", "decryptUrl failed: ${e.javaClass.simpleName}")
            null
        }
    }
    
    // Extension functions moved to util/Extensions.kt
    
    /**
     * Comprehensive search for songs, albums, artists, and playlists.
     * Uses autocomplete endpoint for best "instant search" results.
     */
    suspend fun searchAll(query: String): SearchResults = withContext(Dispatchers.IO) {
        // While backing off, route through search() which serves cache / a typed failure
        // without piling more requests onto a throttled host.
        if (isRateLimited()) {
            android.util.Log.w("RemoteAudio", "searchAll('$query') SKIP backoff active -> cache/search fallback")
            return@withContext SearchResults(songs = search(query))
        }
        try {
            val response = apiService.searchAll(query)
            noteSuccess()
            val data = response.data

            // Parse Songs
            var songs = data?.songs?.results?.mapNotNull { parseSongDto(it) } ?: emptyList()
            if (songs.isEmpty()) {
                android.util.Log.w("RemoteAudio", "searchAll('$query'): autocomplete returned no songs, falling back to search.getResults")
                songs = search(query)
            }

            // Parse Albums
            val albums = data?.albums?.results?.mapNotNull { dto ->
                val id = dto.id ?: return@mapNotNull null
                com.suvojeet.suvmusic.core.model.Album(
                    id = id,
                    title = (dto.name ?: "Album").decodeHtml(),
                    artist = "", // Album DTO in new API might not have direct artist string
                    thumbnailUrl = dto.image?.lastOrNull()?.url,
                    year = dto.year
                )
            } ?: emptyList()
            
            // Parse Artists
            val artists = data?.artists?.results?.mapNotNull { dto ->
                com.suvojeet.suvmusic.core.model.Artist(
                    id = dto.id ?: "",
                    name = (dto.name ?: "Artist").decodeHtml(),
                    thumbnailUrl = dto.image?.lastOrNull()?.url
                )
            } ?: emptyList()

            // Parse Playlists
            val playlists = data?.playlists?.results?.mapNotNull { dto ->
                val id = dto.id ?: return@mapNotNull null
                Playlist(
                    id = id,
                    title = (dto.name ?: "Playlist").decodeHtml(),
                    author = "",
                    thumbnailUrl = dto.image?.lastOrNull()?.url,
                    songs = emptyList()
                )
            } ?: emptyList()
            
            return@withContext SearchResults(songs, albums, artists, playlists)

        } catch (e: Exception) {
            if (is429(e)) noteRateLimited()
            android.util.Log.e("RemoteAudio", "searchAll('$query') failed: ${e.message} — falling back to search.getResults")
            // Even if autocomplete fails entirely, still try to return songs so the tab isn't empty.
            return@withContext SearchResults(songs = search(query))
        }
    }
}

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<com.suvojeet.suvmusic.core.model.Album> = emptyList(),
    val artists: List<com.suvojeet.suvmusic.core.model.Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

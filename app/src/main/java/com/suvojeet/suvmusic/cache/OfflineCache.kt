package com.suvojeet.suvmusic.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import java.io.File

/**
 * Disk-backed cache for network results so a cold start (or an offline launch) can show
 * last-known data instead of a blank screen.
 *
 * The home feed and quick picks are already persisted by `SessionManager`; the gap this
 * fills is **search results**, which previously lived only in an in-memory map in
 * `RemoteAudioRepository` and vanished on process death. Stored as Gson JSON files keyed
 * by a sanitized query — [Song] is a plain data class that round-trips losslessly through
 * Gson (unlike the manual, lossy `songToJson` used elsewhere).
 *
 * Installed as a process-wide holder (see `SuvMusicApplication.onCreate`) for the same
 * reason as `Telemetry`: the call sites are scattered across repositories whose DI graph
 * we don't want to perturb, and a cache is a cross-cutting concern.
 */
object OfflineCache {
    private const val TAG = "OfflineCache"

    /** How long a cached entry is considered "fresh". Stale entries are still returned
     *  when [getSearch] is called with `allowStale = true` (the offline fallback path). */
    private const val FRESH_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

    /** Hard cap so the cache directory can't grow without bound. */
    private const val MAX_ENTRIES = 80

    private val gson = Gson()

    @Volatile
    private var searchDir: File? = null

    /** Artist/album/playlist search results (song search predates this dir). */
    @Volatile
    private var listsDir: File? = null

    /**
     * Wrapper carrying the save time so freshness can be judged on read.
     *
     * The `@SerializedName` annotations are load-bearing, not cosmetic: this app builds
     * with `android.enableR8.fullMode=true`, where `-keepattributes Signature` only
     * retains generic signatures for *kept* members. Gson's bundled consumer ProGuard
     * rules keep `@SerializedName`-annotated fields together with their `Signature`, so
     * without these annotations R8 strips the `List<Song>` type argument from [songs],
     * and Gson then deserializes each element as a raw `LinkedTreeMap` instead of a
     * [Song]. That malformed list flows through the offline fallback into the search
     * grid, whose `key = { song.id }` does a `checkcast Song` → `ClassCastException:
     * LinkedTreeMap cannot be cast to Song` (the v2.5.1.0 crash under saavn 429s).
     */
    private data class Envelope(
        @SerializedName("savedAt") val savedAt: Long,
        @SerializedName("songs") val songs: List<Song>
    )

    fun init(context: Context) {
        searchDir = File(context.filesDir, "offline_search").apply {
            runCatching { mkdirs() }
        }
        listsDir = File(context.filesDir, "offline_lists").apply {
            runCatching { mkdirs() }
        }
    }

    /** A filesystem-safe, collision-resistant file name for a query. */
    private fun fileFor(dir: File, query: String): File {
        val key = query.trim().lowercase()
        // Hash keeps the name short and safe; length suffix lowers collision odds further.
        val name = "${key.hashCode()}_${key.length}.json"
        return File(dir, name)
    }

    /** Persist a non-empty result set for [query]. No-op before [init] or for empty lists. */
    fun putSearch(query: String, songs: List<Song>, now: Long = System.currentTimeMillis()) {
        if (songs.isEmpty()) {
            android.util.Log.i(TAG, "putSearch('$query') SKIP empty list")
            return
        }
        val dir = searchDir ?: run {
            android.util.Log.w(TAG, "putSearch('$query') SKIP cache not initialized")
            return
        }
        try {
            val file = fileFor(dir, query)
            val json = gson.toJson(Envelope(now, songs))
            file.writeText(json)
            android.util.Log.i(TAG, "putSearch('$query') WROTE n=${songs.size} bytes=${json.length} -> ${file.name}")
            pruneIfNeeded(dir)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "putSearch('$query') failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Returns cached songs for [query], or null if absent.
     * When [allowStale] is false, entries older than [FRESH_TTL_MS] are treated as absent.
     */
    fun getSearch(query: String, allowStale: Boolean = true, now: Long = System.currentTimeMillis()): List<Song>? {
        val dir = searchDir ?: run {
            android.util.Log.w(TAG, "getSearch('$query') MISS cache not initialized")
            return null
        }
        return try {
            val file = fileFor(dir, query)
            if (!file.exists()) {
                android.util.Log.i(TAG, "getSearch('$query') MISS no file (${file.name})")
                return null
            }
            val envelope = gson.fromJson(file.readText(), Envelope::class.java) ?: run {
                android.util.Log.w(TAG, "getSearch('$query') MISS null envelope (empty/corrupt file) -> deleting")
                runCatching { file.delete() }
                return null
            }
            val ageMs = now - envelope.savedAt
            val ageMin = ageMs / 60_000
            if (!allowStale && ageMs > FRESH_TTL_MS) {
                android.util.Log.i(TAG, "getSearch('$query') MISS stale-disallowed age=${ageMin}min (ttl=${FRESH_TTL_MS / 60_000}min)")
                return null
            }
            // Gson can populate a declared-non-null field with null (it bypasses Kotlin
            // null checks), and an entry written by an older build may not map cleanly.
            // Inspect as List<*> so the validation itself doesn't trip the per-element
            // checkcast, then treat anything that isn't a real, non-empty Song list as a
            // cache miss so a malformed payload can never reach the UI.
            val raw: List<*>? = envelope.songs
            // Diagnostic: surface the actual runtime element type. If R8 ever strips the
            // List<Song> generic again, this prints the LinkedTreeMap that caused the
            // v2.5.1.0 ClassCastException — without it ever reaching the grid.
            val firstType = raw?.firstOrNull()?.javaClass?.name ?: "none"
            if (raw.isNullOrEmpty() || raw.any { it !is Song }) {
                android.util.Log.e(
                    TAG,
                    "getSearch('$query') CORRUPT payload dropped: n=${raw?.size ?: 0} firstElementType=$firstType " +
                        "age=${ageMin}min (expected com.suvojeet...Song; non-Song means Gson lost the generic) -> deleting"
                )
                runCatching { file.delete() }
                return null
            }
            android.util.Log.i(TAG, "getSearch('$query') HIT n=${raw.size} age=${ageMin}min stale=${ageMs > FRESH_TTL_MS}")
            @Suppress("UNCHECKED_CAST")
            (raw as List<Song>)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "getSearch('$query') failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    // ── Artist / album / playlist search results ─────────────────────────────
    //
    // Same idea as the Song search cache, with one concrete envelope class per
    // element type. Concrete (non-generic-at-the-callsite) envelopes with
    // @SerializedName fields are what keeps the element type argument alive
    // under R8 full mode — see the Envelope doc above for the failure mode.

    private data class ArtistEnvelope(
        @SerializedName("savedAt") val savedAt: Long,
        @SerializedName("items") val items: List<Artist>?
    )

    private data class AlbumEnvelope(
        @SerializedName("savedAt") val savedAt: Long,
        @SerializedName("items") val items: List<Album>?
    )

    private data class PlaylistEnvelope(
        @SerializedName("savedAt") val savedAt: Long,
        @SerializedName("items") val items: List<Playlist>?
    )

    fun putArtists(query: String, artists: List<Artist>, now: Long = System.currentTimeMillis()) =
        putList("artists", query, artists.size) { gson.toJson(ArtistEnvelope(now, artists)) }

    fun getArtists(query: String, now: Long = System.currentTimeMillis()): List<Artist>? =
        getList("artists", query, now) { json ->
            gson.fromJson(json, ArtistEnvelope::class.java)?.let { it.savedAt to it.items }
        }

    fun putAlbums(query: String, albums: List<Album>, now: Long = System.currentTimeMillis()) =
        putList("albums", query, albums.size) { gson.toJson(AlbumEnvelope(now, albums)) }

    fun getAlbums(query: String, now: Long = System.currentTimeMillis()): List<Album>? =
        getList("albums", query, now) { json ->
            gson.fromJson(json, AlbumEnvelope::class.java)?.let { it.savedAt to it.items }
        }

    fun putPlaylists(query: String, playlists: List<Playlist>, now: Long = System.currentTimeMillis()) =
        putList("playlists", query, playlists.size) { gson.toJson(PlaylistEnvelope(now, playlists)) }

    fun getPlaylists(query: String, now: Long = System.currentTimeMillis()): List<Playlist>? =
        getList("playlists", query, now) { json ->
            gson.fromJson(json, PlaylistEnvelope::class.java)?.let { it.savedAt to it.items }
        }

    private fun putList(kind: String, query: String, count: Int, toJson: () -> String) {
        if (count == 0) return
        val dir = listsDir ?: return
        try {
            val file = fileFor(dir, "$kind:$query")
            file.writeText(toJson())
            android.util.Log.i(TAG, "putList($kind, '$query') WROTE n=$count")
            pruneIfNeeded(dir)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "putList($kind, '$query') failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Reads a cached list, tolerating corrupt or absent entries. Like [getSearch],
     * stale entries ARE returned — every caller is an offline/error fallback where
     * old results beat a blank screen. Malformed payloads are deleted on sight.
     * [parse] returns (savedAt, items) or null for an unreadable envelope.
     */
    private inline fun <reified T> getList(
        kind: String,
        query: String,
        now: Long,
        parse: (String) -> Pair<Long, List<T>?>?,
    ): List<T>? {
        val dir = listsDir ?: return null
        return try {
            val file = fileFor(dir, "$kind:$query")
            if (!file.exists()) return null
            val parsed = parse(file.readText()) ?: run {
                runCatching { file.delete() }
                return null
            }
            val (savedAt, items) = parsed
            val raw: List<*>? = items
            if (raw.isNullOrEmpty() || raw.any { it !is T }) {
                android.util.Log.e(TAG, "getList($kind, '$query') CORRUPT payload dropped -> deleting")
                runCatching { file.delete() }
                return null
            }
            android.util.Log.i(TAG, "getList($kind, '$query') HIT n=${raw.size} age=${(now - savedAt) / 60_000}min")
            @Suppress("UNCHECKED_CAST")
            raw as List<T>
        } catch (e: Exception) {
            android.util.Log.w(TAG, "getList($kind, '$query') failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    /** Evict the oldest files once the entry count exceeds [MAX_ENTRIES]. */
    private fun pruneIfNeeded(dir: File) {
        val files = dir.listFiles() ?: return
        if (files.size <= MAX_ENTRIES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - MAX_ENTRIES)
            .forEach { runCatching { it.delete() } }
    }
}

package com.suvojeet.suvmusic.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
        if (songs.isEmpty()) return
        val dir = searchDir ?: return
        try {
            val file = fileFor(dir, query)
            file.writeText(gson.toJson(Envelope(now, songs)))
            pruneIfNeeded(dir)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "putSearch('$query') failed: ${e.message}")
        }
    }

    /**
     * Returns cached songs for [query], or null if absent.
     * When [allowStale] is false, entries older than [FRESH_TTL_MS] are treated as absent.
     */
    fun getSearch(query: String, allowStale: Boolean = true, now: Long = System.currentTimeMillis()): List<Song>? {
        val dir = searchDir ?: return null
        return try {
            val file = fileFor(dir, query)
            if (!file.exists()) return null
            val envelope = gson.fromJson(file.readText(), Envelope::class.java) ?: return null
            val ageMs = now - envelope.savedAt
            if (!allowStale && ageMs > FRESH_TTL_MS) return null
            // Gson can populate a declared-non-null field with null (it bypasses Kotlin
            // null checks), and an entry written by an older build may not map cleanly.
            // Inspect as List<*> so the validation itself doesn't trip the per-element
            // checkcast, then treat anything that isn't a real, non-empty Song list as a
            // cache miss so a malformed payload can never reach the UI.
            val raw: List<*>? = envelope.songs
            if (raw.isNullOrEmpty() || raw.any { it !is Song }) {
                runCatching { file.delete() }
                return null
            }
            @Suppress("UNCHECKED_CAST")
            (raw as List<Song>)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "getSearch('$query') failed: ${e.message}")
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

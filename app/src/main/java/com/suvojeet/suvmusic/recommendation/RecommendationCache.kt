package com.suvojeet.suvmusic.recommendation

import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.model.HomeSection
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache for recommendation results to avoid redundant network calls.
 * Respects TTL and provides invalidation hooks.
 */
@Singleton
class RecommendationCache @Inject constructor() {

    companion object {
        /** Default TTL: 15 minutes */
        private const val DEFAULT_TTL_MS = 15 * 60 * 1000L
        /** Short TTL for volatile data like "up next": 5 minutes */
        private const val SHORT_TTL_MS = 5 * 60 * 1000L
    }

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val ttlMs: Long = DEFAULT_TTL_MS
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttlMs
    }

    // --- Song list caches ---
    private val songCaches = ConcurrentHashMap<String, CacheEntry<List<Song>>>()
    
    // --- Home section caches ---
    private val sectionCaches = ConcurrentHashMap<String, CacheEntry<List<HomeSection>>>()

    // Keys
    object Keys {
        const val QUICK_PICKS = "quick_picks"
        const val PERSONALIZED_MIX = "personalized_mix"
        const val BASED_ON_RECENT = "based_on_recent"
        const val DISCOVERY_MIX = "discovery_mix"
        const val ARTIST_MIX = "artist_mix"
        const val UP_NEXT = "up_next"
        const val HOME_SECTIONS = "home_sections_personalized"
        const val RELATED_PREFIX = "related_"
        const val MOOD_PREFIX = "mood_"
    }

    // --- Song List Operations ---

    fun getSongs(key: String): List<Song>? {
        val entry = songCaches[key] ?: return null
        if (entry.isExpired()) {
            songCaches.remove(key)
            return null
        }
        return entry.data
    }

    fun putSongs(key: String, songs: List<Song>, ttlMs: Long = DEFAULT_TTL_MS) {
        songCaches[key] = CacheEntry(songs, ttlMs = ttlMs)
    }

    fun getRelatedSongs(songId: String): List<Song>? =
        getSongs("${Keys.RELATED_PREFIX}$songId")

    fun putRelatedSongs(songId: String, songs: List<Song>) =
        putSongs("${Keys.RELATED_PREFIX}$songId", songs, SHORT_TTL_MS)

    // --- Section Operations ---

    fun getSections(key: String): List<HomeSection>? {
        val entry = sectionCaches[key] ?: return null
        if (entry.isExpired()) {
            sectionCaches.remove(key)
            return null
        }
        return entry.data
    }

    fun putSections(key: String, sections: List<HomeSection>, ttlMs: Long = DEFAULT_TTL_MS) {
        sectionCaches[key] = CacheEntry(sections, ttlMs = ttlMs)
    }

    // --- Invalidation ---

    /** Invalidate all caches — call when user logs in/out */
    fun invalidateAll() {
        songCaches.clear()
        sectionCaches.clear()
    }

    /** Invalidate song-related caches — call after a new listen is recorded */
    fun invalidateRecommendations() {
        songCaches.remove(Keys.QUICK_PICKS)
        songCaches.remove(Keys.PERSONALIZED_MIX)
        songCaches.remove(Keys.BASED_ON_RECENT)
        songCaches.remove(Keys.DISCOVERY_MIX)
        songCaches.remove(Keys.UP_NEXT)
        sectionCaches.remove(Keys.HOME_SECTIONS)
    }

    /** Invalidate "up next" and related only — for queue refreshes */
    fun invalidateUpNext() {
        songCaches.remove(Keys.UP_NEXT)
        // Clear all related caches
        songCaches.keys.filter { it.startsWith(Keys.RELATED_PREFIX) }.forEach { songCaches.remove(it) }
    }
}

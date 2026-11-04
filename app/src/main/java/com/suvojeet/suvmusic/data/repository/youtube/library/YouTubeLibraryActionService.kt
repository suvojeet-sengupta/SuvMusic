package com.suvojeet.suvmusic.data.repository.youtube.library

import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import com.suvojeet.suvmusic.data.repository.youtube.catalog.YouTubeCatalogService
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeApiClient
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeContentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Write actions against the user's YouTube library — ratings, subscriptions, watch history
 * — plus reading history back to seed recommendations.
 */
@Singleton
class YouTubeLibraryActionService @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiClient: YouTubeApiClient,
    private val parser: YouTubeContentParser,
    private val catalogService: YouTubeCatalogService,
    private val libraryRepository: LibraryRepository,
    private val listeningHistoryRepository: ListeningHistoryRepository
) {

    /** @param rating one of LIKE, DISLIKE, INDIFFERENT. */
    suspend fun rateSong(videoId: String, rating: String): Boolean =
        rate(rating, JSONObject().put("target", JSONObject().put("videoId", videoId)))

    /** @param rating one of LIKE, DISLIKE, INDIFFERENT. */
    suspend fun ratePlaylist(playlistId: String, rating: String): Boolean =
        rate(rating, JSONObject().put("target", JSONObject().put("playlistId", playlistId)))

    private suspend fun rate(rating: String, body: JSONObject): Boolean = withContext(Dispatchers.IO) {
        // Callers treat false as a failed like and undo it; here it should still apply locally.
        if (sessionManager.isYouTubePullOnlyEnabled()) return@withContext true
        try {
            val endpoint = when (rating) {
                "LIKE" -> "like/like"
                "DISLIKE" -> "like/dislike"
                else -> "like/removelike"
            }
            apiClient.performAuthenticatedAction(endpoint, body.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Follows or unfollows an artist. The local library is the source of truth — it works
     * signed out and offline — and the upstream YouTube subscription is best-effort on top.
     */
    suspend fun subscribe(channelId: String, isSubscribe: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isSubscribe) {
                // The local entity needs a name and artwork, which only the artist page has.
                catalogService.getArtist(channelId)?.let { libraryRepository.saveArtist(it) }
            } else {
                libraryRepository.removeArtist(channelId)
            }

            if (sessionManager.isLoggedIn()) {
                val endpoint = if (isSubscribe) "subscription/subscribe" else "subscription/unsubscribe"
                apiClient.performAuthenticatedAction(
                    endpoint,
                    """{"channelIds": ["$channelId"]}"""
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Pulls YouTube and YouTube Music history into the local database, so a freshly signed-in
     * user gets meaningful recommendations straight away instead of after days of listening.
     */
    suspend fun fetchAndSyncHistory() = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) return@withContext

        try {
            val combined = (fetchYouTubeMusicHistory() + fetchYouTubeHistory(musicOnly = true))
                .distinctBy { it.id }

            combined.forEach { song ->
                // Recorded as fully played: these are songs the user already chose to hear,
                // so they should read as strong affinity rather than skips.
                listeningHistoryRepository.recordPlay(
                    song = song,
                    durationListenedMs = song.duration,
                    wasSkipped = false
                )
            }

            android.util.Log.d(TAG, "Synced ${combined.size} songs from YouTube history")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync history", e)
        }
    }

    suspend fun fetchYouTubeMusicHistory(): List<Song> {
        val response = apiClient.fetchInternalApi("FEmusic_history")
        if (response.isEmpty()) return emptyList()
        return parser.parseSongs(response)
    }

    /** @param musicOnly drops the vlogs and talks that plain YouTube history is full of. */
    suspend fun fetchYouTubeHistory(musicOnly: Boolean = true): List<Song> {
        val response = apiClient.fetchInternalApi("FEhistory")
        if (response.isEmpty()) return emptyList()

        val songs = parser.parseSongs(response)
        if (!musicOnly) return songs

        return songs.filter { song ->
            song.artist.contains("Topic", ignoreCase = true) ||
                song.artist.contains("VEVO", ignoreCase = true) ||
                song.artist.contains("Music", ignoreCase = true) ||
                song.title.contains("Music Video", ignoreCase = true) ||
                song.title.contains("Official Video", ignoreCase = true) ||
                song.duration > 0
        }
    }

    /**
     * Reports a play to YouTube so it lands in the account's watch history and feeds back
     * into YouTube's own recommendations.
     */
    suspend fun markAsWatched(videoId: String, durationSeconds: Int = 30) = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext

            if (apiClient.reportPlaybackForHistory(videoId, durationSeconds)) {
                android.util.Log.d(TAG, "Marked as watched (history reported): $videoId")
            } else {
                android.util.Log.w(TAG, "Failed to mark as watched: $videoId")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error marking as watched", e)
        }
    }

    private companion object {
        const val TAG = "YouTubeLibraryAction"
    }
}

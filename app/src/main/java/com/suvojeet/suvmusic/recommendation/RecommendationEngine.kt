package com.suvojeet.suvmusic.recommendation

import com.suvojeet.suvmusic.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recommendation engine that analyzes listening history and generates personalized suggestions.
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val youTubeRepository: YouTubeRepository
) {
    
    /**
     * Generate personalized song recommendations based on listening history.
     * Returns empty list if insufficient data.
     */
    suspend fun getPersonalizedRecommendations(limit: Int = 20): List<Song> {
        // 1. If logged in, prioritize YouTube Music's official recommendations (FEmusic_home)
        if (youTubeRepository.isLoggedIn()) {
            try {
                val officialRecs = youTubeRepository.getRecommendations()
                if (officialRecs.isNotEmpty()) {
                    return officialRecs.take(limit)
                }
            } catch (e: Exception) {
                // Fallback to other methods
            }
        }

        // 2. Try to get recommendations from the last played song (YouTube Radio/Up Next)
        val lastPlayed = try {
            listeningHistoryDao.getRecentlyPlayed(1).first().firstOrNull()
        } catch (e: Exception) {
            null
        }
        
        if (lastPlayed != null) {
            try {
                // Use YouTube Music's native recommendation engine (Radio)
                val relatedSongs = youTubeRepository.getRelatedSongs(lastPlayed.songId)
                    .filter { it.id != lastPlayed.songId }
                
                if (relatedSongs.isNotEmpty()) {
                    return relatedSongs.take(limit)
                }
            } catch (e: Exception) {
                // Fallback
            }
        }
        
        // 3. Fallback: "My Supermix" (RTM) - Available if user has some YT Music history
        try {
            val supermix = youTubeRepository.getPlaylist("RTM")
            if (supermix.songs.isNotEmpty()) {
                return supermix.songs.take(limit)
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        // 4. Final fallback: Trending/Hits
        return try {
            youTubeRepository.search("top hits 2026", YouTubeRepository.FILTER_SONGS).take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get "Based on Recent Plays" recommendations.
     * Quick suggestions based on last few songs.
     */
    suspend fun getRecentBasedSuggestions(limit: Int = 10): List<Song> {
        val recentSongs = listeningHistoryDao.getRecentlyPlayed(1).first()
        val lastPlayed = recentSongs.firstOrNull() ?: return emptyList()
        
        return try {
            youTubeRepository.getRelatedSongs(lastPlayed.songId)
                .filter { it.id != lastPlayed.songId }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Extract potential genre/mood keywords from song title.
     * Simple heuristic to improve search relevance.
     */
    private fun extractGenreFromTitle(title: String): String {
        val keywords = listOf(
            "remix", "acoustic", "live", "cover", "official", 
            "lofi", "chill", "beats", "rap", "rock", "pop", "edm"
        )
        
        return keywords.find { title.lowercase().contains(it) } ?: ""
    }
}

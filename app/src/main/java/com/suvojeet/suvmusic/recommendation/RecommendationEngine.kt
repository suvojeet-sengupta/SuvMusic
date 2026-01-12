package com.suvojeet.suvmusic.recommendation

import com.suvojeet.suvmusic.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.data.model.Song
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
        // Try to get recommendations from the last played song (YouTube Radio/Up Next)
        val lastPlayed = listeningHistoryDao.getRecentlyPlayed(1).first().firstOrNull()
        
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
        
        // Fallback: If no history or radio failed, try "My Supermix" (RTM)
        try {
            val supermix = youTubeRepository.getPlaylist("RTM")
            if (supermix.songs.isNotEmpty()) {
                return supermix.songs.take(limit)
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        // Final fallback: Trending
        return try {
            youTubeRepository.search("trending music 2024", YouTubeRepository.FILTER_SONGS).take(limit)
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

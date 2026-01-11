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
        val totalSongs = listeningHistoryDao.getTotalSongsCount()
        
        // Need at least 5 songs in history for decent recommendations
        if (totalSongs < 5) {
            return emptyList()
        }
        
        // Get top songs from last 7 days (recent preferences weighted higher)
        val recentTopSongs = listeningHistoryDao.getRecentTopSongs(
            System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        ).first().take(5)
        
        // Get top artists
        val topArtists = listeningHistoryDao.getTopArtists(3)
        
        val recommendations = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()
        
        // Strategy 1: Find similar songs to top played (70% of recommendations)
        recentTopSongs.take(3).forEach { history ->
            try {
                // Search for similar songs by same artist
                val similarSongs = youTubeRepository.search(
                    "${history.artist} ${extractGenreFromTitle(history.songTitle)}",
                    YouTubeRepository.FILTER_SONGS
                ).filter { it.id !in seenIds }.take(3)
                
                similarSongs.forEach {
                    if (recommendations.size < limit * 0.7) {
                        recommendations.add(it)
                        seenIds.add(it.id)
                    }
                }
            } catch (e: Exception) {
                // Skip on error
            }
        }
        
        // Strategy 2: Explore top artists' popular songs (30% of recommendations)
        topArtists.forEach { artistStats ->
            try {
                val artistSongs = youTubeRepository.search(
                    artistStats.artist,
                    YouTubeRepository.FILTER_SONGS
                ).filter { it.id !in seenIds }.take(2)
                
                artistSongs.forEach {
                    if (recommendations.size < limit) {
                        recommendations.add(it)
                        seenIds.add(it.id)
                    }
                }
            } catch (e: Exception) {
                // Skip on error
            }
        }
        
        return recommendations.shuffled().take(limit)
    }
    
    /**
     * Get "Based on Recent Plays" recommendations.
     * Quick suggestions based on last few songs.
     */
    suspend fun getRecentBasedSuggestions(limit: Int = 10): List<Song> {
        val recentSongs = listeningHistoryDao.getRecentlyPlayed(5).first()
        
        if (recentSongs.isEmpty()) {
            return emptyList()
        }
        
        val suggestions = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()
        
        // Take most recent song and find similar
        val lastPlayed = recentSongs.firstOrNull() ?: return emptyList()
        
        try {
            val similar = youTubeRepository.search(
                "${lastPlayed.artist} ${extractGenreFromTitle(lastPlayed.songTitle)}",
                YouTubeRepository.FILTER_SONGS
            ).filter { it.id != lastPlayed.songId }.take(limit)
            
            suggestions.addAll(similar)
        } catch (e: Exception) {
            // Return empty on error
        }
        
        return suggestions
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

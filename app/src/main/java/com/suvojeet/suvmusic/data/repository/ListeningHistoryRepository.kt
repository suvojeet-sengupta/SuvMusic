package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.data.local.dao.ArtistStats
import com.suvojeet.suvmusic.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.data.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing listening history and generating recommendations.
 */
@Singleton
class ListeningHistoryRepository @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao
) {
    
    /**
     * Record a song play event.
     * Updates play count, total duration, and last played timestamp.
     */
    suspend fun recordPlay(
        song: Song,
        durationListenedMs: Long,
        wasSkipped: Boolean = false
    ) {
        val existing = listeningHistoryDao.getHistoryForSong(song.id)
        
        val updated = if (existing != null) {
            // Update existing record
            val newPlayCount = existing.playCount + 1
            val newTotalDuration = existing.totalDurationMs + durationListenedMs
            val newSkipCount = if (wasSkipped) existing.skipCount + 1 else existing.skipCount
            
            // Calculate completion rate (how much of the song was listened to)
            val completionPercentage = if (song.duration > 0) {
                (durationListenedMs.toFloat() / song.duration.toFloat()) * 100f
            } else 0f
            
            // Update average completion rate
            val newCompletionRate = (existing.completionRate * existing.playCount + completionPercentage) / newPlayCount
            
            existing.copy(
                playCount = newPlayCount,
                totalDurationMs = newTotalDuration,
                lastPlayed = System.currentTimeMillis(),
                skipCount = newSkipCount,
                completionRate = newCompletionRate,
                songTitle = song.title, // Update in case metadata changed
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl
            )
        } else {
            // Create new record
            val completionPercentage = if (song.duration > 0) {
                (durationListenedMs.toFloat() / song.duration.toFloat()) * 100f
            } else 0f
            
            ListeningHistory(
                songId = song.id,
                songTitle = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                playCount = 1,
                totalDurationMs = durationListenedMs,
                lastPlayed = System.currentTimeMillis(),
                firstPlayed = System.currentTimeMillis(),
                skipCount = if (wasSkipped) 1 else 0,
                completionRate = completionPercentage,
                isLiked = false,
                artistId = song.artistId,
                source = when (song.source) {
                    com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE -> "YOUTUBE"
                    com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE_MUSIC -> "YOUTUBE"
                    com.suvojeet.suvmusic.data.model.SongSource.JIOSAAVN -> "JIOSAAVN"
                    com.suvojeet.suvmusic.data.model.SongSource.LOCAL -> "LOCAL"
                    com.suvojeet.suvmusic.data.model.SongSource.DOWNLOADED -> "DOWNLOADED"
                }
            )
        }
        
        listeningHistoryDao.upsert(updated)
    }
    
    /**
     * Mark a song as liked.
     */
    suspend fun markAsLiked(songId: String, isLiked: Boolean) {
        val existing = listeningHistoryDao.getHistoryForSong(songId)
        existing?.let {
            listeningHistoryDao.upsert(it.copy(isLiked = isLiked))
        }
    }
    
    /**
     * Get top played songs.
     */
    fun getTopSongs(limit: Int = 50): Flow<List<ListeningHistory>> {
        return listeningHistoryDao.getTopSongs(limit)
    }
    
    /**
     * Get recently played songs.
     */
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<ListeningHistory>> {
        return listeningHistoryDao.getRecentlyPlayed(limit)
    }
    
    /**
     * Get songs trending in the last N days.
     */
    fun getRecentTrendingSongs(daysAgo: Int = 7): Flow<List<ListeningHistory>> {
        val timestamp = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000L)
        return listeningHistoryDao.getRecentTopSongs(timestamp)
    }
    
    /**
     * Get top artists.
     */
    suspend fun getTopArtists(limit: Int = 10): List<ArtistStats> {
        return listeningHistoryDao.getTopArtists(limit)
    }
    
    /**
     * Get listening statistics.
     */
    suspend fun getListeningStats(): ListeningStats {
        val totalSongs = listeningHistoryDao.getTotalSongsCount()
        val totalTimeMs = listeningHistoryDao.getTotalListeningTime() ?: 0L
        return ListeningStats(
            totalSongsPlayed = totalSongs,
            totalListeningTimeMs = totalTimeMs
        )
    }
    
    /**
     * Clear all listening history.
     */
    suspend fun clearHistory() {
        listeningHistoryDao.clearAll()
    }
    
    /**
     * Clean up old entries (older than 90 days).
     */
    suspend fun cleanupOldEntries(daysToKeep: Int = 90) {
        val timestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        listeningHistoryDao.deleteOldEntries(timestamp)
    }
}

/**
 * Listening statistics data class.
 */
data class ListeningStats(
    val totalSongsPlayed: Int,
    val totalListeningTimeMs: Long
)

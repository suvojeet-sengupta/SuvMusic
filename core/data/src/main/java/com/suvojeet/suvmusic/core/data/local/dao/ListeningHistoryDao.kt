package com.suvojeet.suvmusic.core.data.local.dao

import androidx.room.*
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing listening history data.
 */
@Dao
interface ListeningHistoryDao {
    
    /**
     * Insert or update listening history for a song.
     */
    @Upsert
    suspend fun upsert(history: ListeningHistory)
    
    /**
     * Get listening history for a specific song.
     */
    @Query("SELECT * FROM listening_history WHERE songId = :songId")
    suspend fun getHistoryForSong(songId: String): ListeningHistory?
    
    /**
     * Get all listening history.
     */
    @Query("SELECT * FROM listening_history")
    suspend fun getAllHistory(): List<ListeningHistory>

    /**
     * Get top played songs ordered by play count.
     */
    @Query("SELECT * FROM listening_history ORDER BY playCount DESC LIMIT :limit")
    fun getTopSongs(limit: Int = 50): Flow<List<ListeningHistory>>
    
    /**
     * Get recently played songs.
     */
    @Query("SELECT * FROM listening_history ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<ListeningHistory>>
    
    /**
     * Get listening history after a specific timestamp (e.g., for last month).
     */
    @Query("SELECT * FROM listening_history WHERE lastPlayed >= :timestamp ORDER BY lastPlayed DESC")
    fun getHistoryAfter(timestamp: Long): Flow<List<ListeningHistory>>
    
    /**
     * Get songs played in the last N days.
     */
    @Query("SELECT * FROM listening_history WHERE lastPlayed > :timestamp ORDER BY playCount DESC")
    fun getRecentTopSongs(timestamp: Long): Flow<List<ListeningHistory>>
    
    /**
     * Get total number of songs in history.
     */
    @Query("SELECT COUNT(*) FROM listening_history")
    suspend fun getTotalSongsCount(): Int
    
    /**
     * Get total listening time in milliseconds.
     */
    @Query("SELECT SUM(totalDurationMs) FROM listening_history")
    suspend fun getTotalListeningTime(): Long?
    
    /**
     * Get all unique artists sorted by total play count.
     */
    @Query("""
        SELECT artist, SUM(playCount) as totalPlays 
        FROM listening_history 
        GROUP BY artist 
        ORDER BY totalPlays DESC 
        LIMIT :limit
    """)
    suspend fun getTopArtists(limit: Int = 10): List<ArtistStats>
    
    /**
     * Delete all listening history.
     */
    @Query("DELETE FROM listening_history")
    suspend fun clearAll()
    
    /**
     * Delete old entries (older than N days).
     */
    @Query("DELETE FROM listening_history WHERE lastPlayed < :timestamp")
    suspend fun deleteOldEntries(timestamp: Long)
}

/**
 * Helper class for artist statistics.
 */
data class ArtistStats(
    val artist: String,
    val totalPlays: Int
)

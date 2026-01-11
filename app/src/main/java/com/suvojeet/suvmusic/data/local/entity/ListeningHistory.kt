package com.suvojeet.suvmusic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track user's listening history and preferences.
 * Stores aggregated statistics for each song.
 */
@Entity(tableName = "listening_history")
data class ListeningHistory(
    @PrimaryKey
    val songId: String,
    
    val songTitle: String,
    val artist: String,
    val thumbnailUrl: String?,
    
    // Listening statistics
    val playCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val lastPlayed: Long = System.currentTimeMillis(),
    val firstPlayed: Long = System.currentTimeMillis(),
    
    // User behavior
    val skipCount: Int = 0,
    val completionRate: Float = 0f, // Average % of song listened
    val isLiked: Boolean = false,
    
    // Metadata for recommendations
    val artistId: String? = null,
    val source: String = "YOUTUBE" // YOUTUBE, JIOSAAVN, LOCAL
)

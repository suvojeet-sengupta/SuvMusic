package com.suvojeet.suvmusic.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to persist songs the user has explicitly disliked.
 * Disliked songs are excluded from all recommendations.
 */
@Entity(tableName = "disliked_songs")
data class DislikedSong(
    @PrimaryKey
    val songId: String,
    val title: String,
    val artist: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Entity to persist artists the user has explicitly disliked.
 * Songs by these artists are heavily penalized in scoring.
 */
@Entity(tableName = "disliked_artists")
data class DislikedArtist(
    @PrimaryKey
    val artistName: String, // Normalized name (lowercase)
    val timestamp: Long = System.currentTimeMillis()
)

package com.suvojeet.suvmusic.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to cache genre vectors for songs.
 * Genre vectors are stored as comma-separated float values (20 dimensions).
 * This avoids redundant genre inference for already-analyzed songs.
 */
@Entity(tableName = "song_genres")
data class SongGenre(
    @PrimaryKey
    val songId: String,

    /** Comma-separated FloatArray(20) — one value per genre dimension */
    val genreVector: String,

    /** Timestamp when this genre was inferred/cached */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Parse the stored genre vector string back to FloatArray.
     */
    fun toFloatArray(): FloatArray {
        return try {
            genreVector.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) {
            FloatArray(20)
        }
    }

    companion object {
        /**
         * Create a SongGenre from a songId and FloatArray.
         */
        fun fromFloatArray(songId: String, vector: FloatArray): SongGenre {
            return SongGenre(
                songId = songId,
                genreVector = vector.joinToString(",")
            )
        }
    }
}

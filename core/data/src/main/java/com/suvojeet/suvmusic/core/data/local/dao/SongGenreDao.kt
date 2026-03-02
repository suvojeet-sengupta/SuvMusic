package com.suvojeet.suvmusic.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.suvojeet.suvmusic.core.data.local.entity.SongGenre

/**
 * Data Access Object for cached song genre vectors.
 * Genre vectors are cached to avoid redundant title/artist-based inference.
 */
@Dao
interface SongGenreDao {

    /** Get the cached genre vector for a specific song. */
    @Query("SELECT * FROM song_genres WHERE songId = :songId")
    suspend fun getGenre(songId: String): SongGenre?

    /** Bulk fetch genre vectors for a set of song IDs. */
    @Query("SELECT * FROM song_genres WHERE songId IN (:songIds)")
    suspend fun getGenres(songIds: List<String>): List<SongGenre>

    /** Insert or update a genre vector. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenre(genre: SongGenre)

    /** Bulk insert genre vectors. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genres: List<SongGenre>)

    /** Clear all cached genres (e.g., if taxonomy changes). */
    @Query("DELETE FROM song_genres")
    suspend fun clearAll()

    /** Count of cached genres — used for diagnostics. */
    @Query("SELECT COUNT(*) FROM song_genres")
    suspend fun count(): Int
}

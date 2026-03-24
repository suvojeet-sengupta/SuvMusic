package com.suvojeet.suvmusic.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.suvojeet.suvmusic.core.data.local.entity.DislikedArtist
import com.suvojeet.suvmusic.core.data.local.entity.DislikedSong

/**
 * Data Access Object for managing user's explicit dislikes.
 */
@Dao
interface DislikedItemDao {
    // --- Songs ---
    @Query("SELECT songId FROM disliked_songs")
    suspend fun getAllDislikedSongIds(): List<String>

    @Query("SELECT * FROM disliked_songs")
    suspend fun getAllDislikedSongs(): List<DislikedSong>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDislikedSong(song: DislikedSong)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDislikedSongs(songs: List<DislikedSong>)

    @Query("DELETE FROM disliked_songs WHERE songId = :songId")
    suspend fun removeDislikedSong(songId: String)

    // --- Artists ---
    @Query("SELECT artistName FROM disliked_artists")
    suspend fun getAllDislikedArtistNames(): List<String>

    @Query("SELECT * FROM disliked_artists")
    suspend fun getAllDislikedArtists(): List<DislikedArtist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDislikedArtist(artist: DislikedArtist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDislikedArtists(artists: List<DislikedArtist>)

    @Query("DELETE FROM disliked_artists WHERE artistName = :artistName")
    suspend fun removeDislikedArtist(artistName: String)

    @Query("DELETE FROM disliked_songs")
    suspend fun clearAllDislikedSongs()

    @Query("DELETE FROM disliked_artists")
    suspend fun clearAllDislikedArtists()
}

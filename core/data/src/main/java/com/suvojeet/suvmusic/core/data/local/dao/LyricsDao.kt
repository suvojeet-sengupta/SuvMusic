package com.suvojeet.suvmusic.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.suvojeet.suvmusic.core.data.local.entity.LyricsEntity

@Dao
interface LyricsDao {

    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId AND providerName = :providerName LIMIT 1")
    suspend fun get(songId: String, providerName: String): LyricsEntity?

    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId")
    suspend fun getAllForSong(songId: String): List<LyricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsEntity)

    @Query("DELETE FROM lyrics_cache WHERE songId = :songId")
    suspend fun deleteForSong(songId: String)

    @Query("DELETE FROM lyrics_cache WHERE songId = :songId AND providerName = :providerName")
    suspend fun delete(songId: String, providerName: String)

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM lyrics_cache")
    suspend fun count(): Int
}

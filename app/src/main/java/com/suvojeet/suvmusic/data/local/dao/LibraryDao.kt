package com.suvojeet.suvmusic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.suvojeet.suvmusic.data.local.entity.LibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LibraryEntity)

    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("SELECT * FROM library_items WHERE id = :id")
    suspend fun getItem(id: String): LibraryEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM library_items WHERE id = :id)")
    fun isItemSavedFlow(id: String): Flow<Boolean>

    @Query("SELECT * FROM library_items WHERE type = :type ORDER BY timestamp DESC")
    fun getItemsByType(type: String): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<LibraryEntity>>
}

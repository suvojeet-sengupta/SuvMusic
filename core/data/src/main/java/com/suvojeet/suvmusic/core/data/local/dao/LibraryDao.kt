package com.suvojeet.suvmusic.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.suvojeet.suvmusic.core.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.core.data.local.entity.PlaylistSongEntity
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

    // Playlist Songs Caching
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(songs: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: String)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY `order` ASC")
    suspend fun getPlaylistSongs(playlistId: String): List<PlaylistSongEntity>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY `order` ASC")
    fun getPlaylistSongsFlow(playlistId: String): Flow<List<PlaylistSongEntity>>

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteSongFromPlaylist(playlistId: String, songId: String)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getPlaylistSongCountFlow(playlistId: String): Flow<Int>

    @Transaction
    suspend fun replacePlaylistSongs(playlistId: String, songs: List<PlaylistSongEntity>) {
        deletePlaylistSongs(playlistId)
        insertPlaylistSongs(songs)
    }
}

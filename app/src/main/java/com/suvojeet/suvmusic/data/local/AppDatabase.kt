package com.suvojeet.suvmusic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.suvojeet.suvmusic.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.data.local.entity.PlaylistSongEntity

/**
 * Main Room database for the app.
 */
@Database(
    entities = [ListeningHistory::class, LibraryEntity::class, PlaylistSongEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun libraryDao(): LibraryDao
}

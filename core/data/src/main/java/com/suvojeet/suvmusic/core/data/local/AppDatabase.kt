package com.suvojeet.suvmusic.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.core.data.local.dao.DislikedItemDao
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.core.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.core.data.local.entity.PlaylistSongEntity
import com.suvojeet.suvmusic.core.data.local.entity.DislikedSong
import com.suvojeet.suvmusic.core.data.local.entity.DislikedArtist

/**
 * Main Room database for the app.
 */
@Database(
    entities = [
        ListeningHistory::class, 
        LibraryEntity::class, 
        PlaylistSongEntity::class,
        DislikedSong::class,
        DislikedArtist::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun libraryDao(): LibraryDao
    abstract fun dislikedItemDao(): DislikedItemDao
}

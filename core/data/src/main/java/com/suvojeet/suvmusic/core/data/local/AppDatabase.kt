package com.suvojeet.suvmusic.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.core.data.local.dao.DislikedItemDao
import com.suvojeet.suvmusic.core.data.local.dao.SongGenreDao
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.core.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.core.data.local.entity.PlaylistSongEntity
import com.suvojeet.suvmusic.core.data.local.entity.DislikedSong
import com.suvojeet.suvmusic.core.data.local.entity.DislikedArtist
import com.suvojeet.suvmusic.core.data.local.entity.SongGenre

/**
 * Main Room database for the app.
 */
@Database(
    entities = [
        ListeningHistory::class, 
        LibraryEntity::class, 
        PlaylistSongEntity::class,
        DislikedSong::class,
        DislikedArtist::class,
        SongGenre::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun libraryDao(): LibraryDao
    abstract fun dislikedItemDao(): DislikedItemDao
    abstract fun songGenreDao(): SongGenreDao
}

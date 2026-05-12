package com.suvojeet.suvmusic.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.core.data.local.dao.DislikedItemDao
import com.suvojeet.suvmusic.core.data.local.dao.LyricsDao
import com.suvojeet.suvmusic.core.data.local.dao.SongGenreDao
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.core.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.core.data.local.entity.LyricsEntity
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
        SongGenre::class,
        LyricsEntity::class
    ],
    version = 12,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun libraryDao(): LibraryDao
    abstract fun dislikedItemDao(): DislikedItemDao
    abstract fun songGenreDao(): SongGenreDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        /**
         * v11 → v12: add lyrics_cache table for persistent Enhanced LRC ("LRC v2") storage.
         * Pure additive change — no existing rows are touched, so user data is preserved.
         */
        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lyrics_cache` (
                        `songId` TEXT NOT NULL,
                        `providerName` TEXT NOT NULL,
                        `lrcContent` TEXT NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        `sourceCredit` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`songId`, `providerName`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}

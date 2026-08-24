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
    version = 13,
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

        /**
         * v12 → v13: allow repeated tracks in a playlist.
         *
         * The previous composite key (playlistId, songId) made Room's REPLACE
         * strategy overwrite earlier occurrences when an import resolved two
         * entries to the same video. Playlist position is the stable entry key;
         * songId remains indexed for lookups and removal.
         */
        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE `playlist_songs_new` (
                        `playlistId` TEXT NOT NULL,
                        `songId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT,
                        `thumbnailUrl` TEXT,
                        `duration` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `localUri` TEXT,
                        `releaseDate` TEXT,
                        `addedAt` INTEGER NOT NULL,
                        `order` INTEGER NOT NULL,
                        PRIMARY KEY(`playlistId`, `order`)
                    )
                    """.trimIndent()
                )

                // Re-number entries per playlist so even databases containing
                // gaps or legacy order collisions migrate deterministically.
                db.execSQL(
                    """
                    INSERT INTO `playlist_songs_new`
                    (`playlistId`, `songId`, `title`, `artist`, `album`, `thumbnailUrl`,
                     `duration`, `source`, `localUri`, `releaseDate`, `addedAt`, `order`)
                    SELECT p.`playlistId`, p.`songId`, p.`title`, p.`artist`, p.`album`,
                           p.`thumbnailUrl`, p.`duration`, p.`source`, p.`localUri`,
                           p.`releaseDate`, p.`addedAt`,
                           (
                               SELECT COUNT(*) - 1
                               FROM `playlist_songs` q
                               WHERE q.`playlistId` = p.`playlistId`
                                 AND (q.`order` < p.`order`
                                      OR (q.`order` = p.`order` AND q.rowid <= p.rowid))
                           )
                    FROM `playlist_songs` p
                    ORDER BY p.`playlistId`, p.`order`, p.rowid
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `playlist_songs`")
                db.execSQL("ALTER TABLE `playlist_songs_new` RENAME TO `playlist_songs`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId_songId` ON `playlist_songs` (`playlistId`, `songId`)")
            }
        }
    }
}

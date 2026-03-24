package com.suvojeet.suvmusic.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.suvojeet.suvmusic.core.data.local.AppDatabase
import com.suvojeet.suvmusic.data.model.BackupData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val gson: Gson
) {
    /**
     * Create a complete backup of SuvMusic data and write it to the provided output stream.
     * The backup is a GZIP compressed JSON file (.suv).
     */
    suspend fun createBackup(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val libraryDao = database.libraryDao()
            val historyDao = database.listeningHistoryDao()
            val dislikeDao = database.dislikedItemDao()
            val genreDao = database.songGenreDao()

            val backup = BackupData(
                settings = sessionManager.getAllSettings(),
                libraryItems = libraryDao.getAllItemsSync(),
                playlistSongs = libraryDao.getAllPlaylistSongs(),
                listeningHistory = historyDao.getAllHistory(),
                dislikedSongs = dislikeDao.getAllDislikedSongs(),
                dislikedArtists = dislikeDao.getAllDislikedArtists(),
                songGenres = genreDao.getAllGenres()
            )

            val json = gson.toJson(backup)
            GZIPOutputStream(outputStream).use { gzip ->
                gzip.write(json.toByteArray())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore SuvMusic data from the provided input stream.
     * This will clear existing data and replace it with the backup content.
     */
    suspend fun restoreBackup(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("BackupManager", "Starting restore...")
            val json = GZIPInputStream(inputStream).bufferedReader().use { it.readText() }
            val backup = gson.fromJson(json, BackupData::class.java) ?: return@withContext Result.failure(Exception("Failed to deserialize backup data"))

            Log.d("BackupManager", "Backup deserialized. Version: ${backup.version}, Items: ${backup.libraryItems.size}")

            // 1. Restore settings
            sessionManager.restoreSettings(backup.settings)
            Log.d("BackupManager", "Settings restored")

            // 2. Restore Database Atomically
            database.withTransaction {
                val libraryDao = database.libraryDao()
                val historyDao = database.listeningHistoryDao()
                val dislikeDao = database.dislikedItemDao()
                val genreDao = database.songGenreDao()

                // Library
                libraryDao.clearAll()
                libraryDao.clearAllPlaylistSongs()
                Log.d("BackupManager", "Database cleared")

                libraryDao.insertItems(backup.libraryItems)
                libraryDao.insertPlaylistSongs(backup.playlistSongs)
                Log.d("BackupManager", "Library and playlist songs inserted: ${backup.libraryItems.size}, ${backup.playlistSongs.size}")

                // History
                historyDao.clearAll()
                backup.listeningHistory.forEach { historyDao.upsert(it) }
                Log.d("BackupManager", "Listening history restored: ${backup.listeningHistory.size}")

                // Dislikes
                dislikeDao.clearAllDislikedSongs()
                dislikeDao.clearAllDislikedArtists()
                dislikeDao.insertDislikedSongs(backup.dislikedSongs)
                dislikeDao.insertDislikedArtists(backup.dislikedArtists)
                Log.d("BackupManager", "Dislikes restored")

                // Genres
                genreDao.clearAll()
                genreDao.insertGenres(backup.songGenres)
                Log.d("BackupManager", "Genres restored: ${backup.songGenres.size}")
            }

            Log.d("BackupManager", "Restore completed successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BackupManager", "Restore failed!", e)
            Result.failure(e)
        }
    }
}

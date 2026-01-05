package com.suvojeet.suvmusic.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository
) {
    companion object {
        private const val TAG = "DownloadRepository"
        private const val SUVMUSIC_FOLDER = "SuvMusic"
    }
    
    private val gson = Gson()
    private val downloadsMetaFile = File(context.filesDir, "downloads_meta.json")
    
    // Old internal storage location (for migration)
    private val oldDownloadsDir = File(context.filesDir, "downloads")
    
    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()
    
    // Dedicated HTTP client for downloads with longer timeouts
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    init {
        loadDownloads()
        // Migrate old downloads from internal storage to public Downloads folder
        migrateOldDownloads()
        // Scan Downloads/SuvMusic folder for any manually added files
        scanDownloadsFolder()
    }

    /**
     * Scan Downloads/SuvMusic folder for audio files that aren't tracked yet.
     * This allows users to manually add songs to the folder.
     */
    private fun scanDownloadsFolder() {
        try {
            val folder = getPublicDownloadsFolder()
            if (!folder.exists()) return
            
            val audioFiles = folder.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac", "wav", "ogg", "opus")
            } ?: return
            
            if (audioFiles.isEmpty()) return
            
            val currentSongs = _downloadedSongs.value.toMutableList()
            var hasNewSongs = false
            
            for (file in audioFiles) {
                // Check if already tracked by URI
                val fileUri = file.toUri()
                val isTracked = currentSongs.any { song ->
                    song.localUri?.path == file.absolutePath || 
                    song.localUri == fileUri
                }
                
                if (!isTracked) {
                    // Parse filename: "Title - Artist.m4a" format
                    val nameWithoutExt = file.nameWithoutExtension
                    val parts = nameWithoutExt.split(" - ", limit = 2)
                    val title = parts.getOrElse(0) { nameWithoutExt }.trim()
                    val artist = parts.getOrElse(1) { "Unknown Artist" }.trim()
                    
                    val song = Song(
                        id = "local_${file.name.hashCode()}",
                        title = title,
                        artist = artist,
                        album = "Downloads",
                        duration = 0L, // We don't have duration info for manually added files
                        thumbnailUrl = null,
                        source = SongSource.DOWNLOADED,
                        streamUrl = null,
                        localUri = fileUri
                    )
                    
                    currentSongs.add(song)
                    hasNewSongs = true
                    Log.d(TAG, "Found untracked file: ${file.name}")
                }
            }
            
            if (hasNewSongs) {
                _downloadedSongs.value = currentSongs
                saveDownloads()
                Log.d(TAG, "Added ${currentSongs.size - _downloadedSongs.value.size} untracked files to downloads")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning downloads folder", e)
        }
    }

    /**
     * Get the public Downloads/SuvMusic folder
     */
    private fun getPublicDownloadsFolder(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val suvMusicDir = File(downloadsDir, SUVMUSIC_FOLDER)
        if (!suvMusicDir.exists()) {
            suvMusicDir.mkdirs()
        }
        return suvMusicDir
    }

    /**
     * Migrate downloads from old internal storage to public Downloads folder
     */
    private fun migrateOldDownloads() {
        if (!oldDownloadsDir.exists()) return
        
        val oldFiles = oldDownloadsDir.listFiles() ?: return
        if (oldFiles.isEmpty()) return
        
        Log.d(TAG, "Found ${oldFiles.size} files to migrate from internal storage")
        
        val currentSongs = _downloadedSongs.value.toMutableList()
        var migrated = false
        
        for (oldFile in oldFiles) {
            try {
                val songId = oldFile.nameWithoutExtension
                val song = currentSongs.find { it.id == songId }
                
                if (song != null) {
                    // Move file to public Downloads folder
                    val newUri = saveFileToPublicDownloads(songId, song.artist, song.title, oldFile.inputStream())
                    
                    if (newUri != null) {
                        // Update song with new URI
                        val index = currentSongs.indexOfFirst { it.id == songId }
                        if (index >= 0) {
                            currentSongs[index] = song.copy(localUri = newUri)
                            migrated = true
                        }
                        
                        // Delete old file
                        oldFile.delete()
                        Log.d(TAG, "Migrated ${song.title} to public Downloads folder")
                    }
                } else {
                    // No metadata, just delete old file
                    oldFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating file: ${oldFile.name}", e)
            }
        }
        
        if (migrated) {
            _downloadedSongs.value = currentSongs
            saveDownloads()
        }
        
        // Clean up old directory if empty
        if (oldDownloadsDir.listFiles()?.isEmpty() == true) {
            oldDownloadsDir.delete()
        }
    }

    /**
     * Save file to public Downloads/SuvMusic folder using appropriate API
     */
    private fun saveFileToPublicDownloads(songId: String, artist: String, title: String, inputStream: InputStream): Uri? {
        val fileName = "${sanitizeFileName(title)} - ${sanitizeFileName(artist)}.m4a"
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ use MediaStore
            saveToMediaStore(songId, fileName, inputStream)
        } else {
            // Android 9 and below use direct file access
            saveToPublicFolder(songId, fileName, inputStream)
        }
    }

    /**
     * Save to MediaStore for Android 10+ (Scoped Storage)
     */
    private fun saveToMediaStore(songId: String, fileName: String, inputStream: InputStream): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/m4a")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$SUVMUSIC_FOLDER")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { mediaUri ->
                resolver.openOutputStream(mediaUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                // Mark as complete
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(mediaUri, contentValues, null, null)
                
                Log.d(TAG, "Saved to MediaStore: $fileName")
            }
            
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to MediaStore", e)
            // Fallback to direct file access
            saveToPublicFolder(songId, fileName, inputStream)
        }
    }

    /**
     * Save to public Downloads folder directly (Android 9 and below)
     */
    private fun saveToPublicFolder(songId: String, fileName: String, inputStream: InputStream): Uri? {
        return try {
            val folder = getPublicDownloadsFolder()
            val file = File(folder, fileName)
            
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            
            Log.d(TAG, "Saved to public folder: ${file.absolutePath}")
            file.toUri()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to public folder", e)
            null
        }
    }

    /**
     * Sanitize filename to remove invalid characters
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    private fun loadDownloads() {
        if (!downloadsMetaFile.exists()) {
            _downloadedSongs.value = emptyList()
            return
        }
        try {
            val json = downloadsMetaFile.readText()
            val type = object : TypeToken<List<Song>>() {}.type
            val songs: List<Song> = gson.fromJson(json, type) ?: emptyList()
            
            // Verify files still exist
            val validSongs = songs.filter { song ->
                song.localUri?.let { uri ->
                    try {
                        // Check if file exists (works for both file:// and content:// URIs)
                        if (uri.scheme == "file") {
                            File(uri.path ?: "").exists()
                        } else {
                            context.contentResolver.openInputStream(uri)?.close()
                            true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "File not found for ${song.title}, removing from list")
                        false
                    }
                } ?: false
            }
            
            _downloadedSongs.value = validSongs
            
            // If some songs were removed, save the updated list
            if (validSongs.size != songs.size) {
                saveDownloads()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading downloads", e)
            _downloadedSongs.value = emptyList()
        }
    }

    private fun saveDownloads() {
        try {
            val json = gson.toJson(_downloadedSongs.value)
            downloadsMetaFile.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving downloads", e)
        }
    }

    suspend fun downloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        if (_downloadedSongs.value.any { it.id == song.id }) {
            Log.d(TAG, "Song ${song.id} already downloaded")
            return@withContext true
        }
        
        // Mark as downloading
        _downloadingIds.value = _downloadingIds.value + song.id
        Log.d(TAG, "Starting download for: ${song.title} (${song.id})")
        
        try {
            // Get stream URL with download quality preference
            val streamUrl = youTubeRepository.getStreamUrlForDownload(song.id)
            if (streamUrl == null) {
                Log.e(TAG, "Failed to get stream URL for ${song.id}")
                _downloadingIds.value = _downloadingIds.value - song.id
                return@withContext false
            }
            
            Log.d(TAG, "Got stream URL, starting download...")
            
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity")
                .header("Connection", "keep-alive")
                .build()
            
            val response = downloadClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Download request failed: ${response.code} - ${response.message}")
                response.close()
                _downloadingIds.value = _downloadingIds.value - song.id
                return@withContext false
            }
            
            val contentLength = response.body?.contentLength() ?: -1L
            Log.d(TAG, "Content length: $contentLength bytes")

            // Save to public Downloads/SuvMusic folder
            val downloadedUri = response.body?.byteStream()?.use { inputStream ->
                saveFileToPublicDownloads(song.id, song.artist, song.title, inputStream)
            }
            
            response.close()
            
            if (downloadedUri == null) {
                Log.e(TAG, "Failed to save file")
                _downloadingIds.value = _downloadingIds.value - song.id
                return@withContext false
            }
            
            Log.d(TAG, "Download complete: saved to $downloadedUri")

            // Download thumbnail if available
            var localThumbnailUrl = song.thumbnailUrl
            if (!song.thumbnailUrl.isNullOrEmpty() && song.thumbnailUrl.startsWith("http")) {
                try {
                    val thumbRequest = Request.Builder().url(song.thumbnailUrl).build()
                    val thumbResponse = downloadClient.newCall(thumbRequest).execute()
                    if (thumbResponse.isSuccessful) {
                        val thumbnailsDir = File(context.filesDir, "thumbnails")
                        if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
                        
                        val thumbFile = File(thumbnailsDir, "${song.id}.jpg")
                        thumbResponse.body?.byteStream()?.use { input ->
                            FileOutputStream(thumbFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        localThumbnailUrl = thumbFile.absolutePath
                        Log.d(TAG, "Downloaded thumbnail to $localThumbnailUrl")
                    }
                    thumbResponse.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download thumbnail", e)
                    // Keep original URL if download fails, so it might work if online
                }
            }

            // Create downloaded song entry
            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = downloadedUri,
                thumbnailUrl = localThumbnailUrl,
                streamUrl = null
            )

            _downloadedSongs.value = _downloadedSongs.value + downloadedSong
            saveDownloads()
            
            _downloadingIds.value = _downloadingIds.value - song.id
            Log.d(TAG, "Song ${song.title} download successful!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error for ${song.id}", e)
            _downloadingIds.value = _downloadingIds.value - song.id
            false
        }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val song = _downloadedSongs.value.find { it.id == songId } ?: return@withContext
        
        try {
            song.localUri?.let { uri ->
                when (uri.scheme) {
                    "file" -> {
                        val file = File(uri.path ?: "")
                        if (file.exists()) file.delete()
                    }
                    "content" -> {
                        try {
                            context.contentResolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not delete via ContentResolver", e)
                        }
                    }
                }
            }
            
            // Delete local thumbnail if exists
            val thumbnailsDir = File(context.filesDir, "thumbnails")
            val thumbFile = File(thumbnailsDir, "${songId}.jpg")
            if (thumbFile.exists()) {
                thumbFile.delete()
            }
            
            _downloadedSongs.value = _downloadedSongs.value.filter { it.id != songId }
            saveDownloads()
            Log.d(TAG, "Deleted download: $songId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting download", e)
        }
    }
    
    fun isDownloaded(songId: String): Boolean {
        return _downloadedSongs.value.any { it.id == songId }
    }
    
    fun isDownloading(songId: String): Boolean {
        return _downloadingIds.value.contains(songId)
    }
    
    /**
     * Rescan Downloads/SuvMusic folder for new files.
     * Call this when entering the Library screen to pick up manually added files.
     */
    fun refreshDownloads() {
        loadDownloads()
        scanDownloadsFolder()
    }
    
    /**
     * Get download count and total duration info.
     */
    fun getDownloadInfo(): Pair<Int, Long> {
        val songs = _downloadedSongs.value
        val totalDuration = songs.sumOf { it.duration }
        return Pair(songs.size, totalDuration)
    }
}

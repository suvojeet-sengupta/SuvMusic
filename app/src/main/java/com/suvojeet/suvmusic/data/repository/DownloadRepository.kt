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
import com.suvojeet.suvmusic.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    @com.suvojeet.suvmusic.di.DownloadDataSource private val dataSourceFactory: androidx.media3.datasource.DataSource.Factory
) {
    companion object {
        private const val TAG = "DownloadRepository"
        private const val SUVMUSIC_FOLDER = "SuvMusic"
    }
    
    private val gson = com.google.gson.GsonBuilder()
        .registerTypeAdapter(Uri::class.java, com.suvojeet.suvmusic.utils.UriTypeAdapter())
        .create()
    private val downloadsMetaFile = File(context.filesDir, "downloads_meta.json")
    
    // Old internal storage location (for migration)
    private val oldDownloadsDir = File(context.filesDir, "downloads")
    
    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()
    
    // Download progress tracking for progressive download UI (songId -> progress 0.0-1.0)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
        
    // Batch Download Queue
    // Fix: Broken Download Cancellation -> Use Synchronized List or ConcurrentLinkedDeque for safety
    private val downloadQueue = java.util.concurrent.ConcurrentLinkedDeque<Song>()
    // Track active jobs for cancellation
    private val activeDownloadJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val _queueState = MutableStateFlow<List<Song>>(emptyList())
    val queueState: StateFlow<List<Song>> = _queueState.asStateFlow()
    
    private val _batchProgress = MutableStateFlow<Pair<Int, Int>>(0 to 0) // current, total
    val batchProgress: StateFlow<Pair<Int, Int>> = _batchProgress.asStateFlow()

    init {
        // Fix Main Thread I/O: Move heavy initialization to background thread
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            loadDownloads()
            // Migrate old downloads from internal storage to public Downloads folder
            migrateOldDownloads()
            // Scan Downloads/SuvMusic folder for any manually added files
            scanDownloadsFolder()
        }
    }

    /**
     * Scan Downloads/SuvMusic folder for audio files that aren't tracked yet.
     * This allows users to manually add songs to the folder.
     */
    private fun scanDownloadsFolder() {
        try {
            val folder = getPublicMusicFolder()
            if (!folder.exists()) return
            
            // Also scan legacy Downloads folder for migration
            scanAndMigrateLegacyFolder()
            
            val audioFiles = folder.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac", "wav", "ogg", "opus")
            } ?: return
            
            if (audioFiles.isEmpty()) return
            
            val currentSongs = _downloadedSongs.value.toMutableList()
            var hasNewSongs = false
            
            for (file in audioFiles) {
                // Check if already tracked by URI or Filename
                val fileUri = file.toUri()
                
                // Parse filename: "Title - Artist.m4a" format
                val nameWithoutExt = file.nameWithoutExtension
                val parts = nameWithoutExt.split(" - ", limit = 2)
                val title = parts.getOrElse(0) { nameWithoutExt }.trim()
                val artist = parts.getOrElse(1) { "Unknown Artist" }.trim()
                
                val isTracked = currentSongs.any { song ->
                    // 1. Check direct URI match
                    if (song.localUri?.path == file.absolutePath || song.localUri == fileUri) {
                        return@any true
                    }
                    
                    // 2. Check filename match (most reliable for our naming convention)
                    // The file on disk is "SanitizedTitle - SanitizedArtist.ext"
                    val expectedFileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}"
                    if (nameWithoutExt == expectedFileName) {
                        return@any true
                    }
                    
                    // 3. Fallback: Check Title and Artist match
                    // Useful if sanitization logic changed slightly or for very similar files
                    if (song.title == title && song.artist == artist) {
                        return@any true
                    }
                    
                    false
                }
                
                if (!isTracked) {
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
     * Get the public Music/SuvMusic folder
     */
    private fun getPublicMusicFolder(): File {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val suvMusicDir = File(musicDir, SUVMUSIC_FOLDER)
        if (!suvMusicDir.exists()) {
            suvMusicDir.mkdirs()
        }
        return suvMusicDir
    }
    
    /**
     * Legacy: Get old Downloads/SuvMusic folder for migration
     */
    private fun getLegacyDownloadsFolder(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, SUVMUSIC_FOLDER)
    }
    
    /**
     * Scan and migrate files from legacy Downloads/SuvMusic folder to Music/SuvMusic
     */
    private fun scanAndMigrateLegacyFolder() {
        try {
            val legacyFolder = getLegacyDownloadsFolder()
            if (!legacyFolder.exists()) return
            
            val legacyFiles = legacyFolder.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac", "wav", "ogg", "opus")
            } ?: return
            
            if (legacyFiles.isEmpty()) return
            
            Log.d(TAG, "Found ${legacyFiles.size} files in legacy Downloads folder to migrate")
            
            val newFolder = getPublicMusicFolder()
            for (file in legacyFiles) {
                try {
                    val newFile = File(newFolder, file.name)
                    if (!newFile.exists()) {
                        file.copyTo(newFile, overwrite = false)
                        file.delete()
                        Log.d(TAG, "Migrated ${file.name} to Music/SuvMusic")
                    } else {
                        // File already exists in new location, just delete legacy
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error migrating file: ${file.name}", e)
                }
            }
            
            // Clean up empty legacy folder
            if (legacyFolder.listFiles()?.isEmpty() == true) {
                legacyFolder.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in legacy folder migration", e)
        }
    }
    
    /**
     * Helper to delete a song file from a specific folder
     */
    /**
     * Helper to delete a song file from a specific folder
     */
    private fun deleteFromFolder(folder: File, song: Song): Boolean {
        if (!folder.exists()) return false
        
        try {
            // Determine search folders: subfolder first, then base folder
            val searchFolders = mutableListOf<File>()
            
            song.customFolderPath?.let { sub ->
                val subFolder = File(folder, sanitizeFileName(sub))
                if (subFolder.exists() && subFolder.isDirectory) {
                    searchFolders.add(subFolder)
                }
            }
            searchFolders.add(folder)

            for (targetFolder in searchFolders) {
                // Try exact filename match first
                val fileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.m4a"
                val file = File(targetFolder, fileName)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "Deleted file from ${targetFolder.name}: $deleted - ${file.absolutePath}")
                    
                    // Cleanup empty subfolder if it was the target
                    if (deleted && targetFolder != folder) {
                        try {
                            if (targetFolder.listFiles()?.isEmpty() == true) {
                                targetFolder.delete()
                            }
                        } catch (e: Exception) {}
                    }
                    
                    if (deleted) return true
                }
                
                // Try alternate extensions
                val extensions = listOf("mp3", "aac", "flac", "wav", "ogg")
                for (ext in extensions) {
                    val altFile = File(targetFolder, "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.$ext")
                    if (altFile.exists()) {
                        val deleted = altFile.delete()
                        if (deleted) {
                            Log.d(TAG, "Deleted file with ext $ext: ${altFile.name}")
                            
                            // Cleanup empty subfolder
                            if (targetFolder != folder) {
                                try {
                                    if (targetFolder.listFiles()?.isEmpty() == true) {
                                        targetFolder.delete()
                                    }
                                } catch (e: Exception) {}
                            }
                            
                            return true
                        }
                    }
                }
                
                // Fallback: search by partial title match
                targetFolder.listFiles()?.forEach { f ->
                    if (f.nameWithoutExtension.contains(song.title.take(20), ignoreCase = true)) {
                        val deleted = f.delete()
                        if (deleted) {
                            Log.d(TAG, "Deleted matching file: ${f.name}")
                            
                            // Cleanup empty subfolder
                            if (targetFolder != folder) {
                                try {
                                    if (targetFolder.listFiles()?.isEmpty() == true) {
                                        targetFolder.delete()
                                    }
                                } catch (e: Exception) {}
                            }
                            
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting from folder ${folder.name}", e)
        }
        
        return false
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
    private fun saveFileToPublicDownloads(songId: String, artist: String, title: String, inputStream: InputStream, subfolder: String? = null): Uri? {
        val fileName = "${sanitizeFileName(title)} - ${sanitizeFileName(artist)}.m4a"
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ use MediaStore
            saveToMediaStore(songId, fileName, inputStream, subfolder)
        } else {
            // Android 9 and below use direct file access
            saveToPublicFolder(songId, fileName, inputStream, subfolder)
        }
    }

    /**
     * Save to MediaStore for Android 10+ (Scoped Storage)
     */
    private fun saveToMediaStore(songId: String, fileName: String, inputStream: InputStream, subfolder: String? = null): Uri? {
        return try {
            val relativePath = if (subfolder != null) {
                "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER/${sanitizeFileName(subfolder)}"
            } else {
                "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/m4a")
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
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
                
                Log.d(TAG, "Saved to MediaStore: $fileName in $relativePath")
            }
            
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to MediaStore", e)
            // Fallback to direct file access
            saveToPublicFolder(songId, fileName, inputStream, subfolder)
        }
    }

    /**
     * Save to public Music folder directly (Android 9 and below)
     */
    private fun saveToPublicFolder(songId: String, fileName: String, inputStream: InputStream, subfolder: String? = null): Uri? {
        return try {
            val rootFolder = getPublicMusicFolder()
            val targetFolder = if (subfolder != null) {
                File(rootFolder, sanitizeFileName(subfolder)).apply { mkdirs() }
            } else {
                rootFolder
            }
            
            val file = File(targetFolder, fileName)
            
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            
            Log.d(TAG, "Saved to Music folder: ${file.absolutePath}")
            file.toUri()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Music folder", e)
            null
        }
    }
    
    /**
     * Save file with progress tracking for notification updates.
     * Streams bytes while emitting progress callbacks.
     */
    private fun saveFileWithProgress(
        songId: String,
        artist: String,
        title: String,
        inputStream: InputStream,
        contentLength: Long,
        subfolder: String? = null,
        onProgress: (Float) -> Unit
    ): Uri? {
        val fileName = "${sanitizeFileName(title)} - ${sanitizeFileName(artist)}.m4a"
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ use MediaStore with progress
                saveToMediaStoreWithProgress(fileName, inputStream, contentLength, subfolder, onProgress)
            } else {
                // Android 9 and below use direct file access
                saveToPublicFolderWithProgress(fileName, inputStream, contentLength, subfolder, onProgress)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file with progress", e)
            null
        }
    }
    
    /**
     * Save to MediaStore with progress tracking (Android 10+)
     */
    private fun saveToMediaStoreWithProgress(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        subfolder: String? = null,
        onProgress: (Float) -> Unit
    ): Uri? {
        return try {
            val relativePath = if (subfolder != null) {
                "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER/${sanitizeFileName(subfolder)}"
            } else {
                "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/m4a")
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { mediaUri ->
                resolver.openOutputStream(mediaUri)?.use { outputStream ->
                    copyWithProgress(inputStream, outputStream, contentLength, onProgress)
                }

                // Mark as complete
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(mediaUri, contentValues, null, null)
                
                Log.d(TAG, "Saved to MediaStore with progress: $fileName in $relativePath")
            }
            
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to MediaStore with progress", e)
            null
        }
    }
    
    /**
     * Save to public folder with progress (Android 9 and below)
     */
    private fun saveToPublicFolderWithProgress(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        subfolder: String? = null,
        onProgress: (Float) -> Unit
    ): Uri? {
        return try {
            val rootFolder = getPublicMusicFolder()
            val targetFolder = if (subfolder != null) {
                File(rootFolder, sanitizeFileName(subfolder)).apply { mkdirs() }
            } else {
                rootFolder
            }
            
            val file = File(targetFolder, fileName)
            
            FileOutputStream(file).use { outputStream ->
                copyWithProgress(inputStream, outputStream, contentLength, onProgress)
            }
            
            Log.d(TAG, "Saved to Music folder with progress: ${file.absolutePath}")
            file.toUri()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Music folder with progress", e)
            null
        }
    }
    
    /**
     * Copy input to output with progress callbacks
     */
    private fun copyWithProgress(
        input: InputStream,
        output: java.io.OutputStream,
        contentLength: Long,
        onProgress: (Float) -> Unit
    ) {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalBytesRead = 0L
        var lastProgressUpdate = 0L
        
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            
            // Update progress every 50KB to avoid too frequent updates
            if (contentLength > 0 && totalBytesRead - lastProgressUpdate > 50 * 1024) {
                val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                onProgress(progress)
                lastProgressUpdate = totalBytesRead
            }
        }
        
        // Final progress update
        if (contentLength > 0) {
            onProgress(1f)
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
            // Fix: Use AtomicFile for safe reading
            val atomicFile = androidx.core.util.AtomicFile(downloadsMetaFile)
            val json = try {
                atomicFile.readFully().toString(Charsets.UTF_8)
            } catch (e: Exception) {
                // Fallback to direct read if AtomicFile fails (e.g. first run/migration issue)
                downloadsMetaFile.readText()
            }
            
            val type = object : TypeToken<List<Song>>() {}.type
            val songs: List<Song> = gson.fromJson(json, type) ?: emptyList()
            
            // Verify files still exist
            // Verify files still exist and repair broken URIs if possible
            val validSongs = songs.mapNotNull { song ->
                val uri = song.localUri
                
                // Case 1: Valid URI (check existence)
                if (uri != null && uri != Uri.EMPTY) {
                   try {
                        if (uri.scheme == "file") {
                            val file = File(uri.path ?: "")
                            if (file.exists()) return@mapNotNull song
                        } else {
                            // Check content URI access
                            context.contentResolver.openInputStream(uri)?.close()
                            return@mapNotNull song
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "File check failed for ${song.title}: $uri")
                    }
                }
                
                // Case 2: Broken/Empty URI or File Not Found - Attempt Repair
                // Try to find the file in public Music folder
                try {
                    val folder = getPublicMusicFolder()
                    val fileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.m4a"
                    val file = File(folder, fileName)
                    
                    if (file.exists()) {
                        Log.d(TAG, "Repaired URI for ${song.title}: ${file.toUri()}")
                        return@mapNotNull song.copy(localUri = file.toUri())
                    }
                    
                    // Try legacy folder
                    val legacyFolder = getLegacyDownloadsFolder()
                    val legacyFile = File(legacyFolder, fileName)
                    if (legacyFile.exists()) {
                         Log.d(TAG, "Repaired URI (Legacy) for ${song.title}: ${legacyFile.toUri()}")
                         return@mapNotNull song.copy(localUri = legacyFile.toUri())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error repairing song ${song.title}", e)
                }
                
                // Case 3: Completely missing
                Log.w(TAG, "Song ${song.title} missing, removing from list")
                null
            }
            
            // Deduplicate songs (fix for duplicate entries issue)
            // Group by Title + Artist and keep the best version (prefer with thumbnail, prefer original ID)
            val distinctSongs = validSongs
                .groupBy { "${it.title.trim().lowercase()}-${it.artist.trim().lowercase()}" }
                .map { (_, duplicates) ->
                    if (duplicates.size > 1) {
                        duplicates.sortedWith(
                            compareByDescending<Song> { !it.thumbnailUrl.isNullOrEmpty() }
                                .thenByDescending { !it.id.startsWith("local_") }
                        ).first()
                    } else {
                        duplicates.first()
                    }
                }
            
            _downloadedSongs.value = distinctSongs
            
            // If some songs were removed (invalid or duplicates), save the updated list
            if (distinctSongs.size != songs.size) {
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
            
            // Fix: Insecure Data Persistence -> Use AtomicFile
            val atomicFile = androidx.core.util.AtomicFile(downloadsMetaFile)
            var fos: java.io.FileOutputStream? = null
            try {
                fos = atomicFile.startWrite()
                fos.write(json.toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(fos)
            } catch (e: Exception) {
                atomicFile.failWrite(fos)
                Log.e(TAG, "Atomic write failed", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving downloads", e)
        }
    }



    private val downloadMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun downloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        // Critical Section: Check atomic state
        val canDownload = downloadMutex.withLock {
            if (_downloadedSongs.value.any { it.id == song.id }) {
                Log.d(TAG, "Song ${song.id} already downloaded")
                return@withLock false
            }
            if (_downloadingIds.value.contains(song.id)) {
                Log.d(TAG, "Song ${song.id} is already downloading")
                return@withLock false
            }
            // Mark as downloading (Atomic update)
            _downloadingIds.update { it + song.id }
            true
        }

        if (!canDownload) return@withContext true
        
        // Track coroutine job for cancellation
        val job = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        if (job != null) {
            activeDownloadJobs[song.id] = job
        }
        
        Log.d(TAG, "Starting download for: ${song.title} (${song.id})")
        
        try {
            // Get stream URL based on song source
            val streamUrl = when (song.source) {
                SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(song.id, 320)
                else -> youTubeRepository.getStreamUrlForDownload(song.id)
            }
            if (streamUrl == null) {
                Log.e(TAG, "Failed to get stream URL for ${song.id}")
                downloadMutex.withLock {
                     _downloadingIds.update { it - song.id }
                }
                return@withContext false
            }
            
            Log.d(TAG, "Got stream URL, initiating DataSource download...")
            
            // Helper function to calculate content length if possible or just use stream
            // Since we are using DataSource, we might not get exact content length upfront without opening it
            // We use the shared logic which handles reading from cache or network
            
            val downloadSuccess = downloadUsingSharedCache(song, streamUrl)
            
            if (downloadSuccess) {
                 // Create downloaded song entry
                 val downloadedSong = song.copy(
                    source = SongSource.DOWNLOADED,
                    // Note: localUri is set inside downloadUsingSharedCache implicitly by returning the saved URI, 
                    // but we need to do it cleanly. 
                    // The helper saves the file and returns URI.
                    localUri = null, // Will be updated
                    thumbnailUrl = song.thumbnailUrl,
                    streamUrl = null,
                    originalSource = song.source 
                 )
                  // The helper function saves the file and returns URI, let's refactor slightly to return URI
            }

            // Refactoring to match the logic flow:
            // 1. downloadUsingSharedCache returns the saved URI or null
            // 2. if not null, download thumbnails etc
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error for ${song.id}", e)
            downloadMutex.withLock {
                _downloadingIds.update { it - song.id }
            }
            _downloadProgress.update { it - song.id }
            false
        } finally {
            activeDownloadJobs.remove(song.id)
        }
    }

    // New method to download using Shared Cache (DataSource)
    private fun downloadUsingSharedCache(song: Song, streamUrl: String): Boolean {
        return try {
            val uri = android.net.Uri.parse(streamUrl)
            val dataSpec = androidx.media3.datasource.DataSpec.Builder()
                .setUri(uri)
                .setKey(song.id) // CRITICAL: Match MusicPlayer's cache key
                .build()
            val dataSource = dataSourceFactory.createDataSource()
            
            // Open the source (this reads from cache if available, or network if not)
            val length = dataSource.open(dataSpec)
            val contentLength = if (length != androidx.media3.common.C.LENGTH_UNSET.toLong()) length else -1L
             Log.d(TAG, "Content lengthfrom DataSource: $contentLength")
            
             // Initialize progress
            _downloadProgress.value = _downloadProgress.value + (song.id to 0f)
            
            // We need to read from dataSource and write to our target file
            // We can wrap the DataSource in an InputStream to reuse existing save logic
            val inputStream = androidx.media3.datasource.DataSourceInputStream(dataSource, dataSpec)
            
            val downloadedUri = saveFileWithProgress(song.id, song.artist, song.title, inputStream, contentLength, song.customFolderPath) { progress ->
                 _downloadProgress.value = _downloadProgress.value + (song.id to progress)
            }
            
            dataSource.close()
            
            if (downloadedUri == null) {
                _downloadingIds.update { it - song.id }
                _downloadProgress.update { it - song.id }
                return false
            }
            
             Log.d(TAG, "Download complete: saved to $downloadedUri")

            // Download high-quality thumbnail if available (same as before)
            var localThumbnailUrl = song.thumbnailUrl
            if (!song.thumbnailUrl.isNullOrEmpty() && song.thumbnailUrl.startsWith("http")) {
                try {
                    val highResThumbnailUrl = getHighResThumbnailUrl(song.thumbnailUrl, song.id)
                    val thumbRequest = Request.Builder().url(highResThumbnailUrl).build()
                    val thumbResponse = downloadClient.newCall(thumbRequest).execute()
                    if (thumbResponse.isSuccessful) {
                        val thumbnailsDir = File(context.filesDir, "thumbnails")
                         if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
                        val thumbFile = File(thumbnailsDir, "${song.id}.jpg")
                        val thumbBytes = thumbResponse.body?.bytes()
                        if (thumbBytes != null) {
                             FileOutputStream(thumbFile).use { output ->
                                output.write(thumbBytes)
                            }
                            localThumbnailUrl = thumbFile.toUri().toString()
                        }
                    }
                    thumbResponse.close()
                } catch (e: Exception) {
                     Log.e(TAG, "Failed to download thumbnail", e)
                }
            }

            // Create downloaded song entry
            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = downloadedUri,
                thumbnailUrl = localThumbnailUrl,
                streamUrl = null,
                originalSource = song.source 
            )

            _downloadedSongs.value = _downloadedSongs.value + downloadedSong
            saveDownloads()
            
            _downloadingIds.value = _downloadingIds.value - song.id
            Log.d(TAG, "Song ${song.title} download successful!")
            true
            
        } catch (e: Exception) {
             Log.e(TAG, "Error in downloadUsingSharedCache", e)
             _downloadingIds.value = _downloadingIds.value - song.id
             _downloadProgress.value = _downloadProgress.value - song.id
             false
        }
    }

    /**
     * Progressive download with playback callback.
     * Downloads first ~30 seconds, triggers onReadyToPlay, then continues downloading.
     * This enables "play while downloading" feature.
     * 
     * @param song The song to download
     * @param onReadyToPlay Callback when first chunk is ready for playback (receives temp file URI)
     * @return true if download completed successfully
     */
    suspend fun downloadSongProgressive(
        song: Song,
        onReadyToPlay: (android.net.Uri) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (_downloadedSongs.value.any { it.id == song.id }) {
            Log.d(TAG, "Song ${song.id} already downloaded")
            // Already downloaded, play from existing file
            _downloadedSongs.value.find { it.id == song.id }?.localUri?.let { uri ->
                withContext(Dispatchers.Main) { onReadyToPlay(uri) }
            }
            return@withContext true
        }
        
        // Mark as downloading
        _downloadingIds.value = _downloadingIds.value + song.id
        _downloadProgress.value = _downloadProgress.value + (song.id to 0f)
        Log.d(TAG, "Starting progressive download for: ${song.title}")
        
        try {
            // Get stream URL
            val streamUrl = youTubeRepository.getStreamUrlForDownload(song.id)
            if (streamUrl == null) {
                Log.e(TAG, "Failed to get stream URL for ${song.id}")
                _downloadingIds.value = _downloadingIds.value - song.id
                _downloadProgress.value = _downloadProgress.value - song.id
                return@withContext false
            }
            
            // Create temp file for progressive download
            val tempDir = File(context.cacheDir, "progressive_downloads")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, "${song.id}.m4a.tmp")
            
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity")
                .build()
            
            val response = downloadClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Progressive download request failed: ${response.code}")
                response.close()
                _downloadingIds.value = _downloadingIds.value - song.id
                _downloadProgress.value = _downloadProgress.value - song.id
                return@withContext false
            }
            
            val contentLength = response.body?.contentLength() ?: -1L
            Log.d(TAG, "Content length: $contentLength bytes")
            
            // Estimate bytes for 30 seconds (assuming ~128kbps = 16KB/s)
            // 30 seconds = ~480KB minimum to start playback
            val minBytesForPlayback = 480 * 1024L
            var playbackTriggered = false
            var totalBytesRead = 0L
            
            response.body?.byteStream()?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192) // 8KB buffer for smooth progress
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        // Update progress
                        if (contentLength > 0) {
                            val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                            _downloadProgress.value = _downloadProgress.value + (song.id to progress)
                        }
                        
                        // Trigger playback when we have enough data (~30 seconds)
                        if (!playbackTriggered && totalBytesRead >= minBytesForPlayback) {
                            playbackTriggered = true
                            Log.d(TAG, "First chunk ready ($totalBytesRead bytes), triggering playback")
                            withContext(Dispatchers.Main) {
                                onReadyToPlay(tempFile.toUri())
                            }
                        }
                    }
                }
            }
            
            response.close()
            
            // If file was small and playback wasn't triggered, trigger now
            if (!playbackTriggered && tempFile.exists()) {
                Log.d(TAG, "Small file, triggering playback now")
                withContext(Dispatchers.Main) {
                    onReadyToPlay(tempFile.toUri())
                }
            }
            
            // Move temp file to final location
            val finalUri = saveFileToPublicDownloads(song.id, song.artist, song.title, tempFile.inputStream(), song.customFolderPath)
            tempFile.delete()
            
            if (finalUri == null) {
                Log.e(TAG, "Failed to save final file")
                _downloadingIds.value = _downloadingIds.value - song.id
                _downloadProgress.value = _downloadProgress.value - song.id
                return@withContext false
            }
            
            Log.d(TAG, "Progressive download complete: $finalUri")
            
            // Download thumbnail
            var localThumbnailUrl = song.thumbnailUrl
            if (!song.thumbnailUrl.isNullOrEmpty() && song.thumbnailUrl.startsWith("http")) {
                try {
                    val highResThumbnailUrl = getHighResThumbnailUrl(song.thumbnailUrl, song.id)
                    val thumbRequest = Request.Builder().url(highResThumbnailUrl).build()
                    val thumbResponse = downloadClient.newCall(thumbRequest).execute()
                    if (thumbResponse.isSuccessful) {
                        val thumbnailsDir = File(context.filesDir, "thumbnails")
                        if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
                        val thumbFile = File(thumbnailsDir, "${song.id}.jpg")
                        thumbResponse.body?.bytes()?.let { bytes ->
                            FileOutputStream(thumbFile).use { it.write(bytes) }
                            localThumbnailUrl = thumbFile.toUri().toString()
                        }
                    }
                    thumbResponse.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download thumbnail", e)
                }
            }
            
            // Save to downloaded songs
            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = finalUri,
                thumbnailUrl = localThumbnailUrl,
                streamUrl = null,
                originalSource = song.source // Preserve original source for credits
            )
            
            _downloadedSongs.value = _downloadedSongs.value + downloadedSong
            saveDownloads()
            
            _downloadingIds.value = _downloadingIds.value - song.id
            _downloadProgress.value = _downloadProgress.value - song.id
            Log.d(TAG, "Song ${song.title} progressive download successful!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Progressive download error for ${song.id}", e)
            _downloadingIds.update { it - song.id }
            _downloadProgress.update { it - song.id }
            false
        } finally {
             activeDownloadJobs.remove(song.id)
        }
    }

    /**
     * Cancel a specific download by ID.
     * If it's queued, remove it.
     * If it's running, cancel the job.
     */
    fun cancelDownload(songId: String) {
        // 1. Check active jobs
        activeDownloadJobs[songId]?.cancel()
        
        // 2. Check queue
        val iterator = downloadQueue.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().id == songId) {
                iterator.remove()
                break
            }
        }
        
        // 3. Update state
        _downloadingIds.update { it - songId }
        _downloadProgress.update { it - songId }
        
        Log.d(TAG, "Cancelled download: $songId")
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val song = _downloadedSongs.value.find { it.id == songId } ?: return@withContext
        
        try {
            // Try to delete the audio file
            var deleted = false
            
            song.localUri?.let { uri ->
                when (uri.scheme) {
                    "file" -> {
                        val file = File(uri.path ?: "")
                        if (file.exists()) {
                            deleted = file.delete()
                            Log.d(TAG, "Deleted file via file:// scheme: $deleted")
                        }
                    }
                    "content" -> {
                        // For content:// URIs, try ContentResolver first
                        try {
                            val rowsDeleted = context.contentResolver.delete(uri, null, null)
                            deleted = rowsDeleted > 0
                            Log.d(TAG, "Deleted via ContentResolver: $deleted (rows: $rowsDeleted)")
                        } catch (e: Exception) {
                            Log.w(TAG, "ContentResolver delete failed", e)
                        }
                    }
                }
            }
            
            // If URI delete didn't work, try finding the file in public Music folder
            if (!deleted) {
                deleted = deleteFromFolder(getPublicMusicFolder(), song)
            }
            
            // Also try legacy Downloads folder
            if (!deleted) {
                deleted = deleteFromFolder(getLegacyDownloadsFolder(), song)
            }
            
            // Delete local thumbnail if exists
            val thumbnailsDir = File(context.filesDir, "thumbnails")
            val thumbFile = File(thumbnailsDir, "${songId}.jpg")
            if (thumbFile.exists()) {
                thumbFile.delete()
                Log.d(TAG, "Deleted thumbnail: ${thumbFile.name}")
            }
            
            // Also try to delete thumbnail from public folders (if it was saved there before)
            try {
                listOf(getPublicMusicFolder(), getLegacyDownloadsFolder()).forEach { publicFolder ->
                    val thumbFileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.jpg"
                    val publicThumbFile = File(publicFolder, thumbFileName)
                    if (publicThumbFile.exists()) {
                        publicThumbFile.delete()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            
            // Remove from list and save
            _downloadedSongs.value = _downloadedSongs.value.filter { it.id != songId }
            saveDownloads()
            Log.d(TAG, "Deleted download from list: $songId (file deleted: $deleted)")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting download", e)
            // Still remove from list even if file delete failed
            _downloadedSongs.value = _downloadedSongs.value.filter { it.id != songId }
            saveDownloads()
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
    
    /**
     * Get high resolution thumbnail URL.
     * Handles various YouTube thumbnail URL formats.
     */
    private fun getHighResThumbnailUrl(originalUrl: String, videoId: String): String {
        return when {
            // Handle lh3.googleusercontent.com URLs (YT Music style)
            originalUrl.contains("lh3.googleusercontent.com") || originalUrl.contains("yt3.ggpht.com") -> {
                originalUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544")
                    .replace(Regex("=s\\d+.*"), "=s544")
            }
            // Handle ytimg.com URLs
            originalUrl.contains("ytimg.com") || originalUrl.contains("youtube.com") -> {
                val ytVideoId = if (originalUrl.contains("/vi/")) {
                    originalUrl.substringAfter("/vi/").substringBefore("/")
                } else videoId
                "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg"
            }
            else -> originalUrl
        }
    }
    
    // --- Storage Management ---
    
    /**
     * Data class for storage breakdown information.
     */
    data class StorageInfo(
        val downloadedSongsBytes: Long = 0L,
        val downloadedSongsCount: Int = 0,
        val thumbnailsBytes: Long = 0L,
        val cacheBytes: Long = 0L,
        val totalBytes: Long = 0L
    ) {
        fun formatSize(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
    
    /**
     * Get detailed storage usage breakdown.
     */
    fun getStorageInfo(): StorageInfo {
        var downloadedSongsBytes = 0L
        var thumbnailsBytes = 0L
        var cacheBytes = 0L
        
        // Calculate downloaded songs size
        val publicFolder = getPublicMusicFolder()
        if (publicFolder.exists()) {
            publicFolder.listFiles()?.forEach { file ->
                if (file.isFile) {
                    downloadedSongsBytes += file.length()
                }
            }
        }
        
        // Calculate thumbnails size
        val thumbnailsDir = File(context.filesDir, "thumbnails")
        if (thumbnailsDir.exists()) {
            thumbnailsDir.listFiles()?.forEach { file ->
                thumbnailsBytes += file.length()
            }
        }
        
        // Calculate cache size (progressive downloads temp files)
        val progressiveCacheDir = File(context.cacheDir, "progressive_downloads")
        if (progressiveCacheDir.exists()) {
            progressiveCacheDir.listFiles()?.forEach { file ->
                cacheBytes += file.length()
            }
        }
        
        // Also count image cache from Coil
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isDirectory && (file.name.contains("coil") || file.name.contains("image"))) {
                file.walkTopDown().forEach { cacheFile ->
                    if (cacheFile.isFile) {
                        cacheBytes += cacheFile.length()
                    }
                }
            }
        }
        
        return StorageInfo(
            downloadedSongsBytes = downloadedSongsBytes,
            downloadedSongsCount = _downloadedSongs.value.size,
            thumbnailsBytes = thumbnailsBytes,
            cacheBytes = cacheBytes,
            totalBytes = downloadedSongsBytes + thumbnailsBytes + cacheBytes
        )
    }
    
    /**
     * Clear cached files (thumbnails and temp downloads).
     * Does NOT delete downloaded songs.
     */
    fun clearCache() {
        // Clear thumbnails
        val thumbnailsDir = File(context.filesDir, "thumbnails")
        if (thumbnailsDir.exists()) {
            thumbnailsDir.deleteRecursively()
            thumbnailsDir.mkdirs()
        }
        
        // Clear progressive downloads cache
        val progressiveCacheDir = File(context.cacheDir, "progressive_downloads")
        if (progressiveCacheDir.exists()) {
            progressiveCacheDir.deleteRecursively()
        }
        
        // Clear Coil image cache
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isDirectory && (file.name.contains("coil") || file.name.contains("image"))) {
                file.deleteRecursively()
            }
        }
        
        Log.d(TAG, "Cache cleared")
    }

    // Batch Download Queue functions
    // We expect the Service to process this queue.
    
    fun getNextFromQueue(): Song? {
        val song = downloadQueue.peek()
        return song
    }
    
    fun popFromQueue(): Song? {
        val song = downloadQueue.poll()
        _queueState.value = downloadQueue.toList()
        return song
    }
    
    fun updateBatchProgress(current: Int, total: Int) {
        _batchProgress.value = current to total
    }

    /**
     * Add songs to queue and start service.
     */
    fun downloadSongs(songs: List<Song>) {
        // Filter duplicates
        val newSongs = songs.filter { song -> 
            _downloadedSongs.value.none { it.id == song.id } && 
            downloadQueue.none { it.id == song.id } &&
            !_downloadingIds.value.contains(song.id)
        }
        
        if (newSongs.isEmpty()) return
        
        val wasEmpty = downloadQueue.isEmpty()
        downloadQueue.addAll(newSongs)
        _queueState.value = downloadQueue.toList()
        
        // Update batch total if starting new or adding
        // If we were 0/0, now we are 0/newSize
        // If we were 3/10, and added 5, we are 3/15
        val currentTotal = _batchProgress.value.second
        val currentDone = _batchProgress.value.first
        
        if (currentTotal == 0 || currentDone >= currentTotal) {
            _batchProgress.value = 0 to newSongs.size
        } else {
            _batchProgress.value = currentDone to (currentTotal + newSongs.size)
        }
        
        // Start Service to process
        DownloadService.startBatchDownload(context)
    }
    
    // Alias for single song
    fun downloadSongToQueue(song: Song) {
        downloadSongs(listOf(song))
    }
    
    suspend fun deleteDownloads(songIds: List<String>) {
        withContext(Dispatchers.IO) {
            songIds.forEach { id ->
                deleteDownload(id)
            }
        }
    }
    
    /**
     * Delete all downloaded songs.
     */
    suspend fun deleteAllDownloads() = withContext(Dispatchers.IO) {
        val songs = _downloadedSongs.value.toList()
        
        songs.forEach { song ->
            try {
                deleteDownload(song.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting ${song.id}", e)
            }
        }
        
        // Clean up legacy/orphan files
        try {
            listOf(getPublicMusicFolder(), getLegacyDownloadsFolder()).forEach { publicFolder ->
                if (publicFolder.exists()) {
                    publicFolder.listFiles()?.forEach { file ->
                        if (file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac")) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        
        _downloadedSongs.value = emptyList()
        saveDownloads()
        Log.d(TAG, "All downloads deleted")
    }


    fun downloadAlbum(album: com.suvojeet.suvmusic.data.model.Album) {
        val songsToDownload = album.songs.map { song ->
            song.copy(
                customFolderPath = album.title,
                collectionId = album.id,
                collectionName = album.title,
                thumbnailUrl = song.thumbnailUrl ?: album.thumbnailUrl // Fallback to album art
            )
        }
        downloadSongs(songsToDownload)
    }

    fun downloadPlaylist(playlist: com.suvojeet.suvmusic.data.model.Playlist) {
        val songsToDownload = playlist.songs.map { song ->
            song.copy(
                customFolderPath = playlist.title,
                collectionId = playlist.id,
                collectionName = playlist.title,
                // Ensure playlist thumbnail isn't applied to every song unless missing
                thumbnailUrl = song.thumbnailUrl ?: playlist.thumbnailUrl  
            )
        }
        downloadSongs(songsToDownload)
    }
}

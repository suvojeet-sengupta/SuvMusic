package com.suvojeet.suvmusic.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.service.DownloadService
import com.suvojeet.suvmusic.util.TaggingUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    @param:ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository,
    private val remoteAudioRepository: RemoteAudioRepository,
    private val sessionManager: com.suvojeet.suvmusic.data.SessionManager,
    @param:com.suvojeet.suvmusic.di.DownloadDataSource private val dataSourceFactory: androidx.media3.datasource.DataSource.Factory
) {
    companion object {
        private const val TAG = "DownloadRepository"
        private const val SUVMUSIC_FOLDER = "SuvMusic"
        // Dedicated logcat tag for end-to-end download tracing.
        // `adb logcat -s DownloadTrace:V` to capture all download events.
        private const val DL_TAG = "DownloadTrace"
    }
    
    private val gson = com.google.gson.GsonBuilder()
        .registerTypeAdapter(Uri::class.java, com.suvojeet.suvmusic.util.UriTypeAdapter())
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
    private val downloadQueue = java.util.concurrent.ConcurrentLinkedDeque<Song>()
    // Track active jobs for cancellation
    private val activeDownloadJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val _queueState = MutableStateFlow<List<Song>>(emptyList())
    val queueState: StateFlow<List<Song>> = _queueState.asStateFlow()
    
    private val _batchProgress = MutableStateFlow<Pair<Int, Int>>(0 to 0) // current, total
    val batchProgress: StateFlow<Pair<Int, Int>> = _batchProgress.asStateFlow()

    private val initScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    init {
        initScope.launch {
            loadDownloads()
            migrateOldDownloads()
            scanDownloadsFolder()
        }
    }

    private suspend fun scanDownloadsFolder() {
        try {
            val currentSongs = _downloadedSongs.value.toMutableList()
            var hasNewSongs = false

            // 1. Scan default folder recursively
            val defaultFolder = getPublicMusicFolder()
            if (defaultFolder.exists()) {
                scanFolderRecursive(defaultFolder, currentSongs).let { hasNewSongs = hasNewSongs || it }
            }
            
            // 2. Scan legacy folder recursively and migrate
            scanAndMigrateLegacyFolder()
            
            // 3. Scan custom folder if set
            val customLocationUri = sessionManager.getDownloadLocation()
            if (customLocationUri != null) {
                try {
                    val rootUri = Uri.parse(customLocationUri)
                    val rootFolder = DocumentFile.fromTreeUri(context, rootUri)
                    if (rootFolder != null && rootFolder.exists()) {
                        scanDocumentFolderRecursive(rootFolder, currentSongs).let { hasNewSongs = hasNewSongs || it }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error scanning custom folder", e)
                }
            }
            
            if (hasNewSongs) {
                _downloadedSongs.value = currentSongs
                saveDownloads()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning downloads folder", e)
        }
    }

    private suspend fun scanFolderRecursive(folder: File, currentSongs: MutableList<Song>, collectionName: String? = null): Boolean {
        var hasNew = false
        val files = folder.listFiles() ?: return false

        for (file in files) {
            // Cooperative cancellation: a deep tree on the IO pool would otherwise
            // run to completion even after the caller's scope is cancelled.
            currentCoroutineContext().ensureActive()
            if (file.isDirectory) {
                // Use subfolder name as collection name
                if (scanFolderRecursive(file, currentSongs, file.name)) hasNew = true
            } else if (file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac", "wav", "ogg", "opus")) {
                if (processScannedFile(file.name, file.toUri(), currentSongs, collectionName)) hasNew = true
            }
        }
        return hasNew
    }

    private fun scanDocumentFolderRecursive(folder: DocumentFile, currentSongs: MutableList<Song>, collectionName: String? = null): Boolean {
        var hasNew = false
        folder.listFiles().forEach { file: DocumentFile ->
            if (file.isDirectory) {
                if (scanDocumentFolderRecursive(file, currentSongs, file.name)) hasNew = true
            } else if (file.isFile && (file.name?.substringAfterLast('.', "")?.lowercase() ?: "") in listOf("m4a", "mp3", "aac", "flac", "wav", "ogg", "opus")) {
                if (processScannedFile(file.name ?: "Unknown", file.uri, currentSongs, collectionName)) hasNew = true
            }
        }
        return hasNew
    }

    private fun processScannedFile(fileName: String, uri: Uri, currentSongs: MutableList<Song>, collectionName: String? = null): Boolean {
        val nameWithoutExt = fileName.substringBeforeLast('.')
        val parts = nameWithoutExt.split(" - ", limit = 2)
        val title = parts.getOrElse(0) { nameWithoutExt }.trim()
        val artist = parts.getOrElse(1) { "Unknown Artist" }.trim()

        val uriString = uri.toString()
        val uriPath = uri.path
        val existingSongIndex = currentSongs.indexOfFirst { song ->
            val songLocal = song.localUri
            if (songLocal == uriString) return@indexOfFirst true
            if (songLocal != null && Uri.parse(songLocal).path == uriPath) return@indexOfFirst true
            val expectedFileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}"
            if (nameWithoutExt == expectedFileName) return@indexOfFirst true
            if (song.title.equals(title, ignoreCase = true) && song.artist.equals(artist, ignoreCase = true)) return@indexOfFirst true
            false
        }

        if (existingSongIndex != -1) {
            val existingSong = currentSongs[existingSongIndex]
            // Update collection info if it was missing but now found in a folder
            if (collectionName != null && (existingSong.collectionId == null || existingSong.collectionName != collectionName)) {
                currentSongs[existingSongIndex] = existingSong.copy(
                    collectionId = "folder_$collectionName",
                    collectionName = collectionName,
                    customFolderPath = collectionName,
                    album = if (existingSong.album == "Downloads" || existingSong.album == "Unknown Album") collectionName else existingSong.album
                )
                return true
            }
            return false
        }

        val song = Song(
            id = "local_${fileName.hashCode()}_${System.currentTimeMillis()}",
            title = title,
            artist = artist,
            album = collectionName ?: "Downloads",
            duration = 0L,
            thumbnailUrl = null,
            source = SongSource.DOWNLOADED,
            streamUrl = null,
            localUri = uriString,
            collectionId = collectionName?.let { "folder_$it" },
            collectionName = collectionName,
            customFolderPath = collectionName
        )
        currentSongs.add(song)
        return true
    }

    private fun getPublicMusicFolder(): File {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val suvMusicDir = File(musicDir, SUVMUSIC_FOLDER)
        if (!suvMusicDir.exists()) {
            suvMusicDir.mkdirs()
        }
        return suvMusicDir
    }
    
    private fun getLegacyDownloadsFolder(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, SUVMUSIC_FOLDER)
    }
    
    private fun scanAndMigrateLegacyFolder() {
        try {
            val legacyFolder = getLegacyDownloadsFolder()
            if (!legacyFolder.exists()) return
            
            val newFolder = getPublicMusicFolder()
            migrateFolderRecursive(legacyFolder, newFolder)
            
            if (legacyFolder.exists() && legacyFolder.listFiles()?.isEmpty() == true) {
                legacyFolder.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in legacy folder migration", e)
        }
    }

    private fun migrateFolderRecursive(source: File, target: File) {
        val files = source.listFiles() ?: return
        if (!target.exists()) target.mkdirs()
        
        for (file in files) {
            val targetFile = File(target, file.name)
            if (file.isDirectory) {
                migrateFolderRecursive(file, targetFile)
                if (file.listFiles()?.isEmpty() == true) file.delete()
            } else if (file.isFile) {
                try {
                    if (!targetFile.exists()) {
                        file.copyTo(targetFile, overwrite = false)
                    }
                    file.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error migrating file: ${file.name}", e)
                }
            }
        }
    }
    
    private fun deleteFromFolder(folder: File, song: Song): Boolean {
        if (!folder.exists()) return false
        
        try {
            val searchFolders = mutableListOf<File>()
            
            song.customFolderPath?.let { sub ->
                val subFolder = File(folder, sanitizeFileName(sub))
                if (subFolder.exists() && subFolder.isDirectory) {
                    searchFolders.add(subFolder)
                }
            }
            searchFolders.add(folder)

            for (targetFolder in searchFolders) {
                val fileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.m4a"
                val file = File(targetFolder, fileName)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted && targetFolder != folder) {
                        try {
                            if (targetFolder.listFiles()?.isEmpty() == true) {
                                targetFolder.delete()
                            }
                        } catch (e: Exception) {}
                    }
                    if (deleted) return true
                }
                
                val extensions = listOf("mp3", "aac", "flac", "wav", "ogg", "opus")
                for (ext in extensions) {
                    val altFile = File(targetFolder, "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.$ext")
                    if (altFile.exists()) {
                        val deleted = altFile.delete()
                        if (deleted) {
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
                
                targetFolder.listFiles()?.forEach { f ->
                    if (f.nameWithoutExtension.contains(song.title.take(20), ignoreCase = true)) {
                        val deleted = f.delete()
                        if (deleted) {
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

    private suspend fun migrateOldDownloads() {
        if (!oldDownloadsDir.exists()) return
        
        val oldFiles = oldDownloadsDir.listFiles() ?: return
        if (oldFiles.isEmpty()) return
        
        val currentSongs = _downloadedSongs.value.toMutableList()
        var migrated = false
        
        for (oldFile in oldFiles) {
            try {
                val songId = oldFile.nameWithoutExtension
                val song = currentSongs.find { it.id == songId }
                
                if (song != null) {
                    val newUri = saveFileToPublicDownloads(songId, song.artist, song.title, oldFile.inputStream())
                    
                    if (newUri != null) {
                        val index = currentSongs.indexOfFirst { it.id == songId }
                        if (index >= 0) {
                            currentSongs[index] = song.copy(localUri = newUri.toString())
                            migrated = true
                        }
                        oldFile.delete()
                    }
                } else {
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
        
        if (oldDownloadsDir.listFiles()?.isEmpty() == true) {
            oldDownloadsDir.delete()
        }
    }

    private suspend fun saveFileToPublicDownloads(songId: String, artist: String, title: String, inputStream: InputStream, subfolder: String? = null, extension: String = "m4a"): Uri? {
        val fileName = "${sanitizeFileName(title)} - ${sanitizeFileName(artist)}.$extension"

        val customLocationUri = sessionManager.getDownloadLocation()
        if (customLocationUri != null) {
            return saveToCustomLocation(fileName, inputStream, customLocationUri, subfolder, extension)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(songId, fileName, inputStream, subfolder, extension)
        } else {
            saveToPublicFolder(songId, fileName, inputStream, subfolder)
        }
    }

    private fun saveToCustomLocation(fileName: String, inputStream: InputStream, treeUri: String, subfolder: String? = null, extension: String = "m4a"): Uri? {
        return try {
            val rootUri = Uri.parse(treeUri)
            var rootFolder = DocumentFile.fromTreeUri(context, rootUri) ?: return null

            if (subfolder != null) {
                val sanitizedSub = sanitizeFileName(subfolder)
                var subDir = rootFolder.findFile(sanitizedSub)
                if (subDir == null || !subDir.isDirectory) {
                    subDir = rootFolder.createDirectory(sanitizedSub)
                }
                if (subDir != null) {
                    rootFolder = subDir
                }
            }

            val existingFile = rootFolder.findFile(fileName)
            existingFile?.delete()

            val mimeType = if (extension == "opus") "audio/opus" else "audio/m4a"
            val newFile = rootFolder.createFile(mimeType, fileName) ?: return null

            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                inputStream.copyTo(output)
            }

            newFile.uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to custom location", e)
            null
        }
    }

    private fun saveToMediaStore(songId: String, fileName: String, inputStream: InputStream, subfolder: String? = null, extension: String = "m4a"): Uri? {
        return try {
            val relativePath = if (subfolder != null) {
                "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER/${sanitizeFileName(subfolder)}"
            } else {
                "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER"
            }

            val mimeType = if (extension == "opus") "audio/opus" else "audio/m4a"
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { mediaUri ->
                resolver.openOutputStream(mediaUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(mediaUri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            android.util.Log.e(
                DL_TAG,
                "[SAVE_MS] EXCEPTION fileName=$fileName msg=${e.message} — falling back to public folder",
                e,
            )
            Log.e(TAG, "Error saving to MediaStore", e)
            saveToPublicFolder(songId, fileName, inputStream, subfolder)
        }
    }

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
            file.toUri()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Music folder", e)
            null
        }
    }

    private suspend fun saveFileWithProgress(
        songId: String,
        artist: String,
        title: String,
        inputStream: InputStream,
        contentLength: Long,
        subfolder: String? = null,
        onProgress: (Float) -> Unit,
        extension: String = "m4a"
    ): Uri? {
        val fileName = "${sanitizeFileName(title)} - ${sanitizeFileName(artist)}.$extension"

        val customLocationUri = sessionManager.getDownloadLocation()
        if (customLocationUri != null) {
            return saveToCustomLocationWithProgress(fileName, inputStream, contentLength, customLocationUri, subfolder, onProgress, extension)
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStoreWithProgress(fileName, inputStream, contentLength, subfolder, onProgress, extension)
            } else {
                saveToPublicFolderWithProgress(fileName, inputStream, contentLength, subfolder, onProgress)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file with progress", e)
            null
        }
    }

    private suspend fun saveToCustomLocationWithProgress(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        treeUri: String,
        subfolder: String? = null,
        onProgress: (Float) -> Unit,
        extension: String = "m4a"
    ): Uri? {
        return try {
            val rootUri = Uri.parse(treeUri)
            var rootFolder = DocumentFile.fromTreeUri(context, rootUri) ?: return null

            if (subfolder != null) {
                val sanitizedSub = sanitizeFileName(subfolder)
                var subDir = rootFolder.findFile(sanitizedSub)
                if (subDir == null || !subDir.isDirectory) {
                    subDir = rootFolder.createDirectory(sanitizedSub)
                }
                if (subDir != null) {
                    rootFolder = subDir
                }
            }

            val existingFile = rootFolder.findFile(fileName)
            existingFile?.delete()

            val mimeType = if (extension == "opus") "audio/opus" else "audio/m4a"
            val newFile = rootFolder.createFile(mimeType, fileName) ?: return null

            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                copyWithProgress(inputStream, output, contentLength, onProgress)
            }
            newFile.uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to custom location with progress", e)
            null
        }
    }

    private suspend fun saveToMediaStoreWithProgress(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        subfolder: String? = null,
        onProgress: (Float) -> Unit,
        extension: String = "m4a"
    ): Uri? {
        return try {
            val relativePath = if (subfolder != null) {
                "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER/${sanitizeFileName(subfolder)}"
            } else {
                "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER"
            }

            val mimeType = if (extension == "opus") "audio/opus" else "audio/m4a"
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { mediaUri ->
                resolver.openOutputStream(mediaUri)?.use { outputStream ->
                    copyWithProgress(inputStream, outputStream, contentLength, onProgress)
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(mediaUri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to MediaStore with progress", e)
            null
        }
    }    
    private suspend fun saveToPublicFolderWithProgress(
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
            file.toUri()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Music folder with progress", e)
            null
        }
    }
    
    /**
     * Suspending copy so cancellation actually stops the byte pump.
     *
     * The previous non-suspend version meant a cancel from
     * [cancelDownload] only flipped UI state — the OkHttp/DataSource read
     * loop kept running until the upstream finished, draining bandwidth
     * and storage long after the user had moved on. `ensureActive()` on
     * each chunk converts the next read into a cooperative exit point.
     */
    private suspend fun copyWithProgress(
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
            currentCoroutineContext().ensureActive()
            output.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead

            if (contentLength > 0 && totalBytesRead - lastProgressUpdate > 50 * 1024) {
                val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                onProgress(progress)
                lastProgressUpdate = totalBytesRead
            }
        }

        // Surface "received fewer bytes than expected" as a real failure
        // instead of silently keeping a truncated file. Without this,
        // a TCP RST mid-stream produced a partial m4a that "played" for a
        // few seconds and then ended — the user thought the download had
        // worked. Allow a small tolerance for content-length lies from CDNs.
        if (contentLength > 0 && totalBytesRead < contentLength - 4096) {
            throw java.io.IOException(
                "Truncated download: got $totalBytesRead of $contentLength bytes"
            )
        }

        if (contentLength > 0) {
            onProgress(1f)
        }
    }

    private suspend fun downloadThumbnailBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            downloadClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body.bytes()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download thumbnail bytes", e)
        }
        null
    }

    private suspend fun tagAudioFile(file: File, song: Song) {
        val thumbUrl = song.thumbnailUrl
        val artBytes = if (!thumbUrl.isNullOrEmpty() && thumbUrl.startsWith("http")) {
            val highResUrl = getHighResThumbnailUrl(thumbUrl, song.id)
            downloadThumbnailBytes(highResUrl)
        } else null
        
        withContext(Dispatchers.IO) {
            TaggingUtils.embedMetadata(file, song, artBytes)
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    private fun findFileRecursive(folder: File, fileName: String): File? {
        val files = folder.listFiles() ?: return null
        for (file in files) {
            if (file.isDirectory) {
                val found = findFileRecursive(file, fileName)
                if (found != null) return found
            } else if (file.isFile && file.name == fileName) {
                return file
            }
        }
        return null
    }

    private fun loadDownloads() {
        if (!downloadsMetaFile.exists()) {
            _downloadedSongs.value = emptyList()
            return
        }
        try {
            val atomicFile = androidx.core.util.AtomicFile(downloadsMetaFile)
            val json = try {
                atomicFile.readFully().toString(Charsets.UTF_8)
            } catch (e: Exception) {
                downloadsMetaFile.readText()
            }
            
            val type = object : TypeToken<List<Song>>() {}.type
            val songs: List<Song> = gson.fromJson(json, type) ?: emptyList()
            
            val validSongs = songs.mapNotNull { song ->
                val uri = song.localUri?.let { Uri.parse(it) }
                if (uri != null && uri != Uri.EMPTY) {
                   try {
                        if (uri.scheme == "file") {
                            val file = File(uri.path ?: "")
                            if (file.exists()) return@mapNotNull song
                        } else {
                            context.contentResolver.openInputStream(uri)?.close()
                            return@mapNotNull song
                        }
                    } catch (e: Exception) {}
                }

                try {
                    val folder = getPublicMusicFolder()
                    val fileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.m4a"
                    val file = File(folder, fileName)
                    if (file.exists()) return@mapNotNull song.copy(localUri = file.toUri().toString())

                    // Fallback: search recursively in case it's in a subfolder (album/playlist)
                    val foundFile = findFileRecursive(folder, fileName)
                    if (foundFile != null) return@mapNotNull song.copy(localUri = foundFile.toUri().toString())

                    // Try other extensions
                    val extensions = listOf("mp3", "aac", "flac", "wav", "ogg", "opus")
                    for (ext in extensions) {
                        val altFileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.$ext"
                        val altFoundFile = findFileRecursive(folder, altFileName)
                        if (altFoundFile != null) return@mapNotNull song.copy(localUri = altFoundFile.toUri().toString())
                    }
                } catch (e: Exception) {}
                null
            }
            
            val distinctSongs = validSongs
                .groupBy { "${it.title.trim().lowercase()}-${it.artist.trim().lowercase()}" }
                .map { (_, duplicates) ->
                    duplicates.sortedWith(
                        compareByDescending<Song> { !it.thumbnailUrl.isNullOrEmpty() }
                            .thenByDescending { !it.id.startsWith("local_") }
                    ).first()
                }
            
            _downloadedSongs.value = distinctSongs
            if (distinctSongs.size != songs.size) saveDownloads()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading downloads", e)
            _downloadedSongs.value = emptyList()
        }
    }

    private fun saveDownloads() {
        try {
            val json = gson.toJson(_downloadedSongs.value)
            val atomicFile = androidx.core.util.AtomicFile(downloadsMetaFile)
            var fos: java.io.FileOutputStream? = null
            try {
                fos = atomicFile.startWrite()
                fos.write(json.toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(fos)
            } catch (e: Exception) {
                // Guard the null case: if startWrite() itself threw, fos
                // is still null and failWrite(null) NPE's on Android < 30.
                if (fos != null) {
                    try { atomicFile.failWrite(fos) } catch (_: Exception) {}
                }
                Log.e(TAG, "AtomicFile write failed", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving downloads", e)
        }
    }

    private val downloadMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Download a single song. Returns a tri-state outcome:
     *  - SUCCESS — wrote the file, song now in `downloadedSongs`.
     *  - SKIPPED — already downloaded or already in flight; no work done.
     *  - FAILED  — stream URL or copy failed; state cleaned up.
     *
     * Previously returned `Boolean` and used `true` for both SUCCESS and
     * SKIPPED, which made [DownloadService] post a "Download complete"
     * notification for songs the user had already downloaded.
     */
    suspend fun downloadSong(song: Song): com.suvojeet.suvmusic.core.model.DownloadResult =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            android.util.Log.i(
                DL_TAG,
                "[ENTER] id=${song.id} src=${song.source} title=${song.title} " +
                    "artist=${song.artist} videoMode=${song.isVideo}",
            )

            // Mutex-guarded admission check. We record the in-flight job
            // *inside* the lock so a concurrent cancelDownload() call can't
            // observe a window where _downloadingIds has the id but
            // activeDownloadJobs doesn't.
            val canDownload = downloadMutex.withLock {
                if (_downloadedSongs.value.any { it.id == song.id }) {
                    return@withLock false
                }
                if (_downloadingIds.value.contains(song.id)) {
                    return@withLock false
                }
                _downloadingIds.update { it + song.id }
                val job = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
                if (job != null) activeDownloadJobs[song.id] = job
                true
            }

            if (!canDownload) return@withContext com.suvojeet.suvmusic.core.model.DownloadResult.SKIPPED

            try {
                val rt0 = System.currentTimeMillis()
                android.util.Log.i(
                    DL_TAG,
                    "[STREAM_URL] fetch start id=${song.id} src=${song.source}",
                )
                // Resolve the stream URL with a bounded retry. Bulk library downloads
                // (60+ songs) repeatedly hit YouTube / RemoteAudio and can get briefly
                // throttled or rate-limited (429). A single null used to fail the song
                // outright — which is why large batches "downloaded to ~50-60% then said
                // download failed": once throttling kicked in, every remaining song failed
                // immediately. A short backoff rides out transient throttle windows so the
                // batch completes instead of cascading into failures.
                var streamResult: Pair<String, String>? = null
                var streamAttempt = 0
                val maxStreamAttempts = 3
                while (streamAttempt < maxStreamAttempts) {
                    streamAttempt++
                    streamResult = when (song.source) {
                        SongSource.REMOTE -> {
                            val url = remoteAudioRepository.getStreamUrl(song.id, 320)
                            if (url != null) url to "m4a" else null
                        }
                        else -> youTubeRepository.getStreamUrlForDownload(song.id)
                    }
                    if (streamResult != null) break
                    if (streamAttempt < maxStreamAttempts) {
                        val backoff = 4000L * streamAttempt // 4s, then 8s
                        android.util.Log.w(
                            DL_TAG,
                            "[STREAM_URL] null id=${song.id} src=${song.source} " +
                                "attempt=$streamAttempt/$maxStreamAttempts — retrying in ${backoff}ms",
                        )
                        kotlinx.coroutines.delay(backoff)
                    }
                }
                val streamElapsed = System.currentTimeMillis() - rt0
                if (streamResult == null) {
                    android.util.Log.e(
                        DL_TAG,
                        "[STREAM_URL] FAILED id=${song.id} src=${song.source} " +
                            "elapsed=${streamElapsed}ms totalElapsed=${System.currentTimeMillis() - t0}ms",
                    )
                    clearInFlight(song.id)
                    return@withContext com.suvojeet.suvmusic.core.model.DownloadResult.FAILED
                }

                val (streamUrl, extension) = streamResult
                android.util.Log.i(
                    DL_TAG,
                    "[STREAM_URL] OK id=${song.id} extension=$extension " +
                        "urlPrefix=${streamUrl.take(80)} elapsed=${streamElapsed}ms",
                )

                val ok = downloadUsingSharedCache(song, streamUrl, extension)
                val total = System.currentTimeMillis() - t0
                if (ok) {
                    com.suvojeet.suvmusic.core.model.DownloadResult.SUCCESS
                } else {
                    com.suvojeet.suvmusic.core.model.DownloadResult.FAILED
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancellation flows through; clear state but don't log as
                // an error and let the exception propagate so the parent
                // coroutine knows the job stopped.
                android.util.Log.w(
                    DL_TAG,
                    "[CANCELLED] id=${song.id} totalElapsed=${System.currentTimeMillis() - t0}ms",
                )
                clearInFlight(song.id)
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    DL_TAG,
                    "[EXCEPTION] downloadSong id=${song.id} causeType=${e.javaClass.simpleName} " +
                        "msg=${e.message} totalElapsed=${System.currentTimeMillis() - t0}ms",
                    e,
                )
                Log.e(TAG, "Download error for ${song.id}", e)
                clearInFlight(song.id)
                com.suvojeet.suvmusic.core.model.DownloadResult.FAILED
            } finally {
                activeDownloadJobs.remove(song.id)
            }
        }

    /**
     * Single cleanup path so success/failure/cancellation all leave the
     * same state behind — no orphaned entries in any of the three flows.
     */
    private fun clearInFlight(songId: String) {
        _downloadingIds.update { it - songId }
        _downloadProgress.update { it - songId }
    }

    private suspend fun downloadUsingSharedCache(
        song: Song,
        streamUrl: String,
        extension: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val ct0 = System.currentTimeMillis()
        val uri = android.net.Uri.parse(streamUrl)
        val dataSpec = androidx.media3.datasource.DataSpec.Builder()
            .setUri(uri)
            .setKey(song.id)
            .build()

        val tempFile = File(context.cacheDir, "${song.id}_download.$extension")
        var dataSource: androidx.media3.datasource.DataSource? = null

        try {
            dataSource = dataSourceFactory.createDataSource()
            val openT0 = System.currentTimeMillis()
            val length = dataSource.open(dataSpec)
            val contentLength = if (length != androidx.media3.common.C.LENGTH_UNSET.toLong()) length else -1L
            android.util.Log.i(
                DL_TAG,
                "[OPEN] OK id=${song.id} contentLength=$contentLength " +
                    "openMs=${System.currentTimeMillis() - openT0}",
            )

            _downloadProgress.update { it + (song.id to 0f) }

            val copyT0 = System.currentTimeMillis()
            // Wrap the already-opened DataSource in a plain InputStream.
            // We CANNOT use androidx.media3.datasource.DataSourceInputStream
            // here: its checkOpened() lazily calls dataSource.open(dataSpec)
            // on the first read, and CronetDataSource.open() has
            // Assertions.checkState(!opened) which throws IllegalStateException
            // because we already opened it above to read contentLength.
            // That double-open is exactly the [COPY] EXCEPTION we were
            // seeing in the field — every download failing at byte 0.
            val input: InputStream = object : InputStream() {
                private val single = ByteArray(1)
                override fun read(): Int {
                    val n = dataSource!!.read(single, 0, 1)
                    return if (n == androidx.media3.common.C.RESULT_END_OF_INPUT) -1
                    else single[0].toInt() and 0xFF
                }
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val n = dataSource!!.read(b, off, len)
                    return if (n == androidx.media3.common.C.RESULT_END_OF_INPUT) -1 else n
                }
            }
            // .use{} on output so a throw mid-copy (network drop,
            // cancellation, disk full) still releases the file handle.
            // Upstream DataSource is released in the outer finally.
            FileOutputStream(tempFile).use { output ->
                copyWithProgress(input, output, contentLength) { progress ->
                    _downloadProgress.update { it + (song.id to progress) }
                }
            }
            val copyMs = System.currentTimeMillis() - copyT0
            val writtenBytes = tempFile.length()
            val throughputKbps = if (copyMs > 0) (writtenBytes * 8 / copyMs).toInt() else 0
            android.util.Log.i(
                DL_TAG,
                "[COPY] OK id=${song.id} bytes=$writtenBytes copyMs=$copyMs " +
                    "throughput=${throughputKbps}kbps",
            )

            val tagT0 = System.currentTimeMillis()
            tagAudioFile(tempFile, song)
            android.util.Log.i(
                DL_TAG,
                "[TAG] OK id=${song.id} tagMs=${System.currentTimeMillis() - tagT0}",
            )

            val saveT0 = System.currentTimeMillis()
            val downloadedUri = tempFile.inputStream().use { input ->
                saveFileToPublicDownloads(
                    song.id, song.artist, song.title, input,
                    song.customFolderPath, extension,
                )
            } ?: run {
                android.util.Log.e(
                    DL_TAG,
                    "[SAVE] FAILED id=${song.id} all destinations returned null " +
                        "elapsed=${System.currentTimeMillis() - saveT0}ms",
                )
                clearInFlight(song.id)
                return@withContext false
            }
            android.util.Log.i(
                DL_TAG,
                "[SAVE] OK id=${song.id} uri=$downloadedUri " +
                    "saveMs=${System.currentTimeMillis() - saveT0}",
            )

            val thumbT0 = System.currentTimeMillis()
            val localThumbnailUrl = downloadAndSaveThumbnail(song) ?: song.thumbnailUrl
            android.util.Log.i(
                DL_TAG,
                "[THUMB] id=${song.id} localResolved=${localThumbnailUrl != song.thumbnailUrl} " +
                    "thumbMs=${System.currentTimeMillis() - thumbT0}",
            )

            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = downloadedUri.toString(),
                thumbnailUrl = localThumbnailUrl,
                streamUrl = null,
                originalSource = song.source,
            )

            // Order matters for UI consistency. The PlayerViewModel
            // observer uses combine(downloadedSongs, downloadingIds) and
            // the `downloaded` branch takes priority over `downloading`.
            // So we publish into `downloadedSongs` FIRST — at that point
            // the combined snapshot is (downloaded ✓, downloading ✓) and
            // the icon flips straight to "downloaded". Clearing the
            // in-flight flags afterward emits another DOWNLOADED snapshot
            // (still downloaded ✓, no longer downloading), so the icon
            // never flickers back through NOT_DOWNLOADED on the way.
            _downloadedSongs.update { it + downloadedSong }
            saveDownloads()
            _downloadingIds.update { it - song.id }
            _downloadProgress.update { it - song.id }
            android.util.Log.i(
                DL_TAG,
                "[PERSIST] OK id=${song.id} totalNow=${_downloadedSongs.value.size} " +
                    "stageElapsed=${System.currentTimeMillis() - ct0}ms",
            )
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Caller cancelled mid-stream. Clean up everything we wrote.
            android.util.Log.w(
                DL_TAG,
                "[COPY] CANCELLED id=${song.id} stageElapsed=${System.currentTimeMillis() - ct0}ms",
            )
            clearInFlight(song.id)
            throw e
        } catch (e: Exception) {
            android.util.Log.e(
                DL_TAG,
                "[COPY] EXCEPTION id=${song.id} causeType=${e.javaClass.simpleName} " +
                    "msg=${e.message} stageElapsed=${System.currentTimeMillis() - ct0}ms",
                e,
            )
            Log.e(TAG, "Error in downloadUsingSharedCache", e)
            clearInFlight(song.id)
            false
        } finally {
            // Always: release upstream and delete the temp file. Without
            // this finally, cancelled/failed downloads orphaned partial
            // .m4a files in cacheDir and the cache grew unbounded over
            // time. Wrap each in its own try so a throw in one doesn't
            // leak the other.
            try { dataSource?.close() } catch (_: Exception) {}
            try { if (tempFile.exists()) tempFile.delete() } catch (_: Exception) {}
        }
    }

    /**
     * Best-effort thumbnail download. Returns the local file:// URI on
     * success, null on failure or when there's no remote URL — caller
     * falls back to the original (possibly remote) thumbnailUrl.
     */
    private suspend fun downloadAndSaveThumbnail(song: Song): String? {
        val remote = song.thumbnailUrl
        if (remote.isNullOrEmpty() || !remote.startsWith("http")) return null
        return try {
            val highResUrl = getHighResThumbnailUrl(remote, song.id)
            val bytes = downloadThumbnailBytes(highResUrl) ?: return null
            val thumbnailsDir = File(context.filesDir, "thumbnails").apply {
                if (!exists()) mkdirs()
            }
            val thumbFile = File(thumbnailsDir, "${song.id}.jpg")
            FileOutputStream(thumbFile).use { it.write(bytes) }
            thumbFile.toUri().toString()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadSongProgressive(
        song: Song,
        onReadyToPlay: (android.net.Uri) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        android.util.Log.i(
            DL_TAG,
            "[PROGRESSIVE] enter id=${song.id} title=${song.title}",
        )
        if (_downloadedSongs.value.any { it.id == song.id }) {
            _downloadedSongs.value.find { it.id == song.id }?.localUri?.let { uri ->
                withContext(Dispatchers.Main) { onReadyToPlay(Uri.parse(uri)) }
            }
            return@withContext true
        }

        // Register the job in activeDownloadJobs so cancelDownload() can
        // actually stop a progressive download. Without this the remove()
        // in finally was a no-op and progressive cancels silently failed.
        _downloadingIds.update { it + song.id }
        _downloadProgress.update { it + (song.id to 0f) }
        kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
            ?.let { activeDownloadJobs[song.id] = it }

        val tempDir = File(context.cacheDir, "progressive_downloads").apply {
            if (!exists()) mkdirs()
        }
        var tempFile: File? = null

        try {
            val streamResult = youTubeRepository.getStreamUrlForDownload(song.id)
            if (streamResult == null) {
                android.util.Log.e(
                    DL_TAG,
                    "[PROGRESSIVE] STREAM_URL_FAIL id=${song.id} elapsed=${System.currentTimeMillis() - t0}ms",
                )
                clearInFlight(song.id)
                return@withContext false
            }

            val (streamUrl, extension) = streamResult
            tempFile = File(tempDir, "${song.id}.$extension.tmp")
            val tmp = tempFile  // smart-cast helper for use inside lambdas

            val finalUri: android.net.Uri? = downloadClient
                .newCall(Request.Builder().url(streamUrl).build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.e(
                            DL_TAG,
                            "[PROGRESSIVE] HTTP_FAIL id=${song.id} code=${response.code}",
                        )
                        return@use null
                    }

                    val contentLength = response.body.contentLength()
                    val minBytesForPlayback = 480 * 1024L
                    var playbackTriggered = false
                    var totalBytesRead = 0L

                    response.body.byteStream().use { inputStream ->
                        FileOutputStream(tmp).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                currentCoroutineContext().ensureActive()
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                if (contentLength > 0) {
                                    val progress = (totalBytesRead.toFloat() / contentLength)
                                        .coerceIn(0f, 1f)
                                    _downloadProgress.update { it + (song.id to progress) }
                                }
                                if (!playbackTriggered && totalBytesRead >= minBytesForPlayback) {
                                    playbackTriggered = true
                                    withContext(Dispatchers.Main) { onReadyToPlay(tmp.toUri()) }
                                }
                            }
                        }
                    }

                    if (contentLength > 0 && totalBytesRead < contentLength - 4096) {
                        throw java.io.IOException(
                            "Truncated progressive download: " +
                                "$totalBytesRead of $contentLength bytes",
                        )
                    }

                    if (!playbackTriggered && tmp.exists()) {
                        withContext(Dispatchers.Main) { onReadyToPlay(tmp.toUri()) }
                    }

                    tagAudioFile(tmp, song)
                    tmp.inputStream().use { input ->
                        saveFileToPublicDownloads(
                            song.id, song.artist, song.title, input,
                            song.customFolderPath, extension,
                        )
                    }
                }

            if (finalUri == null) {
                clearInFlight(song.id)
                return@withContext false
            }

            val localThumbnailUrl = downloadAndSaveThumbnail(song) ?: song.thumbnailUrl

            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = finalUri.toString(),
                thumbnailUrl = localThumbnailUrl,
                streamUrl = null,
                originalSource = song.source,
            )

            // Same ordering rationale as downloadUsingSharedCache:
            // publish to downloadedSongs first so the combine()-based
            // observer never sees a snapshot that's neither downloaded
            // nor downloading (which would flicker the icon back to the
            // default download glyph).
            _downloadedSongs.update { it + downloadedSong }
            saveDownloads()
            _downloadingIds.update { it - song.id }
            _downloadProgress.update { it - song.id }
            android.util.Log.i(
                DL_TAG,
                "[PROGRESSIVE] OK id=${song.id} totalElapsed=${System.currentTimeMillis() - t0}ms",
            )
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.w(
                DL_TAG,
                "[PROGRESSIVE] CANCELLED id=${song.id} totalElapsed=${System.currentTimeMillis() - t0}ms",
            )
            clearInFlight(song.id)
            throw e
        } catch (e: Exception) {
            android.util.Log.e(
                DL_TAG,
                "[PROGRESSIVE] EXCEPTION id=${song.id} causeType=${e.javaClass.simpleName} " +
                    "msg=${e.message} totalElapsed=${System.currentTimeMillis() - t0}ms",
                e,
            )
            Log.e(TAG, "Progressive download error for ${song.id}", e)
            clearInFlight(song.id)
            false
        } finally {
            activeDownloadJobs.remove(song.id)
            try { if (tempFile?.exists() == true) tempFile.delete() } catch (_: Exception) {}
        }
    }

    fun cancelDownload(songId: String) {
        val hadJob = activeDownloadJobs[songId] != null
        activeDownloadJobs[songId]?.cancel()
        var removedFromQueue = false
        val iterator = downloadQueue.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().id == songId) {
                iterator.remove()
                removedFromQueue = true
                break
            }
        }
        _downloadingIds.update { it - songId }
        _downloadProgress.update { it - songId }
        android.util.Log.i(
            DL_TAG,
            "[CANCEL] id=$songId hadActiveJob=$hadJob removedFromQueue=$removedFromQueue",
        )
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val song = _downloadedSongs.value.find { it.id == songId } ?: return@withContext
        try {
            var deleted = false
            song.localUri?.let { uriString ->
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists()) deleted = file.delete()
                } else if (uri.scheme == "content") {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val deleteRequest = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                            throw com.suvojeet.suvmusic.util.FileOperationException.FilePermissionException("Permission needed to delete file", deleteRequest)
                        } else {
                            deleted = context.contentResolver.delete(uri, null, null) > 0
                        }
                    } catch (e: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is android.app.RecoverableSecurityException) {
                            throw com.suvojeet.suvmusic.util.FileOperationException.FilePermissionException(e.message ?: "Permission needed to delete file", e.userAction.actionIntent)
                        } else throw e
                    } catch (e: Exception) {
                        if (e is com.suvojeet.suvmusic.util.FileOperationException.FilePermissionException) throw e
                    }
                }
            }
            if (!deleted) deleted = deleteFromFolder(getPublicMusicFolder(), song)
            if (!deleted) deleted = deleteFromFolder(getLegacyDownloadsFolder(), song)
            
            val thumbnailsDir = File(context.filesDir, "thumbnails")
            val thumbFile = File(thumbnailsDir, "${songId}.jpg")
            if (thumbFile.exists()) thumbFile.delete()
            
            _downloadedSongs.update { songs -> songs.filter { it.id != songId } }
            saveDownloads()
        } catch (e: Exception) {
            if (e is com.suvojeet.suvmusic.util.FileOperationException.FilePermissionException) throw e
            _downloadedSongs.update { songs -> songs.filter { it.id != songId } }
            saveDownloads()
        }
    }
    
    fun isDownloaded(songId: String): Boolean = _downloadedSongs.value.any { it.id == songId }
    fun isDownloading(songId: String): Boolean = _downloadingIds.value.contains(songId)
    fun isVideoDownloaded(songId: String): Boolean = _downloadedSongs.value.any { it.id == songId && it.isVideo }

    suspend fun downloadVideo(song: Song, maxResolution: Int = 720): Boolean = withContext(Dispatchers.IO) {
        val videoKey = "${song.id}_video"
        val canDownload = downloadMutex.withLock {
            if (_downloadedSongs.value.any { it.id == song.id && it.isVideo }) return@withLock false
            if (_downloadingIds.value.contains(videoKey)) return@withLock false
            _downloadingIds.update { it + videoKey }
            true
        }
        if (!canDownload) return@withContext true

        try {
            val muxedUrl = youTubeRepository.getMuxedVideoStreamUrlForDownload(song.id, maxResolution)
            if (muxedUrl == null) {
                downloadMutex.withLock { _downloadingIds.update { it - videoKey } }
                return@withContext false
            }

            val request = Request.Builder().url(muxedUrl).build()
            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                downloadMutex.withLock { _downloadingIds.update { it - videoKey } }
                return@withContext false
            }

            val contentLength = response.body.contentLength()
            _downloadProgress.update { it + (videoKey to 0f) }

            val fileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.mp4"
            val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = "${Environment.DIRECTORY_MOVIES}/SuvMusic"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { videoUri ->
                    resolver.openOutputStream(videoUri)?.use { out ->
                        copyWithProgress(response.body.byteStream(), out, contentLength) { p ->
                            _downloadProgress.update { it + (videoKey to p) }
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(videoUri, contentValues, null, null)
                }
                uri
            } else {
                val videosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "SuvMusic").apply { mkdirs() }
                val file = File(videosDir, fileName)
                FileOutputStream(file).use { out ->
                    copyWithProgress(response.body.byteStream(), out, contentLength) { p ->
                        _downloadProgress.update { it + (videoKey to p) }
                    }
                }
                file.toUri()
            }
            response.close()

            if (savedUri == null) {
                downloadMutex.withLock { _downloadingIds.update { it - videoKey } }
                _downloadProgress.update { it - videoKey }
                return@withContext false
            }

            val currentThumbnailUrl = song.thumbnailUrl
            var localThumbnailUrl = currentThumbnailUrl
            if (!currentThumbnailUrl.isNullOrEmpty() && currentThumbnailUrl.startsWith("http")) {
                try {
                    val highResThumbnailUrl = getHighResThumbnailUrl(currentThumbnailUrl, song.id)
                    val thumbRequest = Request.Builder().url(highResThumbnailUrl).build()
                    downloadClient.newCall(thumbRequest).execute().use { thumbResponse ->
                        if (thumbResponse.isSuccessful) {
                            val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
                            val thumbFile = File(thumbnailsDir, "${song.id}_video.jpg")
                            FileOutputStream(thumbFile).use { it.write(thumbResponse.body.bytes()) }
                            localThumbnailUrl = thumbFile.toUri().toString()
                        }
                    }
                } catch (e: Exception) {}
            }

            val downloadedSong = song.copy(source = SongSource.DOWNLOADED, localUri = savedUri.toString(), thumbnailUrl = localThumbnailUrl, streamUrl = null, originalSource = song.source, isVideo = true)
            _downloadedSongs.update { it + downloadedSong }
            saveDownloads()
            downloadMutex.withLock { _downloadingIds.update { it - videoKey } }
            _downloadProgress.update { it - videoKey }
            true
        } catch (e: Exception) {
            downloadMutex.withLock { _downloadingIds.update { it - videoKey } }
            _downloadProgress.update { it - videoKey }
            false
        }
    }

    fun refreshDownloads() {
        loadDownloads()
        initScope.launch { scanDownloadsFolder() }
    }
    
    fun getDownloadInfo(): Pair<Int, Long> {
        val songs = _downloadedSongs.value
        val totalDuration = songs.fold(0L) { acc, song -> acc + song.duration }
        return Pair(songs.size, totalDuration)
    }
    
    private fun getHighResThumbnailUrl(originalUrl: String, videoId: String): String {
        return when {
            originalUrl.contains("lh3.googleusercontent.com") || originalUrl.contains("yt3.ggpht.com") -> {
                originalUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544").replace(Regex("=s\\d+.*"), "=s544")
            }
            originalUrl.contains("ytimg.com") || originalUrl.contains("youtube.com") -> {
                val ytVideoId = if (originalUrl.contains("/vi/")) originalUrl.substringAfter("/vi/").substringBefore("/") else videoId
                "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg"
            }
            else -> originalUrl
        }
    }
    
    data class StorageInfo(
        val downloadedSongsBytes: Long = 0L,
        val downloadedSongsCount: Int = 0,
        val thumbnailsBytes: Long = 0L,
        val cacheBytes: Long = 0L,
        val progressiveCacheBytes: Long = 0L,
        val imageCacheBytes: Long = 0L,
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
    
    suspend fun getStorageInfo(): StorageInfo {
        var downloadedSongsBytes = 0L
        var thumbnailsBytes = 0L
        var progressiveCacheBytes = 0L
        var imageCacheBytes = 0L
        
        val publicFolder = getPublicMusicFolder()
        if (publicFolder.exists()) {
            publicFolder.listFiles()?.forEach { file ->
                if (file.isFile) downloadedSongsBytes += file.length()
                else if (file.isDirectory) file.walkTopDown().forEach { subFile -> if (subFile.isFile) downloadedSongsBytes += subFile.length() }
            }
        }
        
        val customLocationUri = sessionManager.getDownloadLocation()
        if (customLocationUri != null) {
            try {
                val rootUri = Uri.parse(customLocationUri)
                val rootFolder = DocumentFile.fromTreeUri(context, rootUri)
                if (rootFolder != null && rootFolder.exists()) {
                    rootFolder.listFiles().forEach { file: DocumentFile ->
                        if (file.isFile) downloadedSongsBytes += file.length()
                        else if (file.isDirectory) file.listFiles().forEach { subFile: DocumentFile -> if (subFile.isFile) downloadedSongsBytes += subFile.length() }
                    }
                }
            } catch (e: Exception) {}
        }
        
        val thumbnailsDir = File(context.filesDir, "thumbnails")
        if (thumbnailsDir.exists()) thumbnailsDir.listFiles()?.forEach { file -> thumbnailsBytes += file.length() }
        
        val progressiveCacheDir = File(context.cacheDir, "progressive_downloads")
        if (progressiveCacheDir.exists()) progressiveCacheDir.listFiles()?.forEach { file -> progressiveCacheBytes += file.length() }
        
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isDirectory && (file.name.contains("coil") || file.name.contains("image"))) {
                file.walkTopDown().forEach { cacheFile -> if (cacheFile.isFile) imageCacheBytes += cacheFile.length() }
            }
        }
        
        return StorageInfo(downloadedSongsBytes = downloadedSongsBytes, downloadedSongsCount = _downloadedSongs.value.size, thumbnailsBytes = thumbnailsBytes, cacheBytes = progressiveCacheBytes + imageCacheBytes, progressiveCacheBytes = progressiveCacheBytes, imageCacheBytes = imageCacheBytes, totalBytes = downloadedSongsBytes + thumbnailsBytes + progressiveCacheBytes + imageCacheBytes)
    }
    
    fun clearCache() { clearThumbnails(); clearProgressiveCache(); clearImageCache() }
    fun clearProgressiveCache() { val d = File(context.cacheDir, "progressive_downloads"); if (d.exists()) d.deleteRecursively() }
    fun clearImageCache() { context.cacheDir.listFiles()?.forEach { if (it.isDirectory && (it.name.contains("coil") || it.name.contains("image"))) it.deleteRecursively() } }
    fun clearThumbnails() { val d = File(context.filesDir, "thumbnails"); if (d.exists()) { d.deleteRecursively(); d.mkdirs() } }

    fun getNextFromQueue(): Song? = downloadQueue.peek()
    fun popFromQueue(): Song? { val s = downloadQueue.poll(); _queueState.value = downloadQueue.toList(); return s }
    fun updateBatchProgress(current: Int, total: Int) { _batchProgress.value = current to total }

    /**
     * Atomically advance the "done" counter. Used by [DownloadService] so the
     * worker increment can't race with [downloadSongs] resizing the batch.
     */
    fun incrementBatchDone() {
        _batchProgress.update { (done, total) -> (done + 1) to total }
    }

    fun downloadSongs(songs: List<Song>) {
        val newSongs = songs.filter { song -> _downloadedSongs.value.none { it.id == song.id } && downloadQueue.none { it.id == song.id } && !_downloadingIds.value.contains(song.id) }
        android.util.Log.i(
            DL_TAG,
            "[ENQUEUE] requested=${songs.size} added=${newSongs.size} " +
                "skipped=${songs.size - newSongs.size} queueSizeAfter=${downloadQueue.size + newSongs.size}",
        )
        if (newSongs.isEmpty()) return
        downloadQueue.addAll(newSongs)
        _queueState.value = downloadQueue.toList()
        // Atomic CAS-style update so concurrent done-counter increments from the
        // worker can't race with this caller-thread mutation. The previous
        // read-then-write produced "(5/3)" and "(7/3)" notification counters
        // when the user added more songs to an in-flight batch.
        _batchProgress.update { (currentDone, currentTotal) ->
            if (currentTotal == 0 || currentDone >= currentTotal) {
                0 to newSongs.size
            } else {
                currentDone to (currentTotal + newSongs.size)
            }
        }
        DownloadService.startBatchDownload(context)
    }
    
    fun downloadSongToQueue(song: Song) = downloadSongs(listOf(song))
    
    suspend fun deleteDownloads(songIds: List<String>) = withContext(Dispatchers.IO) {
        if (songIds.isEmpty()) return@withContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = _downloadedSongs.value
                .filter { it.id in songIds && it.localUri?.startsWith("content://") == true }
                .mapNotNull { it.localUri?.let { s -> Uri.parse(s) } }
            if (uris.isNotEmpty()) {
                val deleteRequest = MediaStore.createDeleteRequest(context.contentResolver, uris)
                throw com.suvojeet.suvmusic.util.FileOperationException.FilePermissionException("Permission needed to delete multiple files", deleteRequest)
            }
        }
        songIds.forEach { deleteDownload(it) }
    }
    
    suspend fun deleteAllDownloads() = withContext(Dispatchers.IO) {
        val songIds = _downloadedSongs.value.map { it.id }
        if (songIds.isNotEmpty()) {
            // deleteDownloads throws FilePermissionException on Android 11+ to
            // request the system's multi-file-delete dialog. Let it propagate
            // so the UI can launch the IntentSender — previously this was
            // swallowed by the broad catch below and the user saw nothing
            // happen, then a partial wipe.
            deleteDownloads(songIds)
        }

        try {
            listOf(getPublicMusicFolder(), getLegacyDownloadsFolder()).forEach { publicFolder ->
                if (publicFolder.exists()) publicFolder.listFiles()?.forEach { file -> if (file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac")) file.delete() }
            }
        } catch (e: com.suvojeet.suvmusic.util.FileOperationException.FilePermissionException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "deleteAllDownloads: failed to clean public folder", e)
        }
        _downloadedSongs.value = emptyList()
        saveDownloads()
    }

    fun downloadAlbum(album: com.suvojeet.suvmusic.core.model.Album) {
        val songsToDownload = album.songs.map { song -> song.copy(customFolderPath = album.title, collectionId = album.id, collectionName = album.title, thumbnailUrl = song.thumbnailUrl ?: album.thumbnailUrl) }
        downloadSongs(songsToDownload)
    }

    fun downloadPlaylist(playlist: com.suvojeet.suvmusic.core.model.Playlist) {
        val songsToDownload = playlist.songs.map { song -> song.copy(customFolderPath = playlist.title, collectionId = playlist.id, collectionName = playlist.title, thumbnailUrl = song.thumbnailUrl ?: playlist.thumbnailUrl) }
        downloadSongs(songsToDownload)
    }
}

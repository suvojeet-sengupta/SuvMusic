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
    private val jioSaavnRepository: JioSaavnRepository,
    private val sessionManager: com.suvojeet.suvmusic.data.SessionManager,
    @param:com.suvojeet.suvmusic.di.DownloadDataSource private val dataSourceFactory: androidx.media3.datasource.DataSource.Factory
) {
    companion object {
        private const val TAG = "DownloadRepository"
        private const val SUVMUSIC_FOLDER = "SuvMusic"
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

    init {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            loadDownloads()
            migrateOldDownloads()
            scanDownloadsFolder()
        }
    }

    private fun scanDownloadsFolder() {
        try {
            val currentSongs = _downloadedSongs.value.toMutableList()
            var hasNewSongs = false

            // 1. Scan default folder
            val defaultFolder = getPublicMusicFolder()
            if (defaultFolder.exists()) {
                scanFolder(defaultFolder, currentSongs).let { hasNewSongs = hasNewSongs || it }
            }
            
            // 2. Scan legacy folder
            scanAndMigrateLegacyFolder()
            
            // 3. Scan custom folder if set
            val customLocationUri = kotlinx.coroutines.runBlocking { sessionManager.getDownloadLocation() }
            if (customLocationUri != null) {
                try {
                    val rootUri = Uri.parse(customLocationUri)
                    val rootFolder = DocumentFile.fromTreeUri(context, rootUri)
                    if (rootFolder != null && rootFolder.exists()) {
                        scanDocumentFolder(rootFolder, currentSongs).let { hasNewSongs = hasNewSongs || it }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error scanning custom folder", e)
                }
            }
            
            if (hasNewSongs) {
                _downloadedSongs.value = currentSongs
                saveDownloads()
                Log.d(TAG, "Added new untracked files to downloads")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning downloads folder", e)
        }
    }

    private fun scanFolder(folder: File, currentSongs: MutableList<Song>): Boolean {
        var hasNew = false
        val audioFiles = folder.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac", "wav", "ogg", "opus")
        } ?: return false
        
        for (file in audioFiles) {
            if (processScannedFile(file.name, file.toUri(), currentSongs)) hasNew = true
            
            if (file.isDirectory) {
                file.listFiles { f -> f.isFile && f.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac") }?.forEach { subFile ->
                    if (processScannedFile(subFile.name, subFile.toUri(), currentSongs)) hasNew = true
                }
            }
        }
        return hasNew
    }

    private fun scanDocumentFolder(folder: DocumentFile, currentSongs: MutableList<Song>): Boolean {
        var hasNew = false
        folder.listFiles().forEach { file: DocumentFile ->
            if (file.isFile && (file.name?.substringAfterLast('.', "")?.lowercase() ?: "") in listOf("m4a", "mp3", "aac", "flac")) {
                if (processScannedFile(file.name ?: "Unknown", file.uri, currentSongs)) hasNew = true
            } else if (file.isDirectory) {
                file.listFiles().forEach { subFile: DocumentFile ->
                    if (subFile.isFile && (subFile.name?.substringAfterLast('.', "")?.lowercase() ?: "") in listOf("m4a", "mp3", "aac", "flac")) {
                        if (processScannedFile(subFile.name ?: "Unknown", subFile.uri, currentSongs)) hasNew = true
                    }
                }
            }
        }
        return hasNew
    }

    private fun processScannedFile(fileName: String, uri: Uri, currentSongs: MutableList<Song>): Boolean {
        val nameWithoutExt = fileName.substringBeforeLast('.')
        val parts = nameWithoutExt.split(" - ", limit = 2)
        val title = parts.getOrElse(0) { nameWithoutExt }.trim()
        val artist = parts.getOrElse(1) { "Unknown Artist" }.trim()

        val isTracked = currentSongs.any { song ->
            if (song.localUri == uri || song.localUri?.path == uri.path) return@any true
            val expectedFileName = "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}"
            if (nameWithoutExt == expectedFileName) return@any true
            if (song.title == title && song.artist == artist) return@any true
            false
        }

        if (!isTracked) {
            val song = Song(
                id = "local_${fileName.hashCode()}",
                title = title,
                artist = artist,
                album = "Downloads",
                duration = 0L,
                thumbnailUrl = null,
                source = SongSource.DOWNLOADED,
                streamUrl = null,
                localUri = uri
            )
            currentSongs.add(song)
            return true
        }
        return false
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
            
            val legacyFiles = legacyFolder.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac", "wav", "ogg", "opus")
            } ?: return
            
            if (legacyFiles.isEmpty()) return
            
            val newFolder = getPublicMusicFolder()
            for (file in legacyFiles) {
                try {
                    val newFile = File(newFolder, file.name)
                    if (!newFile.exists()) {
                        file.copyTo(newFile, overwrite = false)
                        file.delete()
                    } else {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error migrating file: ${file.name}", e)
                }
            }
            
            if (legacyFolder.listFiles()?.isEmpty() == true) {
                legacyFolder.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in legacy folder migration", e)
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
                
                val extensions = listOf("mp3", "aac", "flac", "wav", "ogg")
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

    private fun migrateOldDownloads() {
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
                    val newUri = kotlinx.coroutines.runBlocking { 
                        saveFileToPublicDownloads(songId, song.artist, song.title, oldFile.inputStream())
                    }
                    
                    if (newUri != null) {
                        val index = currentSongs.indexOfFirst { it.id == songId }
                        if (index >= 0) {
                            currentSongs[index] = song.copy(localUri = newUri)
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

    private suspend fun saveFileToPublicDownloads(songId: String, artist: String, title: String, inputStream: InputStream, subfolder: String? = null): Uri? {
        val fileName = "${sanitizeFileName(title)} - ${sanitizeFileName(artist)}.m4a"
        
        val customLocationUri = sessionManager.getDownloadLocation()
        if (customLocationUri != null) {
            return saveToCustomLocation(fileName, inputStream, customLocationUri, subfolder)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(songId, fileName, inputStream, subfolder)
        } else {
            saveToPublicFolder(songId, fileName, inputStream, subfolder)
        }
    }

    private fun saveToCustomLocation(fileName: String, inputStream: InputStream, treeUri: String, subfolder: String? = null): Uri? {
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

            val newFile = rootFolder.createFile("audio/m4a", fileName) ?: return null
            
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                inputStream.copyTo(output)
            }
            
            newFile.uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to custom location", e)
            null
        }
    }

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

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(mediaUri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
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
        onProgress: (Float) -> Unit
    ): Uri? {
        val fileName = "${sanitizeFileName(title)} - ${sanitizeFileName(artist)}.m4a"
        
        val customLocationUri = sessionManager.getDownloadLocation()
        if (customLocationUri != null) {
            return saveToCustomLocationWithProgress(fileName, inputStream, contentLength, customLocationUri, subfolder, onProgress)
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStoreWithProgress(fileName, inputStream, contentLength, subfolder, onProgress)
            } else {
                saveToPublicFolderWithProgress(fileName, inputStream, contentLength, subfolder, onProgress)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file with progress", e)
            null
        }
    }

    private fun saveToCustomLocationWithProgress(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        treeUri: String,
        subfolder: String? = null,
        onProgress: (Float) -> Unit
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

            val newFile = rootFolder.createFile("audio/m4a", fileName) ?: return null
            
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                copyWithProgress(inputStream, output, contentLength, onProgress)
            }
            newFile.uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to custom location with progress", e)
            null
        }
    }

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
            file.toUri()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Music folder with progress", e)
            null
        }
    }
    
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
            
            if (contentLength > 0 && totalBytesRead - lastProgressUpdate > 50 * 1024) {
                val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                onProgress(progress)
                lastProgressUpdate = totalBytesRead
            }
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
                val uri = song.localUri
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
                    if (file.exists()) return@mapNotNull song.copy(localUri = file.toUri())
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
                atomicFile.failWrite(fos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving downloads", e)
        }
    }

    private val downloadMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun downloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        val canDownload = downloadMutex.withLock {
            if (_downloadedSongs.value.any { it.id == song.id }) return@withLock false
            if (_downloadingIds.value.contains(song.id)) return@withLock false
            _downloadingIds.update { it + song.id }
            true
        }

        if (!canDownload) return@withContext true
        
        val job = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        if (job != null) activeDownloadJobs[song.id] = job
        
        try {
            val streamUrl = when (song.source) {
                SongSource.JIOSAAVN -> jioSaavnRepository.getStreamUrl(song.id, 320)
                else -> youTubeRepository.getStreamUrlForDownload(song.id)
            }
            if (streamUrl == null) {
                downloadMutex.withLock { _downloadingIds.update { it - song.id } }
                return@withContext false
            }
            
            return@withContext downloadUsingSharedCache(song, streamUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Download error for ${song.id}", e)
            downloadMutex.withLock { _downloadingIds.update { it - song.id } }
            _downloadProgress.update { it - song.id }
            false
        } finally {
            activeDownloadJobs.remove(song.id)
        }
    }

    private suspend fun downloadUsingSharedCache(song: Song, streamUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = android.net.Uri.parse(streamUrl)
            val dataSpec = androidx.media3.datasource.DataSpec.Builder()
                .setUri(uri)
                .setKey(song.id)
                .build()
            val dataSource = dataSourceFactory.createDataSource()
            
            val length = dataSource.open(dataSpec)
            val contentLength = if (length != androidx.media3.common.C.LENGTH_UNSET.toLong()) length else -1L
            
            _downloadProgress.update { it + (song.id to 0f) }
            
            val tempFile = File(context.cacheDir, "${song.id}_download.m4a")
            val inputStream = androidx.media3.datasource.DataSourceInputStream(dataSource, dataSpec)
            
            FileOutputStream(tempFile).use { outputStream ->
                copyWithProgress(inputStream, outputStream, contentLength) { progress ->
                    _downloadProgress.update { it + (song.id to progress) }
                }
            }
            dataSource.close()
            
            tagAudioFile(tempFile, song)
            
            val downloadedUri = tempFile.inputStream().use { input ->
                saveFileToPublicDownloads(song.id, song.artist, song.title, input, song.customFolderPath)
            }
            
            if (tempFile.exists()) tempFile.delete()
            
            if (downloadedUri == null) {
                _downloadingIds.update { it - song.id }
                _downloadProgress.update { it - song.id }
                return@withContext false
            }
            
            val currentThumbnailUrl = song.thumbnailUrl
            var localThumbnailUrl = currentThumbnailUrl
            if (!currentThumbnailUrl.isNullOrEmpty() && currentThumbnailUrl.startsWith("http")) {
                try {
                    val highResThumbnailUrl = getHighResThumbnailUrl(currentThumbnailUrl, song.id)
                    val thumbBytes = downloadThumbnailBytes(url = highResThumbnailUrl)
                    if (thumbBytes != null) {
                        val thumbnailsDir = File(context.filesDir, "thumbnails")
                        if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
                        val thumbFile = File(thumbnailsDir, "${song.id}.jpg")
                        FileOutputStream(thumbFile).use { it.write(thumbBytes) }
                        localThumbnailUrl = thumbFile.toUri().toString()
                    }
                } catch (e: Exception) {}
            }

            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = downloadedUri,
                thumbnailUrl = localThumbnailUrl,
                streamUrl = null,
                originalSource = song.source 
            )

            _downloadedSongs.update { it + downloadedSong }
            saveDownloads()
            _downloadingIds.update { it - song.id }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in downloadUsingSharedCache", e)
            _downloadingIds.update { it - song.id }
            _downloadProgress.update { it - song.id }
            false
        }
    }

    suspend fun downloadSongProgressive(
        song: Song,
        onReadyToPlay: (android.net.Uri) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (_downloadedSongs.value.any { it.id == song.id }) {
            _downloadedSongs.value.find { it.id == song.id }?.localUri?.let { uri ->
                withContext(Dispatchers.Main) { onReadyToPlay(uri) }
            }
            return@withContext true
        }
        
        _downloadingIds.update { it + song.id }
        _downloadProgress.update { it + (song.id to 0f) }
        
        try {
            val streamUrl = youTubeRepository.getStreamUrlForDownload(song.id)
            if (streamUrl == null) {
                _downloadingIds.update { it - song.id }
                _downloadProgress.update { it - song.id }
                return@withContext false
            }
            
            val tempDir = File(context.cacheDir, "progressive_downloads")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, "${song.id}.m4a.tmp")
            
            val request = Request.Builder().url(streamUrl).build()
            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                _downloadingIds.update { it - song.id }
                _downloadProgress.update { it - song.id }
                return@withContext false
            }
            
            val contentLength = response.body.contentLength()
            val minBytesForPlayback = 480 * 1024L
            var playbackTriggered = false
            var totalBytesRead = 0L
            
            response.body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                            _downloadProgress.update { it + (song.id to progress) }
                        }
                        if (!playbackTriggered && totalBytesRead >= minBytesForPlayback) {
                            playbackTriggered = true
                            withContext(Dispatchers.Main) { onReadyToPlay(tempFile.toUri()) }
                        }
                    }
                }
            }
            response.close()
            
            if (!playbackTriggered && tempFile.exists()) {
                withContext(Dispatchers.Main) { onReadyToPlay(tempFile.toUri()) }
            }
            
            tagAudioFile(tempFile, song)
            val finalUri = tempFile.inputStream().use { input ->
                saveFileToPublicDownloads(song.id, song.artist, song.title, input, song.customFolderPath)
            }
            tempFile.delete()
            
            if (finalUri == null) {
                _downloadingIds.update { it - song.id }
                _downloadProgress.update { it - song.id }
                return@withContext false
            }
            
            val currentThumbnailUrl = song.thumbnailUrl
            var localThumbnailUrl = currentThumbnailUrl
            if (!currentThumbnailUrl.isNullOrEmpty() && currentThumbnailUrl.startsWith("http")) {
                try {
                    val highResThumbnailUrl = getHighResThumbnailUrl(currentThumbnailUrl, song.id)
                    val thumbBytes = downloadThumbnailBytes(url = highResThumbnailUrl)
                    if (thumbBytes != null) {
                        val thumbnailsDir = File(context.filesDir, "thumbnails")
                        if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
                        val thumbFile = File(thumbnailsDir, "${song.id}.jpg")
                        FileOutputStream(thumbFile).use { it.write(thumbBytes) }
                        localThumbnailUrl = thumbFile.toUri().toString()
                    }
                } catch (e: Exception) {}
            }
            
            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = finalUri,
                thumbnailUrl = localThumbnailUrl,
                streamUrl = null,
                originalSource = song.source 
            )
            
            _downloadedSongs.update { it + downloadedSong }
            saveDownloads()
            _downloadingIds.update { it - song.id }
            _downloadProgress.update { it - song.id }
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

    fun cancelDownload(songId: String) {
        activeDownloadJobs[songId]?.cancel()
        val iterator = downloadQueue.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().id == songId) {
                iterator.remove()
                break
            }
        }
        _downloadingIds.update { it - songId }
        _downloadProgress.update { it - songId }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val song = _downloadedSongs.value.find { it.id == songId } ?: return@withContext
        try {
            var deleted = false
            song.localUri?.let { uri ->
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

            val downloadedSong = song.copy(source = SongSource.DOWNLOADED, localUri = savedUri, thumbnailUrl = localThumbnailUrl, streamUrl = null, originalSource = song.source, isVideo = true)
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
        scanDownloadsFolder()
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

    fun downloadSongs(songs: List<Song>) {
        val newSongs = songs.filter { song -> _downloadedSongs.value.none { it.id == song.id } && downloadQueue.none { it.id == song.id } && !_downloadingIds.value.contains(song.id) }
        if (newSongs.isEmpty()) return
        downloadQueue.addAll(newSongs)
        _queueState.value = downloadQueue.toList()
        val currentTotal = _batchProgress.value.second
        val currentDone = _batchProgress.value.first
        if (currentTotal == 0 || currentDone >= currentTotal) _batchProgress.value = 0 to newSongs.size
        else _batchProgress.value = currentDone to (currentTotal + newSongs.size)
        DownloadService.startBatchDownload(context)
    }
    
    fun downloadSongToQueue(song: Song) = downloadSongs(listOf(song))
    
    suspend fun deleteDownloads(songIds: List<String>) = withContext(Dispatchers.IO) {
        if (songIds.isEmpty()) return@withContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = _downloadedSongs.value.filter { it.id in songIds && it.localUri?.scheme == "content" }.mapNotNull { it.localUri }
            if (uris.isNotEmpty()) {
                val deleteRequest = MediaStore.createDeleteRequest(context.contentResolver, uris)
                throw com.suvojeet.suvmusic.util.FileOperationException.FilePermissionException("Permission needed to delete multiple files", deleteRequest)
            }
        }
        songIds.forEach { deleteDownload(it) }
    }
    
    suspend fun deleteAllDownloads() = withContext(Dispatchers.IO) {
        _downloadedSongs.value.toList().forEach { try { deleteDownload(it.id) } catch (e: Exception) {} }
        try {
            listOf(getPublicMusicFolder(), getLegacyDownloadsFolder()).forEach { publicFolder ->
                if (publicFolder.exists()) publicFolder.listFiles()?.forEach { file -> if (file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "aac", "flac")) file.delete() }
            }
        } catch (e: Exception) {}
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

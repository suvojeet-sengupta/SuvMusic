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
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
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
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository
) {
    companion object {
        private const val TAG = "DownloadRepository"
        private const val SUVMUSIC_FOLDER = "SuvMusic"
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .create()

    private val downloadsMetaFile = File(context.filesDir, "downloads_meta.json")

    // Временная папка для загрузок (используется для resume/докачки)
    private val tempDownloadDir = File(context.cacheDir, "temp_downloads").apply {
        if (!exists()) mkdirs()
    }

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()
    
    // Download progress tracking for progressive download UI (songId -> progress 0.0-1.0)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
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
        scanMusicFolder()
    }

    class UriTypeAdapter : TypeAdapter<Uri>() {
        override fun write(out: JsonWriter, value: Uri?) {
            out.value(value?.toString())
        }

        override fun read(input: JsonReader): Uri? {
            val uriString = input.nextString()
            return if (uriString.isNotEmpty()) Uri.parse(uriString) else null
        }
    }

    private fun scanMusicFolder() {
        try {
            val folder = getPublicMusicFolder()
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
                    song.localUri == fileUri ||
                            song.localUri?.path == file.absolutePath ||
                            file.name == "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.m4a"
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
                        album = "Music",
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
            Log.e(TAG, "Error scanning music folder", e)
        }
    }

    private fun getPublicMusicFolder(): File {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val suvMusicDir = File(musicDir, SUVMUSIC_FOLDER)
        if (!suvMusicDir.exists()) {
            suvMusicDir.mkdirs()
        }
        return suvMusicDir
    }

    /**
     * Save file to public Downloads/SuvMusic folder using appropriate API
     */
    private fun saveFileToPublicMusic(songId: String, artist: String, title: String, inputStream: InputStream): Uri? {
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
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$SUVMUSIC_FOLDER")
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
                Log.d(TAG, "Saved to MediaStore (Music): $fileName")
            }
            
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to MediaStore", e)
            saveToPublicFolder(songId, fileName, inputStream)
        }
    }

    /**
     * Save to public Music folder directly (Android 9 and below)
     */
    private fun saveToPublicFolder(songId: String, fileName: String, inputStream: InputStream): Uri? {
        return try {
            val folder = getPublicMusicFolder()
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
                            try {
                                context.contentResolver.openInputStream(uri)?.close()
                                true
                            } catch (e: Exception) { false }
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
        Log.d(TAG, "Starting download for: ${song.title} (${song.source})")

        try {
            val finalUri: Uri? = when (song.source) {
                // Use OkHttp for JioSaavn
                SongSource.JIOSAAVN -> downloadFromJioSaavn(song)
                // Use yt-dlp for YouTube
                else -> downloadFromYouTube(song)
            }

            if (finalUri == null) {
                Log.e(TAG, "Download failed (Uri is null)")
                _downloadingIds.value = _downloadingIds.value - song.id
                return@withContext false
            }

            // Download thumbnail
            var localThumbnailUrl = song.thumbnailUrl
            if (!song.thumbnailUrl.isNullOrEmpty() && song.thumbnailUrl.startsWith("http")) {
                localThumbnailUrl = downloadThumbnail(song.id, song.thumbnailUrl) ?: song.thumbnailUrl
            }

            // Finalize
            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = finalUri,
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
            Log.e(TAG, "Download error for ${song.id}", e)
            _downloadingIds.value = _downloadingIds.value - song.id
            _downloadProgress.value = _downloadProgress.value - song.id
            false
        }
    }

    /**
     * Optimized JioSaavn Downloader using OkHttp with Resume support.
     * Checks if file exists in temp folder and appends to it (Range header).
     */
    private suspend fun downloadFromJioSaavn(song: Song): Uri? {
        val streamUrl = jioSaavnRepository.getStreamUrl(song.id, 320) ?: return null
        val tempFile = File(tempDownloadDir, "${song.id}.m4a.tmp")

        var downloadedBytes = 0L
        if (tempFile.exists()) {
            downloadedBytes = tempFile.length()
            Log.d(TAG, "Found temp file for ${song.title}, resuming from $downloadedBytes bytes")
        }

        val requestBuilder = Request.Builder().url(streamUrl)
        if (downloadedBytes > 0) {
            // Add Range header for resuming
            requestBuilder.header("Range", "bytes=$downloadedBytes-")
        }

        val response = downloadClient.newCall(requestBuilder.build()).execute()

        if (!response.isSuccessful) {
            // If server doesn't support range (416 or 200 instead of 206), restart
            if (response.code == 416 || (downloadedBytes > 0 && response.code == 200)) {
                Log.w(TAG, "Server doesn't support resume properly, restarting download")
                downloadedBytes = 0
                tempFile.delete()
                // Retry without range logic here if needed, but for now we fail or get new stream
            } else {
                response.close()
                return null
            }
        }

        val totalSize = (response.body?.contentLength() ?: 0) + downloadedBytes

        response.body?.byteStream()?.use { input ->
            // Use RandomAccessFile to append
            RandomAccessFile(tempFile, "rw").use { output ->
                output.seek(downloadedBytes) // Move to end of existing data

                val buffer = ByteArray(8192 * 4) // 32KB buffer
                var bytesRead: Int
                var currentBatchRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    currentBatchRead += bytesRead

                    // Update progress
                    if (totalSize > 0) {
                        val progress = ((downloadedBytes + currentBatchRead).toFloat() / totalSize).coerceIn(0f, 1f)
                        _downloadProgress.value = _downloadProgress.value + (song.id to progress)
                    }
                }
            }
        }
        response.close()

        // Move from temp to public
        val finalUri = saveFileToPublicMusic(song.id, song.artist, song.title, tempFile.inputStream())
        tempFile.delete() // Cleanup temp
        return finalUri
    }

    /**
     * yt-dlp Downloader with multi-thread support
     */
    private suspend fun downloadFromYouTube(song: Song): Uri? {
        val tempBaseName = "temp_${song.id}"
        val videoUrl = "https://www.youtube.com/watch?v=${song.id}"

        val request = YoutubeDLRequest(videoUrl)
        request.addOption("-o", "${tempDownloadDir.absolutePath}/$tempBaseName.%(ext)s")
        request.addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
        request.addOption("--no-playlist")
        request.addOption("--add-metadata")

        // Multi-threading for yt-dlp
        request.addOption("-N", "8")

        Log.d(TAG, "Executing yt-dlp (8 threads) for $videoUrl")

        YoutubeDL.getInstance().execute(request) { progress, _, _ ->
            val p = progress / 100f
            _downloadProgress.value = _downloadProgress.value + (song.id to p)
        }

        val downloadedFile = tempDownloadDir.listFiles()?.find {
            it.name.startsWith(tempBaseName)
        } ?: throw Exception("Downloaded file not found")

        val inputStream = downloadedFile.inputStream()
        val finalUri = saveFileToPublicMusic(song.id, song.artist, song.title, inputStream)

        inputStream.close()
        downloadedFile.delete()
        return finalUri
    }

    private fun downloadThumbnail(songId: String, url: String): String? {
        return try {
            val highResUrl = getHighResThumbnailUrl(url, songId)
            val request = Request.Builder().url(highResUrl).build()
            val response = downloadClient.newCall(request).execute()

            if (response.isSuccessful) {
                val thumbnailsDir = File(context.filesDir, "thumbnails")
                if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()

                val thumbFile = File(thumbnailsDir, "${songId}.jpg")
                response.body?.bytes()?.let { bytes ->
                    FileOutputStream(thumbFile).use { it.write(bytes) }
                    return thumbFile.toUri().toString()
                }
            }
            response.close()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Thumbnail download failed", e)
            null
        }
    }

    // Keep progressive download as fallback
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
        
        // Mark as downloading
        _downloadingIds.value = _downloadingIds.value + song.id
        _downloadProgress.value = _downloadProgress.value + (song.id to 0f)
        Log.d(TAG, "Starting progressive download for: ${song.title}")
        
        try {
            val streamUrl = if (song.source == SongSource.JIOSAAVN)
                jioSaavnRepository.getStreamUrl(song.id, 320)
            else
                youTubeRepository.getStreamUrlForDownload(song.id)

            if (streamUrl == null) return@withContext false

            // Progressive download
            val tempFile = File(tempDownloadDir, "${song.id}_prog.m4a")
            val request = Request.Builder().url(streamUrl).build()
            val response = downloadClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                response.close()
                return@withContext false
            }
            
            val contentLength = response.body?.contentLength() ?: -1L
            Log.d(TAG, "Content length: $contentLength bytes")
            
            // Estimate bytes for 30 seconds (assuming ~128kbps = 16KB/s)
            // 30 seconds = ~480KB minimum to start playback
            val minBytesForPlayback = 500 * 1024L
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
                            withContext(Dispatchers.Main) { onReadyToPlay(tempFile.toUri()) }
                        }
                    }
                }
            }
            
            response.close()
            
            // If file was small and playback wasn't triggered, trigger now
            if (!playbackTriggered && tempFile.exists()) {
                withContext(Dispatchers.Main) { onReadyToPlay(tempFile.toUri()) }
            }

            val finalUri = saveFileToPublicMusic(song.id, song.artist, song.title, tempFile.inputStream())
            tempFile.delete()

            if (finalUri != null) {
                val downloadedSong = song.copy(
                    source = SongSource.DOWNLOADED,
                    localUri = finalUri,
                    thumbnailUrl = song.thumbnailUrl,
                    streamUrl = null,
                    originalSource = song.source
                )
                _downloadedSongs.value = _downloadedSongs.value + downloadedSong
                saveDownloads()
            }

            _downloadingIds.value = _downloadingIds.value - song.id
            _downloadProgress.value = _downloadProgress.value - song.id
            Log.d(TAG, "Song ${song.title} progressive download successful!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Progressive download error for ${song.id}", e)
            _downloadingIds.value = _downloadingIds.value - song.id
            false
        }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val song = _downloadedSongs.value.find { it.id == songId } ?: return@withContext
        
        try {
            // Try to delete the audio file
            var deleted = false
            
            song.localUri?.let { uri ->
                if (uri.scheme == "file") {
                    deleted = File(uri.path ?: "").delete()
                } else if (uri.scheme == "content") {
                    deleted = context.contentResolver.delete(uri, null, null) > 0
                }
            }
            
            // If URI delete didn't work, try finding the file in public Music folder
            if (!deleted) {
                val file = File(getPublicMusicFolder(), "${sanitizeFileName(song.title)} - ${sanitizeFileName(song.artist)}.m4a")
                if (file.exists()) file.delete()
            }
            File(context.filesDir, "thumbnails/${songId}.jpg").delete()
            _downloadedSongs.value = _downloadedSongs.value.filter { it.id != songId }
            saveDownloads()
        } catch (e: Exception) { Log.e(TAG, "Error deleting", e) }
    }

    fun isDownloaded(songId: String): Boolean = _downloadedSongs.value.any { it.id == songId }

    fun isDownloading(songId: String): Boolean = _downloadingIds.value.contains(songId)

    /**
     * Rescan Downloads/SuvMusic folder for new files.
     * Call this when entering the Library screen to pick up manually added files.
     */
    fun refreshDownloads() {
        loadDownloads()
        scanMusicFolder()
    }
    
    /**
     * Get download count and total duration info.
     */
    fun getDownloadInfo(): Pair<Int, Long> = Pair(_downloadedSongs.value.size, _downloadedSongs.value.sumOf { it.duration })

    private fun getHighResThumbnailUrl(originalUrl: String, videoId: String): String {
        return when {
            originalUrl.contains("lh3.googleusercontent.com") || originalUrl.contains("yt3.ggpht.com") -> {
                originalUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544").replace(Regex("=s\\d+.*"), "=s544")
            }
            originalUrl.contains("ytimg.com") || originalUrl.contains("youtube.com") -> {
                val ytVideoId = if (originalUrl.contains("/vi/")) originalUrl.substringAfter("/vi/").substringBefore("/") else videoId
                "https://img.youtube.com/vi/$ytVideoId/maxresdefault.jpg"
            }
            else -> originalUrl
        }
    }
}
package com.suvojeet.suvmusic.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.di.DownloadDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to download a song and set it as ringtone
 */
@Singleton
class RingtoneHelper @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val downloadRepository: DownloadRepository,
    @DownloadDataSource private val dataSourceFactory: androidx.media3.datasource.DataSource.Factory
) {
    
    companion object {
        private const val CHANNEL_ID = "ringtone_download"
        private const val NOTIFICATION_ID = 9999
    }
    
    /**
     * Download the song and set it as ringtone
     * Shows progress notification during download
     */
    suspend fun downloadAndSetAsRingtone(
        context: Context,
        song: Song,
        onProgress: (Float, String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // Check WRITE_SETTINGS permission
            if (!Settings.System.canWrite(context)) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Permission required to set ringtone")
                }
                return@withContext
            }
            
            onProgress(0f, "Getting audio stream...")
            
            // Check if already downloaded
            val downloadedSong = downloadRepository.downloadedSongs.value.find { it.id == song.id }
            val inputStream: InputStream
            var contentLength: Long = -1L
            var dataSource: androidx.media3.datasource.DataSource? = null

            if (downloadedSong?.localUri != null) {
                onProgress(0.1f, "Using downloaded file...")
                val uri = downloadedSong.localUri
                inputStream = context.contentResolver.openInputStream(uri) ?: run {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Could not open downloaded file")
                    }
                    return@withContext
                }
            } else {
                // Get stream URL
                val streamUrl = youTubeRepository.getStreamUrlForDownload(song.id)
                if (streamUrl == null) {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Failed to get audio stream")
                    }
                    return@withContext
                }
                
                onProgress(0.1f, "Fetching audio (using cache if available)...")
                
                // Use DataSource to leverage cache
                val dataSpec = androidx.media3.datasource.DataSpec.Builder()
                    .setUri(Uri.parse(streamUrl))
                    .setKey(song.id)
                    .build()
                
                val ds = dataSourceFactory.createDataSource()
                dataSource = ds
                val length = ds.open(dataSpec)
                contentLength = if (length != androidx.media3.common.C.LENGTH_UNSET.toLong()) length else -1L
                inputStream = androidx.media3.datasource.DataSourceInputStream(ds, dataSpec)
            }
            
            // Create file in Ringtones directory
            val fileName = "${song.title.replace(Regex("[^a-zA-Z0-9\\s]"), "")}_${song.id}.m4a"
            
            val ringtoneUri: Uri = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10+
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                        put(MediaStore.Audio.Media.IS_RINGTONE, true)
                        put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                        put(MediaStore.Audio.Media.IS_ALARM, true)
                        put(MediaStore.Audio.Media.TITLE, song.title)
                        put(MediaStore.Audio.Media.ARTIST, song.artist)
                    }
                    
                    val uri = context.contentResolver.insert(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: run {
                        withContext(Dispatchers.Main) {
                            onComplete(false, "Failed to create ringtone file")
                        }
                        return@withContext
                    }
                    
                    // Write to URI
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            if (contentLength > 0) {
                                val progress = 0.1f + (totalBytesRead.toFloat() / contentLength) * 0.7f
                                withContext(Dispatchers.Main) {
                                    onProgress(progress, "Downloading... ${(progress * 100).toInt()}%")
                                }
                            }
                        }
                    }
                    uri
                } else {
                    // Legacy approach for older Android versions
                    val ringtonesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
                    if (!ringtonesDir.exists()) ringtonesDir.mkdirs()
                    
                    val file = File(ringtonesDir, fileName)
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            if (contentLength > 0) {
                                val progress = 0.1f + (totalBytesRead.toFloat() / contentLength) * 0.7f
                                withContext(Dispatchers.Main) {
                                    onProgress(progress, "Downloading... ${(progress * 100).toInt()}%")
                                }
                            }
                        }
                    }
                    
                    // Add to MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DATA, file.absolutePath)
                        put(MediaStore.Audio.Media.TITLE, song.title)
                        put(MediaStore.Audio.Media.ARTIST, song.artist)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.IS_RINGTONE, true)
                        put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                        put(MediaStore.Audio.Media.IS_ALARM, true)
                    }
                    
                    context.contentResolver.insert(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: Uri.fromFile(file)
                }
            } finally {
                inputStream.close()
                dataSource?.close()
            }
            
            onProgress(0.9f, "Setting as ringtone...")
            
            // Set as ringtone
            try {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    ringtoneUri
                )
                
                withContext(Dispatchers.Main) {
                    onProgress(1f, "Done!")
                    onComplete(true, "\"${song.title}\" set as ringtone!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, "Failed to set ringtone: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Check if app has WRITE_SETTINGS permission
     */
    fun hasWriteSettingsPermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }
}

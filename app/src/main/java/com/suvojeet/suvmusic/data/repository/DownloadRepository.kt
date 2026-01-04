package com.suvojeet.suvmusic.data.repository

import android.content.Context
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository
) {
    private val gson = Gson()
    private val downloadsFile = File(context.filesDir, "downloads_meta.json")
    private val downloadsDir = File(context.filesDir, "downloads")
    
    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()

    init {
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        loadDownloads()
    }

    private fun loadDownloads() {
        if (!downloadsFile.exists()) {
            _downloadedSongs.value = emptyList()
            return
        }
        try {
            val json = downloadsFile.readText()
            val type = object : TypeToken<List<Song>>() {}.type
            _downloadedSongs.value = gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadedSongs.value = emptyList()
        }
    }

    private fun saveDownloads() {
        try {
            val json = gson.toJson(_downloadedSongs.value)
            downloadsFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun downloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        if (_downloadedSongs.value.any { it.id == song.id }) return@withContext true // Already downloaded
        
        val currentDownloading = _downloadingIds.value.toMutableSet()
        currentDownloading.add(song.id)
        _downloadingIds.value = currentDownloading
        
        try {
            val streamUrl = youTubeRepository.getStreamUrl(song.id)
            if (streamUrl == null) {
                val newDownloading = _downloadingIds.value.toMutableSet()
                newDownloading.remove(song.id)
                _downloadingIds.value = newDownloading
                return@withContext false
            }

            val request = Request.Builder().url(streamUrl).build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val newDownloading = _downloadingIds.value.toMutableSet()
                newDownloading.remove(song.id)
                _downloadingIds.value = newDownloading
                return@withContext false
            }

            val file = File(downloadsDir, "${song.id}.m4a")
            val fos = FileOutputStream(file)
            response.body?.byteStream()?.use { input ->
                input.copyTo(fos)
            }
            fos.close()

            // Create downloaded song entry
            // Note: We need to use file URI that ExoPlayer understands
            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = file.toUri(),
                streamUrl = null
            )

            val currentList = _downloadedSongs.value.toMutableList()
            currentList.add(downloadedSong)
            _downloadedSongs.value = currentList
            saveDownloads()
            
            val newDownloading = _downloadingIds.value.toMutableSet()
            newDownloading.remove(song.id)
            _downloadingIds.value = newDownloading
            true
        } catch (e: Exception) {
            e.printStackTrace()
            val newDownloading = _downloadingIds.value.toMutableSet()
            newDownloading.remove(song.id)
            _downloadingIds.value = newDownloading
            false
        }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val song = _downloadedSongs.value.find { it.id == songId } ?: return@withContext
        
        try {
            val file = File(downloadsDir, "${songId}.m4a")
            if (file.exists()) file.delete()
            
            val currentList = _downloadedSongs.value.toMutableList()
            currentList.remove(song)
            _downloadedSongs.value = currentList
            saveDownloads()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun isDownloaded(songId: String): Boolean {
        return _downloadedSongs.value.any { it.id == songId }
    }
    
    fun isDownloading(songId: String): Boolean {
        return _downloadingIds.value.contains(songId)
    }
}

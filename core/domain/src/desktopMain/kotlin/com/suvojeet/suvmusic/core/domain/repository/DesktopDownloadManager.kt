package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Linux file-download adapter for remote stream URLs and local files. */
class DesktopDownloadManager(
    private val root: Path = Paths.get(System.getProperty("user.home"), "Music", "SuvMusic", "Downloads"),
) : DownloadManager {
    private val executor = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + executor)
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
    private val activeIds = ConcurrentHashMap.newKeySet<String>()
    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())

    override val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()
    override val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()
    override val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    override fun enqueue(song: Song) = enqueue(listOf(song))

    override fun enqueue(songs: List<Song>) {
        songs.filter { activeIds.add(it.id) }.forEach { song ->
            _downloadingIds.update { it + song.id }
            _downloadProgress.update { it + (song.id to 0f) }
            scope.launch { download(song) }
        }
    }

    override suspend fun delete(songId: String) {
        val song = _downloadedSongs.value.firstOrNull { it.id == songId } ?: return
        song.localUri?.let { uri ->
            runCatching { Files.deleteIfExists(Paths.get(URI(uri))) }
        }
        _downloadedSongs.update { songs -> songs.filterNot { it.id == songId } }
    }

    override suspend fun deleteAll() {
        _downloadedSongs.value.forEach { song ->
            song.localUri?.let { uri -> runCatching { Files.deleteIfExists(Paths.get(URI(uri))) } }
        }
        _downloadedSongs.value = emptyList()
    }

    private suspend fun download(song: Song) {
        try {
            Files.createDirectories(root)
            val extension = song.streamUrl?.substringBefore('?')?.substringAfterLast('.', "bin") ?: "bin"
            val target = root.resolve(safeName(song.title) + ".$extension")
            val source = song.streamUrl ?: song.localUri ?: return
            if (source.startsWith("file:")) {
                Files.copy(Paths.get(URI(source)), target, StandardCopyOption.REPLACE_EXISTING)
            } else if (source.startsWith("http://") || source.startsWith("https://")) {
                val request = HttpRequest.newBuilder(URI(source)).GET().build()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target))
                check(response.statusCode() in 200..299) { "Download failed: HTTP ${response.statusCode()}" }
            } else {
                Files.copy(Paths.get(source), target, StandardCopyOption.REPLACE_EXISTING)
            }
            _downloadProgress.update { it + (song.id to 1f) }
            _downloadedSongs.update { songs -> (songs.filterNot { it.id == song.id } + song.copy(source = SongSource.DOWNLOADED, streamUrl = null, localUri = target.toUri().toString())) }
        } finally {
            activeIds.remove(song.id)
            _downloadingIds.update { it - song.id }
            _downloadProgress.update { it - song.id }
        }
    }

    private fun safeName(value: String): String = value
        .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
        .trim()
        .ifBlank { "suvmusic-track" }
        .take(120)
}

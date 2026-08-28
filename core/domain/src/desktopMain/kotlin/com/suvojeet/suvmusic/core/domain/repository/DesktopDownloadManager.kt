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
import java.util.Base64
import java.util.Properties
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
    private val metadataFile: Path = root.resolve(".suvmusic-downloads.properties")
    private val _downloadedSongs = MutableStateFlow(loadPersistedSongs())
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
        persistSongs(_downloadedSongs.value)
    }

    override suspend fun deleteAll() {
        _downloadedSongs.value.forEach { song ->
            song.localUri?.let { uri -> runCatching { Files.deleteIfExists(Paths.get(URI(uri))) } }
        }
        _downloadedSongs.value = emptyList()
        persistSongs(emptyList())
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
            _downloadedSongs.update { songs ->
                songs.filterNot { it.id == song.id } + song.copy(
                    source = SongSource.DOWNLOADED,
                    streamUrl = null,
                    localUri = target.toUri().toString(),
                )
            }
            persistSongs(_downloadedSongs.value)
        } finally {
            activeIds.remove(song.id)
            _downloadingIds.update { it - song.id }
            _downloadProgress.update { it - song.id }
        }
    }

    private fun loadPersistedSongs(): List<Song> {
        if (!Files.isRegularFile(metadataFile)) return emptyList()
        return runCatching {
            val properties = Properties()
            Files.newBufferedReader(metadataFile).use(properties::load)
            val count = properties.getProperty("count", "0").toIntOrNull() ?: 0
            (0 until count).mapNotNull { index ->
                val localUri = decode(properties.getProperty("song.$index.localUri")) ?: return@mapNotNull null
                val localPath = runCatching { Paths.get(URI(localUri)) }.getOrNull() ?: return@mapNotNull null
                if (!Files.isRegularFile(localPath)) return@mapNotNull null
                Song(
                    id = decode(properties.getProperty("song.$index.id")) ?: return@mapNotNull null,
                    title = decode(properties.getProperty("song.$index.title")) ?: "Downloaded track",
                    artist = decode(properties.getProperty("song.$index.artist")) ?: "Unknown Artist",
                    album = decode(properties.getProperty("song.$index.album")) ?: "Downloads",
                    duration = properties.getProperty("song.$index.duration", "0").toLongOrNull() ?: 0L,
                    thumbnailUrl = decode(properties.getProperty("song.$index.thumbnail")),
                    source = SongSource.DOWNLOADED,
                    localUri = localUri,
                    customFolderPath = decode(properties.getProperty("song.$index.folder")),
                )
            }
        }.getOrElse { emptyList() }
    }

    @Synchronized
    private fun persistSongs(songs: List<Song>) {
        runCatching {
            Files.createDirectories(root)
            val properties = Properties()
            properties.setProperty("count", songs.size.toString())
            songs.forEachIndexed { index, song ->
                properties.setProperty("song.$index.id", encode(song.id))
                properties.setProperty("song.$index.title", encode(song.title))
                properties.setProperty("song.$index.artist", encode(song.artist))
                properties.setProperty("song.$index.album", encode(song.album))
                properties.setProperty("song.$index.duration", song.duration.toString())
                properties.setProperty("song.$index.thumbnail", encode(song.thumbnailUrl))
                properties.setProperty("song.$index.localUri", encode(song.localUri))
                properties.setProperty("song.$index.folder", encode(song.customFolderPath))
            }
            val temporary = metadataFile.resolveSibling("${metadataFile.fileName}.tmp")
            Files.newBufferedWriter(temporary).use { writer -> properties.store(writer, "SuvMusic desktop downloads") }
            Files.move(temporary, metadataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    private fun encode(value: String?): String = value?.let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it.encodeToByteArray())
    } ?: "-"

    private fun decode(value: String?): String? = value
        ?.takeUnless { it == "-" }
        ?.let { runCatching { Base64.getUrlDecoder().decode(it).decodeToString() }.getOrNull() }

    private fun safeName(value: String): String = value
        .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
        .trim()
        .ifBlank { "suvmusic-track" }
        .take(120)
}

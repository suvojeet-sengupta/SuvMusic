package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.prefs.Preferences
import kotlin.io.path.isRegularFile

/**
 * Linux/JVM local-media adapter. Roots are persisted as a delimited preference
 * and managed from the shared desktop Library folder-picker UI.
 */
class DesktopLocalMediaSource(
    roots: List<String>? = null,
) : LocalMediaSource, LocalMediaRootManager {
    @Volatile
    private var roots: List<Path> = (roots ?: readRoots().ifEmpty { defaultRoots() })
        .mapNotNull { runCatching { Paths.get(it) }.getOrNull() }

    /** Replace configured folders and persist them for the next desktop launch. */
    @Synchronized
    override fun updateRoots(rootPaths: List<String>) {
        roots = rootPaths
            .mapNotNull { runCatching { Paths.get(it).toAbsolutePath().normalize() }.getOrNull() }
            .distinct()
        saveRoots(roots.map(Path::toString))
    }

    override fun configuredRoots(): List<String> = roots.map(Path::toString)

    @Synchronized
    private fun rootsSnapshot(): List<Path> = roots.toList()

    override suspend fun getAllLocalSongs(): List<Song> = scan().sortedBy { it.title.lowercase() }

    override suspend fun searchLocalSongs(query: String): List<Song> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return getAllLocalSongs()
        return scan().filter { song ->
            song.title.lowercase().contains(needle) || song.artist.lowercase().contains(needle)
        }.sortedBy { it.title.lowercase() }
    }

    private fun scan(): List<Song> = buildList {
        rootsSnapshot().forEach { root ->
            if (!Files.exists(root)) return@forEach
            Files.walk(root).use { paths ->
                paths.filter { path ->
                    path.isRegularFile() && path.fileName.toString().substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS
                }
                    .forEach { path -> add(path.toSong()) }
            }
        }
    }.distinctBy { it.id }

    private fun Path.toSong(): Song {
        val fileName = fileName.toString()
        val title = fileName.substringBeforeLast('.').ifBlank { fileName }
        return Song(
            id = toAbsolutePath().normalize().toString(),
            title = title,
            artist = "Unknown Artist",
            album = "Local Files",
            duration = 0L,
            thumbnailUrl = null,
            source = SongSource.LOCAL,
            localUri = toAbsolutePath().normalize().toUri().toString(),
            customFolderPath = parent?.toString(),
        )
    }

    companion object {
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "webm")
        private val preferences = Preferences.userRoot().node("SuvMusic")

        fun saveRoots(roots: List<String>) {
            preferences.put("local_roots", roots.joinToString(FileSystemSeparator))
        }

        private fun readRoots(): List<String> = preferences.get("local_roots", "")
            .split(FileSystemSeparator)
            .filter(String::isNotBlank)

        private fun defaultRoots(): List<String> = listOf(
            Paths.get(System.getProperty("user.home"), "Music").toString(),
            Paths.get(System.getProperty("user.home"), "Downloads").toString(),
        )

        private const val FileSystemSeparator = "\u001f"
    }
}

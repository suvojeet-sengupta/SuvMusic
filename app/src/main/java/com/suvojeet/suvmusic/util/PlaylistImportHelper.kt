package com.suvojeet.suvmusic.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistImportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spotifyImportHelper: SpotifyImportHelper,
    private val youTubeRepository: YouTubeRepository,
    private val gson: Gson
) {
    /**
     * Data class for universal track info during import.
     */
    data class ImportTrack(
        val title: String,
        val artist: String,
        val durationMs: Long = 0,
        val sourceId: String? = null, // For direct YTM imports
        val song: Song? = null // For direct native imports
    )

    /**
     * Universal import method.
     */
    suspend fun getPlaylistSongs(
        input: String,
        onTrackFetch: (Int) -> Unit = {}
    ): Pair<String, List<ImportTrack>> = withContext(Dispatchers.IO) {
        return@withContext when {
            input.contains("spotify.com") || input.contains("spotify.link") -> {
                val (name, tracks) = spotifyImportHelper.getPlaylistSongs(input, onTrackFetch)
                name to tracks.map { ImportTrack(it.title, it.artist, it.durationMs) }
            }
            input.contains("youtube.com") || input.contains("youtu.be") -> {
                importFromYouTube(input, onTrackFetch)
            }
            else -> "Imported Playlist" to emptyList()
        }
    }

    /**
     * Import songs from a YouTube Music / YouTube playlist URL.
     */
    private suspend fun importFromYouTube(
        url: String,
        onTrackFetch: (Int) -> Unit
    ): Pair<String, List<ImportTrack>> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(url)
            var playlistId = uri.getQueryParameter("list") ?: url.substringAfter("list=", "").substringBefore("&")
            
            // Handle album browse IDs too if they are provided
            if (playlistId.isBlank() && (url.contains("browse/") || url.contains("channel/"))) {
                playlistId = url.substringAfter("browse/").substringAfter("channel/").substringBefore("?").substringBefore("/")
            }

            if (playlistId.isNotBlank()) {
                val playlist = youTubeRepository.getPlaylist(playlistId, autoSave = false)
                val tracks = playlist.songs.map { 
                    ImportTrack(it.title, it.artist, it.duration, it.id, it)
                }
                onTrackFetch(tracks.size)
                return@withContext playlist.title to tracks
            }
        } catch (e: Exception) {
            Log.e("PlaylistImportHelper", "YouTube import failed", e)
        }
        "YouTube Import" to emptyList()
    }

    /**
     * Parse an .m3u file from a Uri.
     */
    suspend fun parseM3U(uri: Uri): Pair<String, List<ImportTrack>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<ImportTrack>()
        var playlistName = uri.lastPathSegment?.substringBeforeLast(".") ?: "M3U Import"
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var currentTitle: String? = null
                    var currentArtist: String? = null
                    
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line!!.trim()
                        if (trimmed.startsWith("#EXTINF:")) {
                            // Parse #EXTINF:duration,Artist - Title
                            // Or #EXTINF:duration,Title
                            val info = trimmed.substringAfter("#EXTINF:")
                            val commaIndex = info.indexOf(',')
                            if (commaIndex != -1) {
                                val metadata = info.substring(commaIndex + 1)
                                if (metadata.contains(" - ")) {
                                    currentArtist = metadata.substringBefore(" - ").trim()
                                    currentTitle = metadata.substringAfter(" - ").trim()
                                } else {
                                    currentTitle = metadata.trim()
                                    currentArtist = "Unknown Artist"
                                }
                            }
                        } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                            // This is a file path or URL
                            // If we didn't get metadata from #EXTINF, use filename
                            val title = currentTitle ?: trimmed.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
                            val artist = currentArtist ?: "Unknown Artist"
                            
                            tracks.add(ImportTrack(title, artist))
                            
                            // Reset for next
                            currentTitle = null
                            currentArtist = null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlaylistImportHelper", "M3U parse failed", e)
        }
        
        playlistName to tracks
    }

    /**
     * Parse a CSV playlist export from a Uri.
     *
     * Supports common Spotify-style headers (`Track Name`, `Artist Name(s)`,
     * `Album Name`, `Duration (ms)`) and simpler exports (`Title`, `Artist`,
     * `Album`, `Duration`). Rows remain in source order and are not
     * deduplicated, so the imported count matches the source file.
     */
    suspend fun parseCSV(uri: Uri): Pair<String, List<ImportTrack>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<ImportTrack>()
        val playlistName = uri.lastPathSegment
            ?.substringBeforeLast(".")
            ?.takeIf { it.isNotBlank() }
            ?: "CSV Import"

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val records = csvRecords(reader).iterator()
                    if (!records.hasNext()) return@use

                    val firstRow = records.next()
                    val headerIndexes = csvHeaderIndexes(firstRow)
                    val hasHeader = headerIndexes.titleIndex != null || headerIndexes.artistIndex != null

                    fun addTrack(row: List<String>) {
                        val titleIndex = headerIndexes.titleIndex ?: 0
                        val artistIndex = headerIndexes.artistIndex ?: 1
                        val durationIndex = headerIndexes.durationIndex
                        val sourceIdIndex = headerIndexes.sourceIdIndex
                        val title = row.getOrNull(titleIndex)?.trim().orEmpty()
                        if (title.isBlank()) return

                        val artist = row.getOrNull(artistIndex)?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: "Unknown Artist"
                        val sourceId = sourceIdIndex
                            ?.let { row.getOrNull(it) }
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }

                        tracks += ImportTrack(
                            title = title,
                            artist = artist,
                            durationMs = parseCsvDuration(durationIndex?.let { row.getOrNull(it) }),
                            sourceId = sourceId,
                        )
                    }

                    if (!hasHeader) addTrack(firstRow)
                    while (records.hasNext()) addTrack(records.next())
                }
            }
        } catch (e: Exception) {
            Log.e("PlaylistImportHelper", "CSV parse failed", e)
        }

        playlistName to tracks
    }

    private data class CsvHeaderIndexes(
        val titleIndex: Int? = null,
        val artistIndex: Int? = null,
        val durationIndex: Int? = null,
        val sourceIdIndex: Int? = null,
    )

    private fun csvHeaderIndexes(row: List<String>): CsvHeaderIndexes {
        fun normalized(value: String): String = value
            .removePrefix("\uFEFF")
            .trim()
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")

        fun find(vararg names: String): Int? {
            val accepted = names.toSet()
            return row.indexOfFirst { normalized(it) in accepted }.takeIf { it >= 0 }
        }

        return CsvHeaderIndexes(
            titleIndex = find("trackname", "title", "song", "songname", "tracktitle"),
            artistIndex = find("artistnames", "artist", "artists", "artistname", "performer"),
            durationIndex = find("durationms", "duration", "length", "durationmillis"),
            sourceIdIndex = find("youtubeid", "videoid", "youtubevideoid"),
        )
    }

    private fun parseCsvDuration(value: String?): Long {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return 0L
        raw.toLongOrNull()?.let { numeric ->
            // Exporters commonly use milliseconds; small plain values are usually seconds.
            return if (numeric >= 10_000L) numeric else numeric * 1000L
        }
        val parts = raw.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toLongOrNull() ?: return 0L
            val seconds = parts[1].toLongOrNull() ?: return 0L
            return (minutes * 60 + seconds) * 1000L
        }
        return 0L
    }

    /** Reads CSV records without splitting a quoted field at an embedded newline. */
    private fun csvRecords(reader: BufferedReader): Sequence<List<String>> = sequence {
        val record = StringBuilder()
        var insideQuotes = false

        while (true) {
            val line = reader.readLine() ?: break
            if (record.isNotEmpty()) record.append('\n')
            record.append(line)

            var index = 0
            while (index < line.length) {
                if (line[index] == '"') {
                    if (index + 1 < line.length && line[index + 1] == '"') {
                        index++
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                index++
            }

            if (!insideQuotes) {
                yield(parseCsvRow(record.toString()))
                record.clear()
            }
        }

        if (record.isNotBlank()) yield(parseCsvRow(record.toString()))
    }

    private fun parseCsvRow(record: String): List<String> {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var insideQuotes = false
        var index = 0

        while (index < record.length) {
            when (val character = record[index]) {
                '"' -> {
                    if (insideQuotes && index + 1 < record.length && record[index + 1] == '"') {
                        field.append('"')
                        index++
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                ',' -> if (insideQuotes) field.append(character) else {
                    fields += field.toString().trim()
                    field.clear()
                }
                else -> field.append(character)
            }
            index++
        }
        fields += field.toString().trim()
        return fields
    }

    /**
     * Parse an .suv file from a Uri.
     */
    suspend fun parseSUV(uri: Uri): Pair<String, List<ImportTrack>> = withContext(Dispatchers.IO) {
        val tracksMap = mutableMapOf<String, ImportTrack>()
        var playlistName = uri.lastPathSegment?.substringBeforeLast(".") ?: "SUV Import"
        var sequence = emptyList<String>()
        
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                // 1. Read Metadata
                val metaStart = content.indexOf("[METADATA]")
                val metaEnd = content.indexOf("[/METADATA]")
                if (metaStart != -1 && metaEnd != -1 && metaStart < metaEnd) {
                    val metaStr = content.substring(metaStart + 10, metaEnd).trim()
                    metaStr.lines().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("title:")) {
                            playlistName = trimmed.substringAfter("title:").trim()
                        }
                    }
                }

                // 2. Read Sequence
                val seqStart = content.indexOf("[SEQUENCE]")
                val seqEnd = content.indexOf("[/SEQUENCE]")
                if (seqStart != -1 && seqEnd != -1 && seqStart < seqEnd) {
                    val sequenceStr = content.substring(seqStart + 10, seqEnd).trim()
                    sequence = sequenceStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }

                // 3. Parse Songs
                val songBlocks = content.split("[SONG]").drop(1).map { it.substringBefore("[/SONG]") }
                for (block in songBlocks) {
                    var currentId = ""
                    var currentTitle = ""
                    var currentArtist = ""
                    var currentAlbum = ""
                    var currentDuration = 0L
                    var currentSourceStr = ""
                    
                    block.lines().forEach { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.startsWith("id:") -> currentId = trimmed.substringAfter("id:").trim()
                            trimmed.startsWith("title:") -> currentTitle = trimmed.substringAfter("title:").trim()
                            trimmed.startsWith("artist:") -> currentArtist = trimmed.substringAfter("artist:").trim()
                            trimmed.startsWith("album:") -> currentAlbum = trimmed.substringAfter("album:").trim()
                            trimmed.startsWith("duration:") -> currentDuration = trimmed.substringAfter("duration:").trim().toLongOrNull() ?: 0L
                            trimmed.startsWith("source:") -> currentSourceStr = trimmed.substringAfter("source:").trim()
                        }
                    }

                    if (currentId.isNotBlank()) {
                        val source = try {
                            com.suvojeet.suvmusic.core.model.SongSource.valueOf(currentSourceStr)
                        } catch (e: Exception) {
                            com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE
                        }

                        val song = com.suvojeet.suvmusic.core.model.Song(
                            id = currentId,
                            title = currentTitle,
                            artist = currentArtist,
                            album = currentAlbum.ifBlank { currentTitle },
                            duration = currentDuration,
                            thumbnailUrl = if (source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE || source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE_MUSIC) {
                                "https://img.youtube.com/vi/$currentId/maxresdefault.jpg"
                            } else null,
                            source = source
                        )
                        
                        tracksMap[currentId] = ImportTrack(
                            title = currentTitle,
                            artist = currentArtist,
                            durationMs = currentDuration,
                            sourceId = currentId,
                            song = song
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlaylistImportHelper", "SUV parse failed", e)
        }
        
        val orderedTracks = if (sequence.isNotEmpty()) {
            sequence.mapNotNull { tracksMap[it] }
        } else {
            tracksMap.values.toList()
        }
        
        playlistName to orderedTracks
    }
}

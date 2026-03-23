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

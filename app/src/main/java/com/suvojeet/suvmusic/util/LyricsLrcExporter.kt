package com.suvojeet.suvmusic.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.suvojeet.suvmusic.providers.lyrics.LyricsLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes parsed lyrics to a canonical Enhanced LRC (".lrc v2") file in
 * Documents/SuvMusic and returns a content URI suitable for an ACTION_SEND
 * share intent. Mirrors the MediaStore approach used by [LyricsPdfGenerator].
 */
object LyricsLrcExporter {

    suspend fun exportAndShareLrc(
        context: Context,
        lines: List<LyricsLine>,
        songTitle: String,
        artistName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            if (lines.isEmpty()) return@withContext null

            val safeTitle = songTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_").ifBlank { "Lyrics" }
            val fileName = "${safeTitle}_${System.currentTimeMillis()}.lrc"

            // Standard LRC ID-tag metadata so receiving apps (SimpMusic, BetterMusic,
            // Musixmatch, …) can attribute the file before the timed payload.
            val header = buildString {
                if (artistName.isNotBlank()) append("[ar:").append(artistName).append("]\n")
                if (songTitle.isNotBlank()) append("[ti:").append(songTitle).append("]\n")
                append("[tool:SuvMusic]\n")
            }
            val body = LyricsUtils.serialize(lines, enhanced = true)
            val payload = (header + body).toByteArray(Charsets.UTF_8)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                // Most receivers don't register a handler for text/x-lrc, so we
                // declare text/plain to maximise share-sheet reach. The .lrc
                // extension is still preserved in DISPLAY_NAME.
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/SuvMusic"
                )
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Files.getContentUri("external"),
                values
            ) ?: return@withContext null

            resolver.openOutputStream(uri)?.use { out -> out.write(payload) }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

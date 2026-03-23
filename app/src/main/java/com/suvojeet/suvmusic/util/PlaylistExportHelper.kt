package com.suvojeet.suvmusic.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.suvojeet.suvmusic.core.model.Playlist
import java.io.File

object PlaylistExportHelper {
    
    fun exportPlaylistToM3U(context: Context, playlist: Playlist) {
        val songs = playlist.songs
        if (songs.isEmpty()) return

        try {
            val m3uContent = StringBuilder("#EXTM3U\n")
            for (song in songs) {
                m3uContent.append("#EXTINF:${song.duration / 1000},${song.artist} - ${song.title}\n")
                // For YouTube songs, use the URL. For local songs, use the URI.
                val url = if (song.localUri != null) {
                    song.localUri.toString()
                } else {
                    "https://www.youtube.com/watch?v=${song.id}"
                }
                m3uContent.append("$url\n")
            }

            val safeTitle = playlist.title.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "$safeTitle.m3u"
            
            val playlistsDir = File(context.cacheDir, "playlists")
            if (!playlistsDir.exists()) playlistsDir.mkdirs()
            
            val tempFile = File(playlistsDir, fileName)
            tempFile.writeText(m3uContent.toString())
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/x-mpegurl"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Export Playlist")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportPlaylistToSUV(context: Context, playlist: Playlist) {
        val songs = playlist.songs
        if (songs.isEmpty()) return

        try {
            val suvContent = StringBuilder("#SUVPLAYLIST\n")
            
            // Add Metadata
            suvContent.append("[METADATA]\n")
            suvContent.append("title: ${playlist.title}\n")
            suvContent.append("author: ${playlist.author}\n")
            playlist.description?.let { suvContent.append("description: $it\n") }
            suvContent.append("[/METADATA]\n\n")

            for (song in songs) {
                suvContent.append("[SONG]\n")
                suvContent.append("id: ${song.id}\n")
                suvContent.append("title: ${song.title}\n")
                suvContent.append("artist: ${song.artist}\n")
                suvContent.append("album: ${song.album}\n")
                suvContent.append("duration: ${song.duration}\n")
                suvContent.append("source: ${song.source}\n")
                suvContent.append("[/SONG]\n")
            }

            suvContent.append("[SEQUENCE]\n")
            suvContent.append(songs.joinToString(",") { it.id })
            suvContent.append("\n[/SEQUENCE]")

            val safeTitle = playlist.title.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "$safeTitle.suv"
            
            val playlistsDir = File(context.cacheDir, "playlists")
            if (!playlistsDir.exists()) playlistsDir.mkdirs()
            
            val tempFile = File(playlistsDir, fileName)
            tempFile.writeText(suvContent.toString())
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Export Playlist (.suv)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

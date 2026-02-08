package com.suvojeet.suvmusic.providers.lyrics

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class LocalLyricsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val name = "Local Storage"

    fun getLyrics(song: Song): String? {
        // 1. Try to resolve the file path of the audio
        val path = getAudioPath(song)
        
        if (path != null) {
            // 2. Check for sidecar .lrc file
            val lrcFile = File(path.substringBeforeLast(".") + ".lrc")
            if (lrcFile.exists()) return lrcFile.readText()

            // 3. Check for sidecar .txt file
            val txtFile = File(path.substringBeforeLast(".") + ".txt")
            if (txtFile.exists()) return txtFile.readText()
        }
        
        // 4. Fallback: Check internal app storage for manually added lyrics
        // Path: /Android/data/com.package/files/lyrics/{songId}.lrc
        val internalDir = File(context.getExternalFilesDir(null), "lyrics")
        if (internalDir.exists()) {
            val internalLrc = File(internalDir, "${song.id}.lrc")
            if (internalLrc.exists()) return internalLrc.readText()
            
            val internalTxt = File(internalDir, "${song.id}.txt")
            if (internalTxt.exists()) return internalTxt.readText()
        }

        return null
    }
    
    /**
     * Helper to save manually added lyrics
     */
    fun saveLyrics(songId: String, content: String) {
        val internalDir = File(context.getExternalFilesDir(null), "lyrics")
        if (!internalDir.exists()) internalDir.mkdirs()
        
        val file = File(internalDir, "$songId.lrc")
        file.writeText(content)
    }

    private fun getAudioPath(song: Song): String? {
        try {
            val uri = song.localUri ?: return null
            
            if (uri.scheme == "file") {
                return uri.path
            }
            
            if (uri.scheme == "content") {
                val projection = arrayOf(MediaStore.Audio.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                         val idx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                         if (idx != -1) return cursor.getString(idx)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

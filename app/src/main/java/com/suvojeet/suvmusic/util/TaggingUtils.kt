package com.suvojeet.suvmusic.util

import android.util.Log
import com.suvojeet.suvmusic.core.model.Song
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File

object TaggingUtils {
    private const val TAG = "TaggingUtils"

    /**
     * Embeds metadata and album art into an audio file.
     * 
     * @param file The audio file to tag
     * @param song The song metadata
     * @param albumArtBytes Optional bytes of the album art image
     */
    fun embedMetadata(file: File, song: Song, albumArtBytes: ByteArray? = null) {
        try {
            // JAudioTagger might have some issues with specific Android versions 
            // but we'll try to use it as standard first.
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateDefault

            tag.setField(FieldKey.TITLE, song.title)
            tag.setField(FieldKey.ARTIST, song.artist)
            tag.setField(FieldKey.ALBUM, song.album ?: "")
            
            // Set year if available (YouTube usually doesn't provide it directly in Song model yet)
            
            if (albumArtBytes != null && albumArtBytes.isNotEmpty()) {
                try {
                    val artwork = ArtworkFactory.getNew()
                    artwork.setBinaryData(albumArtBytes)
                    artwork.setMimeType("image/jpeg")
                    
                    // Remove existing artwork first to avoid duplicates
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to embed artwork for ${song.title}", e)
                }
            }

            audioFile.commit()
            Log.d(TAG, "Successfully embedded metadata for ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error embedding metadata for ${song.title}", e)
        }
    }
}

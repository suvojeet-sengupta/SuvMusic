package com.suvojeet.suvmusic.data.repository.lyrics

import android.content.Context
import com.suvojeet.suvmusic.data.SessionManager
import javax.inject.Inject

/**
 * SimpMusic provider wrapper.
 * Fetches lyrics from SimpMusic API by video ID.
 */
class SimpMusicLyricsProvider @Inject constructor(
    private val sessionManager: SessionManager
) : LyricsProvider {
    
    override val name = "SimpMusic"

    override fun isEnabled(context: Context): Boolean {
        return sessionManager.enableSimpMusic
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): Result<String> = SimpMusicLyrics.getLyrics(id, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit
    ) {
        SimpMusicLyrics.getAllLyrics(id, duration, callback)
    }
}

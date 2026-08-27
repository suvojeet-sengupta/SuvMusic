package com.suvojeet.suvmusic.simpmusic

import com.suvojeet.suvmusic.providers.lyrics.LyricsProvider
import javax.inject.Inject

/**
 * SimpMusic provider wrapper.
 * Fetches lyrics from SimpMusic API by video ID.
 */
class SimpMusicLyricsProvider @Inject constructor() : LyricsProvider {
    
    override val name = "SimpMusic"

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

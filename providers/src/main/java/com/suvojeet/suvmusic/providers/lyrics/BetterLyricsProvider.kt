package com.suvojeet.suvmusic.providers.lyrics

import javax.inject.Inject

/**
 * BetterLyrics provider wrapper.
 * Fetches Apple Music lyrics via BetterLyrics API.
 */
class BetterLyricsProvider @Inject constructor() : LyricsProvider {
    
    override val name = "BetterLyrics"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, album)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit
    ) {
        BetterLyrics.getAllLyrics(title, artist, duration, album, callback)
    }
}

package com.suvojeet.suvmusic.data.repository.lyrics

import android.content.Context
import com.suvojeet.suvmusic.data.SessionManager
import javax.inject.Inject

/**
 * BetterLyrics provider wrapper.
 * Fetches Apple Music lyrics via BetterLyrics API.
 */
class BetterLyricsProvider @Inject constructor(
    private val sessionManager: SessionManager
) : LyricsProvider {
    
    override val name = "BetterLyrics"

    override suspend fun isEnabled(context: Context): Boolean {
        return sessionManager.doesEnableBetterLyrics()
    }

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

package com.suvojeet.suvmusic.lrclib

import com.suvojeet.suvmusic.providers.lyrics.LyricsProvider
import okhttp3.OkHttpClient
import javax.inject.Inject

/** Android DI adapter; the shared client owns cross-platform networking and matching. */
class LrcLibLyricsProvider @Inject constructor(
    @Suppress("UNUSED_PARAMETER") private val okHttpClient: OkHttpClient,
) : LyricsProvider {
    override val name: String = "LRCLIB"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = LrcLibLyrics.getLyrics(title, artist, duration, album)
}

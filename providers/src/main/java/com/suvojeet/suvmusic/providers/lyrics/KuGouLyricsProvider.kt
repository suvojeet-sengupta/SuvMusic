package com.suvojeet.suvmusic.providers.lyrics

import javax.inject.Inject

/**
 * KuGou Lyrics provider wrapper.
 * Fetches lyrics from KuGou service.
 */
class KuGouLyricsProvider @Inject constructor() : LyricsProvider {

    override val name = "Kugou"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): Result<String> {
        return KuGou.getLyrics(title, artist, duration, album)
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, album, callback)
    }
}

package com.suvojeet.suvmusic.providers.lyrics

/**
 * Interface for lyrics providers.
 * Each provider fetches lyrics from a different source.
 */
interface LyricsProvider {
    /**
     * Unique name identifying this provider
     */
    val name: String

    /**
     * Fetch lyrics for a song.
     * @param id Video/Track ID
     * @param title Song title
     * @param artist Song artist
     * @param duration Song duration in seconds
     * @param album Album name (optional)
     * @return Result with LRC format lyrics string
     */
    suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null
    ): Result<String>

    /**
     * Fetch all available lyrics variants and invoke callback for each.
     * @param id Video/Track ID
     * @param title Song title
     * @param artist Song artist
     * @param duration Song duration in seconds
     * @param album Album name (optional)
     * @param callback Invoked for each lyrics variant found
     */
    suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit
    ) {
        getLyrics(id, title, artist, duration, album).onSuccess(callback)
    }
}

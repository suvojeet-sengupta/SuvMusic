package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song

/** Platform-neutral local audio library boundary. */
interface LocalMediaSource {
    suspend fun getAllLocalSongs(): List<Song>
    suspend fun searchLocalSongs(query: String): List<Song>
}

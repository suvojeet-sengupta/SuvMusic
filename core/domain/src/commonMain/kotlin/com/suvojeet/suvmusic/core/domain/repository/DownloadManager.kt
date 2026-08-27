package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.StateFlow

/** Platform-neutral download boundary for Android MediaStore and Linux files. */
interface DownloadManager {
    val downloadedSongs: StateFlow<List<Song>>
    val downloadingIds: StateFlow<Set<String>>
    val downloadProgress: StateFlow<Map<String, Float>>

    fun enqueue(song: Song)
    fun enqueue(songs: List<Song>)
    suspend fun delete(songId: String)
    suspend fun deleteAll()
}

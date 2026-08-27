package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.core.domain.repository.DownloadManager
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Android host adapter for the common download contract. */
@Singleton
class AndroidDownloadManager @Inject constructor(
    private val repository: DownloadRepository,
) : DownloadManager {
    override val downloadedSongs: StateFlow<List<Song>> = repository.downloadedSongs
    override val downloadingIds: StateFlow<Set<String>> = repository.downloadingIds
    override val downloadProgress: StateFlow<Map<String, Float>> = repository.downloadProgress

    override fun enqueue(song: Song) = repository.downloadSongToQueue(song)

    override fun enqueue(songs: List<Song>) = repository.downloadSongs(songs)

    override suspend fun delete(songId: String) = repository.deleteDownload(songId)

    override suspend fun deleteAll() = repository.deleteAllDownloads()
}

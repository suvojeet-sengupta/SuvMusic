package com.suvojeet.suvmusic.core.domain.repository

import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for playback-related operations.
 * Handles queue management, song streaming, and playback state.
 */
interface PlaybackRepository {
    suspend fun getStreamUrl(song: Song): String?
    suspend fun getLyrics(song: Song): String?
    fun getRecentlyPlayed(): Flow<List<Song>>
    suspend fun markAsPlayed(song: Song)
    suspend fun getRecommendedSongs(): List<Song>
    suspend fun getQueueSongs(queueId: String): List<Song>
    suspend fun addToQueue(songs: List<Song>)
}

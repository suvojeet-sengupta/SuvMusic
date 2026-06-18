package com.suvojeet.suvmusic.data.repository.remote

import android.util.Log

class FallbackRemoteAudioApiService(
    private val primaryService: RemoteAudioApiService,
    private val fallbackService: RemoteAudioApiService
) : RemoteAudioApiService {

    companion object {
        private const val FAILURE_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes cooldown before retrying primary
        @Volatile
        private var lastFailureTime = 0L
    }

    private suspend fun <T> executeWithFallback(
        block: suspend RemoteAudioApiService.() -> T
    ): T {
        val now = System.currentTimeMillis()
        if (now - lastFailureTime < FAILURE_COOLDOWN_MS) {
            // Primary has failed recently, go straight to fallback
            return try {
                fallbackService.block()
            } catch (e: Exception) {
                throw e
            }
        }

        return try {
            val result = primaryService.block()
            // Check if API returned successful status in response body
            val success = when (result) {
                is RemoteAudioSearchResponse -> result.success != false
                is RemoteAudioSongDetailsResponse -> result.success != false
                else -> true
            }
            if (!success) {
                throw RuntimeException("Primary API returned success=false")
            }
            
            // If primary call succeeded
            if (lastFailureTime != 0L) {
                lastFailureTime = 0L
                RemoteAudioApiStatus.setPrimaryApiWorking(true)
            }
            result
        } catch (e: Exception) {
            Log.e("FallbackRemoteAudioApi", "Primary HQ Audio API failed, falling back to legacy API. Error: ${e.message}")
            lastFailureTime = System.currentTimeMillis()
            RemoteAudioApiStatus.setPrimaryApiWorking(false)
            
            try {
                fallbackService.block()
            } catch (fallbackEx: Exception) {
                throw fallbackEx
            }
        }
    }

    override suspend fun searchAll(query: String): RemoteAudioSearchResponse {
        return executeWithFallback { searchAll(query) }
    }

    override suspend fun searchSongs(query: String, page: Int, limit: Int): RemoteAudioSearchResponse {
        return executeWithFallback { searchSongs(query, page, limit) }
    }

    override suspend fun getSongDetails(songId: String): RemoteAudioSongDetailsResponse {
        return executeWithFallback { getSongDetails(songId) }
    }

    override suspend fun getSongSuggestions(songId: String, limit: Int): RemoteAudioSongDetailsResponse {
        return executeWithFallback { getSongSuggestions(songId, limit) }
    }

    override suspend fun getAlbumDetails(albumId: String): RemoteAudioSongDetailsResponse {
        return executeWithFallback { getAlbumDetails(albumId) }
    }

    override suspend fun getPlaylist(playlistId: String): RemoteAudioSearchResponse {
        return executeWithFallback { getPlaylist(playlistId) }
    }
}

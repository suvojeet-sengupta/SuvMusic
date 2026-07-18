package com.suvojeet.suvmusic.data.repository.remote

import android.util.Log

class FallbackHqAudioPlaylistApiService(
    private val primaryService: HqAudioPlaylistApiService,
    private val fallbackService: HqAudioPlaylistApiService
) : HqAudioPlaylistApiService {

    private suspend fun <T> executeWithFallback(
        block: suspend HqAudioPlaylistApiService.() -> T
    ): T {
        return try {
            val result = primaryService.block()
            val success = when (result) {
                is ApiResponse<*> -> result.success
                else -> true
            }
            if (success) {
                RemoteAudioApiStatus.setPrimaryApiWorking(true)
            }
            result
        } catch (e: Exception) {
            val isServerErrorOrNetwork = when (e) {
                is java.io.IOException -> true
                is retrofit2.HttpException -> e.code() >= 500 || e.code() == 429
                else -> true
            }

            if (isServerErrorOrNetwork) {
                Log.e("FallbackHqAudioApi", "Primary HQ Audio Playlist API failed. Error: ${e.message}")
                RemoteAudioApiStatus.setPrimaryApiWorking(false)
            } else {
                Log.w("FallbackHqAudioApi", "Primary HQ Audio Playlist API returned client error. Error: ${e.message}")
            }
            throw e
        }
    }

    override suspend fun searchPlaylists(query: String): SearchPlaylistsResponse {
        return executeWithFallback { searchPlaylists(query) }
    }

    override suspend fun searchArtists(query: String): SearchArtistsResponse {
        return executeWithFallback { searchArtists(query) }
    }

    override suspend fun getPlaylistDetails(playlistId: String): PlaylistDetailsResponse {
        return executeWithFallback { getPlaylistDetails(playlistId) }
    }
}

package com.suvojeet.suvmusic.data.repository.remote

import android.util.Log

class FallbackRemoteAudioApiService(
    private val primaryService: RemoteAudioApiService,
    private val fallbackService: RemoteAudioApiService
) : RemoteAudioApiService {

    companion object {
        private const val FAILURE_COOLDOWN_MS = 60 * 1000L // 1 minute cooldown before retrying primary
        @Volatile
        private var lastFailureTime = 0L
    }

    private suspend fun <T> executeWithFallback(
        block: suspend RemoteAudioApiService.() -> T
    ): T {
        return try {
            val result = primaryService.block()
            val success = when (result) {
                is RemoteAudioSearchResponse -> result.success != false
                is RemoteAudioSongDetailsResponse -> result.success != false
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
                Log.e("FallbackRemoteAudioApi", "Primary HQ Audio API failed. Error: ${e.message}")
                RemoteAudioApiStatus.setPrimaryApiWorking(false)
            } else {
                Log.w("FallbackRemoteAudioApi", "Primary HQ Audio API returned client error. Error: ${e.message}")
            }
            throw e
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

    override suspend fun searchArtists(query: String, page: Int, limit: Int): RemoteAudioArtistSearchResponse {
        return executeWithFallback { searchArtists(query, page, limit) }
    }

    override suspend fun getArtist(artistId: String, page: Int, songCount: Int, albumCount: Int): RemoteAudioArtistResponse {
        return executeWithFallback { getArtist(artistId, page, songCount, albumCount) }
    }

    override suspend fun getSongLyrics(songId: String): RemoteAudioLyricsResponse {
        return executeWithFallback { getSongLyrics(songId) }
    }
}

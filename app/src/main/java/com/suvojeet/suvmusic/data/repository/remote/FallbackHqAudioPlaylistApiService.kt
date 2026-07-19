package com.suvojeet.suvmusic.data.repository.remote

class FallbackHqAudioPlaylistApiService(
    private val primaryService: HqAudioPlaylistApiService,
    private val fallbackService: HqAudioPlaylistApiService
) : HqAudioPlaylistApiService {

    private suspend fun <T> executeWithFallback(
        block: suspend HqAudioPlaylistApiService.() -> T
    ): T = withApiFallback(primaryService, fallbackService, "FallbackHqAudioApi", block)

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

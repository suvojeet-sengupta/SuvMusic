package com.suvojeet.suvmusic.data.repository.remote

class FallbackRemoteAudioApiService(
    private val primaryService: RemoteAudioApiService,
    private val fallbackService: RemoteAudioApiService
) : RemoteAudioApiService {

    private suspend fun <T> executeWithFallback(
        block: suspend RemoteAudioApiService.() -> T
    ): T = withApiFallback(primaryService, fallbackService, "FallbackRemoteAudioApi", block)

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

    override suspend fun searchAlbums(query: String, page: Int, limit: Int): RemoteAudioAlbumSearchResponse {
        return executeWithFallback { searchAlbums(query, page, limit) }
    }

    override suspend fun searchPlaylists(query: String, page: Int, limit: Int): RemoteAudioPlaylistSearchResponse {
        return executeWithFallback { searchPlaylists(query, page, limit) }
    }
}

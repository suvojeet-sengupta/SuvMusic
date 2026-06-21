package com.suvojeet.suvmusic.data.repository.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit interface for the remote HQ audio backend. */
interface RemoteAudioApiService {

    @GET("search")
    suspend fun searchAll(
        @Query("query") query: String
    ): RemoteAudioSearchResponse

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): RemoteAudioSearchResponse

    @GET("songs/{id}")
    suspend fun getSongDetails(
        @Path("id") songId: String
    ): RemoteAudioSongDetailsResponse

    @GET("songs/{id}/suggestions")
    suspend fun getSongSuggestions(
        @Path("id") songId: String,
        @Query("limit") limit: Int = 20
    ): RemoteAudioSongDetailsResponse

    @GET("albums")
    suspend fun getAlbumDetails(
        @Query("id") albumId: String
    ): RemoteAudioSongDetailsResponse // Album response usually shares the SongDetails list structure in data

    @GET("playlists")
    suspend fun getPlaylist(
        @Query("id") playlistId: String
    ): RemoteAudioSearchResponse

    @GET("search/artists")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): RemoteAudioArtistSearchResponse

    @GET("artists/{id}")
    suspend fun getArtist(
        @Path("id") artistId: String,
        @Query("page") page: Int = 1,
        @Query("songCount") songCount: Int = 20,
        @Query("albumCount") albumCount: Int = 20
    ): RemoteAudioArtistResponse

    @GET("songs/{id}/lyrics")
    suspend fun getSongLyrics(
        @Path("id") songId: String
    ): RemoteAudioLyricsResponse

    @GET("search/albums")
    suspend fun searchAlbums(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): RemoteAudioAlbumSearchResponse

    @GET("search/playlists")
    suspend fun searchPlaylists(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): RemoteAudioPlaylistSearchResponse
}

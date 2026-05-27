package com.suvojeet.suvmusic.data.repository.jiosaavn

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the saavn.sumit.co JioSaavn API wrapper.
 */
interface JioSaavnApiService {

    @GET("search")
    suspend fun searchAll(
        @Query("query") query: String
    ): JioSaavnSearchResponse

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): JioSaavnSearchResponse

    @GET("songs/{id}")
    suspend fun getSongDetails(
        @Path("id") songId: String
    ): JioSaavnSongDetailsResponse

    @GET("songs/{id}/suggestions")
    suspend fun getSongSuggestions(
        @Path("id") songId: String,
        @Query("limit") limit: Int = 20
    ): JioSaavnSongDetailsResponse

    @GET("albums")
    suspend fun getAlbumDetails(
        @Query("id") albumId: String
    ): JioSaavnSongDetailsResponse // Album response usually shares the SongDetails list structure in data

    @GET("playlists")
    suspend fun getPlaylist(
        @Query("id") playlistId: String
    ): JioSaavnSearchResponse}

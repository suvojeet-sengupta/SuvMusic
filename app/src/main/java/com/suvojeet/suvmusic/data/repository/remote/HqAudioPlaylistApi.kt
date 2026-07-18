package com.suvojeet.suvmusic.data.repository.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T
)

@Serializable
data class DownloadLink(
    val quality: String,
    val url: String
)

@Serializable
data class PlaylistSearchItem(
    val id: String,
    val title: String,
    val image: List<DownloadLink>? = null,
    val url: String? = null,
    val language: String? = null,
    val type: String? = null,
    val description: String? = null
)

@Serializable
data class SearchResultCategory<T>(
    val results: List<T>,
    val position: Int? = null
)

typealias SearchPlaylistsResponse =
    ApiResponse<SearchResultCategory<PlaylistSearchItem>>

typealias SearchArtistsResponse =
    ApiResponse<SearchResultCategory<Artist>>

@Serializable
data class AlbumInfo(
    val id: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val role: String? = null,
    val type: String? = null,
    val image: List<DownloadLink>? = null,
    val url: String? = null
)

@Serializable
data class ArtistGroup(
    val primary: List<Artist>,
    val featured: List<Artist>,
    val all: List<Artist>
)

@Serializable
data class HqAudioPlaylistSong(
    val id: String,
    val name: String,
    val type: String,

    val year: String? = null,
    val releaseDate: String? = null,
    val duration: Int? = null,

    val label: String? = null,
    val copyright: String? = null,

    val explicitContent: Boolean,
    val playCount: Long? = null,

    val language: String,

    val hasLyrics: Boolean,
    val lyricsId: String? = null,

    val url: String,

    val album: AlbumInfo,
    val artists: ArtistGroup,

    val image: List<DownloadLink>,
    val downloadUrl: List<DownloadLink>
)

@Serializable
data class PlaylistDetail(
    val id: String,

    val name: String,
    val description: String? = null,

    val year: Int? = null,
    val type: String? = null,

    val playCount: Long? = null,
    val songCount: Int? = null,

    val language: String? = null,
    val explicitContent: Boolean? = null,

    val url: String,

    val image: List<DownloadLink>,

    val songs: List<HqAudioPlaylistSong>? = null
)

typealias PlaylistDetailsResponse =
    ApiResponse<PlaylistDetail>

interface HqAudioPlaylistApiService {

    @GET("search/playlists")
    suspend fun searchPlaylists(
        @Query("query") query: String
    ): SearchPlaylistsResponse

    @GET("search/artists")
    suspend fun searchArtists(
        @Query("query") query: String
    ): SearchArtistsResponse

    @GET("playlists")
    suspend fun getPlaylistDetails(
        @Query("id") playlistId: String
    ): PlaylistDetailsResponse
}

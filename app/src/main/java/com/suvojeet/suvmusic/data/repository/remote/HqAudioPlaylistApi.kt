package com.suvojeet.suvmusic.data.repository.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null
)

@Serializable
data class DownloadLink(
    val quality: String? = null,
    val url: String? = null
)

@Serializable
data class PlaylistSearchItem(
    val id: String? = null,
    @SerializedName("name") val title: String? = null,
    val image: List<DownloadLink>? = null,
    val url: String? = null,
    val language: String? = null,
    val type: String? = null,
    val description: String? = null
)

@Serializable
data class SearchResultCategory<T>(
    val results: List<T>? = null,
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
    val id: String? = null,
    val name: String? = null,
    val role: String? = null,
    val type: String? = null,
    val image: List<DownloadLink>? = null,
    val url: String? = null
)

@Serializable
data class ArtistGroup(
    val primary: List<Artist>? = null,
    val featured: List<Artist>? = null,
    val all: List<Artist>? = null
)

@Serializable
data class HqAudioPlaylistSong(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,

    val year: String? = null,
    val releaseDate: String? = null,
    val duration: Int? = null,

    val label: String? = null,
    val copyright: String? = null,

    val explicitContent: Boolean? = null,
    val playCount: Long? = null,

    val language: String? = null,

    val hasLyrics: Boolean? = null,
    val lyricsId: String? = null,

    val url: String? = null,

    val album: AlbumInfo? = null,
    val artists: ArtistGroup? = null,

    val image: List<DownloadLink>? = null,
    val downloadUrl: List<DownloadLink>? = null
)

@Serializable
data class PlaylistDetail(
    val id: String? = null,

    val name: String? = null,
    val description: String? = null,

    val year: Int? = null,
    val type: String? = null,

    val playCount: Long? = null,
    val songCount: Int? = null,

    val language: String? = null,
    val explicitContent: Boolean? = null,

    val url: String? = null,

    val image: List<DownloadLink>? = null,

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

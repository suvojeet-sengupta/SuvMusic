package com.suvojeet.suvmusic.data.repository.jiosaavn

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class JioSaavnSearchResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: JioSaavnSearchData? = null
)

@Serializable
data class JioSaavnSearchData(
    @SerializedName("topQuery") val topQuery: JioSaavnTopQuery? = null,
    @SerializedName("songs") val songs: JioSaavnSongsCategory? = null,
    @SerializedName("albums") val albums: JioSaavnAlbumsCategory? = null,
    @SerializedName("artists") val artists: JioSaavnArtistsCategory? = null,
    @SerializedName("playlists") val playlists: JioSaavnPlaylistsCategory? = null
)

@Serializable
data class JioSaavnTopQuery(
    @SerializedName("results") val results: List<JioSaavnSongDto>? = null
)

@Serializable
data class JioSaavnSongsCategory(
    @SerializedName("results") val results: List<JioSaavnSongDto>? = null
)

@Serializable
data class JioSaavnAlbumsCategory(
    @SerializedName("results") val results: List<JioSaavnAlbumDto>? = null
)

@Serializable
data class JioSaavnArtistsCategory(
    @SerializedName("results") val results: List<JioSaavnArtistDto>? = null
)

@Serializable
data class JioSaavnPlaylistsCategory(
    @SerializedName("results") val results: List<JioSaavnPlaylistDto>? = null
)

@Serializable
data class JioSaavnSongDetailsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: List<JioSaavnSongDto>? = null
)

@Serializable
data class JioSaavnSongDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("explicitContent") val explicitContent: Boolean? = null,
    @SerializedName("playCount") val playCount: Long? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("hasLyrics") val hasLyrics: Boolean? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("copyright") val copyright: String? = null,
    @SerializedName("album") val album: JioSaavnAlbumInfoDto? = null,
    @SerializedName("artists") val artists: JioSaavnArtistsDto? = null,
    @SerializedName("image") val image: List<JioSaavnImageDto>? = null,
    @SerializedName("downloadUrl") val downloadUrl: List<JioSaavnDownloadUrlDto>? = null
)

@Serializable
data class JioSaavnAlbumInfoDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null
)

@Serializable
data class JioSaavnArtistsDto(
    @SerializedName("primary") val primary: List<JioSaavnArtistDto>? = null,
    @SerializedName("featured") val featured: List<JioSaavnArtistDto>? = null,
    @SerializedName("all") val all: List<JioSaavnArtistDto>? = null
)

@Serializable
data class JioSaavnArtistDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("image") val image: List<JioSaavnImageDto>? = null
)

@Serializable
data class JioSaavnAlbumDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("image") val image: List<JioSaavnImageDto>? = null
)

@Serializable
data class JioSaavnPlaylistDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("image") val image: List<JioSaavnImageDto>? = null
)

@Serializable
data class JioSaavnImageDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("url") val url: String? = null
)

@Serializable
data class JioSaavnDownloadUrlDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("url") val url: String? = null
)

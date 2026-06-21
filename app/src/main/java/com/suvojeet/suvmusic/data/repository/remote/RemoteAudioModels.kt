package com.suvojeet.suvmusic.data.repository.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteAudioSearchResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: RemoteAudioSearchData? = null
)

@Serializable
data class RemoteAudioSearchData(
    // `/api/search` (global) nests results under category objects…
    @SerializedName("topQuery") val topQuery: RemoteAudioTopQuery? = null,
    @SerializedName("songs") val songs: RemoteAudioSongsCategory? = null,
    @SerializedName("albums") val albums: RemoteAudioAlbumsCategory? = null,
    @SerializedName("artists") val artists: RemoteAudioArtistsCategory? = null,
    @SerializedName("playlists") val playlists: RemoteAudioPlaylistsCategory? = null,
    // …while category endpoints like `/api/search/songs` return a flat results list.
    @SerializedName("results") val results: List<RemoteAudioSongDto>? = null
)

@Serializable
data class RemoteAudioTopQuery(
    @SerializedName("results") val results: List<RemoteAudioSongDto>? = null
)

@Serializable
data class RemoteAudioSongsCategory(
    @SerializedName("results") val results: List<RemoteAudioSongDto>? = null
)

@Serializable
data class RemoteAudioAlbumsCategory(
    @SerializedName("results") val results: List<RemoteAudioAlbumDto>? = null
)

@Serializable
data class RemoteAudioArtistsCategory(
    @SerializedName("results") val results: List<RemoteAudioArtistDto>? = null
)

@Serializable
data class RemoteAudioPlaylistsCategory(
    @SerializedName("results") val results: List<RemoteAudioPlaylistDto>? = null
)

@Serializable
data class RemoteAudioSongDetailsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: List<RemoteAudioSongDto>? = null
)

@Serializable
data class RemoteAudioSongDto(
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
    @SerializedName("album") val album: RemoteAudioAlbumInfoDto? = null,
    @SerializedName("artists") val artists: RemoteAudioArtistsDto? = null,
    @SerializedName("image") val image: List<RemoteAudioImageDto>? = null,
    @SerializedName("downloadUrl") val downloadUrl: List<RemoteAudioDownloadUrlDto>? = null
)

@Serializable
data class RemoteAudioAlbumInfoDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null
)

@Serializable
data class RemoteAudioArtistsDto(
    @SerializedName("primary") val primary: List<RemoteAudioArtistDto>? = null,
    @SerializedName("featured") val featured: List<RemoteAudioArtistDto>? = null,
    @SerializedName("all") val all: List<RemoteAudioArtistDto>? = null
)

@Serializable
data class RemoteAudioArtistDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("image") val image: List<RemoteAudioImageDto>? = null
)

@Serializable
data class RemoteAudioAlbumDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("image") val image: List<RemoteAudioImageDto>? = null
)

@Serializable
data class RemoteAudioPlaylistDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("image") val image: List<RemoteAudioImageDto>? = null
)

@Serializable
data class RemoteAudioImageDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("url") val url: String? = null
)

@Serializable
data class RemoteAudioDownloadUrlDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("url") val url: String? = null
)

@Serializable
data class RemoteAudioLyricsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: RemoteAudioLyricsDto? = null
)

@Serializable
data class RemoteAudioLyricsDto(
    @SerializedName("lyrics") val lyrics: String? = null,
    @SerializedName("copyright") val copyright: String? = null,
    @SerializedName("snippet") val snippet: String? = null
)

@Serializable
data class RemoteAudioArtistSearchResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: RemoteAudioArtistSearchData? = null
)

@Serializable
data class RemoteAudioArtistSearchData(
    @SerializedName("results") val results: List<RemoteAudioArtistDto>? = null
)

@Serializable
data class RemoteAudioArtistResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: RemoteAudioArtistDetailDto? = null
)

@Serializable
data class RemoteAudioArtistDetailDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("fanCount") val fanCount: String? = null,
    @SerializedName("bio") val bio: List<RemoteAudioArtistBioDto>? = null,
    @SerializedName("wiki") val wiki: String? = null,
    @SerializedName("image") val image: List<RemoteAudioImageDto>? = null,
    @SerializedName("topSongs") val topSongs: List<RemoteAudioSongDto>? = null,
    @SerializedName("topAlbums") val topAlbums: List<RemoteAudioAlbumDto>? = null
)

@Serializable
data class RemoteAudioArtistBioDto(
    @SerializedName("text") val text: String? = null,
    @SerializedName("title") val title: String? = null
)

@Serializable
data class RemoteAudioAlbumSearchResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: RemoteAudioAlbumSearchData? = null
)

@Serializable
data class RemoteAudioAlbumSearchData(
    @SerializedName("results") val results: List<RemoteAudioAlbumDto>? = null
)

@Serializable
data class RemoteAudioPlaylistSearchResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: RemoteAudioPlaylistSearchData? = null
)

@Serializable
data class RemoteAudioPlaylistSearchData(
    @SerializedName("results") val results: List<RemoteAudioPlaylistDto>? = null
)

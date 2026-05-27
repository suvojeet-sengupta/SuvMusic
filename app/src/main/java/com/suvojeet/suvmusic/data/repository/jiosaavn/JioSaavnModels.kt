package com.suvojeet.suvmusic.data.repository.jiosaavn

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JioSaavnSearchResponse(
    val success: Boolean,
    val data: JioSaavnSearchData
)

@Serializable
data class JioSaavnSearchData(
    val topQuery: JioSaavnTopQuery? = null,
    val songs: JioSaavnSongsCategory? = null,
    val albums: JioSaavnAlbumsCategory? = null,
    val artists: JioSaavnArtistsCategory? = null,
    val playlists: JioSaavnPlaylistsCategory? = null
)

@Serializable
data class JioSaavnTopQuery(
    val results: List<JioSaavnSongDto> = emptyList()
)

@Serializable
data class JioSaavnSongsCategory(
    val results: List<JioSaavnSongDto> = emptyList()
)

@Serializable
data class JioSaavnAlbumsCategory(
    val results: List<JioSaavnAlbumDto> = emptyList()
)

@Serializable
data class JioSaavnArtistsCategory(
    val results: List<JioSaavnArtistDto> = emptyList()
)

@Serializable
data class JioSaavnPlaylistsCategory(
    val results: List<JioSaavnPlaylistDto> = emptyList()
)

@Serializable
data class JioSaavnSongDetailsResponse(
    val success: Boolean,
    val data: List<JioSaavnSongDto>
)

@Serializable
data class JioSaavnSongDto(
    val id: String,
    val name: String,
    val type: String? = null,
    val year: String? = null,
    val releaseDate: String? = null,
    val duration: Long? = null,
    val label: String? = null,
    val explicitContent: Boolean? = null,
    val playCount: Long? = null,
    val language: String? = null,
    val hasLyrics: Boolean? = null,
    val url: String? = null,
    val copyright: String? = null,
    val album: JioSaavnAlbumInfoDto? = null,
    val artists: JioSaavnArtistsDto? = null,
    val image: List<JioSaavnImageDto> = emptyList(),
    val downloadUrl: List<JioSaavnDownloadUrlDto> = emptyList()
)

@Serializable
data class JioSaavnAlbumInfoDto(
    val id: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class JioSaavnArtistsDto(
    val primary: List<JioSaavnArtistDto> = emptyList(),
    val featured: List<JioSaavnArtistDto> = emptyList(),
    val all: List<JioSaavnArtistDto> = emptyList()
)

@Serializable
data class JioSaavnArtistDto(
    val id: String? = null,
    val name: String,
    val role: String? = null,
    val type: String? = null,
    val url: String? = null,
    val image: List<JioSaavnImageDto> = emptyList()
)

@Serializable
data class JioSaavnAlbumDto(
    val id: String,
    val name: String,
    val year: String? = null,
    val url: String? = null,
    val image: List<JioSaavnImageDto> = emptyList()
)

@Serializable
data class JioSaavnPlaylistDto(
    val id: String,
    val name: String,
    val url: String? = null,
    val image: List<JioSaavnImageDto> = emptyList()
)

@Serializable
data class JioSaavnImageDto(
    val quality: String,
    val url: String
)

@Serializable
data class JioSaavnDownloadUrlDto(
    val quality: String,
    val url: String
)

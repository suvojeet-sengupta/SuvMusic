package com.suvojeet.suvmusic.core.model

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val description: String? = null,
    val subscribers: String? = null,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val singles: List<Album> = emptyList(),
    val isSubscribed: Boolean = false,
    val channelId: String? = null,
    val views: String? = null,
    val videos: List<Song> = emptyList(),
    val relatedArtists: List<ArtistPreview> = emptyList(),
    val featuredPlaylists: List<Playlist> = emptyList()
)

data class ArtistPreview(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscribers: String? = null
)

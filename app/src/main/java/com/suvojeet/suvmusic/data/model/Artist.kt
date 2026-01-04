package com.suvojeet.suvmusic.data.model

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val description: String? = null,
    val subscribers: String? = null,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val singles: List<Album> = emptyList() // Singles are often treated as mini-albums
)

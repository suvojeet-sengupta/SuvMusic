package com.suvojeet.suvmusic.core.model

data class Playlist(
    val id: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val songs: List<Song>,
    val description: String? = null
)

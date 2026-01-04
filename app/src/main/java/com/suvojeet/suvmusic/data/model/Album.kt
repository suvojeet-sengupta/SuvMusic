package com.suvojeet.suvmusic.data.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: String? = null,
    val thumbnailUrl: String?,
    val description: String? = null,
    val songs: List<Song> = emptyList()
)

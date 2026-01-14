package com.suvojeet.suvmusic.data.model

data class ImportResult(
    val originalTitle: String,
    val originalArtist: String,
    val matchedSong: Song?
)

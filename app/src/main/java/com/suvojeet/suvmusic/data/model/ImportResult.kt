package com.suvojeet.suvmusic.data.model
import com.suvojeet.suvmusic.core.model.*

data class ImportResult(
    val originalTitle: String,
    val originalArtist: String,
    val matchedSong: Song?
)

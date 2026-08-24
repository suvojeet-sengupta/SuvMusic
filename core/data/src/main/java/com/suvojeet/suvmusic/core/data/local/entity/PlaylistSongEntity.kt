package com.suvojeet.suvmusic.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    // A playlist may legitimately contain the same track more than once. Keying by
    // songId caused Room REPLACE inserts to overwrite earlier entries during imports.
    primaryKeys = ["playlistId", "order"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["playlistId", "songId"])
    ]
)
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val duration: Long,
    val source: String,
    val localUri: String? = null,
    val releaseDate: String? = null,
    val addedAt: Long = 0L,
    val order: Int
)

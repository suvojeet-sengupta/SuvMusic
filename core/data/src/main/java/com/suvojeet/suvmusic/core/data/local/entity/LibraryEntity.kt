package com.suvojeet.suvmusic.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val type: String, // "PLAYLIST", "ALBUM", "ARTIST"
    val timestamp: Long = System.currentTimeMillis()
)

data class LibraryItemWithCount(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val type: String,
    val timestamp: Long,
    val songCount: Int
)

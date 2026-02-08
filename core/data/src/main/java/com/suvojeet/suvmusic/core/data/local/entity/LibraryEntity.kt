package com.suvojeet.suvmusic.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String, // artist or author
    val thumbnailUrl: String?,
    val type: String, // "PLAYLIST" or "ALBUM"
    val timestamp: Long = System.currentTimeMillis()
)

package com.suvojeet.suvmusic.core.model

data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val type: LibraryItemType,
    val timestamp: Long
)

enum class LibraryItemType {
    PLAYLIST,
    ALBUM,
    ARTIST,
    UNKNOWN
}

package com.suvojeet.suvmusic.core.model

/**
 * Represents a playlist display item.
 * Used for showing playlists in the UI.
 */
data class PlaylistDisplayItem(
    val id: String = "",
    val name: String,
    val url: String,
    val uploaderName: String,
    val thumbnailUrl: String? = null,
    val songCount: Int = 0,
    val description: String? = null
) {
    /**
     * Extract the playlist ID from the URL.
     */
    fun getPlaylistId(): String {
        if (id.isNotBlank()) return id
        return url.substringAfter("list=").substringBefore("&")
    }
}

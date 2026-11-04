package com.suvojeet.suvmusic.data

import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem

/** Which YouTube Music playlists the app mirrors. Stores exclusions so new playlists default to synced. */
object YouTubeSyncSelection {
    const val LIKED_ID = "LM"

    fun normalize(playlistId: String): String = playlistId.removePrefix("VL")

    fun isSynced(playlistId: String, excluded: Set<String>): Boolean =
        normalize(playlistId) !in excluded

    fun visible(playlists: List<PlaylistDisplayItem>, excluded: Set<String>): List<PlaylistDisplayItem> =
        if (excluded.isEmpty()) playlists else playlists.filter { isSynced(it.id, excluded) }

    fun updated(excluded: Set<String>, playlistId: String, synced: Boolean): Set<String> {
        val id = normalize(playlistId)
        return if (synced) excluded - id else excluded + id
    }

    /** Liked songs has its own row in the picker, so it's dropped from the fetched list. */
    fun pickerItems(playlists: List<PlaylistDisplayItem>): List<PlaylistDisplayItem> =
        playlists.distinctBy { normalize(it.id) }.filter { normalize(it.id) != LIKED_ID }
}

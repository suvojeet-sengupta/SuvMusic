package com.suvojeet.suvmusic.data.model
import com.suvojeet.suvmusic.core.model.*

sealed class RecentSearchItem {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String
    abstract val thumbnailUrl: String?
    abstract val timestamp: Long

    data class SongItem(
        val song: Song,
        override val timestamp: Long = System.currentTimeMillis()
    ) : RecentSearchItem() {
        override val id: String = song.id
        override val title: String = song.title
        override val subtitle: String = song.artist
        override val thumbnailUrl: String? = song.thumbnailUrl
    }

    data class AlbumItem(
        val album: Album,
        override val timestamp: Long = System.currentTimeMillis()
    ) : RecentSearchItem() {
        override val id: String = album.id
        override val title: String = album.title
        override val subtitle: String = "Album • ${album.artist}"
        override val thumbnailUrl: String? = album.thumbnailUrl
    }

    data class PlaylistItem(
        val playlist: Playlist,
        override val timestamp: Long = System.currentTimeMillis()
    ) : RecentSearchItem() {
        override val id: String = playlist.id
        override val title: String = playlist.title
        override val subtitle: String = "Playlist • ${playlist.author}"
        override val thumbnailUrl: String? = playlist.thumbnailUrl
    }
}

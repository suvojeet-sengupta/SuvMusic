package com.suvojeet.suvmusic.data.model

data class HomeSection(
    val title: String,
    val items: List<HomeItem>
)

sealed class HomeItem {
    data class SongItem(val song: Song) : HomeItem()
    data class PlaylistItem(val playlist: PlaylistDisplayItem) : HomeItem()
    data class AlbumItem(val album: Album) : HomeItem()
    data class ArtistItem(val artist: Artist) : HomeItem()
}

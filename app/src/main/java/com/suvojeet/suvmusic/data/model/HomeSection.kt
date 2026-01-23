package com.suvojeet.suvmusic.data.model


enum class HomeSectionType {
    HorizontalCarousel,
    Grid,
    LargeCardWithList,
    VerticalList,
    CommunityCarousel,
    ExploreGrid
}

data class HomeSection(
    val title: String,
    val items: List<HomeItem>,
    val type: HomeSectionType = HomeSectionType.HorizontalCarousel
)

sealed class HomeItem {
    data class SongItem(val song: Song) : HomeItem()
    data class PlaylistItem(val playlist: PlaylistDisplayItem, val previewSongs: List<Song> = emptyList()) : HomeItem()
    data class AlbumItem(val album: Album) : HomeItem()
    data class ArtistItem(val artist: Artist) : HomeItem()
    data class ExploreItem(val title: String, val iconRes: Int, val browseId: String) : HomeItem()
}

package com.suvojeet.suvmusic.data.model
import com.suvojeet.suvmusic.core.model.*


enum class HomeSectionType {
    HorizontalCarousel,
    Grid,
    LargeCardWithList,
    VerticalList,
    CommunityCarousel,
    ExploreGrid,
    QuickPicks
}

data class HomeSection(
    val title: String,
    val items: List<HomeItem>,
    val type: HomeSectionType = HomeSectionType.HorizontalCarousel
)

sealed class HomeItem {
    abstract val id: String
    data class SongItem(val song: Song) : HomeItem() {
        override val id: String = song.id
    }
    data class PlaylistItem(val playlist: PlaylistDisplayItem, val previewSongs: List<Song> = emptyList()) : HomeItem() {
        override val id: String = playlist.id
    }
    data class AlbumItem(val album: Album) : HomeItem() {
        override val id: String = album.id
    }
    data class ArtistItem(val artist: Artist) : HomeItem() {
        override val id: String = artist.id
    }
    data class ExploreItem(val title: String, val iconRes: Int, val browseId: String) : HomeItem() {
        override val id: String = browseId
    }
}

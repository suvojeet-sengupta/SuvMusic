package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song

@Composable
fun HorizontalCarouselSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(title = section.title)
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(section.items) { item ->
                HomeItemCard(
                    item = item, 
                    onSongClick = onSongClick, 
                    onPlaylistClick = onPlaylistClick, 
                    onAlbumClick = onAlbumClick,
                    sectionItems = section.items
                )
            }
        }
    }
}

@Composable
fun VerticalListSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(title = section.title)
        
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            section.items.take(5).forEach { item ->
                // Using MusicCard for vertical list look
                when (item) {
                    is HomeItem.SongItem -> {
                        MusicCard(
                            song = item.song,
                            onClick = {
                                val songs = section.items.filterIsInstance<HomeItem.SongItem>().map { it.song }
                                val index = songs.indexOf(item.song)
                                if (index != -1) onSongClick(songs, index)
                            },
                             backgroundColor = Color.Transparent
                        )
                    }
                    is HomeItem.PlaylistItem -> {
                        // Create a temporary song object to use MusicCard for consistency
                        val tempSong = Song(
                            id = item.playlist.id,
                            title = item.playlist.name,
                            artist = item.playlist.uploaderName,
                             thumbnailUrl = item.playlist.thumbnailUrl,
                             album = "Playlist",
                             duration = 0L,
                             source = com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE
                        )
                         MusicCard(
                            song = tempSong,
                            onClick = { onPlaylistClick(item.playlist) },
                            backgroundColor = Color.Transparent
                        )
                    }
                    is HomeItem.AlbumItem -> {
                        val tempSong = Song(
                            id = item.album.id,
                            title = item.album.title,
                            artist = item.album.artist,
                             thumbnailUrl = item.album.thumbnailUrl,
                             album = item.album.year ?: "Album",
                             duration = 0L,
                             source = com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE
                        )
                        MusicCard(
                            song = tempSong,
                            onClick = { onAlbumClick(item.album) },
                            backgroundColor = Color.Transparent
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun LargeCardWithListSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    if (section.items.isEmpty()) return
    
    val firstItem = section.items.first()
    val otherItems = section.items.drop(1).take(3)
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(title = section.title)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Large Card (Left)
            Box(
                modifier = Modifier
                    .weight(0.40f)
                    .aspectRatio(1f) // Square
            ) {
                 HomeItemCardLarge(
                    item = firstItem,
                    onSongClick = onSongClick,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick,
                    sectionItems = section.items
                 )
            }
            
            // List (Right)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                otherItems.forEach { item ->
                    when (item) {
                        is HomeItem.SongItem -> {
                            MusicCard(
                                song = item.song,
                                onClick = {
                                    val songs = section.items.filterIsInstance<HomeItem.SongItem>().map { it.song }
                                    val index = songs.indexOf(item.song)
                                    if (index != -1) onSongClick(songs, index)
                                },
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.height(60.dp)
                            )
                        }
                        is HomeItem.PlaylistItem -> {
                             // Simplified rendering for list items if not Song
                             MusicCard(
                                song = Song(item.playlist.id, item.playlist.name, item.playlist.uploaderName, "Playlist", 0L, item.playlist.thumbnailUrl, com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE),
                                onClick = { onPlaylistClick(item.playlist) },
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.height(60.dp)
                            )
                        }
                         is HomeItem.AlbumItem -> {
                             MusicCard(
                                song = Song(item.album.id, item.album.title, item.album.artist, "Album", 0L, item.album.thumbnailUrl, com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE),
                                onClick = { onAlbumClick(item.album) },
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.height(60.dp)
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun GridSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(title = section.title)
        
        // Use a LazyHorizontalGrid for grid feel but horizontally scrolling
        // OR render a fixed grid if it's supposed to be vertical. 
        // Based on "Fresh finds" typically it creates a 2-row horizontal scroll or a vertical grid.
        // Assuming horizontal scroll with 2 rows for now as it fits typical music apps better within a vertical feed.
        
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.height(340.dp), // Height for 2 items + spacing
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(section.items) { item ->
                HomeItemCard(
                    item = item, 
                    onSongClick = onSongClick, 
                    onPlaylistClick = onPlaylistClick, 
                    onAlbumClick = onAlbumClick,
                     sectionItems = section.items
                )
            }
        }
    }
}


@Composable
fun HomeItemCardLarge(
    item: HomeItem,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    sectionItems: List<HomeItem>
) {
    val context = LocalContext.current
    
    // Large square card implementation
    val (title, subtitle, imageUrl) = when (item) {
        is HomeItem.SongItem -> Triple(item.song.title, item.song.artist, item.song.thumbnailUrl)
        is HomeItem.PlaylistItem -> Triple(item.playlist.name, item.playlist.uploaderName, item.playlist.thumbnailUrl)
        is HomeItem.AlbumItem -> Triple(item.album.title, item.album.artist, item.album.thumbnailUrl)
        else -> Triple("", "", null)
    }
    
    val highResThumbnail = imageUrl?.let { url ->
         url.replace(Regex("w\\d+-h\\d+"), "w544-h544")
            .replace(Regex("=w\\d+"), "=w544")
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bounceClick {
                 when (item) {
                    is HomeItem.SongItem -> {
                         val songs = sectionItems.filterIsInstance<HomeItem.SongItem>().map { it.song }
                         val index = songs.indexOf(item.song)
                         if (index != -1) onSongClick(songs, index)
                    }
                    is HomeItem.PlaylistItem -> onPlaylistClick(item.playlist)
                    is HomeItem.AlbumItem -> onAlbumClick(item.album)
                    else -> {}
                }
            }
    ) {
         AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResThumbnail)
                .crossfade(true)
                .size(544)
                .build(),
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

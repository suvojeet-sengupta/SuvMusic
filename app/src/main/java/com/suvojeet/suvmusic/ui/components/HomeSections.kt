package com.suvojeet.suvmusic.ui.components

import com.suvojeet.suvmusic.ui.utils.SharedTransitionKeys
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier 
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.util.ImageUtils
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun HorizontalCarouselSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                             source = com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE
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
                             source = com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE
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
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    if (section.items.isEmpty()) return
    
    val firstItem = section.items.first()
    val otherItems = section.items.drop(1).take(3)
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                                modifier = Modifier.height(60.dp)
                            )
                        }
                        is HomeItem.PlaylistItem -> {
                             // Simplified rendering for list items if not Song
                             MusicCard(
                                song = Song(item.playlist.id, item.playlist.name, item.playlist.uploaderName, "Playlist", 0L, item.playlist.thumbnailUrl, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE),
                                onClick = { onPlaylistClick(item.playlist) },
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.height(60.dp)
                            )
                        }
                         is HomeItem.AlbumItem -> {
                             MusicCard(
                                song = Song(item.album.id, item.album.title, item.album.artist, "Album", 0L, item.album.thumbnailUrl, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE),
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
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
fun QuickPicksSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeSectionHeader(title = section.title)
        
        val chunkedItems = section.items.chunked(4)
        val lazyListState = rememberLazyListState()
        
        LazyRow(
            state = lazyListState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(chunkedItems) { columnItems ->
                Column(
                    modifier = Modifier.fillParentMaxWidth(0.92f), // Peeking effect
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    columnItems.forEach { item ->
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
                                MusicCard(
                                    song = Song(item.playlist.id, item.playlist.name, item.playlist.uploaderName, "Playlist", 0L, item.playlist.thumbnailUrl, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE),
                                    onClick = { onPlaylistClick(item.playlist) },
                                    backgroundColor = Color.Transparent
                                )
                            }
                            is HomeItem.AlbumItem -> {
                                MusicCard(
                                    song = Song(item.album.id, item.album.title, item.album.artist, "Album", 0L, item.album.thumbnailUrl, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE),
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
    // Adapter to use NewReleaseCard for HomeItem
    val (title, subtitle, imageUrl) = when (item) {
        is HomeItem.SongItem -> Triple(item.song.title, item.song.artist, item.song.thumbnailUrl)
        is HomeItem.PlaylistItem -> Triple(item.playlist.name, item.playlist.uploaderName, item.playlist.thumbnailUrl)
        is HomeItem.AlbumItem -> Triple(item.album.title, item.album.artist, item.album.thumbnailUrl)
        else -> Triple("", "", null)
    }

    NewReleaseCard(
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        onClick = {
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
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun CommunityCarouselSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onStartRadio: () -> Unit = {},
    onSavePlaylist: (PlaylistDisplayItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeSectionHeader(title = section.title)
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(section.items.filterIsInstance<HomeItem.PlaylistItem>()) { item ->
                CommunityPlaylistCard(
                    item = item,
                    onPlaylistClick = onPlaylistClick,
                    onSongClick = onSongClick,
                    onStartRadio = onStartRadio,
                    onSave = { onSavePlaylist(item.playlist) }
                )
            }
        }
    }
}

@Composable
fun CommunityPlaylistCard(
    item: HomeItem.PlaylistItem,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onStartRadio: () -> Unit,
    onSave: () -> Unit
) {
    var isSaved by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .width(320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF2A2020)) // Dark reddish brown placeholder
            .clickable { onPlaylistClick(item.playlist) }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Cover Art
                AsyncImage(
                    model = item.playlist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.playlist.uploaderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                     Text(
                        text = "${item.playlist.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Songs Preview
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item.previewSongs.take(3).forEachIndexed { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Play this song within the preview context
                                onSongClick(item.previewSongs, index)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist, 
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                FilledIconButton(
                    onClick = { 
                         // Play all preview songs
                         if (item.previewSongs.isNotEmpty()) {
                             onSongClick(item.previewSongs, 0)
                         } else {
                             onPlaylistClick(item.playlist)
                         }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black
                    )
                }
                
                // Radio Button
                IconButton(
                    onClick = { onStartRadio() },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Radio,
                        contentDescription = "Radio"
                    )
                }
                
                // Save Button
                IconButton(
                    onClick = { 
                        isSaved = !isSaved
                        onSave() 
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) Color.White else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreGridSection(
    section: HomeSection,
    onExploreItemClick: (String, String) -> Unit, // browseId, title
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeSectionHeader(title = section.title)
        
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
             val items = section.items.filterIsInstance<HomeItem.ExploreItem>()
             val rows = items.chunked(2)
             
             rows.forEach { rowItems ->
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(16.dp)
                 ) {
                     rowItems.forEach { item ->
                         ExploreItemCard(
                             item = item,
                             modifier = Modifier.weight(1f),
                             onClick = { onExploreItemClick(item.browseId, item.title) }
                         )
                     }
                     if (rowItems.size == 1) {
                         Spacer(modifier = Modifier.weight(1f))
                     }
                 }
             }
        }
    }
}

@Composable
fun ExploreItemCard(
    item: HomeItem.ExploreItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E)) // Dark grey
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // Layout: Icon Top Left, Text Bottom Left? Screenshot shows Icon Top Left, Text Bottom Left.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
             Icon(
                 painter = painterResource(id = item.iconRes),
                 contentDescription = null,
                 tint = Color.White,
                 modifier = Modifier.size(24.dp)
             )
             
             Text(
                 text = item.title,
                 style = MaterialTheme.typography.titleSmall,
                 fontWeight = FontWeight.Bold,
                 color = Color.White
             )
        }
    }
}

@Composable
fun NewReleaseCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val context = LocalContext.current
    val imageRequest = ImageRequest.Builder(context)
        .data(imageUrl)
        .crossfade(true)
        .build()

    androidx.compose.material3.Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left Section: Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action Button (Arrow)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Right Section: Image
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

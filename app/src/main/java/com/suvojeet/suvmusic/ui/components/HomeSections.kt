package com.suvojeet.suvmusic.ui.components

import com.suvojeet.suvmusic.util.dpadFocusable
import com.suvojeet.suvmusic.ui.utils.SharedTransitionKeys
import com.suvojeet.suvmusic.ui.theme.NewReleaseCardShape
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier 
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.util.ImageUtils
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.HomeItem
import com.suvojeet.suvmusic.core.model.HomeSection
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun HorizontalCarouselSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSongMoreClick: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (section.items.isEmpty()) return

    val items = remember(section.items) {
        section.items.distinctBy { it.id }
    }

    val songs = remember(items) {
        items.filterIsInstance<HomeItem.SongItem>().map { it.song }
    }

    Column(modifier = modifier) {
        HomeSectionHeader(title = section.title)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id },
                contentType = { _, item -> item::class }
            ) { index, item ->
                when (item) {
                    is HomeItem.SongItem -> {
                        HomeItemCard(
                            item = item,
                            onSongClick = onSongClick,
                            onPlaylistClick = onPlaylistClick,
                            onAlbumClick = onAlbumClick,
                            sectionItems = items,
                            onSongMoreClick = onSongMoreClick
                        )
                    }
                    is HomeItem.PlaylistItem -> {
                        HomeItemCard(
                            item = item, 
                            onSongClick = onSongClick, 
                            onPlaylistClick = onPlaylistClick, 
                            onAlbumClick = onAlbumClick,
                            sectionItems = items,
                            onSongMoreClick = onSongMoreClick
                        )
                    }
                    is HomeItem.AlbumItem -> {
                        HomeItemCard(
                            item = item, 
                            onSongClick = onSongClick, 
                            onPlaylistClick = onPlaylistClick, 
                            onAlbumClick = onAlbumClick,
                            sectionItems = items,
                            onSongMoreClick = onSongMoreClick
                        )
                    }
                    else -> {}
                }
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
    onSongMoreClick: (Song) -> Unit = {},
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
            val displayItems = remember(section.items) { section.items.take(5) }
            val songs = remember(section.items) {
                section.items.filterIsInstance<HomeItem.SongItem>().map { it.song }
            }
            displayItems.forEach { item ->
                when (item) {
                    is HomeItem.SongItem -> {
                         val onCardClick = remember(item.song, songs) {
                             {
                                 val index = songs.indexOf(item.song)
                                 if (index != -1) onSongClick(songs, index)
                             }
                         }
                         val onMoreClick = remember(item.song) { { onSongMoreClick(item.song) } }
                         
                         MusicCard(
                            song = item.song,
                            onClick = onCardClick,
                             onMoreClick = onMoreClick,
                             backgroundColor = Color.Transparent
                        )
                    }
                    is HomeItem.PlaylistItem -> {
                        val tempSong = remember(item.playlist) {
                            Song(
                                id = item.playlist.id,
                                title = item.playlist.name,
                                artist = item.playlist.uploaderName,
                                thumbnailUrl = item.playlist.thumbnailUrl,
                                album = "Playlist",
                                duration = 0L,
                                source = com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE
                            )
                        }
                        val onPlaylistCardClick = remember(item.playlist) { { onPlaylistClick(item.playlist) } }
                         MusicCard(
                            song = tempSong,
                            onClick = onPlaylistCardClick,
                            backgroundColor = Color.Transparent
                        )
                    }
                    is HomeItem.AlbumItem -> {
                        val tempSong = remember(item.album) {
                            Song(
                                id = item.album.id,
                                title = item.album.title,
                                artist = item.album.artist,
                                thumbnailUrl = item.album.thumbnailUrl,
                                album = item.album.year ?: "Album",
                                duration = 0L,
                                source = com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE
                            )
                        }
                        val onAlbumCardClick = remember(item.album) { { onAlbumClick(item.album) } }
                        MusicCard(
                            song = tempSong,
                            onClick = onAlbumCardClick,
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
    onSongMoreClick: (Song) -> Unit = {},
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
                    sectionItems = section.items,
                    onSongMoreClick = onSongMoreClick
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
                            val onCardClick = remember(item.song) {
                                {
                                    val songs = section.items.filterIsInstance<HomeItem.SongItem>().map { it.song }
                                    val index = songs.indexOf(item.song)
                                    if (index != -1) onSongClick(songs, index)
                                }
                            }
                            val onMoreClick = remember(item.song) { { onSongMoreClick(item.song) } }
                            MusicCard(
                                song = item.song,
                                onClick = onCardClick,
                                onMoreClick = onMoreClick,
                                modifier = Modifier.height(60.dp)
                            )
                        }
                        is HomeItem.PlaylistItem -> {
                             // Simplified rendering for list items if not Song
                             val tempSong = remember(item.playlist) {
                                 Song(item.playlist.id, item.playlist.name, item.playlist.uploaderName, "Playlist", 0L, item.playlist.thumbnailUrl, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE)
                             }
                             val onPlaylistCardClick = remember(item.playlist) { { onPlaylistClick(item.playlist) } }
                             MusicCard(
                                song = tempSong,
                                onClick = onPlaylistCardClick,
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.height(60.dp)
                            )
                        }
                         is HomeItem.AlbumItem -> {
                             val tempSong = remember(item.album) {
                                 Song(item.album.id, item.album.title, item.album.artist, "Album", 0L, item.album.thumbnailUrl, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE)
                             }
                             val onAlbumCardClick = remember(item.album) { { onAlbumClick(item.album) } }
                             MusicCard(
                                song = tempSong,
                                onClick = onAlbumCardClick,
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
    onSongMoreClick: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Pre-chunk items into columns of 2 for a grid layout.
    // Using a regular Row + horizontalScroll instead of LazyHorizontalGrid
    // to avoid nested lazy layout conflicts with the parent LazyColumn.
    val isLandscape = com.suvojeet.suvmusic.ui.utils.isLandscape()
    val chunkCount = if (isLandscape) 1 else 2
    val chunkedItems = remember(section.items, isLandscape) { section.items.chunked(chunkCount) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeSectionHeader(title = section.title)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            chunkedItems.forEach { columnItems ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    columnItems.forEach { item ->
                        HomeItemCard(
                            item = item,
                            onSongClick = onSongClick,
                            onPlaylistClick = onPlaylistClick,
                            onAlbumClick = onAlbumClick,
                            sectionItems = section.items,
                            onSongMoreClick = onSongMoreClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPickItem(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val highResThumbnail = remember(song.thumbnailUrl) {
        ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl, size = 160)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .dpadFocusable(onClick = onClick, shape = RoundedCornerShape(12.dp)),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flat-leading thumbnail — square right edge gives the row a
            // distinctive silhouette vs the squircle/round cards used
            // elsewhere on Home.
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(highResThumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = song.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 14.dp,
                            bottomStart = 14.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            // Thin vertical accent stripe sets the row apart from a
            // generic ListItem.
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
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
    onSongMoreClick: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (section.items.isEmpty()) return

    val items = remember(section.items) {
        section.items.distinctBy { it.id }
    }
    val songs = remember(items) {
        items.filterIsInstance<HomeItem.SongItem>().map { it.song }
    }

    val isLandscape = com.suvojeet.suvmusic.ui.utils.isLandscape()
    val chunkCount = if (isLandscape) 2 else 4 // Restore to 4
    val chunkedItems = remember(items, isLandscape) { items.chunked(chunkCount) }
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeSectionHeader(
            title = section.title,
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            if (songs.isNotEmpty()) {
                                onSongClick(songs, 0)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Play all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )

        LazyRow(
            state = lazyListState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            chunkedItems.forEach { columnItems ->
                item(
                    key = columnItems.firstOrNull()?.id ?: java.util.UUID.randomUUID().toString(),
                    contentType = "column"
                ) {
                    Column(
                        modifier = Modifier.fillParentMaxWidth(0.85f), // Refined peeking
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        columnItems.forEach { item ->
                            when (item) {
                                is HomeItem.SongItem -> {
                                    QuickPickItem(
                                        song = item.song,
                                        onClick = {
                                            val index = songs.indexOf(item.song)
                                            if (index != -1) onSongClick(songs, index)
                                        },
                                        onMoreClick = { onSongMoreClick(item.song) }
                                    )
                                }
                                is HomeItem.PlaylistItem -> {
                                    val tempSong = remember(item.playlist.id) {
                                        Song(item.playlist.id, item.playlist.name, item.playlist.uploaderName, "Playlist", 0L, item.playlist.thumbnailUrl, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE)
                                    }
                                    QuickPickItem(
                                        song = tempSong,
                                        onClick = { onPlaylistClick(item.playlist) }
                                    )
                                }
                                is HomeItem.AlbumItem -> {
                                    val tempSong = remember(item.album.id) {
                                        Song(item.album.id, item.album.title, item.album.artist, "Album", 0L, item.album.thumbnailUrl, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE)
                                    }
                                    QuickPickItem(
                                        song = tempSong,
                                        onClick = { onAlbumClick(item.album) }
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
}

@Composable
fun HomeItemCardLarge(
    item: HomeItem,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    sectionItems: List<HomeItem>,
    onSongMoreClick: (Song) -> Unit = {}
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
        onMoreClick = if (item is HomeItem.SongItem) {
            { onSongMoreClick(item.song) }
        } else null,
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
    onSongMoreClick: (Song) -> Unit = {},
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
            val items = section.items.filterIsInstance<HomeItem.PlaylistItem>().distinctBy { it.playlist.id }
            items(
                items = items,
                key = { it.playlist.id }
            ) { item ->
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
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = Modifier
            .width(320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.playlist.uploaderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                     Text(
                        text = "${item.playlist.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant.copy(alpha = 0.7f),
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
                            .clip(RoundedCornerShape(8.dp))
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
                                color = onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist, 
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More",
                            tint = onSurfaceVariant,
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
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play"
                    )
                }
                
                // Radio Button
                IconButton(
                    onClick = { onStartRadio() },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = onSurface)
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
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = if (isSaved) MaterialTheme.colorScheme.primary else onSurface)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Save"
                    )
                }
            }
        }
    }
}

/**
 * Explore grid, redesigned as an asymmetric Pinterest-style mosaic. Tiles
 * alternate between "tall" (120dp) and "short" (80dp) per row so the block
 * doesn't read as a uniform 2×N grid. Each tile carries a subtle hue tint
 * derived from its title hash, varying corner radii (round / squircle-ish
 * / pill-leaning), and a faint gradient backplate so individual tiles
 * stand apart instead of forming one beige slab.
 */
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val items = section.items.filterIsInstance<HomeItem.ExploreItem>()
            val isLandscape = com.suvojeet.suvmusic.ui.utils.isLandscape()
            val chunkCount = if (isLandscape) 1 else 2
            val rows = items.chunked(chunkCount)

            rows.forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    rowItems.forEachIndexed { colIndex, item ->
                        // Stagger pattern: each row flips which column is tall,
                        // landscape-mode rows fall back to the standard short tile.
                        val isTall = !isLandscape && ((rowIndex + colIndex) % 2 == 0)
                        ExploreItemCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            isTall = isTall,
                            tileIndex = rowIndex * 10 + colIndex,
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
    isTall: Boolean = false,
    tileIndex: Int = 0,
    onClick: () -> Unit
) {
    // Title-hash-derived hue gives each tile a unique tint while staying
    // tonally compatible with the active color scheme.
    val accentSeed = remember(item.title) { (item.title.hashCode() and 0xFFFF) / 65535f }
    val baseAccent = MaterialTheme.colorScheme.primary
    val tilePalette = listOf(
        baseAccent,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )
    val accent = tilePalette[(tileIndex + (accentSeed * 1000).toInt()) % tilePalette.size]
    val shape = when (tileIndex % 3) {
        0 -> RoundedCornerShape(20.dp)
        1 -> RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp)
        else -> RoundedCornerShape(14.dp)
    }

    Box(
        modifier = modifier
            .height(if (isTall) 120.dp else 84.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isTall) 2 else 1,
                overflow = TextOverflow.Ellipsis
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
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val context = LocalContext.current
    val highResThumbnail = remember(imageUrl) {
        ImageUtils.getHighResThumbnailUrl(imageUrl, size = 544)
    }
    
    val imageRequest = remember(highResThumbnail, context) {
        ImageRequest.Builder(context)
            .data(highResThumbnail)
            .crossfade(true)
            .build()
    }

    androidx.compose.material3.Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = NewReleaseCardShape,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                
                // Bottom Section: Play Button & More
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.End,
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMoreClick != null) {
                        IconButton(onClick = onMoreClick) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Surface(
                        onClick = onClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Right Section: Image
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Genre carousel — square cover wrapped in a thick tinted ring whose color
// is derived from the section title (genre name). Used only by the
// "Because you like X" block so genre mixes don't blur into regular albums.
// -----------------------------------------------------------------------------

private val GENRE_PALETTE = listOf(
    Color(0xFFE91E63), // pink
    Color(0xFF9C27B0), // purple
    Color(0xFF3F51B5), // indigo
    Color(0xFF009688), // teal
    Color(0xFFFF5722), // deep orange
    Color(0xFFFFB300), // amber
    Color(0xFF4CAF50), // green
    Color(0xFF00BCD4), // cyan
)

private fun genreAccentFor(title: String): Color {
    val hash = title.hashCode().toLong() and 0xFFFFFFFFL
    return GENRE_PALETTE[(hash % GENRE_PALETTE.size).toInt()]
}

@Composable
fun GenreCarousel(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    if (section.items.isEmpty()) return
    val accent = remember(section.title) { genreAccentFor(section.title) }
    val items = remember(section.items) { section.items.distinctBy { it.id } }
    val songs = remember(items) {
        items.filterIsInstance<HomeItem.SongItem>().map { it.song }
    }

    Column(modifier = modifier) {
        // Genre tag chip preceding the title — visually claims the row.
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.4).sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id },
                contentType = { _, item -> "genre_${item::class}" }
            ) { _, item ->
                val (title, subtitle, image) = when (item) {
                    is HomeItem.SongItem -> Triple(item.song.title, item.song.artist, item.song.thumbnailUrl)
                    is HomeItem.PlaylistItem -> Triple(item.playlist.name, item.playlist.uploaderName, item.playlist.thumbnailUrl)
                    is HomeItem.AlbumItem -> Triple(item.album.title, item.album.artist, item.album.thumbnailUrl)
                    else -> Triple("", "", null)
                }
                GenreRingCard(
                    title = title,
                    subtitle = subtitle,
                    imageUrl = image,
                    accent = accent,
                    onClick = {
                        when (item) {
                            is HomeItem.SongItem -> {
                                val idx = songs.indexOf(item.song)
                                if (idx != -1) onSongClick(songs, idx)
                            }
                            is HomeItem.PlaylistItem -> onPlaylistClick(item.playlist)
                            is HomeItem.AlbumItem -> onAlbumClick(item.album)
                            else -> {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GenreRingCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    accent: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverSize = 144.dp
    Column(
        modifier = Modifier
            .width(coverSize)
            .bounceClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(RoundedCornerShape(18.dp))
                .background(accent.copy(alpha = 0.9f))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Personalized Mix carousel — a vinyl-disc treatment used only inside the
// "Personalized for you" block. Cover art sits inside a square sleeve with a
// black disc peeking out from the trailing edge so these mixes are visually
// distinct from regular album/playlist cards.
// -----------------------------------------------------------------------------

@Composable
fun PersonalizedMixCarousel(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    if (section.items.isEmpty()) return

    val items = remember(section.items) { section.items.distinctBy { it.id } }
    val songs = remember(items) {
        items.filterIsInstance<HomeItem.SongItem>().map { it.song }
    }

    Column(modifier = modifier) {
        HomeSectionHeader(title = section.title)
        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id },
                contentType = { _, item -> "vinyl_${item::class}" }
            ) { _, item ->
                val (title, subtitle, image) = when (item) {
                    is HomeItem.SongItem -> Triple(item.song.title, item.song.artist, item.song.thumbnailUrl)
                    is HomeItem.PlaylistItem -> Triple(item.playlist.name, item.playlist.uploaderName, item.playlist.thumbnailUrl)
                    is HomeItem.AlbumItem -> Triple(item.album.title, item.album.artist, item.album.thumbnailUrl)
                    else -> Triple("", "", null)
                }
                PersonalizedMixCard(
                    title = title,
                    subtitle = subtitle,
                    imageUrl = image,
                    onClick = {
                        when (item) {
                            is HomeItem.SongItem -> {
                                val idx = songs.indexOf(item.song)
                                if (idx != -1) onSongClick(songs, idx)
                            }
                            is HomeItem.PlaylistItem -> onPlaylistClick(item.playlist)
                            is HomeItem.AlbumItem -> onAlbumClick(item.album)
                            else -> {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PersonalizedMixCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverSize = 152.dp
    val discPeek = 36.dp

    Column(
        modifier = Modifier
            .width(coverSize + discPeek)
            .bounceClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .height(coverSize)
                .fillMaxWidth()
        ) {
            // Vinyl disc — sits behind the sleeve, peeks from the right.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(coverSize)
                    .offset(x = discPeek - 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
            ) {
                // Concentric "record groove" ring + label disc.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(coverSize * 0.55f)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(coverSize * 0.32f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                )
            }

            // Sleeve / cover art.
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(coverSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = discPeek)
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = discPeek)
            )
        }
    }
}

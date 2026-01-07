package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.HomeLoadingSkeleton
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel
import java.util.Calendar

/**
 * Home screen with dynamic sections fetched from YouTube Music.
 */
@Composable
fun HomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (com.suvojeet.suvmusic.data.model.Album) -> Unit,
    onRecentsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Loading skeleton - only show if loading AND no cached data
        AnimatedVisibility(
            visible = uiState.isLoading && uiState.homeSections.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HomeLoadingSkeleton()
        }
        
        // Actual content - show if we have data (even if still refreshing in background)
        AnimatedVisibility(
            visible = uiState.homeSections.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Greeting Header
                    item {
                        ProfileHeader(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            onRecentsClick = onRecentsClick
                        )
                    }
                    
                    items(uiState.homeSections) { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeSectionHeader(title = section.title)
                            
                            if (section.title.contains("Quick picks", ignoreCase = true) || 
                                section.title.contains("Listen again", ignoreCase = true)) {
                                // Render as Grid-like or specialized list if needed
                                // For now, let's keep it consistent as a row for "Listen Again", 
                                // but maybe "Quick Picks" could be a 2-row grid?
                                // Implementing as a horizontal grid (2 rows) is complex in LazyRow.
                                // Falling back to standard row for simplicity unless it's strictly "Quick Picks"
                                // where we can use a Column of Rows.
                                
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(section.items) { item ->
                                        HomeItemCard(item, onSongClick, onPlaylistClick, onAlbumClick, sectionItems = section.items)
                                    }
                                }
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(section.items) { item ->
                                        HomeItemCard(item, onSongClick, onPlaylistClick, onAlbumClick, sectionItems = section.items)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Refreshing Indicator
                if (uiState.isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeItemCard(
    item: HomeItem,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (com.suvojeet.suvmusic.data.model.Album) -> Unit,
    sectionItems: List<HomeItem>
) {
    when (item) {
        is HomeItem.SongItem -> {
            MediumSongCard(
                song = item.song,
                onClick = { 
                    // Extract all songs from the section for the queue
                    val songs = sectionItems.filterIsInstance<HomeItem.SongItem>().map { it.song }
                    val index = songs.indexOf(item.song)
                    if (index != -1) onSongClick(songs, index)
                }
            )
        }
        is HomeItem.PlaylistItem -> {
            PlaylistDisplayCard(
                playlist = item.playlist,
                onClick = { onPlaylistClick(item.playlist) }
            )
        }
        is HomeItem.AlbumItem -> {
            // Render album similar to playlist
            PlaylistDisplayCard(
                playlist = PlaylistDisplayItem(
                    id = item.album.id,
                    name = item.album.title,
                    url = "",
                    uploaderName = item.album.artist,
                    thumbnailUrl = item.album.thumbnailUrl
                ),
                onClick = { 
                    onAlbumClick(item.album)
                }
            )
        }
        is HomeItem.ArtistItem -> {
            // Placeholder for Artist
        }
    }
}

// -----------------------------------------------------------------------------
// Component Definitions
// -----------------------------------------------------------------------------

/**
 * Get greeting based on time of day
 */
private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
private fun ProfileHeader(
    modifier: Modifier = Modifier,
    onRecentsClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Greeting Text
        Text(
            text = getGreeting(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Recents Button
        IconButton(onClick = onRecentsClick) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Recents",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistDisplayCard(
    playlist: PlaylistDisplayItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Get high-res thumbnail (replace w120 or similar with w544)
    val highResThumbnail = playlist.thumbnailUrl?.let { url ->
        url.replace(Regex("w\\d+-h\\d+"), "w544-h544")
            .replace(Regex("=w\\d+"), "=w544")
    } ?: playlist.thumbnailUrl
    
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResThumbnail)
                .crossfade(true)
                .size(544)  // Request high-res
                .build(),
            contentDescription = playlist.name,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = playlist.uploaderName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun MediumSongCard(
    song: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Get high-res thumbnail (replace w120 or similar with w544)
    val highResThumbnail = song.thumbnailUrl?.let { url ->
        url.replace(Regex("w\\d+-h\\d+"), "w544-h544")
            .replace(Regex("=w\\d+"), "=w544")
    } ?: song.thumbnailUrl
    
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResThumbnail)
                .crossfade(true)
                .size(544)  // Request high-res
                .build(),
            contentDescription = song.title,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}


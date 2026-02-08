package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.HomeLoadingSkeleton
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel
import com.suvojeet.suvmusic.util.ImageUtils
import com.suvojeet.suvmusic.util.dpadFocusable
import java.util.Calendar

/**
 * Dedicated Home Screen for Android TV / Desktop mode.
 * Features a high-fidelity "Hero" section, horizontal scrolling rows, and optimized D-pad navigation.
 */
@Composable
fun TvHomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (com.suvojeet.suvmusic.data.model.PlaylistDisplayItem) -> Unit,
    onAlbumClick: (com.suvojeet.suvmusic.data.model.Album) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.isLoading && uiState.homeSections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Hero Item: Use the first recommendation or the first item from the first playable section
    val heroItem = remember(uiState) {
        uiState.recommendations.firstOrNull() 
            ?: (uiState.homeSections.firstOrNull()?.items?.firstOrNull() as? com.suvojeet.suvmusic.data.model.HomeItem.SongItem)?.song
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 50.dp)
    ) {
        // --- HERO SECTION ---
        item {
            if (heroItem != null) {
                TvHeroSection(
                    song = heroItem,
                    onPlayClick = {
                         // Find the list containing this song to play effectively
                         onSongClick(listOf(heroItem), 0)
                    }
                )
            }
        }

        // --- QUICK ACCESS GRID (Desktop Style) ---
        if (uiState.recommendations.isNotEmpty()) {
            item {
                TvSectionTitle("Quick Access")
                TvQuickAccessGrid(
                    items = uiState.recommendations.take(8), // Show more columns on TV
                    onItemClick = { song ->
                        onSongClick(uiState.recommendations, uiState.recommendations.indexOf(song))
                    }
                )
            }
        }

        // --- SECTIONS ---
        items(uiState.homeSections) { section ->
            if (section.items.isNotEmpty()) {
                TvHorizontalSection(
                    section = section,
                    onSongClick = onSongClick,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick
                )
            }
        }
    }
}


@Composable
private fun TvHeroSection(
    song: Song,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val highResImage = ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl) ?: song.thumbnailUrl
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
    ) {
        // Background Image with Gradient Overlay
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResImage)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient Fade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 1500f
                    )
                )
        )
        
        // Vertical Fade for smooth content transition
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 400f
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 56.dp, bottom = 48.dp)
                .widthIn(max = 600.dp)
        ) {
            Text(
                text = "FEATURED",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.displaySmall, // Large text for TV
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.dpadFocusable(
                    focusedScale = 1.05f,
                    shape = RoundedCornerShape(12.dp)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Play Now", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TvSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 56.dp, top = 32.dp, bottom = 16.dp)
    )
}

@Composable
private fun TvQuickAccessGrid(
    items: List<Song>,
    onItemClick: (Song) -> Unit
) {
    // 2 Rows, Horizontal scrolling grid feel
    Column(
        modifier = Modifier.padding(horizontal = 56.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val rows = items.chunked(4) // 4 items per row
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    TvQuickAccessCard(
                        song = item,
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        onClick = { onItemClick(item) }
                    )
                }
                // Fill empty space
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TvQuickAccessCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .dpadFocusable(
                onClick = onClick,
                focusedScale = 1.05f,
                shape = RoundedCornerShape(8.dp),
                borderWidth = 2.dp,
                borderColor = MaterialTheme.colorScheme.primary
            )
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun TvHorizontalSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (com.suvojeet.suvmusic.data.model.PlaylistDisplayItem) -> Unit,
    onAlbumClick: (com.suvojeet.suvmusic.data.model.Album) -> Unit
) {
    TvSectionTitle(section.title)
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(section.items) { item ->
            when (item) {
                is com.suvojeet.suvmusic.data.model.HomeItem.SongItem -> TvSongCard(song = item.song, onClick = { onSongClick(listOf(item.song), 0) })
                is com.suvojeet.suvmusic.data.model.HomeItem.PlaylistItem -> TvPlaylistCard(playlist = item.playlist, onClick = { onPlaylistClick(item.playlist) })
                is com.suvojeet.suvmusic.data.model.HomeItem.AlbumItem -> TvAlbumCard(album = item.album, onClick = { onAlbumClick(item.album) })
                else -> {} // Skip unsupported items for now
            }
        }
    }
}

@Composable
private fun TvSongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .dpadFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp), 
                focusedScale = 1.1f,
                borderColor = MaterialTheme.colorScheme.primary
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(8.dp) // Inner padding so focus border doesn't overlap text
    ) {
        AsyncImage(
            model = ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl) ?: song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(184.dp) // 200 - 16 padding
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvPlaylistCard(playlist: com.suvojeet.suvmusic.data.model.PlaylistDisplayItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .dpadFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.1f
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageUtils.getHighResThumbnailUrl(playlist.thumbnailUrl) ?: playlist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(184.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Playlist",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TvAlbumCard(album: com.suvojeet.suvmusic.data.model.Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .dpadFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.1f
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageUtils.getHighResThumbnailUrl(album.thumbnailUrl) ?: album.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(184.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Album",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

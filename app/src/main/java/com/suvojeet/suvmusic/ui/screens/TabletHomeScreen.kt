package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel
import com.suvojeet.suvmusic.util.ImageUtils
import java.util.Calendar

/**
 * Tablet-optimized Home Screen with a two-column layout.
 * Left column: Hero spotlight + quick access grid.
 * Right column: Scrollable content sections with wider cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletHomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onRecentsClick: () -> Unit = {},
    onExploreClick: (String, String) -> Unit = { _, _ -> },
    onStartRadio: () -> Unit = {},
    currentSong: Song? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.isLoading && uiState.homeSections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Hero item from recommendations
    val heroItem = remember(uiState.recommendations) {
        uiState.recommendations.firstOrNull()
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ========================
            // LEFT COLUMN — Spotlight
            // ========================
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 12.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row {
                        IconButton(onClick = onRecentsClick) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Recent",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Hero Spotlight Card
                if (heroItem != null) {
                    TabletHeroCard(
                        song = heroItem,
                        onPlayClick = {
                            onSongClick(uiState.recommendations, 0)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Access Grid (2 columns)
                if (uiState.recommendations.size > 1) {
                    Text(
                        text = "Quick picks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            uiState.recommendations.drop(1).take(8),
                            key = { it.id }
                        ) { song ->
                            TabletQuickAccessCard(
                                song = song,
                                onClick = {
                                    val idx = uiState.recommendations.indexOf(song)
                                    onSongClick(uiState.recommendations, idx.coerceAtLeast(0))
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onStartRadio,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Radio", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // ========================
            // RIGHT COLUMN — Sections
            // ========================
            LazyColumn(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = 24.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {
                // Personalized sections
                val allSections = buildList {
                    addAll(uiState.contextSections)
                    addAll(uiState.personalizedSections)
                    addAll(uiState.genreSections)
                    addAll(uiState.filteredSections)
                    addAll(uiState.moreSections)
                }.distinctBy { it.title }

                items(allSections, key = { it.title }) { section ->
                    if (section.items.isNotEmpty()) {
                        TabletContentSection(
                            section = section,
                            onSongClick = onSongClick,
                            onPlaylistClick = onPlaylistClick,
                            onAlbumClick = onAlbumClick
                        )
                    }
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Sub-composables
// =============================================================================

@Composable
private fun TabletHeroCard(
    song: Song,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl) ?: song.thumbnailUrl

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable(onClick = onPlayClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "FEATURED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Play button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TabletQuickAccessCard(
    song: Song,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quickAccessScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(animatedScale)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f)
        )
    }
}

@Composable
private fun TabletContentSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(section.items, key = { "${section.title}_${it.hashCode()}" }) { item ->
                when (item) {
                    is HomeItem.SongItem -> {
                        TabletSectionCard(
                            title = item.song.title,
                            subtitle = item.song.artist,
                            imageUrl = item.song.thumbnailUrl,
                            onClick = {
                                val songs = section.items
                                    .filterIsInstance<HomeItem.SongItem>()
                                    .map { it.song }
                                val idx = songs.indexOf(item.song)
                                onSongClick(songs, idx.coerceAtLeast(0))
                            }
                        )
                    }
                    is HomeItem.PlaylistItem -> {
                        TabletSectionCard(
                            title = item.playlist.name,
                            subtitle = "Playlist",
                            imageUrl = item.playlist.thumbnailUrl,
                            onClick = { onPlaylistClick(item.playlist) }
                        )
                    }
                    is HomeItem.AlbumItem -> {
                        TabletSectionCard(
                            title = item.album.title,
                            subtitle = "Album • ${item.album.artist}",
                            imageUrl = item.album.thumbnailUrl,
                            onClick = { onAlbumClick(item.album) }
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun TabletSectionCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.size(160.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

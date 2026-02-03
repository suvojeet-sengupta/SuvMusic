package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import com.suvojeet.suvmusic.ui.screens.ImportPlaylistScreen
import com.suvojeet.suvmusic.ui.components.MusicCard
import com.suvojeet.suvmusic.ui.components.MediaMenuBottomSheet
import com.suvojeet.suvmusic.ui.viewmodel.LibraryViewModel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Album
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext

/**
 * Library screen with Material 3 design and player-inspired aesthetics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Liked", "Playlists", "Offline")
    
    // Create playlist dialog state
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var isCreatingPlaylist by remember { mutableStateOf(false) }
    
    // Import Spotify dialog state
    var showImportSpotifyDialog by remember { mutableStateOf(false) }

    // Playlist Menu State
    var showPlaylistMenu by remember { mutableStateOf(false) }
    var selectedPlaylist: PlaylistDisplayItem? by remember { mutableStateOf(null) }

    // Player-inspired Background Colors
    val deepPurple = Color(0xFF1E1033) // Deep background
    val vibrantPurple = Color(0xFF6200EA) // Primary Accent
    val secondaryAccent = Color(0xFF3700B3) // Secondary Accent
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient Background Glow (Player Style)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(300.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .blur(80.dp)
                .background(vibrantPurple.copy(alpha = 0.15f), CircleShape)
        )

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Material 3 Style Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp), // More rounded
                            color = containerColor,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable { selectedTab = index }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Content based on selected tab
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        2 -> OfflineTab(
                            localSongs = uiState.localSongs,
                            downloadedSongs = uiState.downloadedSongs,
                            onSongClick = onSongClick,
                            onDownloadsClick = onDownloadsClick
                        )
                    }
                }
            }
        }
        
        // Create Playlist Dialog
        CreatePlaylistDialog(
            isVisible = showCreatePlaylistDialog,
            isCreating = isCreatingPlaylist,
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { title, description, isPrivate ->
                isCreatingPlaylist = true
                viewModel.createPlaylist(title, description, isPrivate) {
                    isCreatingPlaylist = false
                    showCreatePlaylistDialog = false
                }
            },
            isLoggedIn = uiState.isLoggedIn
        )

        // Import Spotify Dialog
        ImportPlaylistScreen(
            isVisible = showImportSpotifyDialog,
            importState = uiState.importState,
            onDismiss = { 
                showImportSpotifyDialog = false
                viewModel.resetImportState()
            },
            onImport = { url ->
                viewModel.importSpotifyPlaylist(url)
            },
            onCancel = {
                viewModel.cancelImport()
            },
            onReset = {
                viewModel.resetImportState()
            }
        )

        // Playlist Menu Bottom Sheet
        if (showPlaylistMenu && selectedPlaylist != null) {
            val playlist = selectedPlaylist!!
            MediaMenuBottomSheet(
                isVisible = showPlaylistMenu,
                onDismiss = { showPlaylistMenu = false },
                title = playlist.name,
                subtitle = "${playlist.songCount} songs",
                thumbnailUrl = playlist.thumbnailUrl,
                isUserPlaylist = playlist.id.startsWith("PL") || playlist.id.startsWith("VL"), // Simple check
                onShuffle = { viewModel.shufflePlay(playlist.id) },
                onStartRadio = { viewModel.shufflePlay(playlist.id) }, // Same as shuffle for now
                onPlayNext = { viewModel.playNext(playlist.id) },
                onAddToQueue = { viewModel.addToQueue(playlist.id) },
                onAddToPlaylist = { /* Already handled elsewhere? */ },
                onDownload = { viewModel.downloadPlaylist(playlist) },
                onShare = { 
                    val shareText = "Check out this playlist: ${playlist.name}\n${playlist.url}"
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                },
                onRename = { /* Handle rename dialog if needed */ },
                onDelete = { viewModel.deletePlaylist(playlist.id) }
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistDisplayItem>,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onMoreClick: (PlaylistDisplayItem) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onImportSpotifyClick: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Cards Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CreatePlaylistCard(
                    modifier = Modifier.weight(1f),
                    onClick = onCreatePlaylistClick
                )
                ImportSpotifyCard(
                    modifier = Modifier.weight(1f),
                    onClick = onImportSpotifyClick
                )
            }
        }
        
        // Section Header
        if (playlists.isNotEmpty()) {
            item {
                Text(
                    text = "Your Playlists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
                )
            }
        }
        
        // Playlists as List Items
        items(playlists) { playlist ->
            PlaylistListItem(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
                onMoreClick = { onMoreClick(playlist) }
            )
        }
        
        if (playlists.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.MusicNote,
                    title = "No playlists yet",
                    message = "Create one to get started!"
                )
            }
        }
    }
}

@Composable
private fun CreatePlaylistCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp), // Material 3 rounding
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = "New Playlist",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Create custom mix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImportSpotifyCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Icon Container (Spotify Greenish tint)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1DB954).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote, // Generic music icon
                    contentDescription = null,
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = "Import Spotify",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sync your library",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaylistListItem(
    playlist: PlaylistDisplayItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(56.dp)
        ) {
            if (playlist.thumbnailUrl != null) {
                coil.compose.AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "${playlist.songCount} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OfflineTab(
    localSongs: List<Song>,
    downloadedSongs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onDownloadsClick: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (localSongs.isEmpty() && downloadedSongs.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CloudDownload,
                    title = "No offline music",
                    message = "Download songs or add local files"
                )
            }
        }
        
        // Downloaded Songs Section Card
        if (downloadedSongs.isNotEmpty()) {
            item {
                DownloadedSongsCard(
                    songCount = downloadedSongs.size,
                    totalDuration = downloadedSongs.sumOf { it.duration },
                    onClick = onDownloadsClick
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        if (localSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Device Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            itemsIndexed(localSongs) { index, song ->
                MusicCard(
                    song = song,
                    onClick = { onSongClick(localSongs, index) }
                )
            }
        }
    }
}

@Composable
private fun DownloadedSongsCard(
    songCount: Int,
    totalDuration: Long,
    onClick: () -> Unit
) {
    val durationText = if (totalDuration > 0) {
        val minutes = (totalDuration / 1000 / 60).toInt()
        val seconds = (totalDuration / 1000 % 60).toInt()
        if (minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            "$hours hr $remainingMinutes min"
        } else {
            "$minutes min"
        }
    } else {
        ""
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Downloaded Songs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "$songCount songs • $durationText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun LikedTab(
    songs: List<Song>,
    isSyncing: Boolean,
    onSongClick: (List<Song>, Int) -> Unit,
    onSync: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Liked Songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(onClick = onSync, enabled = !isSyncing) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        if (songs.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.MusicNote,
                    title = "No liked songs",
                    message = "Songs you like on YouTube Music will appear here"
                )
            }
        }
        
        itemsIndexed(songs) { index, song ->
            MusicCard(
                song = song,
                onClick = { onSongClick(songs, index) }
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

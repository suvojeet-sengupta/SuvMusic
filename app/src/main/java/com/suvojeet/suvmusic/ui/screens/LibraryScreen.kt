package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import com.suvojeet.suvmusic.ui.components.MediaMenuBottomSheet
import com.suvojeet.suvmusic.ui.components.MusicCard
import com.suvojeet.suvmusic.ui.screens.ImportPlaylistScreen
import com.suvojeet.suvmusic.ui.viewmodel.LibraryFilter
import com.suvojeet.suvmusic.ui.viewmodel.LibrarySortOption
import com.suvojeet.suvmusic.ui.viewmodel.LibraryViewMode
import com.suvojeet.suvmusic.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onHistoryClick: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Dialog States
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var isCreatingPlaylist by remember { mutableStateOf(false) }
    var showImportSpotifyDialog by remember { mutableStateOf(false) } // Keep logic but might hide UI entry for now
    
    // Playlist Menu State
    var showPlaylistMenu by remember { mutableStateOf(false) }
    var selectedPlaylist: PlaylistDisplayItem? by remember { mutableStateOf(null) }

    // Add Menu State
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                 Icon(Icons.Default.Add, "Add")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                // 4. Content Area
                if (uiState.selectedFilter == LibraryFilter.PLAYLISTS) {
                    if (uiState.viewMode == LibraryViewMode.GRID) {
                        PlaylistsGrid(
                            uiState = uiState, // Pass full state for smart playlists
                            onPlaylistClick = onPlaylistClick,
                            onSmartPlaylistClick = { type ->
                                when (type) {
                                    SmartPlaylistType.LIKED -> {
                                        // Trigger sync if empty (first time)
                                        if (uiState.likedSongs.isEmpty()) {
                                            viewModel.syncLikedSongs()
                                        }
                                        onPlaylistClick(PlaylistDisplayItem(id = "LM", name = "Liked Songs", url = "", uploaderName = "", thumbnailUrl = null, songCount = 0))
                                    }
                                    SmartPlaylistType.DOWNLOADED -> onDownloadsClick()
                                    SmartPlaylistType.TOP_50 -> {
                                        onPlaylistClick(PlaylistDisplayItem(id = "TOP_50", name = "My Top 50", url = "", uploaderName = "", thumbnailUrl = null, songCount = 0))
                                    }
                                    SmartPlaylistType.CACHED -> {
                                        onPlaylistClick(PlaylistDisplayItem(id = "CACHED_ALL", name = "Cached Songs", url = "", uploaderName = "", thumbnailUrl = null, songCount = 0))
                                    }
                                }
                            },
                            onMoreClick = { playlist ->
                                selectedPlaylist = playlist
                                showPlaylistMenu = true
                            },
                            // Pass Headers
                            topBar = {
                                LibraryTopBar(
                                    onHistoryClick = onHistoryClick,
                                    onSyncClick = { viewModel.refresh() },
                                    isSyncing = uiState.isRefreshing
                                )
                            },
                            filterChips = {
                                LibraryFilterChips(
                                    selectedFilter = uiState.selectedFilter,
                                    onFilterSelected = { viewModel.setFilter(it) }
                                )
                            },
                            controlBar = {
                                LibraryControlBar(
                                    sortOption = uiState.sortOption,
                                    viewMode = uiState.viewMode,
                                    onSortClick = { /* Toggle Sort */ },
                                    onViewModeClick = { viewModel.setViewMode(LibraryViewMode.LIST) },
                                    itemCount = getCountForFilter(uiState)
                                )
                            }
                        )
                    } else {
                        PlaylistsList(
                            playlists = uiState.playlists,
                            onPlaylistClick = onPlaylistClick,
                            onMoreClick = { playlist ->
                                selectedPlaylist = playlist
                                showPlaylistMenu = true
                            },
                            topBar = {
                                LibraryTopBar(
                                    onHistoryClick = onHistoryClick,
                                    onSyncClick = { viewModel.refresh() },
                                    isSyncing = uiState.isRefreshing
                                )
                            },
                            filterChips = {
                                LibraryFilterChips(
                                    selectedFilter = uiState.selectedFilter,
                                    onFilterSelected = { viewModel.setFilter(it) }
                                )
                            },
                            controlBar = {
                                LibraryControlBar(
                                    sortOption = uiState.sortOption,
                                    viewMode = uiState.viewMode,
                                    onSortClick = { /* Toggle Sort */ },
                                    onViewModeClick = { viewModel.setViewMode(LibraryViewMode.GRID) },
                                    itemCount = getCountForFilter(uiState)
                                )
                            }
                        )
                    }
                } else {
                    // Other filters
                     OtherContentList(
                        filter = uiState.selectedFilter,
                        uiState = uiState,
                        onSongClick = onSongClick,
                         topBar = {
                             LibraryTopBar(
                                 onHistoryClick = onHistoryClick,
                                 onSyncClick = { viewModel.refresh() },
                                 isSyncing = uiState.isRefreshing
                             )
                         },
                         filterChips = {
                             LibraryFilterChips(
                                 selectedFilter = uiState.selectedFilter,
                                 onFilterSelected = { viewModel.setFilter(it) }
                             )
                         }
                    )
                }
            }
        }
    }
    
    // Components (Dialogs, Bottom Sheets)
     CreatePlaylistDialog(
        isVisible = showCreatePlaylistDialog,
        isCreating = isCreatingPlaylist,
        onDismiss = { showCreatePlaylistDialog = false },
        onCreate = { title, description, isPrivate, syncWithYt ->
            isCreatingPlaylist = true
            viewModel.createPlaylist(title, description, isPrivate, syncWithYt) {
                isCreatingPlaylist = false
                showCreatePlaylistDialog = false
            }
        },
        isLoggedIn = uiState.isLoggedIn
    )
    
     // Import Spotify Dialog
    if (showImportSpotifyDialog) {
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
    }

    // Playlist Menu Bottom Sheet
    if (showPlaylistMenu && selectedPlaylist != null) {
        val playlist = selectedPlaylist!!
        MediaMenuBottomSheet(
            isVisible = showPlaylistMenu,
            onDismiss = { showPlaylistMenu = false },
            title = playlist.name,
            subtitle = "${playlist.songCount} songs",
            thumbnailUrl = playlist.thumbnailUrl,
            isUserPlaylist = playlist.id.startsWith("PL") || playlist.id.startsWith("VL"),
            onShuffle = { viewModel.shufflePlay(playlist.id) },
            onStartRadio = { viewModel.shufflePlay(playlist.id) },
            onPlayNext = { viewModel.playNext(playlist.id) },
            onAddToQueue = { viewModel.addToQueue(playlist.id) },
            onAddToPlaylist = { },
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
            onRename = { },
            onDelete = { viewModel.deletePlaylist(playlist.id) }
        )
    }

    // Add Menu Bottom Sheet
    if (showAddMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAddMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Add to Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                // Create Playlist Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddMenu = false
                            showCreatePlaylistDialog = true
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Create playlist", style = MaterialTheme.typography.titleMedium)
                }

                // Import Spotify Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddMenu = false
                            showImportSpotifyDialog = true
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1DB954)), // Spotify Green
                        contentAlignment = Alignment.Center
                    ) {
                         // Use generic icon or download generic one, for now Add is fine or specific import
                         Icon(Icons.Outlined.FileDownload, null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Import from Spotify", style = MaterialTheme.typography.titleMedium)
                        Text("Transfer your playlists", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// --- Sub-Composables ---

@Composable
fun LibraryTopBar(
    onHistoryClick: () -> Unit,
    onSyncClick: () -> Unit,
    isSyncing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp, top = 16.dp), // Added top padding as it's now in scroll
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), // Smaller than displaySmall
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSyncClick) {
                if (isSyncing) {
                   androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Cached, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterChips(
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(LibraryFilter.values()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.title) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                border = null
            )
        }
    }
}

@Composable
fun LibraryControlBar(
    sortOption: LibrarySortOption,
    viewMode: LibraryViewMode,
    onSortClick: () -> Unit,
    onViewModeClick: () -> Unit,
    itemCount: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onSortClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Date added", // Dynamic based on sortOption
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Sort",
                modifier = Modifier.size(16.dp).padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
             Text(
                text = itemCount, 
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            IconButton(
                onClick = onViewModeClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (viewMode == LibraryViewMode.GRID) Icons.Default.List else Icons.Default.GridView,
                    contentDescription = "Toggle View",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

enum class SmartPlaylistType {
    LIKED, DOWNLOADED, TOP_50, CACHED
}

@Composable
fun PlaylistsGrid(
    uiState: com.suvojeet.suvmusic.ui.viewmodel.LibraryUiState,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit,
    onMoreClick: (PlaylistDisplayItem) -> Unit,
    topBar: @Composable () -> Unit,
    filterChips: @Composable () -> Unit,
    controlBar: @Composable () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers as Full Span Items
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { topBar() }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { 
             Box(modifier = Modifier.padding(bottom = 16.dp)) { filterChips() }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { 
             Box(modifier = Modifier.padding(bottom = 8.dp)) { controlBar() }
        }

        // 1. Smart Playlists (Fixed)
        item {
            SmartPlaylistCard(
                title = "Liked",
                icon = Icons.Default.FavoriteBorder,
                count = "${uiState.likedSongs.size} songs",
                onClick = { onSmartPlaylistClick(SmartPlaylistType.LIKED) }
            )
        }
        item {
             SmartPlaylistCard(
                title = "Downloaded",
                icon = Icons.Outlined.FileDownload,
                count = "${uiState.downloadedSongs.size} songs",
                onClick = { onSmartPlaylistClick(SmartPlaylistType.DOWNLOADED) }
            )
        }
        item {
             SmartPlaylistCard(
                title = "My top 50",
                icon = Icons.Default.TrendingUp,
                count = "${uiState.top50SongCount} songs",
                onClick = { onSmartPlaylistClick(SmartPlaylistType.TOP_50) }
            )
        }
        item {
             SmartPlaylistCard(
                title = "Cached",
                icon = Icons.Default.Cached,
                count = "${uiState.cachedSongCount} songs",
                onClick = { onSmartPlaylistClick(SmartPlaylistType.CACHED) }
            )
        }
        
        // 2. User Playlists
        items(uiState.playlists) { playlist ->
            GridPlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) }
            )
        }
    }
}


@Composable
fun SmartPlaylistCard(
    title: String,
    icon: ImageVector,
    count: String, // e.g. "Auto playlist" or "343 songs"
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) // Premium tint
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Text at bottom start
             Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                 Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (count.isNotEmpty()) {
                     AnimatedContent(
                         targetState = count,
                         transitionSpec = {
                             slideInVertically { height -> height } + fadeIn() togetherWith
                             slideOutVertically { height -> -height } + fadeOut()
                         },
                         label = "CountAnimation"
                     ) { targetCount ->
                         Text(
                            text = targetCount,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                     }
                }
            }
        }
    }
}

@Composable
fun GridPlaylistCard(
    playlist: PlaylistDisplayItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.aspectRatio(1f)
        ) {
            if (playlist.thumbnailUrl != null) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                 Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        contentDescription = null
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Playlist • ${playlist.uploaderName}", // Placeholder logic
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun PlaylistsList(
    playlists: List<PlaylistDisplayItem>,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onMoreClick: (PlaylistDisplayItem) -> Unit,
    topBar: @Composable () -> Unit,
    filterChips: @Composable () -> Unit,
    controlBar: @Composable () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
         item { topBar() }
         item { Box(modifier = Modifier.padding(bottom = 16.dp)) { filterChips() } }
         item { Box(modifier = Modifier.padding(bottom = 8.dp)) { controlBar() } }
         
         items(playlists) { playlist ->
            // Re-use existing list item style or create simplified one
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist) }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(56.dp)
                ) {
                    if (playlist.thumbnailUrl != null) {
                        AsyncImage(model = playlist.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null) }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("Playlist • ${playlist.songCount} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onMoreClick(playlist) }) {
                    Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun OtherContentList(
    filter: LibraryFilter,
    uiState: com.suvojeet.suvmusic.ui.viewmodel.LibraryUiState,
    onSongClick: (List<Song>, Int) -> Unit,
    topBar: @Composable () -> Unit,
    filterChips: @Composable () -> Unit
) {
    // Placeholder for other tabs
    // If Filter is SONGS -> Show all songs (mixed? or just liked? user needs to clarify, 
    // but typically "Songs" in library means Liked songs or All local)
    // For now we'll show Liked + Local.
    
    val songs = if (filter == LibraryFilter.SONGS) uiState.likedSongs + uiState.localSongs else emptyList()
    
    if (filter == LibraryFilter.SONGS) {
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
             item { topBar() }
             item { Box(modifier = Modifier.padding(bottom = 16.dp)) { filterChips() } }

             items(songs.size) { index ->
                val song = songs[index]
                 MusicCard(song = song, onClick = { onSongClick(songs, index) })
            }
        }
    } else {
         Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            Box(modifier = Modifier.padding(bottom = 16.dp)) { filterChips() }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Coming Soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
         }
    }
}

// Logic Helper
fun getCountForFilter(uiState: com.suvojeet.suvmusic.ui.viewmodel.LibraryUiState): String {
    return when(uiState.selectedFilter) {
        LibraryFilter.PLAYLISTS -> "${uiState.playlists.size} playlists"
        LibraryFilter.SONGS -> "${uiState.likedSongs.size + uiState.localSongs.size} songs"
        LibraryFilter.ALBUMS -> "${uiState.libraryAlbums.size} albums"
        LibraryFilter.ARTISTS -> "${uiState.libraryArtists.size} artists"
    }
}

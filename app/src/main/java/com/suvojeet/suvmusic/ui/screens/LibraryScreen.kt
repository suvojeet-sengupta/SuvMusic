package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import coil3.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import com.suvojeet.suvmusic.ui.components.MediaMenuBottomSheet
import com.suvojeet.suvmusic.ui.components.RenamePlaylistDialog
import com.suvojeet.suvmusic.ui.components.DeletePlaylistDialog
import com.suvojeet.suvmusic.ui.components.ExportPlaylistDialog
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
    var showExportDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Add Menu State
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            com.suvojeet.suvmusic.ui.components.primitives.ExpressiveFab(
                onClick = { showAddMenu = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 150.dp)
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
                                        if (uiState.likedSongsCount == 0) {
                                            viewModel.syncLikedSongs()
                                        }
                                        viewModel.loadLikedSongs()
                                        onPlaylistClick(PlaylistDisplayItem(id = "LM", name = "Liked Songs", url = "", uploaderName = "", thumbnailUrl = null, songCount = 0))
                                    }
                                    SmartPlaylistType.DOWNLOADED -> onDownloadsClick()
                                    SmartPlaylistType.DEVICE_SONGS -> {
                                        onPlaylistClick(PlaylistDisplayItem(id = "DEVICE_SONGS", name = "Device files", url = "", uploaderName = "", thumbnailUrl = null, songCount = 0))
                                    }
                                    SmartPlaylistType.OFFLINE_SHUFFLE -> {
                                        viewModel.playOfflineShuffle()
                                    }
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
                                    onRescanClick = { viewModel.loadData(forceRefresh = true) },
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
                            uiState = uiState,
                            onPlaylistClick = onPlaylistClick,
                            onSmartPlaylistClick = { type ->
                                when (type) {
                                    SmartPlaylistType.LIKED -> {
                                        if (uiState.likedSongsCount == 0) {
                                            viewModel.syncLikedSongs()
                                        }
                                        viewModel.loadLikedSongs()
                                        onPlaylistClick(PlaylistDisplayItem(id = "LM", name = "Liked Songs", url = "", uploaderName = "", thumbnailUrl = null, songCount = 0))
                                    }
                                    SmartPlaylistType.DOWNLOADED -> onDownloadsClick()
                                    SmartPlaylistType.DEVICE_SONGS -> {
                                        onPlaylistClick(PlaylistDisplayItem(id = "DEVICE_SONGS", name = "Device files", url = "", uploaderName = "", thumbnailUrl = null, songCount = 0))
                                    }
                                    SmartPlaylistType.OFFLINE_SHUFFLE -> {
                                        viewModel.playOfflineShuffle()
                                    }
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
                            topBar = {
                                LibraryTopBar(
                                    onHistoryClick = onHistoryClick,
                                    onSyncClick = { viewModel.refresh() },
                                    onRescanClick = { viewModel.loadData(forceRefresh = true) },
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
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick,
                         topBar = {
                             LibraryTopBar(
                                 onHistoryClick = onHistoryClick,
                                 onSyncClick = { viewModel.refresh() },
                                 onRescanClick = { viewModel.loadData(forceRefresh = true) },
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
                viewModel.importPlaylist(url)
            },
            onImportM3U = { uri ->
                viewModel.importM3U(uri)
            },
            onImportSUV = { uri ->
                viewModel.importSUV(uri)
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
            onShuffle = { viewModel.shufflePlay(playlist.id) },
            onStartRadio = { viewModel.shufflePlay(playlist.id) }, // You might want a real radio here
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
            onExport = { showExportDialog = true },
            onRename = { showRenameDialog = true },
            onDelete = { showDeleteDialog = true }
        )
    }

    // Rename Playlist Dialog
    if (showRenameDialog && selectedPlaylist != null) {
        RenamePlaylistDialog(
            isVisible = showRenameDialog,
            currentName = selectedPlaylist!!.name,
            isRenaming = false, // UI handles it
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.renamePlaylist(selectedPlaylist!!.id, newName)
                showRenameDialog = false
            }
        )
    }

    // Delete Playlist Dialog
    if (showDeleteDialog && selectedPlaylist != null) {
        DeletePlaylistDialog(
            isVisible = showDeleteDialog,
            playlistTitle = selectedPlaylist!!.name,
            isDeleting = false,
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                viewModel.deletePlaylist(selectedPlaylist!!.id)
                showDeleteDialog = false
            }
        )
    }

    // Export Playlist Dialog
    if (showExportDialog && selectedPlaylist != null) {
        ExportPlaylistDialog(
            isVisible = showExportDialog,
            onDismiss = { showExportDialog = false },
            onExportM3U = { viewModel.exportPlaylistToM3U(context, selectedPlaylist!!) },
            onExportSUV = { viewModel.exportPlaylistToSUV(context, selectedPlaylist!!) }
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
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
                            .clip(com.suvojeet.suvmusic.ui.theme.SquircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Create playlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                // Import Playlist Item
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
                            .clip(com.suvojeet.suvmusic.ui.theme.SquircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                         Icon(Icons.Outlined.FileDownload, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Import Playlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Spotify, YouTube or .m3u", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onRescanClick: () -> Unit = {},
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
            IconButton(onClick = onRescanClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Rescan Media", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSyncClick) {
                AnimatedContent(
                    targetState = isSyncing,
                    transitionSpec = {
                        (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                        (scaleOut() + fadeOut())
                    },
                    label = "syncIndicator"
                ) { syncing ->
                    if (syncing) {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Cached,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    imageVector = if (viewMode == LibraryViewMode.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                    contentDescription = "Toggle View",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

enum class SmartPlaylistType {
    LIKED, DOWNLOADED, TOP_50, CACHED, DEVICE_SONGS, OFFLINE_SHUFFLE
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
    val windowSize = com.suvojeet.suvmusic.ui.utils.rememberWindowSize()
    val gridColumns = when (windowSize) {
        com.suvojeet.suvmusic.ui.utils.WindowSize.Expanded -> 4
        com.suvojeet.suvmusic.ui.utils.WindowSize.Medium -> 3
        com.suvojeet.suvmusic.ui.utils.WindowSize.Compact -> 2
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 160.dp),
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
                count = "${uiState.likedSongsCount} songs",
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
                title = "Device files",
                icon = Icons.Default.MusicNote,
                count = "${uiState.localSongs.size} songs",
                onClick = { onSmartPlaylistClick(SmartPlaylistType.DEVICE_SONGS) }
            )
        }
        item {
             SmartPlaylistCard(
                title = "Offline Music",
                icon = Icons.Default.Shuffle,
                count = "Shuffle all",
                onClick = { onSmartPlaylistClick(SmartPlaylistType.OFFLINE_SHUFFLE) }
            )
        }
        item {
             SmartPlaylistCard(
                title = "My top 50",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
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
        items(uiState.playlists, key = { it.id }) { playlist ->
            GridPlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) }
            )
        }
    }
}

@Composable
fun SmartPlaylistListItem(
    title: String,
    icon: ImageVector,
    count: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(count, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    uiState: com.suvojeet.suvmusic.ui.viewmodel.LibraryUiState,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit,
    onMoreClick: (PlaylistDisplayItem) -> Unit,
    topBar: @Composable () -> Unit,
    filterChips: @Composable () -> Unit,
    controlBar: @Composable () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
         item { topBar() }
         item { Box(modifier = Modifier.padding(bottom = 16.dp)) { filterChips() } }
         item { Box(modifier = Modifier.padding(bottom = 8.dp)) { controlBar() } }
         
         // Smart Playlists
         item {
             SmartPlaylistListItem(
                 title = "Liked",
                 icon = Icons.Default.FavoriteBorder,
                 count = "${uiState.likedSongsCount} songs",
                 onClick = { onSmartPlaylistClick(SmartPlaylistType.LIKED) }
             )
         }
         item {
             SmartPlaylistListItem(
                 title = "Downloaded",
                 icon = Icons.Outlined.FileDownload,
                 count = "${uiState.downloadedSongs.size} songs",
                 onClick = { onSmartPlaylistClick(SmartPlaylistType.DOWNLOADED) }
             )
         }
         item {
             SmartPlaylistListItem(
                 title = "Device files",
                 icon = Icons.Default.MusicNote,
                 count = "${uiState.localSongs.size} songs",
                 onClick = { onSmartPlaylistClick(SmartPlaylistType.DEVICE_SONGS) }
             )
         }
         item {
             SmartPlaylistListItem(
                 title = "Offline Music",
                 icon = Icons.Default.Shuffle,
                 count = "Shuffle all",
                 onClick = { onSmartPlaylistClick(SmartPlaylistType.OFFLINE_SHUFFLE) }
             )
         }
         item {
             SmartPlaylistListItem(
                 title = "My top 50",
                 icon = Icons.AutoMirrored.Filled.TrendingUp,
                 count = "${uiState.top50SongCount} songs",
                 onClick = { onSmartPlaylistClick(SmartPlaylistType.TOP_50) }
             )
         }
         item {
             SmartPlaylistListItem(
                 title = "Cached",
                 icon = Icons.Default.Cached,
                 count = "${uiState.cachedSongCount} songs",
                 onClick = { onSmartPlaylistClick(SmartPlaylistType.CACHED) }
             )
         }

         items(uiState.playlists, key = { it.id }) { playlist ->
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
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    topBar: @Composable () -> Unit,
    filterChips: @Composable () -> Unit
) {
    val songs = if (filter == LibraryFilter.SONGS) uiState.likedSongs + uiState.localSongs else emptyList()
    val albums = if (filter == LibraryFilter.ALBUMS) (uiState.libraryAlbums + uiState.localAlbums).distinctBy { it.id } else emptyList()
    val artists = if (filter == LibraryFilter.ARTISTS) (uiState.libraryArtists + uiState.localArtists).distinctBy { it.id } else emptyList()
    
    LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
        item { topBar() }
        item { Box(modifier = Modifier.padding(bottom = 16.dp)) { filterChips() } }

        when (filter) {
            LibraryFilter.SONGS -> {
                items(songs.size) { index ->
                    val song = songs[index]
                    MusicCard(song = song, onClick = { onSongClick(songs, index) })
                }
            }
            LibraryFilter.ALBUMS -> {
                items(albums) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(album) }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(56.dp)
                        ) {
                            if (album.thumbnailUrl != null) {
                                AsyncImage(model = album.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop)
                            } else {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null) }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(album.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(album.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            LibraryFilter.ARTISTS -> {
                items(artists) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArtistClick(artist.id) }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(56.dp)
                        ) {
                            if (artist.thumbnailUrl != null) {
                                AsyncImage(model = artist.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop)
                            } else {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null) }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(artist.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
            LibraryFilter.FOLDERS -> {
                items(uiState.localFolders.keys.toList()) { folderPath ->
                    val folderName = folderPath.substringAfterLast("/")
                    val songs = uiState.localFolders[folderPath] ?: emptyList()
                    val songCount = songs.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (songs.isNotEmpty()) {
                                    onSongClick(songs, 0)
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(folderName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text("$songCount songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

// Logic Helper
fun getCountForFilter(uiState: com.suvojeet.suvmusic.ui.viewmodel.LibraryUiState): String {
    return when(uiState.selectedFilter) {
        LibraryFilter.PLAYLISTS -> "${uiState.playlists.size} playlists"
        LibraryFilter.SONGS -> "${uiState.likedSongsCount + uiState.localSongs.size} songs"
        LibraryFilter.ALBUMS -> "${uiState.libraryAlbums.size + uiState.localAlbums.size} albums"
        LibraryFilter.ARTISTS -> "${uiState.libraryArtists.size + uiState.localArtists.size} artists"
        LibraryFilter.FOLDERS -> "${uiState.localFolders.size} folders"
    }
}

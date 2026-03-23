package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SortOrder
import com.suvojeet.suvmusic.core.model.SortType
import com.suvojeet.suvmusic.ui.components.BounceButton
import com.suvojeet.suvmusic.ui.components.PremiumLoadingScreen
import com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import com.suvojeet.suvmusic.ui.theme.PillShape
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistViewModel
import com.suvojeet.suvmusic.util.dpadFocusable

@Composable
fun PlaylistScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit = {},
    onShufflePlay: (List<Song>) -> Unit = {},
    currentSong: Song? = null,
    viewModel: PlaylistViewModel = hiltViewModel(),
    playlistMgmtViewModel: com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val batchProgress by viewModel.batchProgress.collectAsState()
    val context = LocalContext.current
    val playlist = uiState.playlist

    // Check if we are in dark theme based on background luminance (consistent with PlayerScreen)
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Define colors based on theme
    val backgroundColor = if (isDarkTheme) Color(0xFF0D0D0D) else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)

    // Dynamic colors from playlist thumbnail
    val dominantColors = rememberDominantColors(playlist?.thumbnailUrl)
    
    // Track scroll position for collapsing header
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100 }
    }

    // Scroll Direction Tracking for Hiding TopBar
    var isScrollingDown by remember { mutableStateOf(false) }
    var previousIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var previousScrollOffset by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { 
            Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) 
        }.collect { (currentIndex, currentOffset) ->
            if (currentIndex > previousIndex) {
                isScrollingDown = true
            } else if (currentIndex < previousIndex) {
                isScrollingDown = false
            } else {
                if (currentOffset > previousScrollOffset + 10) {
                    isScrollingDown = true
                } else if (currentOffset < previousScrollOffset - 10) {
                    isScrollingDown = false
                }
            }
            previousIndex = currentIndex
            previousScrollOffset = currentOffset
        }
    }
    
    val isTopBarVisible = !isScrolled || !isScrollingDown

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMediaMenu by remember { mutableStateOf(false) }
    
    // Song Menu State
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }

    // Selection mode cleanup when leaving screen
    val currentViewModel by rememberUpdatedState(viewModel)
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            currentViewModel.clearSelection()
        }
    }

    androidx.activity.compose.BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    // Handle messages from ViewModel
    androidx.compose.runtime.LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    // Handle delete success
    androidx.compose.runtime.LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onBackClick()
        }
    }

    val sharePlaylist: (Playlist) -> Unit = { playlistToShare ->
        val shareText = "Check out this playlist: ${playlistToShare.title} by ${playlistToShare.author}\n\nhttps://music.youtube.com/playlist?list=${playlistToShare.id}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Playlist")
        context.startActivity(shareIntent)
    }

    val shareSong: (Song) -> Unit = { song ->
        val shareText = "Check out this song: ${song.title} by ${song.artist}\n\nhttps://music.youtube.com/watch?v=${song.id}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Song")
        context.startActivity(shareIntent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // ... (existing blurred background and LazyColumn)
        
        // Blurred background image - Full screen like Apple Music
        if (playlist?.thumbnailUrl != null) {
            AsyncImage(
                model = playlist.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp),
                contentScale = ContentScale.Crop,
                alpha = if (isDarkTheme) 0.4f else 0.3f
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                // Dark theme
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF0D0D0D).copy(alpha = 0.8f),
                                    Color(0xFF0D0D0D)
                                )
                            } else {
                                // Light theme
                                listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.8f),
                                    Color.White
                                )
                            }
                        )
                    )
            )
        }

        // Content
        if (uiState.isLoading) {
            PremiumLoadingScreen(
                thumbnailUrl = playlist?.thumbnailUrl,
                onBackClick = onBackClick
            )
        } else if (playlist != null) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshPlaylist() },
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 60.dp, bottom = 100.dp), // Top padding for TopBar
                modifier = Modifier.fillMaxSize()
            ) {
                // Playlist Header (scrolls with content)
                item {
                    PlaylistHeader(
                        playlist = playlist,
                        batchProgress = batchProgress,
                        isSaved = uiState.isSaved,
                        sortType = uiState.sortType,
                        sortOrder = uiState.sortOrder,
                        onSortChange = viewModel::setSort,
                        onToggleSortOrder = viewModel::toggleSortOrder,
                        onPlayAll = { onPlayAll(playlist.songs) },
                        onShufflePlay = { onShufflePlay(playlist.songs) },
                        onToggleSave = { 
                            viewModel.toggleSaveToLibrary()
                            val message = if (uiState.isSaved) "Removed from Library" else "Saved to Library"
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onDownload = { viewModel.downloadPlaylist(playlist) },
                        onShare = { sharePlaylist(playlist) },
                        onMoreClick = { showMediaMenu = true },
                        contentColor = contentColor,
                        secondaryContentColor = secondaryContentColor,
                        isDarkTheme = isDarkTheme
                    )
                }
                
                // Empty State or Song List
                if (playlist.songs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(SquircleShape)
                                    .background(
                                        if (isDarkTheme) Color.White.copy(alpha = 0.1f)
                                        else Color.Black.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = secondaryContentColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                text = "No songs yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Add songs to this playlist to start listening",
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryContentColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                            )
                            
                            Button(
                                onClick = { /* Open search or add songs */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = PillShape,
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add Songs",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    // Song List
                    itemsIndexed(playlist.songs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                        val isSelected = uiState.selectedSongIds.contains(song.setVideoId ?: song.id)
                        SongListItem(
                            song = song,
                            isEditable = uiState.isEditable,
                            isSelected = isSelected,
                            isSelectionMode = uiState.isSelectionMode,
                            onReorder = { fromIndex, toIndex -> viewModel.reorderSong(fromIndex, toIndex) },
                            index = index,
                            totalSongs = playlist.songs.size,
                            onClick = { 
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleSongSelection(song)
                                } else {
                                    onSongClick(playlist.songs, index) 
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSongSelection(song)
                            },
                            onMoreClick = { 
                                selectedSong = song
                                showSongMenu = true 
                            },
                            titleColor = contentColor,
                            subtitleColor = secondaryContentColor
                        )
                    }
                }
            }
            
            // Top Bar (Fixed at top)
            androidx.compose.animation.AnimatedVisibility(
                visible = isTopBarVisible,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it })
            ) {
                if (uiState.isSelectionMode) {
                    SelectionTopBar(
                        selectedCount = uiState.selectedSongIds.size,
                        onCloseClick = { viewModel.clearSelection() },
                        onDeleteClick = { viewModel.removeSelectedSongs() },
                        onMoveToTopClick = { viewModel.moveSelectedSongs(0) },
                        contentColor = contentColor,
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    TopBar(
                        title = playlist.title,
                        isScrolled = isScrolled,
                        onBackClick = onBackClick,
                        contentColor = contentColor,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Media Menu Bottom Sheet
            if (showMediaMenu) {
                com.suvojeet.suvmusic.ui.components.MediaMenuBottomSheet(
                    isVisible = showMediaMenu,
                    onDismiss = { showMediaMenu = false },
                    title = playlist.title,
                    subtitle = "${playlist.songs.size} songs",
                    thumbnailUrl = playlist.thumbnailUrl,
                    isUserPlaylist = uiState.isEditable,
                    onShuffle = { onShufflePlay(playlist.songs) },
                    onStartRadio = { onShufflePlay(playlist.songs) },
                    onPlayNext = { viewModel.playNext(playlist.songs) },
                    onAddToQueue = { viewModel.addToQueue(playlist.songs) },
                    onAddToPlaylist = { 
                        if (playlist.songs.isNotEmpty()) {
                            playlistMgmtViewModel.showAddToPlaylistSheet(playlist.songs)
                        }
                    },
                    onDownload = { viewModel.downloadPlaylist(playlist) },
                    onShare = { sharePlaylist(playlist) },
                    onExport = { viewModel.exportPlaylistToM3U(context) },
                    onRename = { showRenameDialog = true },
                    onDelete = { showDeleteDialog = true },
                    showShare = playlist.id != "CACHED_ALL" && playlist.id != "DEVICE_SONGS"
                )
            }
            } // End PullToRefreshBox
        }

        // Song Menu
        if (showSongMenu && selectedSong != null) {
            val song = selectedSong!!
            SongMenuBottomSheet(
                isVisible = showSongMenu,
                onDismiss = { showSongMenu = false },
                song = song,
                isCurrentlyPlaying = song.id == currentSong?.id,
                onPlayNext = { viewModel.playNext(song) },
                onAddToQueue = { viewModel.addToQueue(song) },
                onAddToPlaylist = { playlistMgmtViewModel.showAddToPlaylistSheet(song) },
                onDownload = { viewModel.downloadSong(song) },
                onShare = { shareSong(song) },
                onRemoveFromPlaylist = if (uiState.isEditable) {
                    { viewModel.removeSongFromPlaylist(song) }
                } else null,
                onMoveUp = if (uiState.isEditable && (playlist?.songs?.indexOf(song) ?: -1) > 0) {
                    { 
                        val currentIndex = playlist?.songs?.indexOf(song) ?: -1
                        if (currentIndex > 0) {
                            viewModel.reorderSong(currentIndex, currentIndex - 1)
                        }
                    }
                } else null,
                onMoveDown = if (uiState.isEditable && (playlist?.songs?.indexOf(song) ?: -1) != -1 && (playlist?.songs?.indexOf(song) ?: -1) < (playlist?.songs?.size ?: 0) - 1) {
                    { 
                        val currentIndex = playlist?.songs?.indexOf(song) ?: -1
                        if (currentIndex != -1 && currentIndex < (playlist?.songs?.size ?: 0) - 1) {
                            viewModel.reorderSong(currentIndex, currentIndex + 1)
                        }
                    }
                } else null,
                showShare = playlist?.id != "CACHED_ALL" && playlist?.id != "DEVICE_SONGS"
            )
        }

        // Global Add to Playlist Sheet
        val playlistMgmtState by playlistMgmtViewModel.uiState.collectAsState()

        // Auto-refresh when PlaylistManagementViewModel reports a successful add
        androidx.compose.runtime.LaunchedEffect(playlistMgmtState.successMessage) {
            if (playlistMgmtState.successMessage != null) {
                viewModel.refreshPlaylist()
                playlistMgmtViewModel.clearMessages()
            }
        }

        if (playlistMgmtState.showAddToPlaylistSheet && playlistMgmtState.selectedSongs.isNotEmpty()) {
            com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet(
                songs = playlistMgmtState.selectedSongs,
                isVisible = playlistMgmtState.showAddToPlaylistSheet,
                playlists = playlistMgmtState.userPlaylists,
                isLoading = playlistMgmtState.isLoadingPlaylists,
                onDismiss = { playlistMgmtViewModel.hideAddToPlaylistSheet() },
                onAddToPlaylist = { playlistId -> playlistMgmtViewModel.addSongsToPlaylist(playlistId) },
                onCreateNewPlaylist = { playlistMgmtViewModel.showCreatePlaylistDialog() }
            )
        }

        // Dialogs
        if (showCreateDialog || playlistMgmtState.showCreatePlaylistDialog) {
            com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog(
                isVisible = showCreateDialog || playlistMgmtState.showCreatePlaylistDialog,
                isCreating = uiState.isCreating || playlistMgmtState.isCreatingPlaylist,
                onDismiss = { 
                    showCreateDialog = false
                    playlistMgmtViewModel.hideCreatePlaylistDialog()
                },
                onCreate = { title, desc, isPrivate, syncWithYt ->
                    if (showCreateDialog) {
                        viewModel.createPlaylist(title, desc, isPrivate, syncWithYt)
                        showCreateDialog = false
                    } else {
                        playlistMgmtViewModel.createPlaylist(title, desc, isPrivate, syncWithYt)
                    }
                },
                isLoggedIn = uiState.isLoggedIn
            )
        }

        
        if (showRenameDialog && playlist != null) {
            com.suvojeet.suvmusic.ui.components.RenamePlaylistDialog(
                isVisible = showRenameDialog,
                currentName = playlist.title,
                isRenaming = uiState.isRenaming,
                onDismiss = { showRenameDialog = false },
                onRename = { newName ->
                    viewModel.renamePlaylist(newName)
                    showRenameDialog = false
                }
            )
        }
        
        if (showDeleteDialog && playlist != null) {
            com.suvojeet.suvmusic.ui.components.DeletePlaylistDialog(
                isVisible = showDeleteDialog,
                playlistTitle = playlist.title,
                isDeleting = uiState.isDeleting,
                onDismiss = { showDeleteDialog = false },
                onDelete = {
                    viewModel.deletePlaylist()
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    isScrolled: Boolean,
    onBackClick: () -> Unit,
    contentColor: Color,
    isDarkTheme: Boolean
) {
    // Determine background color when scrolled
    val scrolledColor = if (isDarkTheme) Color(0xFF1D1D1D).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
    val targetColor = if (isScrolled) scrolledColor else Color.Transparent
    val backgroundColor by androidx.compose.animation.animateColorAsState(targetValue = targetColor, label = "TopBarBackground")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .dpadFocusable(
                    onClick = onBackClick,
                    shape = CircleShape,
                )
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = contentColor
            )
        }
        
        // Show title when scrolled
        AnimatedVisibility(
            visible = isScrolled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    batchProgress: Pair<Int, Int>,
    isSaved: Boolean,
    sortType: SortType,
    sortOrder: SortOrder,
    onSortChange: (SortType) -> Unit,
    onToggleSortOrder: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onToggleSave: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onMoreClick: () -> Unit,
    contentColor: Color,
    secondaryContentColor: Color,
    isDarkTheme: Boolean
) {
    val (current, total) = batchProgress
    val isDownloading = total > 0 && current < total
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Artwork
        Box(
            modifier = Modifier
                .size(210.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = SquircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .clip(SquircleShape)
                .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color.LightGray)
        ) {
            if (playlist.thumbnailUrl != null) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Playlist Title (below artwork)
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Author - only show if available
        if (playlist.author.isNotBlank()) {
            Text(
                text = playlist.author,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryContentColor,
                textAlign = TextAlign.Center
            )
        }
        
        // Sort and Info Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = com.suvojeet.suvmusic.util.TimeUtil.formatSongCountAndDuration(playlist.songs),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box {
                Row(
                    modifier = Modifier
                        .clip(SquircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { showSortMenu = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sortType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                    shape = SquircleShape
                ) {
                    SortType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                    fontWeight = if (sortType == type) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = {
                                onSortChange(type)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortType == type) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }

            if (sortType != SortType.CUSTOM) {
                IconButton(
                    onClick = onToggleSortOrder,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (sortOrder == SortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Toggle sort order",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Batch Download Progress
        if (isDownloading) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Downloading $current / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(PillShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions Row (Download, Save, Play, Share, More)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download Button
            BounceButton(
                onClick = onDownload,
                size = 48.dp,
                shape = SquircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = SquircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Playlist",
                    tint = contentColor
                )
            }

            // Save Button (Bookmark icon as requested) - Hide for Liked Songs
            if (playlist.id != "LM") {
                BounceButton(
                    onClick = onToggleSave,
                    size = 48.dp,
                    shape = SquircleShape,
                    modifier = Modifier.background(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                        shape = SquircleShape
                    )
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from Library" else "Save to Library",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else contentColor
                    )
                }
            }
            
            // Play Button (Large Central)
            BounceButton(
                onClick = onPlayAll,
                size = 72.dp,
                shape = CircleShape,
                modifier = Modifier
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // Share Button
            if (playlist.id != "CACHED_ALL" && playlist.id != "DEVICE_SONGS") {
                BounceButton(
                    onClick = onShare,
                    size = 48.dp,
                    shape = SquircleShape,
                    modifier = Modifier.background(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                        shape = SquircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = contentColor
                    )
                }
            }
            
            // More Button
            BounceButton(
                onClick = onMoreClick,
                size = 48.dp,
                shape = SquircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = SquircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = contentColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onCloseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveToTopClick: () -> Unit = {},
    contentColor: Color,
    isDarkTheme: Boolean
) {
    val scrolledColor = if (isDarkTheme) Color(0xFF1D1D1D).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scrolledColor)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCloseClick) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close selection",
                tint = contentColor
            )
        }
        
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onMoveToTopClick) {
            Icon(
                imageVector = Icons.Default.VerticalAlignTop,
                contentDescription = "Move to top",
                tint = contentColor
            )
        }
        
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete selected",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    isEditable: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onReorder: ((from: Int, to: Int) -> Unit)? = null,
    index: Int = 0,
    totalSongs: Int = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    titleColor: Color,
    subtitleColor: Color
) {
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                      else Color.Transparent,
        label = "ItemSelectionBackground"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Indicator (Radio button style)
        if (isSelectionMode) {
            androidx.compose.material3.Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Song Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(SquircleShape)
                .background(Color(0xFF2A2A2A))
        ) {
            if (song.thumbnailUrl != null) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // More Options
        if (!isSelectionMode) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = subtitleColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}
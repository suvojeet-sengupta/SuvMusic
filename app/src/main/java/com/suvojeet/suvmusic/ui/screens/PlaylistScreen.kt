package com.suvojeet.suvmusic.ui.screens

import com.suvojeet.suvmusic.ui.components.BounceButton

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet
import com.suvojeet.suvmusic.ui.components.PremiumLoadingScreen
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistViewModel
import com.suvojeet.suvmusic.util.dpadFocusable

@Composable
fun PlaylistScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit = {},
    onShufflePlay: (List<Song>) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
    playlistMgmtViewModel: com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val batchProgress by viewModel.batchProgress.collectAsState()
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

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMediaMenu by remember { mutableStateOf(false) }
    
    // Song Menu State
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }

    // Handle delete success
    androidx.compose.runtime.LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onBackClick()
        }
    }

    val context = LocalContext.current
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
                                    .size(80.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
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
                                    modifier = Modifier.size(40.dp)
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
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add Songs",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Song List
                    itemsIndexed(playlist.songs) { index, song ->
                        SongListItem(
                            song = song,
                            isEditable = uiState.isEditable,
                            onReorder = { fromIndex, toIndex -> viewModel.reorderSong(fromIndex, toIndex) },
                            index = index,
                            totalSongs = playlist.songs.size,
                            onClick = { onSongClick(playlist.songs, index) },
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
            TopBar(
                title = playlist.title,
                isScrolled = isScrolled,
                onBackClick = onBackClick,
                contentColor = contentColor,
                isDarkTheme = isDarkTheme
            )
            
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
                            playlistMgmtViewModel.showAddToPlaylistSheet(playlist.songs.first())
                        }
                    },
                    onDownload = { viewModel.downloadPlaylist(playlist) },
                    onShare = { sharePlaylist(playlist) },
                    onRename = { showRenameDialog = true },
                    onDelete = { showDeleteDialog = true }
                )
            }
        }

        // Song Menu
        if (showSongMenu && selectedSong != null) {
            val song = selectedSong!!
            SongMenuBottomSheet(
                isVisible = showSongMenu,
                onDismiss = { showSongMenu = false },
                song = song,
                onPlayNext = { viewModel.playNext(song) },
                onAddToQueue = { viewModel.addToQueue(song) },
                onAddToPlaylist = { playlistMgmtViewModel.showAddToPlaylistSheet(song) },
                onDownload = { viewModel.downloadSong(song) },
                onShare = { shareSong(song) }
            )
        }

        // Global Add to Playlist Sheet
        val playlistMgmtState by playlistMgmtViewModel.uiState.collectAsState()
        if (playlistMgmtState.showAddToPlaylistSheet && playlistMgmtState.selectedSong != null) {
            com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet(
                song = playlistMgmtState.selectedSong!!,
                isVisible = playlistMgmtState.showAddToPlaylistSheet,
                playlists = playlistMgmtState.userPlaylists,
                isLoading = playlistMgmtState.isLoadingPlaylists,
                onDismiss = { playlistMgmtViewModel.hideAddToPlaylistSheet() },
                onAddToPlaylist = { playlistId -> playlistMgmtViewModel.addSongToPlaylist(playlistId) },
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
    val scrolledColor = if (isDarkTheme) Color(0xFF1D1D1D) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                if (isScrolled) scrolledColor else Color.Transparent
            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Artwork
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
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
        
        // Updated info (if you have it, otherwise show song count)
        Text(
            text = com.suvojeet.suvmusic.util.TimeUtil.formatSongCountAndDuration(playlist.songs),
            style = MaterialTheme.typography.bodySmall,
            color = secondaryContentColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

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
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
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
                shape = CircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
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
                    shape = CircleShape,
                    modifier = Modifier.background(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                        shape = CircleShape
                    )
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
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
            BounceButton(
                onClick = onShare,
                size = 48.dp,
                shape = CircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = contentColor
                )
            }
            
            // More Button
            BounceButton(
                onClick = onMoreClick,
                size = 48.dp,
                shape = CircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
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
        
        // Sort Option (Date Added)
        if (playlist.id == "LM") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date added",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowDownward, // Or Sort icon
                    contentDescription = null, // decorative
                    tint = contentColor,
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }
        
        if (playlist.id != "LM") Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SongListItem(
    song: Song,
    isEditable: Boolean = false,
    onReorder: ((from: Int, to: Int) -> Unit)? = null,
    index: Int = 0,
    totalSongs: Int = 0,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    titleColor: Color,
    subtitleColor: Color
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Song Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
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
        if (isEditable) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = subtitleColor.copy(alpha = 0.7f)
                    )
                }
                
                androidx.compose.material3.DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (index > 0) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Move Up") },
                            onClick = {
                                showMenu = false
                                onReorder?.invoke(index, index - 1)
                            }
                        )
                    }
                    if (index < totalSongs - 1) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Move Down") },
                            onClick = {
                                showMenu = false
                                onReorder?.invoke(index, index + 1)
                            }
                        )
                    }
                }
            }
        } else {
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
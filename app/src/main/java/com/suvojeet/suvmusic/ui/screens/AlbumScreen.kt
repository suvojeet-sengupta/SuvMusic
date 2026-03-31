package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.BounceButton
import com.suvojeet.suvmusic.ui.components.PremiumLoadingScreen
import com.suvojeet.suvmusic.ui.theme.PillShape
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.viewmodel.AlbumViewModel
import com.suvojeet.suvmusic.util.dpadFocusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun AlbumScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit = {},
    onShufflePlay: (List<Song>) -> Unit = {},
    currentSong: Song? = null,
    viewModel: AlbumViewModel = hiltViewModel(),
    playlistViewModel: com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val batchProgress by viewModel.batchProgress.collectAsState()
    val album = uiState.album
    val haptic = LocalHapticFeedback.current

    // Check if we are in dark theme based on background luminance
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Define colors based on theme
    val backgroundColor = if (isDarkTheme) Color(0xFF0D0D0D) else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)

    // Track scroll position for collapsing header
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100 }
    }

    // Scroll Direction Tracking for Hiding TopBar
    var isScrollingDown by remember { androidx.compose.runtime.mutableStateOf(false) }
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
    
    // Dialog/Menu states
    var showMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showSongMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedSong: Song? by remember { androidx.compose.runtime.mutableStateOf(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val shareAlbum: (Album) -> Unit = { albumToShare ->
        val shareText = "Check out this album: ${albumToShare.title} by ${albumToShare.artist}\n\nhttps://music.youtube.com/playlist?list=${albumToShare.id}"
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Album")
        androidx.core.content.ContextCompat.startActivity(context, shareIntent, null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Blurred background image - Full screen like Apple Music
        if (album?.thumbnailUrl != null) {
            AsyncImage(
                model = album.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp),
                contentScale = ContentScale.Crop,
                alpha = if (isDarkTheme) 0.4f else 0.3f
            )
        }
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color.Transparent,
                                Color(0xFF0D0D0D).copy(alpha = 0.8f),
                                Color(0xFF0D0D0D)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.8f),
                                Color.White
                            )
                        }
                    )
                )
        )
        
        when {
            uiState.isLoading -> {
                PremiumLoadingScreen(
                    thumbnailUrl = album?.thumbnailUrl,
                    onBackClick = onBackClick
                )
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "Unknown Error",
                    color = contentColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            album != null -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 60.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Album Header
                    item {
                        AlbumHeader(
                            album = album,
                            batchProgress = batchProgress,
                            isSaved = uiState.isSaved,
                            onPlayAll = { onPlayAll(album.songs) },
                            onShufflePlay = { onShufflePlay(album.songs) },
                            onToggleSave = { viewModel.toggleSaveToLibrary() },
                            onDownload = { viewModel.downloadAlbum(album) },
                            onShare = { shareAlbum(album) },
                            onMoreClick = { showMenu = true },
                            contentColor = contentColor,
                            secondaryContentColor = secondaryContentColor,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    
                    // Song List
                    itemsIndexed(album.songs, key = { _, song -> song.id }) { index, song ->
                        AlbumSongItem(
                            song = song,
                            trackNumber = index + 1,
                            itemIndex = index,
                            totalSongs = album.songs.size,
                            onReorder = { from, to -> viewModel.reorderSong(from, to) },
                            onClick = { onSongClick(album.songs, index) },
                            onMoreClick = { 
                                selectedSong = song
                                showSongMenu = true 
                            },
                            contentColor = contentColor,
                            secondaryContentColor = secondaryContentColor
                        )
                    }
                }
                
                // Top Bar (Fixed at top)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isTopBarVisible,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it })
                ) {
                    AlbumTopBar(
                        title = album.title,
                        isScrolled = isScrolled,
                        onBackClick = onBackClick,
                        isDarkTheme = isDarkTheme,
                        contentColor = contentColor
                    )
                }
                
                // Media Menu Bottom Sheet
                if (showMenu) {
                    com.suvojeet.suvmusic.ui.components.MediaMenuBottomSheet(
                        isVisible = showMenu,
                        onDismiss = { showMenu = false },
                        title = album.title,
                        subtitle = "${album.songs.size} songs",
                        thumbnailUrl = album.thumbnailUrl,
                        onShuffle = { onShufflePlay(album.songs) },
                        onStartRadio = { onShufflePlay(album.songs) },
                        onPlayNext = { viewModel.playNext(album.songs) },
                        onAddToQueue = { viewModel.addToQueue(album.songs) },
                        onAddToPlaylist = { 
                             selectedSong = album.songs.firstOrNull()
                             selectedSong?.let { song ->
                                 playlistViewModel.showAddToPlaylistSheet(song)
                             }
                        },
                        onDownload = { viewModel.downloadAlbum(album) },
                        onShare = { shareAlbum(album) }
                    )
                }
                
                // Song Menu Bottom Sheet
                selectedSong?.let { song ->
                    if (showSongMenu) {
                        com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet(
                            isVisible = showSongMenu,
                            onDismiss = { showSongMenu = false },
                            song = song,
                            isCurrentlyPlaying = song.id == currentSong?.id,
                            onPlayNext = { viewModel.playNext(listOf(song)) },
                            onAddToQueue = { viewModel.addToQueue(listOf(song)) },
                            onAddToPlaylist = { playlistViewModel.showAddToPlaylistSheet(song) },
                            onDownload = { viewModel.downloadSong(song) },
                            onShare = { 
                                val shareText = "Check out this song: ${song.title} by ${song.artist}\n\nhttps://music.youtube.com/watch?v=${song.id}"
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Song")
                                context.startActivity(shareIntent)
                            }
                        )
                    }
                }

                // Global Add to Playlist Sheet
                val playlistMgmtState by playlistViewModel.uiState.collectAsState()
                if (playlistMgmtState.showAddToPlaylistSheet && playlistMgmtState.selectedSongs.isNotEmpty()) {
                    com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet(
                        songs = playlistMgmtState.selectedSongs,
                        isVisible = true,
                        playlists = playlistMgmtState.userPlaylists,
                        isLoading = playlistMgmtState.isLoadingPlaylists || playlistMgmtState.isAddingSong,
                        onDismiss = { playlistViewModel.hideAddToPlaylistSheet() },
                        onAddToPlaylist = { playlistId -> playlistViewModel.addSongsToPlaylist(playlistId) },
                        onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() }
                    )
                }

                // Create Playlist Dialog
                if (playlistMgmtState.showCreatePlaylistDialog) {
                    com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog(
                        isVisible = playlistMgmtState.showCreatePlaylistDialog,
                        isCreating = playlistMgmtState.isCreatingPlaylist,
                        onDismiss = { playlistViewModel.hideCreatePlaylistDialog() },
                        onCreate = { title, desc, isPrivate, syncWithYt ->
                            playlistViewModel.createPlaylist(title, desc, isPrivate, syncWithYt)
                        },
                        isLoggedIn = true // Assume logged in for now or get from session
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumTopBar(
    title: String,
    isScrolled: Boolean,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    contentColor: Color
) {
    // Determine background color when scrolled
    val scrolledColor = if (isDarkTheme) Color(0xFF1D1D1D).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
    val targetColor = if (isScrolled) scrolledColor else Color.Transparent
    val backgroundColor by androidx.compose.animation.animateColorAsState(targetValue = targetColor, label = "AlbumTopBarBackground")

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
        
        if (!isScrolled) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AlbumHeader(
    album: Album,
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
                .size(210.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = SquircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .clip(SquircleShape)
                .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color.LightGray)
        ) {
            if (album.thumbnailUrl != null) {
                AsyncImage(
                    model = album.thumbnailUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Album Title
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Artist
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryContentColor,
            textAlign = TextAlign.Center
        )
        
        // Year and song count
        Text(
            text = buildString {
                album.year?.let { append("$it • ") }
                append(com.suvojeet.suvmusic.util.TimeUtil.formatSongCountAndDuration(album.songs))
            },
            style = MaterialTheme.typography.bodySmall,
            color = secondaryContentColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Description (if available)
        val description = album.description
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
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

        Spacer(modifier = Modifier.height(24.dp))

        // YT Music inspired Play/Shuffle Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play Button
            Button(
                onClick = onPlayAll,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color.White else Color.Black,
                    contentColor = if (isDarkTheme) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Shuffle Button
            Button(
                onClick = onShufflePlay,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    contentColor = contentColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shuffle",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row (Download, Save, Share, More)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download Button
            ActionButton(
                icon = Icons.Default.Download,
                label = "Download",
                onClick = onDownload,
                contentColor = contentColor,
                isDarkTheme = isDarkTheme
            )

            // Save Button (Heart)
            ActionButton(
                icon = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = "Library",
                onClick = onToggleSave,
                contentColor = if (isSaved) MaterialTheme.colorScheme.primary else contentColor,
                isDarkTheme = isDarkTheme
            )
            
            // Share Button
            ActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = onShare,
                contentColor = contentColor,
                isDarkTheme = isDarkTheme
            )
            
            // More Button
            ActionButton(
                icon = Icons.Default.MoreVert,
                label = "More",
                onClick = onMoreClick,
                contentColor = contentColor,
                isDarkTheme = isDarkTheme
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentColor: Color,
    isDarkTheme: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AlbumSongItem(
    song: Song,
    trackNumber: Int,
    itemIndex: Int = 0,
    totalSongs: Int = 0,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    contentColor: Color,
    secondaryContentColor: Color
) {
    var offsetY by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    
    // Remember updated values for indices to prevent stale state capture in the drag lambda
    val currentIndexState by androidx.compose.runtime.rememberUpdatedState(itemIndex)
    val onReorderState by androidx.compose.runtime.rememberUpdatedState(onReorder)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY.dp / 8)
            .dpadFocusable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryContentColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Drag Handle (On the right)
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Reorder",
            tint = secondaryContentColor.copy(alpha = 0.4f),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(24.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragEnd = { offsetY = 0f },
                        onDragCancel = { offsetY = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetY += dragAmount.y
                            if (offsetY > 50f && currentIndexState < totalSongs - 1) {
                                onReorderState(currentIndexState, currentIndexState + 1)
                                offsetY = 0f
                            } else if (offsetY < -50f && currentIndexState > 0) {
                                onReorderState(currentIndexState, currentIndexState - 1)
                                offsetY = 0f
                            }
                        }                    )
                }
        )

        // More Options
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = secondaryContentColor.copy(alpha = 0.7f)
            )
        }
    }
}

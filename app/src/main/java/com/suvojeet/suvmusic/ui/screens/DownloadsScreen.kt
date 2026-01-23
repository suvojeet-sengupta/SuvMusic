package com.suvojeet.suvmusic.ui.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import com.suvojeet.suvmusic.ui.viewmodel.DownloadsViewModel

@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit = {},
    onShufflePlay: (List<Song>) -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedSongIds by viewModel.selectedSongIds.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    // Theme detection - match PlayerScreen approach
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White

    // Extract dominant colors from first song's artwork
    val firstSongThumbnail = downloadedSongs.firstOrNull()?.thumbnailUrl
    val dominantColors = rememberDominantColors(
        imageUrl = firstSongThumbnail,
        isDarkTheme = isDarkTheme
    )

    // Track scroll position for collapsing header
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100 }
    }

    // Calculate total duration
    val totalDuration = downloadedSongs.sumOf { it.duration }
    val durationText = formatDuration(totalDuration)

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteSong by remember { mutableStateOf<Song?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Dynamic gradient background - like PlayerScreen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dominantColors.secondary,
                            dominantColors.primary,
                            backgroundColor
                        ),
                        startY = 0f,
                        endY = 1200f
                    )
                )
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 140.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header section with collage artwork
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelectionMode) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = dominantColors.onBackground
                                )
                            }
                            
                            Text(
                                text = "${selectedSongIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = dominantColors.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Row {
                                IconButton(onClick = { viewModel.selectAll() }) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Select All",
                                        tint = dominantColors.onBackground
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteSelected() }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Selected",
                                        tint = dominantColors.onBackground
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = dominantColors.onBackground
                                )
                            }
                            
                            Text(
                                text = "DOWNLOADS",
                                style = MaterialTheme.typography.labelMedium,
                                color = dominantColors.onBackground.copy(alpha = 0.7f),
                                letterSpacing = 2.sp
                            )
                            
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = dominantColors.onBackground
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete All", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = { 
                                            showMenu = false
                                            viewModel.deleteAll() 
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Collage or single artwork
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (downloadedSongs.size >= 4) {
                            // 2x2 Collage Grid
                            CollageArtwork(
                                songs = downloadedSongs.take(4),
                                dominantColors = dominantColors
                            )
                        } else if (downloadedSongs.isNotEmpty()) {
                            // Single artwork
                            SingleArtwork(
                                song = downloadedSongs.first(),
                                dominantColors = dominantColors
                            )
                        } else {
                            // Empty state icon
                            EmptyArtworkPlaceholder(dominantColors = dominantColors)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        text = "Downloaded",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = dominantColors.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Song count and duration
                    Text(
                        text = "${downloadedSongs.size} song${if (downloadedSongs.size != 1) "s" else ""} • $durationText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dominantColors.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Play and Shuffle buttons - glassmorphism style
                    if (downloadedSongs.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Play Button - glassmorphism
                            Button(
                                onClick = { onPlayAll(downloadedSongs) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        spotColor = dominantColors.accent.copy(alpha = 0.3f)
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = dominantColors.accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Play",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }

                            // Shuffle Button - glassmorphism
                            Button(
                                onClick = { onShufflePlay(downloadedSongs) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkTheme) 
                                        Color.White.copy(alpha = 0.15f)
                                    else 
                                        Color.Black.copy(alpha = 0.08f),
                                    contentColor = dominantColors.onBackground
                                ),
                                shape = RoundedCornerShape(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Shuffle",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Empty state
            if (downloadedSongs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No downloaded songs yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = dominantColors.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Download songs to listen offline",
                            style = MaterialTheme.typography.bodyMedium,
                            color = dominantColors.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Song list with glassmorphism cards
            itemsIndexed(downloadedSongs) { index, song ->
                DownloadedSongCard(
                    song = song,
                    index = index + 1,
                    onClick = { 
                        if (isSelectionMode) {
                            viewModel.toggleSelection(song.id)
                        } else {
                            onSongClick(downloadedSongs, index) 
                        }
                    },
                    onLongClick = {
                        viewModel.toggleSelection(song.id)
                    },
                    onDeleteClick = {
                        pendingDeleteSong = song
                        showDeleteDialog = true
                    },
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedSongIds.contains(song.id),
                    onSelectionChange = { viewModel.toggleSelection(song.id) },
                    dominantColors = dominantColors,
                    isDarkTheme = isDarkTheme
                )
            }
        }

        // Collapsing header bar (Sticky Top Bar)
        AnimatedVisibility(
            visible = isScrolled,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dominantColors.primary,
                                dominantColors.primary.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = dominantColors.onBackground
                        )
                    }

                    Text(
                        text = "Downloaded",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = dominantColors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && pendingDeleteSong != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteSong = null
            },
            title = {
                Text(
                    text = "Delete Download?",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text("\"${pendingDeleteSong?.title}\" will be removed from your device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteSong?.let { viewModel.deleteDownload(it.id) }
                        showDeleteDialog = false
                        pendingDeleteSong = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        pendingDeleteSong = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 2x2 Collage artwork grid
 */
@Composable
private fun CollageArtwork(
    songs: List<Song>,
    dominantColors: DominantColors
) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = dominantColors.primary.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(modifier = Modifier.weight(1f)) {
                songs.getOrNull(0)?.let { song ->
                    ArtworkTile(song = song, modifier = Modifier.weight(1f))
                }
                songs.getOrNull(1)?.let { song ->
                    ArtworkTile(song = song, modifier = Modifier.weight(1f))
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                songs.getOrNull(2)?.let { song ->
                    ArtworkTile(song = song, modifier = Modifier.weight(1f))
                }
                songs.getOrNull(3)?.let { song ->
                    ArtworkTile(song = song, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ArtworkTile(song: Song, modifier: Modifier = Modifier) {
    Box(modifier = modifier.aspectRatio(1f)) {
        if (song.thumbnailUrl != null) {
            val imageModel = if (song.thumbnailUrl.startsWith("file://")) {
                android.net.Uri.parse(song.thumbnailUrl)
            } else if (song.thumbnailUrl.startsWith("/")) {
                java.io.File(song.thumbnailUrl)
            } else {
                song.thumbnailUrl
            }
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Single artwork display
 */
@Composable
private fun SingleArtwork(
    song: Song,
    dominantColors: DominantColors
) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = dominantColors.primary.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (song.thumbnailUrl != null) {
            val imageModel = if (song.thumbnailUrl.startsWith("file://")) {
                android.net.Uri.parse(song.thumbnailUrl)
            } else if (song.thumbnailUrl.startsWith("/")) {
                java.io.File(song.thumbnailUrl)
            } else {
                song.thumbnailUrl
            }
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty state artwork placeholder
 */
@Composable
private fun EmptyArtworkPlaceholder(dominantColors: DominantColors) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = dominantColors.accent.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        dominantColors.accent.copy(alpha = 0.3f),
                        dominantColors.primary.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DownloadDone,
            contentDescription = null,
            tint = dominantColors.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
    }
}

/**
 * Glassmorphism song card
 */
@Composable
private fun DownloadedSongCard(
    song: Song,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    dominantColors: DominantColors,
    isDarkTheme: Boolean
) {
    val cardBackground = when {
        isSelected -> dominantColors.primary.copy(alpha = 0.2f)
        isDarkTheme -> Color.White.copy(alpha = 0.08f)
        else -> Color.Black.copy(alpha = 0.04f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = dominantColors.accent,
                    uncheckedColor = dominantColors.onBackground.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            // Index number with accent styling
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(dominantColors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = dominantColors.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Thumbnail with shadow
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(8.dp),
                    spotColor = dominantColors.primary.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (song.thumbnailUrl != null) {
                val imageModel = if (song.thumbnailUrl.startsWith("file://")) {
                    android.net.Uri.parse(song.thumbnailUrl)
                } else if (song.thumbnailUrl.startsWith("/")) {
                    java.io.File(song.thumbnailUrl)
                } else {
                    song.thumbnailUrl
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Song info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = dominantColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = dominantColors.accent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = dominantColors.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Delete button (only show when not in selection mode to avoid clutter)
        if (!isSelectionMode) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.alpha(0.7f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = dominantColors.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0 min"
    val totalSeconds = durationMs / 1000
    val minutes = (totalSeconds / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()

    return if (minutes >= 60) {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        "$hours hr $remainingMinutes min"
    } else {
        "$minutes min $seconds sec"
    }
}
package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.DominantColors

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog

@Composable
fun QueueView(
    currentSong: Song?,
    queue: List<Song>,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isAutoplayEnabled: Boolean,
    isFavorite: Boolean,
    isRadioMode: Boolean = false,
    isLoadingMore: Boolean = false,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleLike: () -> Unit,
    onMoreClick: (Song) -> Unit,
    onLoadMore: () -> Unit = {},
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItems: (List<Int>) -> Unit,
    onSaveAsPlaylist: (String, String, Boolean, Boolean) -> Unit,
    dominantColors: DominantColors
) {
    // Capture background color for QueueView as well
    val themeBackgroundColor = MaterialTheme.colorScheme.background
    val haptic = LocalHapticFeedback.current

    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedIndices.isNotEmpty()

    var showSavePlaylistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.secondary,
                        dominantColors.primary,
                        // Use themeBackgroundColor instead of Color.Black
                        themeBackgroundColor
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Selection Mode Top Bar or Header
        if (isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedIndices = emptySet() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Exit selection",
                            tint = dominantColors.onBackground
                        )
                    }
                    Text(
                        text = "${selectedIndices.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = dominantColors.onBackground
                    )
                }

                Row {
                    IconButton(onClick = { 
                        selectedIndices = queue.indices.toSet()
                    }) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "Select all",
                            tint = dominantColors.onBackground
                        )
                    }
                    IconButton(onClick = { 
                        showSavePlaylistDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Save as playlist",
                            tint = dominantColors.onBackground
                        )
                    }
                    IconButton(onClick = { 
                        onRemoveItems(selectedIndices.toList())
                        selectedIndices = emptySet()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = dominantColors.onBackground
                        )
                    }
                }
            }
        } else {
            // Default header with current song or navigation
            if (currentSong != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = currentSong.thumbnailUrl,
                        contentDescription = currentSong.title,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = dominantColors.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = dominantColors.onBackground.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground
                        )
                    }

                    IconButton(onClick = { onMoreClick(currentSong) }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = dominantColors.onBackground
                        )
                    }
                }
            }
        }

        // Playback Controls & Autoplay (Only if not in selection mode)
        if (!isSelectionMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Main Controls (Shuffle, Repeat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (shuffleEnabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onToggleRepeat) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    RepeatMode.ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (repeatMode != RepeatMode.OFF) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Infinite Autoplay Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleAutoplay)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Infinite Autoplay",
                            style = MaterialTheme.typography.labelLarge,
                            color = dominantColors.onBackground,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        androidx.compose.material3.Switch(
                            checked = isAutoplayEnabled,
                            onCheckedChange = { onToggleAutoplay() },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = dominantColors.accent,
                                checkedTrackColor = dominantColors.accent.copy(alpha = 0.3f),
                                uncheckedThumbColor = dominantColors.onBackground.copy(alpha = 0.6f),
                                uncheckedTrackColor = dominantColors.onBackground.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.scale(0.8f) // Make it slightly smaller
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Queue header
        if (!isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Continue Playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = dominantColors.onBackground
                    )
                    Text(
                        text = if (isAutoplayEnabled) "Autoplaying similar music" else "${queue.size} songs in queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = dominantColors.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                IconButton(onClick = { showSavePlaylistDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Save queue as playlist",
                        tint = dominantColors.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Queue list with infinite scroll
        val listState = rememberLazyListState()
        
        // Detect when user scrolls near end (5 items from end) - trigger load more
        val shouldLoadMore = remember {
            derivedStateOf {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = listState.layoutInfo.totalItemsCount
                isRadioMode && !isLoadingMore && lastVisibleItem >= totalItems - 5 && totalItems > 0
            }
        }
        
        LaunchedEffect(shouldLoadMore.value) {
            if (shouldLoadMore.value) {
                onLoadMore()
            }
        }
        
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(queue, key = { index, song -> song.id + "_" + index }) { index, song ->
                QueueItem(
                    song = song,
                    isCurrent = song.id == currentSong?.id,
                    isPlaying = song.id == currentSong?.id && isPlaying,
                    isSelected = selectedIndices.contains(index),
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            selectedIndices = if (selectedIndices.contains(index)) {
                                selectedIndices - index
                            } else {
                                selectedIndices + index
                            }
                        } else {
                            onSongClick(index)
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isSelectionMode) {
                            selectedIndices = setOf(index)
                        } else {
                            onMoreClick(song)
                        }
                    },
                    onMoreClick = { onMoreClick(song) },
                    dominantColors = dominantColors
                )
            }
            
            // Loading indicator at bottom when loading more
            if (isLoadingMore) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = dominantColors.accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Loading more songs...",
                            style = MaterialTheme.typography.bodySmall,
                            color = dominantColors.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Close button
        if (!isSelectionMode) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close queue",
                    tint = dominantColors.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    if (showSavePlaylistDialog) {
        CreatePlaylistDialog(
            isVisible = showSavePlaylistDialog,
            isCreating = false,
            onDismiss = { showSavePlaylistDialog = false },
            onCreate = { title, description, isPrivate, syncWithYt ->
                onSaveAsPlaylist(title, description, isPrivate, syncWithYt)
                showSavePlaylistDialog = false
            }
        )
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) dominantColors.accent.copy(alpha = 0.2f)
                else if (isCurrent) dominantColors.onBackground.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) dominantColors.accent else dominantColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = dominantColors.onBackground.copy(alpha = 0.6f),
                maxLines = 1
            )
        }

        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Playing",
                tint = dominantColors.accent,
                modifier = Modifier.size(24.dp)
            )
        } else if (isSelectionMode) {
             androidx.compose.material3.Checkbox(
                 checked = isSelected,
                 onCheckedChange = { onClick() },
                 colors = androidx.compose.material3.CheckboxDefaults.colors(
                     checkedColor = dominantColors.accent,
                     uncheckedColor = dominantColors.onBackground.copy(alpha = 0.4f)
                 )
             )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Reorder",
                    tint = dominantColors.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = dominantColors.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
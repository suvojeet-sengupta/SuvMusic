package com.suvojeet.suvmusic.ui.screens.player.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.NowPlayingAnimation
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernQueueView(
    currentSong: Song?,
    queue: List<Song>,
    playedSongs: List<Song>,
    upNextSongs: List<Song>,
    selectedQueueIndices: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    currentIndex: Int,
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
    onClearQueue: () -> Unit,
    dominantColors: DominantColors,
    animatedBackgroundEnabled: Boolean = true,
    isDarkTheme: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val isSelectionMode = selectedQueueIndices.isNotEmpty()
    var showSavePlaylistDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantColors.primary.copy(alpha = 0.95f)) // Solid modern background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClearSelection, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, "Close", tint = dominantColors.onBackground)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${selectedQueueIndices.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = dominantColors.onBackground
                        )
                    }
                    Row {
                        IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Select All", tint = dominantColors.onBackground) }
                        IconButton(onClick = { showSavePlaylistDialog = true }) { Icon(Icons.Default.PlaylistAdd, "Save", tint = dominantColors.onBackground) }
                        IconButton(onClick = { onRemoveItems(selectedQueueIndices.toList()) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .dpadFocusable(
                                    onClick = onBack,
                                    shape = CircleShape,
                                )
                                .size(40.dp)
                                .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Close", tint = dominantColors.onBackground, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Playing Next",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = dominantColors.onBackground
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Select button for discoverability
                        Box(
                            modifier = Modifier
                                .clip(SquircleShape)
                                .clickable { if (queue.isNotEmpty()) onToggleSelection(if (currentIndex >= 0) currentIndex else 0) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Select",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = dominantColors.accent
                            )
                        }
                        
                        IconButton(onClick = onClearQueue, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Delete, "Clear Queue", tint = dominantColors.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                        }
                        
                        Box(
                            modifier = Modifier
                                .dpadFocusable(
                                    onClick = { showSavePlaylistDialog = true },
                                    shape = CircleShape,
                                )
                                .size(40.dp)
                                .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlaylistAdd, "Save", tint = dominantColors.onBackground, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // Now Playing Card (Modern Compact)
            if (!isSelectionMode && currentSong != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = SquircleShape,
                    color = dominantColors.accent.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = currentSong.thumbnailUrl,
                                contentDescription = currentSong.title,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(SquircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (isPlaying) {
                                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f), SquircleShape), contentAlignment = Alignment.Center) {
                                    NowPlayingAnimation(color = Color.White, isPlaying = true)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "NOW PLAYING",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
                                color = dominantColors.accent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                currentSong.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = dominantColors.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                currentSong.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = dominantColors.onBackground.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onToggleLike) {
                            Icon(
                                if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                                "Like",
                                tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = { onMoreClick(currentSong) }) {
                            Icon(
                                Icons.Default.MoreVert,
                                "More",
                                tint = dominantColors.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Controls (Modern Switch for Autoplay/Infinite Play)
            if (!isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .clip(SquircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.05f))
                        .clickable { onToggleAutoplay() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(SquircleShape)
                                .background(dominantColors.onBackground.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = if (isAutoplayEnabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Infinite Play",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = dominantColors.onBackground
                            )
                            Text(
                                "Keep the music going forever",
                                style = MaterialTheme.typography.labelSmall,
                                color = dominantColors.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Switch(
                        checked = isAutoplayEnabled,
                        onCheckedChange = { onToggleAutoplay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = dominantColors.accent,
                            uncheckedThumbColor = dominantColors.onBackground.copy(alpha = 0.4f),
                            uncheckedTrackColor = dominantColors.onBackground.copy(alpha = 0.1f),
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            // List
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 1. History Section
                if (playedSongs.isNotEmpty()) {
                    item { ModernHeader("History", dominantColors) }
                    itemsIndexed(playedSongs, key = { index, s -> "history_${s.id}_$index" }) { index, song ->
                        ModernQueueItem(
                            song = song,
                            isCurrent = false,
                            isPlaying = false,
                            isSelected = selectedQueueIndices.contains(index),
                            isSelectionMode = isSelectionMode,
                            onClick = { if (isSelectionMode) onToggleSelection(index) else onSongClick(index) },
                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleSelection(index) },
                            onMoreClick = { onMoreClick(song) },
                            dominantColors = dominantColors
                        )
                    }
                }

                // 2. Currently Playing (In-list indicator)
                if (currentSong != null) {
                    item { ModernHeader("Now Playing", dominantColors) }
                    item(key = "current_${currentSong.id}") {
                        ModernQueueItem(
                            song = currentSong,
                            isCurrent = true,
                            isPlaying = isPlaying,
                            isSelected = selectedQueueIndices.contains(currentIndex),
                            isSelectionMode = isSelectionMode,
                            onClick = { if (isSelectionMode) onToggleSelection(currentIndex) else onPlayPause() },
                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleSelection(currentIndex) },
                            onMoreClick = { onMoreClick(currentSong) },
                            dominantColors = dominantColors
                        )
                    }
                }

                // 3. Upcoming Section
                if (upNextSongs.isNotEmpty()) {
                    item { 
                        ModernHeader(
                            if (isRadioMode || isAutoplayEnabled) "Upcoming (Autoplay)" else "Up Next", 
                            dominantColors 
                        ) 
                    }
                    itemsIndexed(upNextSongs, key = { indexInList, s -> "next_${s.id}_$indexInList" }) { indexInList, song ->
                        // Absolute index in full queue
                        val actualIndex = currentIndex + 1 + indexInList
                        
                        ModernQueueItem(
                            song = song,
                            isCurrent = false,
                            isPlaying = false,
                            isSelected = selectedQueueIndices.contains(actualIndex),
                            isSelectionMode = isSelectionMode,
                            onClick = { if (isSelectionMode) onToggleSelection(actualIndex) else onSongClick(actualIndex) },
                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleSelection(actualIndex) },
                            onMoreClick = { onMoreClick(song) },
                            dominantColors = dominantColors
                        )
                    }
                }

                if (isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator(color = dominantColors.accent, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSavePlaylistDialog) {
        CreatePlaylistDialog(
            isVisible = showSavePlaylistDialog,
            isCreating = false,
            onDismiss = { showSavePlaylistDialog = false },
            onCreate = { title, desc, isPrivate, sync ->
                onSaveAsPlaylist(title, desc, isPrivate, sync)
                showSavePlaylistDialog = false
            }
        )
    }
}

@Composable
private fun ModernHeader(title: String, dominantColors: DominantColors) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = dominantColors.onBackground.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernQueueItem(
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
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .clip(SquircleShape)
            .background(if (isSelected) dominantColors.accent.copy(alpha = 0.2f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(SquircleShape),
                contentScale = ContentScale.Crop
            )
            if (isCurrent && isPlaying) {
                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.4f), SquircleShape), contentAlignment = Alignment.Center) {
                    NowPlayingAnimation(color = dominantColors.accent, isPlaying = true)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
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

        if (isSelectionMode) {
            androidx.compose.material3.Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.scale(0.8f),
                colors = androidx.compose.material3.CheckboxDefaults.colors(
                    checkedColor = dominantColors.accent,
                    uncheckedColor = dominantColors.onBackground.copy(alpha = 0.4f)
                )
            )
        } else {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, "More", tint = dominantColors.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

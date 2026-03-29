package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
    onAddToPlaylistClick: (List<Song>) -> Unit,
    onClearQueue: () -> Unit,
    dominantColors: DominantColors,
    animatedBackgroundEnabled: Boolean = true,
    isDarkTheme: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val isSelectionMode = selectedQueueIndices.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantColors.primary.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Refined Modern Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier
                                .size(36.dp)
                                .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = dominantColors.onBackground, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "${selectedQueueIndices.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = dominantColors.onBackground
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderActionButton(Icons.Default.SelectAll, onSelectAll, dominantColors)
                        HeaderActionButton(Icons.Default.PlaylistAdd, { 
                            val selectedSongs = selectedQueueIndices.mapNotNull { index ->
                                if (index < queue.size) queue[index] else null
                            }
                            onAddToPlaylistClick(selectedSongs) 
                        }, dominantColors)
                        HeaderActionButton(Icons.Default.Delete, { onRemoveItems(selectedQueueIndices.toList()) }, dominantColors, isError = true)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Close", tint = dominantColors.onBackground, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Playing Next",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = dominantColors.onBackground
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onClearQueue) {
                            Icon(Icons.Default.DeleteSweep, "Clear", tint = dominantColors.onBackground.copy(alpha = 0.6f))
                        }
                        IconButton(onClick = { if (queue.isNotEmpty()) onToggleSelection(if (currentIndex >= 0) currentIndex else 0) }) {
                            Icon(Icons.Default.Checklist, "Select", tint = dominantColors.accent)
                        }
                    }
                }
            }

            // Hero Now Playing Card
            if (!isSelectionMode && currentSong != null) {
                HeroNowPlayingCard(currentSong, isPlaying, isFavorite, onToggleLike, { onMoreClick(currentSong) }, dominantColors)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Infinite Play Toggle Section
            if (!isSelectionMode) {
                AutoplayToggleRow(isAutoplayEnabled, onToggleAutoplay, dominantColors)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Queue List
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (playedSongs.isNotEmpty()) {
                    item { SectionDivider("HISTORY", dominantColors) }
                    itemsIndexed(playedSongs, key = { index, s -> "history_${s.id}_$index" }) { index, song ->
                        ModernQueueListItem(
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

                if (currentSong != null) {
                    item { SectionDivider("NOW PLAYING", dominantColors) }
                    item(key = "current_${currentSong.id}") {
                        ModernQueueListItem(
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

                if (upNextSongs.isNotEmpty()) {
                    item { SectionDivider(if (isRadioMode || isAutoplayEnabled) "UPCOMING (AUTOPLAY)" else "UP NEXT", dominantColors) }
                    itemsIndexed(upNextSongs, key = { indexInList, s -> "next_${s.id}_$indexInList" }) { indexInList, song ->
                        val actualIndex = currentIndex + 1 + indexInList
                        ModernQueueListItem(
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
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = dominantColors.accent, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, dominantColors: DominantColors, isError: Boolean = false) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else dominantColors.onBackground.copy(alpha = 0.05f),
            contentColor = if (isError) MaterialTheme.colorScheme.error else dominantColors.onBackground
        )
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HeroNowPlayingCard(song: Song, isPlaying: Boolean, isFavorite: Boolean, onToggleLike: () -> Unit, onMoreClick: () -> Unit, dominantColors: DominantColors) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = SquircleShape,
        color = dominantColors.accent.copy(alpha = 0.1f),
        border = null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(SquircleShape),
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
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = dominantColors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = dominantColors.onBackground.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            IconButton(onClick = onToggleLike) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground)
            }
        }
    }
}

@Composable
private fun AutoplayToggleRow(enabled: Boolean, onToggle: () -> Unit, dominantColors: DominantColors) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onToggle() },
        shape = SquircleShape,
        color = dominantColors.onBackground.copy(alpha = 0.03f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = if (enabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Infinite Play", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = dominantColors.onBackground)
            }
            Switch(
                checked = enabled, onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = dominantColors.accent, uncheckedThumbColor = dominantColors.onBackground.copy(alpha = 0.3f), uncheckedTrackColor = dominantColors.onBackground.copy(alpha = 0.08f), uncheckedBorderColor = Color.Transparent),
                modifier = Modifier.scale(0.7f)
            )
        }
    }
}

@Composable
private fun SectionDivider(title: String, dominantColors: DominantColors) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Black),
        color = dominantColors.onBackground.copy(alpha = 0.3f),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernQueueListItem(
    song: Song, isCurrent: Boolean, isPlaying: Boolean, isSelected: Boolean, isSelectionMode: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit, onMoreClick: () -> Unit, dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(SquircleShape)
            .background(if (isSelected) dominantColors.accent.copy(alpha = 0.15f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = song.thumbnailUrl, contentDescription = null,
                modifier = Modifier.size(52.dp).clip(SquircleShape), contentScale = ContentScale.Crop
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
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = dominantColors.onBackground.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected, onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = dominantColors.accent, uncheckedColor = dominantColors.onBackground.copy(alpha = 0.3f)),
                modifier = Modifier.scale(0.85f)
            )
        } else {
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, null, tint = dominantColors.onBackground.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

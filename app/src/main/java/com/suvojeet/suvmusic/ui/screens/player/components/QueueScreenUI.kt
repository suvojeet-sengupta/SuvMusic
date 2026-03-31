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
import androidx.compose.ui.graphics.Brush
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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernQueueView(
    currentSong: Song?,
    queue: List<Song>,
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
    isDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current
    val isSelectionMode = selectedQueueIndices.isNotEmpty()
    val scope = rememberCoroutineScope()
    
    val backgroundColor = if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.surface
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryContentColor = contentColor.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Gradient Overlay for subtle color tint
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dominantColors.primary.copy(alpha = if (isDarkTheme) 0.15f else 0.1f),
                            backgroundColor
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Refined Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, "Close", tint = contentColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${selectedQueueIndices.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    }
                    Row {
                        IconButton(onClick = onSelectAll) {
                            Icon(Icons.Default.SelectAll, null, tint = contentColor)
                        }
                        IconButton(onClick = { 
                            val selectedSongs = selectedQueueIndices.mapNotNull { index ->
                                if (index < queue.size) queue[index] else null
                            }
                            onAddToPlaylistClick(selectedSongs) 
                        }) {
                            Icon(Icons.Default.PlaylistAdd, null, tint = contentColor)
                        }
                        IconButton(onClick = { onRemoveItems(selectedQueueIndices.toList()) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.KeyboardArrowDown, "Close", tint = contentColor, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Up Next",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClearQueue) {
                            Icon(Icons.Default.DeleteSweep, "Clear", tint = secondaryContentColor)
                        }
                        IconButton(onClick = { if (queue.isNotEmpty()) onToggleSelection(if (currentIndex >= 0) currentIndex else 0) }) {
                            Icon(Icons.Default.Checklist, "Select", tint = dominantColors.accent)
                        }
                    }
                }
            }

            // Hero Now Playing Card
            if (!isSelectionMode && currentSong != null) {
                HeroNowPlayingCard(currentSong, isPlaying, isFavorite, onToggleLike, { onMoreClick(currentSong) }, dominantColors, isDarkTheme, contentColor)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Autoplay Toggle Row
            if (!isSelectionMode) {
                AutoplayToggleRow(isAutoplayEnabled, onToggleAutoplay, dominantColors, isDarkTheme, contentColor)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Queue List
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (currentSong != null) {
                    item { SectionDivider("NOW PLAYING", secondaryContentColor) }
                    item(key = "current_${currentSong.id}") {
                        ModernQueueListItem(
                            song = currentSong,
                            isCurrent = true,
                            isPlaying = isPlaying,
                            isSelected = selectedQueueIndices.contains(currentIndex),
                            isSelectionMode = isSelectionMode,
                            onClick = { if (isSelectionMode) onToggleSelection(currentIndex) else onPlayPause() },
                            onMoreClick = { onMoreClick(currentSong) },
                            onDragMove = { from, to -> onMoveItem(from, to) },
                            itemIndex = currentIndex,
                            dominantColors = dominantColors,
                            isDarkTheme = isDarkTheme,
                            contentColor = contentColor,
                            secondaryContentColor = secondaryContentColor
                        )
                    }
                }

                if (upNextSongs.isNotEmpty()) {
                    item { SectionDivider(if (isRadioMode || isAutoplayEnabled) "UPCOMING (AUTOPLAY)" else "UP NEXT", secondaryContentColor) }
                    itemsIndexed(upNextSongs, key = { _, s -> s.id }) { indexInList, song ->
                        val actualIndex = currentIndex + 1 + indexInList
                        ModernQueueListItem(
                            song = song,
                            isCurrent = false,
                            isPlaying = false,
                            isSelected = selectedQueueIndices.contains(actualIndex),
                            isSelectionMode = isSelectionMode,
                            onClick = { if (isSelectionMode) onToggleSelection(actualIndex) else onSongClick(actualIndex) },
                            onMoreClick = { onMoreClick(song) },
                            onDragMove = { from, to -> onMoveItem(from, to) },
                            itemIndex = actualIndex,
                            dominantColors = dominantColors,
                            isDarkTheme = isDarkTheme,
                            contentColor = contentColor,
                            secondaryContentColor = secondaryContentColor
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
private fun HeroNowPlayingCard(
    song: Song, 
    isPlaying: Boolean, 
    isFavorite: Boolean, 
    onToggleLike: () -> Unit, 
    onMoreClick: () -> Unit, 
    dominantColors: DominantColors,
    isDarkTheme: Boolean,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = contentColor.copy(alpha = 0.05f),
        border = null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                if (isPlaying) {
                    Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        NowPlayingAnimation(color = Color.White, isPlaying = true)
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            IconButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    contentDescription = null, 
                    tint = if (isFavorite) dominantColors.accent else contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AutoplayToggleRow(enabled: Boolean, onToggle: () -> Unit, dominantColors: DominantColors, isDarkTheme: Boolean, contentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = contentColor.copy(alpha = 0.05f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = if (enabled) dominantColors.accent else contentColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Autoplay", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = contentColor)
            }
            Switch(
                checked = enabled, onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, 
                    checkedTrackColor = dominantColors.accent, 
                    uncheckedThumbColor = contentColor.copy(alpha = 0.3f), 
                    uncheckedTrackColor = contentColor.copy(alpha = 0.08f), 
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.scale(0.7f)
            )
        }
    }
}

@Composable
private fun SectionDivider(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.ModernQueueListItem(
    song: Song, isCurrent: Boolean, isPlaying: Boolean, isSelected: Boolean, isSelectionMode: Boolean,
    onClick: () -> Unit, onMoreClick: () -> Unit, 
    onDragMove: (Int, Int) -> Unit, itemIndex: Int,
    dominantColors: DominantColors, isDarkTheme: Boolean, contentColor: Color, secondaryContentColor: Color
) {
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        label = "DragScale"
    )
    
    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "DragElevation"
    )
    
    // Remember updated values for indices to prevent stale state capture in the drag lambda
    val currentIndexState by androidx.compose.runtime.rememberUpdatedState(itemIndex)
    val onDragMoveState by androidx.compose.runtime.rememberUpdatedState(onDragMove)
    
    @OptIn(ExperimentalFoundationApi::class)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .animateItem(
                placementSpec = if (isDragging) null else androidx.compose.animation.core.spring(
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            )
            .graphicsLayer {
                this.translationY = offsetY
                this.scaleX = scale
                this.scaleY = scale
                this.shadowElevation = with(density) { elevation.toPx() }
                this.clip = true
                this.shape = RoundedCornerShape(12.dp)
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) dominantColors.accent.copy(alpha = 0.15f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!isCurrent) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick() // Toggle selection
                    }
                }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
             Checkbox(
                checked = isSelected, onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = dominantColors.accent),
                modifier = Modifier.padding(end = 8.dp).scale(0.85f)
            )
        }

        Box {
            AsyncImage(
                model = song.thumbnailUrl, contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop
            )
            if (isCurrent && isPlaying) {
                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    NowPlayingAnimation(color = dominantColors.accent, isPlaying = true)
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isCurrent) dominantColors.accent else contentColor,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor,
                maxLines = 1
            )
        }
        
        // Drag Handle (Shown only in selection mode or by default on the right if NOT current)
        if (!isCurrent) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = secondaryContentColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { 
                                isDragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress) 
                            },
                            onDragEnd = { 
                                isDragging = false
                                offsetY = 0f 
                            },
                            onDragCancel = { 
                                isDragging = false
                                offsetY = 0f 
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y
                                val threshold = with(density) { 56.dp.toPx() }
                                if (offsetY > threshold) {
                                    onDragMoveState(currentIndexState, currentIndexState + 1)
                                    offsetY -= threshold
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (offsetY < -threshold) {
                                    onDragMoveState(currentIndexState, currentIndexState - 1)
                                    offsetY += threshold
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    }
            )
        }
        
        if (!isSelectionMode) {
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, null, tint = secondaryContentColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

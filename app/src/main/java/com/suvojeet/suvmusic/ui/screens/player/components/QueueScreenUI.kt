package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.RepeatMode
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.NowPlayingAnimation
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalDensity

private val QueueRowShape = RoundedCornerShape(12.dp)
private val QueueRowHorizontalInset = 8.dp
private val QueueRowVerticalInset = 2.dp

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
    onPlayNext: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onClearQueue: () -> Unit,
    dominantColors: DominantColors,
    animatedBackgroundEnabled: Boolean = true,
    isDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    // When hosted inside a modal bottom sheet we drop the status-bar inset (the
    // sheet already insets) and let the sheet supply the drag handle.
    modalMode: Boolean = false,
    // Rendered pinned at the very top when the sheet is fully expanded — the full
    // player (artwork + controls + seekbar), matching YouTube Music's expanded queue.
    nowPlayingHeaderOverride: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val isSelectionMode = selectedQueueIndices.isNotEmpty()
    val scope = rememberCoroutineScope()
    var showQueueMenu by remember { mutableStateOf(false) }
    
    // Flat YouTube-Music-style queue: a single solid surface, no color-tint gradient.
    val backgroundColor = if (isDarkTheme) com.suvojeet.suvmusic.ui.theme.YtFlatBackground else MaterialTheme.colorScheme.surface
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryContentColor = contentColor.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Apply status-bar padding except while the modal sheet is only partially
                // expanded (Material positions that itself). When fully expanded the sheet
                // reaches the top of the screen, so we MUST pad to keep the status bar
                // visible and content beneath it — `nowPlayingHeaderOverride != null` is the
                // "fully expanded" signal (set by the caller only in that state).
                .then(if (modalMode && nowPlayingHeaderOverride == null) Modifier else Modifier.statusBarsPadding())
                .navigationBarsPadding()
        ) {
            // Full player header pinned at the top when the sheet is fully expanded.
            if (nowPlayingHeaderOverride != null) {
                nowPlayingHeaderOverride()
            }

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
                            Icon(
                                Icons.Default.SelectAll,
                                if (selectedQueueIndices.size == queue.size) "Deselect all" else "Select all",
                                tint = contentColor
                            )
                        }
                        
                        IconButton(onClick = { 
                            val selectedSongs = selectedQueueIndices.mapNotNull { index ->
                                // Guard both ends: a stale selection can hold an index
                                // that's now negative or past the end after a queue edit.
                                queue.getOrNull(index)
                            }
                            if (selectedSongs.isNotEmpty()) {
                                onPlayNext(selectedSongs)
                                onClearSelection()
                            }
                        }) {
                            Icon(Icons.Default.PlaylistPlay, null, tint = contentColor)
                        }

                        IconButton(onClick = {
                            val selectedSongs = selectedQueueIndices.mapNotNull { index ->
                                // Guard both ends: a stale selection can hold an index
                                // that's now negative or past the end after a queue edit.
                                queue.getOrNull(index)
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
                    // "Playing from / Your queue" label (matches YouTube Music's queue).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // The modal sheet supplies its own drag handle + swipe-to-dismiss,
                        // so the explicit close chevron is only needed off-sheet (tablet pane).
                        if (!modalMode) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.KeyboardArrowDown, "Close", tint = contentColor, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Column {
                            Text(
                                "Playing from",
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryContentColor
                            )
                            Text(
                                "Your queue",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Save the queue as a playlist.
                        Surface(
                            shape = CircleShape,
                            color = contentColor.copy(alpha = 0.08f),
                            modifier = Modifier.clickable { if (queue.isNotEmpty()) onAddToPlaylistClick(queue) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.PlaylistAdd, null, tint = contentColor, modifier = Modifier.size(20.dp))
                                Text(
                                    "Save",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                            }
                        }

                        // Overflow keeps autoplay / select / clear reachable without
                        // cluttering the header.
                        Box {
                            IconButton(onClick = { showQueueMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More", tint = contentColor)
                            }
                            DropdownMenu(
                                expanded = showQueueMenu,
                                onDismissRequest = { showQueueMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isAutoplayEnabled) "Autoplay: On" else "Autoplay: Off") },
                                    onClick = { onToggleAutoplay(); showQueueMenu = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
                                    trailingIcon = { if (isAutoplayEnabled) Icon(Icons.Default.Check, null, tint = dominantColors.accent) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Select") },
                                    onClick = {
                                        showQueueMenu = false
                                        if (queue.isNotEmpty()) onToggleSelection(if (currentIndex >= 0) currentIndex else 0)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Checklist, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear queue") },
                                    onClick = { showQueueMenu = false; onClearQueue() },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, null) }
                                )
                            }
                        }
                    }
                }
            }

            // Queue List
            val listState = rememberLazyListState()

            // YouTube-Music-style mix filter chips (All / Familiar / Popular / …).
            if (!isSelectionMode) {
                QueueFilterChips(dominantColors = dominantColors, contentColor = contentColor)
            }

            // One list for history, current and up-next, keyed by occurrence so a row
            // keeps its slot (and its in-flight drag) when it crosses the current track.
            val keyedQueue = remember(queue) {
                val seen = HashMap<String, Int>(queue.size)
                queue.map { song ->
                    val occ = seen.getOrDefault(song.id, 0)
                    seen[song.id] = occ + 1
                    song to "${song.id}#$occ"
                }
            }
            val hasHistory = currentIndex > 0 && keyedQueue.isNotEmpty()
            val historyContentColor = contentColor.copy(alpha = 0.55f)
            val historySecondaryColor = secondaryContentColor.copy(alpha = 0.75f)

            LaunchedEffect(currentSong?.id, queue.isNotEmpty()) {
                val target = currentIndex.coerceAtLeast(0) + if (hasHistory) 1 else 0
                listState.scrollToItem(target)
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (hasHistory) {
                    item(key = "history_label", contentType = "label") {
                        Text(
                            text = "Previously played",
                            style = MaterialTheme.typography.labelMedium,
                            color = historySecondaryColor,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                if (keyedQueue.isEmpty() && currentSong != null) {
                    item(key = "current_${currentSong.id}", contentType = "queue_item") {
                        ModernQueueListItem(
                            song = currentSong,
                            isCurrent = true,
                            isPlaying = isPlaying,
                            isSelected = false,
                            isSelectionMode = isSelectionMode,
                            onClick = onPlayPause,
                            onLongPressEnterSelection = {},
                            onMoreClick = { onMoreClick(currentSong) },
                            onDragMove = { _, _ -> },
                            itemIndex = 0,
                            dominantColors = dominantColors,
                            isDarkTheme = isDarkTheme,
                            contentColor = contentColor,
                            secondaryContentColor = secondaryContentColor
                        )
                    }
                }

                itemsIndexed(
                    keyedQueue,
                    key = { _, pair -> pair.second },
                    contentType = { _, _ -> "queue_item" }
                ) { index, (song, _) ->
                    val isCurrentRow = index == currentIndex
                    val isHistoryRow = index < currentIndex
                    val latestIndex by androidx.compose.runtime.rememberUpdatedState(index)
                    val latestOnRemove by androidx.compose.runtime.rememberUpdatedState(onRemoveItems)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { it != SwipeToDismissBoxValue.StartToEnd }
                    )
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            latestOnRemove(listOf(latestIndex))
                            kotlinx.coroutines.delay(350)
                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = !isCurrentRow,
                        backgroundContent = {
                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                val armed = dismissState.targetValue ==
                                    SwipeToDismissBoxValue.EndToStart
                                val iconScale by animateFloatAsState(
                                    targetValue = if (armed) 1.1f else 0.85f,
                                    label = "SwipeDeleteIconScale"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            horizontal = QueueRowHorizontalInset,
                                            vertical = QueueRowVerticalInset
                                        )
                                        .clip(QueueRowShape)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove from queue",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.scale(iconScale)
                                    )
                                }
                            }
                        }
                    ) {
                        ModernQueueListItem(
                            song = song,
                            isCurrent = isCurrentRow,
                            isPlaying = isCurrentRow && isPlaying,
                            isSelected = selectedQueueIndices.contains(index),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                when {
                                    isSelectionMode -> onToggleSelection(index)
                                    isCurrentRow -> onPlayPause()
                                    else -> onSongClick(index)
                                }
                            },
                            onLongPressEnterSelection = { onToggleSelection(index) },
                            onMoreClick = { onMoreClick(song) },
                            onDragMove = { from, to -> onMoveItem(from, to) },
                            itemIndex = index,
                            dragRange = keyedQueue.indices,
                            dominantColors = dominantColors,
                            isDarkTheme = isDarkTheme,
                            contentColor = if (isHistoryRow) historyContentColor else contentColor,
                            secondaryContentColor = if (isHistoryRow) historySecondaryColor else secondaryContentColor
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

/**
 * YouTube-Music-style mix filter chips shown above the queue. Selection is local
 * and presentational for now — the app's autoplay does not expose mix-tuning
 * categories (Familiar / Popular / Discover / Deep cuts), so these tune nothing yet.
 */
@Composable
private fun QueueFilterChips(dominantColors: DominantColors, contentColor: Color) {
    val filters = remember { listOf("All", "Familiar", "Popular", "Discover", "Deep cuts") }
    var selected by remember { mutableStateOf("All") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val isSel = filter == selected
            val background = if (isSel) contentColor.copy(alpha = 0.95f) else contentColor.copy(alpha = 0.08f)
            val foreground = if (isSel) {
                if (contentColor.luminance() > 0.5f) Color.Black else Color.White
            } else {
                contentColor.copy(alpha = 0.85f)
            }
            Surface(
                color = background,
                shape = CircleShape,
                modifier = Modifier.clickable { selected = filter }
            ) {
                Text(
                    text = filter,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = foreground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.ModernQueueListItem(
    song: Song, isCurrent: Boolean, isPlaying: Boolean, isSelected: Boolean, isSelectionMode: Boolean,
    onClick: () -> Unit, onLongPressEnterSelection: () -> Unit, onMoreClick: () -> Unit,
    onDragMove: (Int, Int) -> Unit, itemIndex: Int,
    dragRange: IntRange = IntRange.EMPTY,
    dominantColors: DominantColors, isDarkTheme: Boolean, contentColor: Color, secondaryContentColor: Color
) {
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var rowHeightPx by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rowShape = QueueRowShape
    
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
    val dragRangeState by androidx.compose.runtime.rememberUpdatedState(dragRange)

    // While dragging the row needs an opaque surface so the shadow elevation
    // reads as a "lifted" tile in light mode (where shadow is faint against
    // a near-white background) instead of a translucent floating row.
    val rowBackground = when {
        isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
        isSelected -> dominantColors.accent.copy(alpha = 0.15f)
        isCurrent -> dominantColors.primary.copy(alpha = 0.22f)
        else -> Color.Transparent
    }

    @OptIn(ExperimentalFoundationApi::class)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowHeightPx = it.height }
            .padding(
                horizontal = QueueRowHorizontalInset,
                vertical = QueueRowVerticalInset
            )
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
                this.shape = rowShape
            }
            .clip(rowShape)
            .background(rowBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    // Long press ALWAYS enters selection mode without playing the song
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPressEnterSelection()
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
        
        if (!dragRange.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(32.dp, 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(secondaryContentColor.copy(alpha = if (isDarkTheme) 0.15f else 0.25f))
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
                                // Step by the measured row height so the swapped row lands under
                                // the finger; a fixed guess left a visible jump on every step.
                                val threshold = rowHeightPx.takeIf { it > 0 }?.toFloat()
                                    ?: with(density) { 64.dp.toPx() }
                                var workingIndex = currentIndexState
                                while (offsetY > threshold) {
                                    if (workingIndex + 1 !in dragRangeState) {
                                        offsetY = threshold
                                        break
                                    }
                                    onDragMoveState(workingIndex, workingIndex + 1)
                                    workingIndex += 1
                                    offsetY -= threshold
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                while (offsetY < -threshold) {
                                    if (workingIndex - 1 !in dragRangeState) {
                                        offsetY = -threshold
                                        break
                                    }
                                    onDragMoveState(workingIndex, workingIndex - 1)
                                    workingIndex -= 1
                                    offsetY += threshold
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = secondaryContentColor.copy(alpha = if (isDarkTheme) 0.6f else 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        if (!isSelectionMode) {
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = secondaryContentColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Compact mini-player bar pinned at the top of the queue when the modal sheet is
 * fully expanded — small artwork, title/artist and transport controls with a slim
 * progress line, so the queue list fills the rest of the screen. Mirrors YouTube
 * Music's expanded queue.
 */
@Composable
fun QueuePlayerHeader(
    song: Song?,
    isPlaying: Boolean,
    dominantColors: DominantColors,
    progressProvider: () -> Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    isDarkTheme: Boolean
) {
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song?.thumbnailUrl,
                contentDescription = song?.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, "Previous", tint = contentColor, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = contentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, "Next", tint = contentColor, modifier = Modifier.size(28.dp))
            }
        }
        // Slim progress line beneath the mini-player bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(contentColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressProvider().coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(dominantColors.accent)
            )
        }
    }
}

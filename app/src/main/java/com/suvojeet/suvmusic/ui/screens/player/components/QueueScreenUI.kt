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
                .then(if (modalMode) Modifier else Modifier.statusBarsPadding())
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
                            Icon(Icons.Default.SelectAll, null, tint = contentColor)
                        }
                        
                        IconButton(onClick = { 
                            val selectedSongs = selectedQueueIndices.mapNotNull { index ->
                                if (index < queue.size) queue[index] else null
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
                                if (index < queue.size) queue[index] else null
                            }
                            if (selectedSongs.isNotEmpty()) {
                                onAddToQueue(selectedSongs)
                                onClearSelection()
                            }
                        }) {
                            Icon(Icons.Default.AddToQueue, null, tint = contentColor)
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

            // Compose's LazyList requires unique keys. The queue can legitimately
            // contain the same song twice (manual "Add to queue" of a track that
            // is already enqueued, repeat-one transitions, autoplay echoes), so we
            // disambiguate duplicates by their *occurrence index* within the list
            // rather than by their absolute queue position. The previous
            // "${currentIndex + 1 + idx}_${s.id}" scheme made the key change every
            // time `currentIndex` advanced (every track transition) and every time
            // a row was reordered — which destroyed the row's composable slot
            // mid-gesture. That cancelled the in-flight pointerInput drag
            // coroutine and reset the row's offsetY, so the drag handle worked
            // for exactly one row and then went dead, and the whole list visibly
            // snapped on each track change.
            val keyedUpNext = remember(upNextSongs) {
                val seen = HashMap<String, Int>(upNextSongs.size)
                upNextSongs.map { song ->
                    val occ = seen.getOrDefault(song.id, 0)
                    seen[song.id] = occ + 1
                    song to "${song.id}#$occ"
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (currentSong != null) {
                    item(key = "current_${currentSong.id}") {
                        ModernQueueListItem(
                            song = currentSong,
                            isCurrent = true,
                            isPlaying = isPlaying,
                            isSelected = selectedQueueIndices.contains(currentIndex),
                            isSelectionMode = isSelectionMode,
                            onClick = { if (isSelectionMode) onToggleSelection(currentIndex) else onPlayPause() },
                            onLongPressEnterSelection = { onToggleSelection(currentIndex) },
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
                    itemsIndexed(
                        keyedUpNext,
                        key = { _, pair -> pair.second }
                    ) { indexInList, (song, _) ->
                        val actualIndex = currentIndex + 1 + indexInList
                        ModernQueueListItem(
                            song = song,
                            isCurrent = false,
                            isPlaying = false,
                            isSelected = selectedQueueIndices.contains(actualIndex),
                            isSelectionMode = isSelectionMode,
                            onClick = { if (isSelectionMode) onToggleSelection(actualIndex) else onSongClick(actualIndex) },
                            onLongPressEnterSelection = { onToggleSelection(actualIndex) },
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
        
        // Drag Handle (Shown only in selection mode or by default on the right if NOT current)
        if (!isCurrent) {
            // Improved handle visibility with better contrast for both light and dark modes
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
                                // Match the playlist screen's threshold
                                // (~64dp ≈ row height incl. padding) so the
                                // row that was dragged into the dropping
                                // slot ends up exactly under the user's
                                // finger after the swap. The previous 56dp
                                // value caused a visible ~8dp jump on each
                                // step because the LazyColumn snapped the
                                // item to its new layout position which is
                                // a few dp lower than where translationY
                                // had reset to.
                                //
                                // Track the working index locally so a fast
                                // drag that crosses several rows in one
                                // callback uses fresh indices for each
                                // move instead of repeatedly hitting the
                                // (now-stale) `currentIndexState` value
                                // captured at the start of this dispatch.
                                val threshold = with(density) { 64.dp.toPx() }
                                var workingIndex = currentIndexState
                                while (offsetY > threshold) {
                                    onDragMoveState(workingIndex, workingIndex + 1)
                                    workingIndex += 1
                                    offsetY -= threshold
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                while (offsetY < -threshold) {
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
                Icon(Icons.Default.MoreVert, null, tint = secondaryContentColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatQueueTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * Full player header shown pinned at the top of the queue when the modal sheet is
 * fully expanded — large artwork, title/artist, a seekbar with time labels and the
 * transport controls. Mirrors YouTube Music's expanded "Your queue" view.
 */
@Composable
fun QueuePlayerHeader(
    song: Song?,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    dominantColors: DominantColors,
    progressProvider: () -> Float,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    isDarkTheme: Boolean
) {
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    // While the user is dragging the slider we hold a local override so the thumb
    // tracks the finger instead of snapping back to the (still-old) play position.
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val sliderValue = (dragValue ?: progressProvider()).coerceIn(0f, 1f)
    val displayPosition = dragValue?.let { (it * durationProvider()).toLong() } ?: positionProvider()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = song?.thumbnailUrl,
            contentDescription = song?.title,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = song?.title ?: "",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = song?.artist ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onSeekTo((it * durationProvider()).toLong()) }
                dragValue = null
            },
            colors = SliderDefaults.colors(
                thumbColor = dominantColors.accent,
                activeTrackColor = dominantColors.accent,
                inactiveTrackColor = contentColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatQueueTime(displayPosition), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
            Text(formatQueueTime(durationProvider()), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaybackControls(
            isPlaying = isPlaying,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onShuffleToggle = onShuffleToggle,
            onRepeatToggle = onRepeatToggle,
            dominantColors = dominantColors,
            compact = true
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

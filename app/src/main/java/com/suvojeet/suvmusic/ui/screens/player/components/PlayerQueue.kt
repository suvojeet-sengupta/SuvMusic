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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.DominantColors

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
    onMoreClick: () -> Unit,
    onLoadMore: () -> Unit = {},
    dominantColors: DominantColors
) {
    // Capture background color for QueueView as well
    val themeBackgroundColor = MaterialTheme.colorScheme.background

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
        // Current song header
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

                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = dominantColors.onBackground
                    )
                }
            }
        }

        // Playback mode chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlaybackChip(
                text = "Shuffle",
                icon = Icons.Default.Shuffle,
                isSelected = shuffleEnabled,
                dominantColors = dominantColors,
                onClick = onToggleShuffle
            )
            PlaybackChip(
                text = "Repeat",
                icon = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                isSelected = repeatMode != RepeatMode.OFF,
                dominantColors = dominantColors,
                onClick = onToggleRepeat
            )
            PlaybackChip(
                text = "Autoplay",
                icon = Icons.Default.PlayArrow,
                isSelected = isAutoplayEnabled,
                dominantColors = dominantColors,
                onClick = onToggleAutoplay
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Queue header
        Text(
            text = "Continue Playing",
            style = MaterialTheme.typography.titleMedium,
            color = dominantColors.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = if (isAutoplayEnabled) "Autoplaying similar music" else "${queue.size} songs in queue",
            style = MaterialTheme.typography.bodySmall,
            color = dominantColors.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

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
            itemsIndexed(queue) { index, song ->
                QueueItem(
                    song = song,
                    isCurrent = song.id == currentSong?.id,
                    isPlaying = song.id == currentSong?.id && isPlaying,
                    onClick = { onSongClick(index) },
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

@Composable
private fun PlaybackChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) dominantColors.onBackground.copy(alpha = 0.2f)
        else dominantColors.onBackground.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = dominantColors.onBackground,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = dominantColors.onBackground
            )
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrent) dominantColors.onBackground.copy(alpha = 0.1f) else Color.Transparent)
            .padding(vertical = 8.dp),
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
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Drag",
                tint = dominantColors.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.LoadingIndicator
import com.suvojeet.suvmusic.ui.theme.SquircleShape

@Composable
fun RelatedSheet(
    isVisible: Boolean,
    relatedSongs: List<Song>,
    isLoading: Boolean,
    selectedIndices: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onAddSelectedToQueue: () -> Unit,
    onAddSelectedToPlaylist: () -> Unit,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    onClose: () -> Unit,
    dominantColors: DominantColors
) {
    if (!isVisible) return
    
    val isSelectionMode = selectedIndices.isNotEmpty()
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantColors.primary.copy(alpha = 0.98f))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                            "${selectedIndices.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = dominantColors.onBackground
                        )
                    }
                    Row {
                        IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Select All", tint = dominantColors.onBackground) }
                        IconButton(onClick = onAddSelectedToPlaylist) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to Playlist", tint = dominantColors.onBackground) }
                        IconButton(onClick = onAddSelectedToQueue) { Icon(Icons.Default.Add, "Add to Queue", tint = dominantColors.onBackground) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Close", tint = dominantColors.onBackground)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Related Songs",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = dominantColors.onBackground
                        )
                    }
                }
            }

            if (isLoading && relatedSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(color = dominantColors.accent)
                }
            } else if (relatedSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No related songs found", color = dominantColors.onBackground.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(relatedSongs, key = { _, song -> song.id }) { index, song ->
                        RelatedSongItem(
                            song = song,
                            isSelected = selectedIndices.contains(index),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(index)
                                } else {
                                    onSongClick(song)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleSelection(index)
                            },
                            onMoreClick = { onMoreClick(song) },
                            dominantColors = dominantColors
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RelatedSongItem(
    song: Song,
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
            .background(if (isSelected) dominantColors.onBackground.copy(alpha = 0.1f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(SquircleShape),
                contentScale = ContentScale.Crop
            )
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(SquircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = dominantColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = dominantColors.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isSelectionMode) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = dominantColors.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

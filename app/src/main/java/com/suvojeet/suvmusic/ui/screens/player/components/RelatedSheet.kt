package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.LoadingIndicator
import com.suvojeet.suvmusic.ui.theme.SquircleShape

@OptIn(ExperimentalFoundationApi::class)
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
                            "${selectedIndices.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = dominantColors.onBackground
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderActionButton(Icons.Default.SelectAll, onSelectAll, dominantColors)
                        HeaderActionButton(Icons.AutoMirrored.Filled.PlaylistAdd, onAddSelectedToPlaylist, dominantColors)
                        HeaderActionButton(Icons.Default.Add, onAddSelectedToQueue, dominantColors)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Close", tint = dominantColors.onBackground, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Related Songs",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = dominantColors.onBackground
                        )
                    }
                }
            }

            if (isLoading && relatedSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 64.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = dominantColors.accent, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Finding similar music...", style = MaterialTheme.typography.bodyMedium, color = dominantColors.onBackground.copy(alpha = 0.5f))
                    }
                }
            } else if (relatedSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No related songs found", color = dominantColors.onBackground.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            "BASED ON YOUR CURRENT PLAYBACK",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Black),
                            color = dominantColors.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    itemsIndexed(relatedSongs, key = { _, song -> song.id }) { index, song ->
                        ModernRelatedListItem(
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

@Composable
private fun HeaderActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, dominantColors: DominantColors) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = dominantColors.onBackground.copy(alpha = 0.05f),
            contentColor = dominantColors.onBackground
        )
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernRelatedListItem(
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
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(SquircleShape)
            .background(if (isSelected) dominantColors.accent.copy(alpha = 0.15f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(SquircleShape),
                contentScale = ContentScale.Crop
            )
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(SquircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

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
                style = MaterialTheme.typography.bodySmall,
                color = dominantColors.onBackground.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = dominantColors.accent, uncheckedColor = dominantColors.onBackground.copy(alpha = 0.3f)),
                modifier = Modifier.scale(0.85f)
            )
        } else {
            IconButton(onClick = onMoreClick) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = dominantColors.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

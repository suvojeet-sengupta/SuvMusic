package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.ImageUtils

/**
 * A song row with a drag handle for reordering, selection support and an overflow action.
 * Shared by the playlist and album song lists.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.ReorderableSongRow(
    song: Song,
    titleColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit,
    index: Int = 0,
    totalSongs: Int = 0,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onReorder: ((from: Int, to: Int) -> Unit)? = null,
    onLongClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
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
    val currentIndexState by androidx.compose.runtime.rememberUpdatedState(index)
    val onReorderState by androidx.compose.runtime.rememberUpdatedState(onReorder)

    // Selection tint when not dragging; opaque surface while dragging so the
    // lifted tile reads cleanly in light mode (shadow alone is too faint there).
    val draggingSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val selectionTint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isDragging -> draggingSurface
            isSelected -> selectionTint
            else -> Color.Transparent
        },
        label = "ItemSelectionBackground"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                this.shape = SquircleShape
            }
            .clip(SquircleShape)
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Indicator (Radio button style)
        if (isSelectionMode) {
            androidx.compose.material3.Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Song Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(SquircleShape)
                .background(Color(0xFF2A2A2A))
        ) {
            if (song.thumbnailUrl != null) {
                val ctx = LocalContext.current
                val rowHighRes = remember(song.thumbnailUrl) {
                    ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl, size = 256) ?: song.thumbnailUrl
                }
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(rowHighRes)
                        .crossfade(true)
                        .size(160)
                        .build(),
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Actions Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Drag Handle (Always shown for immediate reordering)
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = subtitleColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(36.dp)
                    .padding(8.dp)
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
                                val threshold = with(density) { 60.dp.toPx() }
                                // Use a `while` loop with a locally-tracked
                                // working index so a fast drag that crosses
                                // several rows in a single callback walks
                                // through them all instead of stopping
                                // after one step (which produced visible
                                // lag when the user flicked quickly).
                                var workingIndex = currentIndexState
                                while (offsetY > threshold && workingIndex < totalSongs - 1) {
                                    onReorderState?.invoke(workingIndex, workingIndex + 1)
                                    workingIndex += 1
                                    offsetY -= threshold
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                while (offsetY < -threshold && workingIndex > 0) {
                                    onReorderState?.invoke(workingIndex, workingIndex - 1)
                                    workingIndex -= 1
                                    offsetY += threshold
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    }
            )

            // More Options (Shown when NOT in selection mode)
            if (!isSelectionMode) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = subtitleColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

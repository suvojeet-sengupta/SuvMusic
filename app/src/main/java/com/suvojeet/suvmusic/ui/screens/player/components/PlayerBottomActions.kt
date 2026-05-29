package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.model.DownloadState
import com.suvojeet.suvmusic.ui.components.DominantColors

/**
 * YouTube-Music-style action chip row that sits directly beneath the song info
 * (title + artist), above the seekbar — matching the now-playing layout where
 * Like / Dislike / Lyrics / Comments are quick chips. Horizontally scrollable so
 * the chips never clip on narrow screens.
 */
@Composable
fun PlayerActionChips(
    isFavorite: Boolean,
    isDisliked: Boolean,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onLyricsClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onRelatedClick: () -> Unit,
    onDownloadClick: () -> Unit,
    downloadState: DownloadState,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like | Dislike segmented pill
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(dominantColors.onBackground.copy(alpha = 0.08f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipIconButton(
                icon = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.9f),
                contentDescription = "Like",
                onClick = onToggleLike
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(dominantColors.onBackground.copy(alpha = 0.15f))
            )
            ChipIconButton(
                icon = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                tint = if (isDisliked) MaterialTheme.colorScheme.error else dominantColors.onBackground.copy(alpha = 0.9f),
                contentDescription = "Dislike",
                onClick = onToggleDislike
            )
        }

        BottomChip(label = "Lyrics", icon = Icons.Default.Lyrics, onClick = onLyricsClick, dominantColors = dominantColors)
        BottomChip(label = "Comments", icon = Icons.Default.Comment, onClick = onCommentsClick, dominantColors = dominantColors)
        BottomChip(label = "Related", icon = Icons.Default.Recommend, onClick = onRelatedClick, dominantColors = dominantColors)

        // Download keeps its four-state animation, wrapped in a chip-shaped pill.
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(dominantColors.onBackground.copy(alpha = 0.08f))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            DownloadAction(
                downloadState = downloadState,
                onClick = onDownloadClick,
                dominantColors = dominantColors
            )
        }
    }
}

/**
 * Bottom "Your queue" handle — a tappable grabber + label that opens the queue,
 * mirroring YouTube Music's pull-up queue affordance at the foot of the player.
 */
@Composable
fun QueueHandle(
    onClick: () -> Unit,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(dominantColors.onBackground.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your queue",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = dominantColors.onBackground
        )
    }
}

@Composable
private fun ChipIconButton(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BottomChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    dominantColors: DominantColors,
    enabled: Boolean = true
) {
    val tint = if (enabled) dominantColors.onBackground.copy(alpha = 0.9f)
    else dominantColors.onBackground.copy(alpha = 0.3f)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(dominantColors.onBackground.copy(alpha = if (enabled) 0.08f else 0.04f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * State-driven download icon. AnimatedContent gives the state change a
 * subtle scale/fade so the user notices the moment a download finishes
 * without us needing a separate toast.
 */
@Composable
private fun DownloadAction(
    downloadState: DownloadState,
    onClick: () -> Unit,
    dominantColors: DominantColors,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                // Block taps while in flight — repeated clicks during a
                // download just spawn duplicate jobs that get deduped at
                // the repo layer; better to short-circuit here.
                enabled = downloadState != DownloadState.DOWNLOADING,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = downloadState,
            transitionSpec = {
                (scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180)))
                    .togetherWith(scaleOut(tween(120)) + fadeOut(tween(120)))
            },
            label = "download-state",
        ) { state ->
            when (state) {
                DownloadState.DOWNLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = dominantColors.accent,
                    )
                }
                DownloadState.DOWNLOADED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = dominantColors.accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DownloadState.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Download failed — tap to retry",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DownloadState.NOT_DOWNLOADED -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = dominantColors.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.screens.player.formatDuration
import com.suvojeet.suvmusic.ui.screens.player.components.AudioQualityDialog

import androidx.compose.material3.LoadingIndicator

@Composable
fun SongInfoSection(
    song: Song?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    isDisliked: Boolean = false,
    onDislikeClick: () -> Unit = {},
    onMoreClick: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    dominantColors: DominantColors,
    isLoading: Boolean = false,
    compact: Boolean = false,
    sleepTimerRemainingMs: Long? = null,
    sleepTimerOption: com.suvojeet.suvmusic.player.SleepTimerOption = com.suvojeet.suvmusic.player.SleepTimerOption.OFF
) {
    var showQualityDialog by remember { mutableStateOf(false) }

    if (showQualityDialog) {
        AudioQualityDialog(
            showDialog = showQualityDialog,
            onDismiss = { showQualityDialog = false },
            dominantColors = dominantColors
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = song?.id,
                transitionSpec = {
                    (slideInVertically(
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                    ) { it / 3 } + fadeIn()) togetherWith
                    (slideOutVertically { -it / 3 } + fadeOut())
                },
                label = "songInfoTransition"
            ) { _ ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            LoadingIndicator(
                                modifier = Modifier.size(if (compact) 18.dp else 22.dp).padding(end = 8.dp),
                                color = dominantColors.accent
                            )
                        }
                        Text(
                            text = song?.title ?: "No song playing",
                            style = if (compact) {
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                )
                            } else {
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                            },
                            color = dominantColors.onBackground,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))

                    Text(
                        text = song?.artist ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp
                        ),
                        color = dominantColors.onBackground.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .clickable {
                                val target = song?.artistId ?: song?.artist
                                target?.let { onArtistClick(it) }
                            }
                    )
                }
            }

            // Sleep Timer indicator - M3 Expressive style
            androidx.compose.animation.AnimatedVisibility(
                visible = sleepTimerOption != com.suvojeet.suvmusic.player.SleepTimerOption.OFF,
                enter = fadeIn() + slideInVertically { -20 },
                exit = fadeOut() + slideOutVertically { -20 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                        tint = dominantColors.accent.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sleepTimerOption == com.suvojeet.suvmusic.player.SleepTimerOption.END_OF_SONG) {
                            "Sleep at end of song"
                        } else {
                            sleepTimerRemainingMs?.let { ms ->
                                val minutes = (ms / 60000).toInt()
                                val seconds = ((ms % 60000) / 1000).toInt()
                                String.format("Sleep in %d:%02d", minutes, seconds)
                            } ?: ""
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = dominantColors.accent.copy(alpha = 0.9f)
                    )
                }
            }

            // Audio Quality Badge - Apple Music style
            if (song != null) {
                Spacer(modifier = Modifier.height(if (compact) 3.dp else 6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            color = dominantColors.onBackground.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { showQualityDialog = true }
                        .padding(
                            horizontal = if (compact) 6.dp else 8.dp,
                            vertical = if (compact) 2.dp else 4.dp
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = dominantColors.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (song.source) {
                            SongSource.JIOSAAVN -> "AAC • 320kbps"
                            SongSource.LOCAL -> "Local"
                            else -> "Opus • 160kbps"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        ),
                        color = dominantColors.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dislike button
            if (isDisliked) {
                FilledIconButton(
                    onClick = onDislikeClick,
                    modifier = Modifier.size(42.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.ThumbDown, "Undislike", modifier = Modifier.size(22.dp))
                }
            } else {
                IconButton(
                    onClick = onDislikeClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(dominantColors.onBackground.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.ThumbDown, "Dislike",
                        tint = dominantColors.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Like button
            if (isFavorite) {
                FilledIconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(42.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = dominantColors.accent.copy(alpha = 0.2f),
                        contentColor = dominantColors.accent
                    )
                ) {
                    Icon(Icons.Filled.ThumbUp, "Unlike", modifier = Modifier.size(22.dp))
                }
            } else {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(dominantColors.onBackground.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.ThumbUp, "Like",
                        tint = dominantColors.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(dominantColors.onBackground.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = dominantColors.onBackground.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun TimeLabelsWithQuality(
    currentPositionProvider: () -> Long,
    durationProvider: () -> Long,
    dominantColors: DominantColors
) {
    val currentPosition = currentPositionProvider()
    val duration = durationProvider()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDuration(currentPosition),
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f)
        )

        Text(
            text = "-${formatDuration(duration - currentPosition)}",
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f)
        )
    }
}

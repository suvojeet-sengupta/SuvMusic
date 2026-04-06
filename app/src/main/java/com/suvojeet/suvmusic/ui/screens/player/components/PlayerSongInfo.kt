package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
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
    sleepTimerOption: com.suvojeet.suvmusic.player.SleepTimerOption = com.suvojeet.suvmusic.player.SleepTimerOption.OFF,
    showMoreButton: Boolean = true,
    isClassic: Boolean = false,
    isAIEnabled: Boolean = false,
    aiStatus: String? = null
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
            // Title and Capsule Row (YT Music style uses this)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                                Box(
                                    modifier = Modifier.width(if (isLoading) (if (compact) 26.dp else 30.dp) else 0.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (isLoading) {
                                        LoadingIndicator(
                                            modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                                            color = dominantColors.accent
                                        )
                                    }
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
                                    ).weight(1f, fill = false)
                                )

                                // AI EQ Indicator Badge
                                if (isAIEnabled) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = dominantColors.accent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, dominantColors.accent.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = dominantColors.accent
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "AI",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp
                                                ),
                                                color = dominantColors.accent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isClassic) {
                    Spacer(modifier = Modifier.width(12.dp))

                    // Like/Dislike Capsule (Visible only in YT Music style here)
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(dominantColors.onBackground.copy(alpha = 0.08f))
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like button
                        IconButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(dominantColors.onBackground.copy(alpha = 0.15f))
                        )

                        // Dislike button
                        IconButton(
                            onClick = onDislikeClick,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (isDisliked) MaterialTheme.colorScheme.error else dominantColors.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 1.dp else 2.dp))

            // Artist and More button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song?.artist ?: "",
                    style = if (compact) {
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp
                        )
                    },
                    color = dominantColors.onBackground.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                        .clickable {
                            val target = song?.artistId ?: song?.artist
                            target?.let { onArtistClick(it) }
                        }
                )

                if (!isClassic && showMoreButton) {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = dominantColors.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // AI Processing Status Indicator
            androidx.compose.animation.AnimatedVisibility(
                visible = aiStatus != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "aiPulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "aiPulseAlpha"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dominantColors.accent.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = aiStatus ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = dominantColors.accent,
                        modifier = Modifier.graphicsLayer { this.alpha = alpha }
                    )
                }
            }
            
            // Artist Radio Badge
            if (song?.album?.startsWith("Artist Radio:") == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dominantColors.accent.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = dominantColors.accent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Artist Radio",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = dominantColors.accent
                    )
                }
            }
            
            // ... rest of the column content (sleep timer, quality)
            
            // Sleep Timer indicator
            androidx.compose.animation.AnimatedVisibility(
                visible = sleepTimerOption != com.suvojeet.suvmusic.player.SleepTimerOption.OFF,
                enter = fadeIn() + slideInVertically { -20 },
                exit = fadeOut() + slideOutVertically { -20 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 12.dp else 14.dp),
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

            // Audio Quality Badge
            if (song != null) {
                Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            color = dominantColors.onBackground.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { showQualityDialog = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
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

        if (isClassic) {
            Spacer(modifier = Modifier.width(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Like/Dislike Capsule (Classic style placement)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.08f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(dominantColors.onBackground.copy(alpha = 0.15f))
                    )

                    // Dislike button
                    IconButton(
                        onClick = onDislikeClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                            contentDescription = "Dislike",
                            tint = if (isDisliked) MaterialTheme.colorScheme.error else dominantColors.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Increased 3-dot button size for Classic style
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .size(46.dp)
                        .background(dominantColors.onBackground.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = dominantColors.onBackground.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                }
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
            .padding(vertical = 2.dp),
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

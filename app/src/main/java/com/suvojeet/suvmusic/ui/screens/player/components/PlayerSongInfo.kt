package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.data.model.DownloadState
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.screens.player.formatDuration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable

@Composable
fun SongInfoSection(
    song: Song?,
    isFavorite: Boolean,
    isDisliked: Boolean = false,
    downloadState: DownloadState,
    onFavoriteClick: () -> Unit,
    onDislikeClick: () -> Unit = {},
    onDownloadClick: () -> Unit,
    onMoreClick: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    dominantColors: DominantColors,
    compact: Boolean = false
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
            Text(
                text = song?.title ?: "No song playing",
                style = if (compact) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                },
                color = dominantColors.onBackground,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    // Album navigation disabled as Song model doesn't consistently have albumId
                    // .clickable { onAlbumClick(song.albumId) }
            )

            Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))

            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = dominantColors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = song?.artistId?.let { artistId ->
                   Modifier.clickable { onArtistClick(artistId) }
                } ?: Modifier
            )

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
                            SongSource.JIOSAAVN -> "HQ Audio • 320kbps"
                            SongSource.LOCAL -> "Local"
                            else -> "Opus • HQ Audio"
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
            val iconButtonModifier = Modifier
                .size(42.dp)
                .background(
                    color = dominantColors.onBackground.copy(alpha = 0.08f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )

            // Download Button
            IconButton(
                onClick = onDownloadClick,
                modifier = iconButtonModifier
            ) {
                when(downloadState) {
                    DownloadState.DOWNLOADING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = dominantColors.accent,
                            strokeWidth = 2.dp
                        )
                    }
                    DownloadState.DOWNLOADED -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = dominantColors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DownloadState.FAILED -> {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Retry Download",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = dominantColors.onBackground.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = iconButtonModifier
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Like",
                    tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Dislike Button — signals the recommendation engine to avoid similar songs
            IconButton(
                onClick = onDislikeClick,
                modifier = iconButtonModifier
            ) {
                Icon(
                    imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                    contentDescription = "Dislike",
                    tint = if (isDisliked) MaterialTheme.colorScheme.error else dominantColors.onBackground.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = iconButtonModifier
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

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
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
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.screens.player.formatDuration

@Composable
fun SongInfoSection(
    song: Song?,
    isFavorite: Boolean,
    downloadState: DownloadState,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onMoreClick: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song?.title ?: "No song playing",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = dominantColors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = dominantColors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Audio Quality Badge - Apple Music style
            if (song != null) {
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = dominantColors.onBackground.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = dominantColors.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (song.source) {
                            SongSource.JIOSAAVN -> "HQ Audio • 320kbps"
                            SongSource.LOCAL -> "Local"
                            else -> "Standard Quality"
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

        // Download Button
        IconButton(onClick = onDownloadClick) {
            when(downloadState) {
                DownloadState.DOWNLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = dominantColors.accent,
                        strokeWidth = 2.dp
                    )
                }
                DownloadState.DOWNLOADED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = dominantColors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                DownloadState.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Retry Download",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = dominantColors.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = "Favorite",
                tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = dominantColors.onBackground.copy(alpha = 0.7f)
            )
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
            .padding(horizontal = 8.dp, vertical = 8.dp),
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

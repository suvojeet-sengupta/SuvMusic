package com.suvojeet.suvmusic.composeapp.ui.components.player.miniplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.image.DominantColors
import com.suvojeet.suvmusic.composeapp.ui.AlbumArt
import com.suvojeet.suvmusic.composeapp.util.ImageUtils
import com.suvojeet.suvmusic.core.model.Song

/**
 * Standard rectangular mini-player — port of
 * `app/.../ui/components/player/miniplayer/StandardMiniPlayer.kt`.
 *
 * API note: the Android original took a heavy `PlayerState` (37 fields,
 * pulls in `OutputDevice` / `AudioQuality` / `DownloadState`). The shared
 * version takes only what it actually renders — [isPlaying] for the
 * play/pause toggle. Callers map their state into these primitives
 * (`PlayerState.isPlaying`, etc.). Same visual output, but the surface
 * is now portable.
 *
 * @param artworkShape one of "CIRCLE", "VINYL", "SQUARE", or any other
 *  string for the default rounded-square shape.
 */
@Composable
fun StandardMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    dominantColors: DominantColors,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onTap: () -> Unit,
    userAlpha: Float = 0f,
    artworkShape: String = "ROUNDED_SQUARE",
    modifier: Modifier = Modifier,
) {
    val effectiveAlpha = 1f - userAlpha

    val artShape: Shape = when (artworkShape) {
        "CIRCLE", "VINYL" -> CircleShape
        "SQUARE" -> RectangleShape
        else -> RoundedCornerShape(8.dp)
    }

    val highResThumbnail = remember(song.thumbnailUrl) {
        ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl, size = 544)
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onTap),
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        dominantColors.primary.copy(alpha = effectiveAlpha),
                        dominantColors.secondary.copy(alpha = effectiveAlpha),
                    ),
                ),
            ),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(
                        thumbnailUrl = highResThumbnail,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(artShape),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = dominantColors.onBackground,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = dominantColors.onBackground.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onPlayPause, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = dominantColors.onBackground,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = dominantColors.onBackground,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    if (!isPlaying) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = dominantColors.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    trackColor = dominantColors.onBackground.copy(alpha = 0.2f),
                    color = dominantColors.accent,
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

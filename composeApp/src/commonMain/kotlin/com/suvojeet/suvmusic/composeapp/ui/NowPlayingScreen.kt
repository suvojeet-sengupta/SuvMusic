package com.suvojeet.suvmusic.composeapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.domain.player.MusicPlayer
import com.suvojeet.suvmusic.core.domain.player.RepeatMode

/**
 * Full-screen "now playing" view — replaces the normal app shell when
 * the user expands the bottom bar. Mirrors the structure of the Android
 * PlayerScreen but freshly written: no Coil (big gradient placeholder
 * for art), no lyrics panel yet, no queue panel yet. Controls bind
 * directly to [MusicPlayer].
 *
 * Future polish (lands as :app PlayerScreen migrates to commonMain):
 *  - Real album art via Coil 3
 *  - Lyrics tab
 *  - Queue panel
 *  - Sleep timer / equalizer / sponsor-block toggles
 */
@Composable
fun NowPlayingScreen(
    player: MusicPlayer,
    onCollapse: () -> Unit,
) {
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val shuffleEnabled by player.shuffleEnabled.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar — collapse button.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse player",
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            // Album art placeholder — gradient using theme colours, with
            // a music-note icon centered. Click toggles play/pause for
            // discoverability.
            ArtPlaceholder(
                onClick = { player.togglePlayPause() },
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )

            Spacer(Modifier.height(32.dp))

            // Track metadata.
            Text(
                text = currentSong?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 480.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = currentSong?.artist.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 480.dp),
            )

            Spacer(Modifier.height(24.dp))

            // Seek bar + time labels.
            Column(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                Slider(
                    value = positionMs.toFloat(),
                    onValueChange = { player.seekTo(it.toLong()) },
                    valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    enabled = currentSong != null && player.isAvailable,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatMs(positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMs(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Transport controls — shuffle | prev | play/pause | next | repeat.
            Row(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShuffleButton(
                    enabled = shuffleEnabled,
                    onToggle = { player.setShuffleEnabled(!shuffleEnabled) },
                )

                IconButton(
                    onClick = { player.previous() },
                    enabled = currentSong != null && player.isAvailable,
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(36.dp),
                    )
                }

                FilledIconButton(
                    onClick = { player.togglePlayPause() },
                    enabled = currentSong != null && player.isAvailable,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp),
                    )
                }

                IconButton(
                    onClick = { player.next() },
                    enabled = currentSong != null && player.isAvailable,
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp),
                    )
                }

                RepeatButton(
                    mode = repeatMode,
                    onToggle = {
                        val nextMode = when (repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                        player.setRepeatMode(nextMode)
                    },
                )
            }
        }
    }
}

@Composable
private fun ArtPlaceholder(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.7f),
                        secondary.copy(alpha = 0.6f),
                        tertiary.copy(alpha = 0.7f),
                    ),
                ),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxSize(0.4f),
        )
    }
}

@Composable
private fun ShuffleButton(enabled: Boolean, onToggle: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    IconButton(onClick = onToggle) {
        Icon(
            Icons.Filled.Shuffle,
            contentDescription = if (enabled) "Shuffle on" else "Shuffle off",
            tint = tint,
        )
    }
}

@Composable
private fun RepeatButton(mode: RepeatMode, onToggle: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (mode == RepeatMode.OFF) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        },
    )
    val icon = if (mode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
    IconButton(onClick = onToggle) {
        Icon(
            icon,
            contentDescription = "Repeat: $mode",
            tint = tint,
        )
    }
}

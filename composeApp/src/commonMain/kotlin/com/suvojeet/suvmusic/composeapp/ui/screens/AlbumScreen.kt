package com.suvojeet.suvmusic.composeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.image.DominantColors
import com.suvojeet.suvmusic.composeapp.image.defaultDominantColors
import com.suvojeet.suvmusic.composeapp.ui.AlbumArt
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Song

/**
 * Album detail screen — stateless port of the visual core of
 * `app/.../ui/screens/AlbumScreen.kt` (845 lines on Android).
 *
 * The Android original is wired through [AlbumViewModel] (Koin) which
 * fetches metadata, batches downloads, and owns scroll-aware header
 * collapsing. This commonMain shape is a stateless surface that takes
 * an [Album] in hand and emits taps. Callers (AndroidAlbumScreen vm-
 * backed wrapper, DesktopAlbumScreen direct usage) own the data flow.
 *
 * Visual design mirrors the Android original:
 *  - Tinted hero header (album art + meta), gradient background
 *  - Play All / Shuffle buttons in the metadata row
 *  - Tracks list with per-row index, title, artist, duration
 */
@Composable
fun AlbumScreen(
    album: Album,
    currentSongId: String? = null,
    dominantColors: DominantColors = defaultDominantColors(true),
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                AlbumHero(
                    album = album,
                    dominantColors = dominantColors,
                    onBackClick = onBackClick,
                    onPlayAll = { onPlayAll(album.songs) },
                    onShufflePlay = { onShufflePlay(album.songs) },
                )
            }

            if (album.songs.isEmpty()) {
                item { EmptyAlbumState() }
            } else {
                itemsIndexed(album.songs) { index, song ->
                    AlbumTrackRow(
                        index = index + 1,
                        song = song,
                        isCurrent = currentSongId == song.id,
                        onClick = { onSongClick(album.songs, index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumHero(
    album: Album,
    dominantColors: DominantColors,
    onBackClick: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.primary,
                        dominantColors.secondary,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Top bar — back button.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = dominantColors.onBackground,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Album",
                    style = MaterialTheme.typography.titleMedium,
                    color = dominantColors.onBackground,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(
                    thumbnailUrl = album.thumbnailUrl,
                    contentDescription = album.title,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )

                Spacer(Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = dominantColors.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = dominantColors.onBackground.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    album.year?.takeIf { it.isNotBlank() }?.let { year ->
                        Text(
                            text = year,
                            style = MaterialTheme.typography.bodyMedium,
                            color = dominantColors.onBackground.copy(alpha = 0.6f),
                        )
                    }
                    Text(
                        text = "${album.songs.size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dominantColors.onBackground.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dominantColors.accent,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play")
                }

                Button(
                    onClick = onShufflePlay,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dominantColors.onBackground.copy(alpha = 0.1f),
                        contentColor = dominantColors.onBackground,
                    ),
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AlbumTrackRow(
    index: Int,
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val highlight = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = highlight,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (song.duration > 0) {
            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyAlbumState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No tracks in this album yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return ""
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.SongActionsSheet
import com.suvojeet.suvmusic.ui.components.WaveformSeeker
import com.suvojeet.suvmusic.ui.components.rememberDominantColors

/**
 * Premium full-screen player with Apple Music-style design.
 * Features dynamic colors, quality badges, and queue view.
 */
@Composable
fun PlayerScreen(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onToggleLike: () -> Unit
) {
    val song = playerState.currentSong
    val context = LocalContext.current
    
    // Dynamic colors from album art
    val dominantColors = rememberDominantColors(song?.thumbnailUrl)
    
    // UI States
    var showQueue by remember { mutableStateOf(false) }
    var showActionsSheet by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    
    // High-res thumbnail
    val highResThumbnail = getHighResThumbnail(song?.thumbnailUrl)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.secondary,
                        dominantColors.primary,
                        Color.Black
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) onBack()
                }
            }
    ) {
        // Main Player Content
        AnimatedVisibility(
            visible = !showQueue,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                PlayerTopBar(
                    onBack = onBack,
                    onShowQueue = { showQueue = true },
                    dominantColors = dominantColors
                )
                
                Spacer(modifier = Modifier.weight(0.5f))
                
                // Album Art with shadow
                AlbumArtwork(
                    imageUrl = highResThumbnail,
                    title = song?.title,
                    dominantColors = dominantColors
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Song Info with actions
                SongInfoSection(
                    song = song,
                    isFavorite = isFavorite,
                    onFavoriteClick = onToggleLike,
                    onMoreClick = { showActionsSheet = true },
                    dominantColors = dominantColors
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Progress & Waveform
                WaveformSeeker(
                    progress = playerState.progress,
                    isPlaying = playerState.isPlaying,
                    onSeek = { progress ->
                        val newPosition = (progress * playerState.duration).toLong()
                        onSeekTo(newPosition)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Time labels with quality badge
                TimeLabelsWithQuality(
                    currentPosition = playerState.currentPosition,
                    duration = playerState.duration,
                    dominantColors = dominantColors
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Playback Controls
                PlaybackControls(
                    isPlaying = playerState.isPlaying,
                    shuffleEnabled = playerState.shuffleEnabled,
                    repeatMode = playerState.repeatMode,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onShuffleToggle = { /* TODO */ },
                    onRepeatToggle = { /* TODO */ },
                    dominantColors = dominantColors
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Actions
                BottomActions(
                    onLyricsClick = { /* TODO */ },
                    onCastClick = { /* TODO */ },
                    onQueueClick = { showQueue = true },
                    dominantColors = dominantColors
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Queue View
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            QueueView(
                currentSong = song,
                queue = playerState.queue,
                onBack = { showQueue = false },
                onSongClick = { /* Play from queue */ },
                dominantColors = dominantColors
            )
        }
        
        // Song Actions Bottom Sheet
        if (song != null) {
            SongActionsSheet(
                song = song,
                isVisible = showActionsSheet,
                onDismiss = { showActionsSheet = false },
                onToggleFavorite = onToggleLike,
                onDownload = onDownload
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    onBack: () -> Unit,
    onShowQueue: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )
        
        IconButton(onClick = onShowQueue) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = dominantColors.onBackground
            )
        }
    }
}

@Composable
private fun AlbumArtwork(
    imageUrl: String?,
    title: String?,
    dominantColors: DominantColors
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = dominantColors.primary.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .size(600)
                    .build(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SongInfoSection(
    song: Song?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
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
private fun TimeLabelsWithQuality(
    currentPosition: Long,
    duration: Long,
    dominantColors: DominantColors
) {
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
        
        // Quality Badge
        Surface(
            color = dominantColors.onBackground.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = dominantColors.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Lossless",
                    style = MaterialTheme.typography.labelSmall,
                    color = dominantColors.onBackground.copy(alpha = 0.8f)
                )
            }
        }
        
        Text(
            text = "-${formatDuration(duration - currentPosition)}",
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Previous / Rewind
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Previous",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(44.dp)
            )
        }
        
        // Play/Pause
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(dominantColors.onBackground)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = dominantColors.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        
        // Next / Forward
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Next",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(44.dp)
            )
        }
        
        // Repeat
        IconButton(
            onClick = onRepeatToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BottomActions(
    onLyricsClick: () -> Unit,
    onCastClick: () -> Unit,
    onQueueClick: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onLyricsClick) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics",
                tint = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }
        
        IconButton(onClick = onCastClick) {
            Icon(
                imageVector = Icons.Default.Cast,
                contentDescription = "Cast",
                tint = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }
        
        IconButton(onClick = onQueueClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun QueueView(
    currentSong: Song?,
    queue: List<Song>,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    dominantColors: DominantColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.secondary,
                        dominantColors.primary,
                        Color.Black
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Current song header
        if (currentSong != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentSong.thumbnailUrl,
                    contentDescription = currentSong.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = dominantColors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = dominantColors.onBackground.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                
                IconButton(onClick = { /* Favorite */ }) {
                    Icon(
                        imageVector = Icons.Default.StarOutline,
                        contentDescription = "Favorite",
                        tint = dominantColors.onBackground
                    )
                }
                
                IconButton(onClick = { /* More */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = dominantColors.onBackground
                    )
                }
            }
        }
        
        // Playback mode chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlaybackChip(
                text = "Shuffle",
                icon = Icons.Default.Shuffle,
                isSelected = false,
                dominantColors = dominantColors,
                onClick = { } 
            )
            PlaybackChip(
                text = "Repeat",
                icon = Icons.Default.Repeat,
                isSelected = false,
                dominantColors = dominantColors,
                onClick = { } 
            )
            PlaybackChip(
                text = "Autoplay",
                icon = Icons.Default.PlayArrow,
                isSelected = true,
                dominantColors = dominantColors,
                onClick = { } 
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Queue header
        Text(
            text = "Continue Playing",
            style = MaterialTheme.typography.titleMedium,
            color = dominantColors.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "Autoplaying similar music",
            style = MaterialTheme.typography.bodySmall,
            color = dominantColors.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        // Queue list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(queue) { index, song ->
                QueueItem(
                    song = song,
                    onClick = { onSongClick(song) },
                    dominantColors = dominantColors
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Close button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close queue",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun PlaybackChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) dominantColors.onBackground.copy(alpha = 0.2f) 
               else dominantColors.onBackground.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = dominantColors.onBackground,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = dominantColors.onBackground
            )
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    onClick: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = dominantColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = dominantColors.onBackground.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = "Drag",
            tint = dominantColors.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun getHighResThumbnail(url: String?): String? {
    return url?.let {
        when {
            it.contains("ytimg.com") -> it
                .replace("hqdefault", "maxresdefault")
                .replace("mqdefault", "maxresdefault")
                .replace("sddefault", "maxresdefault")
                .replace("default", "maxresdefault")
                .replace(Regex("w\\d+-h\\d+"), "w544-h544")
            it.contains("lh3.googleusercontent.com") -> 
                it.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
                  .replace(Regex("=s\\d+"), "=s544")
            else -> it
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
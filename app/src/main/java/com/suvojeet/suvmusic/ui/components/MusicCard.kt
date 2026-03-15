package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.ImageUtils
import com.suvojeet.suvmusic.util.dpadFocusable

/**
 * Beautiful music card with Material 3 Expressive design.
 */
@Composable
fun MusicCard(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    textColor: Color? = null,
    subTextColor: Color? = null
) {
    val context = LocalContext.current
    
    val defaultBackgroundColor = if (isPlaying) 
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    else 
        MaterialTheme.colorScheme.surfaceContainerHigh

    val cardBackgroundColor = backgroundColor ?: defaultBackgroundColor
    
    val highResThumbnail = remember(song.thumbnailUrl) {
        val url = song.thumbnailUrl ?: return@remember null
        
        // Handle local file paths
        if (url.startsWith("/") || url.startsWith("file://")) {
            val path = url.removePrefix("file://")
            val file = java.io.File(path)
            if (file.exists()) file else null
        } else {
            ImageUtils.getHighResThumbnailUrl(url, size = 544)
        }
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .dpadFocusable(onClick = onClick, shape = SquircleShape),
        color = cardBackgroundColor,
        shape = SquircleShape,
        tonalElevation = if (isPlaying) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (highResThumbnail != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(highResThumbnail)
                            .crossfade(true)
                            .size(128)
                            .build(),
                        contentDescription = song.title,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Playing indicator overlay
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NowPlayingAnimation(
                            color = Color.White,
                            isPlaying = true,
                            barCount = 3,
                            barWidth = 3.dp,
                            maxBarHeight = 20.dp,
                            minBarHeight = 8.dp
                        )
                    }
                } else if (song.isMembersOnly) {
                     Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                             imageVector = Icons.Default.Lock,
                             contentDescription = "Members Only",
                             modifier = Modifier.size(18.dp).padding(4.dp),
                             tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Song Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subTextColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // More options
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = subTextColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact music card for horizontal lists - M3E.
 */
@Composable
fun CompactMusicCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val highResThumbnail = remember(song.thumbnailUrl) {
        val url = song.thumbnailUrl ?: return@remember null
        
        if (url.startsWith("/") || url.startsWith("file://")) {
            val path = url.removePrefix("file://")
            val file = java.io.File(path)
            if (file.exists()) file else null
        } else {
            ImageUtils.getHighResThumbnailUrl(url, size = 544)
        }
    }
    
    Surface(
        modifier = modifier
            .width(140.dp)
            .dpadFocusable(onClick = onClick, shape = SquircleShape),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = SquircleShape,
        tonalElevation = 1.dp
    ) {
        Column {
            // Album Art
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (highResThumbnail != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(highResThumbnail)
                            .crossfade(true)
                            .size(280)
                            .build(),
                        contentDescription = song.title,
                        modifier = Modifier.size(140.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Info
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

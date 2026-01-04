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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.theme.AlbumArtShape
import com.suvojeet.suvmusic.ui.theme.GlassPurple
import com.suvojeet.suvmusic.ui.theme.MusicCardShape

/**
 * Get high-resolution thumbnail URL.
 */
private fun getHighResThumbnail(url: String?): String? {
    return url?.let {
        when {
            it.contains("ytimg.com") -> it
                .replace("hqdefault", "maxresdefault")
                .replace("mqdefault", "maxresdefault")
                .replace("sddefault", "maxresdefault")
                .replace("default", "maxresdefault")
                .replace(Regex("w\\d+-h\\d+"), "w226-h226")
            it.contains("lh3.googleusercontent.com") -> 
                it.replace(Regex("=w\\d+-h\\d+"), "=w226-h226")
                  .replace(Regex("=s\\d+"), "=s226")
            else -> it
        }
    }
}

/**
 * Beautiful music card with glassmorphism effect.
 * Used for displaying songs in lists and grids.
 */
@Composable
fun MusicCard(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "elevation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPlaying) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "backgroundColor"
    )
    
    val highResThumbnail = getHighResThumbnail(song.thumbnailUrl)
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, MusicCardShape)
            .clip(MusicCardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = backgroundColor,
        shape = MusicCardShape
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
                    .size(56.dp)
                    .clip(AlbumArtShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (highResThumbnail != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(highResThumbnail)
                            .crossfade(true)
                            .size(112) // 2x for high DPI
                            .build(),
                        contentDescription = song.title,
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Playing indicator overlay
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GlassPurple,
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Playing",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
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
                    color = if (isPlaying) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // More options
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact music card for horizontal lists.
 */
@Composable
fun CompactMusicCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val highResThumbnail = getHighResThumbnail(song.thumbnailUrl)
    
    Surface(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Album Art
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (highResThumbnail != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(highResThumbnail)
                            .crossfade(true)
                            .size(280) // 2x for high DPI
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

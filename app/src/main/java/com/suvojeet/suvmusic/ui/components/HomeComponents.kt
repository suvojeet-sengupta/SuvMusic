package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.util.ImageUtils
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.HomeItem
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.utils.SharedTransitionKeys
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert

@Composable
fun HomeItemCard(
    item: HomeItem,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    sectionItems: List<HomeItem>,
    onSongMoreClick: (Song) -> Unit = {}
) {
    when (item) {
        is HomeItem.SongItem -> {
            SquareSongCard(
                song = item.song,
                onClick = { 
                    // Extract all songs from the section for the queue
                    val songs = sectionItems.filterIsInstance<HomeItem.SongItem>().map { it.song }
                    val index = songs.indexOf(item.song)
                    if (index != -1) onSongClick(songs, index)
                },
                onMoreClick = { onSongMoreClick(item.song) },
                size = 170.dp
            )
        }
        is HomeItem.PlaylistItem -> {
            PlaylistDisplayCard(
                playlist = item.playlist,
                onClick = { onPlaylistClick(item.playlist) }
            )
        }
        is HomeItem.AlbumItem -> {
            // Albums get a thin "CD spine" sliver on the leading edge so they
            // read as physical media (album, not playlist) at a glance.
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 60.dp)
                        .width(5.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF2A2A2A),
                                    Color(0xFF1A1A1A),
                                    Color(0xFF2A2A2A)
                                )
                            )
                        )
                )
                PlaylistDisplayCard(
                    playlist = PlaylistDisplayItem(
                        id = item.album.id,
                        name = item.album.title,
                        url = "",
                        uploaderName = item.album.artist,
                        thumbnailUrl = item.album.thumbnailUrl
                    ),
                    onClick = {
                        onAlbumClick(item.album)
                    }
                )
            }
        }
        is HomeItem.ArtistItem -> {
            // Placeholder for Artist
        }
        is HomeItem.ExploreItem -> {
            // Explore items are handled by ExploreGridSection specifically
        }
    }
}

@Composable
fun PlaylistDisplayCard(
    playlist: PlaylistDisplayItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Get high-res thumbnail (replace w120 or similar with w544)
    val highResThumbnail = ImageUtils.getHighResThumbnailUrl(playlist.thumbnailUrl) ?: playlist.thumbnailUrl

    // YouTube-Music-style card: square artwork with the title/subtitle stacked
    // *below* the image rather than overlaid on it. The Expressive squircle clip
    // is the SuvMusic signature kept on top of the YTM layout.
    Column(
        modifier = Modifier
            .width(170.dp)
            .bounceClick(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(170.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = SquircleShape,
            tonalElevation = 2.dp
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(highResThumbnail)
                    .crossfade(true)
                    .size(544)  // Request high-res
                    .build(),
                contentDescription = playlist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = playlist.uploaderName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HomeSectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        } else if (onSeeAllClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "SEE ALL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable(onClick = onSeeAllClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}


fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "bounce"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

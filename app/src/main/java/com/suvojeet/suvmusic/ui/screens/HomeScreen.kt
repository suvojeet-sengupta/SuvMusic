package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.suvojeet.suvmusic.ui.theme.GlassPurple
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.HomeLoadingSkeleton
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel
import java.util.Calendar

/**
 * Home screen with dynamic sections fetched from YouTube Music.
 */
@Composable
fun HomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (com.suvojeet.suvmusic.data.model.Album) -> Unit,
    onRecentsClick: () -> Unit = {},
    onExploreClick: (String, String) -> Unit = { _, _ -> },
    onStartRadio: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.background
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Simple conditional rendering - no animations to prevent blank flashes
        when {
            // Show skeleton only when loading AND no data
            uiState.isLoading && uiState.homeSections.isEmpty() -> {
                HomeLoadingSkeleton()
            }
            // Show content if we have data (priority over loading state)
            uiState.homeSections.isNotEmpty() -> {
                @OptIn(ExperimentalMaterial3Api::class)
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Greeting Header
                    item {
                        ProfileHeader(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            onRecentsClick = onRecentsClick
                        )
                    }
                    
                    // For You - Personalized Recommendations
                    if (uiState.recommendations.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeSectionHeader(title = "For You")
                                
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.recommendations) { song ->
                                        MediumSongCard(
                                            song = song,
                                            onClick = {
                                                onSongClick(uiState.recommendations, uiState.recommendations.indexOf(song))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    items(uiState.homeSections) { section ->
                        when (section.type) {
                            com.suvojeet.suvmusic.data.model.HomeSectionType.LargeCardWithList -> {
                                com.suvojeet.suvmusic.ui.components.LargeCardWithListSection(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick
                                )
                            }
                            com.suvojeet.suvmusic.data.model.HomeSectionType.Grid -> {
                                com.suvojeet.suvmusic.ui.components.GridSection(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick
                                )
                            }
                            com.suvojeet.suvmusic.data.model.HomeSectionType.VerticalList -> {
                                com.suvojeet.suvmusic.ui.components.VerticalListSection(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick
                                )
                            }
                            com.suvojeet.suvmusic.data.model.HomeSectionType.HorizontalCarousel -> {
                                com.suvojeet.suvmusic.ui.components.HorizontalCarouselSection(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick
                                )
                            }
                            com.suvojeet.suvmusic.data.model.HomeSectionType.CommunityCarousel -> {
                                com.suvojeet.suvmusic.ui.components.CommunityCarouselSection(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick,
                                    onStartRadio = onStartRadio,
                                    onSavePlaylist = { playlist ->
                                        android.widget.Toast.makeText(context, "Saved ${playlist.name} to Library", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            com.suvojeet.suvmusic.data.model.HomeSectionType.ExploreGrid -> {
                                com.suvojeet.suvmusic.ui.components.ExploreGridSection(
                                    section = section,
                                    onExploreItemClick = onExploreClick
                                )
                            }
                        }
                    }

                    // App Footer
                    item {
                        AppFooter(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun HomeItemCard(
    item: HomeItem,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (com.suvojeet.suvmusic.data.model.Album) -> Unit,
    sectionItems: List<HomeItem>
) {
    when (item) {
        is HomeItem.SongItem -> {
            MediumSongCard(
                song = item.song,
                onClick = { 
                    // Extract all songs from the section for the queue
                    val songs = sectionItems.filterIsInstance<HomeItem.SongItem>().map { it.song }
                    val index = songs.indexOf(item.song)
                    if (index != -1) onSongClick(songs, index)
                }
            )
        }
        is HomeItem.PlaylistItem -> {
            PlaylistDisplayCard(
                playlist = item.playlist,
                onClick = { onPlaylistClick(item.playlist) }
            )
        }
        is HomeItem.AlbumItem -> {
            // Render album similar to playlist
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
        is HomeItem.ArtistItem -> {
            // Placeholder for Artist
        }
        is HomeItem.ExploreItem -> {
            // Handle ExploreItem
        }
    }
}

// -----------------------------------------------------------------------------
// Component Definitions
// -----------------------------------------------------------------------------

/**
 * Get greeting based on time of day
 */


private fun getGreeting(userName: String? = null): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeGreeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
    return if (!userName.isNullOrBlank()) "$timeGreeting, $userName" else timeGreeting
}

@Composable
private fun ProfileHeader(
    userName: String? = null,
    modifier: Modifier = Modifier,
    onRecentsClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Greeting Text with optional user name
        Text(
            text = getGreeting(userName),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Recents Button
        IconButton(onClick = onRecentsClick) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Recents",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistDisplayCard(
    playlist: PlaylistDisplayItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Get high-res thumbnail (replace w120 or similar with w544)
    val highResThumbnail = playlist.thumbnailUrl?.let { url ->
        url.replace(Regex("w\\d+-h\\d+"), "w544-h544")
            .replace(Regex("=w\\d+"), "=w544")
    } ?: playlist.thumbnailUrl
    
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
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
        
        // Gradient Overlay for text readability
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = Color.White
                )
                Text(
                    text = playlist.uploaderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MediumSongCard(
    song: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Get high-res thumbnail (replace w120 or similar with w544)
    val highResThumbnail = song.thumbnailUrl?.let { url ->
        url.replace(Regex("w\\d+-h\\d+"), "w544-h544")
            .replace(Regex("=w\\d+"), "=w544")
    } ?: song.thumbnailUrl
    
    Column(
        modifier = Modifier
            .width(160.dp)
            .bounceClick(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResThumbnail)
                .crossfade(true)
                .size(544)  // Request high-res
                .build(),
            contentDescription = song.title,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null
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
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Extensions
// -----------------------------------------------------------------------------

private fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
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

@Composable
private fun AppFooter(
    modifier: Modifier = Modifier
) {
    // Infinite animations for floating effect
    val infiniteTransition = rememberInfiniteTransition(label = "footer")
    
    // Bouncing music note animation
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = androidx.compose.animation.core.EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    // Heart pulse animation
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = androidx.compose.animation.core.EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeat"
    )
    
    // Subtle glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated Music Notes Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left music note
            Text(
                text = "🎵",
                fontSize = 28.sp,
                modifier = Modifier.offset(y = (-bounceOffset).dp)
            )
            
            // Center headphones
            Text(
                text = "🎧",
                fontSize = 40.sp,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = bounceOffset / 2
                    }
            )
            
            // Right music note (opposite phase)
            Text(
                text = "🎶",
                fontSize = 28.sp,
                modifier = Modifier.offset(y = bounceOffset.dp)
            )
        }
        
        // Tagline with gradient effect
        Text(
            text = "That's all for now!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        // Subtext
        Text(
            text = "Keep vibing, new music drops daily ✨",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Made with love section
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Developed with ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            // Animated heart
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Love",
                tint = Color(0xFFFF4081),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(14.dp)
                    .scale(heartScale)
            )
            
            Text(
                text = " from India 🇮🇳",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

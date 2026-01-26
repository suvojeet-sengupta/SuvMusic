package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.HomeLoadingSkeleton
import com.suvojeet.suvmusic.ui.theme.GlassPurple
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel
import java.util.Calendar

/**
 * Home screen with Spotify-style "Good Morning" grid and dynamic visuals.
 */
@Composable
fun HomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (com.suvojeet.suvmusic.data.model.Album) -> Unit,
    onRecentsClick: () -> Unit = {},
    onExploreClick: (String, String) -> Unit = { _, _ -> },
    onStartRadio: () -> Unit = {},
    onCreateMixClick: () -> Unit = {},
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


        // Content
        when {
            uiState.isLoading && uiState.homeSections.isEmpty() -> {
                HomeLoadingSkeleton()
            }
            uiState.homeSections.isNotEmpty() || uiState.recommendations.isNotEmpty() -> {
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
                        verticalArrangement = Arrangement.spacedBy(23.dp)
                    ) {
                        // Greeting & Profile Header
                        item {
                            ProfileHeader(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                                onRecentsClick = onRecentsClick
                            )
                        }

                        // Top "Quick Access" Grid (2x3) - Spotify Style
                        if (uiState.recommendations.isNotEmpty()) {
                            item {
                                QuickAccessGrid(
                                    items = uiState.recommendations.take(6),
                                    onItemClick = { song ->
                                        onSongClick(uiState.recommendations, uiState.recommendations.indexOf(song))
                                    }
                                )
                            }
                        }

                        // Sections Loop
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
                                    // Custom implementation for unified styling inside HomeScreen or component usage
                                    // We can just use the component or inline if we want to restyle strictly 
                                    // But typically HorizontalCarouselSection uses standard cards.
                                    // Let's ensure the cards used inside are "MediumSongCard" which we will restyle below.
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

                        // Create a Mix Section (Quick Access)
                        item {
                             // Spotify often has these functional cards in-between
                             Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                 HomeSectionHeader(title = "More specifically")
                                 Spacer(modifier = Modifier.height(12.dp))
                                 CreateMixCard(onClick = onCreateMixClick)
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

// -----------------------------------------------------------------------------
// New & Refactored Components
// -----------------------------------------------------------------------------

@Composable
fun QuickAccessGrid(
    items: List<Song>,
    onItemClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Create rows of 2
        val chunkedItems = items.chunked(2)
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { song ->
                    QuickAccessCard(
                        song = song,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp), // Fixed height for quick access
                        onClick = { onItemClick(song) }
                    )
                }
                // Fill empty space if odd number
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickAccessCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    // Darker surface for contrast
    Surface(
        modifier = modifier.bounceClick(onClick = onClick),
        shape = RoundedCornerShape(4.dp), // Slightly rounded like Spotify
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), 
        tonalElevation = 2.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.thumbnailUrl)
                    .crossfade(true)
                    .size(160)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp) // Match height
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )
            
            Text(
                text = song.title,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun ProfileHeader(
    modifier: Modifier = Modifier,
    onRecentsClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Greeting
        Text(
            text = getGreeting(),
            style = MaterialTheme.typography.headlineMedium, // Slightly larger
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Actions Row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Recents",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp).clickable(onClick = onRecentsClick)
            )
        }
    }
}

@Composable
private fun MediumSongCard(
    song: Song,
    onClick: () -> Unit
) {
    // Spotify Style: Square image, Title below, Artist below that
    val context = LocalContext.current
    val highResThumbnail = song.thumbnailUrl?.let { url ->
        url.replace(Regex("w\\d+-h\\d+"), "w544-h544")
            .replace(Regex("=w\\d+"), "=w544")
    } ?: song.thumbnailUrl

    Column(
        modifier = Modifier
            .width(150.dp) // Slightly tighter
            .bounceClick(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResThumbnail)
                .crossfade(true)
                .build(),
            contentDescription = song.title,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(4.dp)), // Minimal rounding often looks more modern/album-like
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium, // Use bodyMedium but Bold
            fontWeight = FontWeight.SemiBold, // Not too bold
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = song.artist,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistDisplayCard(
    playlist: PlaylistDisplayItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val highResThumbnail = playlist.thumbnailUrl?.let { url ->
        url.replace(Regex("w\\d+-h\\d+"), "w544-h544")
            .replace(Regex("=w\\d+"), "=w544")
    } ?: playlist.thumbnailUrl

    Column(
        modifier = Modifier
            .width(150.dp)
            .bounceClick(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResThumbnail)
                .crossfade(true)
                .size(544)
                .build(),
            contentDescription = playlist.name,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Playlist • ${playlist.uploaderName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------------------------------------------------------
// Existing Helpers & Footer (Refined)
// -----------------------------------------------------------------------------

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
        verticalAlignment = Alignment.Bottom // Align baseline
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        // Spotify often doesn't show "See All" prominently unless relevant, but we keep it
        if (onSeeAllClick != null) {
            Text(
                text = "MORE", // Cleaner text
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onSeeAllClick)
                    .padding(4.dp),
                 letterSpacing = 1.sp
            )
        }
    }
}

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
private fun CreateMixCard(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = "Create your own mix",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pick artists to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
private fun AppFooter(modifier: Modifier = Modifier) {
    // Kept the same animation logic, just ensuring it fits the new vibe
    val infiniteTransition = rememberInfiniteTransition(label = "footer")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1000, easing = androidx.compose.animation.core.EaseInOutSine), RepeatMode.Reverse),
        label = "bounce"
    )
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(600, easing = androidx.compose.animation.core.EaseInOutSine), RepeatMode.Reverse),
        label = "heartbeat"
    )

    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎵", fontSize = 28.sp, modifier = Modifier.offset(y = (-bounceOffset).dp))
            Text("🎧", fontSize = 40.sp, modifier = Modifier.graphicsLayer { rotationZ = bounceOffset / 2 })
            Text("🎶", fontSize = 28.sp, modifier = Modifier.offset(y = bounceOffset.dp))
        }
        Text("That's all for now!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        Text("Keep vibing, new music drops daily ✨", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Developed with ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Icon(Icons.Default.Favorite, "Love", tint = Color(0xFFFF4081), modifier = Modifier.padding(horizontal = 4.dp).size(14.dp).scale(heartScale))
            Text(" from India 🇮🇳", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

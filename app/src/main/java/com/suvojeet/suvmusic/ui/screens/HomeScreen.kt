package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import android.content.Intent
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
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.ui.components.HomeLoadingSkeleton
import com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.HomeEvent
import com.suvojeet.suvmusic.ui.theme.GlassPurple
import com.suvojeet.suvmusic.ui.utils.animateEnter
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel
import com.suvojeet.suvmusic.util.ImageUtils
import com.suvojeet.suvmusic.util.dpadFocusable
import androidx.compose.ui.graphics.Shape
import java.util.Calendar

/**
 * Home screen with Spotify-style "Good Morning" grid and dynamic visuals.
 */
@Composable
fun HomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onRecentsClick: () -> Unit = {},
    onListenTogetherClick: () -> Unit = {},
    onExploreClick: (String, String) -> Unit = { _, _ -> },
    onStartRadio: () -> Unit = {},
    onCreateMixClick: () -> Unit = {},
    currentSong: Song? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sessionManager = remember { com.suvojeet.suvmusic.data.SessionManager(context) }
    val animatedBackgroundEnabled by sessionManager.playerAnimatedBackgroundFlow.collectAsState(initial = true)
    
    // Song Menu State
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }
    
    // Handle Events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowAddToPlaylistSheet -> {
                    playlistViewModel.showAddToPlaylistSheet(event.song)
                }
            }
        }
    }
    
    // Dynamic Background Colors
    val dominantColors = com.suvojeet.suvmusic.ui.components.rememberDominantColors(
        imageUrl = currentSong?.thumbnailUrl ?: uiState.recommendations.firstOrNull()?.thumbnailUrl
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // fluid mesh gradient background
        if (animatedBackgroundEnabled) {
            com.suvojeet.suvmusic.ui.components.MeshGradientBackground(
                dominantColors = dominantColors
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

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
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        // Greeting & Profile Header
                        item(key = "header", contentType = "header") {
                            ProfileHeader(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                                    .animateEnter(index = 0),
                                onRecentsClick = onRecentsClick,
                                onListenTogetherClick = onListenTogetherClick
                            )
                        }

                        // Mood Chips Section
                        item(key = "mood_chips", contentType = "mood_chips") {
                            MoodChipsSection(
                                selectedMood = uiState.selectedMood,
                                onMoodSelected = viewModel::onMoodSelected,
                                modifier = Modifier.animateEnter(index = 1)
                            )
                        }

                        // Recommended Artists (Last.fm)
                        if (uiState.recommendedArtists.isNotEmpty()) {
                            item(key = "recommended_artists", contentType = "artists") {
                                RecommendedArtistsSection(
                                    artists = uiState.recommendedArtists,
                                    modifier = Modifier.animateEnter(index = 2)
                                )
                            }
                        }

                        // Recommended Tracks (Last.fm)
                        if (uiState.recommendedTracks.isNotEmpty()) {
                            item(key = "recommended_tracks", contentType = "tracks") {
                                RecommendedTracksSection(
                                    tracks = uiState.recommendedTracks,
                                    modifier = Modifier.animateEnter(index = 3)
                                )
                            }
                        }

                        // Quick Picks Section
                        if (uiState.recommendations.isNotEmpty()) {
                            item(key = "quick_picks", contentType = "section_quick_picks") {
                                com.suvojeet.suvmusic.ui.components.QuickPicksSection(
                                    section = com.suvojeet.suvmusic.data.model.HomeSection(
                                        title = "Quick picks",
                                        items = uiState.recommendations.map { com.suvojeet.suvmusic.data.model.HomeItem.SongItem(it) },
                                        type = com.suvojeet.suvmusic.data.model.HomeSectionType.QuickPicks
                                    ),
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick,
                                    onSongMoreClick = { song ->
                                        selectedSong = song
                                        showSongMenu = true
                                    },
                                    modifier = Modifier.animateEnter(index = 4)
                                )
                            }
                        }

                        // Sections Loop
                        itemsIndexed(
                            items = uiState.filteredSections,
                            key = { _, section -> section.title },
                            contentType = { _, section -> section.type }
                        ) { index, section ->
                            // Offset index by 5 for static items above
                            val enterModifier = Modifier.animateEnter(index = index + 5)
                            
                            when (section.type) {
                                com.suvojeet.suvmusic.data.model.HomeSectionType.LargeCardWithList -> {
                                    com.suvojeet.suvmusic.ui.components.LargeCardWithListSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = { song ->
                                            selectedSong = song
                                            showSongMenu = true
                                        },
                                        modifier = enterModifier,
                                    )
                                }
                                com.suvojeet.suvmusic.data.model.HomeSectionType.Grid -> {
                                    com.suvojeet.suvmusic.ui.components.GridSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = { song ->
                                            selectedSong = song
                                            showSongMenu = true
                                        },
                                        modifier = enterModifier,
                                    )
                                }
                                com.suvojeet.suvmusic.data.model.HomeSectionType.VerticalList -> {
                                    com.suvojeet.suvmusic.ui.components.VerticalListSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = { song ->
                                            selectedSong = song
                                            showSongMenu = true
                                        },
                                        modifier = enterModifier,
                                    )
                                }
                                com.suvojeet.suvmusic.data.model.HomeSectionType.HorizontalCarousel -> {
                                    com.suvojeet.suvmusic.ui.components.HorizontalCarouselSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = { song ->
                                            selectedSong = song
                                            showSongMenu = true
                                        },
                                        modifier = enterModifier,
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
                                        },
                                        onSongMoreClick = { song ->
                                            selectedSong = song
                                            showSongMenu = true
                                        },
                                        modifier = enterModifier
                                    )
                                }
                                com.suvojeet.suvmusic.data.model.HomeSectionType.QuickPicks -> {
                                    com.suvojeet.suvmusic.ui.components.QuickPicksSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = { song ->
                                            selectedSong = song
                                            showSongMenu = true
                                        },
                                        modifier = enterModifier,
                                    )
                                }
                                com.suvojeet.suvmusic.data.model.HomeSectionType.ExploreGrid -> {
                                    com.suvojeet.suvmusic.ui.components.ExploreGridSection(
                                        section = section,
                                        onExploreItemClick = onExploreClick,
                                        modifier = enterModifier
                                    )
                                }
                            }
                        }

                        // Create a Mix Section (Quick Access)
                        // Personalized Recommendation Sections (artist mixes, discovery, forgotten favorites, time-based)
                        if (uiState.personalizedSections.isNotEmpty()) {
                            itemsIndexed(
                                items = uiState.personalizedSections,
                                key = { _, section -> "personalized_${section.title}" },
                                contentType = { _, section -> "personalized_${section.type}" }
                            ) { index, section ->
                                val enterModifier = Modifier.animateEnter(index = index + uiState.filteredSections.size + 6)
                                
                                when (section.type) {
                                    com.suvojeet.suvmusic.data.model.HomeSectionType.QuickPicks -> {
                                        com.suvojeet.suvmusic.ui.components.QuickPicksSection(
                                            section = section,
                                            onSongClick = onSongClick,
                                            onPlaylistClick = onPlaylistClick,
                                            onAlbumClick = onAlbumClick,
                                            onSongMoreClick = { song ->
                                                selectedSong = song
                                                showSongMenu = true
                                            },
                                            modifier = enterModifier,
                                        )
                                    }
                                    else -> {
                                        com.suvojeet.suvmusic.ui.components.HorizontalCarouselSection(
                                            section = section,
                                            onSongClick = onSongClick,
                                            onPlaylistClick = onPlaylistClick,
                                            onAlbumClick = onAlbumClick,
                                            onSongMoreClick = { song ->
                                                selectedSong = song
                                                showSongMenu = true
                                            },
                                            modifier = enterModifier,
                                        )
                                    }
                                }
                            }
                        }

                        // Create a Mix Section (Quick Access)
                        item(key = "create_mix", contentType = "create_mix") {
                             // Spotify often has these functional cards in-between
                             Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                 HomeSectionHeader(title = "More specifically")
                                 Spacer(modifier = Modifier.height(12.dp))
                                 CreateMixCard(onClick = onCreateMixClick)
                             }
                        }

                        // App Footer
                        item(key = "footer", contentType = "footer") {
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
        
        // Song Options Menu
        selectedSong?.let { song ->
            SongMenuBottomSheet(
                isVisible = showSongMenu,
                onDismiss = { showSongMenu = false },
                song = song,
                onPlayNext = {
                    viewModel.playNext(song)
                    showSongMenu = false
                },
                onAddToQueue = {
                    viewModel.addToQueue(song)
                    showSongMenu = false
                },
                onAddToPlaylist = {
                    viewModel.addToPlaylist(song)
                    showSongMenu = false
                },
                onDownload = {
                    viewModel.downloadSong(song)
                    showSongMenu = false
                },
                onShare = {
                    val shareText = "🎵 ${song.title}\n🎤 ${song.artist}\n\nhttps://music.youtube.com/watch?v=${song.id}"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
                    showSongMenu = false
                }
            )
        }

        // Add to Playlist Sheet
        val playlistUiState by playlistViewModel.uiState.collectAsState()
        
        AddToPlaylistSheet(
            song = playlistUiState.selectedSong ?: Song("", "", "", "", 0L, null, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE),
            isVisible = playlistUiState.showAddToPlaylistSheet,
            playlists = playlistUiState.userPlaylists,
            isLoading = playlistUiState.isLoadingPlaylists,
            onDismiss = { playlistViewModel.hideAddToPlaylistSheet() },
            onAddToPlaylist = { playlistId ->
                playlistViewModel.addSongToPlaylist(playlistId)
            },
            onCreateNewPlaylist = {
                playlistViewModel.showCreatePlaylistDialog()
            }
        )
    }
}

// -----------------------------------------------------------------------------
// New & Refactored Components
// -----------------------------------------------------------------------------

@Composable
fun QuickAccessGrid(
    items: List<Song>,
    modifier: Modifier = Modifier,
    onItemClick: (Song) -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
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
    val highResThumbnail = ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl) ?: song.thumbnailUrl
    
    // Darker surface for contrast
    Surface(
        modifier = modifier.bounceClick(
            shape = RoundedCornerShape(4.dp),
            onClick = onClick
        ),
        shape = RoundedCornerShape(4.dp), // Slightly rounded like Spotify
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), 
        tonalElevation = 2.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(highResThumbnail)
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
    onRecentsClick: () -> Unit = {},
    onListenTogetherClick: () -> Unit = {}
) {
    val greeting = remember { getGreeting() }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Greeting
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onBackground,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )
            ),
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )

        // Actions Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onListenTogetherClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Listen Together",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onRecentsClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Recents",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Unused local cards removed for cleanliness

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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
        )
        if (onSeeAllClick != null) {
            Text(
                text = "SEE ALL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ), 
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = "Create your own mix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pick artists to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    shape: Shape = RoundedCornerShape(8.dp),
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .dpadFocusable(
            shape = shape,
            focusedScale = 1.05f,
            borderColor = MaterialTheme.colorScheme.primary
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

@Composable
private fun AppFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 32.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simple elegant logo/icon
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = com.suvojeet.suvmusic.R.drawable.logo,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SuvMusic",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp
        )
        
        val versionName = com.suvojeet.suvmusic.BuildConfig.VERSION_NAME
        Text(
            text = "v$versionName",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Professional attribution
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CRAFTED WITH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                letterSpacing = 1.5.sp
            )
            Icon(
                imageVector = Icons.Default.Favorite, 
                contentDescription = null, 
                tint = Color(0xFFFF4081).copy(alpha = 0.7f), 
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(12.dp)
            )
            Text(
                text = "IN INDIA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                letterSpacing = 1.5.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "© 2026 Suvojeet Sengupta",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun RecommendedArtistsSection(
    artists: List<com.suvojeet.suvmusic.lastfm.RecommendedArtist>,
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit = {}
) {
    if (artists.isEmpty()) return

    Column(modifier = modifier.padding(vertical = 16.dp)) {
        HomeSectionHeader(title = "Recommended Artists")
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists) { artist ->
                ArtistCard(artist = artist, onClick = { onArtistClick(artist.name) })
            }
        }
    }
}

@Composable
fun RecommendedTracksSection(
    tracks: List<com.suvojeet.suvmusic.lastfm.RecommendedTrack>,
    modifier: Modifier = Modifier,
    onTrackClick: (com.suvojeet.suvmusic.lastfm.RecommendedTrack) -> Unit = {}
) {
    if (tracks.isEmpty()) return

    Column(modifier = modifier.padding(vertical = 16.dp)) {
        HomeSectionHeader(title = "Recommended Tracks")
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tracks) { track ->
                TrackCard(track = track, onClick = { onTrackClick(track) })
            }
        }
    }
}

@Composable
fun ArtistCard(
    artist: com.suvojeet.suvmusic.lastfm.RecommendedArtist,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    // Get the largest image
    val imageUrl = artist.image.lastOrNull()?.url ?: ""

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .bounceClick(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = artist.name,
            modifier = Modifier
                .size(100.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TrackCard(
    track: com.suvojeet.suvmusic.lastfm.RecommendedTrack,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = track.image.lastOrNull()?.url ?: ""

    Column(
        modifier = Modifier
            .width(140.dp)
            .bounceClick(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = track.name,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = track.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = track.artist.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


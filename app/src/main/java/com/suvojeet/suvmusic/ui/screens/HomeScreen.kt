package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
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
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.data.model.HomeSectionType
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.ui.components.HomeLoadingSkeleton
import com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.HomeEvent
import com.suvojeet.suvmusic.ui.utils.animateEnter
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel
import com.suvojeet.suvmusic.util.ImageUtils
import com.suvojeet.suvmusic.util.dpadFocusable
import com.suvojeet.suvmusic.ui.theme.QuickAccessShape
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.theme.PillShape
import androidx.compose.ui.graphics.Shape
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

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

    // Stable callback reference — avoids creating new lambdas per section item
    val onSongMoreClickHandler = remember {
        { song: Song ->
            selectedSong = song
            showSongMenu = true
        }
    }
    
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
            uiState.error != null && uiState.homeSections.isEmpty() && uiState.recommendations.isEmpty() -> {
                // Full-screen error state
                ErrorState(
                    message = uiState.error ?: "Something went wrong",
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize().statusBarsPadding()
                )
            }
            uiState.homeSections.isNotEmpty() || uiState.recommendations.isNotEmpty() -> {
                val lazyListState = rememberLazyListState()

                // Infinite scroll detection — trigger slightly earlier for seamless loading
                LaunchedEffect(lazyListState, uiState.isLoadingMore) {
                    snapshotFlow {
                        val layoutInfo = lazyListState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        // Trigger when 8 items from bottom and NOT already loading
                        lastVisibleIndex >= totalItems - 8 && totalItems > 0 && !uiState.isLoadingMore
                    }
                        .distinctUntilChanged()
                        .filter { it }
                        .collectLatest {
                            viewModel.loadMore()
                        }
                }

                val state = rememberPullToRefreshState()

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    indicator = {
                        PullToRefreshDefaults.LoadingIndicator(
                            state = state,
                            isRefreshing = uiState.isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
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

                        // Personalized "For You" Banner — shown when logged in
                        if (uiState.isLoggedIn) {
                            item(key = "for_you_banner", contentType = "for_you_banner") {
                                AnimatedVisibility(
                                    visible = uiState.isForYouBannerVisible,
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    ForYouBanner(
                                        onStartRadio = onStartRadio,
                                        onDismiss = viewModel::onDismissForYouBanner,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .animateEnter(index = 2)
                                    )
                                }
                            }
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
                                    section = HomeSection(
                                        title = "Quick picks",
                                        items = uiState.recommendations.map { HomeItem.SongItem(it) },
                                        type = HomeSectionType.QuickPicks
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
                            val enterModifier = Modifier.animateEnter(index = index)
                            
                            when (section.type) {
                                HomeSectionType.LargeCardWithList -> {
                                    com.suvojeet.suvmusic.ui.components.LargeCardWithListSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                        modifier = enterModifier,
                                    )
                                }
                                HomeSectionType.Grid -> {
                                    com.suvojeet.suvmusic.ui.components.GridSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                        modifier = enterModifier,
                                    )
                                }
                                HomeSectionType.VerticalList -> {
                                    com.suvojeet.suvmusic.ui.components.VerticalListSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                        modifier = enterModifier,
                                    )
                                }
                                HomeSectionType.HorizontalCarousel -> {
                                    com.suvojeet.suvmusic.ui.components.HorizontalCarouselSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                        modifier = enterModifier,
                                    )
                                }
                                HomeSectionType.CommunityCarousel -> {
                                    com.suvojeet.suvmusic.ui.components.CommunityCarouselSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onStartRadio = onStartRadio,
                                        onSavePlaylist = { playlist ->
                                            android.widget.Toast.makeText(context, "Saved ${playlist.name} to Library", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        onSongMoreClick = onSongMoreClickHandler,
                                        modifier = enterModifier
                                    )
                                }
                                HomeSectionType.QuickPicks -> {
                                    com.suvojeet.suvmusic.ui.components.QuickPicksSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                        modifier = enterModifier,
                                    )
                                }
                                HomeSectionType.ExploreGrid -> {
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
                            // Personalized section header with sparkle
                            item(key = "personalized_header", contentType = "personalized_header") {
                                PersonalizedSectionHeader(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .animateEnter(index = 0)
                                )
                            }

                            itemsIndexed(
                                items = uiState.personalizedSections,
                                key = { _, section -> "personalized_${section.title}" },
                                contentType = { _, section -> "personalized_${section.type}" }
                            ) { index, section ->
                                val enterModifier = Modifier.animateEnter(index = index)
                                
                                when (section.type) {
                                    HomeSectionType.QuickPicks -> {
                                        com.suvojeet.suvmusic.ui.components.QuickPicksSection(
                                            section = section,
                                            onSongClick = onSongClick,
                                            onPlaylistClick = onPlaylistClick,
                                            onAlbumClick = onAlbumClick,
                                            onSongMoreClick = onSongMoreClickHandler,
                                            modifier = enterModifier,
                                        )
                                    }
                                    else -> {
                                        com.suvojeet.suvmusic.ui.components.HorizontalCarouselSection(
                                            section = section,
                                            onSongClick = onSongClick,
                                            onPlaylistClick = onPlaylistClick,
                                            onAlbumClick = onAlbumClick,
                                            onSongMoreClick = onSongMoreClickHandler,
                                            modifier = enterModifier,
                                        )
                                    }
                                }
                            }
                        }

                        // Genre-Based Discovery Sections ("Because you like Pop", "Your R&B Mix", etc.)
                        if (uiState.genreSections.isNotEmpty()) {
                            item(key = "genre_header", contentType = "genre_header") {
                                SectionDividerHeader(
                                    title = "Your Genres",
                                    subtitle = "Because of your taste",
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .animateEnter(index = 0)
                                )
                            }

                            itemsIndexed(
                                items = uiState.genreSections,
                                key = { _, section -> "genre_${section.title}" },
                                contentType = { _, section -> "genre_${section.type}" }
                            ) { index, section ->
                                val enterModifier = Modifier.animateEnter(index = index)
                                com.suvojeet.suvmusic.ui.components.HorizontalCarouselSection(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick,
                                    onSongMoreClick = onSongMoreClickHandler,
                                    modifier = enterModifier,
                                )
                            }
                        }

                        // Context-Aware Sections (time-of-day, listening patterns)
                        if (uiState.contextSections.isNotEmpty()) {
                            itemsIndexed(
                                items = uiState.contextSections,
                                key = { _, section -> "context_${section.title}" },
                                contentType = { _, section -> "context_${section.type}" }
                            ) { index, section ->
                                val enterModifier = Modifier.animateEnter(index = index)
                                com.suvojeet.suvmusic.ui.components.HorizontalCarouselSection(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick,
                                    onSongMoreClick = onSongMoreClickHandler,
                                    modifier = enterModifier,
                                )
                            }
                        }

                        // Detected Mood Banner
                        uiState.detectedMood?.let { mood ->
                            if (uiState.selectedMood == null) {
                                item(key = "mood_banner", contentType = "mood_banner") {
                                    DetectedMoodBanner(
                                        mood = mood,
                                        onExplore = { viewModel.onMoodSelected(mood) },
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .animateEnter(index = 12)
                                    )
                                }
                            }
                        }

                        // Create a Mix Section (Quick Access)
                        item(key = "create_mix", contentType = "create_mix") {
                             Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                 HomeSectionHeader(title = "More specifically")
                                 Spacer(modifier = Modifier.height(12.dp))
                                 CreateMixCard(onClick = onCreateMixClick)
                             }
                        }

                        // ──────────────────────────────────────────────────
                        // "More for you" — Scroll-loaded sections (varied styles)
                        // ──────────────────────────────────────────────────
                        if (uiState.moreSections.isNotEmpty()) {
                            itemsIndexed(
                                items = uiState.moreSections,
                                key = { _, section -> "more_${section.title}" },
                                contentType = { _, section -> "more_${section.type}" }
                            ) { index, section ->
                                val enterModifier = Modifier.animateEnter(index = index)
                                RenderHomeSection(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick,
                                    onExploreClick = onExploreClick,
                                    onStartRadio = onStartRadio,
                                    onSongMoreClick = onSongMoreClickHandler,
                                    modifier = enterModifier
                                )
                            }
                        }

                        // Loading More Indicator
                        if (uiState.isLoadingMore) {
                            item(key = "loading_more", contentType = "loading_more") {
                                LoadingMoreIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp)
                                )
                            }
                        }

                        // End-of-Feed or App Footer
                        if (uiState.hasReachedEnd) {
                            item(key = "end_of_feed", contentType = "end_of_feed") {
                                EndOfFeedCard(
                                    onStartRadio = onStartRadio,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
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
                },
                onListenTogether = onListenTogetherClick
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
    val highResThumbnail = remember(song.thumbnailUrl) {
        ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl, size = 544)
    }
    
    // Darker surface for contrast
    Surface(
        modifier = modifier.bounceClick(
            shape = QuickAccessShape,
            onClick = onClick
        ),
        shape = QuickAccessShape, 
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
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp
        )

        // Actions Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
// Unified Section Renderer — dispatches by HomeSectionType
// -----------------------------------------------------------------------------

/**
 * Render any [HomeSection] by dispatching to the correct composable based on its [HomeSectionType].
 * Used for moreSections to avoid copy-pasting the when-block.
 */
@Composable
private fun RenderHomeSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onExploreClick: (String, String) -> Unit,
    onStartRadio: () -> Unit,
    onSongMoreClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    when (section.type) {
        HomeSectionType.QuickPicks -> {
            com.suvojeet.suvmusic.ui.components.QuickPicksSection(
                section = section,
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onSongMoreClick = onSongMoreClick,
                modifier = modifier,
            )
        }
        HomeSectionType.LargeCardWithList -> {
            com.suvojeet.suvmusic.ui.components.LargeCardWithListSection(
                section = section,
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onSongMoreClick = onSongMoreClick,
                modifier = modifier,
            )
        }
        HomeSectionType.Grid -> {
            com.suvojeet.suvmusic.ui.components.GridSection(
                section = section,
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onSongMoreClick = onSongMoreClick,
                modifier = modifier,
            )
        }
        HomeSectionType.VerticalList -> {
            com.suvojeet.suvmusic.ui.components.VerticalListSection(
                section = section,
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onSongMoreClick = onSongMoreClick,
                modifier = modifier,
            )
        }
        HomeSectionType.CommunityCarousel -> {
            com.suvojeet.suvmusic.ui.components.CommunityCarouselSection(
                section = section,
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onStartRadio = onStartRadio,
                onSongMoreClick = onSongMoreClick,
                modifier = modifier,
            )
        }
        HomeSectionType.ExploreGrid -> {
            com.suvojeet.suvmusic.ui.components.ExploreGridSection(
                section = section,
                onExploreItemClick = onExploreClick,
                modifier = modifier
            )
        }
        else -> {
            com.suvojeet.suvmusic.ui.components.HorizontalCarouselSection(
                section = section,
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onSongMoreClick = onSongMoreClick,
                modifier = modifier,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// End-of-Feed Card — unique ending element
// -----------------------------------------------------------------------------

/**
 * A visually distinctive "End of Feed" card shown when the user has scrolled
 * through all content. Offers a personal radio as the next action.
 */
@Composable
private fun EndOfFeedCard(
    onStartRadio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        SquircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "You've explored it all",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Start a personal radio for endless music tailored to you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.bounceClick(
                    shape = PillShape,
                    onClick = onStartRadio
                ),
                shape = PillShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Start Your Radio",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Error State & Loading
// -----------------------------------------------------------------------------

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Couldn't load content",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoadingMoreIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LoadingIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading more for you...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Detected mood banner — shows when the engine auto-detects the user's current mood.
 */
@Composable
private fun DetectedMoodBanner(
    mood: String,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExplore)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Feeling $mood?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to explore this vibe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Section divider header with title and subtitle — used between major recommendation categories.
 */
@Composable
private fun SectionDividerHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(2.dp)
                    )
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.3).sp
        )
        if (onSeeAllClick != null) {
            Text(
                text = "SEE ALL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                letterSpacing = 0.5.sp
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
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column {
            Text(
                text = "Create your own mix",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pick artists to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            clip = true
            this.shape = shape
        }
        .dpadFocusable(
            shape = shape,
            focusedScale = 1.05f,
            borderColor = MaterialTheme.colorScheme.primary
        )
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        isPressed = false
                    }
                },
                onTap = { onClick() }
            )
        }
}

@Composable
private fun AppFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 32.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = com.suvojeet.suvmusic.R.drawable.logo,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SuvMusic",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        val versionName = com.suvojeet.suvmusic.BuildConfig.VERSION_NAME
        Text(
            text = "v$versionName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "\u00A9 2026 Suvojeet Sengupta",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

/**
 * "For You" banner — a visually distinct personalized section shown when logged in.
 * Provides quick access to personalized radio and shows the user that content is tailored.
 */
@Composable
private fun ForYouBanner(
    onStartRadio: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Made for you",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Based on your listening history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    modifier = Modifier
                        .bounceClick(
                            shape = RoundedCornerShape(20.dp),
                            onClick = onStartRadio
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Radio",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Close button in top-right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Personalized section header with sparkle icon, visually separates
 * AI-powered recommendations from standard YouTube home sections.
 */
@Composable
private fun PersonalizedSectionHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = "Personalized for you",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.3).sp
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
            val uniqueArtists = artists.distinctBy { it.name }
            items(
                items = uniqueArtists,
                key = { it.name }
            ) { artist ->
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
            val uniqueTracks = tracks.distinctBy { it.name }
            items(
                items = uniqueTracks,
                key = { it.name }
            ) { track ->
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
            .width(110.dp)
            .bounceClick(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = artist.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
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
            .width(150.dp)
            .bounceClick(onClick = onClick)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = track.name,
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        }
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


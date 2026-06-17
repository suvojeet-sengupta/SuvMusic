package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
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
import androidx.compose.ui.draw.drawBehind
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
import org.koin.compose.viewmodel.koinViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.core.model.HomeItem
import com.suvojeet.suvmusic.core.model.HomeSection
import com.suvojeet.suvmusic.core.model.HomeSectionType
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
    onHistoryClick: () -> Unit = {},
    onListenTogetherClick: () -> Unit = {},
    onExploreClick: (String, String) -> Unit = { _, _ -> },
    onStartRadio: () -> Unit = {},
    onCreateMixClick: () -> Unit = {},
    currentSong: Song? = null,
    viewModel: HomeViewModel = koinViewModel(),
    playlistViewModel: PlaylistManagementViewModel = koinViewModel()
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
    
    val playlistMgmtState by playlistViewModel.uiState.collectAsState()
    val isAlbumArtDynamicColorsEnabled by sessionManager.albumArtDynamicColorsEnabledFlow.collectAsState(initial = true)
    
    val lazyListState = rememberLazyListState()

    // Handle Events — collected lifecycle-aware so UI actions (sheets, scroll)
    // aren't dispatched against a paused/destroyed screen.
    val homeLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(homeLifecycleOwner) {
        homeLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is HomeEvent.ShowAddToPlaylistSheet -> {
                        playlistViewModel.showAddToPlaylistSheet(event.song)
                    }
                    is HomeEvent.ScrollToTop -> {
                        if (uiState.homeSections.isNotEmpty() || uiState.recommendations.isNotEmpty()) {
                            lazyListState.animateScrollToItem(0)
                        }
                    }
                    is HomeEvent.Refresh -> {
                       // Refresh is already triggered in ViewModel, which sets isRefreshing = true
                    }
                }
            }
        }
    }

    // Handle messages from PlaylistManagement
    androidx.compose.runtime.LaunchedEffect(playlistMgmtState.successMessage, playlistMgmtState.errorMessage) {
        playlistMgmtState.successMessage?.let {
            com.suvojeet.suvmusic.util.SnackbarUtil.showMessage(it)
            playlistViewModel.clearMessages()
        }
        playlistMgmtState.errorMessage?.let {
            com.suvojeet.suvmusic.util.SnackbarUtil.showError(it)
            playlistViewModel.clearMessages()
        }
    }

    // Dynamic Background Colors - Respect user preference for dynamic colors
    val actualDominantColors = com.suvojeet.suvmusic.ui.components.rememberDominantColors(
        imageUrl = currentSong?.thumbnailUrl ?: uiState.recommendations.firstOrNull()?.thumbnailUrl
    )
    
    val dominantColors = if (isAlbumArtDynamicColorsEnabled) {
        actualDominantColors
    } else {
        // Fallback to Material You theme colors when dynamic colors are disabled
        com.suvojeet.suvmusic.ui.components.DominantColors(
            primary = MaterialTheme.colorScheme.primaryContainer,
            secondary = MaterialTheme.colorScheme.secondaryContainer,
            accent = MaterialTheme.colorScheme.tertiaryContainer,
            onBackground = MaterialTheme.colorScheme.onBackground
        )
    }

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
                        if (uiState.homeSectionsVisibility.contains("greeting")) {
                            item(key = "header", contentType = "header") {
                                val glassCfg = com.suvojeet.suvmusic.ui.components.glass.rememberLiquidGlassConfig()
                                if (glassCfg.enabled) {
                                    // Frosted top bar to match the Liquid Glass nav bar.
                                    com.suvojeet.suvmusic.ui.components.glass.GlassCard(
                                        modifier = Modifier
                                            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                                            .animateEnter(index = 0),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                                    ) {
                                        ProfileHeader(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            avatarUrl = uiState.userAvatarUrl,
                                            onHistoryClick = onHistoryClick,
                                            onListenTogetherClick = onListenTogetherClick
                                        )
                                    }
                                } else {
                                    ProfileHeader(
                                        modifier = Modifier
                                            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 4.dp)
                                            .animateEnter(index = 0),
                                        avatarUrl = uiState.userAvatarUrl,
                                        onHistoryClick = onHistoryClick,
                                        onListenTogetherClick = onListenTogetherClick
                                    )
                                }
                            }
                        }

                        // Mood Chips Section
                        if (uiState.homeSectionsVisibility.contains("mood_chips")) {
                            item(key = "mood_chips", contentType = "mood_chips") {
                                MoodChipsSection(
                                    selectedMood = uiState.selectedMood,
                                    onMoodSelected = viewModel::onMoodSelected,
                                    modifier = Modifier.animateEnter(index = 1)
                                )
                            }
                        }

                        // Personalized "For You" Banner — shown when logged in
                        if (uiState.isLoggedIn && uiState.homeSectionsVisibility.contains("for_you_banner")) {
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
                        if (uiState.recommendedArtists.isNotEmpty() && uiState.homeSectionsVisibility.contains("recommendations")) {
                            item(key = "recommended_artists", contentType = "artists") {
                                RecommendedArtistsSection(
                                    artists = uiState.recommendedArtists,
                                    modifier = Modifier.animateEnter(index = 2)
                                )
                            }
                        }

                        // Recommended Tracks (Last.fm)
                        if (uiState.recommendedTracks.isNotEmpty() && uiState.homeSectionsVisibility.contains("recommendations")) {
                            item(key = "recommended_tracks", contentType = "tracks") {
                                RecommendedTracksSection(
                                    tracks = uiState.recommendedTracks,
                                    modifier = Modifier.animateEnter(index = 3)
                                )
                            }
                        }

                        // Speed dial — YouTube-Music quick-access grid of top picks
                        if (uiState.recommendations.isNotEmpty() && uiState.homeSectionsVisibility.contains("quick_picks")) {
                            item(key = "speed_dial", contentType = "speed_dial") {
                                SpeedDialGrid(
                                    songs = uiState.recommendations,
                                    currentSong = currentSong,
                                    userName = uiState.userName,
                                    avatarUrl = uiState.userAvatarUrl,
                                    onSongClick = onSongClick,
                                    onShuffleClick = viewModel::playRandomMix,
                                    modifier = Modifier.animateEnter(index = 4)
                                )
                            }
                        }

                        // Sections Loop
                        if (uiState.homeSectionsVisibility.contains("youtube_sections")) {
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
                                            com.suvojeet.suvmusic.util.SnackbarUtil.showSuccess("Saved ${playlist.name} to Library")
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
                    }

                    // Personalized Recommendation Sections (artist mixes, discovery, forgotten favorites, time-based)
                        if (uiState.personalizedSections.isNotEmpty() && uiState.homeSectionsVisibility.contains("personalized")) {
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
                                        // Personalized rows get the vinyl-disc treatment so
                                        // they read distinctly from regular YouTube carousels.
                                        com.suvojeet.suvmusic.ui.components.PersonalizedMixCarousel(
                                            section = section,
                                            onSongClick = onSongClick,
                                            onPlaylistClick = onPlaylistClick,
                                            onAlbumClick = onAlbumClick,
                                            modifier = enterModifier,
                                        )
                                    }
                                }
                            }
                        }

                        // Genre-Based Discovery Sections ("Because you like Pop", "Your R&B Mix", etc.)
                        if (uiState.genreSections.isNotEmpty() && uiState.homeSectionsVisibility.contains("genres")) {
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
                                // Genre rows get a tinted ring + tag-chip header so each
                                // "Because you like X" feels visually labeled by genre.
                                com.suvojeet.suvmusic.ui.components.GenreCarousel(
                                    section = section,
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick,
                                    modifier = enterModifier,
                                )
                            }
                        }

                        // Context-Aware Sections (time-of-day, listening patterns)
                        if (uiState.contextSections.isNotEmpty() && uiState.homeSectionsVisibility.contains("contextual")) {
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
                            if (uiState.selectedMood == null && uiState.homeSectionsVisibility.contains("mood_banner")) {
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
                        if (uiState.homeSectionsVisibility.contains("create_mix")) {
                            item(key = "create_mix", contentType = "create_mix") {
                                 Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                     HomeSectionHeader(title = "More specifically")
                                     Spacer(modifier = Modifier.height(12.dp))
                                     CreateMixCard(onClick = onCreateMixClick)
                                 }
                            }
                        }

                        // ──────────────────────────────────────────────────
                        // "More for you" — Scroll-loaded sections (varied styles)
                        // ──────────────────────────────────────────────────
                        if (uiState.moreSections.isNotEmpty() && uiState.homeSectionsVisibility.contains("youtube_sections")) {
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
                isCurrentlyPlaying = song.id == currentSong?.id,
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
        
        if (playlistUiState.showAddToPlaylistSheet && playlistUiState.selectedSongs.isNotEmpty()) {
            AddToPlaylistSheet(
                songs = playlistUiState.selectedSongs,
                isVisible = true,
                playlists = playlistUiState.userPlaylists,
                isLoading = playlistUiState.isLoadingPlaylists || playlistUiState.isAddingSong,
                onDismiss = { playlistViewModel.hideAddToPlaylistSheet() },
                onAddToPlaylist = { playlistId ->
                    playlistViewModel.addSongsToPlaylist(playlistId)
                },
                onCreateNewPlaylist = {
                    playlistViewModel.showCreatePlaylistDialog()
                }
            )
        }
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


/**
 * Time-aware gradient hero banner. Replaces the plain greeting Row with a
 * full-width band tinted by hour-of-day (dawn/day/dusk/night) so the top of
 * Home doesn't blend into the rest of the feed.
 */
@Composable
private fun ProfileHeader(
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    onHistoryClick: () -> Unit = {},
    onListenTogetherClick: () -> Unit = {}
) {
    // YouTube-Music-style compact top bar: brand wordmark on the leading edge,
    // a row of round actions + account avatar on the trailing edge. SuvMusic
    // keeps its own logo + wordmark and theme accent instead of YTM's red.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.suvojeet.suvmusic.ui.components.AppLogo(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Text(
                text = "SuvMusic",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Listen Together — a SuvMusic-specific action kept in the bar.
            TopBarAction(
                icon = Icons.Default.Group,
                contentDescription = "Listen Together",
                onClick = onListenTogetherClick
            )
            // Activity / history — mirrors YTM's notification bell slot.
            TopBarAction(
                icon = Icons.Default.History,
                contentDescription = "Recent activity",
                onClick = onHistoryClick,
                showDot = true
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Account avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
                    .clickable(onClick = onHistoryClick),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Account",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * A round, borderless top-bar action button in the YouTube-Music idiom, with an
 * optional accent dot to signal pending activity (SuvMusic flourish).
 */
@Composable
private fun TopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    showDot: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(23.dp)
        )
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 9.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * YouTube-Music "Speed dial" — a compact 3-column grid of the user's top picks
 * with the artwork title overlaid. The currently-playing tile gets a white ring,
 * exactly like YTM. SuvMusic keeps its squircle-ish 14dp tiles + bounce press.
 */
@Composable
private fun SpeedDialGrid(
    songs: List<Song>,
    currentSong: Song?,
    userName: String?,
    avatarUrl: String?,
    onSongClick: (List<Song>, Int) -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    // 8 picks + a permanent "shuffle" tile in the last cell (3x3 grid), mirroring
    // YouTube Music's speed dial where the trailing tile shuffles your mix.
    val items = remember(songs) { songs.take(8) }
    // Cell indices: 0..items.lastIndex are songs, the final index is the shuffle tile.
    val cellCount = items.size + 1
    val rows = remember(cellCount) { (0 until cellCount).chunked(3) }

    Column(modifier = modifier) {
        // YTM-style header: account avatar + name above the "Speed dial" title.
        if (!userName.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = userName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = "Speed dial",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { cellIndex ->
                        if (cellIndex < items.size) {
                            val song = items[cellIndex]
                            SpeedDialTile(
                                song = song,
                                isPlaying = song.id == currentSong?.id,
                                onClick = { onSongClick(items, cellIndex) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            ShuffleTile(
                                onClick = onShuffleClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * The trailing "shuffle" tile of the speed dial — plays a random mix built from the
 * user's personalized recommendations. Styled to match [SpeedDialTile]: a square,
 * 14dp-rounded tile with a label overlaid at the bottom.
 */
@Composable
private fun ShuffleTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
            .bounceClick(shape = shape, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = "Shuffle play",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(34.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Shuffle",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpeedDialTile(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    val thumb = remember(song.thumbnailUrl) {
        ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl) ?: song.thumbnailUrl
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .bounceClick(shape = shape, onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumb)
                .crossfade(true)
                .size(300)
                .build(),
            contentDescription = song.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.5.dp, Color.White, shape)
            )
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
    // Confetti palette + deterministic positions so the splash doesn't
    // reflow across recompositions.
    val confettiColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFFFB347),
        Color(0xFFFF5E62)
    )
    val confettiSpec = remember {
        listOf(
            Triple(0.08f, 0.18f, 0), Triple(0.18f, 0.62f, 1), Triple(0.27f, 0.32f, 2),
            Triple(0.42f, 0.78f, 3), Triple(0.55f, 0.14f, 4), Triple(0.68f, 0.55f, 0),
            Triple(0.78f, 0.22f, 1), Triple(0.86f, 0.72f, 2), Triple(0.94f, 0.30f, 3)
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Confetti backdrop — fixed-position dots scattered behind the card.
            confettiSpec.forEach { (x, y, ci) ->
                Box(
                    modifier = Modifier
                        .padding(start = (x * 320).dp, top = (y * 200).dp)
                        .size(if (ci % 2 == 0) 8.dp else 6.dp)
                        .clip(if (ci == 3) RoundedCornerShape(2.dp) else CircleShape)
                        .background(confettiColors[ci % confettiColors.size].copy(alpha = 0.65f))
                )
            }

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
            // Dancing equalizer bars instead of a generic spinner — feels
            // like the app is "listening" while it loads more.
            EqualizerGlyph(
                barColor = MaterialTheme.colorScheme.primary,
                barCount = 5,
                height = 24.dp,
                barWidth = 3.dp
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
 * The leading badge animates a 4-bar equalizer so the banner feels live and
 * "listening to you" instead of a static notification.
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
                EqualizerGlyph(
                    barColor = MaterialTheme.colorScheme.tertiary,
                    barCount = 4,
                    height = 18.dp,
                    barWidth = 2.5.dp
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

/**
 * "Create a Mix" CTA, redesigned. Dashed primary-tinted border + a slow
 * horizontal shimmer sweep make it read as an *action* (not content) and
 * stand apart from the surrounding cards.
 */
@Composable
private fun CreateMixCard(
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "createMixShimmer")
    val shimmer by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val borderColor = MaterialTheme.colorScheme.primary
    val shimmerStart = MaterialTheme.colorScheme.primary.copy(alpha = 0f)
    val shimmerMid = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f))
            .drawBehind {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(12.dp.toPx(), 8.dp.toPx()), 0f
                    )
                )
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = stroke
                )

                // Shimmer sweep — diagonal gradient that crosses the card.
                val sweepWidthPx = size.width * 0.4f
                val sweepX = (size.width + sweepWidthPx) * shimmer - sweepWidthPx
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(shimmerStart, shimmerMid, shimmerStart),
                        start = androidx.compose.ui.geometry.Offset(sweepX, 0f),
                        end = androidx.compose.ui.geometry.Offset(sweepX + sweepWidthPx, size.height)
                    )
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Create your own mix",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
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
                com.suvojeet.suvmusic.ui.components.AppLogo(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                    contentDescription = null,
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
            // Drifting-sparkle backdrop — six deterministically-placed dots
            // gently float up and fade. Adds the "personalized magic" feel
            // without any per-item layout cost.
            DriftingSparkles()

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
    val highResUrl = remember(imageUrl) {
        ImageUtils.getHighResThumbnailUrl(imageUrl, size = 320) ?: imageUrl
    }

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
                    .data(highResUrl)
                    .crossfade(true)
                    .size(240)
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

/**
 * Wide horizontal track card — thumbnail on the left, title + artist + a
 * trailing play affordance on the right. Width 280dp so two cards fit a
 * peeking carousel viewport. Spotify-style layout that contrasts with the
 * square album/playlist cards used elsewhere.
 */
@Composable
fun TrackCard(
    track: com.suvojeet.suvmusic.lastfm.RecommendedTrack,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = track.image.lastOrNull()?.url ?: ""
    val highResUrl = remember(imageUrl) {
        ImageUtils.getHighResThumbnailUrl(imageUrl, size = 256) ?: imageUrl
    }

    Row(
        modifier = Modifier
            .width(280.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .bounceClick(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResUrl)
                .crossfade(true)
                .size(160)
                .build(),
            contentDescription = track.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
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

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Animated micro-elements — used by Phase-4 Detected Mood / For You banners and
// the LoadingMore indicator.
// -----------------------------------------------------------------------------

/**
 * Vertical-bar audio equalizer. Each bar runs an out-of-phase animation so
 * the row reads as live audio, not a generic spinner. Kept lightweight —
 * one infinite-transition, all bars share its driver.
 */
@Composable
private fun EqualizerGlyph(
    barColor: Color,
    barCount: Int,
    height: androidx.compose.ui.unit.Dp,
    barWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    // Per-bar phase offsets give the dance an organic, non-mechanical feel.
    val phases = remember(barCount) {
        when (barCount) {
            3 -> floatArrayOf(0f, 0.33f, 0.66f)
            4 -> floatArrayOf(0f, 0.25f, 0.5f, 0.75f)
            5 -> floatArrayOf(0f, 0.2f, 0.6f, 0.4f, 0.8f)
            else -> FloatArray(barCount) { it.toFloat() / barCount }
        }
    }
    val durations = remember(barCount) {
        when (barCount) {
            3 -> intArrayOf(620, 540, 700)
            4 -> intArrayOf(620, 540, 700, 580)
            5 -> intArrayOf(620, 540, 700, 580, 660)
            else -> IntArray(barCount) { 580 + (it % 3) * 60 }
        }
    }

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(barWidth * 0.7f),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val anim by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = durations[i],
                        easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        delayMillis = (phases[i] * 400f).toInt()
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(anim)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

/**
 * Six deterministically-placed sparkle dots that drift up + fade across the
 * For-You banner. Backdrop only — sits behind the banner content. No layout
 * cost because each dot is just a styled Box.
 */
@Composable
private fun DriftingSparkles(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sparkles")
    // Each sparkle: (x-fraction, base y-fraction, period-ms, size-dp, alpha-mul).
    val sparks = remember {
        listOf(
            Triple(0.12f, 0.65f, 2400),
            Triple(0.28f, 0.20f, 3100),
            Triple(0.46f, 0.78f, 2700),
            Triple(0.62f, 0.32f, 2950),
            Triple(0.78f, 0.70f, 2300),
            Triple(0.90f, 0.18f, 3300)
        )
    }
    val tint = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.fillMaxSize()) {
        sparks.forEachIndexed { idx, (x, y, ms) ->
            val drift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = ms,
                        easing = LinearEasing,
                        delayMillis = (idx * 240) % ms
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "spark$idx"
            )
            // Drift upward by ~14dp and fade out as it nears the top.
            val verticalOffset = (-14f * drift).dp
            val alpha = (1f - drift).coerceAtLeast(0f) * 0.55f
            val size = (3 + (idx % 3)).dp
            Box(
                modifier = Modifier
                    .padding(start = (x * 320).dp, top = (y * 80).dp)
                    .offset(y = verticalOffset)
                    .size(size)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = alpha))
            )
        }
    }
}

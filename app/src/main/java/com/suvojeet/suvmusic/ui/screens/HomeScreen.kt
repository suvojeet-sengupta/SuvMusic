package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.suvojeet.suvmusic.core.ui.components.M3ELoadingIndicator
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.HomeEvent
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel
import com.suvojeet.suvmusic.util.ImageUtils
import java.util.Calendar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
    onAvatarClick: () -> Unit = {},
    currentSong: Song? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sessionManager = remember { com.suvojeet.suvmusic.data.SessionManager(context) }
    val animatedBackgroundEnabled by sessionManager.playerAnimatedBackgroundFlow.collectAsState(initial = true)
    
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }

    val onSongMoreClickHandler = remember {
        { song: Song ->
            selectedSong = song
            showSongMenu = true
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowAddToPlaylistSheet -> {
                    playlistViewModel.showAddToPlaylistSheet(event.song)
                }
            }
        }
    }
    
    val dominantColors = rememberDominantColors(
        imageUrl = currentSong?.thumbnailUrl ?: uiState.recommendations.firstOrNull()?.thumbnailUrl
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (animatedBackgroundEnabled) {
            MeshGradientBackground(dominantColors = dominantColors)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        when {
            uiState.isLoading && uiState.homeSections.isEmpty() -> {
                HomeLoadingSkeleton()
            }
            uiState.error != null && uiState.homeSections.isEmpty() -> {
                ErrorState(message = uiState.error ?: "Error", onRetry = { viewModel.refresh() })
            }
            else -> {
                val lazyListState = rememberLazyListState()

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        item {
                            HomeGreetingHeader(
                                greeting = getGreeting(),
                                userName = uiState.userName,
                                onNotificationClick = { /* show notifications */ },
                                onAvatarClick = onAvatarClick
                            )
                        }

                        item {
                            HomeQuickActionsRow(
                                onPlaylistsClick = { /* navigate */ },
                                onRadioClick = onStartRadio,
                                onRecentsClick = onRecentsClick,
                                onListenTogetherClick = onListenTogetherClick
                            )
                        }

                        if (uiState.isLoggedIn) {
                            item {
                                AnimatedVisibility(
                                    visible = uiState.isForYouBannerVisible,
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    ForYouBannerM3E(onStartRadio = onStartRadio, onDismiss = viewModel::onDismissForYouBanner)
                                }
                            }
                        }

                        if (uiState.recommendations.isNotEmpty()) {
                            item {
                                HomeSectionHeader(title = "Quick picks")
                                QuickPicksSection(
                                    section = HomeSection(
                                        title = "Quick picks",
                                        items = uiState.recommendations.map { HomeItem.SongItem(it) },
                                        type = HomeSectionType.QuickPicks
                                    ),
                                    onSongClick = onSongClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onAlbumClick = onAlbumClick,
                                    onSongMoreClick = onSongMoreClickHandler
                                )
                            }
                        }

                        itemsIndexed(uiState.filteredSections) { _, section ->
                            RenderHomeSectionM3E(
                                section = section,
                                onSongClick = onSongClick,
                                onPlaylistClick = onPlaylistClick,
                                onAlbumClick = onAlbumClick,
                                onExploreClick = onExploreClick,
                                onStartRadio = onStartRadio,
                                onSongMoreClick = onSongMoreClickHandler
                            )
                        }

                        item {
                            AppFooterM3E()
                        }
                    }
                }
            }
        }
        
        selectedSong?.let { song ->
            SongMenuBottomSheet(
                isVisible = showSongMenu,
                onDismiss = { showSongMenu = false },
                song = song,
                onPlayNext = { viewModel.playNext(song); showSongMenu = false },
                onAddToQueue = { viewModel.addToQueue(song); showSongMenu = false },
                onAddToPlaylist = { viewModel.addToPlaylist(song); showSongMenu = false },
                onDownload = { viewModel.downloadSong(song); showSongMenu = false },
                onShare = { /* share logic */ },
                onListenTogether = onListenTogetherClick
            )
        }

        val playlistUiState by playlistViewModel.uiState.collectAsState()
        AddToPlaylistSheet(
            song = playlistUiState.selectedSong ?: Song("", "", "", "", 0L, null, com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE),
            isVisible = playlistUiState.showAddToPlaylistSheet,
            playlists = playlistUiState.userPlaylists,
            isLoading = playlistUiState.isLoadingPlaylists,
            onDismiss = { playlistViewModel.hideAddToPlaylistSheet() },
            onAddToPlaylist = { playlistViewModel.addSongToPlaylist(it) },
            onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeGreetingHeader(
    greeting: String,
    userName: String?,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(greeting, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(userName ?: "Listener", style = MaterialTheme.typography.headlineSmallEmphasized, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Outlined.Notifications, "Notifications")
            }
            FilledIconButton(
                onClick = onAvatarClick,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Filled.Person, "Account", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeQuickActionsRow(
    onPlaylistsClick: () -> Unit,
    onRadioClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onListenTogetherClick: () -> Unit,
) {
    val actions = listOf(
        Triple(Icons.Filled.LibraryMusic, "Playlists", onPlaylistsClick),
        Triple(Icons.Filled.Radio, "Radio", onRadioClick),
        Triple(Icons.Filled.History, "Recents", onRecentsClick),
        Triple(Icons.Filled.Group, "Listen Together", onListenTogetherClick),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(actions) { (icon, label, onClick) ->
            InputChip(
                selected = false,
                onClick = onClick,
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { Icon(icon, null, modifier = Modifier.size(InputChipDefaults.IconSize)) },
                shape = MaterialTheme.shapes.large,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeSectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLargeEmphasized)
        if (onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick, contentPadding = PaddingValues(0.dp)) {
                Text("See all", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ForYouBannerM3E(onStartRadio: () -> Unit, onDismiss: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Made for you", style = MaterialTheme.typography.titleMediumEmphasized)
                    Text("Based on your history", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onStartRadio, shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Radio")
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AppFooterM3E() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large), contentAlignment = Alignment.Center) {
            AsyncImage(model = com.suvojeet.suvmusic.R.drawable.logo, contentDescription = null, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("SuvMusic", style = MaterialTheme.typography.titleMedium)
        Text("v${com.suvojeet.suvmusic.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun RenderHomeSectionM3E(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onExploreClick: (String, String) -> Unit,
    onStartRadio: () -> Unit,
    onSongMoreClick: (Song) -> Unit,
) {
    Column {
        HomeSectionHeader(title = section.title)
        when (section.type) {
            HomeSectionType.QuickPicks -> QuickPicksSection(section, onSongClick, onPlaylistClick, onAlbumClick, onSongMoreClick)
            HomeSectionType.Grid -> GridSection(section, onSongClick, onPlaylistClick, onAlbumClick, onSongMoreClick)
            HomeSectionType.VerticalList -> VerticalListSection(section, onSongClick, onPlaylistClick, onAlbumClick, onSongMoreClick)
            else -> HorizontalCarouselSection(section, onSongClick, onPlaylistClick, onAlbumClick, onSongMoreClick)
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Error, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 24.dp)) { Text("Retry") }
    }
}

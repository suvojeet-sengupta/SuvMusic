package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.model.RecentSearchItem
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.ResultFilter
import com.suvojeet.suvmusic.ui.viewmodel.SearchTab
import com.suvojeet.suvmusic.ui.viewmodel.SearchViewModel
import com.suvojeet.suvmusic.util.dpadFocusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var isSearchActive by remember { mutableStateOf(false) }

    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Scroll tracking for hiding headers
    var isHeaderVisible by remember { mutableStateOf(true) }
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }.collect { currentOffset ->
            val delta = currentOffset - lastScrollOffset
            if (delta > 15 && isHeaderVisible && listState.firstVisibleItemIndex >= 0) {
                isHeaderVisible = false
            } else if (delta < -15 && !isHeaderVisible) {
                isHeaderVisible = true
            }
            // Always show header at the very top
            if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                isHeaderVisible = true
            }
            lastScrollOffset = currentOffset
        }
    }
    
    // Always show headers when search is expanded/active
    val effectiveHeaderVisibility = isHeaderVisible || isSearchActive
    
    androidx.compose.runtime.LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }
    
    val accentColor = MaterialTheme.colorScheme.primary
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.suvojeet.suvmusic.ui.viewmodel.SearchEvent.ShowAddToPlaylistSheet -> {
                    playlistViewModel.showAddToPlaylistSheet(event.song)
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val voiceSearchLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
                    if (!spokenText.isNullOrBlank()) {
                        viewModel.onQueryChange(spokenText)
                        viewModel.search()
                    }
                }
            }

            AnimatedVisibility(
                visible = effectiveHeaderVisibility,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + 
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + 
                       shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = uiState.query,
                                onQueryChange = { viewModel.onQueryChange(it) },
                                onSearch = {
                                    viewModel.search()
                                    isSearchActive = false
                                    focusManager.clearFocus()
                                },
                                expanded = isSearchActive,
                                onExpandedChange = { isSearchActive = it },
                                placeholder = {
                                    Text(
                                        "Artists, Songs, Lyrics and more",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                leadingIcon = {
                                    if (isSearchActive) {
                                        IconButton(onClick = {
                                            isSearchActive = false
                                            viewModel.onBackPressed()
                                            focusManager.clearFocus()
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                        }
                                    } else {
                                        Icon(Icons.Default.Search, "Search")
                                    }
                                },
                                trailingIcon = {
                                    if (uiState.query.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                                            Icon(Icons.Default.Clear, "Clear")
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                                            }
                                            try { voiceSearchLauncher.launch(intent) }
                                            catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Voice search not supported", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(Icons.Default.Mic, "Voice Search")
                                        }
                                    }
                                }
                            )
                        },
                        expanded = isSearchActive,
                        onExpandedChange = { isSearchActive = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            if (uiState.showSuggestions && uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty()) {
                                items(uiState.suggestions.take(5)) { suggestion ->
                                    SuggestionItem(
                                        suggestion = suggestion,
                                        accentColor = accentColor,
                                        onClick = {
                                            viewModel.onSuggestionClick(suggestion)
                                            isSearchActive = false
                                        }
                                    )
                                }
                            }
                            
                            if (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recently Searched",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = accentColor,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                                        )
                                    }
                                }
                                
                                items(
                                    items = uiState.recentSearches,
                                    key = { it.id },
                                    contentType = { it.javaClass.simpleName }
                                ) { item ->
                                    RecentSearchItemRow(
                                        item = item,
                                        onSongClick = onSongClick,
                                        onArtistClick = onArtistClick,
                                        onAlbumClick = onAlbumClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onMoreClick = { song ->
                                            selectedSong = song
                                            showSongMenu = true
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }

                    // Tab Selection (YouTube / Local)
                    TabRow(
                        selectedTabIndex = uiState.selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = accentColor,
                        divider = {},
                        indicator = { tabPositions ->
                            if (uiState.selectedTab.ordinal < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                                    color = accentColor
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Tab(
                            selected = uiState.selectedTab == SearchTab.YOUTUBE_MUSIC,
                            onClick = { viewModel.onTabChange(SearchTab.YOUTUBE_MUSIC) },
                            text = { Text("YouTube Music", style = MaterialTheme.typography.titleSmall) }
                        )
                        Tab(
                            selected = uiState.selectedTab == SearchTab.YOUR_LIBRARY,
                            onClick = { viewModel.onTabChange(SearchTab.YOUR_LIBRARY) },
                            text = { Text("Local Library", style = MaterialTheme.typography.titleSmall) }
                        )
                    }
                    
                    // Category Chips for YouTube Tab
                    if (uiState.selectedTab == SearchTab.YOUTUBE_MUSIC) {
                        AnimatedVisibility(
                            visible = uiState.query.isNotBlank(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val filters = listOf(
                                ResultFilter.ALL to "All",
                                ResultFilter.SONGS to "Songs",
                                ResultFilter.VIDEOS to "Videos",
                                ResultFilter.ALBUMS to "Albums",
                                ResultFilter.ARTISTS to "Artists",
                                ResultFilter.COMMUNITY_PLAYLISTS to "Community",
                                ResultFilter.FEATURED_PLAYLISTS to "Featured"
                            )
                            
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                filters.forEachIndexed { index, (filter, label) ->
                                    SegmentedButton(
                                        selected = uiState.resultFilter == filter,
                                        onClick = { viewModel.setResultFilter(filter) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = accentColor.copy(alpha = 0.15f),
                                            activeContentColor = accentColor,
                                            activeBorderColor = accentColor,
                                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                                            inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        icon = {}
                                    ) {
                                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Content Area
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator(color = accentColor)
                        }
                    }
                }
                
                if (uiState.selectedTab == SearchTab.YOUTUBE_MUSIC) {
                    if (uiState.resultFilter != ResultFilter.ALL && !uiState.isLoading) {
                        when (uiState.resultFilter) {
                            ResultFilter.SONGS, ResultFilter.VIDEOS -> {
                                itemsIndexed(uiState.results, key = { index, song -> "song_${song.id}_$index" }) { index, song ->
                                    SearchResultItem(song = song, onClick = { viewModel.addToRecentSearches(song); onSongClick(uiState.results, index) }, onArtistClick = onArtistClick, onMoreClick = { selectedSong = song; showSongMenu = true })
                                }
                            }
                            ResultFilter.ARTISTS -> {
                                items(uiState.artistResults, key = { it.id }) { artist ->
                                    ArtistSearchListItem(artist = artist, onClick = { onArtistClick(artist.id) })
                                }
                            }
                            ResultFilter.ALBUMS -> {
                                items(uiState.albumResults, key = { it.id }) { album ->
                                    AlbumSearchListItem(album = album, onClick = { viewModel.addToRecentSearches(album); onAlbumClick(album) })
                                }
                            }
                            ResultFilter.COMMUNITY_PLAYLISTS, ResultFilter.FEATURED_PLAYLISTS -> {
                                items(uiState.playlistResults, key = { it.id }) { playlist ->
                                    PlaylistSearchListItem(playlist = playlist, onClick = { viewModel.addToRecentSearches(playlist); onPlaylistClick(playlist.id) })
                                }
                            }
                            else -> {}
                        }
                    } else if (uiState.resultFilter == ResultFilter.ALL && !uiState.isLoading && uiState.query.isNotBlank()) {
                        if (uiState.artistResults.isNotEmpty()) {
                            item {
                                Text("Artists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                                    items(uiState.artistResults, key = { it.id }) { artist -> ArtistSearchCard(artist = artist, onClick = { onArtistClick(artist.id) }) }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                            }
                        }
                        if (uiState.playlistResults.isNotEmpty()) {
                            item {
                                Text("Playlists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                                    items(uiState.playlistResults, key = { it.id }) { playlist -> PlaylistSearchCard(playlist = playlist, onClick = { viewModel.addToRecentSearches(playlist); onPlaylistClick(playlist.id) }) }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                            }
                        }
                        if (uiState.albumResults.isNotEmpty()) {
                            item {
                                Text("Albums", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                                    items(uiState.albumResults, key = { it.id }) { album -> AlbumSearchCard(album = album, onClick = { viewModel.addToRecentSearches(album); onAlbumClick(album) }) }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                            }
                        }
                        if (uiState.results.isNotEmpty()) {
                            item { Text("Songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)) }
                            itemsIndexed(uiState.results, key = { index, song -> "main_song_${song.id}_$index" }) { index, song ->
                                SearchResultItem(song = song, onClick = { viewModel.addToRecentSearches(song); onSongClick(uiState.results, index) }, onArtistClick = onArtistClick, onMoreClick = { selectedSong = song; showSongMenu = true })
                            }
                        }
                    }
                } else if (uiState.selectedTab == SearchTab.YOUR_LIBRARY) {
                    // Local Search results
                    if (uiState.results.isNotEmpty()) {
                        itemsIndexed(uiState.results, key = { index: Int, song: Song -> "local_${song.id}_$index" }) { index: Int, song: Song ->
                            SearchResultItem(song = song, onClick = { onSongClick(uiState.results, index) }, onArtistClick = onArtistClick, onMoreClick = { selectedSong = song; showSongMenu = true })
                        }
                    } else if (uiState.query.isNotBlank() && !uiState.isLoading) {
                        item { Text("No local results found for \"${uiState.query}\"", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp)) }
                    }
                }
                
                if (uiState.selectedTab == SearchTab.YOUTUBE_MUSIC && uiState.query.isNotBlank() && uiState.results.isEmpty() && !uiState.isLoading && uiState.artistResults.isEmpty() && uiState.albumResults.isEmpty() && uiState.playlistResults.isEmpty()) {
                    item { Text("No results found for \"${uiState.query}\"", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp)) }
                }
                
                if (uiState.query.isBlank() && uiState.recentSearches.isEmpty() && uiState.selectedTab == SearchTab.YOUTUBE_MUSIC) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Text("Trending Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            uiState.trendingSearches.forEach { term ->
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { viewModel.onTrendingSearchClick(term) }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(term, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showSongMenu && selectedSong != null) {
            val song = selectedSong!!
            SongMenuBottomSheet(
                isVisible = showSongMenu, onDismiss = { showSongMenu = false }, song = song,
                onPlayNext = { viewModel.playNext(song) }, onAddToQueue = { viewModel.addToQueue(song) },
                onAddToPlaylist = { viewModel.addToPlaylist(song) }, onDownload = { viewModel.downloadSong(song) },
                onShare = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out this song: ${song.title} by ${song.artist}\n\nhttps://music.youtube.com/watch?v=${song.id}")
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Song"))
                },
                onViewArtist = song.artistId?.let { id -> { onArtistClick(id) } }, onViewAlbum = null
            )
        }
        
        val playlistMgmtState by playlistViewModel.uiState.collectAsState()
        if (playlistMgmtState.showAddToPlaylistSheet && playlistMgmtState.selectedSong != null) {
            val song = playlistMgmtState.selectedSong!!
            AddToPlaylistSheet(song = song, isVisible = playlistMgmtState.showAddToPlaylistSheet, playlists = playlistMgmtState.userPlaylists, isLoading = playlistMgmtState.isLoadingPlaylists, onDismiss = { playlistViewModel.hideAddToPlaylistSheet() }, onAddToPlaylist = { playlistId -> playlistViewModel.addSongToPlaylist(playlistId) }, onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() })
        }
        
        if (playlistMgmtState.showCreatePlaylistDialog) {
             CreatePlaylistDialog(isVisible = playlistMgmtState.showCreatePlaylistDialog, isCreating = playlistMgmtState.isCreatingPlaylist, onDismiss = { playlistViewModel.hideCreatePlaylistDialog() }, onCreate = { title, description, isPrivate, syncWithYt -> playlistViewModel.createPlaylist(title, description, isPrivate, syncWithYt) }, isLoggedIn = true)
        }
    }
}

@Composable
private fun SuggestionItem(suggestion: String, accentColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().dpadFocusable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, null, tint = accentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(suggestion, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SearchResultItem(song: Song, onClick: () -> Unit, onArtistClick: (String) -> Unit = {}, onMoreClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().dpadFocusable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (song.isVideo) "Video" else "Song", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = if (song.artistId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = if (song.artistId != null) Modifier.clickable { onArtistClick(song.artistId!!) } else Modifier)
            }
        }
        Box(modifier = Modifier.dpadFocusable(onClick = onMoreClick, shape = CircleShape).padding(8.dp)) { Icon(Icons.Default.MoreVert, "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun ArtistSearchListItem(artist: Artist, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().dpadFocusable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = artist.thumbnailUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(artist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            artist.subscribers?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun AlbumSearchListItem(album: Album, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().dpadFocusable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = album.thumbnailUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(album.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Album • ${album.artist}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PlaylistSearchListItem(playlist: Playlist, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().dpadFocusable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = playlist.thumbnailUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(playlist.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Playlist • ${playlist.author}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ArtistSearchCard(artist: Artist, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp).dpadFocusable(onClick = onClick, shape = RoundedCornerShape(8.dp))) {
        AsyncImage(model = artist.thumbnailUrl, contentDescription = artist.name, modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.height(8.dp))
        Text(artist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(artist.subscribers ?: "Artist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun PlaylistSearchCard(playlist: Playlist, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).dpadFocusable(onClick = onClick, shape = RoundedCornerShape(8.dp))) {
        AsyncImage(model = playlist.thumbnailUrl, contentDescription = playlist.title, modifier = Modifier.size(140.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.height(8.dp))
        Text(playlist.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (playlist.author.isNotBlank()) Text(playlist.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AlbumSearchCard(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).dpadFocusable(onClick = onClick, shape = RoundedCornerShape(8.dp))) {
        AsyncImage(model = album.thumbnailUrl, contentDescription = album.title, modifier = Modifier.size(140.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.height(8.dp))
        Text(album.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val subtitle = (if (album.artist.isNotBlank()) album.artist else "") + (if (album.year != null) " • ${album.year}" else "")
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RecentSearchItemRow(item: RecentSearchItem, onSongClick: (List<Song>, Int) -> Unit, onArtistClick: (String) -> Unit, onAlbumClick: (Album) -> Unit, onPlaylistClick: (String) -> Unit, onMoreClick: (Song) -> Unit, viewModel: SearchViewModel) {
    when (item) {
        is RecentSearchItem.SongItem -> SearchResultItem(song = item.song, onClick = { viewModel.onRecentSearchClick(item); onSongClick(listOf(item.song), 0) }, onArtistClick = onArtistClick, onMoreClick = { onMoreClick(item.song) })
        is RecentSearchItem.AlbumItem -> AlbumSearchListItem(album = item.album, onClick = { viewModel.onRecentSearchClick(item); onAlbumClick(item.album) })
        is RecentSearchItem.PlaylistItem -> PlaylistSearchListItem(playlist = item.playlist, onClick = { viewModel.onRecentSearchClick(item); onPlaylistClick(item.playlist.id) })
    }
}

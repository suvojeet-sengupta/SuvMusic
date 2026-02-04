package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.Playlist
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.viewmodel.SearchTab
import com.suvojeet.suvmusic.ui.viewmodel.SearchViewModel
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.ResultFilter
import com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyRow
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

/**
 * Apple Music-inspired search screen with recent searches, suggestions, and inline results.
 */
@Composable
fun SearchScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onArtistClick: (String) -> Unit = {}, // Artist browse ID
    onPlaylistClick: (String) -> Unit = {}, // Playlist ID
    onAlbumClick: (Album) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var isSearchFocused by remember { mutableStateOf(false) }

    // Song Menu State
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    
    // Accent color for the app (works in both light/dark)
    val accentColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search Header with Back Button and Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (visible when search is active or has results)
                AnimatedVisibility(
                    visible = uiState.isSearchActive || uiState.query.isNotBlank()
                ) {
                    IconButton(
                        onClick = { 
                            viewModel.onBackPressed()
                            focusManager.clearFocus()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = accentColor
                        )
                    }
                }
                
                // Search Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Voice Search Launcher
                    val voiceSearchLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val data = result.data
                            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                            val spokenText = results?.get(0)
                            if (!spokenText.isNullOrBlank()) {
                                viewModel.onQueryChange(spokenText)
                                viewModel.search()
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        BasicTextField(
                            value = uiState.query,
                            onValueChange = { viewModel.onQueryChange(it) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .onFocusChanged { 
                                    isSearchFocused = it.isFocused
                                    viewModel.onSearchFocusChange(it.isFocused)
                                },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    viewModel.search()
                                    focusManager.clearFocus()
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.query.isEmpty()) {
                                        Text(
                                            text = "Artists, Songs, Lyrics and more",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        // Clear button (Visible when query is not empty)
                        AnimatedVisibility(visible = uiState.query.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Voice Search Button (Visible when query is empty)
                        AnimatedVisibility(visible = uiState.query.isEmpty()) {
                            IconButton(
                                onClick = {
                                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                                    }
                                    try {
                                        voiceSearchLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Voice search not supported", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Category Chips (Only for YouTube Music tab)
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
                    ResultFilter.COMMUNITY_PLAYLISTS to "Community Playlists",
                    ResultFilter.FEATURED_PLAYLISTS to "Featured Playlists"
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(filters) { (filter, label) ->
                        FilterChip(
                            selected = uiState.resultFilter == filter,
                            onClick = { viewModel.setResultFilter(filter) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = null,
                            shape = CircleShape
                        )
                    }
                }
            }
            
            // Content Area
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                // Show suggestions when typing
                if (uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty()) {
                    items(uiState.suggestions.take(3)) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            accentColor = accentColor,
                            onClick = { viewModel.onSuggestionClick(suggestion) }
                        )
                    }
                    
                    item { 
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                // Loading indicator
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }
                }
                
                // Filtered Views
                if (uiState.resultFilter != ResultFilter.ALL && !uiState.isLoading) {
                    when (uiState.resultFilter) {
                        ResultFilter.SONGS, ResultFilter.VIDEOS -> {
                            itemsIndexed(uiState.results) { index, song ->
                                SearchResultItem(
                                    song = song,
                                    onClick = {
                                        viewModel.addToRecentSearches(song)
                                        onSongClick(uiState.results, index)
                                    },
                                    onArtistClick = { artistId -> onArtistClick(artistId) },
                                    onMoreClick = {
                                        selectedSong = song
                                        showSongMenu = true
                                    }
                                )
                            }
                        }
                        ResultFilter.ARTISTS -> {
                            items(uiState.artistResults) { artist ->
                                ArtistSearchListItem(
                                    artist = artist,
                                    onClick = { onArtistClick(artist.id) }
                                )
                            }
                        }
                        ResultFilter.ALBUMS -> {
                            items(uiState.albumResults) { album ->
                                AlbumSearchListItem(
                                    album = album,
                                    onClick = { onAlbumClick(album) }
                                )
                            }
                        }
                        ResultFilter.COMMUNITY_PLAYLISTS, ResultFilter.FEATURED_PLAYLISTS -> {
                            items(uiState.playlistResults) { playlist ->
                                PlaylistSearchListItem(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist.id) }
                                )
                            }
                        }
                        else -> {}
                    }
                } else if (uiState.resultFilter == ResultFilter.ALL) {
                    // Artist Results (show at top)
                    if (uiState.query.isNotBlank() && uiState.artistResults.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                items(uiState.artistResults) { artist ->
                                    ArtistSearchCard(
                                        artist = artist,
                                        onClick = { onArtistClick(artist.id) }
                                    )
                                }
                            }
                        }
                        
                        item {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                    
                    // Playlist Results (show after artists)
                    if (uiState.query.isNotBlank() && uiState.playlistResults.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                items(uiState.playlistResults) { playlist ->
                                    PlaylistSearchCard(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) }
                                    )
                                }
                            }
                        }
                        
                        item {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
    
                    // Album Results
                    if (uiState.query.isNotBlank() && uiState.albumResults.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                items(uiState.albumResults) { album ->
                                    AlbumSearchCard(
                                        album = album,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                        
                        item {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                    
                    // Search Results
                    if (uiState.query.isNotBlank() && uiState.results.isNotEmpty()) {
                        itemsIndexed(uiState.results) { index, song ->
                            SearchResultItem(
                                song = song,
                                onClick = {
                                    viewModel.addToRecentSearches(song)
                                    onSongClick(uiState.results, index)
                                },
                                onArtistClick = { artistId ->
                                    onArtistClick(artistId)
                                },
                                onMoreClick = {
                                    selectedSong = song
                                    showSongMenu = true
                                }
                            )
                        }
                    }
                }
                
                // No results message
                if (uiState.query.isNotBlank() && uiState.results.isEmpty() && !uiState.isLoading) {
                    item {
                        Text(
                            text = "No results found for \"${uiState.query}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp)
                        )
                    }
                }
                
                // Recent Searches (when not searching)
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
                    
                    items(uiState.recentSearches) { song ->
                        SearchResultItem(
                            song = song,
                            onClick = {
                                viewModel.addToRecentSearches(song)
                                onSongClick(uiState.recentSearches, uiState.recentSearches.indexOf(song))
                            },
                            onArtistClick = { artistId ->
                                onArtistClick(artistId)
                            },
                            onMoreClick = {
                                selectedSong = song
                                showSongMenu = true
                            }
                        )
                    }
                }
                
                // Empty state with Trending Searches
                if (uiState.query.isBlank() && uiState.recentSearches.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "Trending Searches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            uiState.trendingSearches.forEach { term ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.onTrendingSearchClick(term) }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = term,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Song Menu Sheet
        if (showSongMenu && selectedSong != null) {
            SongMenuBottomSheet(
                isVisible = showSongMenu,
                onDismiss = { showSongMenu = false },
                song = selectedSong!!,
                onPlayNext = { viewModel.playNext(selectedSong!!) },
                onAddToQueue = { viewModel.addToQueue(selectedSong!!) },
                onAddToPlaylist = { 
                    playlistViewModel.showAddToPlaylistSheet(selectedSong!!)
                },
                onDownload = { viewModel.downloadSong(selectedSong!!) },
                onShare = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out this song: ${selectedSong!!.title} by ${selectedSong!!.artist}")
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Song"))
                },
                onViewArtist = if (selectedSong!!.artistId != null) { 
                    { onArtistClick(selectedSong!!.artistId!!) } 
                } else null,
                onViewAlbum = null // Album navigation via ID not supported yet as Song doesn't have albumId
            )
        }
        
        // Observe Playlist ViewModel state for Add to Playlist Sheet
        val playlistUiState by playlistViewModel.uiState.collectAsState()
        
        if (playlistUiState.showAddToPlaylistSheet && playlistUiState.selectedSong != null) {
            AddToPlaylistSheet(
                song = playlistUiState.selectedSong!!,
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
        
        // Also handle Create Playlist Dialog if it is triggered
        if (playlistUiState.showCreatePlaylistDialog) {
             // We need to implement the dialog here or ensure it's handled. 
             // LibraryScreen has CreatePlaylistDialog. SearchScreen might need it too?
             // Or we just don't support creating new playlist from search directly yet unless we add the component.
             // Let's add the component.
             com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog(
                isVisible = playlistUiState.showCreatePlaylistDialog,
                isCreating = playlistUiState.isCreatingPlaylist,
                onDismiss = { playlistViewModel.hideCreatePlaylistDialog() },
                onCreate = { title, description, isPrivate, syncWithYt ->
                    playlistViewModel.createPlaylist(title, description, isPrivate, syncWithYt)
                },
                isLoggedIn = true 
             )
        }
    }
}

@Composable
fun ArtistSearchListItem(
    artist: Artist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            if (artist.subscribers != null) {
                Text(
                    text = artist.subscribers,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AlbumSearchListItem(
    album: Album,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "Album • ${album.artist}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlaylistSearchListItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "Playlist • ${playlist.author}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SearchResultItem(
    song: Song,
    onClick: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Title and Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (song.isVideo) "Video" else "Song",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (song.artistId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (song.artistId != null) {
                        Modifier.clickable { onArtistClick(song.artistId) }
                    } else {
                        Modifier
                    }
                )
            }
        }
        
        // Menu button
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArtistSearchCard(
    artist: Artist,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick)
    ) {
        // Circular artist image
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (artist.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artist.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = artist.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Artist name
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Subscriber count
        if (artist.subscribers != null) {
            Text(
                text = artist.subscribers,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        } else {
            Text(
                text = "Artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistSearchCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        // Playlist thumbnail
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(playlist.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Playlist title
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Author
        if (playlist.author.isNotBlank()) {
            Text(
                text = playlist.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumSearchCard(
    album: Album,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        // Album thumbnail
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (album.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Album title
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Artist/Year
        val artistText = if (album.artist.isNotBlank()) album.artist else ""
        val yearText = if (album.year != null) " • ${album.year}" else ""
        val subtitle = artistText + yearText
        
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

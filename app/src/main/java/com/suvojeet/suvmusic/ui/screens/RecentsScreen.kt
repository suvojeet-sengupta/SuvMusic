package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.RecentlyPlayed
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import com.suvojeet.suvmusic.ui.screens.viewmodel.RecentsViewModel
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet
import android.text.format.DateUtils

/**
 * Recents screen showing listening history with YT Music-inspired UI.
 * Now includes Incognito Mode and Multi-select functionality.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: RecentsViewModel = koinViewModel(),
    playlistViewModel: PlaylistManagementViewModel = koinViewModel()
) {
    val recentlyPlayed by viewModel.recentSongs.collectAsState()
    val selectedSongIds by viewModel.selectedSongs.collectAsState()
    val incognitoModeEnabled by viewModel.incognitoModeEnabled.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    
    // Song Menu State
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val haptic = LocalHapticFeedback.current
    
    val filteredHistory = remember(recentlyPlayed, searchQuery) {
        if (searchQuery.isEmpty()) recentlyPlayed
        else recentlyPlayed.filter { 
            it.song.title.contains(searchQuery, ignoreCase = true) || 
            it.song.artist.contains(searchQuery, ignoreCase = true) 
        }
    }

    val isSelectionMode = selectedSongIds.isNotEmpty()

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear history?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all songs from your listening history. This action cannot be undone.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); showClearConfirmDialog = false }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(28.dp)
        )
    }
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedSongIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Clear, "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val selectedSongs = recentlyPlayed
                                .filter { selectedSongIds.contains(it.song.id) }
                                .map { it.song }
                            playlistViewModel.showAddToPlaylistSheet(selectedSongs)
                        }) {
                            Icon(Icons.Default.PlaylistAdd, "Add to Playlist")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            } else {
                LargeTopAppBar(
                    title = { Text(text = "History", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Incognito Mode Toggle
                        IconButton(onClick = { viewModel.setIncognitoMode(!incognitoModeEnabled) }) {
                            Icon(
                                imageVector = if (incognitoModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Incognito Mode",
                                tint = if (incognitoModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (recentlyPlayed.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirmDialog = true }) {
                                Icon(Icons.Default.DeleteOutline, "Clear")
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Incognito Indicator
            AnimatedVisibility(
                visible = incognitoModeEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "Incognito Mode is on. Your listening history is not being saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search history") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) } }
                } else null,
                shape = CircleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            if (filteredHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Text(text = if (searchQuery.isEmpty()) "No history yet" else "No results found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val groupedByDate = remember(filteredHistory) {
                    filteredHistory.groupBy { getDateLabel(it.playedAt) }
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                    groupedByDate.forEach { (dateLabel, items) ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                        
                        items(items, key = { "${it.song.id}_${it.playedAt}" }) { recent ->
                            val isSelected = selectedSongIds.contains(recent.song.id)
                            val allSongsInHistory = recentlyPlayed.map { it.song }
                            val indexInAll = allSongsInHistory.indexOf(recent.song)
                            
                            RecentSongItem(
                                recent = recent,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = { 
                                    if (isSelectionMode) viewModel.toggleSelection(recent.song.id)
                                    else onSongClick(allSongsInHistory, indexInAll)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleSelection(recent.song.id)
                                },
                                onMoreClick = {
                                    selectedSong = recent.song
                                    showSongMenu = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Song Menu
    if (showSongMenu && selectedSong != null) {
        SongMenuBottomSheet(
            isVisible = true,
            onDismiss = { showSongMenu = false },
            song = selectedSong!!,
            onPlayNext = { /* handled by navigation/player */ },
            onAddToQueue = { /* handled by navigation/player */ },
            onAddToPlaylist = { playlistViewModel.showAddToPlaylistSheet(selectedSong!!) },
            onDownload = { viewModel.downloadSong(selectedSong!!) },
            onShare = { 
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "Listen to ${selectedSong!!.title} by ${selectedSong!!.artist} on SuvMusic")
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share song"))
            }
        )
    }

    // Add to Playlist Sheet
    val playlistMgmtState by playlistViewModel.uiState.collectAsState()
    if (playlistMgmtState.showAddToPlaylistSheet && playlistMgmtState.selectedSongs.isNotEmpty()) {
        AddToPlaylistSheet(
            songs = playlistMgmtState.selectedSongs,
            isVisible = true,
            playlists = playlistMgmtState.userPlaylists,
            isLoading = playlistMgmtState.isLoadingPlaylists || playlistMgmtState.isAddingSong,
            onDismiss = { playlistViewModel.hideAddToPlaylistSheet() },
            onAddToPlaylist = { playlistId -> 
                playlistViewModel.addSongsToPlaylist(playlistId)
                viewModel.clearSelection()
            },
            onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() }
        )
    }

    if (playlistMgmtState.showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            isVisible = true,
            isCreating = playlistMgmtState.isCreatingPlaylist,
            onDismiss = { playlistViewModel.hideCreatePlaylistDialog() },
            onCreate = { title, desc, private, sync -> playlistViewModel.createPlaylist(title, desc, private, sync) },
            isLoggedIn = true
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RecentSongItem(
    recent: RecentlyPlayed,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = recent.song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                alpha = if (isSelected) 0.5f else 1f
            )
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
        
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = recent.song.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "${recent.song.artist} • ${getTimeLabel(recent.playedAt)}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        if (!isSelectionMode) {
            IconButton(onClick = onMoreClick) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun getDateLabel(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    val playedCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val playedDay = playedCalendar.get(Calendar.DAY_OF_YEAR)
    val playedYear = playedCalendar.get(Calendar.YEAR)
    return when {
        DateUtils.isToday(timestamp) -> "Today"
        DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
        year == playedYear -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}


private fun getTimeLabel(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}

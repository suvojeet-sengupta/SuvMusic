package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.MusicCard
import com.suvojeet.suvmusic.ui.components.PlaylistCard
import com.suvojeet.suvmusic.ui.viewmodel.LibraryViewModel

/**
 * Library screen with playlists, offline music, and liked songs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Playlists", "Offline", "Liked")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> PlaylistsTab(
                playlists = uiState.playlists,
                onPlaylistClick = onPlaylistClick
            )
            1 -> OfflineTab(
                localSongs = uiState.localSongs,
                downloadedSongs = uiState.downloadedSongs,
                onSongClick = onSongClick
            )
            2 -> LikedTab(
                songs = uiState.likedSongs,
                onSongClick = onSongClick
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistDisplayItem>,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(playlists) { playlist ->
            PlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) }
            )
        }
    }
}

@Composable
private fun OfflineTab(
    localSongs: List<Song>,
    downloadedSongs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (localSongs.isEmpty() && downloadedSongs.isEmpty()) {
            item {
                Text(
                    text = "No offline songs yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
        
        if (downloadedSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(downloadedSongs) { song ->
                MusicCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
        
        if (localSongs.isNotEmpty()) {
             item {
                Text(
                    text = "Device Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(localSongs) { song ->
                MusicCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun LikedTab(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (songs.isEmpty()) {
            item {
                Text(
                    text = "No liked songs yet.\nLog in to YouTube Music to see your likes.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
        
        items(songs) { song ->
            MusicCard(
                song = song,
                onClick = { onSongClick(song) }
            )
        }
    }
}
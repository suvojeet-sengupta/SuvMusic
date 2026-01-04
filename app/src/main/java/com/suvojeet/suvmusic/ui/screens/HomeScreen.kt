package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.CompactMusicCard
import com.suvojeet.suvmusic.ui.components.FeaturedPlaylistCard
import com.suvojeet.suvmusic.ui.components.MusicCard
import com.suvojeet.suvmusic.ui.components.PlaylistCard
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel

/**
 * Home screen with recommendations, quick picks, and playlists.
 */
@Composable
fun HomeScreen(
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Good ${getTimeGreeting()}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "What do you want to listen to?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Featured Playlist
        if (uiState.playlists.isNotEmpty()) {
            item {
                FeaturedPlaylistCard(
                    playlist = uiState.playlists.first(),
                    onClick = { onPlaylistClick(uiState.playlists.first()) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        
        // Quick Picks Section
                    item {
                        HomeSectionHeader(title = "Quick Picks")
                    }        
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.recommendations.take(10)) { song ->
                    CompactMusicCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
        
        // Playlists Section
        if (uiState.playlists.size > 1) {
            item {
                HomeSectionHeader(title = "Your Playlists")
            }
            
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.playlists.drop(1)) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                }
            }
        }
        
        // Recently Played
        if (uiState.recommendations.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "Recently Played")
            }
            
            items(uiState.recommendations.take(5)) { song ->
                MusicCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

private fun getTimeGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Morning"
        hour < 17 -> "Afternoon"
        else -> "Evening"
    }
}

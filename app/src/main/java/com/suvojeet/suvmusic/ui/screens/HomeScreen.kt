package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.CompactMusicCard
import com.suvojeet.suvmusic.ui.components.FeaturedPlaylistCard
import com.suvojeet.suvmusic.ui.components.HomeLoadingSkeleton
import com.suvojeet.suvmusic.ui.components.MusicCard
import com.suvojeet.suvmusic.ui.components.PlaylistCard
import com.suvojeet.suvmusic.ui.viewmodel.HomeViewModel

/**
 * Home screen with recommendations, quick picks, and playlists.
 * Shows shimmer loading animation while content loads.
 */
@Composable
fun HomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Shimmer loading skeleton
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HomeLoadingSkeleton()
        }
        
        // Actual content
        AnimatedVisibility(
            visible = !uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
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
                if (uiState.recommendations.isNotEmpty()) {
                    val quickPicks = uiState.recommendations.take(10)
                    item {
                        HomeSectionHeader(title = "Quick Picks")
                    }
                    
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(quickPicks) { index, song ->
                                CompactMusicCard(
                                    song = song,
                                    onClick = { onSongClick(quickPicks, index) }
                                )
                            }
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
                    val recentlyPlayed = uiState.recommendations.take(5)
                    item {
                        HomeSectionHeader(title = "Recently Played")
                    }
                    
                    itemsIndexed(recentlyPlayed) { index, song ->
                        MusicCard(
                            song = song,
                            onClick = { onSongClick(recentlyPlayed, index) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
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
        fontWeight = FontWeight.SemiBold,
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

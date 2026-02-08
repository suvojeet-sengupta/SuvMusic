package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.ui.viewmodel.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    HomeLoadingSkeleton()
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        items(uiState.sections) { section ->
                            when (section.type) {
                                com.suvojeet.suvmusic.data.model.HomeSectionType.LargeCardWithList -> {
                                    LargeCardWithListSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick
                                    )
                                }
                                com.suvojeet.suvmusic.data.model.HomeSectionType.Grid -> {
                                    GridSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick
                                    )
                                }
                                com.suvojeet.suvmusic.data.model.HomeSectionType.VerticalList -> {
                                    VerticalListSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick
                                    )
                                }
                                else -> {
                                    HorizontalCarouselSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

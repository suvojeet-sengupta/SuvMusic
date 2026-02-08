package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.CategoryCard
import com.suvojeet.suvmusic.ui.components.MusicCard
import com.suvojeet.suvmusic.ui.viewmodel.MoodAndGenresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAndGenresScreen(
    onCategoryClick: (String, String, String) -> Unit, // browseId, params, title
    onBackClick: () -> Unit,
    viewModel: MoodAndGenresViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moods & Genres") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoadingCategories) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.categories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = {
                                onCategoryClick(
                                    category.browseId,
                                    category.params ?: "",
                                    category.title
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAndGenresDetailScreen(
    browseId: String,
    params: String?,
    title: String,
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    viewModel: MoodAndGenresViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(browseId, params) {
        if (uiState.selectedCategoryTitle != title || uiState.categorySongs.isEmpty()) {
             viewModel.loadCategoryContent(browseId, params, title)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoadingContent) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp), // Space for player
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.categorySongs) { song ->
                        MusicCard(
                            song = song,
                            onClick = {
                                onSongClick(uiState.categorySongs, uiState.categorySongs.indexOf(song))
                            },
                            backgroundColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    }
                }
            }
        }
    }
}

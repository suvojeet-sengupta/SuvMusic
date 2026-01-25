package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.viewmodel.PickMusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickMusicScreen(
    onBackClick: () -> Unit,
    onMixCreated: (List<Song>) -> Unit,
    viewModel: PickMusicViewModel = hiltViewModel()
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedArtists by viewModel.selectedArtists.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pick music",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = { /* Search Action if needed separately or integrated */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                // Search Bar (Optional based on screenshot, but needed for functionality)
                // Screenshot shows just "Pick music" and Search Icon. 
                // Let's implement clicking search icon expands search bar? 
                // Or just keep a search field below header for ease.
                // For simplicity: Always visible search field.
                TextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search artists") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        bottomBar = {
             if (selectedArtists.isNotEmpty()) {
                 Button(
                     onClick = { viewModel.createMix(onMixCreated) },
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(16.dp)
                         .height(56.dp),
                     shape = RoundedCornerShape(28.dp),
                     colors = ButtonDefaults.buttonColors(
                         containerColor = MaterialTheme.colorScheme.primaryContainer, // Use primary or white like screenshot
                         contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                     )
                 ) {
                     if (isLoading) {
                         CircularProgressIndicator(
                             modifier = Modifier.size(24.dp),
                             color = MaterialTheme.colorScheme.onPrimaryContainer
                         )
                     } else {
                         Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                     }
                 }
             }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading && searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), // 3 columns as per screenshot
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { artist ->
                        ArtistSelectionItem(
                            artist = artist,
                            isSelected = selectedArtists.any { it.id == artist.id },
                            onClick = { viewModel.toggleSelection(artist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistSelectionItem(
    artist: Artist,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
            // Artist Image
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.background(Color.DarkGray.copy(alpha = 0.6f)) // Dim effect when selected
                        else Modifier
                    )
            )
            
            // Selection Overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

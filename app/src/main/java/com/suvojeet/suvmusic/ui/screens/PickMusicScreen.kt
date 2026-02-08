package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.viewmodel.PickMusicUiState
import com.suvojeet.suvmusic.ui.viewmodel.PickMusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickMusicScreen(
    onBackClick: () -> Unit,
    onMixCreated: (List<Song>) -> Unit,
    viewModel: PickMusicViewModel = hiltViewModel(),
    onLoginClick: () -> Unit = {} // Add login navigation if possible
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedArtists by viewModel.selectedArtists.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle Login Required Popup
    if (uiState is PickMusicUiState.LoginRequired) {
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text("Login Required") },
            text = { Text("You need to be logged in to YouTube Music to create and save personalized mixes.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.resetState()
                    onLoginClick()
                }) {
                    Text("Login Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("Cancel")
                }
            }
        )
    }

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        }, 
        label = "PickMusicState"
    ) { state ->
        when (state) {
            is PickMusicUiState.Selection, is PickMusicUiState.LoginRequired -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .statusBarsPadding() // FIX: Add status bar padding
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
                                
                                IconButton(onClick = { /* Search Action */ }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                                
                                IconButton(onClick = onBackClick) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                            
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
                                     .height(56.dp)
                                     .navigationBarsPadding(), // Support nav bar padding
                                 shape = RoundedCornerShape(28.dp),
                                 colors = ButtonDefaults.buttonColors(
                                     containerColor = MaterialTheme.colorScheme.primaryContainer,
                                     contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                 )
                             ) {
                                Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Medium)
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
                                columns = GridCells.Fixed(3),
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
            
            is PickMusicUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            strokeWidth = 5.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Curating your mix...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            is PickMusicUiState.Success -> {
                val successState = state // Smart cast helper
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    
                    // Success Icon/Animation with Playlist Thumbnail
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Blurred background thumbnail
                        AsyncImage(
                            model = successState.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        
                        // Overlay Checkmark
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(
                                 imageVector = Icons.Default.Done,
                                 contentDescription = "Success",
                                 tint = Color.White,
                                 modifier = Modifier.size(32.dp)
                             )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = successState.message,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Your \"${successState.playlistName}\" is ready.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Share Button
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Check out my new mix!")
                                putExtra(Intent.EXTRA_TEXT, "Hey! I just created a new mix on SuvMusic: https://music.youtube.com/playlist?list=${successState.playlistId}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Playlist", fontSize = 18.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Done Button
                    Button(
                        onClick = { 
                            onBackClick() // Or navigate to playlist
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Done", fontSize = 18.sp)
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

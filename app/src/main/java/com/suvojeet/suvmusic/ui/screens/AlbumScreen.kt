package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.viewmodel.AlbumViewModel

@Composable
fun AlbumScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit = {},
    onShufflePlay: (List<Song>) -> Unit = {},
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val album = uiState.album

    // Check if we are in dark theme based on background luminance
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Define colors based on theme
    val backgroundColor = if (isDarkTheme) Color(0xFF0D0D0D) else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)

    // Track scroll position for collapsing header
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Blurred background image - Full screen like Apple Music
        if (album?.thumbnailUrl != null) {
            AsyncImage(
                model = album.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp),
                contentScale = ContentScale.Crop,
                alpha = if (isDarkTheme) 0.4f else 0.3f
            )
        }
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color.Transparent,
                                Color(0xFF0D0D0D).copy(alpha = 0.8f),
                                Color(0xFF0D0D0D)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.8f),
                                Color.White
                            )
                        }
                    )
                )
        )
        
        // Content
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "Unknown Error",
                    color = contentColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            album != null -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 60.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Album Header
                    item {
                        AlbumHeader(
                            album = album,
                            onPlayAll = { onPlayAll(album.songs) },
                            onShufflePlay = { onShufflePlay(album.songs) },
                            contentColor = contentColor,
                            secondaryContentColor = secondaryContentColor,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    
                    // Song List
                    itemsIndexed(album.songs) { index, song ->
                        AlbumSongItem(
                            song = song,
                            trackNumber = index + 1,
                            onClick = { onSongClick(album.songs, index) },
                            contentColor = contentColor,
                            secondaryContentColor = secondaryContentColor
                        )
                    }
                }
                
                // Top Bar (Fixed at top)
                AlbumTopBar(
                    title = album.title,
                    isScrolled = isScrolled,
                    onBackClick = onBackClick,
                    isDarkTheme = isDarkTheme,
                    contentColor = contentColor
                )
            }
        }
    }
}

@Composable
private fun AlbumTopBar(
    title: String,
    isScrolled: Boolean,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    contentColor: Color
) {
    // Determine background color when scrolled
    val scrolledColor = if (isDarkTheme) Color(0xFF1D1D1D) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                if (isScrolled) scrolledColor else Color.Transparent
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = contentColor
            )
        }
        
        // Show title when scrolled
        AnimatedVisibility(
            visible = isScrolled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        
        if (!isScrolled) {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = contentColor
            )
        }
    }
}

@Composable
private fun AlbumHeader(
    album: Album,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    contentColor: Color,
    secondaryContentColor: Color,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Artwork
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color.LightGray)
        ) {
            if (album.thumbnailUrl != null) {
                AsyncImage(
                    model = album.thumbnailUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Album Title
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Artist
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryContentColor,
            textAlign = TextAlign.Center
        )
        
        // Year and song count
        Text(
            text = buildString {
                album.year?.let { append("$it • ") }
                append("${album.songs.size} songs")
            },
            style = MaterialTheme.typography.bodySmall,
            color = secondaryContentColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Description (if available)
        if (!album.description.isNullOrBlank()) {
            Text(
                text = album.description,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Play & Shuffle Buttons
        val buttonContainerColor = if (isDarkTheme)
            Color.White.copy(alpha = 0.15f)
        else
            Color.Black.copy(alpha = 0.05f)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play Button
            Button(
                onClick = onPlayAll,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = contentColor
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play",
                    fontWeight = FontWeight.Medium
                )
            }

            // Shuffle Button
            Button(
                onClick = onShufflePlay,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = contentColor
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shuffle",
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AlbumSongItem(
    song: Song,
    trackNumber: Int,
    onClick: () -> Unit,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Song Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            if (song.thumbnailUrl != null) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryContentColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // More Options
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = secondaryContentColor.copy(alpha = 0.7f)
            )
        }
    }
}

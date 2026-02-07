package com.suvojeet.suvmusic.ui.screens

import com.suvojeet.suvmusic.ui.components.BounceButton

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import com.suvojeet.suvmusic.ui.components.PremiumLoadingScreen
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
    val batchProgress by viewModel.batchProgress.collectAsState()
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
    
    // Dialog/Menu states
    var showMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showSongMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedSong: Song? by remember { androidx.compose.runtime.mutableStateOf(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val shareAlbum: (Album) -> Unit = { albumToShare ->
        val shareText = "Check out this album: ${albumToShare.title} by ${albumToShare.artist}\n\nhttps://music.youtube.com/playlist?list=${albumToShare.id}"
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Album")
        androidx.core.content.ContextCompat.startActivity(context, shareIntent, null)
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
        
        when {
            uiState.isLoading -> {
                PremiumLoadingScreen(
                    thumbnailUrl = album?.thumbnailUrl,
                    onBackClick = onBackClick
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
                            batchProgress = batchProgress,
                            isSaved = uiState.isSaved,
                            onPlayAll = { onPlayAll(album.songs) },
                            onShufflePlay = { onShufflePlay(album.songs) },
                            onToggleSave = { viewModel.toggleSaveToLibrary() },
                            onDownload = { viewModel.downloadAlbum(album) },
                            onShare = { shareAlbum(album) },
                            onMoreClick = { showMenu = true },
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
                            onMoreClick = { 
                                selectedSong = song
                                showSongMenu = true 
                            },
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
                
                // Media Menu Bottom Sheet
                if (showMenu) {
                    com.suvojeet.suvmusic.ui.components.MediaMenuBottomSheet(
                        isVisible = showMenu,
                        onDismiss = { showMenu = false },
                        title = album.title,
                        subtitle = "${album.songs.size} songs",
                        thumbnailUrl = album.thumbnailUrl,
                        onShuffle = { onShufflePlay(album.songs) },
                        onStartRadio = { onShufflePlay(album.songs) },
                        onPlayNext = { viewModel.playNext(album.songs) },
                        onAddToQueue = { viewModel.addToQueue(album.songs) },
                        onAddToPlaylist = { /* TODO: Show add to playlist dialog */ },
                        onDownload = { viewModel.downloadAlbum(album) },
                        onShare = { shareAlbum(album) }
                    )
                }
                
                // Song Menu Bottom Sheet
                if (showSongMenu && selectedSong != null) {
                    val song = selectedSong!!
                    com.suvojeet.suvmusic.ui.components.SongMenuBottomSheet(
                        isVisible = showSongMenu,
                        onDismiss = { showSongMenu = false },
                        song = song,
                        onPlayNext = { viewModel.playNext(listOf(song)) },
                        onAddToQueue = { viewModel.addToQueue(listOf(song)) },
                        onAddToPlaylist = { /* TODO */ },
                        onDownload = { viewModel.downloadAlbum(album) }, // Using album download for now as context is album
                        onShare = { /* TODO share song */ }
                    )
                }
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
    }
}

@Composable
private fun AlbumHeader(
    album: Album,
    batchProgress: Pair<Int, Int>,
    isSaved: Boolean,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onToggleSave: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onMoreClick: () -> Unit,
    contentColor: Color,
    secondaryContentColor: Color,
    isDarkTheme: Boolean
) {
    val (current, total) = batchProgress
    val isDownloading = total > 0 && current < total
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
                append(com.suvojeet.suvmusic.util.TimeUtil.formatSongCountAndDuration(album.songs))
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

        // Batch Download Progress
        if (isDownloading) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Downloading $current / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions Row (Download, Save, Play, Share, More)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download Button
            BounceButton(
                onClick = onDownload,
                size = 48.dp,
                shape = CircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Album",
                    tint = contentColor
                )
            }

            // Save Button (Heart)
            BounceButton(
                onClick = onToggleSave,
                size = 48.dp,
                shape = CircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
                )
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isSaved) "Remove from Library" else "Save to Library",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else contentColor
                )
            }
            
            // Play Button (Large Central)
            BounceButton(
                onClick = onPlayAll,
                size = 72.dp,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier
                    .shadow(elevation = 12.dp, shape = androidx.compose.foundation.shape.CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // Share Button
            BounceButton(
                onClick = onShare,
                size = 48.dp,
                shape = CircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = contentColor
                )
            }
            
            // More Button
            BounceButton(
                onClick = onMoreClick,
                size = 48.dp,
                shape = CircleShape,
                modifier = Modifier.background(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = contentColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AlbumSongItem(
    song: Song,
    trackNumber: Int,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
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
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = secondaryContentColor.copy(alpha = 0.7f)
            )
        }
    }
}

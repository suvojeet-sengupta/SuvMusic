package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.data.model.DownloadState
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.Lyrics
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import android.content.Intent
import android.provider.Settings
import com.suvojeet.suvmusic.util.RingtoneHelper
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.LoadingArtworkOverlay
import com.suvojeet.suvmusic.ui.components.RingtoneProgressDialog
import com.suvojeet.suvmusic.ui.components.SongActionsSheet
import com.suvojeet.suvmusic.ui.components.SongCreditsSheet
import com.suvojeet.suvmusic.ui.components.WaveformSeeker
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import com.suvojeet.suvmusic.ui.components.SleepTimerSheet
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.player.SleepTimerManager
import com.suvojeet.suvmusic.player.SleepTimerOption
import com.suvojeet.suvmusic.ui.viewmodel.RingtoneViewModel
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import com.suvojeet.suvmusic.ui.utils.isLandscape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

/**
 * Premium full-screen player with Apple Music-style design.
 * Features dynamic colors, quality badges, and queue view.
 */
@Composable
fun PlayerScreen(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onToggleLike: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleVideoMode: () -> Unit = {},
    player: Player? = null,
    onPlayFromQueue: (Int) -> Unit = {},
    lyrics: Lyrics? = null,
    isFetchingLyrics: Boolean = false,
    // Sleep timer
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    onSetSleepTimer: (SleepTimerOption) -> Unit = {},
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel(),
    ringtoneViewModel: RingtoneViewModel = hiltViewModel()
) {

    val song = playerState.currentSong
    val context = LocalContext.current
    val playlistUiState by playlistViewModel.uiState.collectAsState()
    
    // Show toast messages from playlist operations
    LaunchedEffect(playlistUiState.successMessage) {
        playlistUiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            playlistViewModel.clearMessages()
        }
    }
    
    LaunchedEffect(playlistUiState.errorMessage) {
        playlistUiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            playlistViewModel.clearMessages()
        }
    }

    
    // Dynamic colors from album art
    val dominantColors = rememberDominantColors(song?.thumbnailUrl)
    
    // UI States
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showActionsSheet by remember { mutableStateOf(false) }
    var showCreditsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    
    // Ringtone states
    var showRingtoneProgress by remember { mutableStateOf(false) }
    var ringtoneProgress by remember { mutableStateOf(0f) }
    var ringtoneStatusMessage by remember { mutableStateOf("") }
    var ringtoneComplete by remember { mutableStateOf(false) }
    var ringtoneSuccess by remember { mutableStateOf(false) }
    
    // Coroutine scope for ringtone operation
    val coroutineScope = rememberCoroutineScope()
    
    // High-res thumbnail
    val highResThumbnail = getHighResThumbnail(song?.thumbnailUrl)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.secondary,
                        dominantColors.primary,
                        Color.Black
                    )
                )
            )
            .pointerInput(Unit) {
                var hasTriggeredBack = false
                detectVerticalDragGestures(
                    onDragStart = { hasTriggeredBack = false },
                    onDragEnd = { hasTriggeredBack = false },
                    onDragCancel = { hasTriggeredBack = false }
                ) { _, dragAmount ->
                    if (!hasTriggeredBack && dragAmount > 50) {
                        hasTriggeredBack = true
                        onBack()
                    }
                }
            }
    ) {
        // Main Player Content - Use BoxWithConstraints for dynamic adaptive layout
        // This responds to floating windows and resizing on tablets
        AnimatedVisibility(
            visible = !showQueue,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                // Use width-based breakpoint (500dp threshold)
                // This works for: landscape, tablets, floating windows
                val useWideLayout = maxWidth > 500.dp
                
                if (useWideLayout) {
                // Landscape Layout - Side by side
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Album Artwork
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumArtwork(
                            imageUrl = highResThumbnail,
                            title = song?.title,
                            dominantColors = dominantColors,
                            isLoading = playerState.isLoading,
                            onSwipeLeft = onNext,
                            onSwipeRight = onPrevious
                        )
                    }
                    
                    // Right side - Controls
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Top Bar - compact for landscape
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Close",
                                    tint = dominantColors.onBackground
                                )
                            }
                            Text(
                                text = "NOW PLAYING",
                                style = MaterialTheme.typography.labelMedium,
                                color = dominantColors.onBackground.copy(alpha = 0.7f),
                                letterSpacing = 2.sp
                            )
                            IconButton(onClick = { showQueue = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = dominantColors.onBackground
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Song Info
                        SongInfoSection(
                            song = song,
                            isFavorite = playerState.isLiked,
                            downloadState = playerState.downloadState,
                            onFavoriteClick = onToggleLike,
                            onDownloadClick = onDownload,
                            onMoreClick = { showActionsSheet = true },
                            dominantColors = dominantColors
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Progress & Waveform
                        WaveformSeeker(
                            progress = playerState.progress,
                            isPlaying = playerState.isPlaying,
                            onSeek = { progress ->
                                val newPosition = (progress * playerState.duration).toLong()
                                onSeekTo(newPosition)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            activeColor = dominantColors.accent,
                            inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f)
                        )
                        
                        // Time labels
                        TimeLabelsWithQuality(
                            currentPosition = playerState.currentPosition,
                            duration = playerState.duration,
                            dominantColors = dominantColors
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Playback Controls
                        PlaybackControls(
                            isPlaying = playerState.isPlaying,
                            shuffleEnabled = playerState.shuffleEnabled,
                            repeatMode = playerState.repeatMode,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onShuffleToggle = onShuffleToggle,
                            onRepeatToggle = onRepeatToggle,
                            dominantColors = dominantColors
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Bottom Actions
                        BottomActions(
                            onLyricsClick = { showLyrics = true },
                            onCastClick = { /* TODO */ },
                            onQueueClick = { showQueue = true },
                            dominantColors = dominantColors,
                            isYouTubeSong = song?.source == com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE,
                            isVideoMode = playerState.isVideoMode,
                            onVideoToggle = onToggleVideoMode
                        )
                    }
                }
            } else {
                // Portrait Layout - Original vertical layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar
                    PlayerTopBar(
                        onBack = onBack,
                        onShowQueue = { showQueue = true },
                        dominantColors = dominantColors
                    )
                    
                    Spacer(modifier = Modifier.weight(0.5f))
                    
                    // Album Art or Video Player - swipeable
                    if (playerState.isVideoMode && player != null) {
                        // Video Player
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(16f / 9f)
                                .shadow(
                                    elevation = 32.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = dominantColors.primary.copy(alpha = 0.5f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).apply {
                                        this.player = player
                                        useController = false
                                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                    }
                                },
                                update = { playerView ->
                                    playerView.player = player
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            LoadingArtworkOverlay(isVisible = playerState.isLoading)
                        }
                    } else {
                        // Album artwork
                        AlbumArtwork(
                            imageUrl = highResThumbnail,
                            title = song?.title,
                            dominantColors = dominantColors,
                            isLoading = playerState.isLoading,
                            onSwipeLeft = onNext,
                            onSwipeRight = onPrevious
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Song Info with actions
                    SongInfoSection(
                        song = song,
                        isFavorite = playerState.isLiked,
                        downloadState = playerState.downloadState,
                        onFavoriteClick = onToggleLike,
                        onDownloadClick = onDownload,
                        onMoreClick = { showActionsSheet = true },
                        dominantColors = dominantColors
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Progress & Waveform
                    WaveformSeeker(
                        progress = playerState.progress,
                        isPlaying = playerState.isPlaying,
                        onSeek = { progress ->
                            val newPosition = (progress * playerState.duration).toLong()
                            onSeekTo(newPosition)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        activeColor = dominantColors.accent,
                        inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f)
                    )
                    
                    // Time labels with quality badge
                    TimeLabelsWithQuality(
                        currentPosition = playerState.currentPosition,
                        duration = playerState.duration,
                        dominantColors = dominantColors
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Playback Controls
                    PlaybackControls(
                        isPlaying = playerState.isPlaying,
                        shuffleEnabled = playerState.shuffleEnabled,
                        repeatMode = playerState.repeatMode,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onShuffleToggle = onShuffleToggle,
                        onRepeatToggle = onRepeatToggle,
                        dominantColors = dominantColors
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bottom Actions
                    BottomActions(
                        onLyricsClick = { showLyrics = true },
                        onCastClick = { /* TODO */ },
                        onQueueClick = { showQueue = true },
                        dominantColors = dominantColors,
                        isYouTubeSong = song?.source == com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE,
                        isVideoMode = playerState.isVideoMode,
                        onVideoToggle = onToggleVideoMode
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
        }
        // Queue View
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            QueueView(
                currentSong = song,
                queue = playerState.queue,
                isPlaying = playerState.isPlaying,
                shuffleEnabled = playerState.shuffleEnabled,
                repeatMode = playerState.repeatMode,
                isAutoplayEnabled = playerState.isAutoplayEnabled,
                isFavorite = playerState.isLiked,
                onBack = { showQueue = false },
                onSongClick = { index -> onPlayFromQueue(index) },
                onPlayPause = onPlayPause,
                onToggleShuffle = onShuffleToggle,
                onToggleRepeat = onRepeatToggle,
                onToggleAutoplay = onToggleAutoplay,
                onToggleLike = onToggleLike,
                onMoreClick = { showActionsSheet = true },
                dominantColors = dominantColors
            )
        }
        
        // Lyrics View
        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            LyricsScreen(
                lyrics = lyrics,
                isFetching = isFetchingLyrics,
                currentTime = playerState.currentPosition,
                artworkUrl = highResThumbnail,
                onClose = { showLyrics = false }
            )
        }

        // Song Actions Bottom Sheet
        if (song != null) {
            SongActionsSheet(
                song = song,
                isVisible = showActionsSheet,
                onDismiss = { showActionsSheet = false },
                onToggleFavorite = onToggleLike,
                onDownload = onDownload,
                onViewCredits = { 
                    showActionsSheet = false
                    showCreditsSheet = true
                },
                onAddToPlaylist = {
                    showActionsSheet = false
                    playlistViewModel.showAddToPlaylistSheet(song)
                },
                onSleepTimer = {
                    showActionsSheet = false
                    showSleepTimerSheet = true
                },
                onSetRingtone = {
                    showActionsSheet = false
                    
                    if (song.id == null) return@SongActionsSheet
                    
                    // Check for WRITE_SETTINGS permission
                    if (!ringtoneViewModel.ringtoneHelper.hasWriteSettingsPermission(context)) {
                        Toast.makeText(context, "Permission required to set ringtone. Please grant it in settings.", Toast.LENGTH_LONG).show()
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                           Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                        }
                        return@SongActionsSheet
                    }
                    
                    // Start ringtone process
                    coroutineScope.launch {
                        showRingtoneProgress = true
                        ringtoneProgress = 0f
                        ringtoneStatusMessage = "Initializing..."
                        ringtoneComplete = false
                        ringtoneSuccess = false
                        
                        ringtoneViewModel.ringtoneHelper.downloadAndSetAsRingtone(
                            context = context,
                            song = song,
                            onProgress = { progress, message ->
                                ringtoneProgress = progress
                                ringtoneStatusMessage = message
                            },
                            onComplete = { success, message ->
                                ringtoneComplete = true
                                ringtoneSuccess = success
                                ringtoneStatusMessage = message
                            }
                        )
                    }
                }
            )
            
            // Ringtone Progress Dialog
            RingtoneProgressDialog(
                isVisible = showRingtoneProgress,
                progress = ringtoneProgress,
                statusMessage = ringtoneStatusMessage,
                isComplete = ringtoneComplete,
                isSuccess = ringtoneSuccess,
                onDismiss = { showRingtoneProgress = false }
            )
            
            // Song Credits Sheet
            SongCreditsSheet(
                song = song,
                isVisible = showCreditsSheet,
                onDismiss = { showCreditsSheet = false }
            )
            
            // Add to Playlist Sheet
            if (playlistUiState.showAddToPlaylistSheet && playlistUiState.selectedSong != null) {
                AddToPlaylistSheet(
                    song = playlistUiState.selectedSong!!,
                    isVisible = true,
                    playlists = playlistUiState.userPlaylists,
                    isLoading = playlistUiState.isLoadingPlaylists,
                    onDismiss = { playlistViewModel.hideAddToPlaylistSheet() },
                    onAddToPlaylist = { playlistId ->
                        playlistViewModel.addSongToPlaylist(playlistId)
                    },
                    onCreateNewPlaylist = {
                        playlistViewModel.showCreatePlaylistDialog()
                    }
                )
            }
            
            // Create Playlist Dialog
            CreatePlaylistDialog(
                isVisible = playlistUiState.showCreatePlaylistDialog,
                isCreating = playlistUiState.isCreatingPlaylist,
                onDismiss = { playlistViewModel.hideCreatePlaylistDialog() },
                onCreate = { title, description, isPrivate ->
                    playlistViewModel.createPlaylist(title, description, isPrivate)
                }
            )
            
            // Sleep Timer Sheet
            SleepTimerSheet(
                isVisible = showSleepTimerSheet,
                currentOption = sleepTimerOption,
                remainingTimeFormatted = sleepTimerRemainingMs?.let {
                    val minutes = it / 1000 / 60
                    val seconds = (it / 1000) % 60
                    String.format("%d:%02d", minutes, seconds)
                },
                onSelectOption = onSetSleepTimer,
                onDismiss = { showSleepTimerSheet = false },
                accentColor = dominantColors.accent
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    onBack: () -> Unit,
    onShowQueue: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )
        
        IconButton(onClick = onShowQueue) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = dominantColors.onBackground
            )
        }
    }
}

@Composable
private fun AlbumArtwork(
    imageUrl: String?,
    title: String?,
    dominantColors: DominantColors,
    isLoading: Boolean = false,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Track horizontal drag offset
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 150f // Minimum swipe distance to trigger action
    
    // Animate offset when dragging ends
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = if (offsetX == 0f) 200 else 0),
        label = "swipe_offset"
    )
    
    // Calculate scale and rotation based on offset for visual feedback
    val scale = 1f - (kotlin.math.abs(animatedOffsetX) / 1500f).coerceIn(0f, 0.1f)
    val rotation = animatedOffsetX / 30f
    
    BoxWithConstraints {
        // Use width-based check for dynamic resizing support
        val isWideLayout = maxWidth > 500.dp
        
        Box(
            modifier = Modifier
                .then(
                    if (isWideLayout) {
                        Modifier.fillMaxHeight(0.85f).aspectRatio(1f)
                    } else {
                        Modifier.fillMaxWidth(0.85f).aspectRatio(1f)
                    }
                )
            .graphicsLayer {
                translationX = animatedOffsetX
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX < -swipeThreshold -> {
                                // Swiped left - next song
                                onSwipeLeft()
                            }
                            offsetX > swipeThreshold -> {
                                // Swiped right - previous song
                                onSwipeRight()
                            }
                        }
                        // Reset offset
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    }
                )
            }
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = dominantColors.primary.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .size(600)
                    .build(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LoadingArtworkOverlay(
            isVisible = isLoading
        )
    }
    }
}

@Composable
private fun SongInfoSection(
    song: Song?,
    isFavorite: Boolean,
    downloadState: DownloadState,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onMoreClick: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song?.title ?: "No song playing",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = dominantColors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = dominantColors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Audio Quality Badge - Apple Music style
            if (song != null) {
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = dominantColors.onBackground.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = dominantColors.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (song.source) {
                            com.suvojeet.suvmusic.data.model.SongSource.JIOSAAVN -> "HQ Audio • 320kbps"
                            com.suvojeet.suvmusic.data.model.SongSource.LOCAL -> "Local"
                            else -> "YouTube Music"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        ),
                        color = dominantColors.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Download Button
        IconButton(onClick = onDownloadClick) {
             when(downloadState) {
                 DownloadState.DOWNLOADING -> {
                     CircularProgressIndicator(
                         modifier = Modifier.size(24.dp),
                         color = dominantColors.accent,
                         strokeWidth = 2.dp
                     )
                 }
                 DownloadState.DOWNLOADED -> {
                     Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = dominantColors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                 }
                 DownloadState.FAILED -> {
                     Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Retry Download",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                 }
                 else -> {
                     Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = dominantColors.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                 }
             }
        }

        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = "Favorite",
                tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }
        
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = dominantColors.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TimeLabelsWithQuality(
    currentPosition: Long,
    duration: Long,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDuration(currentPosition),
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f)
        )
        
        Text(
            text = "-${formatDuration(duration - currentPosition)}",
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Previous / Rewind
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Previous",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(44.dp)
            )
        }
        
        // Play/Pause
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(dominantColors.onBackground)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = dominantColors.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        
        // Next / Forward
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Next",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(44.dp)
            )
        }
        
        // Repeat
        IconButton(
            onClick = onRepeatToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BottomActions(
    onLyricsClick: () -> Unit,
    onCastClick: () -> Unit,
    onQueueClick: () -> Unit,
    dominantColors: DominantColors,
    isYouTubeSong: Boolean = false,
    isVideoMode: Boolean = false,
    onVideoToggle: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onLyricsClick) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics",
                tint = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }
        
        // Video mode toggle - only for YouTube songs
        if (isYouTubeSong) {
            IconButton(onClick = onVideoToggle) {
                Icon(
                    imageVector = if (isVideoMode) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (isVideoMode) "Switch to Audio" else "Switch to Video",
                    tint = if (isVideoMode) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            IconButton(onClick = onCastClick) {
                Icon(
                    imageVector = Icons.Default.Cast,
                    contentDescription = "Cast",
                    tint = dominantColors.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        
        IconButton(onClick = onQueueClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun QueueView(
    currentSong: Song?,
    queue: List<Song>,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isAutoplayEnabled: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleLike: () -> Unit,
    onMoreClick: () -> Unit,
    dominantColors: DominantColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.secondary,
                        dominantColors.primary,
                        Color.Black
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Current song header
        if (currentSong != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentSong.thumbnailUrl,
                    contentDescription = currentSong.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = dominantColors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = dominantColors.onBackground.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground
                    )
                }
                
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = dominantColors.onBackground
                    )
                }
            }
        }
        
        // Playback mode chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlaybackChip(
                text = "Shuffle",
                icon = Icons.Default.Shuffle,
                isSelected = shuffleEnabled,
                dominantColors = dominantColors,
                onClick = onToggleShuffle
            )
            PlaybackChip(
                text = "Repeat",
                icon = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                isSelected = repeatMode != RepeatMode.OFF,
                dominantColors = dominantColors,
                onClick = onToggleRepeat
            )
            PlaybackChip(
                text = "Autoplay",
                icon = Icons.Default.PlayArrow,
                isSelected = isAutoplayEnabled,
                dominantColors = dominantColors,
                onClick = onToggleAutoplay
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Queue header
        Text(
            text = "Continue Playing",
            style = MaterialTheme.typography.titleMedium,
            color = dominantColors.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = if (isAutoplayEnabled) "Autoplaying similar music" else "${queue.size} songs in queue",
            style = MaterialTheme.typography.bodySmall,
            color = dominantColors.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        // Queue list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(queue) { index, song ->
                QueueItem(
                    song = song,
                    isCurrent = song.id == currentSong?.id,
                    isPlaying = song.id == currentSong?.id && isPlaying,
                    onClick = { onSongClick(index) },
                    dominantColors = dominantColors
                )
            }
        }
        
        // Close button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close queue",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun PlaybackChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) dominantColors.onBackground.copy(alpha = 0.2f) 
               else dominantColors.onBackground.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = dominantColors.onBackground,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = dominantColors.onBackground
            )
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    onClick: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = dominantColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = dominantColors.onBackground.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = "Drag",
            tint = dominantColors.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun getHighResThumbnail(url: String?): String? {
    return url?.let {
        when {
            it.contains("ytimg.com") -> it
                .replace("hqdefault", "maxresdefault")
                .replace("mqdefault", "maxresdefault")
                .replace("sddefault", "maxresdefault")
                .replace("default", "maxresdefault")
                .replace(Regex("w\\d+-h\\d+"), "w544-h544")
            it.contains("lh3.googleusercontent.com") -> 
                it.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
                  .replace(Regex("=s\\d+"), "=s544")
            else -> it
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun QueueHeader(
    song: Song,
    dominantColors: DominantColors,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtwork(
            imageUrl = song.thumbnailUrl,
            title = song.title,
            dominantColors = dominantColors,
            isLoading = false
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = dominantColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = dominantColors.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onTogglePlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = dominantColors.primary
            )
        }
    }
}

@Composable
private fun QueueControlsRow(
    dominantColors: DominantColors,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isAutoplayEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoplay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
         IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f)
            )
        }
        IconButton(onClick = onToggleRepeat) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f)
            )
        }
        IconButton(onClick = onToggleAutoplay) {
             Icon(
                 imageVector = Icons.AutoMirrored.Filled.QueueMusic, // Fallback icon
                 contentDescription = "Autoplay",
                 tint = if (isAutoplayEnabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f)
             )
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrent) dominantColors.onBackground.copy(alpha = 0.1f) else Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
             Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) dominantColors.accent else dominantColors.onBackground,
                maxLines = 1
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = dominantColors.onBackground.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
        if (isPlaying) {
             Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Playing",
                tint = dominantColors.accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


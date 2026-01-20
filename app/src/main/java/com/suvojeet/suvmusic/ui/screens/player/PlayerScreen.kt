package com.suvojeet.suvmusic.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.widget.Toast
import com.suvojeet.suvmusic.data.model.Lyrics
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.player.SleepTimerOption
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import com.suvojeet.suvmusic.ui.components.LoadingArtworkOverlay
import com.suvojeet.suvmusic.ui.components.RingtoneProgressDialog
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.components.SleepTimerSheet
import com.suvojeet.suvmusic.ui.components.SongActionsSheet
import com.suvojeet.suvmusic.ui.components.SongCreditsSheet
import com.suvojeet.suvmusic.ui.components.PlaybackSpeedSheet
import com.suvojeet.suvmusic.ui.components.WaveformSeeker
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import com.suvojeet.suvmusic.ui.screens.LyricsScreen
import com.suvojeet.suvmusic.ui.screens.player.components.AlbumArtwork
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkShape
import com.suvojeet.suvmusic.ui.screens.player.components.BottomActions
import com.suvojeet.suvmusic.ui.screens.player.components.PlaybackControls
import com.suvojeet.suvmusic.ui.screens.player.components.PlayerTopBar
import com.suvojeet.suvmusic.ui.screens.player.components.QueueView
import com.suvojeet.suvmusic.ui.screens.player.components.SongInfoSection
import com.suvojeet.suvmusic.ui.screens.player.components.TimeLabelsWithQuality
import com.suvojeet.suvmusic.ui.screens.player.components.VolumeIndicator
import com.suvojeet.suvmusic.ui.screens.player.components.SystemVolumeObserver
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.RingtoneViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Premium full-screen player with Apple Music-style design.
 * Features dynamic colors, quality badges, and queue view.
 */
@Composable
fun PlayerScreen(
    playbackInfo: PlayerState,
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
    onStartRadio: () -> Unit = {},
    onLoadMoreRadioSongs: () -> Unit = {},
    isRadioMode: Boolean = false,
    isLoadingMoreSongs: Boolean = false,
    player: Player? = null,
    onPlayFromQueue: (Int) -> Unit = {},
    onSwitchDevice: (com.suvojeet.suvmusic.data.model.OutputDevice) -> Unit = {},
    onRefreshDevices: () -> Unit = {},
    onSetPlaybackSpeed: (Float) -> Unit = {},
    lyrics: Lyrics? = null,
    isFetchingLyrics: Boolean = false,
    comments: List<com.suvojeet.suvmusic.data.model.Comment>? = null,
    isFetchingComments: Boolean = false,
    isLoggedIn: Boolean = false,
    isPostingComment: Boolean = false,
    onPostComment: (String) -> Unit = {},
    isLoadingMoreComments: Boolean = false,
    onLoadMoreComments: () -> Unit = {},
    // Sleep timer
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    onSetSleepTimer: (SleepTimerOption, Int?) -> Unit = { _, _ -> },
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel(),
    ringtoneViewModel: RingtoneViewModel = hiltViewModel(),
    volumeKeyEvents: SharedFlow<Unit>? = null
) {

    val song = playbackInfo.currentSong
    val context = LocalContext.current
    val playlistUiState by playlistViewModel.uiState.collectAsState()
    
    // Customization styles from settings
    val sessionManager = remember { SessionManager(context) }
    val savedSeekbarStyleString by sessionManager.seekbarStyleFlow.collectAsState(initial = "WAVEFORM")
    val savedArtworkShapeString by sessionManager.artworkShapeFlow.collectAsState(initial = "ROUNDED_SQUARE")
    val volumeSliderEnabled by sessionManager.volumeSliderEnabledFlow.collectAsState(initial = true)
    
    val currentSeekbarStyle = try {
        SeekbarStyle.valueOf(savedSeekbarStyleString)
    } catch (e: Exception) {
        SeekbarStyle.WAVEFORM
    }
    
    val currentArtworkShape = try {
        ArtworkShape.valueOf(savedArtworkShapeString) 
    } catch (e: Exception) {
        ArtworkShape.ROUNDED_SQUARE
    }

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


    // Fix status bar color for immersive player
    // Force light icons (dark status bar) because player header is usually dark/colorful
    val view = LocalView.current
    val isAppInDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Dynamic colors from album art
    val dominantColors = rememberDominantColors(
        imageUrl = song?.thumbnailUrl,
        isDarkTheme = isAppInDarkTheme
    )
    
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        
        // In dark mode, force light icons (for dark background)
        // In light mode, force dark icons (for light background)
        insetsController.isAppearanceLightStatusBars = !isAppInDarkTheme
        
        onDispose {
            // Restore based on app theme
            // If app is dark, we want light icons (false)
            // If app is light, we want dark icons (true)
            insetsController.isAppearanceLightStatusBars = !isAppInDarkTheme
        }
    }

    // UI States
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showActionsSheet by remember { mutableStateOf(false) }
    var showCreditsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showOutputDeviceSheet by remember { mutableStateOf(false) }
    var showPlaybackSpeedSheet by remember { mutableStateOf(false) }

    // Ringtone states
    var showRingtoneProgress by remember { mutableStateOf(false) }
    var ringtoneProgress by remember { mutableStateOf(0f) }
    var ringtoneStatusMessage by remember { mutableStateOf("") }
    var ringtoneComplete by remember { mutableStateOf(false) }
    var ringtoneSuccess by remember { mutableStateOf(false) }

    // Coroutine scope for animations and async tasks
    val coroutineScope = rememberCoroutineScope()

    // Swipe to dismiss variables
    val offsetY = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx() }

    // High-res thumbnail
    val highResThumbnail = getHighResThumbnail(song?.thumbnailUrl)

    // Use pure black for dark mode, pure white for light mode
    val playerBackgroundColor = if (isAppInDarkTheme) Color.Black else Color.White

    // Volume control states
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    var maxVolume by remember {
        mutableStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
    }
    
    var currentVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var lastVolumeChangeTime by remember { mutableStateOf(0L) }
    
    // Listen for System Volume Changes
    SystemVolumeObserver(context = context) { newVol, newMax ->
        maxVolume = newMax
        if (currentVolume != newVol) {
            currentVolume = newVol
            lastVolumeChangeTime = System.currentTimeMillis()
        }
    }

    // Listen for Volume Key Events (Manual Trigger)
    LaunchedEffect(volumeKeyEvents) {
        volumeKeyEvents?.collect {
            // Update current volume (it might have changed, or not if at boundaries)
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            // Show indicator
            lastVolumeChangeTime = System.currentTimeMillis()
        }
    }

    // Auto-hide volume indicator
    LaunchedEffect(lastVolumeChangeTime) {
        if (lastVolumeChangeTime > 0) {
            showVolumeIndicator = true
            kotlinx.coroutines.delay(2000) // 2 seconds delay
            showVolumeIndicator = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Detect vertical drag on the entire screen container
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            // Threshold to dismiss: 20% of screen height
                            if (offsetY.value > screenHeightPx * 0.20f) {
                                // Animate off screen (200ms)
                                offsetY.animateTo(
                                    targetValue = screenHeightPx,
                                    animationSpec = tween(durationMillis = 200)
                                )
                                onBack()
                            } else {
                                // Snap back to top
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring()
                            )
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                        coroutineScope.launch {
                            offsetY.snapTo(newOffset)
                        }
                    }
                )
            }
    ) {
        // Background Layer with Fade Out effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(1f - (offsetY.value / screenHeightPx).coerceIn(0f, 1f))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dominantColors.secondary,
                            dominantColors.primary,
                            playerBackgroundColor
                        )
                    )
                )
        )

        // Volume Gesture Layer (Right side only - doesn't interfere with swipe dismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            // Activate on right 50% of screen (from center to right)
                            if (offset.x > size.width * 0.5f) {
                                lastVolumeChangeTime = System.currentTimeMillis()
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            // Only handle if on right side
                            if (change.position.x > size.width * 0.5f) {
                                change.consume()
                                // Adjust volume (drag up = increase, drag down = decrease)
                                val volumeChange = (-dragAmount / 30).roundToInt()
                                val newVolume = (currentVolume + volumeChange).coerceIn(0, maxVolume)
                                if (newVolume != currentVolume) {
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        newVolume,
                                        0
                                    )
                                    // currentVolume will be updated by observer, but update strictly for responsiveness
                                    currentVolume = newVolume
                                    lastVolumeChangeTime = System.currentTimeMillis()
                                } else {
                                    // Even if volume doesn't change (max/min), keep indicator alive
                                    lastVolumeChangeTime = System.currentTimeMillis()
                                }
                            }
                        },
                        onDragEnd = {
                            // Timer handles hiding
                        }
                    )
                }
        )

        // Main Content Layer that moves with drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = 0, y = offsetY.value.roundToInt()) }
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
                                    onSwipeRight = onPrevious,
                                    initialShape = currentArtworkShape,
                                    onShapeChange = { shape ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            sessionManager.setArtworkShape(shape.name)
                                        }
                                    }
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
                                    progressProvider = { playerState.progress },
                                    isPlaying = playbackInfo.isPlaying,
                                    onSeek = { progress ->
                                        val newPosition = (progress * playerState.duration).toLong()
                                        onSeekTo(newPosition)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    activeColor = dominantColors.accent,
                                    inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f),
                                    initialStyle = currentSeekbarStyle,
                                    onStyleChange = { style ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            sessionManager.setSeekbarStyle(style.name)
                                        }
                                    },
                                    duration = playerState.duration
                                )

                                // Time labels
                                TimeLabelsWithQuality(
                                    currentPositionProvider = { playerState.currentPosition },
                                    durationProvider = { playerState.duration },
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
                                    onCastClick = { showOutputDeviceSheet = true },
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
                                    onSwipeRight = onPrevious,
                                    initialShape = currentArtworkShape,
                                    onShapeChange = { shape ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            sessionManager.setArtworkShape(shape.name)
                                        }
                                    }
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
                                progressProvider = { playerState.progress },
                                isPlaying = playbackInfo.isPlaying,
                                onSeek = { progress ->
                                    val newPosition = (progress * playerState.duration).toLong()
                                    onSeekTo(newPosition)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                activeColor = dominantColors.accent,
                                inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f),
                                initialStyle = currentSeekbarStyle,
                                onStyleChange = { style ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        sessionManager.setSeekbarStyle(style.name)
                                    }
                                },
                                duration = playerState.duration
                            )

                            // Time labels with quality badge
                            TimeLabelsWithQuality(
                                currentPositionProvider = { playerState.currentPosition },
                                durationProvider = { playerState.duration },
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
                                onCastClick = { showOutputDeviceSheet = true },
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
                    isRadioMode = isRadioMode,
                    isLoadingMore = isLoadingMoreSongs,
                    onBack = { showQueue = false },
                    onSongClick = { index -> onPlayFromQueue(index) },
                    onPlayPause = onPlayPause,
                    onToggleShuffle = onShuffleToggle,
                    onToggleRepeat = onRepeatToggle,
                    onToggleAutoplay = onToggleAutoplay,
                    onToggleLike = onToggleLike,
                    onMoreClick = { showActionsSheet = true },
                    onLoadMore = onLoadMoreRadioSongs,
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
                    currentTimeProvider = { playerState.currentPosition },
                    artworkUrl = highResThumbnail,
                    onClose = { showLyrics = false },
                    isDarkTheme = isAppInDarkTheme,
                    onSeekTo = onSeekTo
                )
            }

            // Comments Sheet
            com.suvojeet.suvmusic.ui.screens.player.components.CommentsSheet(
                isVisible = showCommentsSheet,
                comments = comments,
                isLoading = isFetchingComments,
                onDismiss = { showCommentsSheet = false },
                accentColor = dominantColors.accent,
                isLoggedIn = isLoggedIn,
                isPostingComment = isPostingComment,
                onPostComment = onPostComment,
                isLoadingMore = isLoadingMoreComments,
                onLoadMore = onLoadMoreComments
            )
        }

        // Volume Indicator Overlay (Moved to end to appear on top)
        if (volumeSliderEnabled) {
            VolumeIndicator(
                isVisible = showVolumeIndicator,
                currentVolume = currentVolume,
                maxVolume = maxVolume,
                dominantColors = dominantColors,
                onVolumeChange = { newVolume ->
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        newVolume,
                        0
                    )
                    currentVolume = newVolume
                    lastVolumeChangeTime = System.currentTimeMillis()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
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
                onViewComments = {
                    showActionsSheet = false
                    showCommentsSheet = true
                },
                onSleepTimer = {
                    showActionsSheet = false
                    showSleepTimerSheet = true
                },
                onStartRadio = {
                    showActionsSheet = false
                    onStartRadio()
                },
                onPlaybackSpeed = {
                    showActionsSheet = false
                    showPlaybackSpeedSheet = true
                },
                currentSpeed = playerState.playbackSpeed,
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
                onSelectOption = { option, minutes -> 
                    onSetSleepTimer(option, minutes)
                },
                onDismiss = { showSleepTimerSheet = false },
                accentColor = dominantColors.accent
            )

            // Output Device Sheet
            com.suvojeet.suvmusic.ui.components.OutputDeviceSheet(
                isVisible = showOutputDeviceSheet,
                devices = playerState.availableDevices,
                onDeviceSelected = onSwitchDevice,
                onDismiss = { showOutputDeviceSheet = false },
                onRefreshDevices = onRefreshDevices,
                accentColor = dominantColors.accent
            )

            // Playback Speed Sheet
            PlaybackSpeedSheet(
                isVisible = showPlaybackSpeedSheet,
                currentSpeed = playerState.playbackSpeed,
                onDismiss = { showPlaybackSpeedSheet = false },
                onSpeedSelected = { speed ->
                    onSetPlaybackSpeed(speed)
                }
            )
        }
    }
}

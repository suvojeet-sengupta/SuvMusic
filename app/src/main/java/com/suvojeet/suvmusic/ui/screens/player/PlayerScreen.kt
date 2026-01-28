package com.suvojeet.suvmusic.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

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
import androidx.compose.ui.graphics.toArgb
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
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
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
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkSize
import com.suvojeet.suvmusic.ui.screens.player.components.BottomActions
import com.suvojeet.suvmusic.ui.screens.player.components.PlaybackControls
import com.suvojeet.suvmusic.ui.screens.player.components.PlayerTopBar
import com.suvojeet.suvmusic.ui.screens.player.components.QueueView
import com.suvojeet.suvmusic.ui.screens.player.components.SongInfoSection
import com.suvojeet.suvmusic.ui.components.VideoErrorDialog
import com.suvojeet.suvmusic.ui.screens.player.components.TimeLabelsWithQuality
import com.suvojeet.suvmusic.ui.screens.player.components.VolumeIndicator
import com.suvojeet.suvmusic.ui.screens.player.components.SystemVolumeObserver
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.RingtoneViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    onDismissVideoError: () -> Unit = {},
    onStartRadio: () -> Unit = {},
    onLoadMoreRadioSongs: () -> Unit = {},
    isRadioMode: Boolean = false,
    isLoadingMoreSongs: Boolean = false,
    player: Player? = null,
    onPlayFromQueue: (Int) -> Unit = {},
    onSwitchDevice: (com.suvojeet.suvmusic.data.model.OutputDevice) -> Unit = {},
    onRefreshDevices: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},

    onSetPlaybackParameters: (Float, Float) -> Unit = { _, _ -> },
    lyrics: Lyrics? = null,
    isFetchingLyrics: Boolean = false,
    comments: List<com.suvojeet.suvmusic.data.model.Comment>? = null,
    isFetchingComments: Boolean = false,
    isLoggedIn: Boolean = false,
    isPostingComment: Boolean = false,
    onPostComment: (String) -> Unit = {},
    isLoadingMoreComments: Boolean = false,
    onLoadMoreComments: () -> Unit = {},
    // Lyrics Provider
    selectedLyricsProvider: com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType = com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.AUTO,
    enabledLyricsProviders: Map<com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType, Boolean> = emptyMap(),
    onLyricsProviderChange: (com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType) -> Unit = {},
    // Sleep timer
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    onSetSleepTimer: (SleepTimerOption, Int?) -> Unit = { _, _ -> },
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel(),
    ringtoneViewModel: RingtoneViewModel = hiltViewModel(),
    playerViewModel: com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel = hiltViewModel(),
    onToggleDislike: () -> Unit = { playerViewModel.dislikeCurrentSong() },
    volumeKeyEvents: SharedFlow<Unit>? = null,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {

    val song = playbackInfo.currentSong
    val context = LocalContext.current
    val playlistUiState by playlistViewModel.uiState.collectAsState()
    
    // Customization styles from settings
    val sessionManager = remember { SessionManager(context) }
    val savedSeekbarStyleString by sessionManager.seekbarStyleFlow.collectAsState(initial = "WAVEFORM")
    val savedArtworkShapeString by sessionManager.artworkShapeFlow.collectAsState(initial = "ROUNDED_SQUARE")
    val savedArtworkSizeString by sessionManager.artworkSizeFlow.collectAsState(initial = "LARGE")
    val volumeSliderEnabled by sessionManager.volumeSliderEnabledFlow.collectAsState(initial = true)
    val doubleTapSeekSeconds by sessionManager.doubleTapSeekSecondsFlow.collectAsState(initial = 10)
    val keepScreenOn by sessionManager.keepScreenOnEnabledFlow.collectAsState(initial = false)
    val lyricsTextPosition by sessionManager.lyricsTextPositionFlow.collectAsState(initial = com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition.CENTER)
    val lyricsAnimationType by sessionManager.lyricsAnimationTypeFlow.collectAsState(initial = com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType.WORD)
    
    // Keep Screen On Logic
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        onDispose {
            // Always clear flag when leaving player screen (or when setting changes)
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
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
    
    val currentArtworkSize = try {
        ArtworkSize.valueOf(savedArtworkSizeString)
    } catch (e: Exception) {
        ArtworkSize.LARGE
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
    
    // Sync dominant color to PlayerViewModel for Widget
    // Sync dominant color to PlayerViewModel for Widget
    LaunchedEffect(dominantColors) {
        // Use primary or secondary color, ensure it's suitable for background
        val colorArgb = dominantColors.primary.toArgb()
        // Only update if it's a valid color (not transparent)
        if (colorArgb != 0) {
            playerViewModel.updateDominantColor(colorArgb)
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

    // Optimistic seek state for rapid double-taps
    var pendingSeekPosition by remember { mutableStateOf<Long?>(null) }
    var seekDebounceJob by remember { mutableStateOf<Job?>(null) }

    val handleDoubleTapSeek: (Boolean) -> Unit = { forward ->
        val current = pendingSeekPosition ?: playerState.currentPosition
        val seekAmount = doubleTapSeekSeconds * 1000L
        val newPos = if (forward) {
            (current + seekAmount).coerceAtMost(playerState.duration)
        } else {
            (current - seekAmount).coerceAtLeast(0)
        }
        pendingSeekPosition = newPos
        onSeekTo(newPos)

        // Reset pending position after 1 sec to sync back with real player
        seekDebounceJob?.cancel()
        seekDebounceJob = coroutineScope.launch {
            delay(1000)
            pendingSeekPosition = null
        }
    }

    // Swipe to dismiss variables
    val offsetY = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx() }



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

    val sharedModifier = Modifier

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
            .then(sharedModifier)
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
                                val artworkModifier = Modifier

                                AlbumArtwork(
                                    imageUrl = song?.thumbnailUrl,
                                    title = song?.title,
                                    dominantColors = dominantColors,
                                    isLoading = playerState.isLoading,
                                    onSwipeLeft = onNext,
                                    onSwipeRight = onPrevious,
                                    initialShape = currentArtworkShape,
                                    artworkSize = currentArtworkSize,
                                    onShapeChange = { shape ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            sessionManager.setArtworkShape(shape.name)
                                        }
                                    },
                                    onDoubleTapLeft = {
                                        handleDoubleTapSeek(false)
                                    },
                                    onDoubleTapRight = {
                                        handleDoubleTapSeek(true)
                                    },
                                    modifier = artworkModifier
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
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
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
                                // Video Player with Ambient Mode
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    // Ambient Glow Effect
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.95f)
                                            .aspectRatio(16f / 9f)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        dominantColors.primary.copy(alpha = 0.5f),
                                                        dominantColors.primary.copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                    
                                    // Main Player Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .shadow(
                                                elevation = 32.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                spotColor = dominantColors.primary.copy(alpha = 0.7f),
                                                ambientColor = dominantColors.primary.copy(alpha = 0.7f)
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
                                }
                            } else {
                                // Album artwork
                                val artworkModifier = Modifier

                                AlbumArtwork(
                                    imageUrl = song?.thumbnailUrl,
                                    title = song?.title,
                                    dominantColors = dominantColors,
                                    isLoading = playerState.isLoading,
                                    onSwipeLeft = onNext,
                                    onSwipeRight = onPrevious,
                                    initialShape = currentArtworkShape,
                                    artworkSize = currentArtworkSize,
                                    onShapeChange = { shape ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            sessionManager.setArtworkShape(shape.name)
                                        }
                                    },
                                    onDoubleTapLeft = {
                                        handleDoubleTapSeek(false)
                                    },
                                    onDoubleTapRight = {
                                        handleDoubleTapSeek(true)
                                    },
                                    modifier = artworkModifier
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
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                    dominantColors = dominantColors
                                )

                            if (playerState.isVideoMode) {
                                Spacer(modifier = Modifier.height(16.dp))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

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
                    artworkUrl = song?.thumbnailUrl,
                    onClose = { showLyrics = false },
                    isDarkTheme = isAppInDarkTheme,
                    onSeekTo = onSeekTo,
                    songTitle = song?.title ?: "",
                    artistName = song?.artist ?: "",
                    duration = playerState.duration,
                    selectedProvider = selectedLyricsProvider,
                    enabledProviders = enabledLyricsProviders,
                    onProviderChange = onLyricsProviderChange,
                    lyricsTextPosition = lyricsTextPosition,
                    lyricsAnimationType = lyricsAnimationType,
                    isPlaying = playerState.isPlaying,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious
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
                isDownloaded = playerState.downloadState == com.suvojeet.suvmusic.data.model.DownloadState.DOWNLOADED,
                onToggleFavorite = onToggleLike,
                onToggleDislike = onToggleDislike,
                isFavorite = playerState.isLiked,
                isDisliked = playerState.isDisliked,
                onDownload = onDownload,
                onDeleteDownload = {
                    playerViewModel.deleteDownload(song.id)
                },
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
                onDismiss = { showCreditsSheet = false },
                audioFormatDisplay = playerState.audioFormatDisplay
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
                currentPitch = playerState.pitch,
                onDismiss = { showPlaybackSpeedSheet = false },
                onApply = { speed, pitch ->
                    onSetPlaybackParameters(speed, pitch)
                }
            )
            
            // Video Error Dialog
            if (playerState.videoNotFound) {
                VideoErrorDialog(
                    onDismiss = onDismissVideoError,
                    onSwitchToAudio = {
                        onDismissVideoError()
                        onToggleVideoMode() // Switch back to audio
                    },
                    dominantColors = dominantColors
                )
            }
        }
    }
}

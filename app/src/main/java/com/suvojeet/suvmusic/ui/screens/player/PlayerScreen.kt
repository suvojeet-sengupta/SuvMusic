package com.suvojeet.suvmusic.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.text.style.TextAlign
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
import android.widget.Toast
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.player.SleepTimerOption
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.ui.components.CreatePlaylistDialog
import com.suvojeet.suvmusic.ui.screens.ListenTogetherScreen
import com.suvojeet.suvmusic.ui.components.LoadingArtworkOverlay
import com.suvojeet.suvmusic.ui.components.MeshGradientBackground
import com.suvojeet.suvmusic.ui.components.RingtoneProgressDialog
import com.suvojeet.suvmusic.ui.components.RingtoneTrimmerDialog
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.components.SleepTimerSheet
import com.suvojeet.suvmusic.ui.components.SongActionsSheet
import com.suvojeet.suvmusic.ui.components.SongInfoSheet
import com.suvojeet.suvmusic.ui.components.PlaybackSpeedSheet
import com.suvojeet.suvmusic.ui.components.WaveformSeeker
import com.suvojeet.suvmusic.ui.components.DominantColors
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
import com.suvojeet.suvmusic.ui.screens.player.components.VolumeControl
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.RingtoneViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

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
    mainViewModel: com.suvojeet.suvmusic.ui.viewmodel.MainViewModel = hiltViewModel(),
    onToggleDislike: () -> Unit = { playerViewModel.dislikeCurrentSong() },
    volumeKeyEvents: SharedFlow<Unit>? = null
) {

    val song = playbackInfo.currentSong
    val context = LocalContext.current
    val mainUiState by mainViewModel.uiState.collectAsState()
    val isInPip = mainUiState.isInPictureInPictureMode
    val playlistUiState by playlistViewModel.uiState.collectAsState()
    val sponsorSegments by playerViewModel.sponsorSegments.collectAsState(initial = emptyList())
    val lyricsState by playerViewModel.lyricsState.collectAsState()
    val isFetchingLyrics by playerViewModel.isFetchingLyrics.collectAsState()
    val isFullScreen by playerViewModel.isFullScreen.collectAsState()
    
    // Customization styles from settings
    val sessionManager = remember { SessionManager(context) }
    
    val savedSeekbarStyleString by sessionManager.seekbarStyleFlow.collectAsState(initial = "WAVE_LINE")
    val savedArtworkShapeString by sessionManager.artworkShapeFlow.collectAsState(initial = "ROUNDED_SQUARE")
    val savedArtworkSizeString by sessionManager.artworkSizeFlow.collectAsState(initial = "LARGE")
    val volumeSliderEnabled by sessionManager.volumeSliderEnabledFlow.collectAsState(initial = true)
    val doubleTapSeekSeconds by sessionManager.doubleTapSeekSecondsFlow.collectAsState(initial = 10)
    val keepScreenOn by sessionManager.keepScreenOnEnabledFlow.collectAsState(initial = false)
    val animatedBackgroundEnabled by sessionManager.playerAnimatedBackgroundFlow.collectAsState(initial = true)
    val lyricsTextPosition by sessionManager.lyricsTextPositionFlow.collectAsState(initial = com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition.CENTER)
    val lyricsAnimationType by sessionManager.lyricsAnimationTypeFlow.collectAsState(initial = com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType.WORD)
    val lyricsLineSpacing by sessionManager.lyricsLineSpacingFlow.collectAsState(initial = 1.5f)
    val lyricsFontSize by sessionManager.lyricsFontSizeFlow.collectAsState(initial = 26f)
    val audioArEnabled by sessionManager.audioArEnabledFlow.collectAsState(initial = false)
    
    val eqEnabled by playerViewModel.getEqEnabled().collectAsState(initial = false)
    val eqBands by playerViewModel.getEqBands().collectAsState(initial = FloatArray(10) { 0f })
    val eqPreamp by playerViewModel.getEqPreamp().collectAsState(initial = 0f)
    val bassBoost by playerViewModel.getBassBoost().collectAsState(initial = 0f)
    val virtualizer by playerViewModel.getVirtualizer().collectAsState(initial = 0f)
    
    // Keep Screen On Logic — only clear the flag if *we* were the ones who added it
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        val didAddFlag = keepScreenOn
        if (keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        onDispose {
            if (didAddFlag) {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    
    val currentSeekbarStyle = remember(savedSeekbarStyleString) {
        runCatching { SeekbarStyle.valueOf(savedSeekbarStyleString) }
            .getOrDefault(SeekbarStyle.WAVE_LINE)
    }
    
    val currentArtworkShape = remember(savedArtworkShapeString) {
        runCatching { ArtworkShape.valueOf(savedArtworkShapeString) }
            .getOrDefault(ArtworkShape.ROUNDED_SQUARE)
    }
    
    val currentArtworkSize = remember(savedArtworkSizeString) {
        runCatching { ArtworkSize.valueOf(savedArtworkSizeString) }
            .getOrDefault(ArtworkSize.LARGE)
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
    val extractedColors = rememberDominantColors(
        imageUrl = song?.thumbnailUrl,
        isDarkTheme = isAppInDarkTheme
    )
    
    val rawColors = extractedColors

    // Smooth color transitions when the song changes — crossfade instead of snapping
    val animatedPrimary by animateColorAsState(rawColors.primary, tween(800), label = "primary")
    val animatedSecondary by animateColorAsState(rawColors.secondary, tween(800), label = "secondary")
    val animatedAccent by animateColorAsState(rawColors.accent, tween(800), label = "accent")
    val animatedOnBg by animateColorAsState(rawColors.onBackground, tween(800), label = "onBg")

    val dominantColors = DominantColors(
        primary = animatedPrimary,
        secondary = animatedSecondary,
        accent = animatedAccent,
        onBackground = animatedOnBg
    )
    
    // Fix status bar: capture previous state and restore it on dispose
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
        
        insetsController.isAppearanceLightStatusBars = !isAppInDarkTheme
        
        onDispose {
            insetsController.isAppearanceLightStatusBars = previousLightStatusBars
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

    // Single overlay state — only one sheet/overlay can be visible at a time
    var activeOverlay by remember { mutableStateOf<PlayerOverlay>(PlayerOverlay.None) }

    // Convenience booleans derived from the sealed state (read-only)
    val showQueue = activeOverlay is PlayerOverlay.Queue
    val showLyrics = activeOverlay is PlayerOverlay.Lyrics
    val showCommentsSheet = activeOverlay is PlayerOverlay.Comments
    val showActionsSheet = activeOverlay is PlayerOverlay.Actions
    val selectedSongForMenu = (activeOverlay as? PlayerOverlay.Actions)?.targetSong
    val showInfoSheet = activeOverlay is PlayerOverlay.SongInfo
    val showSleepTimerSheet = activeOverlay is PlayerOverlay.SleepTimer
    val showOutputDeviceSheet = activeOverlay is PlayerOverlay.OutputDevice
    val showPlaybackSpeedSheet = activeOverlay is PlayerOverlay.PlaybackSpeed
    val showListenTogetherSheet = activeOverlay is PlayerOverlay.ListenTogether
    val showEqualizerSheet = activeOverlay is PlayerOverlay.Equalizer

    // Ringtone states (independent — these run in parallel with overlays)
    var showRingtoneTrimmer by remember { mutableStateOf(false) }
    var showRingtoneProgress by remember { mutableStateOf(false) }
    var ringtoneProgress by remember { mutableStateOf(0f) }
    var ringtoneStatusMessage by remember { mutableStateOf("") }
    var ringtoneComplete by remember { mutableStateOf(false) }
    var ringtoneSuccess by remember { mutableStateOf(false) }
    var savedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Coroutine scope for animations and async tasks
    val coroutineScope = rememberCoroutineScope()

    // Back Handler to handle overlays and minimize player
    BackHandler(enabled = true) {
        if (activeOverlay != PlayerOverlay.None) {
            activeOverlay = PlayerOverlay.None
        } else {
            onBack()
        }
    }

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
        // Update UI optimistically — no seek fired yet
        pendingSeekPosition = newPos

        // Debounce the actual Media3 seek so rapid taps consolidate into one call
        seekDebounceJob?.cancel()
        seekDebounceJob = coroutineScope.launch {
            delay(400) // short window to batch consecutive taps
            onSeekTo(newPos)
            delay(600) // let player catch up before clearing optimistic state
            pendingSeekPosition = null
        }
    }

    val configuration = LocalConfiguration.current



    // Use pure black for dark mode, pure white for light mode
    val playerBackgroundColor = if (isAppInDarkTheme) Color.Black else Color.White

    // Volume state is fully managed inside VolumeControl — no top-level reads here

    if (isInPip) {
        // Simplified PiP UI — show video if in video mode, else album art
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (playerState.isVideoMode && player != null) {
                // Render actual video in PiP
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
            } else {
                // Audio mode — show album art
                val song = playbackInfo.currentSong
                if (song?.thumbnailUrl != null) {
                    coil.compose.AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = 0.6f
                    )
                }

                // Minimal Info Overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    Text(
                        text = song?.title ?: "Unknown",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = song?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Layer
            if (animatedBackgroundEnabled && !playerState.isVideoMode) {
                MeshGradientBackground(
                    dominantColors = dominantColors
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
            }

        // Main Content Layer
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main Player Content - Use BoxWithConstraints for dynamic adaptive layout
            // This responds to floating windows and resizing on tablets
            // BoxWithConstraints outside AnimatedVisibility to avoid re-measuring
            // constraints on every frame of the enter/exit animation
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
            AnimatedVisibility(
                visible = !showQueue,
                enter = fadeIn(),
                exit = fadeOut()
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
                                    songId = song?.id,
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
                                    IconButton(onClick = { activeOverlay = PlayerOverlay.Queue }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = "Queue",
                                            tint = dominantColors.onBackground
                                        )
                                    }
                                    
                                    if (audioArEnabled) {
                                        IconButton(onClick = { playerViewModel.calibrateAudioAr() }) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Recenter Audio",
                                                tint = dominantColors.onBackground
                                            )
                                        }
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
                                    onMoreClick = { 
                                        activeOverlay = PlayerOverlay.Actions(song)
                                    },
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
                                    duration = playerState.duration,
                                    sponsorSegments = sponsorSegments
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

                                BottomActions(
                                    onLyricsClick = { activeOverlay = PlayerOverlay.Lyrics },
                                    onCastClick = { activeOverlay = PlayerOverlay.OutputDevice },
                                    onQueueClick = { activeOverlay = PlayerOverlay.Queue },
                                    dominantColors = dominantColors,
                                    isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                                    isVideoMode = playerState.isVideoMode,
                                    onVideoToggle = onToggleVideoMode
                                )
                            }
                        }
                    } else {
                        // Portrait Layout - Adaptive vertical layout
                        // Detect compact screens (16:9 and similar short aspect ratios)
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val availableHeight = maxHeight
                            val isCompactHeight = availableHeight < 600.dp

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .padding(horizontal = if (isCompactHeight) 16.dp else 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Top Bar
                                PlayerTopBar(
                                    onBack = onBack,
                                    dominantColors = dominantColors,
                                    audioArEnabled = audioArEnabled,
                                    onRecenter = { playerViewModel.calibrateAudioAr() }
                                )

                                // Album Art or Video Player
                                if (playerState.isVideoMode && player != null && !isFullScreen) {
                                    // Video container - limit height on compact screens so controls stay visible
                                    Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isCompactHeight) {
                                                    Modifier.weight(1f, fill = false)
                                                        .fillMaxHeight(0.40f)
                                                } else {
                                                    Modifier.weight(1f)
                                                }
                                            )
                                    ) {
                                        // Ambient Glow Effect
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.95f)
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
                                        
                                        // Main Player Box with AndroidView
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.Black)
                                                .clickable { playerViewModel.setFullScreen(true) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Capture the PlayerView reference for proper cleanup
                                            var embeddedPlayerView by remember { mutableStateOf<PlayerView?>(null) }

                                            AndroidView(
                                                factory = { context ->
                                                    PlayerView(context).apply {
                                                        this.player = player
                                                        useController = false
                                                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                                        embeddedPlayerView = this
                                                    }
                                                },
                                                update = { playerView ->
                                                    playerView.player = player
                                                    embeddedPlayerView = playerView
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Release the Player reference so FullScreenVideoPlayer
                                            // can take exclusive ownership of the video surface.
                                            DisposableEffect(Unit) {
                                                onDispose {
                                                    embeddedPlayerView?.player = null
                                                }
                                            }

                                            LoadingArtworkOverlay(isVisible = playerState.isLoading)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))
                                } else {
                                    // Album artwork (audio mode)
                                    Spacer(modifier = Modifier.weight(1f))
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
                                        songId = song?.id,
                                        modifier = artworkModifier
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                // Song Info with actions
                                SongInfoSection(
                                    song = song,
                                    isFavorite = playerState.isLiked,
                                    downloadState = playerState.downloadState,
                                    onFavoriteClick = onToggleLike,
                                    onDownloadClick = onDownload,
                                    onMoreClick = { 
                                        activeOverlay = PlayerOverlay.Actions(song)
                                    },
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                    dominantColors = dominantColors,
                                    compact = isCompactHeight
                                )

                                // Adaptive spacer between song info and seekbar
                                Spacer(modifier = Modifier.height(
                                    if (isCompactHeight) {
                                        if (playerState.isVideoMode) 8.dp else 12.dp
                                    } else {
                                        if (playerState.isVideoMode) 16.dp else 24.dp
                                    }
                                ))

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
                                    duration = playerState.duration,
                                    sponsorSegments = sponsorSegments
                                )

                                // Time labels with quality badge
                                TimeLabelsWithQuality(
                                    currentPositionProvider = { playerState.currentPosition },
                                    durationProvider = { playerState.duration },
                                    dominantColors = dominantColors
                                )

                                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 24.dp))

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
                                    dominantColors = dominantColors,
                                    compact = isCompactHeight
                                )

                                Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 16.dp))

                                // Bottom Actions
                                BottomActions(
                                    onLyricsClick = { activeOverlay = PlayerOverlay.Lyrics },
                                    onCastClick = { activeOverlay = PlayerOverlay.OutputDevice },
                                    onQueueClick = { activeOverlay = PlayerOverlay.Queue },
                                    dominantColors = dominantColors,
                                    isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                                    isVideoMode = playerState.isVideoMode,
                                    onVideoToggle = onToggleVideoMode,
                                    compact = isCompactHeight
                                )

                                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 24.dp))
                            }
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
                    currentIndex = playerState.currentIndex,
                    isPlaying = playerState.isPlaying,
                    shuffleEnabled = playerState.shuffleEnabled,
                    repeatMode = playerState.repeatMode,
                    isAutoplayEnabled = playerState.isAutoplayEnabled,
                    isFavorite = playerState.isLiked,
                    isRadioMode = isRadioMode,
                    isLoadingMore = isLoadingMoreSongs,
                    onBack = { activeOverlay = PlayerOverlay.None },
                    onSongClick = { index -> onPlayFromQueue(index) },
                    onPlayPause = onPlayPause,
                    onToggleShuffle = onShuffleToggle,
                    onToggleRepeat = onRepeatToggle,
                    onToggleAutoplay = onToggleAutoplay,
                    onToggleLike = onToggleLike,
                    onMoreClick = { targetSong -> 
                        activeOverlay = PlayerOverlay.Actions(targetSong)
                    },
                    onLoadMore = onLoadMoreRadioSongs,
                    onMoveItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
                    onRemoveItems = { indices -> playerViewModel.removeQueueItems(indices) },
                    onSaveAsPlaylist = { title, desc, isPrivate, syncWithYt ->
                        playerViewModel.saveQueueAsPlaylist(title, desc, isPrivate, syncWithYt) { success ->
                            if (success) {
                                Toast.makeText(context, "Queue saved as playlist", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to save queue", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    dominantColors = dominantColors,
                    animatedBackgroundEnabled = animatedBackgroundEnabled,
                    isDarkTheme = isAppInDarkTheme
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
                    onClose = { activeOverlay = PlayerOverlay.None },
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
                    lyricsLineSpacing = lyricsLineSpacing,
                    lyricsFontSize = lyricsFontSize,
                    onLineSpacingChange = { coroutineScope.launch { sessionManager.setLyricsLineSpacing(it) } },
                    onFontSizeChange = { coroutineScope.launch { sessionManager.setLyricsFontSize(it) } },
                    onTextPositionChange = { coroutineScope.launch { sessionManager.setLyricsTextPosition(it) } },
                    onAnimationTypeChange = { coroutineScope.launch { sessionManager.setLyricsAnimationType(it) } },
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
                onDismiss = { activeOverlay = PlayerOverlay.None },
                accentColor = dominantColors.accent,
                isLoggedIn = isLoggedIn,
                isPostingComment = isPostingComment,
                onPostComment = onPostComment,
                isLoadingMore = isLoadingMoreComments,
                onLoadMore = onLoadMoreComments
            )
        }

        // Volume Indicator Overlay — self-contained, no state leaks into PlayerScreen
        if (volumeSliderEnabled) {
            VolumeControl(
                dominantColors = dominantColors,
                volumeKeyEvents = volumeKeyEvents,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )
        }

        // Song Actions Bottom Sheet
        val menuSong = selectedSongForMenu ?: song
        if (menuSong != null) {
            SongActionsSheet(
                song = menuSong,
                isVisible = showActionsSheet,
                onDismiss = { 
                    if (activeOverlay is PlayerOverlay.Actions) {
                        activeOverlay = PlayerOverlay.None
                    }
                },
                dominantColors = dominantColors,
                isDownloaded = if (menuSong.id == song?.id) playerState.downloadState == com.suvojeet.suvmusic.data.model.DownloadState.DOWNLOADED else playerViewModel.isDownloaded(menuSong.id),
                onToggleFavorite = {
                    if (menuSong.id == song?.id) onToggleLike()
                    else playerViewModel.likeSong(menuSong)
                },
                onToggleDislike = {
                    if (menuSong.id == song?.id) onToggleDislike()
                    else playerViewModel.dislikeCurrentSong() // Assuming dislike only for current for now
                },
                isFavorite = if (menuSong.id == song?.id) playerState.isLiked else false,
                isDisliked = if (menuSong.id == song?.id) playerState.isDisliked else false,
                onDownload = {
                    if (menuSong.id == song?.id) onDownload()
                    else {
                        // Handle download for non-current song
                        com.suvojeet.suvmusic.service.DownloadService.startDownload(context, menuSong)
                    }
                },
                onDeleteDownload = {
                    playerViewModel.deleteDownload(menuSong.id)
                },
                onPlayNext = {
                    playerViewModel.playNext(menuSong)
                    Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
                },
                onAddToQueue = {
                    playerViewModel.addToQueue(menuSong)
                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                },
                onViewInfo = {
                    activeOverlay = PlayerOverlay.SongInfo
                },
                onAddToPlaylist = {
                    activeOverlay = PlayerOverlay.None
                    playlistViewModel.showAddToPlaylistSheet(menuSong)
                },
                onViewComments = {
                    activeOverlay = PlayerOverlay.Comments
                },
                onSleepTimer = {
                    activeOverlay = PlayerOverlay.SleepTimer
                },
                onStartRadio = {
                    playerViewModel.startRadio(menuSong)
                },
                onListenTogether = {
                    activeOverlay = PlayerOverlay.ListenTogether
                },
                onPlaybackSpeed = {
                    activeOverlay = PlayerOverlay.PlaybackSpeed
                },
                onEqualizerClick = {
                    activeOverlay = PlayerOverlay.Equalizer
                },
                currentSpeed = playerState.playbackSpeed,
                onMoveUp = if (showQueue && playerState.queue.indexOf(menuSong) > 0) {
                    { playerViewModel.moveQueueItem(playerState.queue.indexOf(menuSong), playerState.queue.indexOf(menuSong) - 1) }
                } else null,
                onMoveDown = if (showQueue && playerState.queue.indexOf(menuSong) < playerState.queue.size - 1 && playerState.queue.indexOf(menuSong) != -1) {
                    { playerViewModel.moveQueueItem(playerState.queue.indexOf(menuSong), playerState.queue.indexOf(menuSong) + 1) }
                } else null,
                onRemoveFromQueue = if (showQueue) {
                    { playerViewModel.removeQueueItems(listOf(playerState.queue.indexOf(menuSong))) }
                } else null,
                isFromQueue = showQueue,
                onSetRingtone = {
                    activeOverlay = PlayerOverlay.None
                    showRingtoneTrimmer = true
                }
            )

            // Ringtone Trimmer Dialog
            RingtoneTrimmerDialog(
                isVisible = showRingtoneTrimmer,
                song = menuSong,
                onDismiss = { showRingtoneTrimmer = false },
                onResolveStreamUrl = { videoId ->
                    ringtoneViewModel.getStreamUrl(videoId)
                },
                onConfirm = { startMs, endMs ->
                    showRingtoneTrimmer = false
                    // Start ringtone process
                    coroutineScope.launch {
                        showRingtoneProgress = true
                        ringtoneProgress = 0f
                        ringtoneStatusMessage = "Initializing..."
                        ringtoneComplete = false
                        ringtoneSuccess = false
                        var savedUri: android.net.Uri? = null

                        ringtoneViewModel.ringtoneHelper.downloadAndTrimAsRingtone(
                            context = context,
                            song = menuSong,
                            startMs = startMs,
                            endMs = endMs,
                            onProgress = { progress, message ->
                                ringtoneProgress = progress
                                ringtoneStatusMessage = message
                            },
                            onComplete = { success, message, uri ->
                                ringtoneComplete = true
                                ringtoneSuccess = success
                                ringtoneStatusMessage = message
                                savedUri = uri
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
                onDismiss = { showRingtoneProgress = false },
                onOpenSettings = {
                    ringtoneViewModel.ringtoneHelper.openRingtoneSettings(context, savedUri)
                }
            )

            // Song Info (Credits) Sheet
            if (song != null) {
                SongInfoSheet(
                    song = song,
                    isVisible = showInfoSheet,
                    onDismiss = { activeOverlay = PlayerOverlay.None },
                    onArtistClick = onArtistClick,
                    audioCodec = playerState.audioCodec,
                    audioBitrate = playerState.audioBitrate
                )
            }

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
                onCreate = { title, description, isPrivate, syncWithYt ->
                    playlistViewModel.createPlaylist(title, description, isPrivate, syncWithYt)
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
                onDismiss = { activeOverlay = PlayerOverlay.None },
                accentColor = dominantColors.accent
            )

            // Output Device Sheet
            com.suvojeet.suvmusic.ui.components.OutputDeviceSheet(
                isVisible = showOutputDeviceSheet,
                devices = playerState.availableDevices,
                onDeviceSelected = onSwitchDevice,
                onDismiss = { activeOverlay = PlayerOverlay.None },
                onRefreshDevices = onRefreshDevices,
                accentColor = dominantColors.accent
            )

            // Playback Speed Sheet
            PlaybackSpeedSheet(
                isVisible = showPlaybackSpeedSheet,
                currentSpeed = playerState.playbackSpeed,
                currentPitch = playerState.pitch,
                onDismiss = { activeOverlay = PlayerOverlay.None },
                onApply = { speed, pitch ->
                    onSetPlaybackParameters(speed, pitch)
                }
            )
            
            // Equalizer Sheet
            AnimatedVisibility(
                visible = showEqualizerSheet,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                com.suvojeet.suvmusic.ui.components.EqualizerSheet(
                    isVisible = true,
                    onDismiss = { activeOverlay = PlayerOverlay.None },
                    dominantColor = dominantColors.accent,
                    onEnabledChange = { enabled -> playerViewModel.setEqEnabled(enabled) },
                    onBandChange = { band, gain -> playerViewModel.setEqBandGain(band, gain) },
                    onBandsChange = { bands -> playerViewModel.setEqBands(bands) },
                    onPreampChange = { gain -> playerViewModel.setEqPreamp(gain) },
                    onBassBoostChange = { strength -> playerViewModel.setBassBoost(strength) },
                    onVirtualizerChange = { strength -> playerViewModel.setVirtualizer(strength) },
                    onReset = { playerViewModel.resetEqBands() },
                    initialEnabled = eqEnabled,
                    initialBands = eqBands,
                    initialPreamp = eqPreamp,
                    initialBassBoost = bassBoost,
                    initialVirtualizer = virtualizer
                )
            }
            
            // Listen Together Sheet
            AnimatedVisibility(
                visible = showListenTogetherSheet,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                ListenTogetherScreen(
                    onDismiss = { activeOverlay = PlayerOverlay.None },
                    dominantColors = dominantColors
                )
            }
            
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

        // Fullscreen Video Overlay — inside the main UI hierarchy
        if (isFullScreen) {
            FullScreenVideoPlayer(
                viewModel = playerViewModel,
                dominantColors = dominantColors,
                onDismiss = { playerViewModel.setFullScreen(false) }
            )
        }
    }
}
}

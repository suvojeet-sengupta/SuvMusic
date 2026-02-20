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
import com.suvojeet.suvmusic.ui.screens.player.components.VolumeIndicator
import com.suvojeet.suvmusic.ui.screens.player.components.SystemVolumeObserver
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.RingtoneViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
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
    
    val savedSeekbarStyleString by sessionManager.seekbarStyleFlow.collectAsState(initial = "WAVEFORM")
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
    
    val currentSeekbarStyle = remember(savedSeekbarStyleString) {
        runCatching { SeekbarStyle.valueOf(savedSeekbarStyleString) }
            .getOrDefault(SeekbarStyle.WAVEFORM)
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
    
    val dominantColors = extractedColors
    
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
    var selectedSongForMenu by remember { mutableStateOf<com.suvojeet.suvmusic.core.model.Song?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showOutputDeviceSheet by remember { mutableStateOf(false) }
    var showPlaybackSpeedSheet by remember { mutableStateOf(false) }
    var showListenTogetherSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }

    // Ringtone states
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
        when {
            showQueue -> showQueue = false
            showLyrics -> showLyrics = false
            showListenTogetherSheet -> showListenTogetherSheet = false
            showCommentsSheet -> showCommentsSheet = false
            showActionsSheet -> showActionsSheet = false
            showInfoSheet -> showInfoSheet = false
            showSleepTimerSheet -> showSleepTimerSheet = false
            showOutputDeviceSheet -> showOutputDeviceSheet = false
            showPlaybackSpeedSheet -> showPlaybackSpeedSheet = false
            showEqualizerSheet -> showEqualizerSheet = false
            else -> onBack()
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
        pendingSeekPosition = newPos
        onSeekTo(newPos)

        // Reset pending position after 1 sec to sync back with real player
        seekDebounceJob?.cancel()
        seekDebounceJob = coroutineScope.launch {
            delay(1000)
            pendingSeekPosition = null
        }
    }

    val configuration = LocalConfiguration.current



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

    if (isInPip) {
        // Simplified PiP UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Album Art background
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
                                    IconButton(onClick = { showQueue = true }) {
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
                                        selectedSongForMenu = song
                                        showActionsSheet = true 
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

                                // Bottom Actions
                                BottomActions(
                                    onLyricsClick = { showLyrics = true },
                                    onCastClick = { showOutputDeviceSheet = true },
                                    onQueueClick = { showQueue = true },
                                    dominantColors = dominantColors,
                                    isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
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
                                dominantColors = dominantColors,
                                audioArEnabled = audioArEnabled,
                                onRecenter = { playerViewModel.calibrateAudioAr() }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Album Art or Video Player - swipeable
                            if (playerState.isVideoMode && player != null && !isFullScreen) {
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
                                    
                                    // Main Player Box with AndroidView
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black)
                                            .clickable { playerViewModel.setFullScreen(true) },
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

                                        // Explicitly detach player when this PlayerView leaves composition
                                        DisposableEffect(Unit) {
                                            onDispose {
                                                // Detach player to prevent conflicts with FullScreenVideoPlayer
                                                // We can't easily access the PlayerView from here to set player = null
                                                // but the factory/update logic above will be cleaned up by Compose.
                                                // In some cases, a more explicit detachment is needed:
                                            }
                                        }

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
                                    songId = song?.id,
                                    modifier = artworkModifier
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Song Info with actions
                                SongInfoSection(
                                    song = song,
                                    isFavorite = playerState.isLiked,
                                    downloadState = playerState.downloadState,
                                    onFavoriteClick = onToggleLike,
                                    onDownloadClick = onDownload,
                                    onMoreClick = { 
                                        selectedSongForMenu = song
                                        showActionsSheet = true 
                                    },
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                    dominantColors = dominantColors
                                )

                            if (playerState.isVideoMode) {
                                Spacer(modifier = Modifier.height(16.dp))
                            } else {
                                Spacer(modifier = Modifier.height(24.dp))
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
                                duration = playerState.duration,
                                sponsorSegments = sponsorSegments
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
                                isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
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
                    currentIndex = playerState.currentIndex,
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
                    onMoreClick = { targetSong -> 
                        selectedSongForMenu = targetSong
                        showActionsSheet = true 
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
        val menuSong = selectedSongForMenu ?: song
        if (menuSong != null) {
            SongActionsSheet(
                song = menuSong,
                isVisible = showActionsSheet,
                onDismiss = { 
                    showActionsSheet = false
                    selectedSongForMenu = null
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
                    showActionsSheet = false
                    showInfoSheet = true
                },
                onAddToPlaylist = {
                    showActionsSheet = false
                    playlistViewModel.showAddToPlaylistSheet(menuSong)
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
                    playerViewModel.startRadio(menuSong)
                },
                onListenTogether = {
                    showListenTogetherSheet = true
                },
                onPlaybackSpeed = {
                    showPlaybackSpeedSheet = true
                },
                onEqualizerClick = {
                    showActionsSheet = false
                    showEqualizerSheet = true
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
                    // Show trimmer directly without permission check
                    showActionsSheet = false
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
                    onDismiss = { showInfoSheet = false },
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
            
            // Equalizer Sheet
            AnimatedVisibility(
                visible = showEqualizerSheet,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                com.suvojeet.suvmusic.ui.components.EqualizerSheet(
                    isVisible = true,
                    onDismiss = { showEqualizerSheet = false },
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
                    onDismiss = { showListenTogetherSheet = false },
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
    }
}

    // Fullscreen Video Overlay
    if (isFullScreen) {
        FullScreenVideoPlayer(
            viewModel = playerViewModel,
            dominantColors = dominantColors,
            onDismiss = { playerViewModel.setFullScreen(false) }
        )
    }
}
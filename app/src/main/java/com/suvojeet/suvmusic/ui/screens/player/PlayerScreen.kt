package com.suvojeet.suvmusic.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
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
import com.suvojeet.suvmusic.ui.components.OutputDeviceSheet
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
import com.suvojeet.suvmusic.ui.screens.player.components.ModernQueueView
import com.suvojeet.suvmusic.ui.screens.player.components.SongInfoSection
import com.suvojeet.suvmusic.ui.components.VideoErrorDialog
import com.suvojeet.suvmusic.ui.screens.player.components.TimeLabelsWithQuality
import com.suvojeet.suvmusic.ui.screens.player.components.VolumeControl
import com.suvojeet.suvmusic.ui.screens.player.components.M3ESeekbarShimmer
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.RingtoneViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Fullscreen
import com.suvojeet.suvmusic.data.repository.SponsorSegment
import com.suvojeet.suvmusic.ui.screens.player.FullScreenVideoPlayer

/**
 * State object for PlayerScreen to reduce parameter count.
 */
data class PlayerScreenState(
    val playbackInfo: PlayerState,
    val playerState: PlayerState,
    val lyrics: Lyrics? = null,
    val isFetchingLyrics: Boolean = false,
    val comments: List<com.suvojeet.suvmusic.data.model.Comment>? = null,
    val isFetchingComments: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isPostingComment: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val isRadioMode: Boolean = false,
    val isLoadingMoreSongs: Boolean = false,
    val selectedLyricsProvider: com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType = com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.AUTO,
    val enabledLyricsProviders: Map<com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType, Boolean> = emptyMap(),
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingMs: Long? = null
)

/**
 * Action callbacks for PlayerScreen.
 */
data class PlayerScreenActions(
    val onPlayPause: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onBack: () -> Unit,
    val onDownload: () -> Unit,
    val onToggleLike: () -> Unit,
    val onToggleDislike: () -> Unit,
    val onShuffleToggle: () -> Unit,
    val onRepeatToggle: () -> Unit,
    val onToggleAutoplay: () -> Unit,
    val onToggleVideoMode: () -> Unit = {},
    val onDismissVideoError: () -> Unit = {},
    val onStartRadio: () -> Unit = {},
    val onLoadMoreRadioSongs: () -> Unit = {},
    val onPlayFromQueue: (Int) -> Unit = {},
    val onSwitchDevice: (com.suvojeet.suvmusic.data.model.OutputDevice) -> Unit = {},
    val onRefreshDevices: () -> Unit = {},
    val onArtistClick: (String) -> Unit = {},
    val onAlbumClick: (String) -> Unit = {},
    val onSetPlaybackParameters: (Float, Float) -> Unit = { _, _ -> },
    val onPostComment: (String) -> Unit = {},
    val onLoadMoreComments: () -> Unit = {},
    val onLyricsProviderChange: (com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType) -> Unit = {},
    val onSetSleepTimer: (SleepTimerOption, Int?) -> Unit = { _, _ -> },
    val onClearQueue: () -> Unit = {},
    val onListenTogetherClick: () -> Unit = {}
)


/**
 * Premium full-screen player with Apple Music-style design.
 * Features dynamic colors, quality badges, and queue view.
 */
@Composable
fun PlayerScreen(
    state: PlayerScreenState,
    actions: PlayerScreenActions,
    player: Player? = null,
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel(),
    ringtoneViewModel: RingtoneViewModel = hiltViewModel<RingtoneViewModel>(),
    playerViewModel: com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel = hiltViewModel(),
    mainViewModel: com.suvojeet.suvmusic.ui.viewmodel.MainViewModel = hiltViewModel(),
    volumeKeyEvents: SharedFlow<Unit>? = null
) {
    val playbackInfo = state.playbackInfo
    val playerState = state.playerState
    val song = playbackInfo.currentSong
    val context = LocalContext.current
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isInPip = mainUiState.isInPictureInPictureMode
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val sponsorSegments by playerViewModel.sponsorSegments.collectAsStateWithLifecycle(initialValue = emptyList())
    val isFullScreen by playerViewModel.isFullScreen.collectAsStateWithLifecycle()
    
    // Queue Selection & Sections
    val historySongs by playerViewModel.historySongs.collectAsStateWithLifecycle()
    val upNextSongs by playerViewModel.upNextSongs.collectAsStateWithLifecycle()
    val selectedQueueIndices by playerViewModel.selectedQueueIndices.collectAsStateWithLifecycle()
    
    // Customization styles from settings
    val sessionManager = remember { SessionManager(context) }
    
    val savedSeekbarStyleString by sessionManager.seekbarStyleFlow.collectAsStateWithLifecycle(initialValue = "WAVE_LINE")
    val savedArtworkShapeString by sessionManager.artworkShapeFlow.collectAsStateWithLifecycle(initialValue = "ROUNDED_SQUARE")
    val savedArtworkSizeString by sessionManager.artworkSizeFlow.collectAsStateWithLifecycle(initialValue = "LARGE")
    val volumeSliderEnabled by sessionManager.volumeSliderEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val doubleTapSeekSeconds by sessionManager.doubleTapSeekSecondsFlow.collectAsStateWithLifecycle(initialValue = 10)
    val keepScreenOn by sessionManager.keepScreenOnEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val animatedBackgroundEnabled by sessionManager.playerAnimatedBackgroundFlow.collectAsStateWithLifecycle(initialValue = true)
    val lyricsTextPosition by sessionManager.lyricsTextPositionFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition.CENTER)
    val lyricsAnimationType by sessionManager.lyricsAnimationTypeFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType.WORD)
    val lyricsLineSpacing by sessionManager.lyricsLineSpacingFlow.collectAsStateWithLifecycle(initialValue = 1.5f)
    val lyricsFontSize by sessionManager.lyricsFontSizeFlow.collectAsStateWithLifecycle(initialValue = 26f)
    val audioArEnabled by sessionManager.audioArEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    
    val pendingIntent by playerViewModel.pendingIntent.collectAsStateWithLifecycle()
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Permission granted, file deleted
        }
        playerViewModel.consumePendingIntent()
    }

    LaunchedEffect(pendingIntent) {
        pendingIntent?.let { intent ->
            val intentSenderRequest = IntentSenderRequest.Builder(intent).build()
            deleteLauncher.launch(intentSenderRequest)
        }
    }
    
    val eqEnabled by playerViewModel.getEqEnabled().collectAsStateWithLifecycle(initialValue = false)
    val eqBands by playerViewModel.getEqBands().collectAsStateWithLifecycle(initialValue = FloatArray(10) { 0f })
    val eqPreamp by playerViewModel.getEqPreamp().collectAsStateWithLifecycle(initialValue = 0f)
    val bassBoost by playerViewModel.getBassBoost().collectAsStateWithLifecycle(initialValue = 0f)
    val virtualizer by playerViewModel.getVirtualizer().collectAsStateWithLifecycle(initialValue = 0f)
    
    // Keep Screen On Logic
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if (keepScreenOn) window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    
    val currentSeekbarStyle = remember(savedSeekbarStyleString) { runCatching { SeekbarStyle.valueOf(savedSeekbarStyleString) }.getOrDefault(SeekbarStyle.WAVE_LINE) }
    val currentArtworkShape = remember(savedArtworkShapeString) { runCatching { ArtworkShape.valueOf(savedArtworkShapeString) }.getOrDefault(ArtworkShape.ROUNDED_SQUARE) }
    val currentArtworkSize = remember(savedArtworkSizeString) { runCatching { ArtworkSize.valueOf(savedArtworkSizeString) }.getOrDefault(ArtworkSize.LARGE) }

    // Fix status bar color
    val view = LocalView.current
    val isAppInDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val extractedColors = rememberDominantColors(imageUrl = song?.thumbnailUrl, isDarkTheme = isAppInDarkTheme)
    
    // M3E Spring-based color transitions
    val animatedPrimary by animateColorAsState(
        targetValue = extractedColors.primary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = extractedColors.secondary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "secondary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = extractedColors.accent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "accent"
    )
    val animatedOnBg by animateColorAsState(
        targetValue = extractedColors.onBackground,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "onBg"
    )

    val dominantColors = DominantColors(primary = animatedPrimary, secondary = animatedSecondary, accent = animatedAccent, onBackground = animatedOnBg)
    
    // Background loading pulse
    val bgLoadingAlpha by animateFloatAsState(
        targetValue = if (playerState.isLoading) 0.85f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "bgLoadingDim"
    )

    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
        insetsController.isAppearanceLightStatusBars = !isAppInDarkTheme
        onDispose { insetsController.isAppearanceLightStatusBars = previousLightStatusBars }
    }
    
    LaunchedEffect(dominantColors) {
        val colorArgb = dominantColors.primary.toArgb()
        if (colorArgb != 0) playerViewModel.updateDominantColor(colorArgb)
    }

    var activeOverlay by remember { mutableStateOf<PlayerOverlay>(PlayerOverlay.None) }
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

    val coroutineScope = rememberCoroutineScope()
    BackHandler { 
        val overlay = activeOverlay
        if (overlay != PlayerOverlay.None) {
            if (overlay is PlayerOverlay.Actions && overlay.fromQueue) {
                activeOverlay = PlayerOverlay.Queue
            } else {
                activeOverlay = PlayerOverlay.None 
            }
        } else {
            actions.onBack() 
        }
    }

    var pendingSeekPosition by remember { mutableStateOf<Long?>(null) }
    var seekDebounceJob by remember { mutableStateOf<Job?>(null) }

    val handleDoubleTapSeek: (Boolean) -> Unit = { forward ->
        val current = pendingSeekPosition ?: playerState.currentPosition
        val seekAmount = doubleTapSeekSeconds * 1000L
        val newPos = if (forward) (current + seekAmount).coerceAtMost(playerState.duration) else (current - seekAmount).coerceAtLeast(0)
        pendingSeekPosition = newPos
        seekDebounceJob?.cancel()
        seekDebounceJob = coroutineScope.launch {
            delay(400)
            actions.onSeekTo(newPos)
            delay(600)
            pendingSeekPosition = null
        }
    }

    val playerBackgroundColor = if (isAppInDarkTheme) Color.Black else Color.White

    if (isInPip) {
        PiPPlayerContent(song = song, isVideoMode = playerState.isVideoMode, player = player)
    } else {
        Box(modifier = Modifier.fillMaxSize().background(playerBackgroundColor).graphicsLayer { alpha = bgLoadingAlpha }) {
            // Background
            if (animatedBackgroundEnabled && !playerState.isVideoMode) {
                MeshGradientBackground(dominantColors = dominantColors, backgroundColor = playerBackgroundColor)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(dominantColors.secondary, dominantColors.primary, playerBackgroundColor))))
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useWideLayout = maxWidth > 500.dp
                AnimatedVisibility(visible = !showQueue, enter = fadeIn(), exit = fadeOut()) {
                    if (useWideLayout) {
                        LandscapePlayerContent(
                            song = song, playerState = playerState, playbackInfo = playbackInfo, dominantColors = dominantColors,
                            currentArtworkShape = currentArtworkShape, currentArtworkSize = currentArtworkSize, currentSeekbarStyle = currentSeekbarStyle,
                            sponsorSegments = sponsorSegments, audioArEnabled = audioArEnabled, actions = actions,
                            onShowActions = { activeOverlay = PlayerOverlay.Actions(song) },
                            onShowLyrics = { activeOverlay = PlayerOverlay.Lyrics },
                            onShowQueue = { activeOverlay = PlayerOverlay.Queue },
                            onShowDevices = { activeOverlay = PlayerOverlay.OutputDevice },
                            onShowSleepTimer = { activeOverlay = PlayerOverlay.SleepTimer },
                            onShowPlaybackSpeed = { activeOverlay = PlayerOverlay.PlaybackSpeed },
                            onShowEqualizer = { activeOverlay = PlayerOverlay.Equalizer },
                            onShowListenTogether = { activeOverlay = PlayerOverlay.ListenTogether },
                            isVideoMode = playerState.isVideoMode,
                            onToggleVideoMode = actions.onToggleVideoMode,
                            handleDoubleTapSeek = handleDoubleTapSeek,
                            onShapeChange = { shape -> coroutineScope.launch(Dispatchers.IO) { sessionManager.setArtworkShape(shape.name) } },
                            onSeekbarStyleChange = { style -> coroutineScope.launch(Dispatchers.IO) { sessionManager.setSeekbarStyle(style.name) } },
                            onRecenterAr = { playerViewModel.calibrateAudioAr() },
                            player = player,
                            isFullScreen = isFullScreen,
                            onSetFullScreen = { playerViewModel.setFullScreen(it) }
                        )
                    } else {
                        val isCompactHeight = maxHeight < 600.dp
                        PortraitPlayerContent(
                            song = song, playerState = playerState, playbackInfo = playbackInfo, dominantColors = dominantColors,
                            currentArtworkShape = currentArtworkShape, currentArtworkSize = currentArtworkSize, currentSeekbarStyle = currentSeekbarStyle,
                            sponsorSegments = sponsorSegments, audioArEnabled = audioArEnabled, player = player, isFullScreen = isFullScreen,
                            isCompactHeight = isCompactHeight, actions = actions,
                            onShowActions = { activeOverlay = PlayerOverlay.Actions(song) },
                            onShowQueue = { activeOverlay = PlayerOverlay.Queue },
                            onShowLyrics = { activeOverlay = PlayerOverlay.Lyrics },
                            onShowDevices = { activeOverlay = PlayerOverlay.OutputDevice },
                            onShowSleepTimer = { activeOverlay = PlayerOverlay.SleepTimer },
                            onShowPlaybackSpeed = { activeOverlay = PlayerOverlay.PlaybackSpeed },
                            onShowEqualizer = { activeOverlay = PlayerOverlay.Equalizer },
                            onShowListenTogether = { activeOverlay = PlayerOverlay.ListenTogether },
                            handleDoubleTapSeek = handleDoubleTapSeek,
                            onShapeChange = { shape -> coroutineScope.launch(Dispatchers.IO) { sessionManager.setArtworkShape(shape.name) } },
                            onSeekbarStyleChange = { style -> coroutineScope.launch(Dispatchers.IO) { sessionManager.setSeekbarStyle(style.name) } },
                            onRecenterAr = { playerViewModel.calibrateAudioAr() },
                            onSetFullScreen = { playerViewModel.setFullScreen(it) }
                        )
                    }
                }

                OverlaysContent(
                    state = state, actions = actions.copy(onClearQueue = { playerViewModel.clearQueue() }), activeOverlay = activeOverlay, onOverlayChange = { activeOverlay = it },
                    dominantColors = dominantColors, playerViewModel = playerViewModel, playlistViewModel = playlistViewModel,
                    ringtoneViewModel = hiltViewModel<RingtoneViewModel>(), // Explicit type for clarity and to fix inference errors
                    historySongs = historySongs, upNextSongs = upNextSongs, selectedQueueIndices = selectedQueueIndices,
                    isAppInDarkTheme = isAppInDarkTheme, animatedBackgroundEnabled = animatedBackgroundEnabled,
                    volumeSliderEnabled = volumeSliderEnabled, volumeKeyEvents = volumeKeyEvents,
                    lyricsTextPosition = lyricsTextPosition, lyricsAnimationType = lyricsAnimationType,
                    lyricsLineSpacing = lyricsLineSpacing, lyricsFontSize = lyricsFontSize,
                    sessionManager = sessionManager, coroutineScope = coroutineScope, isFullScreen = isFullScreen,
                    eqEnabled = eqEnabled, eqBands = eqBands, eqPreamp = eqPreamp, bassBoost = bassBoost, virtualizer = virtualizer
                )
            }
        }
    }
}


@Composable
fun PiPPlayerContent(song: com.suvojeet.suvmusic.core.model.Song?, isVideoMode: Boolean, player: Player?) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (isVideoMode && player != null) {
            AndroidView(factory = { context -> PlayerView(context).apply { this.player = player; useController = false } }, modifier = Modifier.fillMaxSize())
        } else {
            if (song?.thumbnailUrl != null) {
                coil.compose.AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop, alpha = 0.6f)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(4.dp)) {
                Text(text = song?.title ?: "Unknown", style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                Text(text = song?.artist ?: "Unknown Artist", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            }
        }
    }
}

@Composable
fun PortraitPlayerContent(
    song: com.suvojeet.suvmusic.core.model.Song?,
    playerState: PlayerState,
    playbackInfo: PlayerState,
    dominantColors: DominantColors,
    currentArtworkShape: ArtworkShape,
    currentArtworkSize: ArtworkSize,
    currentSeekbarStyle: SeekbarStyle,
    sponsorSegments: List<SponsorSegment>,
    audioArEnabled: Boolean,
    player: Player?,
    isFullScreen: Boolean,
    isCompactHeight: Boolean,
    actions: PlayerScreenActions,
    onShowActions: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowPlaybackSpeed: () -> Unit,
    onShowEqualizer: () -> Unit,
    onShowListenTogether: () -> Unit,
    handleDoubleTapSeek: (Boolean) -> Unit,
    onShapeChange: (ArtworkShape) -> Unit,
    onSeekbarStyleChange: (SeekbarStyle) -> Unit,
    onRecenterAr: () -> Unit,
    onSetFullScreen: (Boolean) -> Unit
) {
    // Controls dimming when loading
    val controlsAlpha by animateFloatAsState(
        targetValue = if (playerState.isLoading) 0.45f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "controlsDimOnLoad"
    )

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = if (isCompactHeight) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerTopBar(onBack = actions.onBack, dominantColors = dominantColors, audioArEnabled = audioArEnabled, onRecenter = onRecenterAr)

        Spacer(modifier = Modifier.weight(1f))
        
        // Show Video or Artwork in the same center space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = playerState.isVideoMode && player != null && !isFullScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                    fadeOut(animationSpec = tween(500))
                },
                label = "video_artwork_transition"
            ) { isVideo ->
                if (isVideo) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth(ArtworkSize.LARGE.fraction)
                            .aspectRatio(1f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(currentArtworkSize.fraction / ArtworkSize.LARGE.fraction)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .clickable { onSetFullScreen(true) },
                            tonalElevation = 16.dp,
                            shadowElevation = 16.dp
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).apply {
                                        this.player = player
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Small expand icon overlay
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Filled.Fullscreen,
                                    contentDescription = "Full Screen",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(4.dp)
                                )
                            }
                        }
                    }
                } else {
                    AlbumArtwork(
                        imageUrl = song?.thumbnailUrl, title = song?.title, dominantColors = dominantColors, isLoading = playerState.isLoading,
                        onSwipeLeft = actions.onNext, onSwipeRight = actions.onPrevious, initialShape = currentArtworkShape, artworkSize = currentArtworkSize,
                        onShapeChange = onShapeChange, onDoubleTapLeft = { handleDoubleTapSeek(false) }, onDoubleTapRight = { handleDoubleTapSeek(true) }, songId = song?.id
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        SongInfoSection(
            song = song, isFavorite = playerState.isLiked, onFavoriteClick = actions.onToggleLike, isDisliked = playerState.isDisliked,
            onDislikeClick = actions.onToggleDislike, onMoreClick = onShowActions, onArtistClick = actions.onArtistClick, onAlbumClick = actions.onAlbumClick,
            dominantColors = dominantColors, isLoading = playerState.isLoading, compact = isCompactHeight
        )

        Spacer(modifier = Modifier.weight(if (isCompactHeight) 0.1f else 0.4f))

        if (playerState.isLoading) {
            M3ESeekbarShimmer(
                isVisible = true,
                dominantColors = dominantColors,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(4.dp)
            )
        } else {
            WaveformSeeker(
                progressProvider = { playerState.progress }, isPlaying = playbackInfo.isPlaying, onSeek = { actions.onSeekTo((it * playerState.duration).toLong()) },
                modifier = Modifier.fillMaxWidth(), activeColor = dominantColors.accent, inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f),
                initialStyle = currentSeekbarStyle, onStyleChange = onSeekbarStyleChange, duration = playerState.duration, sponsorSegments = sponsorSegments
            )
        }

        TimeLabelsWithQuality(currentPositionProvider = { playerState.currentPosition }, durationProvider = { playerState.duration }, dominantColors = dominantColors)

        Spacer(modifier = Modifier.weight(if (isCompactHeight) 0.1f else 0.4f))

        Box(modifier = Modifier.graphicsLayer { alpha = controlsAlpha }) {
            PlaybackControls(
                isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode,
                onPlayPause = actions.onPlayPause, onNext = actions.onNext, onPrevious = actions.onPrevious, onShuffleToggle = actions.onShuffleToggle,
                onRepeatToggle = actions.onRepeatToggle, dominantColors = dominantColors, compact = isCompactHeight
            )
        }

        Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 16.dp))

        BottomActions(
            onLyricsClick = onShowLyrics, onCastClick = onShowDevices, onQueueClick = onShowQueue, onDownloadClick = actions.onDownload,
            downloadState = playerState.downloadState, dominantColors = dominantColors, isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
            isVideoMode = playerState.isVideoMode, onVideoToggle = actions.onToggleVideoMode, compact = isCompactHeight
        )
        Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 24.dp))
    }
}

@Composable
fun LandscapePlayerContent(
    song: com.suvojeet.suvmusic.core.model.Song?, playerState: PlayerState, playbackInfo: PlayerState, dominantColors: DominantColors,
    currentArtworkShape: ArtworkShape, currentArtworkSize: ArtworkSize, currentSeekbarStyle: SeekbarStyle, sponsorSegments: List<SponsorSegment>,
    audioArEnabled: Boolean, actions: PlayerScreenActions, onShowActions: () -> Unit, onShowLyrics: () -> Unit, onShowQueue: () -> Unit,
    onShowDevices: () -> Unit, onShowSleepTimer: () -> Unit, onShowPlaybackSpeed: () -> Unit, onShowEqualizer: () -> Unit, onShowListenTogether: () -> Unit,
    isVideoMode: Boolean, onToggleVideoMode: () -> Unit, handleDoubleTapSeek: (Boolean) -> Unit, onShapeChange: (ArtworkShape) -> Unit,
    onSeekbarStyleChange: (SeekbarStyle) -> Unit, onRecenterAr: () -> Unit,
    player: Player?, isFullScreen: Boolean, onSetFullScreen: (Boolean) -> Unit
) {
    // Controls dimming when loading
    val controlsAlpha by animateFloatAsState(
        targetValue = if (playerState.isLoading) 0.45f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "controlsDimOnLoadLandscape"
    )

    Row(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(0.45f).fillMaxHeight().padding(end = 16.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isVideoMode && player != null && !isFullScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                    fadeOut(animationSpec = tween(500))
                },
                label = "video_artwork_transition_landscape"
            ) { isVideo ->
                if (isVideo) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxHeight(ArtworkSize.LARGE.fraction).aspectRatio(1f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight(currentArtworkSize.fraction / ArtworkSize.LARGE.fraction)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .clickable { onSetFullScreen(true) },
                            tonalElevation = 16.dp,
                            shadowElevation = 16.dp
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).apply {
                                        this.player = player
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Filled.Fullscreen,
                                    contentDescription = "Full Screen",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(4.dp)
                                )
                            }
                        }
                    }
                } else {
                    AlbumArtwork(
                        imageUrl = song?.thumbnailUrl, title = song?.title, dominantColors = dominantColors, isLoading = playerState.isLoading,
                        onSwipeLeft = actions.onNext, onSwipeRight = actions.onPrevious, initialShape = currentArtworkShape, artworkSize = currentArtworkSize,
                        onShapeChange = onShapeChange, onDoubleTapLeft = { handleDoubleTapSeek(false) }, onDoubleTapRight = { handleDoubleTapSeek(true) }, songId = song?.id
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(0.55f).fillMaxHeight().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            PlayerTopBar(onBack = actions.onBack, dominantColors = dominantColors, audioArEnabled = audioArEnabled, onRecenter = onRecenterAr)
            Spacer(modifier = Modifier.height(8.dp))
            SongInfoSection(song = song, isFavorite = playerState.isLiked, onFavoriteClick = actions.onToggleLike, isDisliked = playerState.isDisliked, onDislikeClick = actions.onToggleDislike, onMoreClick = onShowActions, onArtistClick = actions.onArtistClick, onAlbumClick = actions.onAlbumClick, dominantColors = dominantColors, isLoading = playerState.isLoading)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (playerState.isLoading) {
                M3ESeekbarShimmer(
                    isVisible = true,
                    dominantColors = dominantColors,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(4.dp)
                )
            } else {
                WaveformSeeker(progressProvider = { playerState.progress }, isPlaying = playbackInfo.isPlaying, onSeek = { actions.onSeekTo((it * playerState.duration).toLong()) }, modifier = Modifier.fillMaxWidth(), activeColor = dominantColors.accent, inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f), initialStyle = currentSeekbarStyle, onStyleChange = onSeekbarStyleChange, duration = playerState.duration, sponsorSegments = sponsorSegments)
            }
            
            TimeLabelsWithQuality(currentPositionProvider = { playerState.currentPosition }, durationProvider = { playerState.duration }, dominantColors = dominantColors)
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(modifier = Modifier.graphicsLayer { alpha = controlsAlpha }) {
                PlaybackControls(isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode, onPlayPause = actions.onPlayPause, onNext = actions.onNext, onPrevious = actions.onPrevious, onShuffleToggle = actions.onShuffleToggle, onRepeatToggle = actions.onRepeatToggle, dominantColors = dominantColors)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            BottomActions(onLyricsClick = onShowLyrics, onCastClick = onShowDevices, onQueueClick = onShowQueue, onDownloadClick = actions.onDownload, downloadState = playerState.downloadState, dominantColors = dominantColors, isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE, isVideoMode = isVideoMode, onVideoToggle = onToggleVideoMode)
        }
    }
}

@Composable
fun BoxScope.OverlaysContent(
    state: PlayerScreenState, actions: PlayerScreenActions, activeOverlay: PlayerOverlay, onOverlayChange: (PlayerOverlay) -> Unit,
    dominantColors: DominantColors, playerViewModel: com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel,
    playlistViewModel: PlaylistManagementViewModel, ringtoneViewModel: RingtoneViewModel, historySongs: List<com.suvojeet.suvmusic.core.model.Song>,
    upNextSongs: List<com.suvojeet.suvmusic.core.model.Song>, selectedQueueIndices: Set<Int>, isAppInDarkTheme: Boolean,
    animatedBackgroundEnabled: Boolean, volumeSliderEnabled: Boolean, volumeKeyEvents: SharedFlow<Unit>?,
    lyricsTextPosition: com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition, lyricsAnimationType: com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType,
    lyricsLineSpacing: Float, lyricsFontSize: Float, sessionManager: SessionManager, coroutineScope: kotlinx.coroutines.CoroutineScope,
    isFullScreen: Boolean, eqEnabled: Boolean, eqBands: FloatArray, eqPreamp: Float, bassBoost: Float, virtualizer: Float
) {
    val context = LocalContext.current
    val song = state.playbackInfo.currentSong
    val playerState = state.playerState
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    
    // Use rememberUpdatedState to prevent stale state capture in lambdas
    val currentOverlay by androidx.compose.runtime.rememberUpdatedState(activeOverlay)

    // VolumeIndicator
    if (volumeSliderEnabled) {
        VolumeControl(dominantColors = dominantColors, volumeKeyEvents = volumeKeyEvents, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.3f).padding(end = 16.dp))
    }

    // Queue View
    AnimatedVisibility(
        visible = activeOverlay is PlayerOverlay.Queue || (activeOverlay is PlayerOverlay.Actions && activeOverlay.fromQueue),
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        ModernQueueView(
            currentSong = song, queue = playerState.queue, playedSongs = historySongs, upNextSongs = upNextSongs, selectedQueueIndices = selectedQueueIndices,
            onToggleSelection = { playerViewModel.toggleQueueSelection(it) }, onSelectAll = { playerViewModel.selectAllQueueItems() }, onClearSelection = { playerViewModel.clearQueueSelection() },
            currentIndex = playerState.currentIndex, isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode,
            isAutoplayEnabled = playerState.isAutoplayEnabled, isFavorite = playerState.isLiked, isRadioMode = state.isRadioMode, isLoadingMore = state.isLoadingMoreSongs,
            onBack = { if (currentOverlay is PlayerOverlay.Queue) onOverlayChange(PlayerOverlay.None) }, onSongClick = actions.onPlayFromQueue, onPlayPause = actions.onPlayPause,
            onToggleShuffle = actions.onShuffleToggle, onToggleRepeat = actions.onRepeatToggle, onToggleAutoplay = actions.onToggleAutoplay, onToggleLike = actions.onToggleLike,
            onMoreClick = { onOverlayChange(PlayerOverlay.Actions(it, fromQueue = true)) }, onLoadMore = actions.onLoadMoreRadioSongs, onMoveItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
            onRemoveItems = { playerViewModel.removeQueueItems(it) }, onSaveAsPlaylist = { t, d, p, s -> playerViewModel.saveQueueAsPlaylist(t, d, p, s) { if (it) Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() } },
            onClearQueue = actions.onClearQueue,
            dominantColors = dominantColors, animatedBackgroundEnabled = animatedBackgroundEnabled, isDarkTheme = isAppInDarkTheme
        )
    }

    // Lyrics View
    AnimatedVisibility(visible = activeOverlay is PlayerOverlay.Lyrics, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        LyricsScreen(
            lyrics = state.lyrics, isFetching = state.isFetchingLyrics, currentTimeProvider = { playerState.currentPosition }, artworkUrl = song?.thumbnailUrl,
            onClose = { if (currentOverlay is PlayerOverlay.Lyrics) onOverlayChange(PlayerOverlay.None) }, isDarkTheme = isAppInDarkTheme, onSeekTo = actions.onSeekTo, songTitle = song?.title ?: "",
            artistName = song?.artist ?: "", duration = playerState.duration, selectedProvider = state.selectedLyricsProvider,
            enabledProviders = state.enabledLyricsProviders, onProviderChange = actions.onLyricsProviderChange, lyricsTextPosition = lyricsTextPosition,
            lyricsAnimationType = lyricsAnimationType, lyricsLineSpacing = lyricsLineSpacing, lyricsFontSize = lyricsFontSize,
            onLineSpacingChange = { coroutineScope.launch { sessionManager.setLyricsLineSpacing(it) } }, onFontSizeChange = { coroutineScope.launch { sessionManager.setLyricsFontSize(it) } },
            onTextPositionChange = { coroutineScope.launch { sessionManager.setLyricsTextPosition(it) } }, onAnimationTypeChange = { coroutineScope.launch { sessionManager.setLyricsAnimationType(it) } },
            isPlaying = playerState.isPlaying, onPlayPause = actions.onPlayPause, onNext = actions.onNext, onPrevious = actions.onPrevious
        )
    }

    // Other Sheets
    val menuSong = (activeOverlay as? PlayerOverlay.Actions)?.targetSong ?: song
    if (menuSong != null) {
        SongActionsSheet(
            song = menuSong, isVisible = activeOverlay is PlayerOverlay.Actions, 
            onDismiss = { 
                val overlay = currentOverlay
                if (overlay is PlayerOverlay.Actions) {
                    if (overlay.fromQueue) onOverlayChange(PlayerOverlay.Queue)
                    else onOverlayChange(PlayerOverlay.None)
                }
            }, 
            dominantColors = dominantColors,
            isDownloaded = playerViewModel.isDownloaded(menuSong.id), onToggleFavorite = { if (menuSong.id == song?.id) actions.onToggleLike() else playerViewModel.likeSong(menuSong) },
            onToggleDislike = { if (menuSong.id == song?.id) actions.onToggleDislike() else playerViewModel.dislikeCurrentSong() },
            isFavorite = if (menuSong.id == song?.id) playerState.isLiked else false, isDisliked = if (menuSong.id == song?.id) playerState.isDisliked else false,
            onDownload = { if (menuSong.id == song?.id) actions.onDownload() else com.suvojeet.suvmusic.service.DownloadService.startDownload(context, menuSong) },
            onDeleteDownload = { playerViewModel.deleteDownload(menuSong.id) }, onPlayNext = { playerViewModel.playNext(menuSong) }, onAddToQueue = { playerViewModel.addToQueue(menuSong) },
            onViewInfo = { onOverlayChange(PlayerOverlay.SongInfo) }, onAddToPlaylist = { onOverlayChange(PlayerOverlay.None); playlistViewModel.showAddToPlaylistSheet(menuSong) },
            onViewComments = { onOverlayChange(PlayerOverlay.Comments) }, onSleepTimer = { onOverlayChange(PlayerOverlay.SleepTimer) }, onStartRadio = { actions.onStartRadio() },
            onListenTogether = { 
                onOverlayChange(PlayerOverlay.None)
                actions.onListenTogetherClick() 
            }, 
            onPlaybackSpeed = { onOverlayChange(PlayerOverlay.None); onOverlayChange(PlayerOverlay.PlaybackSpeed) }, onEqualizerClick = { onOverlayChange(PlayerOverlay.None); onOverlayChange(PlayerOverlay.Equalizer) },
            currentSpeed = playerState.playbackSpeed,
            onSetRingtone = { 
                if (ringtoneViewModel.ringtoneHelper.hasSettingsPermission(context)) {
                    ringtoneViewModel.showTrimmer(menuSong)
                } else {
                    Toast.makeText(context, "Please allow 'Modify System Settings' permission", Toast.LENGTH_LONG).show()
                    ringtoneViewModel.ringtoneHelper.requestSettingsPermission(context)
                }
            }
            )
    }

    // Simplified remaining overlays
    com.suvojeet.suvmusic.ui.screens.player.components.CommentsSheet(
        isVisible = activeOverlay is PlayerOverlay.Comments, comments = state.comments, isLoading = state.isFetchingComments,
        onDismiss = { if (currentOverlay is PlayerOverlay.Comments) onOverlayChange(PlayerOverlay.None) }, accentColor = dominantColors.accent, isLoggedIn = state.isLoggedIn,
        isPostingComment = state.isPostingComment, onPostComment = actions.onPostComment, isLoadingMore = state.isLoadingMoreComments, onLoadMore = actions.onLoadMoreComments
    )

    if (song != null) {
        SongInfoSheet(song = song, isVisible = activeOverlay is PlayerOverlay.SongInfo, onDismiss = { if (currentOverlay is PlayerOverlay.SongInfo) onOverlayChange(PlayerOverlay.None) }, onArtistClick = actions.onArtistClick, audioCodec = playerState.audioCodec, audioBitrate = playerState.audioBitrate)
    }

    if (playlistUiState.showAddToPlaylistSheet && playlistUiState.selectedSong != null) {
        AddToPlaylistSheet(song = playlistUiState.selectedSong!!, isVisible = true, playlists = playlistUiState.userPlaylists, isLoading = playlistUiState.isLoadingPlaylists, onDismiss = { playlistViewModel.hideAddToPlaylistSheet() }, onAddToPlaylist = { playlistViewModel.addSongToPlaylist(it) }, onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() })
    }

    SleepTimerSheet(isVisible = activeOverlay is PlayerOverlay.SleepTimer, currentOption = state.sleepTimerOption, remainingTimeFormatted = state.sleepTimerRemainingMs?.let { String.format("%d:%02d", it/60000, (it/1000)%60) }, onSelectOption = actions.onSetSleepTimer, onDismiss = { if (currentOverlay is PlayerOverlay.SleepTimer) onOverlayChange(PlayerOverlay.None) }, accentColor = dominantColors.accent)

    if (activeOverlay is PlayerOverlay.Equalizer) {
        com.suvojeet.suvmusic.ui.components.EqualizerSheet(isVisible = true, onDismiss = { onOverlayChange(PlayerOverlay.None) }, dominantColor = dominantColors.accent, onEnabledChange = { playerViewModel.setEqEnabled(it) }, onBandChange = { b, g -> playerViewModel.setEqBandGain(b, g) }, onBandsChange = { playerViewModel.setEqBands(it) }, onPreampChange = { playerViewModel.setEqPreamp(it) }, onBassBoostChange = { playerViewModel.setBassBoost(it) }, onVirtualizerChange = { playerViewModel.setVirtualizer(it) }, onReset = { playerViewModel.resetEqBands() }, initialEnabled = eqEnabled, initialBands = eqBands, initialPreamp = eqPreamp, initialBassBoost = bassBoost, initialVirtualizer = virtualizer)
    }

    PlaybackSpeedSheet(
        isVisible = activeOverlay is PlayerOverlay.PlaybackSpeed,
        currentSpeed = playerState.playbackSpeed,
        currentPitch = playerState.pitch,
        onDismiss = { if (currentOverlay is PlayerOverlay.PlaybackSpeed) onOverlayChange(PlayerOverlay.None) },
        onApply = { speed, pitch -> actions.onSetPlaybackParameters(speed, pitch) }
    )

    OutputDeviceSheet(
        isVisible = activeOverlay is PlayerOverlay.OutputDevice,
        devices = playerState.availableDevices,
        onDeviceSelected = { actions.onSwitchDevice(it) },
        onDismiss = { if (currentOverlay is PlayerOverlay.OutputDevice) onOverlayChange(PlayerOverlay.None) },
        onRefreshDevices = { actions.onRefreshDevices() },
        accentColor = dominantColors.accent
    )

    // Ringtone Dialogs
    val ringtoneUiState by ringtoneViewModel.uiState.collectAsStateWithLifecycle()
    if (ringtoneUiState.showTrimmer && ringtoneUiState.targetSong != null) {
        RingtoneTrimmerDialog(
            isVisible = true,
            song = ringtoneUiState.targetSong!!,
            onDismiss = { ringtoneViewModel.hideTrimmer() },
            onResolveStreamUrl = { ringtoneViewModel.getStreamUrl(it) },
            onConfirm = { start, end -> 
                ringtoneViewModel.setAsRingtone(context, ringtoneUiState.targetSong!!, start, end)
            }
        )
    }
    
    if (ringtoneUiState.showProgress) {
        RingtoneProgressDialog(
            isVisible = true,
            progress = ringtoneUiState.progress,
            statusMessage = ringtoneUiState.statusMessage,
            isComplete = ringtoneUiState.isComplete,
            isSuccess = ringtoneUiState.isSuccess,
            onDismiss = { ringtoneViewModel.dismissProgress() },
            onOpenSettings = { 
                ringtoneViewModel.ringtoneHelper.openRingtoneSettings(context, ringtoneUiState.ringtoneUri)
                ringtoneViewModel.dismissProgress()
            }
        )
    }

    if (isFullScreen) {
        FullScreenVideoPlayer(viewModel = playerViewModel, dominantColors = dominantColors, onDismiss = { playerViewModel.setFullScreen(false) })
    }
}

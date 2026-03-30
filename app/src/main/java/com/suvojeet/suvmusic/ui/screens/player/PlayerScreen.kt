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
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.material.icons.filled.ErrorOutline
import com.suvojeet.suvmusic.data.repository.SponsorSegment
import com.suvojeet.suvmusic.ui.screens.player.FullScreenVideoPlayer

import com.suvojeet.suvmusic.data.model.PlayerStyle
import com.suvojeet.suvmusic.ui.screens.player.styles.YTMusicPlayerStyle
import com.suvojeet.suvmusic.ui.screens.player.styles.ClassicPlayerStyle

import com.suvojeet.suvmusic.ui.screens.player.components.RelatedSheet

/**
 * State object for PlayerScreen to reduce parameter count.
 */
data class PlayerScreenState(
    val playbackInfo: PlayerState,
    val playerState: PlayerState,
    val lyrics: Lyrics? = null,
    val isFetchingLyrics: Boolean = false,
    val relatedSongs: List<com.suvojeet.suvmusic.core.model.Song> = emptyList(),
    val isFetchingRelated: Boolean = false,
    val selectedRelatedIndices: Set<Int> = emptySet(),
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
    val onListenTogetherClick: () -> Unit = {},
    val onPlayRelated: (com.suvojeet.suvmusic.core.model.Song) -> Unit = {},
    val onToggleRelatedSelection: (Int) -> Unit = {},
    val onSelectAllRelated: () -> Unit = {},
    val onClearRelatedSelection: () -> Unit = {},
    val onAddRelatedToQueue: (List<com.suvojeet.suvmusic.core.model.Song>) -> Unit = {},
    val onAddRelatedToPlaylist: (List<com.suvojeet.suvmusic.core.model.Song>) -> Unit = {}
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
    val isSwitchingMode by playerViewModel.isSwitchingMode.collectAsStateWithLifecycle()
    
    // Queue Selection & Sections
    val historySongs by playerViewModel.historySongs.collectAsStateWithLifecycle()
    val upNextSongs by playerViewModel.upNextSongs.collectAsStateWithLifecycle()
    val selectedQueueIndices by playerViewModel.selectedQueueIndices.collectAsStateWithLifecycle()
    
    // Customization styles from settings
    val sessionManager = remember { SessionManager(context) }
    
    val savedSeekbarStyleString by playerViewModel.seekbarStyle.collectAsStateWithLifecycle()
    val savedArtworkShapeString by playerViewModel.artworkShape.collectAsStateWithLifecycle()
    val savedArtworkSizeString by playerViewModel.artworkSize.collectAsStateWithLifecycle()
    val volumeSliderEnabled by sessionManager.volumeSliderEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val doubleTapSeekSeconds by sessionManager.doubleTapSeekSecondsFlow.collectAsStateWithLifecycle(initialValue = 10)
    val keepScreenOn by sessionManager.keepScreenOnEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val animatedBackgroundEnabled by sessionManager.playerAnimatedBackgroundFlow.collectAsStateWithLifecycle(initialValue = true)
    val lyricsTextPosition by sessionManager.lyricsTextPositionFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition.CENTER)
    val lyricsAnimationType by sessionManager.lyricsAnimationTypeFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType.WORD)
    val lyricsLineSpacing by sessionManager.lyricsLineSpacingFlow.collectAsStateWithLifecycle(initialValue = 1.5f)
    val lyricsFontSize by sessionManager.lyricsFontSizeFlow.collectAsStateWithLifecycle(initialValue = 26f)
    val audioArEnabled by sessionManager.audioArEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val albumArtDynamicColorsEnabled by sessionManager.albumArtDynamicColorsEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val rotatingVinylAnimationEnabled by sessionManager.rotatingVinylAnimationEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val playerStyle by sessionManager.playerStyleFlow.collectAsStateWithLifecycle(initialValue = PlayerStyle.YT_MUSIC)
    
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

    // Optimization: Use derivedStateOf for values that change frequently to avoid recomposing the whole screen
    val currentProgress by androidx.compose.runtime.remember(playerState.progress) {
        androidx.compose.runtime.mutableFloatStateOf(playerState.progress)
    }
    val currentPosition by androidx.compose.runtime.remember(playerState.currentPosition) {
        androidx.compose.runtime.mutableLongStateOf(playerState.currentPosition)
    }
    val currentDuration by androidx.compose.runtime.remember(playerState.duration) {
        androidx.compose.runtime.mutableLongStateOf(playerState.duration)
    }

    // Fix status bar color
    val view = LocalView.current
    val isAppInDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val extractedColors = rememberDominantColors(imageUrl = song?.thumbnailUrl, isDarkTheme = isAppInDarkTheme)
    
    // Apply dynamic colors only if enabled
    val colorScheme = MaterialTheme.colorScheme
    val finalColors = if (albumArtDynamicColorsEnabled) {
        extractedColors
    } else {
        if (isAppInDarkTheme) {
            DominantColors(
                primary = MaterialTheme.colorScheme.surfaceContainerHigh,
                secondary = MaterialTheme.colorScheme.surfaceContainerHighest,
                accent = MaterialTheme.colorScheme.primary,
                onBackground = MaterialTheme.colorScheme.onSurface
            )
        } else {
            DominantColors(
                primary = MaterialTheme.colorScheme.surfaceContainerLow,
                secondary = MaterialTheme.colorScheme.surfaceContainerLowest,
                accent = MaterialTheme.colorScheme.primary,
                onBackground = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // M3E Fast Expressive color transitions
    val animatedPrimary by animateColorAsState(
        targetValue = finalColors.primary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = finalColors.secondary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "secondary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = finalColors.accent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "accent"
    )
    val animatedOnBg by animateColorAsState(
        targetValue = finalColors.onBackground,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
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
            when {
                overlay is PlayerOverlay.Actions && overlay.fromQueue -> activeOverlay = PlayerOverlay.Queue
                overlay is PlayerOverlay.Actions && overlay.fromRelated -> activeOverlay = PlayerOverlay.Related
                else -> activeOverlay = PlayerOverlay.None
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
                val isCompactHeight = maxHeight < 600.dp

                AnimatedVisibility(visible = !showQueue, enter = fadeIn(), exit = fadeOut()) {
                    when (playerStyle) {
                        PlayerStyle.YT_MUSIC -> {
                            YTMusicPlayerStyle(
                                song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
                                currentSeekbarStyle, sponsorSegments, audioArEnabled, rotatingVinylAnimationEnabled,
                                player, isFullScreen, isCompactHeight, useWideLayout, actions,
                                onShowActions = { activeOverlay = PlayerOverlay.Actions(song) },
                                onShowQueue = { activeOverlay = PlayerOverlay.Queue },
                                onShowLyrics = { activeOverlay = PlayerOverlay.Lyrics },
                                onShowRelated = { activeOverlay = PlayerOverlay.Related },
                                onShowDevices = { activeOverlay = PlayerOverlay.OutputDevice },
                                onShowSleepTimer = { activeOverlay = PlayerOverlay.SleepTimer },
                                onShowPlaybackSpeed = { activeOverlay = PlayerOverlay.PlaybackSpeed },
                                onShowEqualizer = { activeOverlay = PlayerOverlay.Equalizer },
                                onShowListenTogether = { activeOverlay = PlayerOverlay.ListenTogether },
                                handleDoubleTapSeek = handleDoubleTapSeek,
                                onShapeChange = { shape -> coroutineScope.launch(Dispatchers.IO) { sessionManager.setArtworkShape(shape.name) } },
                                onSeekbarStyleChange = { style -> coroutineScope.launch(Dispatchers.IO) { sessionManager.setSeekbarStyle(style.name) } },
                                onRecenterAr = { playerViewModel.calibrateAudioAr() },
                                onSetFullScreen = { playerViewModel.setFullScreen(it) },
                                isSwitchingMode = isSwitchingMode,
                                sleepTimerOption = state.sleepTimerOption,
                                sleepTimerRemainingMs = state.sleepTimerRemainingMs,
                                currentProgress = currentProgress,
                                currentPosition = currentPosition,
                                currentDuration = currentDuration
                            )
                        }
                        PlayerStyle.CLASSIC -> {
                            ClassicPlayerStyle(
                                song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
                                currentSeekbarStyle, sponsorSegments, audioArEnabled, rotatingVinylAnimationEnabled,
                                player, isFullScreen, isCompactHeight, useWideLayout, actions,
                                onShowActions = { activeOverlay = PlayerOverlay.Actions(song) },
                                onShowQueue = { activeOverlay = PlayerOverlay.Queue },
                                onShowLyrics = { activeOverlay = PlayerOverlay.Lyrics },
                                onShowRelated = { activeOverlay = PlayerOverlay.Related },
                                onShowDevices = { activeOverlay = PlayerOverlay.OutputDevice },
                                onShowSleepTimer = { activeOverlay = PlayerOverlay.SleepTimer },
                                onShowPlaybackSpeed = { activeOverlay = PlayerOverlay.PlaybackSpeed },
                                onShowEqualizer = { activeOverlay = PlayerOverlay.Equalizer },
                                onShowListenTogether = { activeOverlay = PlayerOverlay.ListenTogether },
                                handleDoubleTapSeek = handleDoubleTapSeek,
                                onShapeChange = { shape -> coroutineScope.launch(Dispatchers.IO) { sessionManager.setArtworkShape(shape.name) } },
                                onSeekbarStyleChange = { style -> coroutineScope.launch(Dispatchers.IO) { sessionManager.setSeekbarStyle(style.name) } },
                                onRecenterAr = { playerViewModel.calibrateAudioAr() },
                                onSetFullScreen = { playerViewModel.setFullScreen(it) },
                                isSwitchingMode = isSwitchingMode,
                                sleepTimerOption = state.sleepTimerOption,
                                sleepTimerRemainingMs = state.sleepTimerRemainingMs,
                                currentProgress = currentProgress,
                                currentPosition = currentPosition,
                                currentDuration = currentDuration
                            )
                        }
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
                coil3.compose.AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop, alpha = 0.6f)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(4.dp)) {
                Text(text = song?.title ?: "Unknown", style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                Text(text = song?.artist ?: "Unknown Artist", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            }
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
            onAddToPlaylistClick = { playlistViewModel.showAddToPlaylistSheet(it) },
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

    // Related View
    AnimatedVisibility(
        visible = activeOverlay is PlayerOverlay.Related || (activeOverlay is PlayerOverlay.Actions && activeOverlay.fromRelated),
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        RelatedSheet(
            isVisible = true,
            relatedSongs = state.relatedSongs,
            isLoading = state.isFetchingRelated,
            selectedIndices = state.selectedRelatedIndices,
            onToggleSelection = actions.onToggleRelatedSelection,
            onSelectAll = actions.onSelectAllRelated,
            onClearSelection = actions.onClearRelatedSelection,
            onAddSelectedToQueue = {
                val selected = state.selectedRelatedIndices.mapNotNull { index ->
                    if (index < state.relatedSongs.size) state.relatedSongs[index] else null
                }
                actions.onAddRelatedToQueue(selected)
                actions.onClearRelatedSelection()
            },
            onAddSelectedToPlaylist = {
                val selected = state.selectedRelatedIndices.mapNotNull { index ->
                    if (index < state.relatedSongs.size) state.relatedSongs[index] else null
                }
                actions.onAddRelatedToPlaylist(selected)
                actions.onClearRelatedSelection()
            },
            onSongClick = {
                actions.onPlayRelated(it)
                onOverlayChange(PlayerOverlay.None)
            },
            onMoreClick = { onOverlayChange(PlayerOverlay.Actions(it, fromRelated = true)) },
            onClose = { if (currentOverlay is PlayerOverlay.Related) onOverlayChange(PlayerOverlay.None) },
            dominantColors = dominantColors,
            isDarkTheme = isAppInDarkTheme
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
                    when {
                        overlay.fromQueue -> onOverlayChange(PlayerOverlay.Queue)
                        overlay.fromRelated -> onOverlayChange(PlayerOverlay.Related)
                        else -> onOverlayChange(PlayerOverlay.None)
                    }
                }
            },            dominantColors = dominantColors,
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
            isFromQueue = (activeOverlay as? PlayerOverlay.Actions)?.fromQueue ?: false,
            isCurrentlyPlaying = menuSong.id == song?.id,
            onSetRingtone = { 
                if (ringtoneViewModel.ringtoneHelper.hasSettingsPermission(context)) {
                    ringtoneViewModel.showTrimmer(menuSong)
                } else {
                    Toast.makeText(context, "Please allow 'Modify System Settings' permission", Toast.LENGTH_LONG).show()
                    ringtoneViewModel.ringtoneHelper.requestSettingsPermission(context)
                }
            },
            isDarkTheme = isAppInDarkTheme
            )
    }

    // Simplified remaining overlays
    com.suvojeet.suvmusic.ui.screens.player.components.CommentsSheet(
        isVisible = activeOverlay is PlayerOverlay.Comments, comments = state.comments, isLoading = state.isFetchingComments,
        onDismiss = { if (currentOverlay is PlayerOverlay.Comments) onOverlayChange(PlayerOverlay.None) }, accentColor = dominantColors.accent, isLoggedIn = state.isLoggedIn,
        isPostingComment = state.isPostingComment, onPostComment = actions.onPostComment, isLoadingMore = state.isLoadingMoreComments, onLoadMore = actions.onLoadMoreComments,
        dominantColors = dominantColors,
        isDarkTheme = isAppInDarkTheme
    )

    if (song != null) {
        SongInfoSheet(
            song = song, 
            isVisible = activeOverlay is PlayerOverlay.SongInfo, 
            onDismiss = { if (currentOverlay is PlayerOverlay.SongInfo) onOverlayChange(PlayerOverlay.None) }, 
            onArtistClick = actions.onArtistClick, 
            audioCodec = playerState.audioCodec, 
            audioBitrate = playerState.audioBitrate,
            dominantColors = dominantColors,
            isDarkTheme = isAppInDarkTheme
        )
    }

    if (playlistUiState.showAddToPlaylistSheet && playlistUiState.selectedSongs.isNotEmpty()) {
        AddToPlaylistSheet(songs = playlistUiState.selectedSongs, isVisible = true, playlists = playlistUiState.userPlaylists, isLoading = playlistUiState.isLoadingPlaylists, onDismiss = { playlistViewModel.hideAddToPlaylistSheet() }, onAddToPlaylist = { playlistViewModel.addSongsToPlaylist(it) }, onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() })
    }

    SleepTimerSheet(
        isVisible = activeOverlay is PlayerOverlay.SleepTimer, 
        currentOption = state.sleepTimerOption, 
        remainingTimeFormatted = state.sleepTimerRemainingMs?.let { String.format("%d:%02d", it/60000, (it/1000)%60) }, 
        onSelectOption = actions.onSetSleepTimer, 
        onDismiss = { if (currentOverlay is PlayerOverlay.SleepTimer) onOverlayChange(PlayerOverlay.None) }, 
        accentColor = dominantColors.accent,
        dominantColors = dominantColors,
        isDarkTheme = isAppInDarkTheme
    )

    if (activeOverlay is PlayerOverlay.Equalizer) {
        com.suvojeet.suvmusic.ui.components.EqualizerSheet(
            isVisible = true, 
            onDismiss = { onOverlayChange(PlayerOverlay.None) }, 
            dominantColor = dominantColors.accent, 
            onEnabledChange = { playerViewModel.setEqEnabled(it) }, 
            onBandChange = { b, g -> playerViewModel.setEqBandGain(b, g) }, 
            onBandsChange = { playerViewModel.setEqBands(it) }, 
            onPreampChange = { playerViewModel.setEqPreamp(it) }, 
            onBassBoostChange = { playerViewModel.setBassBoost(it) }, 
            onVirtualizerChange = { playerViewModel.setVirtualizer(it) }, 
            onReset = { playerViewModel.resetEqBands() }, 
            initialEnabled = eqEnabled, 
            initialBands = eqBands, 
            initialPreamp = eqPreamp, 
            initialBassBoost = bassBoost, 
            initialVirtualizer = virtualizer,
            dominantColors = dominantColors,
            isDarkTheme = isAppInDarkTheme
        )
    }

    PlaybackSpeedSheet(
        isVisible = activeOverlay is PlayerOverlay.PlaybackSpeed,
        currentSpeed = playerState.playbackSpeed,
        currentPitch = playerState.pitch,
        onDismiss = { if (currentOverlay is PlayerOverlay.PlaybackSpeed) onOverlayChange(PlayerOverlay.None) },
        onApply = { speed, pitch -> actions.onSetPlaybackParameters(speed, pitch) },
        dominantColors = dominantColors,
        isDarkTheme = isAppInDarkTheme
    )

    OutputDeviceSheet(
        isVisible = activeOverlay is PlayerOverlay.OutputDevice,
        devices = playerState.availableDevices,
        onDeviceSelected = { actions.onSwitchDevice(it) },
        onDismiss = { if (currentOverlay is PlayerOverlay.OutputDevice) onOverlayChange(PlayerOverlay.None) },
        onRefreshDevices = { actions.onRefreshDevices() },
        accentColor = dominantColors.accent,
        dominantColors = dominantColors,
        isDarkTheme = isAppInDarkTheme
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
            },
            dominantColors = dominantColors
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

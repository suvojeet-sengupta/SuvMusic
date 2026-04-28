package com.suvojeet.suvmusic.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import android.app.Activity
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.core.model.OutputDevice
import com.suvojeet.suvmusic.core.model.PlayerState
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.screens.player.components.M3ELoadingOverlay
import com.suvojeet.suvmusic.ui.components.MeshGradientBackground
import com.suvojeet.suvmusic.ui.screens.LyricsScreen
import com.suvojeet.suvmusic.ui.components.AddToPlaylistSheet
import com.suvojeet.suvmusic.core.model.ArtworkShape
import com.suvojeet.suvmusic.core.model.ArtworkSize
import com.suvojeet.suvmusic.ui.screens.player.components.ModernQueueView
import com.suvojeet.suvmusic.ui.screens.player.components.SongInfoSection
import com.suvojeet.suvmusic.ui.components.VideoErrorDialog
import com.suvojeet.suvmusic.ui.screens.player.components.TimeLabelsWithQuality
import com.suvojeet.suvmusic.ui.screens.player.components.VolumeControl
import com.suvojeet.suvmusic.ui.screens.player.components.M3ESeekbarShimmer
import com.suvojeet.suvmusic.core.model.SeekbarStyle
import com.suvojeet.suvmusic.ui.viewmodel.PlaylistManagementViewModel
import com.suvojeet.suvmusic.ui.viewmodel.RingtoneViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import com.suvojeet.suvmusic.core.model.ThemeMode
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import com.suvojeet.suvmusic.data.repository.SponsorSegment
import com.suvojeet.suvmusic.ui.screens.player.FullScreenVideoPlayer

import com.suvojeet.suvmusic.core.model.PlayerStyle
import com.suvojeet.suvmusic.ui.screens.player.styles.YTMusicPlayerStyle
import com.suvojeet.suvmusic.ui.screens.player.styles.ClassicPlayerStyle
import com.suvojeet.suvmusic.ui.screens.player.styles.LiquidGlassPlayerStyle

import com.suvojeet.suvmusic.ui.screens.player.components.RelatedSheet
import com.suvojeet.suvmusic.ui.components.SongActionsSheet
import com.suvojeet.suvmusic.ui.components.SongInfoSheet
import com.suvojeet.suvmusic.ui.components.SleepTimerSheet
import com.suvojeet.suvmusic.ui.components.PlaybackSpeedSheet
import com.suvojeet.suvmusic.ui.components.OutputDeviceSheet
import com.suvojeet.suvmusic.ui.components.RingtoneTrimmerDialog
import com.suvojeet.suvmusic.ui.components.RingtoneProgressDialog

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
    val comments: List<com.suvojeet.suvmusic.core.model.Comment>? = null,
    val isFetchingComments: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isPostingComment: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val commentReplies: Map<String, List<com.suvojeet.suvmusic.core.model.Comment>> = emptyMap(),
    val loadingReplies: Set<String> = emptySet(),
    val sleepTimerOption: com.suvojeet.suvmusic.player.SleepTimerOption = com.suvojeet.suvmusic.player.SleepTimerOption.OFF,
    val sleepTimerRemainingMs: Long? = null,
    val isRadioMode: Boolean = false,
    val isLoadingMoreSongs: Boolean = false,
    val selectedLyricsProvider: LyricsProviderType = LyricsProviderType.AUTO,
    val enabledLyricsProviders: Map<LyricsProviderType, Boolean> = emptyMap()
)

/**
 * Actions for PlayerScreen to reduce parameter count.
 */
data class PlayerScreenActions(
    val onBack: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onToggleLike: () -> Unit,
    val onToggleDislike: () -> Unit,
    val onShuffleToggle: () -> Unit,
    val onRepeatToggle: () -> Unit,
    val onDownload: () -> Unit,
    val onToggleVideoMode: () -> Unit,
    val onDismissVideoError: () -> Unit,
    val onArtistClick: (String) -> Unit,
    val onAlbumClick: (String) -> Unit,
    val onPlayFromQueue: (Int) -> Unit,
    val onToggleAutoplay: () -> Unit,
    val onLoadMoreRadioSongs: () -> Unit,
    val onPostComment: (String) -> Unit,
    val onLoadMoreComments: () -> Unit,
    val onLoadReplies: (String) -> Unit = {},
    val onLoadMoreReplies: (String) -> Unit = {},
    val onSetSleepTimer: (com.suvojeet.suvmusic.player.SleepTimerOption, Int?) -> Unit,
    val onSwitchDevice: (OutputDevice) -> Unit,
    val onRefreshDevices: () -> Unit,
    val onSetPlaybackParameters: (Float, Float) -> Unit,
    val onLyricsProviderChange: (LyricsProviderType) -> Unit,
    val onImportLyrics: (String) -> Unit,
    val onShowAIEqualizer: () -> Unit,
    val onListenTogetherClick: () -> Unit,
    val onStartRadio: () -> Unit,
    val onToggleRelatedSelection: (Int) -> Unit,
    val onSelectAllRelated: () -> Unit,
    val onClearRelatedSelection: () -> Unit,
    val onAddRelatedToQueue: (List<com.suvojeet.suvmusic.core.model.Song>) -> Unit,
    val onAddRelatedToPlaylist: (List<com.suvojeet.suvmusic.core.model.Song>) -> Unit,
    val onPlayRelated: (com.suvojeet.suvmusic.core.model.Song) -> Unit,
    val onClearQueue: () -> Unit = {}
)

/**
 * Premium full-screen player with Apple Music-style design.
 * Features dynamic colors, quality badges, and queue view.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PlayerScreen(
    state: PlayerScreenState,
    actions: PlayerScreenActions,
    player: Player? = null,
    playlistViewModel: PlaylistManagementViewModel = koinViewModel(),
    ringtoneViewModel: RingtoneViewModel = koinViewModel<RingtoneViewModel>(),
    playerViewModel: com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel = koinViewModel(),
    mainViewModel: com.suvojeet.suvmusic.ui.viewmodel.MainViewModel = koinViewModel(),
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
    
    // Adaptive Layout Support
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val isExpanded = windowSizeClass.windowWidthSizeClass != androidx.window.core.layout.WindowWidthSizeClass.COMPACT
    val navigator = rememberSupportingPaneScaffoldNavigator()
    
    // Queue Selection & Sections
    val upNextSongs by playerViewModel.upNextSongs.collectAsStateWithLifecycle()
    val selectedQueueIndices by playerViewModel.selectedQueueIndices.collectAsStateWithLifecycle()
    
    // Customization styles from settings
    val sessionManager = remember { SessionManager(context) }
    val playerStyle by sessionManager.playerStyleFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.core.model.PlayerStyle.YT_MUSIC)
    val animatedBackgroundEnabled by sessionManager.playerAnimatedBackgroundFlow.collectAsStateWithLifecycle(initialValue = true)
    val currentArtworkShapeName by sessionManager.artworkShapeFlow.collectAsStateWithLifecycle(initialValue = ArtworkShape.ROUNDED_SQUARE.name)
    val currentArtworkSizeName by sessionManager.artworkSizeFlow.collectAsStateWithLifecycle(initialValue = ArtworkSize.LARGE.name)
    val currentSeekbarStyleName by sessionManager.seekbarStyleFlow.collectAsStateWithLifecycle(initialValue = SeekbarStyle.WAVEFORM.name)
    val volumeSliderEnabled by sessionManager.volumeSliderEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val playerGlassBlur by sessionManager.playerGlassBlurFlow.collectAsStateWithLifecycle(initialValue = 60f)
    val playerGlassIntensity by sessionManager.playerGlassIntensityFlow.collectAsStateWithLifecycle(initialValue = 1f)
    val audioArEnabled by sessionManager.audioArEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val rotatingVinylAnimationEnabled by sessionManager.rotatingVinylAnimationEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    
    val themeMode by sessionManager.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val isAppInDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val isAIAutoModeEnabled by playerViewModel.isAIAutoModeEnabled.collectAsStateWithLifecycle()
    val aiAutoStatus by playerViewModel.aiAutoStatus.collectAsStateWithLifecycle()

    // Lyrics appearance
    val lyricsTextPosition by sessionManager.lyricsTextPositionFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.core.model.LyricsTextPosition.CENTER)
    val lyricsAnimationType by sessionManager.lyricsAnimationTypeFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.core.model.LyricsAnimationType.WORD)
    val lyricsLineSpacing by sessionManager.lyricsLineSpacingFlow.collectAsStateWithLifecycle(initialValue = 1.2f)
    val lyricsFontSize by sessionManager.lyricsFontSizeFlow.collectAsStateWithLifecycle(initialValue = 24f)
    val lyricsBlur by sessionManager.lyricsBlurFlow.collectAsStateWithLifecycle(initialValue = 0f)

    // Audio Effects State
    val eqEnabled by playerViewModel.getEqEnabled().collectAsStateWithLifecycle(initialValue = false)
    val eqBands by playerViewModel.getEqBands().collectAsStateWithLifecycle(initialValue = FloatArray(10) { 0f })
    val eqPreamp by playerViewModel.getEqPreamp().collectAsStateWithLifecycle(initialValue = 0f)
    val bassBoost by playerViewModel.getBassBoost().collectAsStateWithLifecycle(initialValue = 0f)
    val virtualizer by playerViewModel.getVirtualizer().collectAsStateWithLifecycle(initialValue = 0f)

    val currentArtworkShape = try { ArtworkShape.valueOf(currentArtworkShapeName) } catch (e: Exception) { ArtworkShape.ROUNDED_SQUARE }
    val currentArtworkSize = try { ArtworkSize.valueOf(currentArtworkSizeName) } catch (e: Exception) { ArtworkSize.LARGE }
    val currentSeekbarStyle = try { SeekbarStyle.valueOf(currentSeekbarStyleName) } catch (e: Exception) { SeekbarStyle.WAVEFORM }

    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    
    val finalColors = rememberDominantColors(song?.thumbnailUrl, isAppInDarkTheme)
    val animatedPrimary by animateColorAsState(targetValue = finalColors.primary, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "primary")
    val animatedSecondary by animateColorAsState(targetValue = finalColors.secondary, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "secondary")
    val animatedAccent by animateColorAsState(targetValue = finalColors.accent, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "accent")
    val animatedOnBg by animateColorAsState(targetValue = finalColors.onBackground, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "onBg")
    
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
    
    // Sync navigator with activeOverlay for adaptive layouts
    LaunchedEffect(activeOverlay) {
        if (isExpanded) {
            when (activeOverlay) {
                is PlayerOverlay.Queue, is PlayerOverlay.Lyrics, is PlayerOverlay.Related -> {
                    navigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
                }
                else -> {
                    if (navigator.canNavigateBack()) {
                        navigator.navigateBack()
                    }
                }
            }
        }
    }

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

    val playerStateProvider by androidx.compose.runtime.rememberUpdatedState(playerState)

    val progressProvider = remember { { playerStateProvider.progress } }
    val positionProvider = remember { { playerStateProvider.currentPosition } }
    val durationProvider = remember { { playerStateProvider.duration } }

    val handleDoubleTapSeek: (Boolean) -> Unit = remember {
        { forward ->
            val currentPos = playerStateProvider.currentPosition
            val duration = playerStateProvider.duration
            val current = pendingSeekPosition ?: currentPos
            val seekAmount = 10000L // default
            val newPos = if (forward) (current + seekAmount).coerceAtMost(duration) else (current - seekAmount).coerceAtLeast(0)
            pendingSeekPosition = newPos
            seekDebounceJob?.cancel()
            seekDebounceJob = coroutineScope.launch {
                delay(400)
                actions.onSeekTo(newPos)
                delay(600)
                pendingSeekPosition = null
            }
        }
    }

    val playerBackgroundColor = if (isAppInDarkTheme) Color.Black else Color.White

    if (isInPip) {
        PiPPlayerContent(song = song, isVideoMode = playerState.isVideoMode, player = player)
    } else {
        Box(modifier = Modifier.fillMaxSize().background(playerBackgroundColor).graphicsLayer { alpha = bgLoadingAlpha }) {
            // Background — skip the mesh gradient for Liquid Glass; it draws its own blurred backdrop.
            if (playerStyle == PlayerStyle.LIQUID_GLASS) {
                // intentional: LiquidGlassPlayerStyle draws its own full-screen backdrop.
            } else if (animatedBackgroundEnabled && !playerState.isVideoMode) {
                MeshGradientBackground(dominantColors = dominantColors, backgroundColor = playerBackgroundColor)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(dominantColors.secondary, dominantColors.primary, playerBackgroundColor))))
            }

            val playerMainContent: @Composable () -> Unit = {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useWideLayout = maxWidth > 500.dp || isExpanded
                    val isCompactHeight = maxHeight < 600.dp

                    when (playerStyle) {
                        PlayerStyle.YT_MUSIC -> {
                            YTMusicPlayerStyle(
                                song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
                                currentSeekbarStyle, sponsorSegments, audioArEnabled, rotatingVinylAnimationEnabled,
                                player, isFullScreen, isCompactHeight, useWideLayout, actions,
                                onShowActions = { activeOverlay = PlayerOverlay.Actions(song!!) },
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
                                progressProvider = progressProvider,
                                positionProvider = positionProvider,
                                durationProvider = durationProvider,
                                isAIEnabled = isAIAutoModeEnabled,
                                aiStatus = aiAutoStatus,
                                windowSizeClass = windowSizeClass
                            )
                        }
                        PlayerStyle.LIQUID_GLASS -> {
                            LiquidGlassPlayerStyle(
                                song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
                                currentSeekbarStyle, sponsorSegments, audioArEnabled, rotatingVinylAnimationEnabled,
                                player, isFullScreen, isCompactHeight, useWideLayout, actions,
                                onShowActions = { activeOverlay = PlayerOverlay.Actions(song!!) },
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
                                progressProvider = progressProvider,
                                positionProvider = positionProvider,
                                durationProvider = durationProvider,
                                isAIEnabled = isAIAutoModeEnabled,
                                aiStatus = aiAutoStatus,
                                windowSizeClass = windowSizeClass,
                                blurRadius = playerGlassBlur,
                                intensity = playerGlassIntensity
                            )
                        }
                        PlayerStyle.CLASSIC -> {
                            ClassicPlayerStyle(
                                song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
                                currentSeekbarStyle, sponsorSegments, audioArEnabled, rotatingVinylAnimationEnabled,
                                player, isFullScreen, isCompactHeight, useWideLayout, actions,
                                onShowActions = { activeOverlay = PlayerOverlay.Actions(song!!) },
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
                                progressProvider = progressProvider,
                                positionProvider = positionProvider,
                                durationProvider = durationProvider,
                                isAIEnabled = isAIAutoModeEnabled,
                                aiStatus = aiAutoStatus,
                                windowSizeClass = windowSizeClass
                            )
                        }
                    }
                }
            }

            // Only use SupportingPaneScaffold when the device actually has room for two
            // side-by-side panes (tablets / large foldables / desktop). On phones the
            // scaffold's directive in material3.adaptive 1.3-alpha10 could split the
            // screen into two vertical panes on some OEMs (reported on Poco X6 Neo and
            // X5 Pro 5G), which shrank the main pane height and made the player layout
            // treat itself as "very short" — collapsing the artwork to a tiny thumbnail.
            if (isExpanded) {
                SupportingPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    mainPane = {
                        AnimatedPane(modifier = Modifier.fillMaxSize()) {
                            playerMainContent()
                        }
                    },
                    supportingPane = {
                        AnimatedPane(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
                            AdaptiveSupportingContent(
                                state = state, actions = actions, activeOverlay = activeOverlay,
                                onOverlayChange = { activeOverlay = it }, dominantColors = dominantColors,
                                playerViewModel = playerViewModel, playlistViewModel = playlistViewModel,
                                upNextSongs = upNextSongs, selectedQueueIndices = selectedQueueIndices,
                                isAppInDarkTheme = isAppInDarkTheme, animatedBackgroundEnabled = animatedBackgroundEnabled,
                                lyricsTextPosition = lyricsTextPosition, lyricsAnimationType = lyricsAnimationType,
                                lyricsLineSpacing = lyricsLineSpacing, lyricsFontSize = lyricsFontSize, lyricsBlur = lyricsBlur,
                                sessionManager = sessionManager, coroutineScope = coroutineScope,
                                progressProvider = progressProvider,
                                positionProvider = positionProvider,
                                durationProvider = durationProvider
                            )
                        }
                    }
                )
            } else {
                playerMainContent()
            }

            // Modals and non-pane overlays (always floating)
            Box(modifier = Modifier.fillMaxSize()) {
                OverlaysContent(
                    state = state, actions = actions.copy(onClearQueue = { playerViewModel.clearQueue() }), activeOverlay = activeOverlay, onOverlayChange = { activeOverlay = it },
                    dominantColors = dominantColors, playerViewModel = playerViewModel, playlistViewModel = playlistViewModel,
                    ringtoneViewModel = koinViewModel<RingtoneViewModel>(), // Explicit type for clarity and to fix inference errors
                    upNextSongs = upNextSongs, selectedQueueIndices = selectedQueueIndices,
                    isAppInDarkTheme = isAppInDarkTheme, animatedBackgroundEnabled = animatedBackgroundEnabled,
                    volumeSliderEnabled = volumeSliderEnabled, volumeKeyEvents = volumeKeyEvents,
                    lyricsTextPosition = lyricsTextPosition, lyricsAnimationType = lyricsAnimationType,
                    lyricsLineSpacing = lyricsLineSpacing, lyricsFontSize = lyricsFontSize, lyricsBlur = lyricsBlur,
                    sessionManager = sessionManager, coroutineScope = coroutineScope, isFullScreen = isFullScreen,
                    eqEnabled = eqEnabled, eqBands = eqBands, eqPreamp = eqPreamp, bassBoost = bassBoost, virtualizer = virtualizer,
                    isExpanded = isExpanded,
                    progressProvider = progressProvider,
                    positionProvider = positionProvider,
                    durationProvider = durationProvider
                )
            }
        }
    }
}

@Composable
fun AdaptiveSupportingContent(
    state: PlayerScreenState, actions: PlayerScreenActions, activeOverlay: PlayerOverlay, onOverlayChange: (PlayerOverlay) -> Unit,
    dominantColors: DominantColors, playerViewModel: com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel,
    playlistViewModel: PlaylistManagementViewModel, upNextSongs: List<com.suvojeet.suvmusic.core.model.Song>,
    selectedQueueIndices: Set<Int>, isAppInDarkTheme: Boolean, animatedBackgroundEnabled: Boolean,
    lyricsTextPosition: com.suvojeet.suvmusic.core.model.LyricsTextPosition,
    lyricsAnimationType: com.suvojeet.suvmusic.core.model.LyricsAnimationType,
    lyricsLineSpacing: Float, lyricsFontSize: Float, lyricsBlur: Float, sessionManager: SessionManager,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    progressProvider: () -> Float,
    positionProvider: () -> Long,
    durationProvider: () -> Long
) {
    val song = state.playbackInfo.currentSong
    val playerState = state.playerState

    when (activeOverlay) {
        is PlayerOverlay.Queue -> {
            ModernQueueView(
                currentSong = song, queue = playerState.queue, upNextSongs = upNextSongs, selectedQueueIndices = selectedQueueIndices,
                onToggleSelection = { playerViewModel.toggleQueueSelection(it) }, onSelectAll = { playerViewModel.selectAllQueueItems() }, onClearSelection = { playerViewModel.clearQueueSelection() },
                currentIndex = playerState.currentIndex, isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode,
                isAutoplayEnabled = playerState.isAutoplayEnabled, isFavorite = playerState.isLiked, isRadioMode = state.isRadioMode, isLoadingMore = state.isLoadingMoreSongs,
                onBack = { onOverlayChange(PlayerOverlay.None) }, onSongClick = actions.onPlayFromQueue, onPlayPause = actions.onPlayPause,
                onToggleShuffle = actions.onShuffleToggle, onToggleRepeat = actions.onRepeatToggle, onToggleAutoplay = actions.onToggleAutoplay, onToggleLike = actions.onToggleLike,
                onMoreClick = { onOverlayChange(PlayerOverlay.Actions(it, fromQueue = true)) }, onLoadMore = actions.onLoadMoreRadioSongs, onMoveItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
                onRemoveItems = { playerViewModel.removeQueueItems(it) }, onSaveAsPlaylist = { t, d, p, s -> playerViewModel.saveQueueAsPlaylist(t, d, p, s) { if (it) com.suvojeet.suvmusic.util.SnackbarUtil.showSuccess("Saved") } },
                onAddToPlaylistClick = { playlistViewModel.showAddToPlaylistSheet(it) },
                onPlayNext = { playerViewModel.playNext(it) },
                onAddToQueue = { playerViewModel.addToQueue(it) },
                onClearQueue = { playerViewModel.clearQueue() },
                dominantColors = dominantColors, animatedBackgroundEnabled = animatedBackgroundEnabled, isDarkTheme = isAppInDarkTheme
            )
        }
        is PlayerOverlay.Lyrics -> {
            LyricsScreen(
                lyrics = state.lyrics, isFetching = state.isFetchingLyrics, currentTimeProvider = positionProvider, artworkUrl = song?.thumbnailUrl,
                onClose = { onOverlayChange(PlayerOverlay.None) }, isDarkTheme = isAppInDarkTheme, onSeekTo = actions.onSeekTo, songTitle = song?.title ?: "",
                artistName = song?.artist ?: "", songId = song?.id ?: "", duration = playerState.duration, selectedProvider = state.selectedLyricsProvider,
                enabledProviders = state.enabledLyricsProviders, onProviderChange = actions.onLyricsProviderChange, onImportLyrics = actions.onImportLyrics, lyricsTextPosition = lyricsTextPosition,
                lyricsAnimationType = lyricsAnimationType, lyricsLineSpacing = lyricsLineSpacing, lyricsFontSize = lyricsFontSize, lyricsBlur = lyricsBlur,
                onLineSpacingChange = { coroutineScope.launch { sessionManager.setLyricsLineSpacing(it) } }, onFontSizeChange = { coroutineScope.launch { sessionManager.setLyricsFontSize(it) } },
                onBlurChange = { coroutineScope.launch { sessionManager.setLyricsBlur(it) } },
                onTextPositionChange = { coroutineScope.launch { sessionManager.setLyricsTextPosition(it) } }, onAnimationTypeChange = { coroutineScope.launch { sessionManager.setLyricsAnimationType(it) } },
                isPlaying = playerState.isPlaying, onPlayPause = actions.onPlayPause, onNext = actions.onNext, onPrevious = actions.onPrevious
            )
        }
        is PlayerOverlay.Related -> {
            RelatedSheet(
                isVisible = true, relatedSongs = state.relatedSongs, isLoading = state.isFetchingRelated, selectedIndices = state.selectedRelatedIndices,
                onToggleSelection = actions.onToggleRelatedSelection, onSelectAll = actions.onSelectAllRelated, onClearSelection = actions.onClearRelatedSelection,
                onAddSelectedToQueue = { val selected = state.selectedRelatedIndices.mapNotNull { if (it < state.relatedSongs.size) state.relatedSongs[it] else null }; actions.onAddRelatedToQueue(selected); actions.onClearRelatedSelection() },
                onAddSelectedToPlaylist = { val selected = state.selectedRelatedIndices.mapNotNull { if (it < state.relatedSongs.size) state.relatedSongs[it] else null }; actions.onAddRelatedToPlaylist(selected); actions.onClearRelatedSelection() },
                onSongClick = { actions.onPlayRelated(it); onOverlayChange(PlayerOverlay.None) }, onMoreClick = { onOverlayChange(PlayerOverlay.Actions(it, fromRelated = true)) },
                onClose = { onOverlayChange(PlayerOverlay.None) }, dominantColors = dominantColors, isDarkTheme = isAppInDarkTheme
            )
        }
        else -> {
            // Placeholder or empty state for supporting pane when nothing is selected
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select Queue or Lyrics to view here", color = dominantColors.onBackground.copy(alpha = 0.5f))
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
    playlistViewModel: PlaylistManagementViewModel, ringtoneViewModel: RingtoneViewModel,
    upNextSongs: List<com.suvojeet.suvmusic.core.model.Song>, selectedQueueIndices: Set<Int>, isAppInDarkTheme: Boolean,
    animatedBackgroundEnabled: Boolean, volumeSliderEnabled: Boolean, volumeKeyEvents: SharedFlow<Unit>?,
    lyricsTextPosition: com.suvojeet.suvmusic.core.model.LyricsTextPosition, lyricsAnimationType: com.suvojeet.suvmusic.core.model.LyricsAnimationType,
    lyricsLineSpacing: Float, lyricsFontSize: Float, lyricsBlur: Float, sessionManager: SessionManager, coroutineScope: kotlinx.coroutines.CoroutineScope,
    isFullScreen: Boolean, eqEnabled: Boolean, eqBands: FloatArray, eqPreamp: Float, bassBoost: Float, virtualizer: Float,
    isExpanded: Boolean = false,
    progressProvider: () -> Float,
    positionProvider: () -> Long,
    durationProvider: () -> Long
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

    // Modal Overlays (Queue, Lyrics, Related - only if NOT in expanded pane mode)
    if (!isExpanded) {
        // Queue View
        AnimatedVisibility(
            visible = activeOverlay is PlayerOverlay.Queue || (activeOverlay is PlayerOverlay.Actions && activeOverlay.fromQueue),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            ModernQueueView(
                currentSong = song, queue = playerState.queue, upNextSongs = upNextSongs, selectedQueueIndices = selectedQueueIndices,
                onToggleSelection = { playerViewModel.toggleQueueSelection(it) }, onSelectAll = { playerViewModel.selectAllQueueItems() }, onClearSelection = { playerViewModel.clearQueueSelection() },
                currentIndex = playerState.currentIndex, isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode,
                isAutoplayEnabled = playerState.isAutoplayEnabled, isFavorite = playerState.isLiked, isRadioMode = state.isRadioMode, isLoadingMore = state.isLoadingMoreSongs,
                onBack = { if (currentOverlay is PlayerOverlay.Queue) onOverlayChange(PlayerOverlay.None) }, onSongClick = actions.onPlayFromQueue, onPlayPause = actions.onPlayPause,
                onToggleShuffle = actions.onShuffleToggle, onToggleRepeat = actions.onRepeatToggle, onToggleAutoplay = actions.onToggleAutoplay, onToggleLike = actions.onToggleLike,
                onMoreClick = { onOverlayChange(PlayerOverlay.Actions(it, fromQueue = true)) }, onLoadMore = actions.onLoadMoreRadioSongs, onMoveItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
                onRemoveItems = { playerViewModel.removeQueueItems(it) }, onSaveAsPlaylist = { t, d, p, s -> playerViewModel.saveQueueAsPlaylist(t, d, p, s) { if (it) com.suvojeet.suvmusic.util.SnackbarUtil.showSuccess("Saved") } },
                onAddToPlaylistClick = { playlistViewModel.showAddToPlaylistSheet(it) },
                onPlayNext = { playerViewModel.playNext(it) },
                onAddToQueue = { playerViewModel.addToQueue(it) },
                onClearQueue = actions.onClearQueue,
                dominantColors = dominantColors, animatedBackgroundEnabled = animatedBackgroundEnabled, isDarkTheme = isAppInDarkTheme
            )
        }

        // Lyrics View
        AnimatedVisibility(visible = activeOverlay is PlayerOverlay.Lyrics, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            LyricsScreen(
                lyrics = state.lyrics, isFetching = state.isFetchingLyrics, currentTimeProvider = positionProvider, artworkUrl = song?.thumbnailUrl,
                onClose = { if (currentOverlay is PlayerOverlay.Lyrics) onOverlayChange(PlayerOverlay.None) }, isDarkTheme = isAppInDarkTheme, onSeekTo = actions.onSeekTo, songTitle = song?.title ?: "",
                artistName = song?.artist ?: "", songId = song?.id ?: "", duration = playerState.duration, selectedProvider = state.selectedLyricsProvider,
                enabledProviders = state.enabledLyricsProviders, onProviderChange = actions.onLyricsProviderChange, onImportLyrics = actions.onImportLyrics, lyricsTextPosition = lyricsTextPosition,
                lyricsAnimationType = lyricsAnimationType, lyricsLineSpacing = lyricsLineSpacing, lyricsFontSize = lyricsFontSize, lyricsBlur = lyricsBlur,
                onLineSpacingChange = { coroutineScope.launch { sessionManager.setLyricsLineSpacing(it) } }, onFontSizeChange = { coroutineScope.launch { sessionManager.setLyricsFontSize(it) } },
                onBlurChange = { coroutineScope.launch { sessionManager.setLyricsBlur(it) } },
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
                isVisible = true, relatedSongs = state.relatedSongs, isLoading = state.isFetchingRelated, selectedIndices = state.selectedRelatedIndices,
                onToggleSelection = actions.onToggleRelatedSelection, onSelectAll = actions.onSelectAllRelated, onClearSelection = actions.onClearRelatedSelection,
                onAddSelectedToQueue = { val selected = state.selectedRelatedIndices.mapNotNull { if (it < state.relatedSongs.size) state.relatedSongs[it] else null }; actions.onAddRelatedToQueue(selected); actions.onClearRelatedSelection() },
                onAddSelectedToPlaylist = { val selected = state.selectedRelatedIndices.mapNotNull { if (it < state.relatedSongs.size) state.relatedSongs[it] else null }; actions.onAddRelatedToPlaylist(selected); actions.onClearRelatedSelection() },
                onSongClick = { actions.onPlayRelated(it); onOverlayChange(PlayerOverlay.None) }, onMoreClick = { onOverlayChange(PlayerOverlay.Actions(it, fromRelated = true)) },
                onClose = { if (currentOverlay is PlayerOverlay.Related) onOverlayChange(PlayerOverlay.None) }, dominantColors = dominantColors, isDarkTheme = isAppInDarkTheme
            )
        }
    }

    // Other Sheets (Always modal/floating)
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
            }, dominantColors = dominantColors,
            isDownloaded = playerViewModel.isDownloaded(menuSong.id), onToggleFavorite = { if (menuSong.id == song?.id) actions.onToggleLike() else playerViewModel.likeSong(menuSong) },
            onToggleDislike = { if (menuSong.id == song?.id) actions.onToggleDislike() else playerViewModel.dislikeCurrentSong() },
            isFavorite = if (menuSong.id == song?.id) playerState.isLiked else false, isDisliked = if (menuSong.id == song?.id) playerState.isDisliked else false,
            onDownload = { if (menuSong.id == song?.id) actions.onDownload() else com.suvojeet.suvmusic.service.DownloadService.startDownload(context, menuSong) },
            onDeleteDownload = { playerViewModel.deleteDownload(menuSong.id) }, onPlayNext = { playerViewModel.playNext(menuSong) }, onAddToQueue = { playerViewModel.addToQueue(menuSong) },
            onViewInfo = { onOverlayChange(PlayerOverlay.SongInfo) }, onAddToPlaylist = { onOverlayChange(PlayerOverlay.None); playlistViewModel.showAddToPlaylistSheet(menuSong) },
            onViewComments = { onOverlayChange(PlayerOverlay.Comments) }, onSleepTimer = { onOverlayChange(PlayerOverlay.SleepTimer) }, onStartRadio = { actions.onStartRadio() },
            onListenTogether = { onOverlayChange(PlayerOverlay.None); actions.onListenTogetherClick() }, 
            onPlaybackSpeed = { onOverlayChange(PlayerOverlay.None); onOverlayChange(PlayerOverlay.PlaybackSpeed) }, onEqualizerClick = { onOverlayChange(PlayerOverlay.None); onOverlayChange(PlayerOverlay.Equalizer) },
            currentSpeed = playerState.playbackSpeed, isFromQueue = (activeOverlay as? PlayerOverlay.Actions)?.fromQueue ?: false, isCurrentlyPlaying = menuSong.id == song?.id,
            onSetRingtone = { if (ringtoneViewModel.ringtoneHelper.hasSettingsPermission(context)) ringtoneViewModel.showTrimmer(menuSong) else { com.suvojeet.suvmusic.util.SnackbarUtil.showWarning("Please allow 'Modify System Settings' permission"); ringtoneViewModel.ringtoneHelper.requestSettingsPermission(context) } },
            isDarkTheme = isAppInDarkTheme
        )
    }

    com.suvojeet.suvmusic.ui.screens.player.components.CommentsSheet(
        isVisible = activeOverlay is PlayerOverlay.Comments, comments = state.comments, isLoading = state.isFetchingComments,
        onDismiss = { if (currentOverlay is PlayerOverlay.Comments) onOverlayChange(PlayerOverlay.None) }, accentColor = dominantColors.accent, isLoggedIn = state.isLoggedIn,
        isPostingComment = state.isPostingComment, onPostComment = actions.onPostComment, isLoadingMore = state.isLoadingMoreComments, onLoadMore = actions.onLoadMoreComments,
        commentReplies = state.commentReplies, loadingReplies = state.loadingReplies,
        onLoadReplies = actions.onLoadReplies, onLoadMoreReplies = actions.onLoadMoreReplies,
        dominantColors = dominantColors, isDarkTheme = isAppInDarkTheme
    )

    if (song != null) {
        SongInfoSheet(
            song = song, isVisible = activeOverlay is PlayerOverlay.SongInfo, onDismiss = { if (currentOverlay is PlayerOverlay.SongInfo) onOverlayChange(PlayerOverlay.None) }, 
            onArtistClick = actions.onArtistClick, audioCodec = playerState.audioCodec, audioBitrate = playerState.audioBitrate,
            dominantColors = dominantColors, isDarkTheme = isAppInDarkTheme
        )
    }

    if (playlistUiState.showAddToPlaylistSheet && playlistUiState.selectedSongs.isNotEmpty()) {
        AddToPlaylistSheet(songs = playlistUiState.selectedSongs, isVisible = true, playlists = playlistUiState.userPlaylists, isLoading = playlistUiState.isLoadingPlaylists, onDismiss = { playlistViewModel.hideAddToPlaylistSheet() }, onAddToPlaylist = { playlistViewModel.addSongsToPlaylist(it) }, onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() })
    }

    SleepTimerSheet(
        isVisible = activeOverlay is PlayerOverlay.SleepTimer, currentOption = state.sleepTimerOption, 
        remainingTimeFormatted = state.sleepTimerRemainingMs?.let { String.format("%d:%02d", it/60000, (it/1000)%60) }, 
        onSelectOption = actions.onSetSleepTimer, onDismiss = { if (currentOverlay is PlayerOverlay.SleepTimer) onOverlayChange(PlayerOverlay.None) }, 
        accentColor = dominantColors.accent, dominantColors = dominantColors, isDarkTheme = isAppInDarkTheme
    )

    if (activeOverlay is PlayerOverlay.Equalizer) {
        com.suvojeet.suvmusic.ui.components.EqualizerSheet(
            isVisible = true, onDismiss = { onOverlayChange(PlayerOverlay.None) }, dominantColor = dominantColors.accent, 
            onEnabledChange = { playerViewModel.setEqEnabled(it) }, onBandChange = { b, g -> playerViewModel.setEqBandGain(b, g) }, 
            onBandsChange = { playerViewModel.setEqBands(it) }, onPreampChange = { playerViewModel.setEqPreamp(it) }, 
            onBassBoostChange = { playerViewModel.setBassBoost(it) }, onVirtualizerChange = { playerViewModel.setVirtualizer(it) }, 
            onReset = { playerViewModel.resetEqBands() }, onAIEqualizerClick = actions.onShowAIEqualizer, initialEnabled = eqEnabled,
            initialBands = eqBands, initialPreamp = eqPreamp, initialBassBoost = bassBoost, initialVirtualizer = virtualizer,
            dominantColors = dominantColors, isDarkTheme = isAppInDarkTheme
        )
    }

    PlaybackSpeedSheet(
        isVisible = activeOverlay is PlayerOverlay.PlaybackSpeed, currentSpeed = playerState.playbackSpeed, currentPitch = playerState.pitch,
        onDismiss = { if (currentOverlay is PlayerOverlay.PlaybackSpeed) onOverlayChange(PlayerOverlay.None) },
        onApply = { speed, pitch -> actions.onSetPlaybackParameters(speed, pitch) }, dominantColors = dominantColors, isDarkTheme = isAppInDarkTheme
    )

    OutputDeviceSheet(
        isVisible = activeOverlay is PlayerOverlay.OutputDevice, devices = playerState.availableDevices, onDeviceSelected = { actions.onSwitchDevice(it) },
        onDismiss = { if (currentOverlay is PlayerOverlay.OutputDevice) onOverlayChange(PlayerOverlay.None) }, onRefreshDevices = { actions.onRefreshDevices() },
        accentColor = dominantColors.accent, dominantColors = dominantColors, isDarkTheme = isAppInDarkTheme, listenTogetherManager = playerViewModel.listenTogetherManager
    )

    val ringtoneUiState by ringtoneViewModel.uiState.collectAsStateWithLifecycle()
    if (ringtoneUiState.showTrimmer && ringtoneUiState.targetSong != null) {
        RingtoneTrimmerDialog(
            isVisible = true, song = ringtoneUiState.targetSong!!, onDismiss = { ringtoneViewModel.hideTrimmer() },
            onResolveStreamUrl = suspend { ringtoneViewModel.getStreamUrl(it) },
            onConfirm = { start, end -> ringtoneViewModel.setAsRingtone(context, ringtoneUiState.targetSong!!, start, end) },
            dominantColors = dominantColors
        )
    }
    
    if (ringtoneUiState.showProgress) {
        RingtoneProgressDialog(
            isVisible = true, progress = ringtoneUiState.progress, statusMessage = ringtoneUiState.statusMessage,
            isComplete = ringtoneUiState.isComplete, isSuccess = ringtoneUiState.isSuccess, onDismiss = { ringtoneViewModel.dismissProgress() },
            onOpenSettings = { ringtoneViewModel.ringtoneHelper.openRingtoneSettings(context, ringtoneUiState.ringtoneUri); ringtoneViewModel.dismissProgress() }
        )
    }

    if (isFullScreen) {
        FullScreenVideoPlayer(viewModel = playerViewModel, dominantColors = dominantColors, onDismiss = { playerViewModel.setFullScreen(false) })
    }
}

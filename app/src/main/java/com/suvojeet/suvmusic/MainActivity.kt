package com.suvojeet.suvmusic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Lock
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.AppTheme
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.data.model.MiniPlayerStyle
import com.suvojeet.suvmusic.navigation.Destination
import com.suvojeet.suvmusic.navigation.NavGraph
import com.suvojeet.suvmusic.ui.components.ExpressiveBottomNav
import com.suvojeet.suvmusic.ui.components.player.ExpandablePlayerSheet
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import androidx.compose.ui.graphics.luminance
import com.suvojeet.suvmusic.ui.screens.player.components.VolumeIndicator
import com.suvojeet.suvmusic.ui.screens.player.components.SystemVolumeObserver
import com.suvojeet.suvmusic.ui.theme.SuvMusicTheme
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel
import com.suvojeet.suvmusic.ui.viewmodel.MainViewModel
import com.suvojeet.suvmusic.data.model.UpdateState
import androidx.activity.viewModels
import com.suvojeet.suvmusic.ui.components.UpdateAvailableDialog
import com.suvojeet.suvmusic.ui.components.DownloadProgressDialog
import com.suvojeet.suvmusic.ui.components.UpdateErrorDialog
import com.suvojeet.suvmusic.util.NetworkMonitor
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.suvojeet.suvmusic.pip.PipHelper
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val mainViewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var networkMonitor: NetworkMonitor
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    @Inject
    lateinit var downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository
    
    @Inject
    lateinit var musicPlayer: com.suvojeet.suvmusic.player.MusicPlayer

    @Inject
    lateinit var pipHelper: PipHelper

    private lateinit var audioManager: AudioManager
    
    // Track whether song is playing for volume key interception
    private var isSongPlaying: Boolean = false
    
    // Track whether in-app volume slider is enabled (if false, show system UI)
    private var isVolumeSliderEnabled: Boolean = true

    // Track whether Picture-in-Picture is enabled in settings
    private var isPipEnabled: Boolean = false
    
    // Flow to emit volume key events to the UI
    private val _volumeKeyEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Keep splash screen on until ViewModel reports isReady = true
        splashScreen.setKeepOnScreenCondition {
            !mainViewModel.uiState.value.isReady
        }
        
        enableEdgeToEdge()
        enableMaxRefreshRate()
        
        // Initialize audio manager for volume control
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        requestPermissions()
        
        setContent {
            val themeMode by sessionManager.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val dynamicColor by sessionManager.dynamicColorFlow.collectAsStateWithLifecycle(initialValue = true)
            val appTheme by sessionManager.appThemeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.DEFAULT)
            val pureBlackEnabled by sessionManager.pureBlackEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
            val forceMaxRefreshRate by sessionManager.forceMaxRefreshRateFlow.collectAsStateWithLifecycle(initialValue = true)
            val systemDarkTheme = isSystemInDarkTheme()
            LaunchedEffect(forceMaxRefreshRate) {
                if (forceMaxRefreshRate) {
                    enableMaxRefreshRate()
                } else {
                    // Reset to default
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = 0 // 0 means default/no preference
                    }
                }
            }

            // Observe PiP enabled state globally
            LaunchedEffect(Unit) {
                sessionManager.dynamicIslandEnabledFlow.collect { enabled ->
                    isPipEnabled = enabled
                    pipHelper.updatePipParams(this@MainActivity, isPipEnabled)
                }
            }

            val darkTheme = remember(themeMode, systemDarkTheme) {
                when (themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> systemDarkTheme
                }
            }
            
            SuvMusicTheme(
                darkTheme = darkTheme, 
                dynamicColor = dynamicColor,
                appTheme = appTheme,
                pureBlack = pureBlackEnabled
            ) {
                SuvMusicApp(
                    intent = intent,
                    networkMonitor = networkMonitor,
                    audioManager = audioManager,
                    volumeKeyEvents = _volumeKeyEvents,
                    downloadRepository = downloadRepository,
                    sessionManager = sessionManager, // Pass the injected instance
                    onPlaybackStateChanged = { hasSong -> 
                        isSongPlaying = hasSong
                        // Update PiP params whenever playback state changes
                        // so the play/pause icon stays in sync
                        pipHelper.updatePipParams(this@MainActivity, isPipEnabled)
                    },
                    onVolumeSliderEnabledChanged = { enabled ->
                        isVolumeSliderEnabled = enabled
                    }
                )
            }
        }
    }
    
    /**
     * Intercept hardware volume keys to control music volume
     * without showing the system volume UI panel - only when song is playing
     * and in-app volume slider is enabled.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Only intercept volume keys when a song is playing AND in-app volume slider is enabled
        // When volume slider is disabled, let system handle it (shows system volume UI)
        if (!isSongPlaying || !isVolumeSliderEnabled) {
            return super.dispatchKeyEvent(event)
        }
        
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        0 // No flags = no system UI
                    )
                    _volumeKeyEvents.tryEmit(Unit)
                }
                true // Consume the event
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        0 // No flags = no system UI
                    )
                    _volumeKeyEvents.tryEmit(Unit)
                }
                true // Consume the event
            }
            else -> super.dispatchKeyEvent(event)
        }
    }
    
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    
    private fun requestPermissions() {
        val missingPermissions = com.suvojeet.suvmusic.util.PermissionUtils.getMissingPermissions(this)
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun enableMaxRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val modes = display?.supportedModes
            val maxRefreshRate = modes?.maxByOrNull { it.refreshRate }?.refreshRate ?: return
            val preferredMode = modes.find { it.refreshRate == maxRefreshRate } ?: return
            
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = preferredMode.modeId
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // Disable video track for bandwidth optimization when backgrounded
        // but NOT when entering PiP mode (video needs to remain active for PiP)
        if (!isInPictureInPictureMode) {
            musicPlayer.optimizeBandwidth(true)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        lifecycleScope.launch {
            if (isSongPlaying && isPipEnabled) {
                // Enter PiP (only if enabled in Settings -> General -> Picture-in-Picture)
                val isVideoMode = musicPlayer.playerState.value.isVideoMode
                pipHelper.enterPipIfEligible(this@MainActivity, forceVideoPip = isVideoMode, isPipEnabled = isPipEnabled)
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        mainViewModel.setPictureInPictureMode(isInPictureInPictureMode)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Re-enable video track when returning to foreground
        musicPlayer.optimizeBandwidth(false)
        
        // Update PiP params (e.g., sync play/pause icon after returning from PiP)
        pipHelper.updatePipParams(this, isPipEnabled)
    }
}

@Composable
fun SuvMusicApp(
    intent: Intent? = null,
    networkMonitor: NetworkMonitor,
    audioManager: AudioManager,
    volumeKeyEvents: SharedFlow<Unit>? = null,
    downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository? = null,
    sessionManager: SessionManager, // Injected instance passed from MainActivity
    onPlaybackStateChanged: (Boolean) -> Unit,
    onVolumeSliderEnabledChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect volume slider enabled preference
    val volumeSliderEnabled by sessionManager.volumeSliderEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val miniPlayerAlpha by sessionManager.miniPlayerAlphaFlow.collectAsStateWithLifecycle(initialValue = 0f)
    val miniPlayerStyle by sessionManager.miniPlayerStyleFlow.collectAsStateWithLifecycle(initialValue = MiniPlayerStyle.FLOATING_PILL)
    
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val isLoggedIn by sessionManager.isLoggedInFlow.collectAsStateWithLifecycle(initialValue = false)

    val scope = androidx.compose.runtime.rememberCoroutineScope()    
    // Optimized states to reduce recompositions
    val playbackInfo by playerViewModel.playbackInfo.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.data.model.PlayerState())
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.data.model.PlayerState())
    val isPlayerExpanded by playerViewModel.isPlayerExpanded.collectAsStateWithLifecycle(initialValue = false)
    
    val lyrics by playerViewModel.lyricsState.collectAsStateWithLifecycle(initialValue = null)
    val isFetchingLyrics by playerViewModel.isFetchingLyrics.collectAsStateWithLifecycle(initialValue = false)
    val selectedLyricsProvider by playerViewModel.selectedLyricsProvider.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.AUTO)
    
    val comments by playerViewModel.commentsState.collectAsStateWithLifecycle(initialValue = null)
    val isFetchingComments by playerViewModel.isFetchingComments.collectAsStateWithLifecycle(initialValue = false)
    val isPostingComment by playerViewModel.isPostingComment.collectAsStateWithLifecycle(initialValue = false)
    val isLoadingMoreComments by playerViewModel.isLoadingMoreComments.collectAsStateWithLifecycle(initialValue = false)
    
    // Track if song is playing for Activity-level volume interception
    // Use playbackInfo (stable) to avoid recomposing the whole app shell on progress updates
    val hasSong = playbackInfo.currentSong != null
    LaunchedEffect(hasSong) {
        onPlaybackStateChanged(hasSong)
    }
    
    // Sync volume slider enabled state to Activity
    LaunchedEffect(volumeSliderEnabled) {
        onVolumeSliderEnabledChanged(volumeSliderEnabled)
    }

    // Handle intent changes
    LaunchedEffect(intent) {
        if (intent != null) {
            if (intent.action == Intent.ACTION_VIEW && intent.type?.startsWith("audio/") == true) {
                mainViewModel.handleAudioIntent(intent.data)
            } else {
                mainViewModel.handleDeepLink(intent.data)
            }
        }
    }

    // Handle MainEvents (Navigation, Toasts)
    LaunchedEffect(Unit) {
        mainViewModel.events.collect { event ->
            when (event) {
                is com.suvojeet.suvmusic.ui.viewmodel.MainEvent.PlayFromDeepLink -> {
                    playerViewModel.playFromDeepLink(event.videoId)
                }
                is com.suvojeet.suvmusic.ui.viewmodel.MainEvent.PlayFromLocalUri -> {
                    playerViewModel.playFromLocalUri(context, event.uri)
                }
                is com.suvojeet.suvmusic.ui.viewmodel.MainEvent.ShowToast -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Dialog State for Restricted HQ Audio
    var showRestrictedDialog by remember { mutableStateOf(false) }

    // Error Observer
    LaunchedEffect(playerState.error) {
        if (playerState.error == "RESTRICTED_HQ_AUDIO") {
            showRestrictedDialog = true
        }
    }

    if (showRestrictedDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRestrictedDialog = false },
            title = { androidx.compose.material3.Text("Restricted Access", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = { androidx.compose.material3.Text("You are not authorised to play HQ audio.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showRestrictedDialog = false }) {
                    androidx.compose.material3.Text("OK")
                }
            },
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
            }
        )
    }
    
    // Volume control states for global indicator
    var maxVolume by remember {
        mutableStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
    }
    var currentVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var lastVolumeChangeTime by remember { mutableStateOf(0L) }
    
    // Listen for Volume Key Events (Manual Trigger)
    LaunchedEffect(volumeKeyEvents) {
        volumeKeyEvents?.collect {
            // Update current volume (it might have changed, or not if at boundaries)
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            // Show indicator
            lastVolumeChangeTime = System.currentTimeMillis()
        }
    }
    
    // Listen for System Volume Changes
    SystemVolumeObserver(context = context) { newVol, newMax ->
        maxVolume = newMax
        if (currentVolume != newVol) {
            currentVolume = newVol
            lastVolumeChangeTime = System.currentTimeMillis()
        }
    }
    
    // Auto-hide volume indicator
    LaunchedEffect(lastVolumeChangeTime) {
        if (lastVolumeChangeTime > 0 && hasSong) {
            showVolumeIndicator = true
            kotlinx.coroutines.delay(2000) // 2 seconds delay
            showVolumeIndicator = false
        }
    }
    
    // Monitor network connectivity
    val isConnected by networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = networkMonitor.isCurrentlyConnected())
    
    // Show snackbar when offline for 30 seconds
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            snackbarHostState.showSnackbar(
                message = "No internet connection",
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            // Auto-dismiss after 30 seconds
            delay(30000)
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    
    // Sleep Timer
    val sleepTimerOption by playerViewModel.sleepTimerOption.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.player.SleepTimerOption.OFF)
    val sleepTimerRemainingMs by playerViewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle(initialValue = null)
    
    // Radio Mode
    val isRadioMode by playerViewModel.isRadioMode.collectAsStateWithLifecycle(initialValue = false)
    val isLoadingMoreSongs by playerViewModel.isLoadingMoreSongs.collectAsStateWithLifecycle(initialValue = false)
    val isMiniPlayerDismissed by playerViewModel.isMiniPlayerDismissed.collectAsStateWithLifecycle(initialValue = false)
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var currentDestination by remember { mutableStateOf<Destination>(Destination.Home) }
    
    // Restore Playback only if no deep link handled
    var restoreAttempted by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!restoreAttempted && intent?.data == null) {
            restoreAttempted = true
            if (playerState.currentSong == null) {
                playerViewModel.restoreLastPlayback()
            }
        }
    }
    
    // Update current destination based on route
    currentDestination = when (currentRoute) {
        Destination.Home.route -> Destination.Home
        Destination.Search.route -> Destination.Search
        Destination.Library.route -> Destination.Library
        Destination.Settings.route -> Destination.Settings
        else -> currentDestination
    }
    
    val showBottomNav = currentRoute in listOf(
        Destination.Home.route,
        Destination.Search.route,
        Destination.Library.route,
        Destination.Settings.route
    )
    
    // Auto-show MiniPlayer when returning to Home
    LaunchedEffect(currentRoute) {
        if (currentRoute == Destination.Home.route) {
            playerViewModel.showMiniPlayer()
        }
    }
    
    
    // Don't show MiniPlayer on Player screen itself or if explicitly dismissed
    // With bottom sheet, "Player screen" is just the expanded state.
    // We hide the sheet if the current route is one where we don't want player (e.g. login?)
    // or if dismissed.
    val showMiniPlayer = !isMiniPlayerDismissed && currentRoute != Destination.YouTubeLogin.route && hasSong
    
    // Don't show global volume indicator on PlayerScreen (it has its own) - 
    // For now, simpler to just show it if we have a song, unless we are in expanded state?
    // Let's keep it simple: show if song Playing
    val showGlobalVolumeIndicator = hasSong
    
    // Extract dominant colors from current song's album art
    val isAppInDarkTheme = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val songDominantColors = rememberDominantColors(
        imageUrl = playbackInfo.currentSong?.thumbnailUrl,
        isDarkTheme = isAppInDarkTheme
    )
    
    // Fallback colors for non-player screens (volume indicator etc.)
    val defaultDominantColors = if (playbackInfo.currentSong != null) songDominantColors else DominantColors(
        primary = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        secondary = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
        accent = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
        onBackground = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    )
    

    
    // Welcome Dialog State
    val onboardingCompleted by sessionManager.onboardingCompletedFlow.collectAsStateWithLifecycle(initialValue = true) // Start assuming true to avoid flicker if already done
    var showWelcomeDialog by remember { mutableStateOf(false) }
    
    // Check actual onboarding status on launch
    LaunchedEffect(Unit) {
        if (!sessionManager.isOnboardingCompleted()) {
            showWelcomeDialog = true
        }
    }
    
    // Check for TV Mode
    val isTv = remember { com.suvojeet.suvmusic.util.TvUtils.isTv(context) }

    if (showWelcomeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* Prevent dismiss */ },
            title = { 
                androidx.compose.material3.Text(
                    "Welcome to SuvMusic", 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 22.sp
                ) 
            },
            text = { 
                Column {
                    androidx.compose.material3.Text("Experience music like never before.\n\nSuvMusic offers high-quality playback, ad-free experience, and seamless streaming from YouTube Music.")
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Text("Login to sync your library or continue as guest.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { 
                        showWelcomeDialog = false
                        // Navigate to Login, which will set onboarding completed on success
                        navController.navigate(Destination.YouTubeLogin.route)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Text("Login with YT Music")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { 
                        showWelcomeDialog = false
                         // Mark onboarding as complete and stay on Home
                        scope.launch {
                            sessionManager.setOnboardingCompleted(true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Text("Continue without Login")
                }
            },
            icon = {
                 androidx.compose.material3.Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                 )
            }
        )
    }

        Box(modifier = Modifier.fillMaxSize()) {
             Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                },
                bottomBar = {
                    if (showBottomNav && !isTv) {
                        Column {

                            
                            // Bottom navigation
                            val navBarAlpha by sessionManager.navBarAlphaFlow.collectAsStateWithLifecycle(initialValue = 0.85f)
                            val iosLiquidGlassEnabled by sessionManager.iosLiquidGlassEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
                            
                            ExpressiveBottomNav(
                                currentDestination = currentDestination,
                                onDestinationChange = { destination ->
                                    navController.navigate(destination.route) {
                                        popUpTo(Destination.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                alpha = navBarAlpha,
                                iosLiquidGlassEnabled = iosLiquidGlassEnabled
                            )
                        }
                    }
                }
            ) { innerPadding ->
                // Make innerPadding available to children
                // We need to pass the bottom padding to the ExpandablePlayerSheet so it sits above the nav bar
                val currentBottomPadding = innerPadding.calculateBottomPadding()

                Row(modifier = Modifier.fillMaxSize()) {
                    if (isTv && showBottomNav) {
                        com.suvojeet.suvmusic.ui.components.TvNavigationRail(
                            currentDestination = currentDestination,
                            onDestinationChange = { destination ->
                                navController.navigate(destination.route) {
                                    popUpTo(Destination.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                    // NavGraph content with its own bottom padding for the nav bar
                    // We add EXTRA padding for the mini player (64dp) if it's visible
                    val miniPlayerHeight = if (showMiniPlayer) 64.dp else 0.dp
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = 0.dp // Allow content to flow behind nav bar for glass effect
                            )
                    ) {
                        NavGraph(
                            navController = navController,
                            playbackInfo = playbackInfo,
                            playerState = playerState,
                            sessionManager = sessionManager,
                            onPlaySong = { songs, index ->
                                if (songs.isNotEmpty() && index in songs.indices) {
                                    playerViewModel.playSong(songs[index], songs, index)
                                }
                            },
                            onPlayPause = { playerViewModel.togglePlayPause() },
                            onSeekTo = { playerViewModel.seekTo(it) },
                            onNext = { playerViewModel.seekToNext() },
                            onPrevious = { playerViewModel.seekToPrevious() },
                            onDownloadCurrentSong = { playerViewModel.downloadCurrentSong() },
                            onLikeCurrentSong = { playerViewModel.likeCurrentSong() },
                            onDislikeCurrentSong = { playerViewModel.dislikeCurrentSong() },
                            onShuffleToggle = { playerViewModel.toggleShuffle() },
                            onRepeatToggle = { playerViewModel.toggleRepeat() },
                            onToggleAutoplay = { playerViewModel.toggleAutoplay() },
                            onToggleVideoMode = { playerViewModel.toggleVideoMode() },
                            onDismissVideoError = { playerViewModel.dismissVideoError() },
                            onStartRadio = { song, initialQueue ->
                                val targetSong = song ?: playbackInfo.currentSong
                                targetSong?.let { 
                                    playerViewModel.startRadio(it, initialQueue)
                                }
                            },
                            onLoadMoreRadioSongs = { playerViewModel.loadMoreRadioSongs() },
                            isRadioMode = isRadioMode,
                            isLoadingMoreSongs = isLoadingMoreSongs,
                            onSwitchDevice = { playerViewModel.switchOutputDevice(it) },
                            onRefreshDevices = { playerViewModel.refreshDevices() },
                            player = playerViewModel.getPlayer(),
                            lyrics = lyrics,
                            isFetchingLyrics = isFetchingLyrics,
                            comments = comments,
                            isFetchingComments = isFetchingComments,
                            isLoggedIn = isLoggedIn,
                            isPostingComment = isPostingComment,
                            onPostComment = { commentText -> playerViewModel.postComment(commentText) },
                            isLoadingMoreComments = isLoadingMoreComments,
                            onLoadMoreComments = { playerViewModel.loadMoreComments() },
                            sleepTimerOption = sleepTimerOption,
                            sleepTimerRemainingMs = sleepTimerRemainingMs,
                            onSetSleepTimer = { option, minutes -> playerViewModel.setSleepTimer(option, minutes) },
                            onSetPlaybackParameters = { speed, pitch -> playerViewModel.setPlaybackParameters(speed, pitch) },
                            volumeKeyEvents = volumeKeyEvents,
                            downloadRepository = downloadRepository,
                            selectedLyricsProvider = selectedLyricsProvider,
                            enabledLyricsProviders = playerViewModel.enabledLyricsProviders.collectAsStateWithLifecycle().value,
                            onLyricsProviderChange = { playerViewModel.switchLyricsProvider(it) },
                            startDestination = Destination.Home.route, // Always start at Home
                            // Removed sharedTransitionScope
                            isTv = isTv,
                            dominantColors = defaultDominantColors
                        )
                    }


                }
            }
        }

    // Expandable Player Sheet - Overlay
    // Sits above Scaffold, aligned to bottom
    if (showMiniPlayer) {
        val density = LocalDensity.current
        val navBarPadding = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val navBarHeight = if (showBottomNav && !isTv) 80.dp else 0.dp
        val bottomPaddingPx = with(density) { navBarPadding.toPx() + navBarHeight.toPx() }

        ExpandablePlayerSheet(
            playerState = playerState,
            dominantColors = defaultDominantColors, 
            onPlayPause = { playerViewModel.togglePlayPause() },
            onNext = { playerViewModel.seekToNext() },
            onPrevious = { playerViewModel.seekToPrevious() },
            onClose = { playerViewModel.stop() },
            bottomPadding = bottomPaddingPx,
            isExpanded = isPlayerExpanded,
            userAlpha = miniPlayerAlpha,
            style = miniPlayerStyle,
            onExpandChange = { expanded ->
                if (expanded) playerViewModel.expandPlayer() else playerViewModel.collapsePlayer()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            expandedContent = { onCollapse ->
                com.suvojeet.suvmusic.ui.screens.player.PlayerScreen(
                    playbackInfo = playbackInfo,
                    playerState = playerState,
                    onPlayPause = { playerViewModel.togglePlayPause() },
                    onSeekTo = { playerViewModel.seekTo(it) },
                    onNext = { playerViewModel.seekToNext() },
                    onPrevious = { playerViewModel.seekToPrevious() },
                    onBack = onCollapse,
                    onDownload = { playerViewModel.downloadCurrentSong() },
                    onToggleLike = { playerViewModel.likeCurrentSong() },
                    onToggleDislike = { playerViewModel.dislikeCurrentSong() },
                    onShuffleToggle = { playerViewModel.toggleShuffle() },
                    onRepeatToggle = { playerViewModel.toggleRepeat() },
                    onToggleAutoplay = { playerViewModel.toggleAutoplay() },
                    onToggleVideoMode = { playerViewModel.toggleVideoMode() },
                    onDismissVideoError = { playerViewModel.dismissVideoError() },
                    onStartRadio = { 
                         playbackInfo.currentSong?.let { 
                             playerViewModel.startRadio(it, null)
                         }
                    },
                    onLoadMoreRadioSongs = { playerViewModel.loadMoreRadioSongs() },
                    isRadioMode = isRadioMode,
                    isLoadingMoreSongs = isLoadingMoreSongs,
                    player = playerViewModel.getPlayer(),
                    onPlayFromQueue = { index ->
                        if (playerState.queue.isNotEmpty() && index in playerState.queue.indices) {
                            playerViewModel.playSong(playerState.queue[index], playerState.queue, index)
                        }
                    },
                    onSwitchDevice = { playerViewModel.switchOutputDevice(it) },
                    onRefreshDevices = { playerViewModel.refreshDevices() },
                    onArtistClick = { artistId ->
                        onCollapse()
                        navController.navigate(Destination.Artist(artistId).route)
                    },
                    onAlbumClick = { albumId ->
                        onCollapse()
                        navController.navigate(Destination.Album(albumId = albumId, name = null, thumbnailUrl = null).route)
                    },
                    onSetPlaybackParameters = { speed, pitch -> playerViewModel.setPlaybackParameters(speed, pitch) },
                    lyrics = lyrics,
                    isFetchingLyrics = isFetchingLyrics,
                    comments = comments,
                    isFetchingComments = isFetchingComments,
                    isLoggedIn = isLoggedIn,
                    isPostingComment = isPostingComment,
                    onPostComment = { commentText -> playerViewModel.postComment(commentText) },
                    isLoadingMoreComments = isLoadingMoreComments,
                    onLoadMoreComments = { playerViewModel.loadMoreComments() },
                    selectedLyricsProvider = selectedLyricsProvider,
                    enabledLyricsProviders = playerViewModel.enabledLyricsProviders.collectAsStateWithLifecycle().value,
                    onLyricsProviderChange = { playerViewModel.switchLyricsProvider(it) },
                    sleepTimerOption = sleepTimerOption,
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                    onSetSleepTimer = { option, minutes -> playerViewModel.setSleepTimer(option, minutes) },
                    playlistViewModel = hiltViewModel(),
                    ringtoneViewModel = hiltViewModel(),
                    playerViewModel = playerViewModel,
                    volumeKeyEvents = volumeKeyEvents
                )
             }
        )
    }    
        // Global Volume Indicator (shows on all screens except PlayerScreen when song is playing)
        if (showGlobalVolumeIndicator && volumeSliderEnabled) {
            VolumeIndicator(
                isVisible = showVolumeIndicator,
                currentVolume = currentVolume,
                maxVolume = maxVolume,
                dominantColors = defaultDominantColors,
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

        // Global Update Dialogs
        when (val state = mainUiState.updateState) {
            is com.suvojeet.suvmusic.data.model.UpdateState.UpdateAvailable -> {
                UpdateAvailableDialog(
                    update = state.update,
                    currentVersion = mainUiState.currentVersion,
                    onDownload = { mainViewModel.downloadUpdate(state.update.downloadUrl, state.update.versionName) },
                    onDismiss = { mainViewModel.dismissUpdateDialog() }
                )
            }
            is com.suvojeet.suvmusic.data.model.UpdateState.Downloading -> {
                DownloadProgressDialog(
                    progress = state.progress,
                    onCancel = { mainViewModel.cancelDownload() }
                )
            }
            is com.suvojeet.suvmusic.data.model.UpdateState.Error -> {
                UpdateErrorDialog(
                    errorMessage = state.message,
                    onRetry = { mainViewModel.dismissUpdateDialog() }, // Just dismiss for now on auto-check error
                    onDismiss = { mainViewModel.dismissUpdateDialog() }
                )
            }
            else -> {}
        }
    }
}

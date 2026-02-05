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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.AppTheme
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.navigation.Destination
import com.suvojeet.suvmusic.navigation.NavGraph
import com.suvojeet.suvmusic.ui.components.ExpressiveBottomNav
import com.suvojeet.suvmusic.ui.components.MiniPlayer
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.screens.player.components.VolumeIndicator
import com.suvojeet.suvmusic.ui.screens.player.components.SystemVolumeObserver
import com.suvojeet.suvmusic.ui.theme.SuvMusicTheme
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel
import com.suvojeet.suvmusic.ui.viewmodel.MainViewModel
import com.suvojeet.suvmusic.data.model.UpdateState
import com.suvojeet.suvmusic.ui.components.UpdateAvailableDialog
import com.suvojeet.suvmusic.ui.components.DownloadProgressDialog
import com.suvojeet.suvmusic.ui.components.UpdateErrorDialog
import com.suvojeet.suvmusic.utils.NetworkMonitor
import com.suvojeet.suvmusic.service.DynamicIslandService
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.media3.datasource.cache.Cache
import javax.inject.Inject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var networkMonitor: NetworkMonitor
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    @Inject
    lateinit var downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository

    @Inject
    lateinit var musicPlayer: com.suvojeet.suvmusic.player.MusicPlayer

    @Inject
    lateinit var playerCache: Cache
    
    @Inject
    lateinit var lastFmRepository: com.suvojeet.suvmusic.lastfm.LastFmRepository

    private lateinit var audioManager: AudioManager
    
    // Track whether song is playing for volume key interception
    private var isSongPlaying: Boolean = false
    
    // Track whether in-app volume slider is enabled (if false, show system UI)
    private var isVolumeSliderEnabled: Boolean = true
    
    // Flow to emit volume key events to the UI
    private val _volumeKeyEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize audio manager for volume control
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        requestPermissions()

        // Check for player cache auto-clear
        lifecycleScope.launch(Dispatchers.IO) {
            val intervalDays = sessionManager.getPlayerCacheAutoClearInterval()
            if (intervalDays > 0) {
                val lastCleared = sessionManager.getLastCacheClearedTimestamp()
                val intervalMillis = intervalDays * 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastCleared > intervalMillis) {
                    try {
                        // Clear all cached resources
                        playerCache.keys.forEach { key ->
                            playerCache.removeResource(key)
                        }
                        sessionManager.updateLastCacheClearedTimestamp()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        // Handle deep link from intent
        val deepLinkUrl = intent?.data?.toString()
        if (intent?.data?.scheme == "suvmusic" && intent?.data?.host == "lastfm-auth") {
            val token = intent?.data?.getQueryParameter("token")
            if (token != null) {
                lifecycleScope.launch {
                    val result = lastFmRepository.fetchSession(token)
                    result.onSuccess { auth ->
                        sessionManager.setLastFmSession(auth.session.key, auth.session.name)
                        android.widget.Toast.makeText(this@MainActivity, "Connected to Last.fm as ${auth.session.name}", android.widget.Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        android.widget.Toast.makeText(this@MainActivity, "Failed to connect to Last.fm", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // Handle audio file from external app
        val audioFileUri = extractAudioUri(intent)
        
        setContent {
            val sessionManager = remember { SessionManager(this) }
            val themeMode by sessionManager.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by sessionManager.dynamicColorFlow.collectAsState(initial = true)
            val appTheme by sessionManager.appThemeFlow.collectAsState(initial = AppTheme.DEFAULT)
            val pureBlackEnabled by sessionManager.pureBlackEnabledFlow.collectAsState(initial = false)
            val systemDarkTheme = isSystemInDarkTheme()
            
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDarkTheme
            }
            
            SuvMusicTheme(
                darkTheme = darkTheme, 
                dynamicColor = dynamicColor,
                appTheme = appTheme,
                pureBlack = pureBlackEnabled
            ) {
                SuvMusicApp(
                    initialDeepLink = if (audioFileUri == null) deepLinkUrl else null,
                    initialAudioUri = audioFileUri,
                    networkMonitor = networkMonitor,
                    audioManager = audioManager,
                    volumeKeyEvents = _volumeKeyEvents,
                    downloadRepository = downloadRepository,
                    onPlaybackStateChanged = { hasSong -> 
                        isSongPlaying = hasSong
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
        
        // Handle Last.fm Auth
        if (intent?.data?.scheme == "suvmusic" && intent?.data?.host == "lastfm-auth") {
            val token = intent?.data?.getQueryParameter("token")
            if (token != null) {
                lifecycleScope.launch {
                    val result = lastFmRepository.fetchSession(token)
                    result.onSuccess { auth ->
                        sessionManager.setLastFmSession(auth.session.key, auth.session.name)
                        android.widget.Toast.makeText(this@MainActivity, "Connected to Last.fm as ${auth.session.name}", android.widget.Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        android.widget.Toast.makeText(this@MainActivity, "Failed to connect to Last.fm", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // Deep links are handled in SuvMusicApp composable via LaunchedEffect
        // Don't call recreate() as it wipes all state including HomeScreen data
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        // Audio permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // Disable video track for bandwidth optimization when backgrounded
        musicPlayer.optimizeBandwidth(true)
        
        // Start Floating Player if enabled and music might be playing
        lifecycleScope.launch {
            if (sessionManager.isDynamicIslandEnabled() && 
                DynamicIslandService.hasOverlayPermission(this@MainActivity)) {
                DynamicIslandService.start(this@MainActivity)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Re-enable video track when returning to foreground
        musicPlayer.optimizeBandwidth(false)
        
        // Stop Floating Player when app comes to foreground
        DynamicIslandService.stop(this)
    }
}

@Composable
fun SuvMusicApp(
    initialDeepLink: String? = null,
    initialAudioUri: android.net.Uri? = null,
    networkMonitor: NetworkMonitor,
    audioManager: AudioManager,
    volumeKeyEvents: SharedFlow<Unit>? = null,
    downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository? = null,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onVolumeSliderEnabledChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect volume slider enabled preference
    val volumeSliderEnabled by sessionManager.volumeSliderEnabledFlow.collectAsState(initial = true)
    val miniPlayerAlpha by sessionManager.miniPlayerAlphaFlow.collectAsState(initial = 1f)
    
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val mainUiState by mainViewModel.uiState.collectAsState()
    
    // Optimized states to reduce recompositions
    val playbackInfo by playerViewModel.playbackInfo.collectAsState(initial = com.suvojeet.suvmusic.data.model.PlayerState())
    val playerState by playerViewModel.playerState.collectAsState() // Still needed for some components
    
    val lyrics by playerViewModel.lyricsState.collectAsState()
    val isFetchingLyrics by playerViewModel.isFetchingLyrics.collectAsState()
    val selectedLyricsProvider by playerViewModel.selectedLyricsProvider.collectAsState()
    
    val comments by playerViewModel.commentsState.collectAsState()
    val isFetchingComments by playerViewModel.isFetchingComments.collectAsState()
    val isPostingComment by playerViewModel.isPostingComment.collectAsState()
    val isLoadingMoreComments by playerViewModel.isLoadingMoreComments.collectAsState()
    
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
                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
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
    val isConnected by networkMonitor.isConnected.collectAsState(initial = networkMonitor.isCurrentlyConnected())
    
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
    val sleepTimerOption by playerViewModel.sleepTimerOption.collectAsState()
    val sleepTimerRemainingMs by playerViewModel.sleepTimerRemainingMs.collectAsState()
    
    // Radio Mode
    val isRadioMode by playerViewModel.isRadioMode.collectAsState()
    val isLoadingMoreSongs by playerViewModel.isLoadingMoreSongs.collectAsState()
    val isMiniPlayerDismissed by playerViewModel.isMiniPlayerDismissed.collectAsState()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var currentDestination by remember { mutableStateOf<Destination>(Destination.Home) }
    
    // Handle deep link on first composition
    var deepLinkHandled by remember { mutableStateOf(false) }
    var restoreAttempted by remember { mutableStateOf(false) }
    
    LaunchedEffect(initialDeepLink, initialAudioUri) {
        if (initialAudioUri != null && !deepLinkHandled) {
            // Handle audio file from external app
            deepLinkHandled = true
            playerViewModel.playFromLocalUri(context, initialAudioUri)
            navController.navigate(Destination.Player.route)
        } else if (initialDeepLink != null && !deepLinkHandled) {
            deepLinkHandled = true
            val videoId = extractVideoId(initialDeepLink)
            if (videoId != null) {
                // Create a song from the video ID and play it
                playerViewModel.playFromDeepLink(videoId)
                // Navigate to player screen
                navController.navigate(Destination.Player.route)
            }
        } else if (!restoreAttempted && initialDeepLink == null && initialAudioUri == null) {
            restoreAttempted = true
            // Only restore if no song is currently playing (i.e., app was force-stopped/crashed)
            // If app is running normally in background, currentSong will not be null
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
    val showMiniPlayer = currentRoute != Destination.Player.route && !isMiniPlayerDismissed
    
    // Don't show global volume indicator on PlayerScreen (it has its own)
    val showGlobalVolumeIndicator = currentRoute != Destination.Player.route && hasSong
    
    // Default colors for non-player screens
    val defaultDominantColors = DominantColors(
        primary = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        secondary = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
        accent = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
        onBackground = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    )
    

    
    // Welcome Dialog State
    val onboardingCompleted by sessionManager.onboardingCompletedFlow.collectAsState(initial = true) // Start assuming true to avoid flicker if already done
    var showWelcomeDialog by remember { mutableStateOf(false) }
    
    // Check actual onboarding status on launch
    LaunchedEffect(Unit) {
        if (!sessionManager.isOnboardingCompleted()) {
            showWelcomeDialog = true
        }
    }
    
    // Check for TV Mode
    val isTv = remember { com.suvojeet.suvmusic.utils.TvUtils.isTv(context) }

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
                        playerViewModel.viewModelScope.launch {
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
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_music_note), // Ensure this resource exists or use vector
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                 )
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        @OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
        androidx.compose.animation.SharedTransitionLayout {
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
                            val navBarAlpha by sessionManager.navBarAlphaFlow.collectAsState(initial = 0.9f)
                            
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
                                alpha = navBarAlpha
                            )
                        }
                    }
                }
            ) { innerPadding ->
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
                            .padding(
                                bottom = if (showBottomNav && !isTv) innerPadding.calculateBottomPadding() else 0.dp
                                // We don't apply top padding here as it might be handled by screens or scaffolds inside
                                // But for TV, we might need some padding if rail is taking space
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
                        onStartRadio = { song ->
                            val targetSong = song ?: playbackInfo.currentSong
                            targetSong?.let { 
                                playerViewModel.startRadio(it)
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
                        isLoggedIn = playerViewModel.isLoggedIn(),
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
                        enabledLyricsProviders = playerViewModel.enabledLyricsProviders.collectAsState().value,
                        onLyricsProviderChange = { playerViewModel.switchLyricsProvider(it) },
                        startDestination = Destination.Home.route, // Always start at Home
                        sharedTransitionScope = this@SharedTransitionLayout,
                        isTv = isTv
                    )

                    // MiniPlayer floating content overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showMiniPlayer,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        MiniPlayer(
                            playerState = playerState,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onNextClick = { playerViewModel.seekToNext() },
                            onPreviousClick = { playerViewModel.seekToPrevious() },
                            onPlayerClick = { navController.navigate(Destination.Player.route) },
                            onLikeClick = { playerViewModel.likeCurrentSong() },
                            modifier = Modifier,
                            onCloseClick = if (currentRoute != Destination.Home.route && currentRoute != Destination.Player.route) {
                                { playerViewModel.dismissMiniPlayer() }
                            } else null,
                            alpha = miniPlayerAlpha,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
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
        when (val updateState = mainUiState.updateState) {
            is UpdateState.UpdateAvailable -> {
                UpdateAvailableDialog(
                    update = updateState.update,
                    currentVersion = mainUiState.currentVersion,
                    onDownload = { mainViewModel.downloadUpdate(updateState.update.downloadUrl, updateState.update.versionName) },
                    onDismiss = { mainViewModel.dismissUpdateDialog() }
                )
            }
            is UpdateState.Downloading -> {
                DownloadProgressDialog(
                    progress = updateState.progress,
                    onCancel = { mainViewModel.cancelDownload() }
                )
            }
            is UpdateState.Error -> {
                UpdateErrorDialog(
                    errorMessage = updateState.message,
                    onRetry = { mainViewModel.dismissUpdateDialog() }, // Just dismiss for now on auto-check error
                    onDismiss = { mainViewModel.dismissUpdateDialog() }
                )
            }
            else -> {}
        }
    }
}
}

/**
 * Extracts video ID from various YouTube/YouTube Music URL formats.
 * Supports:
 * - https://music.youtube.com/watch?v=VIDEO_ID
 * - https://www.youtube.com/watch?v=VIDEO_ID
 * - https://youtu.be/VIDEO_ID
 * - https://youtube.com/shorts/VIDEO_ID
 */
private fun extractVideoId(url: String): String? {
    return try {
        when {
            // SuvMusic custom URL: suvmusic://play?id=VIDEO_ID
            url.startsWith("suvmusic://play") -> {
                val uri = android.net.Uri.parse(url)
                uri.getQueryParameter("id")
            }
            // youtu.be/VIDEO_ID format
            url.contains("youtu.be/") -> {
                url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            }
            // youtube.com/shorts/VIDEO_ID format
            url.contains("/shorts/") -> {
                url.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
            }
            // Standard watch?v= format
            url.contains("v=") -> {
                url.substringAfter("v=").substringBefore("&").substringBefore("#")
            }
            else -> null
        }?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}

/**
 * Check if the intent is an audio file intent from external app.
 */
private fun isAudioFileIntent(intent: Intent?): Boolean {
    if (intent == null) return false
    val action = intent.action
    val type = intent.type
    
    return action == Intent.ACTION_VIEW && type?.startsWith("audio/") == true
}

/**
 * Extract audio file URI from an intent.
 */
private fun extractAudioUri(intent: Intent?): android.net.Uri? {
    if (!isAudioFileIntent(intent)) return null
    return intent?.data
}

/**
 * Extract song ID from SuvMusic custom URL (suvmusic://play?id=VIDEO_ID)
 */
private fun extractSuvMusicId(url: String?): String? {
    if (url == null) return null
    return try {
        when {
            url.startsWith("suvmusic://play") -> {
                // Extract id parameter from suvmusic://play?id=VIDEO_ID
                val uri = android.net.Uri.parse(url)
                uri.getQueryParameter("id")
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Generate a SuvMusic share URL for a song.
 * Format: suvmusic://play?id=VIDEO_ID
 */
fun generateSuvMusicShareUrl(songId: String): String {
    return "suvmusic://play?id=$songId"
}

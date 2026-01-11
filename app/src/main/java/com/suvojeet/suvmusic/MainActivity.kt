package com.suvojeet.suvmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.navigation.Destination
import com.suvojeet.suvmusic.navigation.NavGraph
import com.suvojeet.suvmusic.ui.components.ExpressiveBottomNav
import com.suvojeet.suvmusic.ui.components.MiniPlayer
import com.suvojeet.suvmusic.ui.theme.SuvMusicTheme
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel
import com.suvojeet.suvmusic.utils.NetworkMonitor
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var networkMonitor: NetworkMonitor
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestPermissions()
        
        // Handle deep link from intent
        val deepLinkUrl = intent?.data?.toString()
        
        setContent {
            val sessionManager = remember { SessionManager(this) }
            val themeMode by sessionManager.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by sessionManager.dynamicColorFlow.collectAsState(initial = true)
            val systemDarkTheme = isSystemInDarkTheme()
            
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDarkTheme
            }
            
            SuvMusicTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                SuvMusicApp(
                    initialDeepLink = deepLinkUrl,
                    networkMonitor = networkMonitor
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
}

@Composable
fun SuvMusicApp(
    initialDeepLink: String? = null,
    networkMonitor: NetworkMonitor
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.playerState.collectAsState()
    val lyrics by playerViewModel.lyricsState.collectAsState()
    val isFetchingLyrics by playerViewModel.isFetchingLyrics.collectAsState()
    
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
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var currentDestination by remember { mutableStateOf<Destination>(Destination.Home) }
    
    // Handle deep link on first composition
    var deepLinkHandled by remember { mutableStateOf(false) }
    var restoreAttempted by remember { mutableStateOf(false) }
    
    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null && !deepLinkHandled) {
            deepLinkHandled = true
            val videoId = extractVideoId(initialDeepLink)
            if (videoId != null) {
                // Create a song from the video ID and play it
                playerViewModel.playFromDeepLink(videoId)
                // Navigate to player screen
                navController.navigate(Destination.Player.route)
            }
        } else if (!restoreAttempted && initialDeepLink == null) {
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
    
    // Don't show MiniPlayer on Player screen itself
    val showMiniPlayer = currentRoute != Destination.Player.route
    
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
                if (showBottomNav) {
                    Column {
                        // Mini player on main screens
                        MiniPlayer(
                            playerState = playerState,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onNextClick = { playerViewModel.seekToNext() },
                            onPlayerClick = { navController.navigate(Destination.Player.route) }
                        )
                        
                        // Bottom navigation
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
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (showBottomNav) innerPadding.calculateBottomPadding() else innerPadding.calculateBottomPadding()
                    )
            ) {
                NavGraph(
                    navController = navController,
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
                    onShuffleToggle = { playerViewModel.toggleShuffle() },
                    onRepeatToggle = { playerViewModel.toggleRepeat() },
                    onToggleAutoplay = { playerViewModel.toggleAutoplay() },
                    onToggleVideoMode = { playerViewModel.toggleVideoMode() },
                    onSwitchDevice = { playerViewModel.switchOutputDevice(it) },
                    player = playerViewModel.getPlayer(),
                    lyrics = lyrics,
                    isFetchingLyrics = isFetchingLyrics,
                    sleepTimerOption = sleepTimerOption,
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                    onSetSleepTimer = { playerViewModel.setSleepTimer(it) },
                    startDestination = if (sessionManager.isOnboardingCompleted()) Destination.Home.route else Destination.Welcome.route
                )
            }
        }
        
        // Floating MiniPlayer for detail screens (Playlist, Album, etc.)
        // Only show if bottom nav is hidden, it's not player screen, logic allows it, AND song is playing
        var isFloatingMiniPlayerVisible by remember { mutableStateOf(true) }
        
        // Reset visibility when song changes or significant navigation happens? 
        // For now, let's keep it simple: if song is playing and user hasn't closed it.
        // But if a new song starts, maybe it should reappear?
        // Let's rely on user explicitly closing it.
        
        if (!showBottomNav && showMiniPlayer && playerState.currentSong != null) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isFloatingMiniPlayerVisible,
                enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                MiniPlayer(
                    playerState = playerState,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onNextClick = { playerViewModel.seekToNext() },
                    onPlayerClick = { navController.navigate(Destination.Player.route) },
                    onCloseClick = { isFloatingMiniPlayerVisible = false }
                )
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
        }?.takeIf { it.isNotBlank() && it.length == 11 }
    } catch (e: Exception) {
        null
    }
}

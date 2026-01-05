package com.suvojeet.suvmusic

import android.Manifest
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.suvojeet.suvmusic.navigation.Destination
import com.suvojeet.suvmusic.navigation.NavGraph
import com.suvojeet.suvmusic.ui.components.ExpressiveBottomNav
import com.suvojeet.suvmusic.ui.components.MiniPlayer
import com.suvojeet.suvmusic.ui.theme.SuvMusicTheme
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestPermissions()
        
        setContent {
            SuvMusicTheme {
                SuvMusicApp()
            }
        }
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
fun SuvMusicApp() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.playerState.collectAsState()
    val lyrics by playerViewModel.lyricsState.collectAsState()
    val isFetchingLyrics by playerViewModel.isFetchingLyrics.collectAsState()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var currentDestination by remember { mutableStateOf<Destination>(Destination.Home) }
    
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
                    lyrics = lyrics,
                    isFetchingLyrics = isFetchingLyrics
                )
            }
        }
        
        // Floating MiniPlayer for detail screens (Playlist, Album, etc.)
        if (!showBottomNav && showMiniPlayer) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                MiniPlayer(
                    playerState = playerState,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onNextClick = { playerViewModel.seekToNext() },
                    onPlayerClick = { navController.navigate(Destination.Player.route) }
                )
            }
        }
    }
}

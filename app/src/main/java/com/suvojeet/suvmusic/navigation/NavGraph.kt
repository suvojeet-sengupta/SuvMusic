package com.suvojeet.suvmusic.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.ui.screens.AboutScreen
import com.suvojeet.suvmusic.ui.screens.HomeScreen
import com.suvojeet.suvmusic.ui.screens.LibraryScreen
import com.suvojeet.suvmusic.ui.screens.PlayerScreen
import com.suvojeet.suvmusic.ui.screens.PlaylistScreen
import com.suvojeet.suvmusic.ui.screens.SearchScreen
import com.suvojeet.suvmusic.ui.screens.SettingsScreen
import com.suvojeet.suvmusic.ui.screens.YouTubeLoginScreen

/**
 * Main navigation graph for the app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    playerState: PlayerState,
    sessionManager: SessionManager,
    onPlaySong: (List<Song>, Int) -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDownloadCurrentSong: () -> Unit,
    onLikeCurrentSong: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onPlaylistClick = { playlist ->
                    navController.navigate(Destination.Playlist(playlist.id).route)
                }
            )
        }
        
        composable(Destination.Search.route) {
            SearchScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) }
            )
        }
        
        composable(Destination.Library.route) {
            LibraryScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onPlaylistClick = { playlist ->
                    navController.navigate(Destination.Playlist(playlist.id).route)
                }
            )
        }
        
        composable(Destination.Settings.route) {
            SettingsScreen(
                onLoginClick = {
                    navController.navigate(Destination.YouTubeLogin.route)
                },
                onAboutClick = {
                    navController.navigate(Destination.About.route)
                }
            )
        }
        
        composable(Destination.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.Player.route) {
            PlayerScreen(
                playerState = playerState,
                onPlayPause = onPlayPause,
                onSeekTo = onSeekTo,
                onNext = onNext,
                onPrevious = onPrevious,
                onBack = { navController.popBackStack() },
                onDownload = onDownloadCurrentSong,
                onToggleLike = onLikeCurrentSong,
                onShuffleToggle = onShuffleToggle,
                onRepeatToggle = onRepeatToggle
            )
        }
        
        composable(Destination.YouTubeLogin.route) {
            YouTubeLoginScreen(
                sessionManager = sessionManager,
                onLoginSuccess = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Destination.Playlist.ROUTE,
            arguments = listOf(
                navArgument(Destination.Playlist.ARG_PLAYLIST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            PlaylistScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onPlayAll = { songs -> 
                    if (songs.isNotEmpty()) {
                         onPlaySong(songs, 0)
                    }
                },
                onShufflePlay = { songs ->
                     if (songs.isNotEmpty()) {
                         val shuffled = songs.shuffled()
                         onPlaySong(shuffled, 0)
                     }
                }
            )
        }

        composable(
            route = Destination.Artist.ROUTE,
            arguments = listOf(
                navArgument(Destination.Artist.ARG_ARTIST_ID) { type = NavType.StringType }
            )
        ) {
            com.suvojeet.suvmusic.ui.screens.ArtistScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { onPlaySong(listOf(it), 0) },
                onAlbumClick = { albumId -> 
                    navController.navigate(Destination.Album(albumId).route)
                }
            )
        }

        composable(
            route = Destination.Album.ROUTE,
            arguments = listOf(
                navArgument(Destination.Album.ARG_ALBUM_ID) { type = NavType.StringType }
            )
        ) {
            com.suvojeet.suvmusic.ui.screens.AlbumScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { onPlaySong(listOf(it), 0) }
            )
        }
    }
}
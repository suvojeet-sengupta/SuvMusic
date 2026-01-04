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
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.ui.screens.HomeScreen
import com.suvojeet.suvmusic.ui.screens.LibraryScreen
import com.suvojeet.suvmusic.ui.screens.PlayerScreen
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
    onPlaySong: (Any) -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDownloadCurrentSong: () -> Unit,
    onLikeCurrentSong: () -> Unit,
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
                onSongClick = { onPlaySong(it) },
                onPlaylistClick = { /* Navigate to playlist */ }
            )
        }
        
        composable(Destination.Search.route) {
            SearchScreen(
                onSongClick = { onPlaySong(it) }
            )
        }
        
        composable(Destination.Library.route) {
            LibraryScreen(
                onSongClick = { onPlaySong(it) },
                onPlaylistClick = { /* Navigate to playlist */ }
            )
        }
        
        composable(Destination.Settings.route) {
            SettingsScreen(
                onLoginClick = {
                    navController.navigate(Destination.YouTubeLogin.route)
                }
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
                onToggleLike = onLikeCurrentSong
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
            val playlistId = backStackEntry.arguments?.getString(Destination.Playlist.ARG_PLAYLIST_ID)
            // PlaylistScreen(playlistId = playlistId ?: "")
        }

        composable(
            route = Destination.Artist.ROUTE,
            arguments = listOf(
                navArgument(Destination.Artist.ARG_ARTIST_ID) { type = NavType.StringType }
            )
        ) {
            com.suvojeet.suvmusic.ui.screens.ArtistScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { onPlaySong(it) },
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
                onSongClick = { onPlaySong(it) }
            )
        }
    }
}
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
import com.suvojeet.suvmusic.ui.screens.HowItWorksScreen
import com.suvojeet.suvmusic.ui.screens.AppearanceSettingsScreen
import com.suvojeet.suvmusic.ui.screens.ArtworkShapeScreen
import com.suvojeet.suvmusic.ui.screens.ArtworkSizeScreen
import com.suvojeet.suvmusic.ui.screens.CustomizationScreen
import com.suvojeet.suvmusic.ui.screens.HomeScreen
import com.suvojeet.suvmusic.ui.screens.LibraryScreen
import com.suvojeet.suvmusic.ui.screens.player.PlayerScreen
import com.suvojeet.suvmusic.ui.screens.PlaybackSettingsScreen
import com.suvojeet.suvmusic.ui.screens.PlaylistScreen
import com.suvojeet.suvmusic.ui.screens.RecentsScreen
import com.suvojeet.suvmusic.ui.screens.SearchScreen
import com.suvojeet.suvmusic.ui.screens.SeekbarStyleScreen
import com.suvojeet.suvmusic.ui.screens.SettingsScreen
import com.suvojeet.suvmusic.ui.screens.StorageScreen
import com.suvojeet.suvmusic.ui.screens.SupportScreen
import com.suvojeet.suvmusic.ui.screens.WelcomeScreen
import com.suvojeet.suvmusic.ui.screens.YouTubeLoginScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow
import androidx.media3.common.Player

/**
 * Main navigation graph for the app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    playbackInfo: PlayerState,
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
    onToggleAutoplay: () -> Unit,
    onToggleVideoMode: () -> Unit = {},
    onStartRadio: () -> Unit = {},
    onLoadMoreRadioSongs: () -> Unit = {},
    isRadioMode: Boolean = false,
    isLoadingMoreSongs: Boolean = false,
    onSwitchDevice: (com.suvojeet.suvmusic.data.model.OutputDevice) -> Unit = {},
    onRefreshDevices: () -> Unit = {},
    onSetPlaybackParameters: (Float, Float) -> Unit = { _, _ -> },
    player: Player? = null,
    lyrics: com.suvojeet.suvmusic.data.model.Lyrics?,
    isFetchingLyrics: Boolean,
    comments: List<com.suvojeet.suvmusic.data.model.Comment>?,
    isFetchingComments: Boolean,
    isLoggedIn: Boolean = false,
    isPostingComment: Boolean = false,
    onPostComment: (String) -> Unit = {},
    isLoadingMoreComments: Boolean = false,
    onLoadMoreComments: () -> Unit = {},
    // Lyrics Provider
    selectedLyricsProvider: com.suvojeet.suvmusic.data.model.LyricsProviderType = com.suvojeet.suvmusic.data.model.LyricsProviderType.AUTO,
    onLyricsProviderChange: (com.suvojeet.suvmusic.data.model.LyricsProviderType) -> Unit = {},
    // Sleep timer
    sleepTimerOption: com.suvojeet.suvmusic.player.SleepTimerOption = com.suvojeet.suvmusic.player.SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    onSetSleepTimer: (com.suvojeet.suvmusic.player.SleepTimerOption, Int?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    volumeKeyEvents: SharedFlow<Unit>? = null,
    downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository? = null,
    startDestination: String = Destination.Home.route
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
            if (initialState.destination.route == Destination.Player.route) {
                fadeIn(animationSpec = tween(300))
            } else {
                fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onPlaylistClick = { playlist ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlist.id,
                            name = playlist.name,
                            thumbnailUrl = playlist.thumbnailUrl
                        ).route
                    )
                },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        ).route
                    )
                },
                onRecentsClick = {
                    navController.navigate(Destination.Recents.route)
                }
            )
        }
        
        composable(Destination.Search.route) {
            SearchScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onArtistClick = { artistId ->
                    navController.navigate(Destination.Artist(artistId).route)
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlistId,
                            name = null,
                            thumbnailUrl = null
                        ).route
                    )
                }
            )
        }
        
        composable(Destination.Library.route) {
            LibraryScreen(
                onSongClick = { songs, index -> 
                    onPlaySong(songs, index)
                    navController.navigate(Destination.Player.route)
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlist.id,
                            name = playlist.name,
                            thumbnailUrl = playlist.thumbnailUrl
                        ).route
                    )
                },
                onDownloadsClick = {
                    navController.navigate(Destination.Downloads.route)
                }
            )
        }

        composable(Destination.Downloads.route) {
            com.suvojeet.suvmusic.ui.screens.DownloadsScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onPlayAll = { songs -> onPlaySong(songs, 0) },
                onShufflePlay = { songs -> 
                    // Shuffle logic should be handled by player or passing shuffled list
                    // For now, we can just pass the list and handle shuffle in player if needed
                    // Or ideally pass a shuffled list from here
                    val shuffledSongs = songs.shuffled()
                    onPlaySong(shuffledSongs, 0)
                }
            )
        }
        
        composable(Destination.Settings.route) {
            SettingsScreen(
                onLoginClick = { navController.navigate(Destination.YouTubeLogin.route) },
                onPlaybackClick = { navController.navigate(Destination.PlaybackSettings.route) },
                onAppearanceClick = { navController.navigate(Destination.AppearanceSettings.route) },
                onCustomizationClick = { navController.navigate(Destination.CustomizationSettings.route) },
                onStorageClick = { navController.navigate(Destination.Storage.route) },
                onStatsClick = { navController.navigate(Destination.ListeningStats.route) },
                onSupportClick = { navController.navigate(Destination.Support.route) },
                onAboutClick = { navController.navigate(Destination.About.route) }
            )
        }
        
        composable(Destination.Storage.route) {
            downloadRepository?.let { repo ->
                StorageScreen(
                    downloadRepository = repo,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        
        composable(Destination.PlaybackSettings.route) {
            PlaybackSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.AppearanceSettings.route) {
            AppearanceSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.CustomizationSettings.route) {
            CustomizationScreen(
                onBack = { navController.popBackStack() },
                onSeekbarStyleClick = { navController.navigate(Destination.SeekbarStyleSettings.route) },
                onArtworkShapeClick = { navController.navigate(Destination.ArtworkShapeSettings.route) },
                onArtworkSizeClick = { navController.navigate(Destination.ArtworkSizeSettings.route) }
            )
        }
        
        composable(Destination.ArtworkShapeSettings.route) {
            ArtworkShapeScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.SeekbarStyleSettings.route) {
            SeekbarStyleScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.ArtworkSizeSettings.route) {
            ArtworkSizeScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.Recents.route) {
            RecentsScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onHowItWorksClick = { navController.navigate(Destination.HowItWorks.route) }
            )
        }
        
        composable(Destination.HowItWorks.route) {
            HowItWorksScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.Support.route) {
            SupportScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Destination.ListeningStats.route) {
            com.suvojeet.suvmusic.ui.screens.ListeningStatsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Destination.Player.route,
            enterTransition = {
                // Slide up from bottom when opening
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(400)
                )
            },
            exitTransition = {
                // Fade out when navigating away (not back)
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                // Fade in when returning to player
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                // Slide down to bottom when closing (back)
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(400)
                )
            }
        ) {
            PlayerScreen(
                playbackInfo = playbackInfo,
                playerState = playerState,
                onPlayPause = onPlayPause,
                onSeekTo = onSeekTo,
                onNext = onNext,
                onPrevious = onPrevious,
                onBack = { navController.popBackStack() },
                onDownload = onDownloadCurrentSong,
                onToggleLike = onLikeCurrentSong,
                onShuffleToggle = onShuffleToggle,
                onRepeatToggle = onRepeatToggle,
                onToggleAutoplay = onToggleAutoplay,
                onToggleVideoMode = onToggleVideoMode,
                onStartRadio = onStartRadio,
                onLoadMoreRadioSongs = onLoadMoreRadioSongs,
                isRadioMode = isRadioMode,
                isLoadingMoreSongs = isLoadingMoreSongs,
                player = player,
                onPlayFromQueue = { index ->
                    if (playerState.queue.isNotEmpty() && index in playerState.queue.indices) {
                        onPlaySong(playerState.queue, index)
                    }
                },
                onSwitchDevice = onSwitchDevice,
                onRefreshDevices = onRefreshDevices,
                lyrics = lyrics,
                isFetchingLyrics = isFetchingLyrics,
                comments = comments,
                isFetchingComments = isFetchingComments,
                isLoggedIn = isLoggedIn,
                isPostingComment = isPostingComment,
                onPostComment = onPostComment,
                isLoadingMoreComments = isLoadingMoreComments,
                onLoadMoreComments = onLoadMoreComments,
                sleepTimerOption = sleepTimerOption,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                onSetSleepTimer = onSetSleepTimer,
                volumeKeyEvents = volumeKeyEvents,
                onSetPlaybackParameters = onSetPlaybackParameters,
                selectedLyricsProvider = selectedLyricsProvider,
                onLyricsProviderChange = onLyricsProviderChange
            )
        }
        
        composable(Destination.YouTubeLogin.route) {
            YouTubeLoginScreen(
                sessionManager = sessionManager,
                onLoginSuccess = {
                    // Show success message
                    android.widget.Toast.makeText(
                        navController.context,
                        "Login Successful",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    // Mark onboarding as completed
                    scope.launch {
                        sessionManager.setOnboardingCompleted(true)
                    }
                    
                    // Navigate to Home and clear back stack
                    navController.navigate(Destination.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destination.Welcome.route) {
            WelcomeScreen(
                onLoginClick = {
                    // Navigate to login page
                    navController.navigate(Destination.YouTubeLogin.route)
                },
                onSkipClick = {
                    // Navigate to home, and clear back stack
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Destination.Playlist.ROUTE,
            arguments = listOf(
                navArgument(Destination.Playlist.ARG_PLAYLIST_ID) {
                    type = NavType.StringType
                },
                navArgument(Destination.Playlist.ARG_NAME) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Destination.Playlist.ARG_THUMBNAIL) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
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
        ) { backStackEntry ->
            com.suvojeet.suvmusic.ui.screens.ArtistScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { onPlaySong(listOf(it), 0) },
                onAlbumClick = { album -> 
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        ).route
                    )
                },
                onSeeAllAlbumsClick = {
                    val currentId = backStackEntry.arguments?.getString(Destination.Artist.ARG_ARTIST_ID)
                    if (currentId != null) {
                        navController.navigate(
                            Destination.ArtistDiscography(currentId, Destination.ArtistDiscography.TYPE_ALBUMS).route
                        )
                    }
                },
                onSeeAllSinglesClick = {
                    val currentId = backStackEntry.arguments?.getString(Destination.Artist.ARG_ARTIST_ID)
                    if (currentId != null) {
                        navController.navigate(
                            Destination.ArtistDiscography(currentId, Destination.ArtistDiscography.TYPE_SINGLES).route
                        )
                    }
                },
                onArtistClick = { artist ->
                    navController.navigate(Destination.Artist(artist.id).route)
                }
            )
        }

        composable(
            route = Destination.ArtistDiscography.ROUTE,
            arguments = listOf(
                navArgument(Destination.ArtistDiscography.ARG_ARTIST_ID) { type = NavType.StringType },
                navArgument(Destination.ArtistDiscography.ARG_TYPE) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString(Destination.ArtistDiscography.ARG_ARTIST_ID) ?: ""
            val type = backStackEntry.arguments?.getString(Destination.ArtistDiscography.ARG_TYPE) ?: ""
            
            com.suvojeet.suvmusic.ui.screens.ArtistDiscographyScreen(
                artistId = artistId,
                type = type,
                onBackClick = { navController.popBackStack() },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        ).route
                    )
                }
            )
        }

        composable(
            route = Destination.Album.ROUTE,
            arguments = listOf(
                navArgument(Destination.Album.ARG_ALBUM_ID) { type = NavType.StringType },
                navArgument(Destination.Album.ARG_NAME) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Destination.Album.ARG_THUMBNAIL) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            com.suvojeet.suvmusic.ui.screens.AlbumScreen(
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
    }
}
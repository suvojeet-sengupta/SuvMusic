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
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.Album
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
import com.suvojeet.suvmusic.ui.screens.YouTubeLoginScreen
import com.suvojeet.suvmusic.ui.screens.MiscScreen
import com.suvojeet.suvmusic.ui.screens.LyricsProvidersScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow
import androidx.media3.common.Player
import com.suvojeet.suvmusic.ui.screens.SponsorBlockSettingsScreen

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
    onDislikeCurrentSong: () -> Unit = {},
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleVideoMode: () -> Unit = {},
    onDismissVideoError: () -> Unit = {},
    onStartRadio: (Song?) -> Unit = { _ -> },
    onLoadMoreRadioSongs: () -> Unit = {},
    isRadioMode: Boolean = false,
    isLoadingMoreSongs: Boolean = false,
    onSwitchDevice: (com.suvojeet.suvmusic.data.model.OutputDevice) -> Unit = {},
    onRefreshDevices: () -> Unit = {},
    onSetPlaybackParameters: (Float, Float) -> Unit = { _, _ -> },
    player: Player? = null,
    lyrics: com.suvojeet.suvmusic.providers.lyrics.Lyrics?,
    isFetchingLyrics: Boolean,
    comments: List<com.suvojeet.suvmusic.data.model.Comment>?,
    isFetchingComments: Boolean,
    isLoggedIn: Boolean = false,
    isPostingComment: Boolean = false,
    onPostComment: (String) -> Unit = {},
    isLoadingMoreComments: Boolean = false,
    onLoadMoreComments: () -> Unit = {},
    // Lyrics Provider
    selectedLyricsProvider: com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType = com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.AUTO,
    enabledLyricsProviders: Map<com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType, Boolean> = emptyMap(),
    onLyricsProviderChange: (com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType) -> Unit = {},
    // Sleep timer
    sleepTimerOption: com.suvojeet.suvmusic.player.SleepTimerOption = com.suvojeet.suvmusic.player.SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    onSetSleepTimer: (com.suvojeet.suvmusic.player.SleepTimerOption, Int?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    volumeKeyEvents: SharedFlow<Unit>? = null,
    downloadRepository: com.suvojeet.suvmusic.data.repository.DownloadRepository? = null,
    startDestination: String = Destination.Home.route,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null
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
                },
                onExploreClick = { browseId, title ->
                    if (browseId == "FEmusic_moods_and_genres") {
                        navController.navigate(Destination.MoodAndGenres.route)
                    } else {
                        navController.navigate(Destination.Explore.buildRoute(browseId, title))
                    }
                },
                onStartRadio = { onStartRadio(null) },
                onCreateMixClick = {
                    navController.navigate(Destination.PickMusic.route)
                }
            )
        }
        
        composable(
            route = Destination.Explore.ROUTE,
            arguments = listOf(
                navArgument(Destination.Explore.ARG_BROWSE_ID) { type = NavType.StringType },
                navArgument(Destination.Explore.ARG_TITLE) { type = NavType.StringType }
            )
        ) {
            com.suvojeet.suvmusic.ui.screens.ExploreScreen(
                onBackClick = { navController.popBackStack() },
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
                }
            )
                }


        composable(Destination.MoodAndGenres.route) {
            com.suvojeet.suvmusic.ui.screens.MoodAndGenresScreen(
                onCategoryClick = { browseId, params, title ->
                    navController.navigate(
                        Destination.MoodAndGenresDetail.buildRoute(browseId, params, title)
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.MoodAndGenresDetail.ROUTE,
            arguments = listOf(
                navArgument(Destination.MoodAndGenresDetail.ARG_BROWSE_ID) { type = NavType.StringType },
                navArgument(Destination.MoodAndGenresDetail.ARG_PARAMS) { 
                    type = NavType.StringType 
                    nullable = true
                },
                navArgument(Destination.MoodAndGenresDetail.ARG_TITLE) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val browseId = backStackEntry.arguments?.getString(Destination.MoodAndGenresDetail.ARG_BROWSE_ID) ?: ""
            val params = backStackEntry.arguments?.getString(Destination.MoodAndGenresDetail.ARG_PARAMS)
            val title = backStackEntry.arguments?.getString(Destination.MoodAndGenresDetail.ARG_TITLE) ?: ""

            com.suvojeet.suvmusic.ui.screens.MoodAndGenresDetailScreen(
                browseId = browseId,
                params = params,
                title = title,
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs, index -> onPlaySong(songs, index) }
            )
        }
        
        composable(Destination.Search.route) {
            SearchScreen(
                onSongClick = { songs, index -> 
                    onStartRadio(songs[index])
                },
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
                },
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
        
        composable(Destination.Library.route) {
            LibraryScreen(
                onSongClick = { songs, index -> 
                    onPlaySong(songs, index)
                    navController.navigate(Destination.Player.route)
                },
                onHistoryClick = {
                    navController.navigate(Destination.Recents.route)
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
                onArtistClick = { artistId ->
                    navController.navigate(Destination.Artist(artistId).route)
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
                onAboutClick = { navController.navigate(Destination.About.route) },
                onMiscClick = { navController.navigate(Destination.Misc.route) },
                onCreditsClick = { navController.navigate(Destination.Credits.route) },
                onLastFmClick = { navController.navigate(Destination.LastFmLogin.route) },
                onSponsorBlockClick = { navController.navigate(Destination.SponsorBlockSettings.route) },
                onDiscordClick = { navController.navigate(Destination.DiscordSettings.route) }
            )
        }
        
        composable(Destination.Storage.route) {
            downloadRepository?.let { repo ->
                StorageScreen(
                    downloadRepository = repo,
                    onBackClick = { navController.popBackStack() },
                    onPlayerCacheClick = { navController.navigate(Destination.PlayerCache.route) }
                )
            }
        }

        composable(Destination.PlayerCache.route) {
            downloadRepository?.let { repo ->
                val settingsViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel>()
                com.suvojeet.suvmusic.ui.screens.PlayerCacheScreen(
                    onBackClick = { navController.popBackStack() },
                    settingsViewModel = settingsViewModel,
                    downloadRepository = repo
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

        composable(Destination.Misc.route) {
            MiscScreen(
                onBack = { navController.popBackStack() },
                onLyricsProvidersClick = { navController.navigate(Destination.LyricsProviders.route) }
            )
        }

        composable(Destination.Credits.route) {
            com.suvojeet.suvmusic.ui.screens.CreditsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destination.LyricsProviders.route) {
            LyricsProvidersScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destination.SponsorBlockSettings.route) {
            SponsorBlockSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destination.PickMusic.route) {
            com.suvojeet.suvmusic.ui.screens.PickMusicScreen(
                onBackClick = { navController.popBackStack() },
                onMixCreated = { songs ->
                    if (songs.isNotEmpty()) {
                        // Play the mixed playlist
                         onPlaySong(songs, 0)
                         // Navigate to player
                         navController.navigate(Destination.Player.route)
                    } else {
                         navController.popBackStack()
                    }
                }
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
                    animationSpec = tween(300)
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
                    animationSpec = tween(300)
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
                onToggleDislike = onDislikeCurrentSong,
                onShuffleToggle = onShuffleToggle,
                onRepeatToggle = onRepeatToggle,
                onToggleAutoplay = onToggleAutoplay,
                onToggleVideoMode = onToggleVideoMode,
                onDismissVideoError = onDismissVideoError,
                onStartRadio = { onStartRadio(null) },
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
                onArtistClick = { artistId ->
                    navController.navigate(Destination.Artist(artistId).route) {
                        launchSingleTop = true
                    }
                    // Close player if we are navigating away? Or usually player stays open?
                    // Standard behavior is to navigate "under" the player or close the player sheet. 
                    // Since PlayerScreen here seems to be a full screen route (Destination.Player), 
                    // we are navigating internally. 
                    // However, standard Compose Navigation pushes to stack.
                    // The transition for Player is slideUp/slideDown.
                    // If we navigate to Artist, it might look weird if Player slides down.
                    // But Destination.Artist will push on top.
                },
                onAlbumClick = { albumId ->
                    navController.navigate(
                        Destination.Album(
                            albumId = albumId,
                            name = null,
                            thumbnailUrl = null
                        ).route
                    ) {
                        launchSingleTop = true
                    }
                },
                selectedLyricsProvider = selectedLyricsProvider,
                enabledLyricsProviders = enabledLyricsProviders,
                onLyricsProviderChange = onLyricsProviderChange,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this
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

        composable(Destination.LastFmLogin.route) {
            com.suvojeet.suvmusic.ui.screens.settings.LastFmSettingsScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = { username ->
                    android.widget.Toast.makeText(navController.context, "Connected as $username", android.widget.Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            )
        }

        composable(Destination.DiscordSettings.route) {
            com.suvojeet.suvmusic.ui.screens.settings.DiscordSettingsScreen(
                onBack = { navController.popBackStack() }
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
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlist.id,
                            name = playlist.name,
                            thumbnailUrl = playlist.thumbnailUrl
                        ).route
                    )
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
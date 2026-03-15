package com.suvojeet.suvmusic.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.ui.utils.DeviceType
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
import com.suvojeet.suvmusic.ui.screens.ChangelogScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow
import androidx.media3.common.Player
import com.suvojeet.suvmusic.ui.screens.SponsorBlockSettingsScreen
import com.suvojeet.suvmusic.ui.screens.ListenTogetherScreen
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.navigation.toRoute

/**
 * Main navigation graph for the app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    playbackInfo: PlayerState,
    playerState: PlayerState,
    sessionManager: SessionManager,
    youTubeRepository: com.suvojeet.suvmusic.data.repository.YouTubeRepository,
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
    onStartRadio: (Song?, List<Song>?) -> Unit = { _, _ -> },
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
    startDestination: Any = Destination.Home,
    deviceType: DeviceType = DeviceType.Phone,
    dominantColors: com.suvojeet.suvmusic.ui.components.DominantColors? = null
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
             fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                 towards = AnimatedContentTransitionScope.SlideDirection.End,
                 animationSpec = tween(300)
             )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable<Destination.Home> {
            when (deviceType) {
                DeviceType.TV -> {
                    com.suvojeet.suvmusic.ui.screens.TvHomeScreen(
                        onSongClick = { songs, index -> onPlaySong(songs, index) },
                        onPlaylistClick = { playlist ->
                            navController.navigate(
                                Destination.Playlist(
                                    playlistId = playlist.id,
                                    name = playlist.name,
                                    thumbnailUrl = playlist.thumbnailUrl
                                )
                            )
                        },
                        onAlbumClick = { album ->
                            navController.navigate(
                                Destination.Album(
                                    albumId = album.id,
                                    name = album.title,
                                    thumbnailUrl = album.thumbnailUrl
                                )
                            )
                        }
                    )
                }
                DeviceType.Tablet -> {
                    com.suvojeet.suvmusic.ui.screens.TabletHomeScreen(
                        onSongClick = { songs, index -> onPlaySong(songs, index) },
                        onPlaylistClick = { playlist ->
                            navController.navigate(
                                Destination.Playlist(
                                    playlistId = playlist.id,
                                    name = playlist.name,
                                    thumbnailUrl = playlist.thumbnailUrl
                                )
                            )
                        },
                        onAlbumClick = { album ->
                            navController.navigate(
                                Destination.Album(
                                    albumId = album.id,
                                    name = album.title,
                                    thumbnailUrl = album.thumbnailUrl
                                )
                            )
                        },
                        onRecentsClick = {
                            navController.navigate(Destination.Recents)
                        },
                        onExploreClick = { browseId, title ->
                            if (browseId == "FEmusic_moods_and_genres") {
                                navController.navigate(Destination.MoodAndGenres)
                            } else {
                                navController.navigate(Destination.Explore(browseId, title))
                            }
                        },
                        onStartRadio = { onStartRadio(null, null) },
                        currentSong = playbackInfo.currentSong
                    )
                }
                DeviceType.Phone -> {
                    HomeScreen(
                        onSongClick = { songs, index -> onPlaySong(songs, index) },
                        onPlaylistClick = { playlist ->
                            navController.navigate(
                                Destination.Playlist(
                                    playlistId = playlist.id,
                                    name = playlist.name,
                                    thumbnailUrl = playlist.thumbnailUrl
                                )
                            )
                        },
                        onAlbumClick = { album ->
                            navController.navigate(
                                Destination.Album(
                                    albumId = album.id,
                                    name = album.title,
                                    thumbnailUrl = album.thumbnailUrl
                                )
                            )
                        },
                        onRecentsClick = {
                            navController.navigate(Destination.Recents)
                        },
                        onListenTogetherClick = {
                            navController.navigate(Destination.ListenTogether)
                        },
                        onExploreClick = { browseId, title ->
                            if (browseId == "FEmusic_moods_and_genres") {
                                navController.navigate(Destination.MoodAndGenres)
                            } else {
                                navController.navigate(Destination.Explore(browseId, title))
                            }
                        },
                        onStartRadio = { onStartRadio(null, null) },
                        onCreateMixClick = {
                            navController.navigate(Destination.PickMusic)
                        },
                        currentSong = playbackInfo.currentSong
                    )
                }
            }
        }
        
        composable<Destination.ListenTogether> {
            ListenTogetherScreen(
                onDismiss = { navController.popBackStack() },
                dominantColors = dominantColors ?: com.suvojeet.suvmusic.ui.components.DominantColors(
                    primary = MaterialTheme.colorScheme.primary,
                    secondary = MaterialTheme.colorScheme.secondary,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onBackground = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        
        composable<Destination.Explore> {
            com.suvojeet.suvmusic.ui.screens.ExploreScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onPlaylistClick = { playlist ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlist.id,
                            name = playlist.name,
                            thumbnailUrl = playlist.thumbnailUrl
                        )
                    )
                },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                }
            )
        }


        composable<Destination.MoodAndGenres> {
            com.suvojeet.suvmusic.ui.screens.MoodAndGenresScreen(
                onCategoryClick = { browseId, params, title ->
                    navController.navigate(
                        Destination.MoodAndGenresDetail(browseId, params, title)
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Destination.MoodAndGenresDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Destination.MoodAndGenresDetail>()

            com.suvojeet.suvmusic.ui.screens.MoodAndGenresDetailScreen(
                browseId = route.browseId,
                params = route.params,
                title = route.title,
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs, index -> onPlaySong(songs, index) }
            )
        }
        
        composable<Destination.Search> {
            SearchScreen(
                onSongClick = { songs, index -> 
                    // Don't pass search results as queue — fetch recommendations instead
                    onStartRadio(songs[index], null)
                },
                onArtistClick = { artistId ->
                    navController.navigate(Destination.Artist(artistId))
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlistId,
                            name = null,
                            thumbnailUrl = null
                        )
                    )
                },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                }
            )
        }
        
        composable<Destination.Library> {
            LibraryScreen(
                onSongClick = { songs, index -> 
                    onPlaySong(songs, index)
                },
                onHistoryClick = {
                    navController.navigate(Destination.Recents)
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlist.id,
                            name = playlist.name,
                            thumbnailUrl = playlist.thumbnailUrl
                        )
                    )
                },
                onArtistClick = { artistId ->
                    navController.navigate(Destination.Artist(artistId))
                },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                },
                onDownloadsClick = {
                    navController.navigate(Destination.Downloads)
                }
            )
        }

        composable<Destination.Downloads> {
            com.suvojeet.suvmusic.ui.screens.DownloadsScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onPlayAll = { songs -> onPlaySong(songs, 0) },
                onShufflePlay = { songs -> 
                    val shuffledSongs = songs.shuffled()
                    onPlaySong(shuffledSongs, 0)
                }
            )
        }
        
        composable<Destination.Settings> {
            SettingsScreen(
                onLoginClick = { navController.navigate(Destination.YouTubeLogin) },
                onPlaybackClick = { navController.navigate(Destination.PlaybackSettings) },
                onAppearanceClick = { navController.navigate(Destination.AppearanceSettings) },
                onCustomizationClick = { navController.navigate(Destination.CustomizationSettings) },
                onStorageClick = { navController.navigate(Destination.Storage) },
                onStatsClick = { navController.navigate(Destination.ListeningStats) },
                onSupportClick = { navController.navigate(Destination.Support) },
                onAboutClick = { navController.navigate(Destination.About) },
                onMiscClick = { navController.navigate(Destination.Misc) },
                onCreditsClick = { navController.navigate(Destination.Credits) },
                onLastFmClick = { navController.navigate(Destination.LastFmLogin) },
                onSponsorBlockClick = { navController.navigate(Destination.SponsorBlockSettings) },
                onDiscordClick = { navController.navigate(Destination.DiscordSettings) },
                onUpdaterClick = { navController.navigate(Destination.Updater) }
            )
        }

        composable<Destination.Updater> {
            com.suvojeet.suvmusic.updater.UpdaterScreen(
                currentVersionCode = com.suvojeet.suvmusic.BuildConfig.VERSION_CODE,
                currentVersionName = com.suvojeet.suvmusic.BuildConfig.VERSION_NAME,
                viewModel = hiltViewModel(),
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Destination.Changelog> {
            com.suvojeet.suvmusic.ui.screens.ChangelogScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<Destination.Storage> {
            val settingsViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel>()
            downloadRepository?.let { repo ->
                StorageScreen(
                    downloadRepository = repo,
                    settingsViewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() },
                    onPlayerCacheClick = { navController.navigate(Destination.PlayerCache) }
                )
            }
        }

        composable<Destination.PlayerCache> {
            downloadRepository?.let { repo ->
                val settingsViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel>()
                com.suvojeet.suvmusic.ui.screens.PlayerCacheScreen(
                    onBackClick = { navController.popBackStack() },
                    settingsViewModel = settingsViewModel,
                    downloadRepository = repo
                )
            }
        }
        
        composable<Destination.PlaybackSettings> {
            PlaybackSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<Destination.AppearanceSettings> {
            AppearanceSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<Destination.CustomizationSettings> {
            CustomizationScreen(
                onBack = { navController.popBackStack() },
                onSeekbarStyleClick = { navController.navigate(Destination.SeekbarStyleSettings) },
                onArtworkShapeClick = { navController.navigate(Destination.ArtworkShapeSettings) },
                onArtworkSizeClick = { navController.navigate(Destination.ArtworkSizeSettings) }
            )
        }
        
        composable<Destination.ArtworkShapeSettings> {
            ArtworkShapeScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<Destination.SeekbarStyleSettings> {
            SeekbarStyleScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<Destination.ArtworkSizeSettings> {
            ArtworkSizeScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<Destination.Recents> {
            RecentsScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<Destination.About> {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onHowItWorksClick = { navController.navigate(Destination.HowItWorks) }
            )
        }
        
        composable<Destination.HowItWorks> {
            HowItWorksScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<Destination.Support> {
            SupportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Destination.Misc> {
            MiscScreen(
                onBack = { navController.popBackStack() },
                onLyricsProvidersClick = { navController.navigate(Destination.LyricsProviders) }
            )
        }

        composable<Destination.Credits> {
            com.suvojeet.suvmusic.ui.screens.CreditsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Destination.LyricsProviders> {
            LyricsProvidersScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Destination.SponsorBlockSettings> {
            SponsorBlockSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Destination.PickMusic> {
            com.suvojeet.suvmusic.ui.screens.PickMusicScreen(
                onBackClick = { navController.popBackStack() },
                onMixCreated = { songs ->
                    if (songs.isNotEmpty()) {
                         onPlaySong(songs, 0)
                    } else {
                         navController.popBackStack()
                    }
                }
            )
        }
        
        composable<Destination.ListeningStats> {
            com.suvojeet.suvmusic.ui.screens.ListeningStatsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        
        composable<Destination.YouTubeLogin> {
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
                        // Fetch and sync history from YouTube to provide better recommendations immediately
                        youTubeRepository.fetchAndSyncHistory()
                    }

                    // Navigate to Home and clear back stack
                    navController.navigate(Destination.Home) {
                        popUpTo<Destination.Home> { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Destination.LastFmLogin> {
            com.suvojeet.suvmusic.ui.screens.settings.LastFmSettingsScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = { username ->
                    android.widget.Toast.makeText(navController.context, "Connected as $username", android.widget.Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            )
        }

        composable<Destination.DiscordSettings> {
            com.suvojeet.suvmusic.ui.screens.settings.DiscordSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Destination.Playlist> {
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

        composable<Destination.Artist> { backStackEntry ->
            com.suvojeet.suvmusic.ui.screens.ArtistScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs, index -> 
                    onPlaySong(songs, index)
                },
                onAlbumClick = { album -> 
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                },
                onSeeAllAlbumsClick = {
                    val route = backStackEntry.toRoute<Destination.Artist>()
                    navController.navigate(
                        Destination.ArtistDiscography(route.artistId, Destination.ArtistDiscography.TYPE_ALBUMS)
                    )
                },
                onSeeAllSinglesClick = {
                    val route = backStackEntry.toRoute<Destination.Artist>()
                    navController.navigate(
                        Destination.ArtistDiscography(route.artistId, Destination.ArtistDiscography.TYPE_SINGLES)
                    )
                },
                onArtistClick = { artist ->
                    navController.navigate(Destination.Artist(artist.id))
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlist.id,
                            name = playlist.title,
                            thumbnailUrl = playlist.thumbnailUrl
                        )
                    )
                },
                onStartRadio = { radioId ->
                     navController.navigate(
                         Destination.Playlist(
                             playlistId = radioId,
                             name = null, // Navigation will fetch details or use generic "Radio"
                             thumbnailUrl = null
                         )
                     )
                }
            )
        }

        composable<Destination.ArtistDiscography> { backStackEntry ->
            val route = backStackEntry.toRoute<Destination.ArtistDiscography>()
            com.suvojeet.suvmusic.ui.screens.ArtistDiscographyScreen(
                artistId = route.artistId,
                type = route.type,
                onBackClick = { navController.popBackStack() },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                }
            )
        }

        composable<Destination.Album> {
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

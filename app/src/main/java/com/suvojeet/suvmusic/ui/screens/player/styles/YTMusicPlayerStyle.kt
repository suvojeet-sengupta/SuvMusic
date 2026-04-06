package com.suvojeet.suvmusic.ui.screens.player.styles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.repository.SponsorSegment
import com.suvojeet.suvmusic.player.SleepTimerOption
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.WaveformSeeker
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.screens.player.PlayerScreenActions
import com.suvojeet.suvmusic.ui.screens.player.components.*

@Composable
fun YTMusicPlayerStyle(
    song: com.suvojeet.suvmusic.core.model.Song?,
    playerState: PlayerState,
    playbackInfo: PlayerState,
    dominantColors: DominantColors,
    currentArtworkShape: ArtworkShape,
    currentArtworkSize: ArtworkSize,
    currentSeekbarStyle: SeekbarStyle,
    sponsorSegments: List<SponsorSegment>,
    audioArEnabled: Boolean,
    isRotatingEnabled: Boolean,
    player: Player?,
    isFullScreen: Boolean,
    isCompactHeight: Boolean,
    useWideLayout: Boolean,
    actions: PlayerScreenActions,
    onShowActions: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowRelated: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowPlaybackSpeed: () -> Unit,
    onShowEqualizer: () -> Unit,
    onShowListenTogether: () -> Unit,
    handleDoubleTapSeek: (Boolean) -> Unit,
    onShapeChange: (ArtworkShape) -> Unit,
    onSeekbarStyleChange: (SeekbarStyle) -> Unit,
    onRecenterAr: () -> Unit,
    onSetFullScreen: (Boolean) -> Unit,
    isSwitchingMode: Boolean = false,
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    currentProgress: Float = 0f,
    currentPosition: Long = 0L,
    currentDuration: Long = 0L,
    isAIEnabled: Boolean = false,
    aiStatus: String? = null
) {
    if (useWideLayout) {
        YTMusicLandscapeContent(
            song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
            currentSeekbarStyle, sponsorSegments, audioArEnabled, isRotatingEnabled, actions,
            onShowActions, onShowLyrics, onShowQueue, onShowRelated, onShowDevices, onShowSleepTimer,
            onShowPlaybackSpeed, onShowEqualizer, onShowListenTogether, playerState.isVideoMode,
            actions.onToggleVideoMode, handleDoubleTapSeek, onShapeChange, onSeekbarStyleChange,
            onRecenterAr, player, isFullScreen, onSetFullScreen, isSwitchingMode,
            sleepTimerOption, sleepTimerRemainingMs, currentProgress, currentPosition, currentDuration,
            isAIEnabled, aiStatus
        )
    } else {
        YTMusicPortraitContent(
            song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
            currentSeekbarStyle, sponsorSegments, audioArEnabled, isRotatingEnabled, player,
            isFullScreen, isCompactHeight, actions, onShowActions, onShowQueue, onShowLyrics,
            onShowRelated,
            onShowDevices, onShowSleepTimer, onShowPlaybackSpeed, onShowEqualizer,
            onShowListenTogether, handleDoubleTapSeek, onShapeChange, onSeekbarStyleChange,
            onRecenterAr, onSetFullScreen, isSwitchingMode, sleepTimerOption,
            sleepTimerRemainingMs, currentProgress, currentPosition, currentDuration,
            isAIEnabled, aiStatus
        )
    }
}

@Composable
private fun YTMusicPortraitContent(
    song: com.suvojeet.suvmusic.core.model.Song?,
    playerState: PlayerState,
    playbackInfo: PlayerState,
    dominantColors: DominantColors,
    currentArtworkShape: ArtworkShape,
    currentArtworkSize: ArtworkSize,
    currentSeekbarStyle: SeekbarStyle,
    sponsorSegments: List<SponsorSegment>,
    audioArEnabled: Boolean,
    isRotatingEnabled: Boolean,
    player: Player?,
    isFullScreen: Boolean,
    isCompactHeight: Boolean,
    actions: PlayerScreenActions,
    onShowActions: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowRelated: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowPlaybackSpeed: () -> Unit,
    onShowEqualizer: () -> Unit,
    onShowListenTogether: () -> Unit,
    handleDoubleTapSeek: (Boolean) -> Unit,
    onShapeChange: (ArtworkShape) -> Unit,
    onSeekbarStyleChange: (SeekbarStyle) -> Unit,
    onRecenterAr: () -> Unit,
    onSetFullScreen: (Boolean) -> Unit,
    isSwitchingMode: Boolean = false,
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    currentProgress: Float = 0f,
    currentPosition: Long = 0L,
    currentDuration: Long = 0L,
    isAIEnabled: Boolean = false,
    aiStatus: String? = null
) {
    val combinedLoading = playerState.isLoading || isSwitchingMode
    val controlsAlpha by animateFloatAsState(
        targetValue = if (combinedLoading) 0.45f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "controlsDimOnLoad"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        
        // Dynamic thresholds
        val isVeryShort = screenHeight < 600.dp
        val isShort = screenHeight < 700.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (isVeryShort) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerTopBar(
                onBack = actions.onBack,
                dominantColors = dominantColors,
                isVideoMode = playerState.isVideoMode,
                isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                onVideoToggle = actions.onToggleVideoMode,
                onMoreClick = onShowActions,
                onCastClick = onShowDevices,
                audioArEnabled = audioArEnabled,
                onRecenter = onRecenterAr
            )

            // Flexible space above artwork
            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.2f else 1f))
            
            // Adaptive Artwork Box
            // On short screens, we limit the artwork height to ensure it doesn't push other content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isVeryShort) 1.5f else 4f, fill = false)
                    .then(if (!isVeryShort) Modifier.aspectRatio(1f) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = playerState.isVideoMode && player != null && !isFullScreen,
                    transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) },
                    label = "video_artwork_transition"
                ) { isVideo ->
                    if (isVideo) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxHeight(if (isVeryShort) 0.9f else 1f)
                                .aspectRatio(1f)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize(currentArtworkSize.fraction / ArtworkSize.LARGE.fraction)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .clickable { onSetFullScreen(true) },
                                tonalElevation = 16.dp,
                                shadowElevation = 16.dp
                            ) {
                                AndroidView(
                                    factory = { context ->
                                        PlayerView(context).apply {
                                            this.player = player
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            setBackgroundColor(android.graphics.Color.BLACK)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = Icons.Filled.Fullscreen,
                                        contentDescription = "Full Screen",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(4.dp)
                                    )
                                }

                                M3ELoadingOverlay(isLoading = combinedLoading, dominantColors = dominantColors, modifier = Modifier.fillMaxSize())
                            }
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxHeight(if (isVeryShort) 0.9f else 1f)
                                .aspectRatio(1f)
                        ) {
                            AlbumArtwork(
                                imageUrl = song?.thumbnailUrl, title = song?.title, dominantColors = dominantColors, isLoading = combinedLoading,
                                isPlaying = playerState.isPlaying, isRotatingEnabled = isRotatingEnabled,
                                onSwipeLeft = actions.onNext, onSwipeRight = actions.onPrevious, initialShape = currentArtworkShape, artworkSize = currentArtworkSize,
                                onShapeChange = onShapeChange, onDoubleTapLeft = { handleDoubleTapSeek(false) }, onDoubleTapRight = { handleDoubleTapSeek(true) }, songId = song?.id,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            ErrorOverlay(playerState.error, dominantColors, actions, song)
                        }
                    }
                }
            }
            
            // Flexible space below artwork
            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.2f else 1f))

            SongInfoSection(
                song = song, isFavorite = playerState.isLiked, onFavoriteClick = actions.onToggleLike, isDisliked = playerState.isDisliked,
                onDislikeClick = actions.onToggleDislike, onMoreClick = onShowActions, onArtistClick = actions.onArtistClick, onAlbumClick = actions.onAlbumClick,
                dominantColors = dominantColors, isLoading = combinedLoading, compact = isShort,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                sleepTimerOption = sleepTimerOption,
                showMoreButton = false,
                isAIEnabled = isAIEnabled,
                aiStatus = aiStatus
            )

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.1f else 0.15f))

            SeekbarSection(combinedLoading, dominantColors, currentProgress, playbackInfo.isPlaying, actions, currentDuration, currentSeekbarStyle, onSeekbarStyleChange, sponsorSegments)

            TimeLabelsWithQuality(currentPositionProvider = { currentPosition }, durationProvider = { currentDuration }, dominantColors = dominantColors)

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.05f else 0.08f))

            Box(modifier = Modifier.graphicsLayer { alpha = controlsAlpha }) {
                PlaybackControls(
                    isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode,
                    onPlayPause = actions.onPlayPause, onNext = actions.onNext, onPrevious = actions.onPrevious, onShuffleToggle = actions.onShuffleToggle,
                    onRepeatToggle = actions.onRepeatToggle, dominantColors = dominantColors, compact = isShort
                )
            }

            Spacer(modifier = Modifier.height(if (isVeryShort) 8.dp else 16.dp))

            BottomActions(
                onLyricsClick = onShowLyrics, onCastClick = onShowDevices, onQueueClick = onShowQueue, onRelatedClick = onShowRelated, onDownloadClick = actions.onDownload,
                downloadState = playerState.downloadState, dominantColors = dominantColors, isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                isVideoMode = playerState.isVideoMode, onVideoToggle = actions.onToggleVideoMode, compact = isShort
            )
            
            Spacer(modifier = Modifier.height(if (isVeryShort) 12.dp else 24.dp))
        }
    }
}

@Composable
private fun YTMusicLandscapeContent(
    song: com.suvojeet.suvmusic.core.model.Song?, playerState: PlayerState, playbackInfo: PlayerState, dominantColors: DominantColors,
    currentArtworkShape: ArtworkShape, currentArtworkSize: ArtworkSize, currentSeekbarStyle: SeekbarStyle, sponsorSegments: List<SponsorSegment>,
    audioArEnabled: Boolean, isRotatingEnabled: Boolean, actions: PlayerScreenActions, onShowActions: () -> Unit, onShowLyrics: () -> Unit, onShowQueue: () -> Unit,
    onShowRelated: () -> Unit,
    onShowDevices: () -> Unit, onShowSleepTimer: () -> Unit, onShowPlaybackSpeed: () -> Unit, onShowEqualizer: () -> Unit, onShowListenTogether: () -> Unit,
    isVideoMode: Boolean, onToggleVideoMode: () -> Unit, handleDoubleTapSeek: (Boolean) -> Unit, onShapeChange: (ArtworkShape) -> Unit,
    onSeekbarStyleChange: (SeekbarStyle) -> Unit, onRecenterAr: () -> Unit,
    player: Player?, isFullScreen: Boolean, onSetFullScreen: (Boolean) -> Unit,
    isSwitchingMode: Boolean = false,
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    currentProgress: Float = 0f,
    currentPosition: Long = 0L,
    currentDuration: Long = 0L,
    isAIEnabled: Boolean = false,
    aiStatus: String? = null
) {
    val combinedLoading = playerState.isLoading || isSwitchingMode
    val controlsAlpha by animateFloatAsState(
        targetValue = if (combinedLoading) 0.45f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "controlsDimOnLoadLandscape"
    )

    Row(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(0.45f).fillMaxHeight().padding(end = 16.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isVideoMode && player != null && !isFullScreen,
                transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) },
                label = "video_artwork_transition_landscape"
            ) { isVideo ->
                if (isVideo) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxHeight(ArtworkSize.LARGE.fraction).aspectRatio(1f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight(currentArtworkSize.fraction / ArtworkSize.LARGE.fraction)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .clickable { onSetFullScreen(true) },
                            tonalElevation = 16.dp,
                            shadowElevation = 16.dp
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).apply {
                                        this.player = player
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Filled.Fullscreen,
                                    contentDescription = "Full Screen",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(4.dp)
                                )
                            }

                            M3ELoadingOverlay(isLoading = combinedLoading, dominantColors = dominantColors, modifier = Modifier.fillMaxSize())
                        }
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        AlbumArtwork(
                            imageUrl = song?.thumbnailUrl, title = song?.title, dominantColors = dominantColors, isLoading = combinedLoading,
                            isPlaying = playerState.isPlaying, isRotatingEnabled = isRotatingEnabled,
                            onSwipeLeft = actions.onNext, onSwipeRight = actions.onPrevious, initialShape = currentArtworkShape, artworkSize = currentArtworkSize,
                            onShapeChange = onShapeChange, onDoubleTapLeft = { handleDoubleTapSeek(false) }, onDoubleTapRight = { handleDoubleTapSeek(true) }, songId = song?.id
                        )
                        
                        ErrorOverlay(playerState.error, dominantColors, actions, song)
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(0.55f).fillMaxHeight().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            PlayerTopBar(
                onBack = actions.onBack, dominantColors = dominantColors, isVideoMode = isVideoMode,
                isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                onVideoToggle = onToggleVideoMode, onMoreClick = onShowActions, onCastClick = onShowDevices,
                audioArEnabled = audioArEnabled, onRecenter = onRecenterAr
            )
            Spacer(modifier = Modifier.height(8.dp))
            SongInfoSection(
                song = song, isFavorite = playerState.isLiked, onFavoriteClick = actions.onToggleLike, isDisliked = playerState.isDisliked,
                onDislikeClick = actions.onToggleDislike, onMoreClick = onShowActions, onArtistClick = actions.onArtistClick, onAlbumClick = actions.onAlbumClick,
                dominantColors = dominantColors, isLoading = combinedLoading,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                sleepTimerOption = sleepTimerOption,
                showMoreButton = false,
                isAIEnabled = isAIEnabled,
                aiStatus = aiStatus
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            SeekbarSection(combinedLoading, dominantColors, currentProgress, playbackInfo.isPlaying, actions, currentDuration, currentSeekbarStyle, onSeekbarStyleChange, sponsorSegments)
            
            TimeLabelsWithQuality(currentPositionProvider = { currentPosition }, durationProvider = { currentDuration }, dominantColors = dominantColors)
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(modifier = Modifier.graphicsLayer { alpha = controlsAlpha }) {
                PlaybackControls(isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode, onPlayPause = actions.onPlayPause, onNext = actions.onNext, onPrevious = actions.onPrevious, onShuffleToggle = actions.onShuffleToggle, onRepeatToggle = actions.onRepeatToggle, dominantColors = dominantColors)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            BottomActions(
                onLyricsClick = onShowLyrics,
                onCastClick = onShowDevices,
                onQueueClick = onShowQueue,
                onRelatedClick = onShowRelated,
                onDownloadClick = actions.onDownload,
                downloadState = playerState.downloadState,
                dominantColors = dominantColors,
                isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                isVideoMode = isVideoMode,
                onVideoToggle = onToggleVideoMode
            )
        }
    }
}

@Composable
private fun SeekbarSection(
    combinedLoading: Boolean,
    dominantColors: DominantColors,
    currentProgress: Float,
    isPlaying: Boolean,
    actions: PlayerScreenActions,
    currentDuration: Long,
    currentSeekbarStyle: SeekbarStyle,
    onSeekbarStyleChange: (SeekbarStyle) -> Unit,
    sponsorSegments: List<SponsorSegment>
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        if (combinedLoading) {
            M3ESeekbarShimmer(isVisible = true, dominantColors = dominantColors, modifier = Modifier.fillMaxWidth())
        } else {
            WaveformSeeker(
                progressProvider = { currentProgress }, isPlaying = isPlaying,
                onSeek = { actions.onSeekTo((it * currentDuration).toLong()) },
                modifier = Modifier.fillMaxWidth(), activeColor = dominantColors.accent,
                inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f),
                initialStyle = currentSeekbarStyle, onStyleChange = onSeekbarStyleChange,
                duration = currentDuration, sponsorSegments = sponsorSegments
            )
        }
    }
}

@Composable
private fun ErrorOverlay(
    error: String?,
    dominantColors: DominantColors,
    actions: PlayerScreenActions,
    song: com.suvojeet.suvmusic.core.model.Song?
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        val context = LocalContext.current
        val errorText = error ?: ""
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f).padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.75f),
            contentColor = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = "Playback Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("SuvMusic Error", errorText)
                            clipboardManager.setPrimaryClip(clip)
                            com.suvojeet.suvmusic.util.SnackbarUtil.showMessage("Error copied to clipboard")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Copy", color = Color.White)
                    }
                    
                    Button(
                        onClick = { if (song != null) actions.onPlayPause() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = dominantColors.accent)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

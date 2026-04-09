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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.data.repository.SponsorSegment
import com.suvojeet.suvmusic.player.SleepTimerOption
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.WaveformSeeker
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.screens.player.PlayerScreenActions
import com.suvojeet.suvmusic.ui.screens.player.components.*
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable

@Composable
fun ClassicPlayerStyle(
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
    aiStatus: String? = null,
    windowSizeClass: WindowSizeClass? = null
) {
    if (useWideLayout) {
        ClassicLandscapeContent(
            song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
            currentSeekbarStyle, sponsorSegments, audioArEnabled, isRotatingEnabled, actions,
            onShowActions, onShowLyrics, onShowQueue, onShowRelated, onShowDevices, onShowSleepTimer,
            onShowPlaybackSpeed, onShowEqualizer, onShowListenTogether, player, isFullScreen,
            onSetFullScreen, isSwitchingMode, sleepTimerOption, sleepTimerRemainingMs,
            currentProgress, currentPosition, currentDuration, isAIEnabled, aiStatus, windowSizeClass
        )
    } else {
        ClassicPortraitContent(
            song, playerState, playbackInfo, dominantColors, currentArtworkShape, currentArtworkSize,
            currentSeekbarStyle, sponsorSegments, audioArEnabled, isRotatingEnabled, player,
            isFullScreen, isCompactHeight, actions, onShowActions, onShowQueue, onShowLyrics,
            onShowRelated, onShowDevices, onShowSleepTimer, onShowPlaybackSpeed, onShowEqualizer,
            onShowListenTogether, handleDoubleTapSeek, onShapeChange, onSeekbarStyleChange,
            onRecenterAr, onSetFullScreen, isSwitchingMode, sleepTimerOption,
            sleepTimerRemainingMs, currentProgress, currentPosition, currentDuration, isAIEnabled, aiStatus, windowSizeClass
        )
    }
}

@Composable
private fun ClassicPortraitContent(
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
    aiStatus: String? = null,
    windowSizeClass: WindowSizeClass? = null
) {
    val combinedLoading = playerState.isLoading || isSwitchingMode

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        
        // WindowSizeClass based logic
        val heightSizeClass = windowSizeClass?.windowHeightSizeClass ?: WindowHeightSizeClass.MEDIUM
        
        // Dynamic thresholds
        val isVeryShort = heightSizeClass == WindowHeightSizeClass.COMPACT || screenHeight < 600.dp
        val isShort = screenHeight < 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (isVeryShort) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ClassicTopBar(
                onBack = actions.onBack,
                dominantColors = dominantColors,
                isVideoMode = playerState.isVideoMode,
                isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                onVideoToggle = actions.onToggleVideoMode,
                audioArEnabled = audioArEnabled,
                onRecenter = onRecenterAr
            )

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.2f else 1f))
            
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
                            modifier = Modifier
                                .fillMaxHeight(if (isVeryShort) 0.9f else 1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize(currentArtworkSize.fraction / ArtworkSize.LARGE.fraction)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .clickable { onSetFullScreen(true) },
                                tonalElevation = 16.dp, shadowElevation = 16.dp
                            ) {
                                AndroidView(factory = { context -> PlayerView(context).apply { this.player = player; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; setBackgroundColor(android.graphics.Color.BLACK) } }, modifier = Modifier.fillMaxSize())
                                M3ELoadingOverlay(isLoading = combinedLoading, dominantColors = dominantColors, modifier = Modifier.fillMaxSize())
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(if (isVeryShort) 0.9f else 1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            AlbumArtwork(
                                imageUrl = song?.thumbnailUrl, title = song?.title, dominantColors = dominantColors, isLoading = combinedLoading,
                                isPlaying = playerState.isPlaying, isRotatingEnabled = isRotatingEnabled,
                                onSwipeLeft = actions.onNext, onSwipeRight = actions.onPrevious, initialShape = currentArtworkShape, artworkSize = currentArtworkSize,
                                onShapeChange = onShapeChange, onDoubleTapLeft = { handleDoubleTapSeek(false) }, onDoubleTapRight = { handleDoubleTapSeek(true) }, songId = song?.id,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.2f else 1f))

            SongInfoSection(
                song = song, isFavorite = playerState.isLiked, onFavoriteClick = actions.onToggleLike, isDisliked = playerState.isDisliked,
                onDislikeClick = actions.onToggleDislike, onMoreClick = onShowActions, onArtistClick = actions.onArtistClick, onAlbumClick = actions.onAlbumClick,
                dominantColors = dominantColors, isLoading = combinedLoading, compact = isShort,
                sleepTimerRemainingMs = sleepTimerRemainingMs, sleepTimerOption = sleepTimerOption,
                isClassic = true,
                isAIEnabled = isAIEnabled,
                aiStatus = aiStatus
            )

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.1f else 0.4f))

            Box(modifier = Modifier.fillMaxWidth().height(if (isVeryShort) 44.dp else 60.dp), contentAlignment = Alignment.Center) {
                if (combinedLoading) {
                    M3ESeekbarShimmer(isVisible = true, dominantColors = dominantColors, modifier = Modifier.fillMaxWidth())
                } else {
                    WaveformSeeker(
                        progressProvider = { currentProgress }, isPlaying = playbackInfo.isPlaying,
                        onSeek = { actions.onSeekTo((it * currentDuration).toLong()) },
                        modifier = Modifier.fillMaxWidth(), activeColor = dominantColors.accent,
                        inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f),
                        initialStyle = currentSeekbarStyle, onStyleChange = onSeekbarStyleChange,
                        duration = currentDuration, sponsorSegments = sponsorSegments
                    )
                }
            }

            TimeLabelsWithQuality(currentPositionProvider = { currentPosition }, durationProvider = { currentDuration }, dominantColors = dominantColors)

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.1f else 0.4f))

            ClassicPlaybackControls(
                isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode,
                onPlayPause = actions.onPlayPause, onNext = actions.onNext, onPrevious = actions.onPrevious, onShuffleToggle = actions.onShuffleToggle,
                onRepeatToggle = actions.onRepeatToggle, dominantColors = dominantColors, compact = isShort
            )

            Spacer(modifier = Modifier.height(if (isVeryShort) 4.dp else 16.dp))

            ClassicBottomActions(
                onLyricsClick = onShowLyrics, onCastClick = onShowDevices, onQueueClick = onShowQueue, onRelatedClick = onShowRelated, onDownloadClick = actions.onDownload,
                downloadState = playerState.downloadState, dominantColors = dominantColors, isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                isVideoMode = playerState.isVideoMode, onVideoToggle = actions.onToggleVideoMode, compact = isShort
            )
            Spacer(modifier = Modifier.height(if (isVeryShort) 8.dp else 24.dp))
        }
    }
}

@Composable
private fun ClassicLandscapeContent(
    song: com.suvojeet.suvmusic.core.model.Song?, playerState: PlayerState, playbackInfo: PlayerState, dominantColors: DominantColors,
    currentArtworkShape: ArtworkShape, currentArtworkSize: ArtworkSize, currentSeekbarStyle: SeekbarStyle, sponsorSegments: List<SponsorSegment>,
    audioArEnabled: Boolean, isRotatingEnabled: Boolean, actions: PlayerScreenActions, onShowActions: () -> Unit, onShowLyrics: () -> Unit, onShowQueue: () -> Unit,
    onShowRelated: () -> Unit,
    onShowDevices: () -> Unit, onShowSleepTimer: () -> Unit, onShowPlaybackSpeed: () -> Unit, onShowEqualizer: () -> Unit, onShowListenTogether: () -> Unit,
    player: Player?, isFullScreen: Boolean, onSetFullScreen: (Boolean) -> Unit,
    isSwitchingMode: Boolean = false,
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    currentProgress: Float = 0f,
    currentPosition: Long = 0L,
    currentDuration: Long = 0L,
    isAIEnabled: Boolean = false,
    aiStatus: String? = null,
    windowSizeClass: WindowSizeClass? = null
) {
    val combinedLoading = playerState.isLoading || isSwitchingMode
    
    val widthSizeClass = windowSizeClass?.windowWidthSizeClass ?: WindowWidthSizeClass.MEDIUM
    val isExpanded = widthSizeClass == WindowWidthSizeClass.EXPANDED

    Row(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = if (isExpanded) 32.dp else 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(if (isExpanded) 0.4f else 0.45f).fillMaxHeight().padding(end = if (isExpanded) 32.dp else 16.dp), contentAlignment = Alignment.Center) {
            AlbumArtwork(
                imageUrl = song?.thumbnailUrl, title = song?.title, dominantColors = dominantColors, isLoading = combinedLoading,
                isPlaying = playerState.isPlaying, isRotatingEnabled = isRotatingEnabled,
                onSwipeLeft = actions.onNext, onSwipeRight = actions.onPrevious, initialShape = currentArtworkShape, artworkSize = currentArtworkSize,
                onShapeChange = { }, onDoubleTapLeft = { }, onDoubleTapRight = { }, songId = song?.id
            )
        }
        Column(modifier = Modifier.weight(0.55f).fillMaxHeight().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ClassicTopBar(
                onBack = actions.onBack,
                dominantColors = dominantColors,
                isVideoMode = playerState.isVideoMode,
                isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE,
                onVideoToggle = actions.onToggleVideoMode,
                audioArEnabled = audioArEnabled,
                onRecenter = { }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SongInfoSection(
                song = song, isFavorite = playerState.isLiked, onFavoriteClick = actions.onToggleLike, isDisliked = playerState.isDisliked,
                onDislikeClick = actions.onToggleDislike, onMoreClick = onShowActions, onArtistClick = actions.onArtistClick, onAlbumClick = actions.onAlbumClick,
                dominantColors = dominantColors, isLoading = combinedLoading,
                sleepTimerRemainingMs = sleepTimerRemainingMs, sleepTimerOption = sleepTimerOption,
                isClassic = true,
                isAIEnabled = isAIEnabled,
                aiStatus = aiStatus
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                if (combinedLoading) {
                    M3ESeekbarShimmer(isVisible = true, dominantColors = dominantColors, modifier = Modifier.fillMaxWidth())
                } else {
                    WaveformSeeker(
                        progressProvider = { currentProgress }, isPlaying = playbackInfo.isPlaying,
                        onSeek = { actions.onSeekTo((it * currentDuration).toLong()) },
                        modifier = Modifier.fillMaxWidth(), activeColor = dominantColors.accent,
                        inactiveColor = dominantColors.onBackground.copy(alpha = 0.3f),
                        initialStyle = currentSeekbarStyle, onStyleChange = { },
                        duration = currentDuration, sponsorSegments = sponsorSegments
                    )
                }
            }
            
            TimeLabelsWithQuality(currentPositionProvider = { currentPosition }, durationProvider = { currentDuration }, dominantColors = dominantColors)
            Spacer(modifier = Modifier.height(12.dp))
            
            ClassicPlaybackControls(isPlaying = playerState.isPlaying, shuffleEnabled = playerState.shuffleEnabled, repeatMode = playerState.repeatMode, onPlayPause = actions.onPlayPause, onNext = actions.onNext, onPrevious = actions.onPrevious, onShuffleToggle = actions.onShuffleToggle, onRepeatToggle = actions.onRepeatToggle, dominantColors = dominantColors)
            
            Spacer(modifier = Modifier.height(12.dp))
            ClassicBottomActions(onLyricsClick = onShowLyrics, onCastClick = onShowDevices, onQueueClick = onShowQueue, onRelatedClick = onShowRelated, onDownloadClick = actions.onDownload, downloadState = playerState.downloadState, dominantColors = dominantColors, isYouTubeSong = song?.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE, isVideoMode = playerState.isVideoMode, onVideoToggle = actions.onToggleVideoMode)
        }
    }
}

// Internal Classic Components

@Composable
private fun ClassicTopBar(
    onBack: () -> Unit,
    dominantColors: DominantColors,
    isVideoMode: Boolean = false,
    isYouTubeSong: Boolean = false,
    onVideoToggle: () -> Unit = {},
    audioArEnabled: Boolean = false,
    onRecenter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(SquircleShape)
                .background(dominantColors.onBackground.copy(alpha = 0.1f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        // Center: Switch or Title — uses weight to take available space, prevents overlap
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.Center
        ) {
            if (isYouTubeSong) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.08f))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (!isVideoMode) dominantColors.onBackground.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { if (isVideoMode) onVideoToggle() }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Audio",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isVideoMode) dominantColors.onBackground else dominantColors.onBackground.copy(alpha = 0.6f),
                            fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isVideoMode) dominantColors.onBackground.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { if (!isVideoMode) onVideoToggle() }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isVideoMode) dominantColors.onBackground else dominantColors.onBackground.copy(alpha = 0.6f),
                            fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    color = dominantColors.onBackground.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        // Right side: Audio AR or Spacer
        if (audioArEnabled) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SquircleShape)
                    .background(dominantColors.onBackground.copy(alpha = 0.1f))
                    .clickable(onClick = onRecenter),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recenter Audio",
                    tint = dominantColors.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
private fun AppleMusicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable (isPressed: Boolean) -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundAlpha by animateFloatAsState(targetValue = if (isPressed) 0.18f else 0f, label = "pressedAlpha")
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )
    
    Box(
        modifier = modifier.size(size).scale(scale).clip(CircleShape).clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center
    ) {
        content(isPressed)
    }
}

@Composable
private fun ClassicPlaybackControls(
    isPlaying: Boolean, shuffleEnabled: Boolean, repeatMode: RepeatMode, onPlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit, onRepeatToggle: () -> Unit, dominantColors: DominantColors, compact: Boolean = false
) {
    val playSize = if (compact) 56.dp else 80.dp
    val playIconSize = if (compact) 40.dp else 56.dp
    val skipSize = if (compact) 40.dp else 56.dp
    val skipIconSize = if (compact) 28.dp else 40.dp
    val secondarySize = if (compact) 36.dp else 48.dp
    val secondaryIconSize = if (compact) 22.dp else 28.dp

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconToggleButton(
            checked = shuffleEnabled, onCheckedChange = { onShuffleToggle() }, modifier = Modifier.size(secondarySize),
            colors = IconButtonDefaults.iconToggleButtonColors(contentColor = dominantColors.onBackground.copy(alpha = 0.6f), checkedContentColor = dominantColors.accent)
        ) {
            Icon(imageVector = Icons.Default.Shuffle, contentDescription = "Shuffle", modifier = Modifier.size(secondaryIconSize))
        }

        AppleMusicButton(onClick = onPrevious, size = skipSize) {
            Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Previous", tint = dominantColors.onBackground, modifier = Modifier.size(skipIconSize))
        }

        AppleMusicButton(onClick = onPlayPause, size = playSize) {
            AnimatedContent(targetState = isPlaying, label = "playPause") { playing ->
                Icon(imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = dominantColors.onBackground, modifier = Modifier.size(playIconSize))
            }
        }

        AppleMusicButton(onClick = onNext, size = skipSize) {
            Icon(imageVector = Icons.Default.FastForward, contentDescription = "Next", tint = dominantColors.onBackground, modifier = Modifier.size(skipIconSize))
        }

        IconToggleButton(
            checked = repeatMode != RepeatMode.OFF, onCheckedChange = { onRepeatToggle() }, modifier = Modifier.size(secondarySize),
            colors = IconButtonDefaults.iconToggleButtonColors(contentColor = dominantColors.onBackground.copy(alpha = 0.6f), checkedContentColor = dominantColors.accent)
        ) {
             Icon(imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, contentDescription = "Repeat", modifier = Modifier.size(secondaryIconSize))
        }
    }
}

@Composable
private fun ClassicBottomActions(
    onLyricsClick: () -> Unit, onCastClick: () -> Unit, onQueueClick: () -> Unit, onRelatedClick: () -> Unit, onDownloadClick: () -> Unit,
    downloadState: com.suvojeet.suvmusic.data.model.DownloadState, dominantColors: DominantColors, isYouTubeSong: Boolean, isVideoMode: Boolean, onVideoToggle: () -> Unit, compact: Boolean = false
) {
    val iconSize = if (compact) 20.dp else 22.dp
    val containerPadding = if (compact) 4.dp else 6.dp

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(28.dp)).background(dominantColors.onBackground.copy(alpha = 0.08f)).padding(horizontal = containerPadding, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            IconButton(onClick = onLyricsClick, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Default.Lyrics, contentDescription = "Lyrics", tint = dominantColors.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
            }

            IconButton(onClick = onRelatedClick, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Related", tint = dominantColors.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
            }

            IconButton(onClick = onDownloadClick, modifier = Modifier.weight(1f)) {
                val icon = when(downloadState) {
                    com.suvojeet.suvmusic.data.model.DownloadState.DOWNLOADED -> Icons.Default.CheckCircle
                    com.suvojeet.suvmusic.data.model.DownloadState.FAILED -> Icons.Default.Error
                    else -> Icons.Default.Download
                }
                Icon(imageVector = icon, contentDescription = "Download", tint = if (downloadState == com.suvojeet.suvmusic.data.model.DownloadState.DOWNLOADED) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
            }

            IconButton(onClick = onCastClick, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Default.Devices, contentDescription = "Output Device", tint = dominantColors.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
            }

            IconButton(onClick = onQueueClick, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = dominantColors.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
            }
        }
    }
}

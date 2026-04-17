package com.suvojeet.suvmusic.ui.screens.player.styles

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.repository.SponsorSegment
import com.suvojeet.suvmusic.player.SleepTimerOption
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.screens.player.PlayerScreenActions
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkShape
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkSize

/**
 * iOS-style Liquid Glass player.
 *
 * Layered structure:
 *   1. Heavily blurred album artwork filling the entire screen (the "glass backdrop")
 *   2. A dark/light scrim gradient for text legibility
 *   3. A subtle dominant-color wash
 *   4. The full YouTube Music style player on top, with its own background made fully
 *      transparent so the glass backdrop shines through.
 *
 * This keeps the entire existing player behavior intact (queue, lyrics, controls, seekbar,
 * gestures) while giving the whole screen the signature liquid-glass look.
 */
@Composable
fun LiquidGlassPlayerStyle(
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
    isSwitchingMode: Boolean,
    sleepTimerOption: SleepTimerOption,
    sleepTimerRemainingMs: Long?,
    currentProgress: Float,
    currentPosition: Long,
    currentDuration: Long,
    isAIEnabled: Boolean,
    aiStatus: String?,
    windowSizeClass: WindowSizeClass,
    blurRadius: Float = 60f,
    intensity: Float = 1f
) {
    val isDarkTheme = isSystemInDarkTheme()
    val thumbnailUrl = song?.thumbnailUrl
    val scrimAlpha = if (isDarkTheme) 0.55f else 0.35f
    val i = intensity.coerceIn(0.3f, 1.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Blurred album artwork as the entire backdrop
        if (!thumbnailUrl.isNullOrBlank() && !playerState.isVideoMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    blurRadius * 1.8f,
                                    blurRadius * 1.8f,
                                    android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        } else {
                            Modifier.blur((blurRadius * 0.9f).dp)
                        }
                    )
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Layer 2: Scrim for legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = scrimAlpha * i * 0.7f),
                                Color.Black.copy(alpha = scrimAlpha * i),
                                Color.Black.copy(alpha = scrimAlpha * i * 1.2f)
                            )
                        )
                    )
            )

            // Layer 3: Dominant color wash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                dominantColors.primary.copy(alpha = 0.18f * i),
                                Color.Transparent
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color(0xFF0B0B0F) else Color(0xFFF2F2F6))
            )
        }

        // Layer 4: Real player UI on top — delegate to YTMusicPlayerStyle.
        // All its internal backgrounds are drawn on top of the glass backdrop;
        // MeshGradientBackground from PlayerScreen is disabled by the caller for this style.
        YTMusicPlayerStyle(
            song = song,
            playerState = playerState,
            playbackInfo = playbackInfo,
            dominantColors = dominantColors,
            currentArtworkShape = currentArtworkShape,
            currentArtworkSize = currentArtworkSize,
            currentSeekbarStyle = currentSeekbarStyle,
            sponsorSegments = sponsorSegments,
            audioArEnabled = audioArEnabled,
            isRotatingEnabled = isRotatingEnabled,
            player = player,
            isFullScreen = isFullScreen,
            isCompactHeight = isCompactHeight,
            useWideLayout = useWideLayout,
            actions = actions,
            onShowActions = onShowActions,
            onShowQueue = onShowQueue,
            onShowLyrics = onShowLyrics,
            onShowRelated = onShowRelated,
            onShowDevices = onShowDevices,
            onShowSleepTimer = onShowSleepTimer,
            onShowPlaybackSpeed = onShowPlaybackSpeed,
            onShowEqualizer = onShowEqualizer,
            onShowListenTogether = onShowListenTogether,
            handleDoubleTapSeek = handleDoubleTapSeek,
            onShapeChange = onShapeChange,
            onSeekbarStyleChange = onSeekbarStyleChange,
            onRecenterAr = onRecenterAr,
            onSetFullScreen = onSetFullScreen,
            isSwitchingMode = isSwitchingMode,
            sleepTimerOption = sleepTimerOption,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            currentProgress = currentProgress,
            currentPosition = currentPosition,
            currentDuration = currentDuration,
            isAIEnabled = isAIEnabled,
            aiStatus = aiStatus,
            windowSizeClass = windowSizeClass
        )
    }
}

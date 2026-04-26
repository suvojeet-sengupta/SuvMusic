package com.suvojeet.suvmusic.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.media3.ui.PlayerView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.media3.ui.AspectRatioFrameLayout
import com.suvojeet.suvmusic.data.model.VideoDownloadQuality
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RotateRight

import com.suvojeet.suvmusic.ui.components.BounceButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenVideoPlayer(
    viewModel: PlayerViewModel,
    dominantColors: DominantColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerState by viewModel.playerState.collectAsState()
    val player = viewModel.getPlayer()

    // UI State
    var areControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var isVideoDownloading by remember { mutableStateOf(false) }
    var videoDownloaded by remember { mutableStateOf(false) }
    val downloadSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Double tap seeking indicators
    var showForwardIndicator by remember { mutableStateOf(false) }
    var showRewindIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(areControlsVisible, playerState.isPlaying, isLocked) {
        if (areControlsVisible && playerState.isPlaying && !isLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Modern immersive mode handling
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }

        insetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            
            // Restore system brightness
            val layoutParams = activity?.window?.attributes
            layoutParams?.screenBrightness = -1.0f
            activity?.window?.attributes = layoutParams
        }
    }

    BackHandler {
        if (isLocked) {
            isLocked = false
            areControlsVisible = true
        } else {
            onDismiss()
        }
    }

    // Controls state
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var brightness by remember { mutableStateOf(-1.0f) }
    var volumeLevel by remember { mutableStateOf(0.7f) }
    var gestureStatusText by remember { mutableStateOf("") }
    var showGestureStatus by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf(Icons.Filled.Settings) }

    LaunchedEffect(showGestureStatus) {
        if (showGestureStatus) {
            delay(1200)
            showGestureStatus = false
        }
    }

    if (showQualityDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = Color(0xFF1A1A2E),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Quality", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    com.suvojeet.suvmusic.data.model.VideoQuality.entries.forEach { quality ->
                        Surface(
                            onClick = {
                                viewModel.setVideoQuality(quality)
                                showQualityDialog = false
                            },
                            color = if (playerState.videoQuality == quality) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = playerState.videoQuality == quality,
                                    onClick = null,
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = dominantColors.primary)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(quality.label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { 
                    Text("Cancel", color = dominantColors.primary) 
                }
            }
        )
    }

    // Video download bottom sheet
    if (showDownloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadSheet = false },
            sheetState = downloadSheetState,
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                Text(
                    text = "Download Video",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))

                VideoDownloadQuality.entries.forEach { quality ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                val song = playerState.currentSong ?: return@clickable
                                showDownloadSheet = false
                                isVideoDownloading = true
                                scope.launch {
                                    val success = viewModel.downloadCurrentVideo(song, quality.maxResolution)
                                    isVideoDownloading = false
                                    if (success) videoDownloaded = true
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SaveAlt, null, tint = Color.White.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = quality.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (isLocked) {
                    detectTapGestures(onTap = { areControlsVisible = !areControlsVisible })
                } else {
                    detectTapGestures(
                        onTap = { areControlsVisible = !areControlsVisible },
                        onDoubleTap = { offset ->
                            val isForward = offset.x > size.width / 2
                            if (isForward) {
                                viewModel.seekTo(playerState.currentPosition + 10000)
                                gestureStatusText = "+10s"
                                gestureIcon = Icons.Filled.Forward10
                                scope.launch {
                                    showForwardIndicator = true
                                    delay(600)
                                    showForwardIndicator = false
                                }
                            } else {
                                viewModel.seekTo(playerState.currentPosition - 10000)
                                gestureStatusText = "-10s"
                                gestureIcon = Icons.Filled.Replay10
                                scope.launch {
                                    showRewindIndicator = true
                                    delay(600)
                                    showRewindIndicator = false
                                }
                            }
                            showGestureStatus = true
                        }
                    )
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectVerticalDragGestures { change, dragAmount ->
                        val isVolume = change.position.x > size.width / 2
                        if (isVolume) {
                            volumeLevel = (volumeLevel - dragAmount / size.height).coerceIn(0f, 1f)
                            gestureStatusText = "${(volumeLevel * 100).toInt()}%"
                            gestureIcon = if (volumeLevel > 0.6f) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown
                            
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                            audioManager?.let {
                                val max = it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                val newVolume = (volumeLevel * max).toInt()
                                it.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                            }
                        } else {
                            val activity = context as? Activity
                            if (brightness < 0) {
                                val currentBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                                brightness = if (currentBrightness < 0) 0.5f else currentBrightness
                            }
                            brightness = (brightness - dragAmount / size.height).coerceIn(0f, 1f)
                            gestureStatusText = "${(brightness * 100).toInt()}%"
                            gestureIcon = if (brightness > 0.5f) Icons.Filled.BrightnessHigh else Icons.Filled.BrightnessLow
                            
                            val layoutParams = activity?.window?.attributes
                            layoutParams?.screenBrightness = brightness
                            activity?.window?.attributes = layoutParams
                        }
                        showGestureStatus = true
                    }
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom > 1.1f && resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            gestureStatusText = "Fill"
                            gestureIcon = Icons.Filled.AspectRatio
                            showGestureStatus = true
                        } else if (zoom < 0.9f && resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            gestureStatusText = "Fit"
                            gestureIcon = Icons.Filled.AspectRatio
                            showGestureStatus = true
                        }
                    }
                }
            }
    ) {
        // Video
        AndroidView<PlayerView>(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.resizeMode = resizeMode },
            modifier = Modifier.fillMaxSize()
        )

        // Double tap seek indicators
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                DoubleTapSeekAnimation(visible = showRewindIndicator, isForward = false)
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                DoubleTapSeekAnimation(visible = showForwardIndicator, isForward = true)
            }
        }

        // Gesture Feedback
        AnimatedVisibility(
            visible = showGestureStatus,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "gesture")
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
                        label = "pulse"
                    )
                    
                    Icon(
                        imageVector = gestureIcon, 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(28.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(gestureStatusText, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isLocked) 0.15f else 0.5f))
                    .systemBarsPadding()
            ) {
                if (isLocked) {
                    // Lock button only
                    BounceButton(
                        onClick = { isLocked = false; areControlsVisible = true },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(24.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.LockOpen, "Unlock", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                } else {
                    // ─── Top Bar ──────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BounceButton(onClick = onDismiss) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Minimize", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                text = playerState.currentSong?.title ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = playerState.currentSong?.artist ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }

                        // Compact controls group
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BounceButton(onClick = { showQualityDialog = true }) {
                                val resLabel = playerState.videoQuality?.let { "${it.maxResolution}p" } ?: "Res"
                                Text(resLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            
                            BounceButton(onClick = {
                                val activity = context as? Activity
                                activity?.requestedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }) {
                                Icon(Icons.Filled.ScreenRotation, "Rotate", tint = Color.White, modifier = Modifier.size(22.dp).padding(8.dp))
                            }
                            
                            BounceButton(onClick = { showDownloadSheet = true }) {
                                when {
                                    isVideoDownloading -> LoadingIndicator(color = dominantColors.primary, modifier = Modifier.size(18.dp))
                                    videoDownloaded -> Icon(Icons.Filled.CheckCircle, "Done", tint = dominantColors.accent, modifier = Modifier.size(24.dp).padding(8.dp))
                                    else -> Icon(Icons.Filled.SaveAlt, "Download", tint = Color.White, modifier = Modifier.size(24.dp).padding(8.dp))
                                }
                            }
                        }
                    }

                    // ─── Middle Controls ───────────
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(isLandscape.let { if (it) 80.dp else 48.dp })
                    ) {
                        BounceButton(onClick = { viewModel.seekTo(playerState.currentPosition - 10000L) }) {
                            Icon(Icons.Filled.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(48.dp))
                        }

                        BounceButton(onClick = { viewModel.togglePlayPause() }) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)) {
                                if (playerState.isLoading) {
                                    LoadingIndicator(color = dominantColors.primary, modifier = Modifier.size(56.dp))
                                } else {
                                    Icon(if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(56.dp))
                                }
                            }
                        }

                        BounceButton(onClick = { viewModel.seekTo(playerState.currentPosition + 10000L) }) {
                            Icon(Icons.Filled.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }

                    // ─── Bottom Group ──────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isLandscape) 12.dp else 32.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatDuration(playerState.currentPosition), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                            
                            // Bottom actions group
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BounceButton(onClick = {
                                    resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }) {
                                    Icon(Icons.Filled.AspectRatio, "Resize", tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) dominantColors.primary else Color.White, modifier = Modifier.size(24.dp).padding(8.dp))
                                }
                                BounceButton(onClick = { isLocked = true }) {
                                    Icon(Icons.Filled.Lock, "Lock", tint = Color.White, modifier = Modifier.size(24.dp).padding(8.dp))
                                }
                            }
                            
                            Text(formatDuration(playerState.duration), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Slider(
                            value = if (playerState.duration > 0) playerState.currentPosition.toFloat() else 0f,
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..playerState.duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = dominantColors.primary,
                                activeTrackColor = dominantColors.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoubleTapSeekAnimation(
    visible: Boolean,
    isForward: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

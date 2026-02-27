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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.media3.ui.PlayerView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.media3.ui.AspectRatioFrameLayout
import com.suvojeet.suvmusic.data.model.VideoDownloadQuality
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Auto-hide controls
    var areControlsVisible by remember { mutableStateOf(true) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var isVideoDownloading by remember { mutableStateOf(false) }
    var videoDownloaded by remember { mutableStateOf(false) }
    val downloadSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(areControlsVisible, playerState.isPlaying) {
        if (areControlsVisible && playerState.isPlaying) {
            delay(3500)
            areControlsVisible = false
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Use WindowInsetsControllerCompat for modern immersive mode
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
            
            // Restore system brightness control
            val layoutParams = activity?.window?.attributes
            layoutParams?.screenBrightness = -1.0f
            activity?.window?.attributes = layoutParams
        }
    }

    BackHandler {
        // Return to embedded player screen (PiP triggers via Home/switch apps)
        onDismiss()
    }

    // Video quality dialog
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var brightness by remember { mutableStateOf(-1.0f) } // Default to system brightness (-1.0f)
    var volumeLevel by remember { mutableStateOf(0.7f) } // Default 70%
    var gestureStatusText by remember { mutableStateOf("") }
    var showGestureStatus by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf(Icons.Filled.Settings) }

    LaunchedEffect(showGestureStatus) {
        if (showGestureStatus) {
            delay(1500)
            showGestureStatus = false
        }
    }

    if (showQualityDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Video Quality", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    com.suvojeet.suvmusic.data.model.VideoQuality.entries.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setVideoQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerState.videoQuality == quality,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(quality.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Close") }
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
                    .padding(bottom = 48.dp, top = 8.dp)
            ) {
                Text(
                    text = "Download Video",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = playerState.currentSong?.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                VideoDownloadQuality.entries.forEach { quality ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quality.label,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                            }
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { areControlsVisible = !areControlsVisible },
                    onDoubleTap = { offset ->
                        val isForward = offset.x > size.width / 2
                        if (isForward) {
                            viewModel.seekTo(playerState.currentPosition + 10000)
                            gestureStatusText = "+10s"
                            gestureIcon = Icons.Filled.Forward10
                        } else {
                            viewModel.seekTo(playerState.currentPosition - 10000)
                            gestureStatusText = "-10s"
                            gestureIcon = Icons.Filled.Replay10
                        }
                        showGestureStatus = true
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    val isVolume = change.position.x > size.width / 2
                    if (isVolume) {
                        volumeLevel = (volumeLevel - dragAmount / size.height).coerceIn(0f, 1f)
                        gestureStatusText = "Volume: ${(volumeLevel * 100).toInt()}%"
                        gestureIcon = Icons.Filled.VolumeUp
                        
                        // Set system volume
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                        audioManager?.let {
                            val max = it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val newVolume = (volumeLevel * max).toInt()
                            it.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                        }
                    } else {
                        // If it's the first drag, initialize brightness from current window if it's -1
                        val activity = context as? Activity
                        if (brightness < 0) {
                            val currentBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                            brightness = if (currentBrightness < 0) 0.5f else currentBrightness
                        }
                        
                        brightness = (brightness - dragAmount / size.height).coerceIn(0f, 1f)
                        gestureStatusText = "Brightness: ${(brightness * 100).toInt()}%"
                        gestureIcon = Icons.Filled.BrightnessHigh
                        
                        val layoutParams = activity?.window?.attributes
                        layoutParams?.screenBrightness = brightness
                        activity?.window?.attributes = layoutParams
                    }
                    showGestureStatus = true
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom > 1.1f) {
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        gestureStatusText = "Zoom: Fill"
                        gestureIcon = Icons.Filled.AspectRatio
                        showGestureStatus = true
                    } else if (zoom < 0.9f) {
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        gestureStatusText = "Zoom: Fit"
                        gestureIcon = Icons.Filled.AspectRatio
                        showGestureStatus = true
                    }
                }
            }
    ) {
        // Ambient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            dominantColors.primary.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        radius = 1500f
                    )
                )
        )

        // Video Player
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
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )

        // Gesture Feedback Overlay
        AnimatedVisibility(
            visible = showGestureStatus,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = gestureIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = gestureStatusText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .systemBarsPadding()
            ) {
                // ─── Top Bar ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
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

                    // Resize Toggle
                    IconButton(onClick = {
                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        gestureStatusText = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) "Fill" else "Fit"
                        gestureIcon = Icons.Filled.AspectRatio
                        showGestureStatus = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.AspectRatio,
                            contentDescription = "Resize",
                            tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) dominantColors.primary else Color.White
                        )
                    }

                    // Resolution chip
                    val resLabel = playerState.videoQuality?.let { "${it.maxResolution}p" } ?: ""
                    if (resLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = resLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Quality picker
                    IconButton(onClick = { showQualityDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Quality",
                            tint = Color.White
                        )
                    }

                    // Download video
                    IconButton(
                        onClick = {
                            if (!isVideoDownloading && !videoDownloaded) {
                                showDownloadSheet = true
                            }
                        }
                    ) {
                        when {
                            isVideoDownloading -> CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            videoDownloaded -> Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = dominantColors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Filled.SaveAlt,
                                contentDescription = "Download Video",
                                tint = Color.White
                            )
                        }
                    }

                    // Rotate screen
                    IconButton(onClick = {
                        val activity = context as? Activity
                        if (isLandscape) {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ScreenRotation,
                            contentDescription = "Rotate",
                            tint = Color.White
                        )
                    }
                }

                // ─── Center Controls ──────────────────────────────────────────
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Skip backward 10s
                    IconButton(
                        onClick = { viewModel.seekTo(playerState.currentPosition - 10000L) },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play / Pause / Loading
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                        if (playerState.isLoading) {
                            CircularProgressIndicator(
                                color = dominantColors.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(52.dp)
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    IconButton(
                                        onClick = { viewModel.togglePlayPause() },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Skip forward 10s
                    IconButton(
                        onClick = { viewModel.seekTo(playerState.currentPosition + 10000L) },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // ─── Bottom Seekbar ───────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(playerState.currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatDuration(playerState.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    Slider(
                        value = if (playerState.duration > 0) playerState.currentPosition.toFloat() else 0f,
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..playerState.duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = dominantColors.primary,
                            activeTrackColor = dominantColors.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

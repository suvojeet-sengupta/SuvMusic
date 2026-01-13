package com.suvojeet.suvmusic.ui.screens.player.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.ui.components.DominantColors
import kotlin.math.roundToInt

/**
 * Fluid Vertical Volume Indicator with smooth spring animations.
 * Similar to iOS Control Center / MX Player volume slider.
 */
@Composable
fun VolumeIndicator(
    isVisible: Boolean,
    currentVolume: Int,
    maxVolume: Int,
    dominantColors: DominantColors,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val volumePercentage = if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f
    
    // Smooth spring animation for ultra-fluid fill
    val animatedFill by animateFloatAsState(
        targetValue = volumePercentage,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "VolumeFill"
    )
    
    // Smooth animation for percentage text
    val animatedPercentage by animateFloatAsState(
        targetValue = volumePercentage * 100,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "VolumePercentage"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + 
                slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { it / 2 } +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) + 
               slideOutHorizontally(
                   animationSpec = spring(stiffness = Spring.StiffnessHigh)
               ) { it / 2 } +
               scaleOut(
                   targetScale = 0.8f,
                   animationSpec = spring(stiffness = Spring.StiffnessHigh)
               ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val newPct = 1f - (offset.y / size.height)
                            val newVol = (newPct * maxVolume).roundToInt().coerceIn(0, maxVolume)
                            onVolumeChange(newVol)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val newPct = 1f - (change.position.y / size.height)
                            val newVol = (newPct * maxVolume).roundToInt().coerceIn(0, maxVolume)
                            onVolumeChange(newVol)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newPct = 1f - (offset.y / size.height)
                        val newVol = (newPct * maxVolume).roundToInt().coerceIn(0, maxVolume)
                        onVolumeChange(newVol)
                    }
                }
        ) {
            // Background track with subtle pattern
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dominantColors.onBackground.copy(alpha = 0.08f))
            )

            // Animated filled part (Bottom to Top) with gradient
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFill.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dominantColors.accent,
                                dominantColors.primary
                            )
                        )
                    )
            )

            // Content Overlay (Icon at bottom, Percentage at top)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Percentage Text with animated value
                Text(
                    text = "${animatedPercentage.roundToInt()}",
                    color = if (volumePercentage > 0.45f) Color.White else dominantColors.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Volume Icon - changes based on level
                Icon(
                    imageVector = when {
                        volumePercentage == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                        volumePercentage < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = if (volumePercentage < 0.12f) dominantColors.onBackground else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Composable that listens to System Volume Changes and updates the state.
 */
@Composable
fun SystemVolumeObserver(
    context: Context,
    onVolumeChanged: (Int, Int) -> Unit
) {
    val audioManager = androidx.compose.runtime.remember { 
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager 
    }
    val currentOnVolumeChanged by rememberUpdatedState(onVolumeChanged)

    DisposableEffect(context) {
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    currentOnVolumeChanged(currentVolume, maxVolume)
                }
            }
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(volumeReceiver, filter)

        // Initial check
        val initialCurrent = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val initialMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentOnVolumeChanged(initialCurrent, initialMax)

        onDispose {
            context.unregisterReceiver(volumeReceiver)
        }
    }
}

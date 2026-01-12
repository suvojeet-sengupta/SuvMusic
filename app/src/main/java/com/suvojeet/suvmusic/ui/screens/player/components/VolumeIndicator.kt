package com.suvojeet.suvmusic.ui.screens.player.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.ui.components.DominantColors

/**
 * Vertical Volume Indicator similar to MX Player / iOS Control Center.
 * Appears on the right side of the screen.
 */
@Composable
fun VolumeIndicator(
    isVisible: Boolean,
    currentVolume: Int,
    maxVolume: Int,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier
) {
    val volumePercentage = if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f
    
    // Smooth animation for the fill level
    val animatedFill by animateFloatAsState(targetValue = volumePercentage, label = "VolumeFill")

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(200.dp)
                .padding(end = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    // Apple Music style "Blur" equivalent - translucent surface
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dominantColors.onBackground.copy(alpha = 0.1f))
            )

            // Filled part (Bottom to Top)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFill)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dominantColors.primary,
                                dominantColors.accent
                            )
                        )
                    )
            )

            // Content Overlay (Icon and Percentage)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween // Icon at bottom, Text at top
            ) {
                // Percentage Text
                Text(
                    text = "${(volumePercentage * 100).toInt()}",
                    color = if (volumePercentage > 0.5f) Color.White else dominantColors.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Volume Icon
                Icon(
                    imageVector = when {
                        volumePercentage == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                        volumePercentage < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = if (volumePercentage < 0.15f) dominantColors.onBackground else Color.White,
                    modifier = Modifier.size(24.dp)
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
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
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

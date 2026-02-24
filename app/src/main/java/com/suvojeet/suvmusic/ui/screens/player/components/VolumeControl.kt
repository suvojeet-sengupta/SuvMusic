package com.suvojeet.suvmusic.ui.screens.player.components

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.components.DominantColors
import kotlinx.coroutines.flow.SharedFlow

/**
 * Self-contained volume control that manages its own state internally.
 *
 * Encapsulates [SystemVolumeObserver], volume state, auto-hide timer, and [VolumeIndicator]
 * so none of this rapidly-changing state propagates up to [PlayerScreen] and triggers
 * expensive top-level recompositions.
 */
@Composable
fun VolumeControl(
    dominantColors: DominantColors,
    volumeKeyEvents: SharedFlow<Unit>?,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioManager = remember {
        // Safe cast — VolumeControl is always hosted inside an Activity
        @Suppress("DEPRECATION")
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var maxVolume by remember {
        mutableStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
    }
    var currentVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var lastVolumeChangeTime by remember { mutableStateOf(0L) }

    // Listen for system volume broadcast changes
    SystemVolumeObserver(
        context = androidx.compose.ui.platform.LocalContext.current
    ) { newVol, newMax ->
        maxVolume = newMax
        if (currentVolume != newVol) {
            currentVolume = newVol
            lastVolumeChangeTime = System.currentTimeMillis()
        }
    }

    // Listen for hardware volume key events forwarded from the Activity
    LaunchedEffect(volumeKeyEvents) {
        volumeKeyEvents?.collect {
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            lastVolumeChangeTime = System.currentTimeMillis()
        }
    }

    // Auto-hide the indicator after 2 seconds of inactivity
    LaunchedEffect(lastVolumeChangeTime) {
        if (lastVolumeChangeTime > 0) {
            showVolumeIndicator = true
            kotlinx.coroutines.delay(2000)
            showVolumeIndicator = false
        }
    }

    VolumeIndicator(
        isVisible = showVolumeIndicator,
        currentVolume = currentVolume,
        maxVolume = maxVolume,
        dominantColors = dominantColors,
        onVolumeChange = { newVolume ->
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            currentVolume = newVolume
            lastVolumeChangeTime = System.currentTimeMillis()
        },
        modifier = modifier
    )
}

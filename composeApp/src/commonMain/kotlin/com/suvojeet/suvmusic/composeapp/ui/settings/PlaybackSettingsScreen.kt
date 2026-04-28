package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.theme.SquircleShape
import com.suvojeet.suvmusic.core.model.AudioQuality
import com.suvojeet.suvmusic.core.model.DownloadQuality
import com.suvojeet.suvmusic.core.model.HapticsIntensity
import com.suvojeet.suvmusic.core.model.HapticsMode
import com.suvojeet.suvmusic.core.model.MusicSource
import com.suvojeet.suvmusic.core.model.VideoQuality
import kotlinx.coroutines.launch

/**
 * Playback settings screen — ported from
 * `app/.../ui/screens/PlaybackSettingsScreen.kt` to commonMain.
 *
 * The Android original is wired to a SettingsViewModel through Koin and
 * carries every preference the app exposes for playback. The commonMain
 * port follows the same parallel-port shape as the other settings
 * screens:
 *   - Stateless: takes a [PlaybackSettingsState] snapshot + a
 *     [PlaybackSettingsCallbacks] bag of setters. The :app side keeps the
 *     VM/Koin/DataStore plumbing and feeds state in.
 *   - No Scaffold + TopAppBar — host owns chrome.
 *   - dpadFocusable replaced with plain Modifier.clickable.
 *   - String.format("%.1fx", x) — JVM-only — replaced with a manual
 *     one-decimal rounding helper that works on every platform.
 *   - Developer-mode music-source toggle: the host decides whether to
 *     show the JioSaavn option by passing [PlaybackSettingsState.allowJioSaavnSource].
 *   - Language picker dialog: Android original declares state but never
 *     renders a dialog (dead code). We expose [PlaybackSettingsCallbacks.onLanguagesClick]
 *     so the host can open whatever picker it has.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    state: PlaybackSettingsState,
    callbacks: PlaybackSettingsCallbacks,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp),
) {
    var showAudioQualitySheet by remember { mutableStateOf(false) }
    var showVideoQualitySheet by remember { mutableStateOf(false) }
    var showDownloadQualitySheet by remember { mutableStateOf(false) }
    var showMusicSourceSheet by remember { mutableStateOf(false) }
    var showDoubleTapSeekSheet by remember { mutableStateOf(false) }
    var showHapticsModeSheet by remember { mutableStateOf(false) }
    var showHapticsIntensitySheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val downloadSheetState = rememberModalBottomSheetState()
    val videoSheetState = rememberModalBottomSheetState()
    val musicSourceSheetState = rememberModalBottomSheetState()
    val doubleTapSeekSheetState = rememberModalBottomSheetState()
    val hapticsModeSheetState = rememberModalBottomSheetState()
    val hapticsIntensitySheetState = rememberModalBottomSheetState()

    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            SectionTitle("Music Source")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                NavigationRow(
                    icon = Icons.Default.MusicNote,
                    title = "Primary Source",
                    subtitle = when (state.musicSource) {
                        MusicSource.YOUTUBE -> "YouTube Music (256 kbps)"
                        MusicSource.JIOSAAVN -> "HQ Audio (320 kbps)"
                        MusicSource.BOTH -> "Both"
                    },
                    onClick = { showMusicSourceSheet = true },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Content Preferences")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                NavigationRow(
                    icon = Icons.Default.Language,
                    title = "Music Languages",
                    subtitle = if (state.preferredLanguages.isEmpty()) "All languages"
                    else state.preferredLanguages.joinToString(", "),
                    onClick = callbacks.onLanguagesClick,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.History,
                    title = "Sync with YouTube History",
                    subtitle = "Add played songs to your YouTube watch history",
                    checked = state.youtubeHistorySyncEnabled,
                    onCheckedChange = callbacks.onSetYouTubeHistorySyncEnabled,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Audio")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                NavigationRow(
                    icon = Icons.Default.HighQuality,
                    title = "Streaming Quality",
                    subtitle = audioQualityLabel(state.audioQuality, state.musicSource),
                    onClick = { showAudioQualitySheet = true },
                )
                ThinDivider()
                NavigationRow(
                    icon = Icons.Default.HighQuality,
                    title = "Video Quality",
                    subtitle = state.videoQuality.label,
                    onClick = { showVideoQualitySheet = true },
                )
                ThinDivider()
                NavigationRow(
                    icon = Icons.Default.Download,
                    title = "Download Quality",
                    subtitle = downloadQualityLabel(state.downloadQuality, state.musicSource),
                    onClick = { showDownloadQualitySheet = true },
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.MusicNote,
                    title = "Headphone Crossfeed",
                    subtitle = "More natural stereo imaging for headphones",
                    checked = state.crossfeedEnabled,
                    onCheckedChange = callbacks.onSetCrossfeedEnabled,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Track Transitions")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Gapless Playback",
                    subtitle = "Seamlessly transitions between songs without any pause",
                    checked = state.gaplessPlaybackEnabled,
                    onCheckedChange = callbacks.onSetGaplessPlayback,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.FastForward,
                    title = "Preload Next Song",
                    subtitle = "Start loading the next song early for instant playback",
                    checked = state.nextSongPreloadingEnabled,
                    onCheckedChange = callbacks.onSetNextSongPreloadingEnabled,
                )
                if (state.nextSongPreloadingEnabled) {
                    SliderRow(
                        label = "Start Preloading After",
                        valueLabel = "${state.nextSongPreloadDelay}s",
                        value = state.nextSongPreloadDelay.toFloat(),
                        valueRange = 0f..30f,
                        steps = 29,
                        onValueChange = { callbacks.onSetNextSongPreloadDelay(it.toInt()) },
                    )
                }
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.Refresh,
                    title = "Automix",
                    subtitle = "Keep the queue playing by adding related songs when autoplay or radio is on",
                    checked = state.automixEnabled,
                    onCheckedChange = callbacks.onSetAutomix,
                )
                ThinDivider()
                val crossLabel = if (state.crossfadeMs <= 0) "Off" else "${state.crossfadeMs / 1000f}s"
                SliderRow(
                    label = "Crossfade",
                    valueLabel = crossLabel,
                    value = state.crossfadeMs.toFloat(),
                    valueRange = 0f..12000f,
                    steps = 23,
                    onValueChange = { callbacks.onSetCrossfadeMs(it.toInt()) },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Volume")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.VolumeUp,
                    title = "In-App Volume Slider",
                    subtitle = "Show volume slider overlay when adjusting volume",
                    checked = state.volumeSliderEnabled,
                    onCheckedChange = callbacks.onSetVolumeSliderEnabled,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.Equalizer,
                    title = "Volume Normalization",
                    subtitle = "Adjust volume to a standard level",
                    checked = state.volumeNormalizationEnabled,
                    onCheckedChange = callbacks.onSetVolumeNormalizationEnabled,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.MusicNote,
                    title = "Play During Calls",
                    subtitle = "Keep music playing during Google Meet or phone calls",
                    checked = state.ignoreAudioFocusDuringCalls,
                    onCheckedChange = callbacks.onSetIgnoreAudioFocusDuringCalls,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.PhoneCallback,
                    title = "Auto-resume After Calls",
                    subtitle = "Automatically resume playback when a call ends",
                    checked = state.autoResumeAfterCall,
                    onCheckedChange = callbacks.onSetAutoResumeAfterCall,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.VolumeUp,
                    title = "Volume Boost",
                    subtitle = "Boost volume beyond 100%. Use with caution.",
                    checked = state.volumeBoostEnabled,
                    onCheckedChange = callbacks.onSetVolumeBoostEnabled,
                )
                if (state.volumeBoostEnabled) {
                    SliderRow(
                        label = "Boost Amount",
                        valueLabel = "+${(state.volumeBoostAmount * 0.15).toInt()} dB",
                        value = state.volumeBoostAmount.toFloat(),
                        valueRange = 0f..100f,
                        steps = 19,
                        onValueChange = { callbacks.onSetVolumeBoostAmount(it.toInt()) },
                    )
                }
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.HighQuality,
                    title = "Audio Offload",
                    subtitle = "Use specialized audio hardware for playback. Saves battery.",
                    checked = state.audioOffloadEnabled,
                    onCheckedChange = callbacks.onSetAudioOffloadEnabled,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.Language,
                    title = "Spatial Audio (Audio AR)",
                    subtitle = "Rotate soundstage based on device rotation. Needs headphones.",
                    checked = state.audioArEnabled,
                    onCheckedChange = callbacks.onSetAudioArEnabled,
                )
                if (state.audioArEnabled) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        SwitchRow(
                            icon = Icons.Default.Refresh,
                            title = "Auto-Calibration",
                            subtitle = "Automatically adjust center point if head is stable",
                            checked = state.audioArAutoCalibrate,
                            onCheckedChange = callbacks.onSetAudioArAutoCalibrate,
                        )
                        SliderRow(
                            label = "Soundstage Depth (Sensitivity)",
                            valueLabel = "${formatOneDecimal(state.audioArSensitivity)}x",
                            value = state.audioArSensitivity,
                            valueRange = 0.5f..2.5f,
                            steps = 19,
                            onValueChange = callbacks.onSetAudioArSensitivity,
                        )
                        NavigationRow(
                            icon = Icons.Default.Refresh,
                            title = "Recenter Audio",
                            subtitle = "Set current direction as front",
                            onClick = callbacks.onCalibrateAudioAr,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Gestures")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.Gesture,
                    title = "Swipe Down to Dismiss",
                    subtitle = "Swipe down on mini player to stop and close",
                    checked = state.swipeDownToDismissEnabled,
                    onCheckedChange = callbacks.onSetSwipeDownToDismissEnabled,
                )
                ThinDivider()
                NavigationRow(
                    icon = Icons.Default.Gesture,
                    title = "Double Tap to Seek",
                    subtitle = "${state.doubleTapSeekSeconds} seconds",
                    onClick = { showDoubleTapSeekSheet = true },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Music Haptics")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.Vibration,
                    title = "Music Haptics",
                    subtitle = "Feel the music with taps & vibrations synced to the beat",
                    checked = state.musicHapticsEnabled,
                    onCheckedChange = callbacks.onSetMusicHapticsEnabled,
                )
                if (state.musicHapticsEnabled) {
                    Column {
                        ThinDivider()
                        NavigationRow(
                            icon = Icons.Default.Vibration,
                            title = "Haptics Mode",
                            subtitle = when (state.hapticsMode) {
                                HapticsMode.OFF -> "Disabled"
                                HapticsMode.BASIC -> "Basic (Strong beats only)"
                                HapticsMode.ADVANCED -> "Advanced (Full analysis)"
                                HapticsMode.CUSTOM -> "Custom"
                            },
                            onClick = { showHapticsModeSheet = true },
                        )
                        ThinDivider()
                        NavigationRow(
                            icon = Icons.Default.Vibration,
                            title = "Vibration Intensity",
                            subtitle = state.hapticsIntensity.displayName,
                            onClick = { showHapticsIntensitySheet = true },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAudioQualitySheet) {
        QualityBottomSheet(
            title = "Audio Quality",
            sheetState = sheetState,
            onDismiss = { showAudioQualitySheet = false },
        ) {
            AudioQuality.entries.forEach { quality ->
                RadioRow(
                    label = audioQualityLabel(quality, state.musicSource),
                    selected = state.audioQuality == quality,
                    onClick = {
                        callbacks.onSetAudioQuality(quality)
                        scope.launch {
                            sheetState.hide()
                            showAudioQualitySheet = false
                        }
                    },
                )
            }
        }
    }

    if (showVideoQualitySheet) {
        QualityBottomSheet(
            title = "Video Quality",
            sheetState = videoSheetState,
            onDismiss = { showVideoQualitySheet = false },
        ) {
            VideoQuality.entries.forEach { quality ->
                RadioRow(
                    label = quality.label,
                    selected = state.videoQuality == quality,
                    onClick = {
                        callbacks.onSetVideoQuality(quality)
                        scope.launch {
                            videoSheetState.hide()
                            showVideoQualitySheet = false
                        }
                    },
                )
            }
        }
    }

    if (showDownloadQualitySheet) {
        QualityBottomSheet(
            title = "Download Quality",
            subtitle = "Lower quality = smaller file size",
            sheetState = downloadSheetState,
            onDismiss = { showDownloadQualitySheet = false },
        ) {
            DownloadQuality.entries.forEach { quality ->
                RadioRow(
                    label = downloadQualityLabel(quality, state.musicSource),
                    selected = state.downloadQuality == quality,
                    onClick = {
                        callbacks.onSetDownloadQuality(quality)
                        scope.launch {
                            downloadSheetState.hide()
                            showDownloadQualitySheet = false
                        }
                    },
                )
            }
        }
    }

    if (showMusicSourceSheet) {
        QualityBottomSheet(
            title = "Primary Music Source",
            subtitle = if (state.allowJioSaavnSource) {
                "HQ Audio offers higher quality (320 kbps) audio"
            } else {
                "Select your preferred music source"
            },
            sheetState = musicSourceSheetState,
            onDismiss = { showMusicSourceSheet = false },
        ) {
            val sourceOptions = buildList {
                add(MusicSource.YOUTUBE to "YouTube Music (256 kbps max)")
                if (state.allowJioSaavnSource) {
                    add(MusicSource.JIOSAAVN to "HQ Audio (320 kbps)")
                }
            }
            sourceOptions.forEach { (source, label) ->
                RadioRow(
                    label = label,
                    selected = state.musicSource == source,
                    onClick = {
                        callbacks.onSetMusicSource(source)
                        scope.launch {
                            musicSourceSheetState.hide()
                            showMusicSourceSheet = false
                        }
                    },
                )
            }
        }
    }

    if (showDoubleTapSeekSheet) {
        QualityBottomSheet(
            title = "Double Tap to Seek",
            sheetState = doubleTapSeekSheetState,
            onDismiss = { showDoubleTapSeekSheet = false },
        ) {
            listOf(5, 10, 15, 30).forEach { seconds ->
                RadioRow(
                    label = "$seconds seconds",
                    selected = state.doubleTapSeekSeconds == seconds,
                    onClick = {
                        callbacks.onSetDoubleTapSeekSeconds(seconds)
                        scope.launch {
                            doubleTapSeekSheetState.hide()
                            showDoubleTapSeekSheet = false
                        }
                    },
                )
            }
        }
    }

    if (showHapticsModeSheet) {
        QualityBottomSheet(
            title = "Haptics Mode",
            subtitle = "Choose how sensitive the haptic feedback should be",
            sheetState = hapticsModeSheetState,
            onDismiss = { showHapticsModeSheet = false },
        ) {
            HapticsMode.entries.filter { it != HapticsMode.OFF }.forEach { mode ->
                val (label, description) = when (mode) {
                    HapticsMode.BASIC -> "Basic" to "Responds to strong beats only"
                    HapticsMode.ADVANCED -> "Advanced" to "Full audio spectrum analysis"
                    HapticsMode.CUSTOM -> "Custom" to "Fine-tuned for your preference"
                    HapticsMode.OFF -> "" to ""
                }

                RadioRow(
                    label = label,
                    description = description,
                    selected = state.hapticsMode == mode,
                    onClick = {
                        callbacks.onSetHapticsMode(mode)
                        scope.launch {
                            hapticsModeSheetState.hide()
                            showHapticsModeSheet = false
                        }
                    },
                )
            }
        }
    }

    if (showHapticsIntensitySheet) {
        QualityBottomSheet(
            title = "Vibration Intensity",
            subtitle = "Adjust how strong the vibrations feel",
            sheetState = hapticsIntensitySheetState,
            onDismiss = { showHapticsIntensitySheet = false },
        ) {
            HapticsIntensity.entries.forEach { intensity ->
                RadioRow(
                    label = intensity.displayName,
                    selected = state.hapticsIntensity == intensity,
                    onClick = {
                        callbacks.onSetHapticsIntensity(intensity)
                        scope.launch {
                            hapticsIntensitySheetState.hide()
                            showHapticsIntensitySheet = false
                        }
                    },
                )
            }
        }
    }
}

/* ----- helpers ---------------------------------------------------------- */

private fun audioQualityLabel(quality: AudioQuality, source: MusicSource): String =
    if (source == MusicSource.JIOSAAVN) {
        when (quality) {
            AudioQuality.LOW -> "Low (96 kbps)"
            AudioQuality.MEDIUM -> "Standard (160 kbps)"
            AudioQuality.HIGH -> "High (320 kbps)"
            AudioQuality.AUTO -> "Auto (Adaptive)"
        }
    } else {
        when (quality) {
            AudioQuality.LOW -> "Low (48 kbps)"
            AudioQuality.MEDIUM -> "Normal (128 kbps)"
            AudioQuality.HIGH -> "High (256 kbps)"
            AudioQuality.AUTO -> "Auto (Adaptive)"
        }
    }

private fun downloadQualityLabel(quality: DownloadQuality, source: MusicSource): String =
    if (source == MusicSource.JIOSAAVN) {
        when (quality) {
            DownloadQuality.LOW -> "Low (96 kbps)"
            DownloadQuality.MEDIUM -> "Standard (160 kbps)"
            DownloadQuality.HIGH -> "High (320 kbps)"
        }
    } else {
        when (quality) {
            DownloadQuality.LOW -> "Low (48 kbps) • Saves data"
            DownloadQuality.MEDIUM -> "Medium (128 kbps)"
            DownloadQuality.HIGH -> "High (256 kbps)"
        }
    }

/** One-decimal rounding without `String.format` (which is JVM-only). */
private fun formatOneDecimal(value: Float): String {
    val tenths = (value * 10f + if (value >= 0) 0.5f else -0.5f).toInt()
    val whole = tenths / 10
    val frac = kotlin.math.abs(tenths % 10)
    return "$whole.$frac"
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
private fun ThinDivider() {
    M3HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
    )
}

@Composable
private fun LeadingIconBox(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(SquircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = { LeadingIconBox(icon) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = { LeadingIconBox(icon) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun SliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualityBottomSheet(
    title: String,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = if (subtitle == null) 16.dp else 8.dp, start = 8.dp),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
                )
            }
            content()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = null,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = description?.let { { Text(it) } },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/* ----- state + callbacks ------------------------------------------------ */

/**
 * Snapshot of every preference [PlaybackSettingsScreen] reads from. The
 * Android side currently builds this from SettingsViewModel.uiState — desktop
 * hosts can populate it from whatever persistence they wire up.
 */
data class PlaybackSettingsState(
    val musicSource: MusicSource = MusicSource.YOUTUBE,
    val preferredLanguages: Set<String> = emptySet(),
    val youtubeHistorySyncEnabled: Boolean = false,
    val audioQuality: AudioQuality = AudioQuality.AUTO,
    val videoQuality: VideoQuality,
    val downloadQuality: DownloadQuality = DownloadQuality.MEDIUM,
    val crossfeedEnabled: Boolean = false,
    val gaplessPlaybackEnabled: Boolean = true,
    val nextSongPreloadingEnabled: Boolean = false,
    val nextSongPreloadDelay: Int = 0,
    val automixEnabled: Boolean = false,
    val crossfadeMs: Int = 0,
    val volumeSliderEnabled: Boolean = true,
    val volumeNormalizationEnabled: Boolean = false,
    val ignoreAudioFocusDuringCalls: Boolean = false,
    val autoResumeAfterCall: Boolean = true,
    val volumeBoostEnabled: Boolean = false,
    val volumeBoostAmount: Int = 0,
    val audioOffloadEnabled: Boolean = false,
    val audioArEnabled: Boolean = false,
    val audioArAutoCalibrate: Boolean = true,
    val audioArSensitivity: Float = 1f,
    val swipeDownToDismissEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val musicHapticsEnabled: Boolean = false,
    val hapticsMode: HapticsMode = HapticsMode.OFF,
    val hapticsIntensity: HapticsIntensity,
    val allowJioSaavnSource: Boolean = false,
)

/**
 * Setter callbacks for every preference [PlaybackSettingsScreen] can change.
 * Bundled into a class so the screen's parameter list stays manageable.
 */
data class PlaybackSettingsCallbacks(
    val onSetMusicSource: (MusicSource) -> Unit,
    val onLanguagesClick: () -> Unit,
    val onSetYouTubeHistorySyncEnabled: (Boolean) -> Unit,
    val onSetAudioQuality: (AudioQuality) -> Unit,
    val onSetVideoQuality: (VideoQuality) -> Unit,
    val onSetDownloadQuality: (DownloadQuality) -> Unit,
    val onSetCrossfeedEnabled: (Boolean) -> Unit,
    val onSetGaplessPlayback: (Boolean) -> Unit,
    val onSetNextSongPreloadingEnabled: (Boolean) -> Unit,
    val onSetNextSongPreloadDelay: (Int) -> Unit,
    val onSetAutomix: (Boolean) -> Unit,
    val onSetCrossfadeMs: (Int) -> Unit,
    val onSetVolumeSliderEnabled: (Boolean) -> Unit,
    val onSetVolumeNormalizationEnabled: (Boolean) -> Unit,
    val onSetIgnoreAudioFocusDuringCalls: (Boolean) -> Unit,
    val onSetAutoResumeAfterCall: (Boolean) -> Unit,
    val onSetVolumeBoostEnabled: (Boolean) -> Unit,
    val onSetVolumeBoostAmount: (Int) -> Unit,
    val onSetAudioOffloadEnabled: (Boolean) -> Unit,
    val onSetAudioArEnabled: (Boolean) -> Unit,
    val onSetAudioArAutoCalibrate: (Boolean) -> Unit,
    val onSetAudioArSensitivity: (Float) -> Unit,
    val onCalibrateAudioAr: () -> Unit,
    val onSetSwipeDownToDismissEnabled: (Boolean) -> Unit,
    val onSetDoubleTapSeekSeconds: (Int) -> Unit,
    val onSetMusicHapticsEnabled: (Boolean) -> Unit,
    val onSetHapticsMode: (HapticsMode) -> Unit,
    val onSetHapticsIntensity: (HapticsIntensity) -> Unit,
)

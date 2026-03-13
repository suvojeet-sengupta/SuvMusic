package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.MusicSource
import com.suvojeet.suvmusic.data.model.*
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var showAudioQualitySheet by remember { mutableStateOf(false) }
    var showVideoQualitySheet by remember { mutableStateOf(false) }
    var showDownloadQualitySheet by remember { mutableStateOf(false) }
    var showHapticsIntensitySheet by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3EPageHeader(
                title = "Playback",
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            )
        ) {
            // AUDIO QUALITY Section
            item { M3ESettingsGroupHeader("AUDIO QUALITY") }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Streaming Quality",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    M3EButtonGroup(
                        options = AudioQuality.entries,
                        selected = uiState.audioQuality,
                        onSelect = { viewModel.setAudioQuality(it) },
                        label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                    )
                }
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Download,
                    title = "Download Quality",
                    subtitle = getDownloadQualityLabel(uiState.downloadQuality, uiState.musicSource),
                    onClick = { showDownloadQualitySheet = true }
                )
            }

            // PLAYBACK BEHAVIOR Section
            item { M3ESettingsGroupHeader("PLAYBACK BEHAVIOR") }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Gapless Playback",
                    subtitle = "Seamless transitions between songs",
                    checked = uiState.gaplessPlaybackEnabled,
                    onCheckedChange = { viewModel.setGaplessPlayback(it) }
                )
            }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.Equalizer,
                    title = "Normalize Volume",
                    subtitle = "Adjust volume to a standard level",
                    checked = uiState.volumeNormalizationEnabled,
                    onCheckedChange = { viewModel.setVolumeNormalizationEnabled(it) }
                )
            }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.Vibration,
                    title = "Music Haptics",
                    subtitle = "Feel the music with vibrations",
                    checked = uiState.musicHapticsEnabled,
                    onCheckedChange = { viewModel.setMusicHapticsEnabled(it) }
                )
            }
            if (uiState.musicHapticsEnabled) {
                item {
                    M3ENavigationItem(
                        icon = Icons.Default.Tune,
                        title = "Haptic Intensity",
                        subtitle = uiState.hapticsIntensity.displayName,
                        onClick = { showHapticsIntensitySheet = true }
                    )
                }
            }

            // SEEK & SKIP Section
            item { M3ESettingsGroupHeader("SEEK & SKIP") }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Double Tap to Seek",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    M3EButtonGroup(
                        options = listOf(5, 10, 15, 30),
                        selected = uiState.doubleTapSeekSeconds,
                        onSelect = { viewModel.setDoubleTapSeekSeconds(it) },
                        label = { "${it}s" }
                    )
                }
            }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.FastForward,
                    title = "Preload Next Song",
                    checked = uiState.nextSongPreloadingEnabled,
                    onCheckedChange = { viewModel.setNextSongPreloadingEnabled(it) }
                )
            }

            // VIDEO Section
            item { M3ESettingsGroupHeader("VIDEO") }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Videocam,
                    title = "Video Quality",
                    subtitle = uiState.videoQuality.label,
                    onClick = { showVideoQualitySheet = true }
                )
            }

            // EQUALIZER & EFFECTS Section
            item { M3ESettingsGroupHeader("EQUALIZER & EFFECTS") }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Equalizer",
                    onClick = { /* Show Equalizer Sheet */ }
                )
            }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.Language,
                    title = "Spatial Audio",
                    subtitle = "Audio AR support for headphones",
                    checked = uiState.audioArEnabled,
                    onCheckedChange = { viewModel.setAudioArEnabled(it) }
                )
            }

            // HISTORY & DATA Section
            item { M3ESettingsGroupHeader("HISTORY & DATA") }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.History,
                    title = "Save Listening History",
                    subtitle = "Sync with YouTube watch history",
                    checked = uiState.youtubeHistorySyncEnabled,
                    onCheckedChange = { viewModel.setYouTubeHistorySyncEnabled(it) }
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.BarChart,
                    title = "Listening Stats",
                    onClick = { /* Navigate to stats */ }
                )
            }
        }
    }

    // Picker Sheets
    if (showDownloadQualitySheet) {
        M3EPickerSheet(
            title = "Download Quality",
            options = DownloadQuality.entries,
            selected = uiState.downloadQuality,
            label = { getDownloadQualityLabel(it, uiState.musicSource) },
            onSelect = { viewModel.setDownloadQuality(it) },
            onDismiss = { showDownloadQualitySheet = false },
            sheetState = sheetState
        )
    }

    if (showVideoQualitySheet) {
        M3EPickerSheet(
            title = "Video Quality",
            options = VideoQuality.entries,
            selected = uiState.videoQuality,
            label = { it.label },
            onSelect = { viewModel.setVideoQuality(it) },
            onDismiss = { showVideoQualitySheet = false },
            sheetState = sheetState
        )
    }

    if (showHapticsIntensitySheet) {
        M3EPickerSheet(
            title = "Haptic Intensity",
            options = HapticsIntensity.entries,
            selected = uiState.hapticsIntensity,
            label = { it.displayName },
            onSelect = { viewModel.setHapticsIntensity(it) },
            onDismiss = { showHapticsIntensitySheet = false },
            sheetState = sheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> M3EPickerSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(bottom = 16.dp))
            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable { onSelect(option); onDismiss() },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = MaterialTheme.shapes.large,
                ) {
                    ListItem(
                        headlineContent = { Text(label(option), style = if (isSelected) MaterialTheme.typography.bodyLargeEmphasized else MaterialTheme.typography.bodyLarge) },
                        trailingContent = if (isSelected) {
                            { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun getAudioQualityLabel(quality: AudioQuality, source: MusicSource): String {
    return if (source == MusicSource.JIOSAAVN) {
        when (quality) {
            AudioQuality.LOW -> "Low (96 kbps)"
            AudioQuality.MEDIUM -> "Standard (160 kbps)"
            AudioQuality.HIGH -> "High (320 kbps)"
        }
    } else {
        when (quality) {
            AudioQuality.LOW -> "Low (48 kbps)"
            AudioQuality.MEDIUM -> "Normal (128 kbps)"
            AudioQuality.HIGH -> "High (256 kbps)"
        }
    }
}

private fun getDownloadQualityLabel(quality: DownloadQuality, source: MusicSource): String {
    return if (source == MusicSource.JIOSAAVN) {
        when (quality) {
            DownloadQuality.LOW -> "Low (96 kbps)"
            DownloadQuality.MEDIUM -> "Standard (160 kbps)"
            DownloadQuality.HIGH -> "High (320 kbps)"
        }
    } else {
        when (quality) {
            DownloadQuality.LOW -> "Low (48 kbps)"
            DownloadQuality.MEDIUM -> "Medium (128 kbps)"
            DownloadQuality.HIGH -> "High (256 kbps)"
        }
    }
}

package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.*
import com.suvojeet.suvmusic.data.MusicSource
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.util.MusicHapticsManager
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.theme.PillShape
import com.suvojeet.suvmusic.util.dpadFocusable
import kotlinx.coroutines.launch
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider

/**
 * Playback settings screen with Material 3 Expressive design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAudioQualitySheet by remember { mutableStateOf(false) }
    var showVideoQualitySheet by remember { mutableStateOf(false) }
    var showDownloadQualitySheet by remember { mutableStateOf(false) }
    var showMusicSourceSheet by remember { mutableStateOf(false) }
    var showDoubleTapSeekSheet by remember { mutableStateOf(false) }
    var showHapticsModeSheet by remember { mutableStateOf(false) }
    var showHapticsIntensitySheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    val downloadSheetState = rememberModalBottomSheetState()
    val videoSheetState = rememberModalBottomSheetState()
    val musicSourceSheetState = rememberModalBottomSheetState()
    val doubleTapSeekSheetState = rememberModalBottomSheetState()
    val hapticsModeSheetState = rememberModalBottomSheetState()
    val hapticsIntensitySheetState = rememberModalBottomSheetState()

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .dpadFocusable(
                                onClick = onBack,
                                shape = CircleShape,
                            )
                            .padding(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Music Source
            item {
                PlaybackSectionTitle("Music Source")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaybackNavigationItem(
                        icon = Icons.Default.MusicNote,
                        title = "Primary Source",
                        subtitle = when (uiState.musicSource) {
                            MusicSource.YOUTUBE -> "YouTube Music (256 kbps)"
                            MusicSource.JIOSAAVN -> "HQ Audio (320 kbps)"
                            MusicSource.BOTH -> "Both"
                        },
                        onClick = { showMusicSourceSheet = true }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Content Preferences
            item {
                PlaybackSectionTitle("Content Preferences")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaybackNavigationItem(
                        icon = Icons.Default.Language,
                        title = "Music Languages",
                        subtitle = if (uiState.preferredLanguages.isEmpty()) "All languages"
                                   else uiState.preferredLanguages.joinToString(", "),
                        onClick = { showLanguageDialog = true }
                    )
                    
                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.History,
                        title = "Sync with YouTube History",
                        subtitle = "Add played songs to your YouTube watch history",
                        checked = uiState.youtubeHistorySyncEnabled,
                        onCheckedChange = { viewModel.setYouTubeHistorySyncEnabled(it) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Audio Section
            item {
                PlaybackSectionTitle("Audio")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaybackNavigationItem(
                        icon = Icons.Default.HighQuality,
                        title = "Streaming Quality",
                        subtitle = getAudioQualityLabel(uiState.audioQuality, uiState.musicSource),
                        onClick = { showAudioQualitySheet = true }
                    )

                    HorizontalDivider()

                    PlaybackNavigationItem(
                        icon = Icons.Default.HighQuality,
                        title = "Video Quality",
                        subtitle = uiState.videoQuality.label,
                        onClick = { showVideoQualitySheet = true }
                    )
                    
                    HorizontalDivider()

                    PlaybackNavigationItem(
                        icon = Icons.Default.Download,
                        title = "Download Quality",
                        subtitle = getDownloadQualityLabel(uiState.downloadQuality, uiState.musicSource),
                        onClick = { showDownloadQualitySheet = true }
                    )

                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.MusicNote,
                        title = "Headphone Crossfeed",
                        subtitle = "More natural stereo imaging for headphones",
                        checked = uiState.crossfeedEnabled,
                        onCheckedChange = { viewModel.setCrossfeedEnabled(it) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Track Transitions
            item {
                PlaybackSectionTitle("Track Transitions")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaybackSwitchItem(
                        icon = Icons.Default.GraphicEq,
                        title = "Gapless Playback",
                        subtitle = "Seamlessly transitions between songs without any pause",
                        checked = uiState.gaplessPlaybackEnabled,
                        onCheckedChange = { viewModel.setGaplessPlayback(it) }
                    )
                    
                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.FastForward,
                        title = "Preload Next Song",
                        subtitle = "Start loading the next song early for instant playback",
                        checked = uiState.nextSongPreloadingEnabled,
                        onCheckedChange = { viewModel.setNextSongPreloadingEnabled(it) }
                    )
                    
                    if (uiState.nextSongPreloadingEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Start Preloading After",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${uiState.nextSongPreloadDelay}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = uiState.nextSongPreloadDelay.toFloat(),
                                onValueChange = { viewModel.setNextSongPreloadDelay(it.toInt()) },
                                valueRange = 0f..30f,
                                steps = 29
                            )
                        }
                    }
                    
                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.Refresh,
                        title = "Automix",
                        subtitle = "Allows seamless transitions between songs on certain playlists",
                        checked = uiState.automixEnabled,
                        onCheckedChange = { viewModel.setAutomix(it) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Volume Controls
            item {
                PlaybackSectionTitle("Volume")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaybackSwitchItem(
                        icon = Icons.Default.VolumeUp,
                        title = "In-App Volume Slider",
                        subtitle = "Show volume slider overlay when adjusting volume",
                        checked = uiState.volumeSliderEnabled,
                        onCheckedChange = { viewModel.setVolumeSliderEnabled(it) }
                    )

                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.Equalizer,
                        title = "Volume Normalization",
                        subtitle = "Adjust volume to a standard level",
                        checked = uiState.volumeNormalizationEnabled,
                        onCheckedChange = { viewModel.setVolumeNormalizationEnabled(it) }
                    )

                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.MusicNote,
                        title = "Play During Calls",
                        subtitle = "Keep music playing during Google Meet or phone calls",
                        checked = uiState.ignoreAudioFocusDuringCalls,
                        onCheckedChange = { viewModel.setIgnoreAudioFocusDuringCalls(it) }
                    )

                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.PhoneCallback,
                        title = "Auto-resume After Calls",
                        subtitle = "Automatically resume playback when a call ends",
                        checked = uiState.autoResumeAfterCall,
                        onCheckedChange = { viewModel.setAutoResumeAfterCall(it) }
                    )

                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Volume Boost",
                        subtitle = "Boost volume beyond 100%. Use with caution.",
                        checked = uiState.volumeBoostEnabled,
                        onCheckedChange = { viewModel.setVolumeBoostEnabled(it) }
                    )

                    if (uiState.volumeBoostEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Boost Amount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "+${(uiState.volumeBoostAmount * 0.15).toInt()} dB",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = uiState.volumeBoostAmount.toFloat(),
                                onValueChange = { viewModel.setVolumeBoostAmount(it.toInt()) },
                                valueRange = 0f..100f,
                                steps = 19
                            )
                        }
                    }

                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.HighQuality,
                        title = "Audio Offload",
                        subtitle = "Use specialized audio hardware for playback. Saves battery.",
                        checked = uiState.audioOffloadEnabled,
                        onCheckedChange = { viewModel.setAudioOffloadEnabled(it) }
                    )

                    HorizontalDivider()

                    PlaybackSwitchItem(
                        icon = Icons.Default.Language,
                        title = "Spatial Audio (Audio AR)",
                        subtitle = "Rotate soundstage based on device rotation. Needs headphones.",
                        checked = uiState.audioArEnabled,
                        onCheckedChange = { viewModel.setAudioArEnabled(it) }
                    )

                    if (uiState.audioArEnabled) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            PlaybackSwitchItem(
                                icon = Icons.Default.Refresh,
                                title = "Auto-Calibration",
                                subtitle = "Automatically adjust center point if head is stable",
                                checked = uiState.audioArAutoCalibrate,
                                onCheckedChange = { viewModel.setAudioArAutoCalibrate(it) }
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Soundstage Depth (Sensitivity)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = String.format("%.1fx", uiState.audioArSensitivity),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Slider(
                                    value = uiState.audioArSensitivity,
                                    onValueChange = { viewModel.setAudioArSensitivity(it) },
                                    valueRange = 0.5f..2.5f,
                                    steps = 19
                                )
                            }

                            PlaybackNavigationItem(
                                icon = Icons.Default.Refresh,
                                title = "Recenter Audio",
                                subtitle = "Set current direction as front",
                                onClick = { viewModel.calibrateAudioAr() }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Gestures
            item {
                PlaybackSectionTitle("Gestures")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaybackSwitchItem(
                        icon = Icons.Default.Gesture,
                        title = "Swipe Down to Dismiss",
                        subtitle = "Swipe down on mini player to stop and close",
                        checked = uiState.swipeDownToDismissEnabled,
                        onCheckedChange = { viewModel.setSwipeDownToDismissEnabled(it) }
                    )

                    HorizontalDivider()

                    PlaybackNavigationItem(
                        icon = Icons.Default.Gesture,
                        title = "Double Tap to Seek",
                        subtitle = "${uiState.doubleTapSeekSeconds} seconds",
                        onClick = { showDoubleTapSeekSheet = true }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Music Haptics Section
            item {
                PlaybackSectionTitle("Music Haptics")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaybackSwitchItem(
                        icon = Icons.Default.Vibration,
                        title = "Music Haptics",
                        subtitle = "Feel the music with taps & vibrations synced to the beat",
                        checked = uiState.musicHapticsEnabled,
                        onCheckedChange = { viewModel.setMusicHapticsEnabled(it) }
                    )
                    
                    if (uiState.musicHapticsEnabled) {
                        Column {
                            HorizontalDivider()
                            PlaybackNavigationItem(
                                icon = Icons.Default.Vibration,
                                title = "Haptics Mode",
                                subtitle = when (uiState.hapticsMode) {
                                    HapticsMode.OFF -> "Disabled"
                                    HapticsMode.BASIC -> "Basic (Strong beats only)"
                                    HapticsMode.ADVANCED -> "Advanced (Full analysis)"
                                    HapticsMode.CUSTOM -> "Custom"
                                },
                                onClick = { showHapticsModeSheet = true }
                            )
                            
                            HorizontalDivider()
                            PlaybackNavigationItem(
                                icon = Icons.Default.Vibration,
                                title = "Vibration Intensity",
                                subtitle = uiState.hapticsIntensity.displayName,
                                onClick = { showHapticsIntensitySheet = true }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Audio Quality Bottom Sheet
    if (showAudioQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioQualitySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Audio Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                AudioQuality.entries.forEach { quality ->
                    ListItem(
                        headlineContent = { Text(getAudioQualityLabel(quality, uiState.musicSource)) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.audioQuality == quality,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setAudioQuality(quality)
                                    scope.launch {
                                        sheetState.hide()
                                        showAudioQualitySheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Video Quality Bottom Sheet
    if (showVideoQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showVideoQualitySheet = false },
            sheetState = videoSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                VideoQuality.entries.forEach { quality ->
                    ListItem(
                        headlineContent = { Text(quality.label) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.videoQuality == quality,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setVideoQuality(quality)
                                    scope.launch {
                                        videoSheetState.hide()
                                        showVideoQualitySheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDownloadQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadQualitySheet = false },
            sheetState = downloadSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Download Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
                
                Text(
                    text = "Lower quality = smaller file size",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                DownloadQuality.entries.forEach { quality ->
                    ListItem(
                        headlineContent = { Text(getDownloadQualityLabel(quality, uiState.musicSource)) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.downloadQuality == quality,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setDownloadQuality(quality)
                                    scope.launch {
                                        downloadSheetState.hide()
                                        showDownloadQualitySheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Music Source Bottom Sheet
    if (showMusicSourceSheet) {
        val isDeveloperMode by viewModel.isDeveloperMode.collectAsState(initial = false)
        
        ModalBottomSheet(
            onDismissRequest = { showMusicSourceSheet = false },
            sheetState = musicSourceSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Primary Music Source",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
                
                Text(
                    text = if (isDeveloperMode) "HQ Audio offers higher quality (320 kbps) audio" else "Select your preferred music source",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                val sourceOptions = buildList {
                    add(MusicSource.YOUTUBE to "YouTube Music (256 kbps max)")
                    if (isDeveloperMode) {
                        add(MusicSource.JIOSAAVN to "HQ Audio (320 kbps)")
                    }
                }
                
                sourceOptions.forEach { (source, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.musicSource == source,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setMusicSource(source)
                                    scope.launch {
                                        musicSourceSheetState.hide()
                                        showMusicSourceSheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Double Tap Seek Sheet
    if (showDoubleTapSeekSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDoubleTapSeekSheet = false },
            sheetState = doubleTapSeekSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Double Tap to Seek",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                val options = listOf(5, 10, 15, 30)
                
                options.forEach { seconds ->
                    ListItem(
                        headlineContent = { Text("$seconds seconds") },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.doubleTapSeekSeconds == seconds,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setDoubleTapSeekSeconds(seconds)
                                    scope.launch {
                                        doubleTapSeekSheetState.hide()
                                        showDoubleTapSeekSheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Haptics Mode Bottom Sheet
    if (showHapticsModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHapticsModeSheet = false },
            sheetState = hapticsModeSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Haptics Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
                
                Text(
                    text = "Choose how sensitive the haptic feedback should be",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                HapticsMode.entries.filter { it != HapticsMode.OFF }.forEach { mode ->
                    val (label, description) = when (mode) {
                        HapticsMode.BASIC -> "Basic" to "Responds to strong beats only"
                        HapticsMode.ADVANCED -> "Advanced" to "Full audio spectrum analysis"
                        HapticsMode.CUSTOM -> "Custom" to "Fine-tuned for your preference"
                        else -> "" to ""
                    }
                    
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { Text(description) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.hapticsMode == mode,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setHapticsMode(mode)
                                    scope.launch {
                                        hapticsModeSheetState.hide()
                                        showHapticsModeSheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Haptics Intensity Bottom Sheet
    if (showHapticsIntensitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHapticsIntensitySheet = false },
            sheetState = hapticsIntensitySheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Vibration Intensity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
                
                Text(
                    text = "Adjust how strong the vibrations feel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                HapticsIntensity.entries.forEach { intensity ->
                    ListItem(
                        headlineContent = { Text(intensity.displayName) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.hapticsIntensity == intensity,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setHapticsIntensity(intensity)
                                    scope.launch {
                                        hapticsIntensitySheetState.hide()
                                        showHapticsIntensitySheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PlaybackSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun HorizontalDivider() {
    M3HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    )
}

@Composable
private fun PlaybackNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun PlaybackSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = Modifier
            .dpadFocusable(onClick = { onCheckedChange(!checked) }, shape = SquircleShape)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

private fun getAudioQualityLabel(quality: AudioQuality, source: MusicSource): String {
    return if (source == MusicSource.JIOSAAVN) {
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
            DownloadQuality.LOW -> "Low (48 kbps) • Saves data"
            DownloadQuality.MEDIUM -> "Medium (128 kbps)"
            DownloadQuality.HIGH -> "High (256 kbps)"
        }
    }
}

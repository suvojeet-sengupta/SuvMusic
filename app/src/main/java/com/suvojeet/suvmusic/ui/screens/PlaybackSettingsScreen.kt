package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.data.model.VideoQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import com.suvojeet.suvmusic.data.model.HapticsIntensity
import com.suvojeet.suvmusic.data.model.HapticsMode
import com.suvojeet.suvmusic.data.MusicSource
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.util.MusicHapticsManager
import kotlinx.coroutines.launch

/**
 * Playback settings screen with streaming quality, download quality, gapless playback, and automix.
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
    val sheetState = rememberModalBottomSheetState()
    val downloadSheetState = rememberModalBottomSheetState()
    val videoSheetState = rememberModalBottomSheetState()

    val musicSourceSheetState = rememberModalBottomSheetState()
    val doubleTapSeekSheetState = rememberModalBottomSheetState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showHapticsModeSheet by remember { mutableStateOf(false) }
    var showHapticsIntensitySheet by remember { mutableStateOf(false) }
    val hapticsModeSheetState = rememberModalBottomSheetState()
    val hapticsIntensitySheetState = rememberModalBottomSheetState()
    var showReverbSheet by remember { mutableStateOf(false) }
    val reverbSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
     var showOffloadInfo by remember { mutableStateOf(false) }
    var showGaplessInfo by remember { mutableStateOf(false) }
    var showHistorySyncInfo by remember { mutableStateOf(false) }
    var showAudioArInfo by remember { mutableStateOf(false) }

    if (showAudioArInfo) {
        AlertDialog(
            onDismissRequest = { showAudioArInfo = false },
            title = { 
                Text(
                    "What is Audio AR?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Text(
                    "Audio AR (Spatial Audio) creates a 3D soundstage using your device's sensors.\n\n" +
                    "• Dynamic Soundstage: As you rotate your phone, the audio positioning shifts to simulate a fixed sound source.\n" +
                    "• Immersion: Provides a more realistic, concert-like listening experience.\n\n" +
                    "Note: Best experienced with headphones. Uses gyroscope sensors.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showAudioArInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showHistorySyncInfo) {
        AlertDialog(
            onDismissRequest = { showHistorySyncInfo = false },
            title = { 
                Text(
                    "Sync with YouTube History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Text(
                    "Enabling this will add songs you play here to your official YouTube Music history.\n\n" +
                    "• Recommendations: Your YouTube Music recommendations will improve based on what you listen to here.\n" +
                    "• Resume Watching: Songs may appear in your 'Resume Playing' lists on other YouTube apps.\n\n" +
                    "Note: This is an experimental feature.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showHistorySyncInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showOffloadInfo) {
        AlertDialog(
            onDismissRequest = { showOffloadInfo = false },
            title = { 
                Text(
                    "What is Audio Offload?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Text(
                    "Audio Offload enables your phone's specialized audio hardware to handle music playback instead of the main processor (CPU).\n\n" +
                    "• Battery Save: Reduces CPU load, which helps improve battery life.\n" +
                    "• Performance: Allows background tasks to run more smoothly.\n\n" +
                    "Note: If you experience any playback issues or stutters, please disable this feature.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showOffloadInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }
    
    if (showGaplessInfo) {
        AlertDialog(
            onDismissRequest = { showGaplessInfo = false },
            title = { 
                Text(
                    "What is Gapless Playback?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Text(
                    "Gapless playback eliminates the brief pause between songs. When enabled, the app detects when a track is about to end and seamlessly switches to the next song.\n\n" +
                    "This is especially useful for:\n" +
                    "• Live albums or concerts where songs flow together\n" +
                    "• Classical music with continuous movements\n" +
                    "• DJ mixes and playlists designed for uninterrupted listening\n\n" +
                    "Note: This feature uses additional battery and data to preload the next song in advance.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showGaplessInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Playback") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Music Source
            PlaybackSectionTitle("Music Source")
            
            ListItem(
                headlineContent = { Text("Primary Source") },
                supportingContent = { 
                    Text(
                        when (uiState.musicSource) {
                            MusicSource.YOUTUBE -> "YouTube Music (256 kbps)"
                            MusicSource.JIOSAAVN -> "HQ Audio (320 kbps)"
                            MusicSource.BOTH -> "Both"
                        }
                    ) 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showMusicSourceSheet = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Content Preferences
            PlaybackSectionTitle("Content Preferences")

            ListItem(
                headlineContent = { Text("Music Languages") },
                supportingContent = {
                    val languages = uiState.preferredLanguages
                    Text(
                        if (languages.isEmpty()) "All languages"
                        else languages.joinToString(", ")
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showLanguageDialog = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sync with YouTube History")
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showHistorySyncInfo = true },
                            modifier = Modifier.height(24.dp).width(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.height(16.dp).width(16.dp)
                            )
                        }
                    }
                },
                supportingContent = { 
                    Text("Add played songs to your YouTube watch history") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.youtubeHistorySyncEnabled,
                        onCheckedChange = { viewModel.setYouTubeHistorySyncEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            // Audio Section
            PlaybackSectionTitle("Audio")
            
            ListItem(
                headlineContent = { Text("Streaming Quality") },
                supportingContent = { 
                    Text(getAudioQualityLabel(uiState.audioQuality, uiState.musicSource)) 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showAudioQualitySheet = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { Text("Video Quality") },
                supportingContent = {
                    Text(uiState.videoQuality.label)
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showVideoQualitySheet = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { Text("Download Quality") },
                supportingContent = { 
                    Text(getDownloadQualityLabel(uiState.downloadQuality, uiState.musicSource)) 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showDownloadQualitySheet = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            // Track Transitions
            PlaybackSectionTitle("Track transitions")
            
            ListItem(
                headlineContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Gapless playback")
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showGaplessInfo = true },
                            modifier = Modifier.height(24.dp).width(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.height(16.dp).width(16.dp)
                            )
                        }
                    }
                },
                supportingContent = { 
                    Text("Seamlessly transitions between songs without any pause.") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.gaplessPlaybackEnabled,
                        onCheckedChange = { viewModel.setGaplessPlayback(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { Text("Preload next song") },
                supportingContent = { 
                    Text("Start loading the next song early for instant playback.") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.nextSongPreloadingEnabled,
                        onCheckedChange = { viewModel.setNextSongPreloadingEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                    
                    Text(
                        text = "Set to 0s for immediate preload, or higher to save data when skipping.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            ListItem(
                headlineContent = { Text("Automix") },
                supportingContent = { 
                    Text("Allows seamless transitions between songs on certain playlists.") 
                },
                trailingContent = {
                    Switch(
                        checked = uiState.automixEnabled,
                        onCheckedChange = { viewModel.setAutomix(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            // Volume Controls
            PlaybackSectionTitle("Volume")
            
            ListItem(
                headlineContent = { Text("In-app volume slider") },
                supportingContent = { 
                    Text("Show volume slider overlay when adjusting volume in the app.") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.volumeSliderEnabled,
                        onCheckedChange = { 
                            viewModel.setVolumeSliderEnabled(it)
                        }
                    )
                },

                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { Text("Volume Normalization") },
                supportingContent = { 
                    Text("Adjust volume to a standard level") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.volumeNormalizationEnabled,
                        onCheckedChange = { 
                            viewModel.setVolumeNormalizationEnabled(it)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { Text("Play during calls") },
                supportingContent = { 
                    Text("Keep music playing during Google Meet or phone calls") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.ignoreAudioFocusDuringCalls,
                        onCheckedChange = { 
                            viewModel.setIgnoreAudioFocusDuringCalls(it)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { Text("Volume Boost") },
                supportingContent = { 
                    Text("Boost volume beyond 100%. Use with caution.") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.volumeBoostEnabled,
                        onCheckedChange = { 
                            viewModel.setVolumeBoostEnabled(it)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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

            ListItem(
                headlineContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable offload")
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showOffloadInfo = true },
                            modifier = Modifier.height(24.dp).width(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.height(16.dp).width(16.dp)
                            )
                        }
                    }
                },
                supportingContent = { 
                    Text("Use the offload audio path for audio playback. Disabling this may increase power usage.") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.audioOffloadEnabled,
                        onCheckedChange = { 
                            viewModel.setAudioOffloadEnabled(it)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Audio AR (Spatial Audio)")
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showAudioArInfo = true },
                            modifier = Modifier.height(24.dp).width(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.height(16.dp).width(16.dp)
                            )
                        }
                    }
                },
                supportingContent = { 
                    Text("Rotate soundstage based on device rotation. Requires headphones.") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.audioArEnabled,
                        onCheckedChange = { viewModel.setAudioArEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            if (uiState.audioArEnabled) {
                ListItem(
                    headlineContent = { Text("Auto-Calibration") },
                    supportingContent = { Text("Automatically adjust center point if head is stable") },
                    trailingContent = {
                        Switch(
                            checked = uiState.audioArAutoCalibrate,
                            onCheckedChange = { viewModel.setAudioArAutoCalibrate(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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

                ListItem(
                    headlineContent = { Text("Recenter Audio") },
                    supportingContent = { Text("Set current direction as front") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { viewModel.calibrateAudioAr() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            // Audio Effects
            PlaybackSectionTitle("Audio Effects")

            ListItem(
                headlineContent = { Text("Reverb Preset") },
                supportingContent = { Text(uiState.reverbPreset.label) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showReverbSheet = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                        text = "Virtualizer (Surround Sound)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${(uiState.virtualizerStrength / 10)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Slider(
                    value = uiState.virtualizerStrength.toFloat(),
                    onValueChange = { viewModel.setVirtualizerStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    steps = 99
                )
            }
            
            // Gestures
            PlaybackSectionTitle("Gestures")
            
            ListItem(
                headlineContent = { Text("Double tap to seek") },
                supportingContent = { 
                    Text("${uiState.doubleTapSeekSeconds} seconds") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showDoubleTapSeekSheet = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            // Music Haptics Section
            PlaybackSectionTitle("Music Haptics")
            
            ListItem(
                headlineContent = { Text("Music Haptics") },
                supportingContent = { 
                    Text("Feel the music with taps & vibrations synced to the beat") 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.musicHapticsEnabled,
                        onCheckedChange = { viewModel.setMusicHapticsEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            // Show mode and intensity options only when haptics is enabled
            if (uiState.musicHapticsEnabled) {
                ListItem(
                    headlineContent = { Text("Haptics Mode") },
                    supportingContent = { 
                        Text(
                            when (uiState.hapticsMode) {
                                HapticsMode.OFF -> "Disabled"
                                HapticsMode.BASIC -> "Basic (Strong beats only)"
                                HapticsMode.ADVANCED -> "Advanced (Full analysis)"
                                HapticsMode.CUSTOM -> "Custom"
                            }
                        )
                    },
                    modifier = Modifier.clickable { showHapticsModeSheet = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                
                ListItem(
                    headlineContent = { Text("Vibration Intensity") },
                    supportingContent = { 
                        Text(uiState.hapticsIntensity.displayName)
                    },
                    modifier = Modifier.clickable { showHapticsIntensitySheet = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    
    // Audio Quality Bottom Sheet
    if (showAudioQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioQualitySheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Audio Quality",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setAudioQuality(quality)
                                scope.launch {
                                    sheetState.hide()
                                    showAudioQualitySheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.audioQuality == quality,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = getAudioQualityLabel(quality, uiState.musicSource))
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Download Quality Bottom Sheet
    if (showVideoQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showVideoQualitySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                VideoQuality.entries.forEach { quality ->
                    val isSelected = uiState.videoQuality == quality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setVideoQuality(quality)
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showVideoQualitySheet = false
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    if (showDownloadQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadQualitySheet = false },
            sheetState = downloadSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Download Quality",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Lower quality = smaller file size",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                DownloadQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setDownloadQuality(quality)
                                scope.launch {
                                    downloadSheetState.hide()
                                    showDownloadQualitySheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.downloadQuality == quality,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = getDownloadQualityLabel(quality, uiState.musicSource))
                    }
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
            sheetState = musicSourceSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Primary Music Source",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = if (isDeveloperMode) "HQ Audio offers higher quality (320 kbps) audio" else "Select your preferred music source",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Build source list based on developer mode
                val sourceOptions = buildList {
                    add(MusicSource.YOUTUBE to "YouTube Music (256 kbps max)")
                    if (isDeveloperMode) {
                        add(MusicSource.JIOSAAVN to "HQ Audio (320 kbps)")
                    }
                }
                
                sourceOptions.forEach { (source, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setMusicSource(source)
                                scope.launch {
                                    musicSourceSheetState.hide()
                                    showMusicSourceSheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.musicSource == source,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = label)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    
    // Double Tap Seek Sheet
    if (showDoubleTapSeekSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDoubleTapSeekSheet = false },
            sheetState = doubleTapSeekSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Double Tap to Seek",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val options = listOf(5, 10, 15, 30)
                
                options.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setDoubleTapSeekSeconds(seconds)
                                scope.launch {
                                    doubleTapSeekSheetState.hide()
                                    showDoubleTapSeekSheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.doubleTapSeekSeconds == seconds,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "$seconds seconds")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Haptics Mode Bottom Sheet
    if (showHapticsModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHapticsModeSheet = false },
            sheetState = hapticsModeSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Haptics Mode",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Choose how sensitive the haptic feedback should be",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val modeOptions = listOf(
                    HapticsMode.BASIC to "Basic" to "Responds to strong beats only",
                    HapticsMode.ADVANCED to "Advanced" to "Full audio spectrum analysis",
                    HapticsMode.CUSTOM to "Custom" to "Fine-tuned for your preference"
                )
                
                HapticsMode.entries.filter { it != HapticsMode.OFF }.forEach { mode ->
                    val (label, description) = when (mode) {
                        HapticsMode.BASIC -> "Basic" to "Responds to strong beats only"
                        HapticsMode.ADVANCED -> "Advanced" to "Full audio spectrum analysis"
                        HapticsMode.CUSTOM -> "Custom" to "Fine-tuned for your preference"
                        else -> "" to ""
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setHapticsMode(mode)
                                scope.launch {
                                    hapticsModeSheetState.hide()
                                    showHapticsModeSheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.hapticsMode == mode,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = label)
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
            }
        }
    }

    // Haptics Intensity Bottom Sheet
    if (showHapticsIntensitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHapticsIntensitySheet = false },
            sheetState = hapticsIntensitySheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Vibration Intensity",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Adjust how strong the vibrations feel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                HapticsIntensity.entries.forEach { intensity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setHapticsIntensity(intensity)
                                scope.launch {
                                    hapticsIntensitySheetState.hide()
                                    showHapticsIntensitySheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.hapticsIntensity == intensity,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = intensity.displayName)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Reverb Preset Bottom Sheet
    if (showReverbSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReverbSheet = false },
            sheetState = reverbSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Text(
                    text = "Reverb Preset",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                com.suvojeet.suvmusic.data.model.ReverbPreset.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setReverbPreset(preset)
                                scope.launch {
                                    reverbSheetState.hide()
                                    showReverbSheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.reverbPreset == preset,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        com.suvojeet.suvmusic.ui.components.LanguageSelectionDialog(
            initialSelection = uiState.preferredLanguages,
            onDismiss = { showLanguageDialog = false },
            onSave = { languages ->
                viewModel.setPreferredLanguages(languages)
                showLanguageDialog = false
            }
        )
    }
}

@Composable
private fun PlaybackSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

private fun getAudioQualityLabel(quality: AudioQuality, source: MusicSource): String {
    return if (source == MusicSource.JIOSAAVN) {
        when (quality) {
            AudioQuality.LOW -> "Low (96 kbps)"
            AudioQuality.MEDIUM -> "Standard (160 kbps)"
            AudioQuality.HIGH -> "High (320 kbps)"
        }
    } else {
        // YouTube Defaults
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
            DownloadQuality.LOW -> "Low (96 kbps) • Saves data"
            DownloadQuality.MEDIUM -> "Standard (160 kbps)"
            DownloadQuality.HIGH -> "High (320 kbps)"
        }
    } else {
        // YouTube Defaults
        when (quality) {
            DownloadQuality.LOW -> "Low (48 kbps) • Saves data"
            DownloadQuality.MEDIUM -> "Medium (128 kbps)"
            DownloadQuality.HIGH -> "High (256 kbps)"
        }
    }
}

package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
    var showQualitySheet by remember { mutableStateOf(false) }
    var showDownloadQualitySheet by remember { mutableStateOf(false) }
    var showMusicSourceSheet by remember { mutableStateOf(false) }
    var showDoubleTapSeekSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val downloadSheetState = rememberModalBottomSheetState()
    val musicSourceSheetState = rememberModalBottomSheetState()
    val doubleTapSeekSheetState = rememberModalBottomSheetState()
    var showHapticsModeSheet by remember { mutableStateOf(false) }
    var showHapticsIntensitySheet by remember { mutableStateOf(false) }
    val hapticsModeSheetState = rememberModalBottomSheetState()
    val hapticsIntensitySheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
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
            SectionTitle("Music Source")
            
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
            
            // Audio Section
            SectionTitle("Audio")
            
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
                modifier = Modifier.clickable { showQualitySheet = true },
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
            SectionTitle("Track transitions")
            
            ListItem(
                headlineContent = { Text("Gapless playback") },
                supportingContent = { 
                    Text("Removes any gaps or pauses that may occur in between tracks.") 
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
            SectionTitle("Volume")
            
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
            
            // Gestures
            SectionTitle("Gestures")
            
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
            SectionTitle("Music Haptics")
            
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
    
    // Streaming Quality Bottom Sheet
    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
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
                                    showQualitySheet = false
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
                
                Spacer(modifier = Modifier.height(32.dp))
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
}

@Composable
private fun SectionTitle(title: String) {
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

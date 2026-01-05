package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.Slider
import androidx.compose.material.icons.filled.GraphicEq
import kotlin.math.roundToInt

/**
 * Settings screen with audio quality, theme, and account settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onLoginClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQualitySheet by remember { mutableStateOf(false) }
    var showDownloadQualitySheet by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val downloadSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Account Section
        SectionTitle("Account")
        
        if (uiState.isLoggedIn) {
            // Signed in with YT Music logo
            ListItem(
                headlineContent = { 
                    Text(
                        text = "Signed in",
                        fontWeight = FontWeight.Medium
                    ) 
                },
                supportingContent = { 
                    Text(
                        text = "YouTube Music connected",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    if (uiState.userAvatarUrl != null) {
                        AsyncImage(
                            model = uiState.userAvatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
            
            // Sign Out button with warning
            ListItem(
                headlineContent = { 
                    Text(
                        text = "Sign out",
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                supportingContent = { 
                    Text(
                        text = "Disconnect from YouTube Music",
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    ) 
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable { showSignOutDialog = true },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        } else {
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Login,
                title = "Sign in to YouTube Music",
                subtitle = "Access your playlists and recommendations",
                onClick = onLoginClick
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Playback Section
        SectionTitle("Playback")
        
        SettingsItem(
            icon = Icons.Default.HighQuality,
            title = "Streaming Quality",
            subtitle = uiState.audioQuality.label,
            onClick = { showQualitySheet = true }
        )
        
        // Download Quality
        ListItem(
            headlineContent = { Text("Download Quality") },
            supportingContent = { Text(uiState.downloadQuality.label) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )
            },
            modifier = Modifier.clickable { showDownloadQualitySheet = true },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Track Transitions Section
        SectionTitle("Track transitions")
        
        // Gapless playback
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
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
        
        // Automix
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
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
        
        // Crossfade slider
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Crossfade",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Adjust the length of fading and overlap in between tracks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "0 s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = uiState.crossfadeDuration.toFloat(),
                    onValueChange = { viewModel.setCrossfadeDuration(it.roundToInt()) },
                    valueRange = 0f..12f,
                    steps = 11,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text(
                    text = "12 s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uiState.crossfadeDuration > 0) {
                Text(
                    text = "Current: ${uiState.crossfadeDuration}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Appearance Section
        SectionTitle("Appearance")
        
        ListItem(
            headlineContent = { Text("Dynamic Theme") },
            supportingContent = { Text("Use system colors (Android 12+)") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null
                )
            },
            trailingContent = {
                Switch(
                    checked = uiState.dynamicColorEnabled,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // About Section
        SectionTitle("About")
        
        SettingsItem(
            icon = Icons.Default.Info,
            title = "About SuvMusic",
            subtitle = "Version, credits & more",
            onClick = onAboutClick
        )
        
        Spacer(modifier = Modifier.height(100.dp))
    }
    
    // Sign Out Warning Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Sign out?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You will be disconnected from YouTube Music. Your playlists and recommendations will no longer be available until you sign in again."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout()
                        showSignOutDialog = false
                    }
                ) {
                    Text(
                        text = "Sign Out",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Audio Quality Bottom Sheet
    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
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
                        Text(text = quality.label)
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
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
                        Text(text = quality.label)
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
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

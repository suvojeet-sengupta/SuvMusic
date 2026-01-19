package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.UpdateState
import com.suvojeet.suvmusic.service.DynamicIslandService
import com.suvojeet.suvmusic.ui.components.CheckingUpdateDialog
import com.suvojeet.suvmusic.ui.components.DownloadProgressDialog
import com.suvojeet.suvmusic.ui.components.NoUpdateDialog
import com.suvojeet.suvmusic.ui.components.UpdateAvailableDialog
import com.suvojeet.suvmusic.ui.components.UpdateErrorDialog
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Settings screen with navigation to sub-settings.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onLoginClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onCustomizationClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    // Dynamic Island
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dynamicIslandEnabled by viewModel.dynamicIslandEnabled.collectAsState(initial = false)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp)
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
        
        // Playback - Navigate to PlaybackSettingsScreen
        SettingsItem(
            icon = Icons.Default.GraphicEq,
            title = "Playback",
            subtitle = buildPlaybackSubtitle(uiState.gaplessPlaybackEnabled, uiState.automixEnabled),
            onClick = onPlaybackClick
        )
        
        // Appearance - Navigate to AppearanceSettingsScreen
        SettingsItem(
            icon = Icons.Default.DarkMode,
            title = "Appearance",
            subtitle = buildAppearanceSubtitle(uiState.themeMode.label, uiState.dynamicColorEnabled),
            onClick = onAppearanceClick
        )
        
        // Customization - Navigate to CustomizationScreen
        SettingsItem(
            icon = Icons.Default.Tune,
            title = "Customization",
            subtitle = "Seekbar style, artwork shape",
            onClick = onCustomizationClick
        )
        
        // Dynamic Island Toggle
        ListItem(
            headlineContent = { Text("Dynamic Island") },
            supportingContent = { 
                Text("Floating mini-player near camera when minimized") 
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null
                )
            },
            trailingContent = {
                Switch(
                    checked = dynamicIslandEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Check permission first
                            if (DynamicIslandService.hasOverlayPermission(context)) {
                                scope.launch { viewModel.setDynamicIslandEnabled(true) }
                            } else {
                                // Open overlay permission settings
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        } else {
                            scope.launch { viewModel.setDynamicIslandEnabled(false) }
                            DynamicIslandService.stop(context)
                        }
                    }
                )
            },
            modifier = Modifier.clickable {
                if (!dynamicIslandEnabled && !DynamicIslandService.hasOverlayPermission(context)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Storage & Downloads Section
        SectionTitle("Storage & Downloads")
        
        // Offline Mode Toggle
        val offlineModeEnabled by viewModel.offlineModeEnabled.collectAsState(initial = false)
        
        ListItem(
            headlineContent = { Text("Offline Mode") },
            supportingContent = { 
                Text("Only play downloaded songs") 
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null
                )
            },
            trailingContent = {
                Switch(
                    checked = offlineModeEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { viewModel.setOfflineMode(enabled) }
                    }
                )
            },
            modifier = Modifier.clickable {
                scope.launch { viewModel.setOfflineMode(!offlineModeEnabled) }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        
        // Storage Info
        SettingsItem(
            icon = Icons.Default.Storage,
            title = "Storage",
            subtitle = "Manage downloads and clear cache",
            onClick = onStorageClick
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Statistics
        SettingsItem(
            icon = Icons.Default.Info,
            title = "Listening Statistics",
            subtitle = "View your listening history and stats",
            onClick = onStatsClick
        )
        
        // About Section
        SectionTitle("About")
        
        SettingsItem(
            icon = Icons.Default.Info,
            title = "About SuvMusic",
            subtitle = "Version, credits & more",
            onClick = onAboutClick
        )
        
        // Check for Updates
        SettingsItem(
            icon = Icons.Default.SystemUpdate,
            title = "Check for Updates",
            subtitle = "Current version: ${uiState.currentVersion}",
            onClick = { viewModel.checkForUpdates() }
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
    
    // Update Dialogs
    when (val updateState = uiState.updateState) {
        is UpdateState.Checking -> {
            CheckingUpdateDialog()
        }
        is UpdateState.UpdateAvailable -> {
            UpdateAvailableDialog(
                update = updateState.update,
                currentVersion = uiState.currentVersion,
                onDownload = { 
                    viewModel.downloadUpdate(
                        downloadUrl = updateState.update.downloadUrl,
                        versionName = updateState.update.versionName
                    )
                },
                onDismiss = { viewModel.resetUpdateState() }
            )
        }
        is UpdateState.NoUpdate -> {
            NoUpdateDialog(
                currentVersion = uiState.currentVersion,
                onDismiss = { viewModel.resetUpdateState() }
            )
        }
        is UpdateState.Downloading -> {
            DownloadProgressDialog(
                progress = updateState.progress,
                onCancel = { viewModel.cancelDownload() }
            )
        }
        is UpdateState.Error -> {
            UpdateErrorDialog(
                errorMessage = updateState.message,
                onRetry = { viewModel.checkForUpdates() },
                onDismiss = { viewModel.resetUpdateState() }
            )
        }
        is UpdateState.Downloaded, is UpdateState.Idle -> {
            // No dialog needed
        }
    }
}

private fun buildPlaybackSubtitle(gapless: Boolean, automix: Boolean): String {
    val parts = mutableListOf<String>()
    if (gapless) parts.add("Gapless playback")
    if (automix) parts.add("Automix")
    return if (parts.isEmpty()) "Configure playback settings" else parts.joinToString(" • ")
}

private fun buildAppearanceSubtitle(themeLabel: String, dynamicColor: Boolean): String {
    return if (dynamicColor) "$themeLabel • Dynamic colors" else themeLabel
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

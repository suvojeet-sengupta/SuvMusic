package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Settings screen with Material 3 design and organized categories.
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
    onSupportClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAccountsDialog by remember { mutableStateOf(false) }
    
    // Floating Player
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val floatingPlayerEnabled by viewModel.dynamicIslandEnabled.collectAsState(initial = false)
    val offlineModeEnabled by viewModel.offlineModeEnabled.collectAsState(initial = false)

    // Background Gradient (Subtle Premium Feel)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp)
            ) {
                item {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp, bottom = 16.dp)
                    )
                }

                // --- Account Section ---
                item {
                    SettingsSectionTitle("Account")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (uiState.isLoggedIn) {
                            // User Info
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = uiState.storedAccounts.firstOrNull { it.email == "current" }?.name ?: "Signed In", // Fallback if name not available directly
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = "YouTube Music Connected",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingContent = {
                                    if (uiState.userAvatarUrl != null) {
                                        AsyncImage(
                                            model = uiState.userAvatarUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(56.dp)
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
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            
                            HorizontalDivider()

                            // Account Actions
                            SettingsActionItem(
                                icon = Icons.Default.SwitchAccount,
                                title = "Switch Account",
                                onClick = { showAccountsDialog = true }
                            )
                            
                            SettingsActionItem(
                                icon = Icons.AutoMirrored.Filled.Logout,
                                title = "Sign Out",
                                titleColor = MaterialTheme.colorScheme.error,
                                iconColor = MaterialTheme.colorScheme.error,
                                onClick = { showSignOutDialog = true }
                            )
                        } else {
                            // Sign In Prompt
                            ListItem(
                                headlineContent = { Text("Sign in to YouTube Music", fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("Sync playlists and library") },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                trailingContent = {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.clickable(onClick = onLoginClick),
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- General Section ---
                item {
                    SettingsSectionTitle("General")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsSwitchItem(
                            icon = Icons.Default.WifiOff,
                            title = "Offline Mode",
                            subtitle = "Only play downloaded songs",
                            checked = offlineModeEnabled,
                            onCheckedChange = { scope.launch { viewModel.setOfflineMode(it) } }
                        )
                        
                        HorizontalDivider()
                        
                        SettingsSwitchItem(
                            icon = Icons.Default.Smartphone,
                            title = "Floating Player",
                            subtitle = "Mini player over other apps",
                            checked = floatingPlayerEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (DynamicIslandService.hasOverlayPermission(context)) {
                                        scope.launch { viewModel.setDynamicIslandEnabled(true) }
                                    } else {
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
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- Player & Audio ---
                item {
                    SettingsSectionTitle("Player & Audio")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsNavigationItem(
                            icon = Icons.Default.GraphicEq,
                            title = "Playback",
                            subtitle = "Audio quality, gapless, equalizer",
                            onClick = onPlaybackClick
                        )
                        
                        HorizontalDivider()
                        
                        SettingsNavigationItem(
                            icon = Icons.Default.Tune,
                            title = "Customization",
                            subtitle = "Player UI, artwork style",
                            onClick = onCustomizationClick
                        )
                        
                        HorizontalDivider()
                        
                        SettingsNavigationItem(
                            icon = Icons.Default.DarkMode,
                            title = "Appearance",
                            subtitle = "Theme, dynamic colors",
                            onClick = onAppearanceClick
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- Content (Lyrics) ---
                item {
                    SettingsSectionTitle("Content")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Header for Lyrics
                        ListItem(
                            headlineContent = { 
                                Text(
                                    "Lyrics Providers",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Default.Lyrics, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        
                        SettingsSwitchItem(
                            icon = null, // Indented
                            title = "BetterLyrics (Apple Music)",
                            subtitle = "Time-synced lyrics database",
                            checked = uiState.betterLyricsEnabled,
                            onCheckedChange = { viewModel.setBetterLyricsEnabled(it) },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        
                        SettingsSwitchItem(
                            icon = null, // Indented
                            title = "SimpMusic",
                            subtitle = "Community sourced lyrics",
                            checked = uiState.simpMusicEnabled,
                            onCheckedChange = { viewModel.setSimpMusicEnabled(it) },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- Storage & Data ---
                item {
                    SettingsSectionTitle("Storage & Data")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsNavigationItem(
                            icon = Icons.Default.Storage,
                            title = "Storage Manager",
                            subtitle = "Manage downloads & cache",
                            onClick = onStorageClick
                        )
                        
                        HorizontalDivider()
                        
                        SettingsNavigationItem(
                            icon = Icons.Default.Info,
                            title = "Listening Stats",
                            subtitle = "Your music habits",
                            onClick = onStatsClick
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- Support & About ---
                item {
                    SettingsSectionTitle("About & Support")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsNavigationItem(
                            icon = Icons.Default.HeadsetMic,
                            title = "Help & Support",
                            subtitle = "FAQ, contact us",
                            onClick = onSupportClick
                        )
                        
                        HorizontalDivider()
                        
                        SettingsNavigationItem(
                            icon = Icons.Default.Album,
                            title = "About SuvMusic",
                            subtitle = "Version ${uiState.currentVersion}",
                            onClick = onAboutClick
                        )
                        
                        HorizontalDivider()
                        
                        SettingsNavigationItem(
                            icon = Icons.Default.SystemUpdate,
                            title = "Check for Updates",
                            subtitle = if (uiState.updateState is UpdateState.Checking) "Checking..." else "Tap to check",
                            onClick = { viewModel.checkForUpdates() }
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // --- Dialogs ---
    
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
            title = { Text(text = "Sign out?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "You will be disconnected from the current account.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                        android.webkit.CookieManager.getInstance().flush()
                        viewModel.prepareAddAccount()
                        showSignOutDialog = false
                    }
                ) {
                    Text(text = "Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // Accounts Dialog
    if (showAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showAccountsDialog = false },
            icon = { Icon(Icons.Default.SwitchAccount, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Switch Account") },
            text = {
                Column {
                    Text("Select an account:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (uiState.storedAccounts.isNotEmpty()) {
                        uiState.storedAccounts.forEach { account ->
                            ListItem(
                                headlineContent = { Text(account.name, maxLines = 1) },
                                supportingContent = { Text(account.email, maxLines = 1) },
                                leadingContent = {
                                    AsyncImage(
                                        model = account.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(CircleShape)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.switchAccount(account)
                                        showAccountsDialog = false
                                    }
                                    .padding(vertical = 4.dp),
                                trailingContent = {
                                    Icon(
                                        Icons.Default.Close, 
                                        "Remove", 
                                        modifier = Modifier.clickable { viewModel.removeAccount(account.email) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    TextButton(
                        onClick = {
                            viewModel.prepareAddAccount()
                            showAccountsDialog = false
                            onLoginClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add another account")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAccountsDialog = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
    
    // Update Dialogs
    when (val updateState = uiState.updateState) {
        is UpdateState.Checking -> CheckingUpdateDialog()
        is UpdateState.UpdateAvailable -> UpdateAvailableDialog(
            update = updateState.update,
            currentVersion = uiState.currentVersion,
            onDownload = { viewModel.downloadUpdate(updateState.update.downloadUrl, updateState.update.versionName) },
            onDismiss = { viewModel.resetUpdateState() }
        )
        is UpdateState.NoUpdate -> NoUpdateDialog(
            currentVersion = uiState.currentVersion,
            onDismiss = { viewModel.resetUpdateState() }
        )
        is UpdateState.Downloading -> DownloadProgressDialog(
            progress = updateState.progress,
            onCancel = { viewModel.cancelDownload() }
        )
        is UpdateState.Error -> UpdateErrorDialog(
            errorMessage = updateState.message,
            onRetry = { viewModel.checkForUpdates() },
            onDismiss = { viewModel.resetUpdateState() }
        )
        else -> {}
    }
}

// --- Components ---

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        tonalElevation = 0.dp
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
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    )
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium, color = titleColor) },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor)
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = icon?.let { 
            { Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        modifier = modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

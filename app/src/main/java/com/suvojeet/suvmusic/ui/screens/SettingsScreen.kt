package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import com.suvojeet.suvmusic.R
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
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
@OptIn(ExperimentalMaterial3Api::class)
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
    onAboutClick: () -> Unit = {},
    onMiscClick: () -> Unit = {},
    onSponsorBlockClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {},
    onLastFmClick: () -> Unit = {},
    onDiscordClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAccountsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    var showUpdateChannelDialog by remember { mutableStateOf(false) }
    
    // Floating Player
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val floatingPlayerEnabled by viewModel.dynamicIslandEnabled.collectAsState(initial = false)
    val offlineModeEnabled by viewModel.offlineModeEnabled.collectAsState(initial = false)
    val sponsorBlockEnabled by viewModel.sponsorBlockEnabled.collectAsState(initial = true)

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
// ... (omitted for brevity, context is maintained by line numbers in tool but since I'm implementing replacing the whole function signature/start and the button logic, I need to be careful)
// Actually the previous tool calls suggested I can use multi-replace or just replace chunks.
// The chunk above is too large and risky.

// Let's do it in two chunks.
// 1. Signature update
// 2. Button update

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
                                onClick = { showAccountsSheet = true }
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

                // --- Appearance Section ---
                item {
                    SettingsSectionTitle("Appearance")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsNavigationItem(
                            icon = Icons.Default.DarkMode,
                            title = "Appearance",
                            subtitle = "Theme, dark mode, colors",
                            onClick = onAppearanceClick
                        )
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
                            icon = Icons.Default.Warning, // Or Icons.Default.Security or Icons.Default.VisibilityOff
                            title = "Privacy Mode",
                            subtitle = "Stop history & activity sharing",
                            checked = uiState.privacyModeEnabled,
                            onCheckedChange = { viewModel.setPrivacyModeEnabled(it) }
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


                // --- Bluetooth Section ---
                item {
                    SettingsSectionTitle("Bluetooth")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsSwitchItem(
                            icon = Icons.Default.HeadsetMic,
                            title = "Bluetooth Autoplay",
                            subtitle = "Resume when connecting to devices",
                            checked = uiState.bluetoothAutoplayEnabled,
                            onCheckedChange = { scope.launch { viewModel.setBluetoothAutoplayEnabled(it) } }
                        )

                        HorizontalDivider()

                        SettingsSwitchItem(
                            icon = Icons.Default.Lyrics,
                            title = "Announce Songs",
                            subtitle = "Speak title when song changes (TTS)",
                            checked = uiState.speakSongDetailsEnabled,
                            onCheckedChange = { scope.launch { viewModel.setSpeakSongDetailsEnabled(it) } }
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
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- Integrations ---
                item {
                    SettingsSectionTitle("Integrations")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsSwitchItem(
                            icon = Icons.Default.FastForward,
                            title = "Enable SponsorBlock",
                            subtitle = "Automatically skip non-music segments",
                            checked = sponsorBlockEnabled,
                            onCheckedChange = { scope.launch { viewModel.setSponsorBlockEnabled(it) } }
                        )
                        
                        HorizontalDivider()
                        SettingsNavigationItem(
                            icon = Icons.Default.FastForward,
                            title = "SponsorBlock",
                            subtitle = "Skip non-music segments",
                            onClick = onSponsorBlockClick
                        )

                        HorizontalDivider()

                        // Last.fm Integration
                        val isLastFmConnected = uiState.lastFmUsername != null
                        SettingsNavigationItem(
                            icon = Icons.Default.MusicNote,
                            title = "Last.fm",
                            subtitle = if (isLastFmConnected) "Connected as ${uiState.lastFmUsername}" else "Scrobble your music hits",
                            onClick = onLastFmClick
                        )

                        HorizontalDivider()

                        val isDiscordConnected = uiState.discordToken.isNotBlank()
                        SettingsNavigationItem(
                            icon = Icons.Default.GraphicEq,
                            title = "Discord RPC",
                            subtitle = if (isDiscordConnected) "Connected" else "Connect your Discord",
                            onClick = onDiscordClick
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- Misc Section ---
                item {
                    SettingsSectionTitle("Misc")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsNavigationItem(
                            icon = Icons.Default.Tune, // Using Tune or similar generic icon
                            title = "Misc Settings",
                            subtitle = "Other settings",
                            onClick = onMiscClick
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
                            icon = Icons.Default.Favorite,
                            title = "Support Project",
                            subtitle = "Your support keeps SuvMusic alive! 💖",
                            onClick = onSupportClick
                        )
                        
                        HorizontalDivider()

                         SettingsNavigationItem(
                            icon = Icons.Default.Person,
                            title = "Credits",
                            subtitle = "Developers & Libraries",
                            onClick = onCreditsClick
                        )

                        HorizontalDivider()
                        
                        SettingsNavigationItem(
                            icon = Icons.Default.Album,
                            title = "About SuvMusic",
                            subtitle = "Version ${uiState.currentVersion}",
                            onClick = onAboutClick
                        )
                        
                        HorizontalDivider()
                        
                        // Update Channel
                        SettingsNavigationItem(
                            icon = Icons.Default.Tune, // Or another relevant icon like Build
                            title = "Update Channel",
                            subtitle = uiState.updateChannel.name.lowercase().replaceFirstChar { it.uppercase() },
                            onClick = { showUpdateChannelDialog = true }
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

    // Accounts Bottom Sheet
    if (showAccountsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp) // Add padding for navigation bar/gesture area
            ) {
                Text(
                    text = "Switch Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                if (uiState.storedAccounts.isNotEmpty()) {
                    uiState.storedAccounts.forEach { account ->
                        val isCurrent = account.email == (uiState.storedAccounts.firstOrNull { it.email == "current" }?.email ?: "")
                        // Note: current detection might need better logic if "current" isn't explicitly marked in list,
                        // usually the first one or we match cookies.
                        // Assuming list contains all saved accounts.
                        
                        ListItem(
                            headlineContent = { 
                                Text(
                                    account.name, 
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal 
                                )
                            },
                            supportingContent = { Text(account.email) },
                            leadingContent = {
                                AsyncImage(
                                    model = account.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(
                                            if (isCurrent) 2.dp else 0.dp, 
                                            MaterialTheme.colorScheme.primary, 
                                            CircleShape
                                        )
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Default.Close,
                                    "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable { viewModel.removeAccount(account.email) }
                                        .padding(8.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchAccount(account)
                                    // Scope needed to hide sheet properly
                                    scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                        showAccountsSheet = false 
                                    }
                                }
                                .padding(horizontal = 8.dp), // Inner padding
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                ListItem(
                    headlineContent = { Text("Add another account", fontWeight = FontWeight.Medium) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Person, 
                                null, 
                                modifier = Modifier.padding(10.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Fix: Don't logout immediately! Only clear WebView cookies.
                            viewModel.clearWebViewCookies()
                            
                            scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                showAccountsSheet = false 
                                onLoginClick()
                            }
                        }
                        .padding(horizontal = 8.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    // Update Channel Dialog
    if (showUpdateChannelDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateChannelDialog = false },
            title = { Text("Select Update Channel") },
            text = {
                Column {
                    com.suvojeet.suvmusic.data.model.UpdateChannel.entries.forEach { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setUpdateChannel(channel)
                                    showUpdateChannelDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = uiState.updateChannel == channel,
                                onClick = {
                                    viewModel.setUpdateChannel(channel)
                                    showUpdateChannelDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = channel.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (channel == com.suvojeet.suvmusic.data.model.UpdateChannel.NIGHTLY) {
                                    Text(
                                        text = "Get the latest features (potentially unstable)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUpdateChannelDialog = false }) { Text("Cancel") }
            },
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

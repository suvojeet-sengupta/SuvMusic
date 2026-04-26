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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
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
import org.koin.compose.viewmodel.koinViewModel
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.data.model.UpdateChannel
import com.suvojeet.suvmusic.updater.UpdateViewModel
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.components.BetaBadge
import com.suvojeet.suvmusic.util.dpadFocusable
import kotlinx.coroutines.launch

/**
 * Settings screen with Material 3 Expressive design and organized categories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
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
    onDiscordClick: () -> Unit = {},
    onAISettingsClick: () -> Unit = {},
    onUpdaterClick: () -> Unit = {}
    )
 {
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAccountsSheet by remember { mutableStateOf(false) }
    var showUpdateChannelSheet by remember { mutableStateOf(false) }
    var bugDescription by remember { mutableStateOf("") }
    var showBugDescriptionDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // Floating Player
    val scope = rememberCoroutineScope()
    val floatingPlayerEnabled by viewModel.dynamicIslandEnabled.collectAsState(initial = false)
    val offlineModeEnabled by viewModel.offlineModeEnabled.collectAsState(initial = false)
    val sponsorBlockEnabled by viewModel.sponsorBlockEnabled.collectAsState(initial = true)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
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
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (uiState.isLoggedIn) {
                        // User Info
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = uiState.userName ?: "Signed In",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.5).sp
                                )
                            },
                            supportingContent = {
                                val email = uiState.storedAccounts.firstOrNull()?.email
                                if (email != null) {
                                    Text(
                                        text = email,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "YouTube Music Connected",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            leadingContent = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (uiState.userAvatarUrl != null) {
                                        AsyncImage(
                                            model = uiState.userAvatarUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(SquircleShape)
                                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), SquircleShape)
                                        )
                                    } else {
                                        Surface(
                                            shape = SquircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        HorizontalDivider()

                        // Account Actions
                        SettingsActionItem(
                            icon = Icons.Default.SwitchAccount,
                            title = "Switch Account",
                            onClick = { 
                                viewModel.fetchAvailableAccounts()
                                showAccountsSheet = true 
                            }
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
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(SquircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier
                                .dpadFocusable(onClick = onLoginClick, shape = SquircleShape)
                                .clip(SquircleShape),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Appearance Section ---
            item {
                SettingsSectionTitle("Appearance")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.WifiOff,
                        title = "Offline Mode",
                        subtitle = "Only play downloaded songs",
                        checked = offlineModeEnabled,
                        onCheckedChange = { scope.launch { viewModel.setOfflineMode(it) } }
                    )
                    
                    HorizontalDivider()

                    SettingsSwitchItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Privacy Mode",
                        subtitle = "Stop history & activity sharing",
                        checked = uiState.privacyModeEnabled,
                        onCheckedChange = { viewModel.setPrivacyModeEnabled(it) }
                    )
                    
                    HorizontalDivider()
                    
                    SettingsSwitchItem(
                        icon = Icons.Default.PictureInPicture,
                        title = "Picture-in-Picture",
                        subtitle = "Show mini player when backgrounded",
                        checked = floatingPlayerEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { viewModel.setDynamicIslandEnabled(enabled) }
                        }
                    )

                    HorizontalDivider()

                    SettingsSwitchItem(
                        icon = Icons.Default.Warning,
                        title = "Crash Reporting & Logging",
                        subtitle = "Help developer fix issues by sharing logs",
                        checked = uiState.loggingEnabled,
                        onCheckedChange = { viewModel.setLoggingEnabled(it) }
                    )
                    
                    if (uiState.loggingEnabled) {
                        HorizontalDivider()
                        SettingsActionItem(
                            icon = Icons.Default.Info,
                            title = "Share App Logs",
                            onClick = { viewModel.sharePersistentLogs() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Bug Reproduction Section ---
            item {
                SettingsSectionTitle("Bug Reproduction")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("SuvMusic Session", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { 
                            Text(
                                if (uiState.isBugReportingSessionActive) "Recording logs... Reproduce the bug now" 
                                else "Start a session to capture logs for debugging"
                            ) 
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(SquircleShape)
                                    .background(
                                        if (uiState.isBugReportingSessionActive) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isBugReportingSessionActive) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (uiState.isBugReportingSessionActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    if (uiState.isBugReportingSessionActive) {
                                        viewModel.stopBugReportingSession { file ->
                                            viewModel.shareBugReport(file)
                                        }
                                    } else {
                                        bugDescription = ""
                                        showBugDescriptionDialog = true
                                    }
                                },
                                colors = if (uiState.isBugReportingSessionActive) {
                                    androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                } else {
                                    androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                }
                            ) {
                                Text(if (uiState.isBugReportingSessionActive) "Stop & Share" else "Start")
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Bluetooth Section ---
            item {
                SettingsSectionTitle("Bluetooth")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                        icon = Icons.Default.AutoAwesome,
                        title = "AI Assistant",
                        subtitle = "OpenAI, Anthropic, Gemini config",
                        badge = { BetaBadge() },
                        onClick = onAISettingsClick
                    )
                    }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Integrations ---
            item {
                SettingsSectionTitle("Integrations")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.FastForward,
                        title = "Enable SponsorBlock",
                        subtitle = "Automatically skip non-music segments",
                        checked = sponsorBlockEnabled,
                        onCheckedChange = { scope.launch { viewModel.setSponsorBlockEnabled(it) } }
                    )
                    
                    if (sponsorBlockEnabled) {
                        HorizontalDivider()
                        SettingsNavigationItem(
                            icon = Icons.Default.FastForward,
                            title = "SponsorBlock Settings",
                            subtitle = "Configure segment types to skip",
                            onClick = onSponsorBlockClick
                        )
                    }

                    HorizontalDivider()

                    // Last.fm Integration
                    val isLastFmConnected = uiState.lastFmUsername != null
                    SettingsNavigationItem(
                        icon = Icons.Default.MusicNote,
                        title = "Last.fm",
                        subtitle = if (isLastFmConnected) "Connected as ${uiState.lastFmUsername}" else "Scrobble your music hits",
                        onClick = onLastFmClick
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Misc Section ---
            item {
                SettingsSectionTitle("Misc")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsNavigationItem(
                        icon = Icons.Default.Tune,
                        title = "Misc Settings",
                        subtitle = "Advanced & experimental features",
                        onClick = onMiscClick
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Storage & Data ---
            item {
                SettingsSectionTitle("Storage & Data")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    SettingsNavigationItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "How SuvMusic handles your data",
                        onClick = { uriHandler.openUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/suvmusic-privacy.html") }
                    )

                    HorizontalDivider()

                    SettingsNavigationItem(
                        icon = Icons.Default.SystemUpdate,
                        title = "Update Channel",
                        subtitle = uiState.updateChannel.label,
                        onClick = { showUpdateChannelSheet = true }
                    )

                    HorizontalDivider()

                    SettingsNavigationItem(
                        icon = Icons.Default.SystemUpdate,
                        title = "Check for Updates",
                        subtitle = "Check for app updates and changelogs",
                        onClick = onUpdaterClick
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = SquircleShape
        )
    }

    // Accounts Bottom Sheet
    if (showAccountsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Switch Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                if (uiState.availableAccounts.isNotEmpty()) {
                    Text(
                        text = "Channels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    
                    uiState.availableAccounts.forEach { account ->
                        val isCurrent = account.authUserIndex == (uiState.storedAccounts.firstOrNull { it.email == "current" }?.authUserIndex ?: 0) &&
                                        account.name == (uiState.storedAccounts.firstOrNull { it.email == "current" }?.name ?: "")
                        
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
                                        .clip(SquircleShape)
                                        .border(
                                            if (isCurrent) 1.5.dp else 0.dp, 
                                            MaterialTheme.colorScheme.primary, 
                                            SquircleShape
                                        )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .dpadFocusable(
                                    onClick = {
                                        viewModel.switchAccount(account)
                                        scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                            showAccountsSheet = false 
                                        }
                                    },
                                    shape = SquircleShape
                                )
                                .padding(horizontal = 8.dp),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    HorizontalDivider()
                }

                if (uiState.storedAccounts.isNotEmpty()) {
                    Text(
                        text = "Saved Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    uiState.storedAccounts.forEach { account ->
                        val isCurrent = account.email == (uiState.storedAccounts.firstOrNull { it.email == "current" }?.email ?: "")
                        
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
                                        .clip(SquircleShape)
                                        .border(
                                            if (isCurrent) 1.5.dp else 0.dp, 
                                            MaterialTheme.colorScheme.primary, 
                                            SquircleShape
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
                                .dpadFocusable(
                                    onClick = {
                                        viewModel.switchAccount(account)
                                        scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                            showAccountsSheet = false 
                                        }
                                    },
                                    shape = SquircleShape
                                )
                                .padding(horizontal = 8.dp),
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
                            shape = SquircleShape,
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
                        .dpadFocusable(
                            onClick = {
                                viewModel.clearWebViewCookies()
                                scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                    showAccountsSheet = false 
                                    onLoginClick()
                                }
                            },
                            shape = SquircleShape
                        )
                        .padding(horizontal = 8.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    // Bug Description Dialog
    if (showBugDescriptionDialog) {
        AlertDialog(
            onDismissRequest = { showBugDescriptionDialog = false },
            title = { Text("Report an Issue") },
            text = {
                Column {
                    Text(
                        text = "Briefly describe what's wrong. This will be included in the log file.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = bugDescription,
                        onValueChange = { bugDescription = it },
                        placeholder = { Text("e.g. App crashes when I skip songs...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        shape = SquircleShape
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.startBugReportingSession(bugDescription)
                        showBugDescriptionDialog = false
                    }
                ) {
                    Text("Start Recording")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBugDescriptionDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = SquircleShape
        )
    }

    // Update Channel Bottom Sheet
    if (showUpdateChannelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUpdateChannelSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Update Channel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                UpdateChannel.entries.forEach { channel ->
                    val isSelected = uiState.updateChannel == channel
                    ListItem(
                        headlineContent = { Text(channel.label) },
                        leadingContent = {
                            androidx.compose.material3.RadioButton(
                                selected = isSelected,
                                onClick = { 
                                    viewModel.setUpdateChannel(channel)
                                    showUpdateChannelSheet = false
                                }
                            )
                        },
                        modifier = Modifier.dpadFocusable(
                            onClick = { 
                                viewModel.setUpdateChannel(channel)
                                showUpdateChannelSheet = false
                            },
                            shape = SquircleShape
                        ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
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
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Medium)
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    badge()
                }
            }
        },
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
                    tint = if (titleColor == MaterialTheme.colorScheme.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = Modifier
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .clip(SquircleShape),
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
            { 
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SquircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
        modifier = modifier
            .dpadFocusable(onClick = { onCheckedChange(!checked) }, shape = SquircleShape)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
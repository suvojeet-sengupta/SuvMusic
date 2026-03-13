package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import com.suvojeet.suvmusic.R
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.data.model.UpdateChannel
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.core.ui.components.*
import com.suvojeet.suvmusic.updater.UpdateViewModel
import kotlinx.coroutines.launch

/**
 * Settings screen with Material 3 Expressive design.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
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
    onUpdaterClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAccountsSheet by remember { mutableStateOf(false) }
    var showBugDescriptionDialog by remember { mutableStateOf(false) }
    var showUpdateChannelSheet by remember { mutableStateOf(false) }
    var bugDescription by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    
    val offlineModeEnabled by viewModel.offlineModeEnabled.collectAsState(initial = false)
    val sponsorBlockEnabled by viewModel.sponsorBlockEnabled.collectAsState(initial = true)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        style = MaterialTheme.typography.headlineMediumEmphasized 
                    ) 
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding()
                )
                .consumeWindowInsets(paddingValues),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp)
        ) {
            // Account Card
            item {
                M3EAccountCard(
                    accountName = uiState.userName,
                    accountEmail = uiState.storedAccounts.firstOrNull()?.email,
                    accountPhotoUrl = uiState.userAvatarUrl,
                    isLoggedIn = uiState.isLoggedIn,
                    onLoginClick = onLoginClick,
                    onLogoutClick = { showSignOutDialog = true },
                    onSwitchAccount = { 
                        viewModel.fetchAvailableAccounts()
                        showAccountsSheet = true 
                    }
                )
            }

            // AUDIO Section
            item { M3ESettingsGroupHeader("AUDIO") }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Playback",
                    subtitle = "Quality, gapless, speed",
                    onClick = onPlaybackClick
                )
            }

            // APPEARANCE Section
            item { M3ESettingsGroupHeader("APPEARANCE") }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    subtitle = "Themes, dark mode",
                    onClick = onAppearanceClick
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Tune,
                    title = "Customization",
                    subtitle = "Player layout, artwork",
                    onClick = onCustomizationClick
                )
            }

            // SERVICES Section
            item { M3ESettingsGroupHeader("SERVICES") }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.MusicNote,
                    title = "Lyrics Providers",
                    onClick = {} // Placeholder for Lyrics Providers
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Block,
                    title = "SponsorBlock",
                    onClick = onSponsorBlockClick
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.GraphicEq, // Discord icon placeholder
                    title = "Discord RPC",
                    onClick = onDiscordClick
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.MusicNote, // Last.fm icon placeholder
                    title = "Last.fm Scrobbling",
                    onClick = onLastFmClick
                )
            }

            // STORAGE & CACHE Section
            item { M3ESettingsGroupHeader("STORAGE & CACHE") }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Storage,
                    title = "Storage",
                    subtitle = "Downloads, cache",
                    onClick = onStorageClick
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Memory,
                    title = "Player Cache",
                    onClick = {} // Placeholder
                )
            }

            // MISC Section
            item { M3ESettingsGroupHeader("MISC") }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.WifiOff,
                    title = "Offline Mode",
                    checked = offlineModeEnabled,
                    onCheckedChange = { scope.launch { viewModel.setOfflineMode(it) } }
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Notes,
                    title = "Misc Settings",
                    onClick = onMiscClick
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.BugReport,
                    title = "Report a Bug",
                    onClick = { showBugDescriptionDialog = true }
                )
            }

            // INFO Section
            item { M3ESettingsGroupHeader("INFO") }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "Check for Updates",
                    badge = uiState.currentVersion,
                    onClick = onUpdaterClick
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    onClick = onAboutClick
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Album,
                    title = "Changelog",
                    onClick = {} // Placeholder
                )
            }
        }
    }

    // Dialogs and Sheets (Keeping original logic for functionality)
    
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?", style = MaterialTheme.typography.titleLargeEmphasized) },
            text = { Text("You will be disconnected from the current account.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                        android.webkit.CookieManager.getInstance().flush()
                        viewModel.prepareAddAccount()
                        showSignOutDialog = false
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAccountsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountsSheet = false },
            sheetState = sheetState,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    text = "Switch Account",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                if (uiState.availableAccounts.isNotEmpty()) {
                    uiState.availableAccounts.forEach { account ->
                        val isCurrent = account.authUserIndex == (uiState.storedAccounts.firstOrNull { it.email == "current" }?.authUserIndex ?: 0)
                        ListItem(
                            headlineContent = { Text(account.name, style = if (isCurrent) MaterialTheme.typography.bodyLargeEmphasized else MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text(account.email) },
                            leadingContent = {
                                AsyncImage(
                                    model = account.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.switchAccount(account)
                                scope.launch { sheetState.hide() }.invokeOnCompletion { showAccountsSheet = false }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    if (showBugDescriptionDialog) {
        AlertDialog(
            onDismissRequest = { showBugDescriptionDialog = false },
            title = { Text("Report an Issue", style = MaterialTheme.typography.titleLargeEmphasized) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = bugDescription,
                    onValueChange = { bugDescription = it },
                    placeholder = { Text("Describe what's wrong...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.startBugReportingSession(bugDescription)
                        showBugDescriptionDialog = false
                    }
                ) { Text("Start Recording") }
            },
            dismissButton = {
                TextButton(onClick = { showBugDescriptionDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3EAccountCard(
    accountName: String?,
    accountEmail: String?,
    accountPhotoUrl: String?,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSwitchAccount: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "account_card_scale"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource, indication = null) { /* Handle whole card click if needed */ },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoggedIn && accountPhotoUrl != null) {
                AsyncImage(
                    model = accountPhotoUrl,
                    contentDescription = "Account avatar",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                }
            }
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) accountName ?: "YouTube Account" else "Not signed in",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isLoggedIn && accountEmail != null) accountEmail else "Sign in to sync your library",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoggedIn) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onSwitchAccount, 
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Switch", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick = onLogoutClick, 
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Sign out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                FilledTonalButton(onClick = onLoginClick, shape = MaterialTheme.shapes.medium) {
                    Text("Sign in", style = MaterialTheme.typography.labelMediumEmphasized)
                }
            }
        }
    }
}

package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider

import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.suvojeet.suvmusic.ui.viewmodel.BackupViewModel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscScreen(
    onBack: () -> Unit,
    onLyricsProvidersClick: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    backupViewModel: BackupViewModel = koinViewModel(),
    externalSnackbarHostState: SnackbarHostState? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val backupState by backupViewModel.uiState.collectAsState()
    val snackbarHostState = externalSnackbarHostState ?: remember { SnackbarHostState() }
    // Bug-reporting session dialog state (moved here from the main Settings screen).
    var bugDescription by remember { mutableStateOf("") }
    var showBugDescriptionDialog by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        uri?.let { backupViewModel.createBackup(it) }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { backupViewModel.restoreBackup(it) }
    }

    LaunchedEffect(backupState.successMessage) {
        backupState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            backupViewModel.clearMessages()
        }
    }

    LaunchedEffect(backupState.lastError) {
        backupState.lastError?.let {
            snackbarHostState.showSnackbar(it)
            backupViewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { 
            // Only show local SnackbarHost if no external one is provided
            // Actually, if we use the external state, it will be shown by MainActivity's host
            if (externalSnackbarHostState == null) {
                SnackbarHost(snackbarHostState)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Advanced", fontWeight = FontWeight.Bold) },
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp)
        ) {
            item {
                SettingsSectionTitle("Diagnostics")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MiscSwitchItem(
                        icon = Icons.Default.Warning,
                        title = "Crash Reporting & Logging",
                        subtitle = "Help the developer fix issues by sharing logs",
                        checked = uiState.loggingEnabled,
                        onCheckedChange = { viewModel.setLoggingEnabled(it) }
                    )

                    if (uiState.loggingEnabled) {
                        HorizontalDivider()
                        MiscNavigationItem(
                            icon = Icons.Default.Info,
                            title = "Share App Logs",
                            subtitle = "Export the current log file",
                            onClick = { viewModel.sharePersistentLogs() }
                        )
                    }

                    HorizontalDivider()

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
                                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                } else {
                                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                }
                            ) {
                                Text(if (uiState.isBugReportingSessionActive) "Stop & Share" else "Start")
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionTitle("Advanced & Experimental")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MiscSwitchItem(
                        icon = Icons.Default.Notes,
                        title = "Stop music on task clear",
                        subtitle = "Kills the playback service when app is swiped away",
                        checked = uiState.stopMusicOnTaskClear,
                        onCheckedChange = { viewModel.setStopMusicOnTaskClear(it) }
                    )

                    HorizontalDivider()

                    MiscSwitchItem(
                        icon = Icons.Default.VolumeOff,
                        title = "Pause music when media is muted",
                        subtitle = "Pauses playback if volume becomes zero",
                        checked = uiState.pauseMusicOnMediaMuted,
                        onCheckedChange = { viewModel.setPauseMusicOnMediaMuted(it) }
                    )
                    
                    HorizontalDivider()

                    MiscSwitchItem(
                        icon = Icons.Default.Smartphone,
                        title = "Keep screen on when player is expanded",
                        subtitle = "Prevents device from sleeping while viewing player",
                        checked = uiState.keepScreenOn,
                        onCheckedChange = { viewModel.setKeepScreenOn(it) }
                    )
                    
                    HorizontalDivider()

                    MiscSwitchItem(
                        icon = Icons.Default.Notes,
                        title = "Filter local audio by duration",
                        subtitle = "Hide short audio files from your library",
                        checked = uiState.filterLocalByDurationEnabled,
                        onCheckedChange = { viewModel.setFilterLocalByDurationEnabled(it) }
                    )

                    if (uiState.filterLocalByDurationEnabled) {
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
                                    text = "Minimum Duration",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = if (uiState.localDurationFilterThreshold < 60) 
                                        "${uiState.localDurationFilterThreshold}s"
                                    else 
                                        "${uiState.localDurationFilterThreshold / 60}m ${uiState.localDurationFilterThreshold % 60}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = uiState.localDurationFilterThreshold.toFloat(),
                                onValueChange = { viewModel.setLocalDurationFilterThreshold(it.toInt()) },
                                valueRange = 0f..300f,
                                steps = 59
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    MiscNavigationItem(
                        icon = Icons.Default.Lyrics,
                        title = "Lyrics Providers",
                        subtitle = "Configure where to fetch lyrics from",
                        onClick = onLyricsProvidersClick
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionTitle("Backup & Restore")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MiscNavigationItem(
                        icon = Icons.Default.Save,
                        title = "Create Backup",
                        subtitle = if (backupState.isBackingUp) "Backing up..." else "Save library, history, and settings to .suv file",
                        onClick = { 
                            if (!backupState.isBackingUp) {
                                createBackupLauncher.launch("suvmusic_backup_${System.currentTimeMillis()}.suv")
                            }
                        }
                    )

                    HorizontalDivider()

                    MiscNavigationItem(
                        icon = Icons.Default.Restore,
                        title = "Restore Backup",
                        subtitle = if (backupState.isRestoring) "Restoring..." else "Restore your data from a .suv backup file",
                        onClick = { 
                            if (!backupState.isRestoring) {
                                restoreBackupLauncher.launch("*/*")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Bug Description Dialog (moved here from the main Settings screen).
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
}

@Composable
private fun SettingsSectionTitle(title: String) {
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
private fun MiscSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
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

@Composable
private fun MiscNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
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

package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.theme.SquircleShape

/**
 * Miscellaneous settings + Backup/Restore — ported from
 * `app/.../ui/screens/MiscScreen.kt` to commonMain.
 *
 * Differences vs the Android original:
 *   - Stateless props instead of injected ViewModels. The :app side keeps
 *     SettingsViewModel + BackupViewModel and feeds values into this composable.
 *   - Backup/restore Activity Result launchers (Android-only) become
 *     onCreateBackup / onRestoreBackup callbacks. The host opens the
 *     platform file picker and feeds the chosen URI/path back to its VM.
 *   - Snackbar handling moves to the host. backupBusyMessage gives the
 *     subtitle text shown while a backup or restore is in flight.
 *   - dpadFocusable replaced with plain Modifier.clickable (TV-input parity
 *     comes back when the focus utility is multiplatform).
 *   - No Scaffold/TopAppBar/SnackbarHost.
 */
@Composable
fun MiscScreen(
    stopMusicOnTaskClear: Boolean,
    onStopMusicOnTaskClearChange: (Boolean) -> Unit,
    pauseMusicOnMediaMuted: Boolean,
    onPauseMusicOnMediaMutedChange: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    filterLocalByDurationEnabled: Boolean,
    onFilterLocalByDurationEnabledChange: (Boolean) -> Unit,
    localDurationFilterThreshold: Int,
    onLocalDurationFilterThresholdChange: (Int) -> Unit,
    onLyricsProvidersClick: () -> Unit,
    isBackingUp: Boolean,
    isRestoring: Boolean,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 20.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            SectionTitle("Advanced & Experimental")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.Notes,
                    title = "Stop music on task clear",
                    subtitle = "Kills the playback service when app is swiped away",
                    checked = stopMusicOnTaskClear,
                    onCheckedChange = onStopMusicOnTaskClearChange,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.VolumeOff,
                    title = "Pause music when media is muted",
                    subtitle = "Pauses playback if volume becomes zero",
                    checked = pauseMusicOnMediaMuted,
                    onCheckedChange = onPauseMusicOnMediaMutedChange,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.Smartphone,
                    title = "Keep screen on when player is expanded",
                    subtitle = "Prevents device from sleeping while viewing player",
                    checked = keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.Notes,
                    title = "Filter local audio by duration",
                    subtitle = "Hide short audio files from your library",
                    checked = filterLocalByDurationEnabled,
                    onCheckedChange = onFilterLocalByDurationEnabledChange,
                )

                if (filterLocalByDurationEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Minimum Duration",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = if (localDurationFilterThreshold < 60) {
                                    "${localDurationFilterThreshold}s"
                                } else {
                                    "${localDurationFilterThreshold / 60}m ${localDurationFilterThreshold % 60}s"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Slider(
                            value = localDurationFilterThreshold.toFloat(),
                            onValueChange = { onLocalDurationFilterThresholdChange(it.toInt()) },
                            valueRange = 0f..300f,
                            steps = 59,
                        )
                    }
                }

                ThinDivider()
                NavigationRow(
                    icon = Icons.Default.Lyrics,
                    title = "Lyrics Providers",
                    subtitle = "Configure where to fetch lyrics from",
                    onClick = onLyricsProvidersClick,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("Backup & Restore")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                NavigationRow(
                    icon = Icons.Default.Save,
                    title = "Create Backup",
                    subtitle = if (isBackingUp) "Backing up..." else "Save library, history, and settings to .suv file",
                    onClick = { if (!isBackingUp) onCreateBackup() },
                )
                ThinDivider()
                NavigationRow(
                    icon = Icons.Default.Restore,
                    title = "Restore Backup",
                    subtitle = if (isRestoring) "Restoring..." else "Restore your data from a .suv backup file",
                    onClick = { if (!isRestoring) onRestoreBackup() },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
private fun ThinDivider() {
    M3HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { LeadingIconBox(icon) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { LeadingIconBox(icon) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun LeadingIconBox(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(SquircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

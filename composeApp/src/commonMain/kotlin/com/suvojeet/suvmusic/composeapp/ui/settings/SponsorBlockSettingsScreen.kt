package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import com.suvojeet.suvmusic.core.model.SponsorCategory

/**
 * SuvMusic SponsorBlock settings screen — ported from
 * `app/.../ui/screens/SponsorBlockSettingsScreen.kt` to commonMain so
 * Android and Desktop share the exact same surface.
 *
 * Differences vs the Android original (mirrors AboutScreen's port):
 *   - Stateless: takes (`isEnabled`, `enabledCategoryKeys`, callbacks)
 *     instead of a `SettingsViewModel`. The :app side keeps a thin
 *     wrapper that owns the VM and feeds state into this composable
 *     so the migration doesn't ripple into Hilt/Koin/DataStore yet.
 *   - No Scaffold + TopAppBar — the host (Android nav graph or Desktop
 *     window) already provides chrome; double-bars look broken on both
 *     platforms.
 *   - `dpadFocusable` (TV-remote helper, Android-only) replaced with a
 *     plain `Modifier.clickable`. TV input parity returns when the
 *     focus utility is multiplatform.
 */
@Composable
fun SponsorBlockSettingsScreen(
    isMasterEnabled: Boolean,
    enabledCategoryKeys: Set<String>,
    onMasterToggle: (Boolean) -> Unit,
    onCategoryToggle: (categoryKey: String, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 20.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            SectionTitle("General")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.FastForward,
                    title = "Enable SponsorBlock",
                    subtitle = "Skip non-music segments automatically",
                    checked = isMasterEnabled,
                    onCheckedChange = onMasterToggle,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isMasterEnabled) {
            item {
                SectionTitle("Categories to Skip")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SponsorCategory.entries.forEachIndexed { index, category ->
                        val isChecked = enabledCategoryKeys.contains(category.key)
                        CategoryRow(
                            category = category,
                            isChecked = isChecked,
                            isEnabled = isMasterEnabled,
                            onCheckedChange = { checked ->
                                onCategoryToggle(category.key, checked)
                            },
                        )
                        if (index < SponsorCategory.entries.lastIndex) {
                            ThinDivider()
                        }
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
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            content()
        }
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
        leadingContent = {
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
        },
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
private fun CategoryRow(
    category: SponsorCategory,
    isChecked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(category.displayName, fontWeight = FontWeight.Medium) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(category.color, CircleShape),
                )
            }
        },
        trailingContent = {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                enabled = isEnabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        modifier = Modifier
            .then(
                if (isEnabled) {
                    Modifier.clickable { onCheckedChange(!isChecked) }
                } else {
                    Modifier
                }
            )
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable

/**
 * Visual treatment of a settings row's leading icon and its interaction affordance.
 *
 * [Boxed] renders the icon inside a tinted squircle container and uses D-pad focus handling.
 * [Plain] renders a bare icon and uses a plain clickable, matching the account-linking screens.
 */
enum class SettingsRowStyle {
    Boxed,
    Plain
}

/**
 * Rounded container that groups a set of settings rows.
 *
 * @param flat when true, drops the outline and tonal elevation and uses a solid
 * `surfaceContainer` background instead of the translucent tinted default.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = if (flat) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f)
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (flat) {
            null
        } else {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        },
        tonalElevation = if (flat) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            content()
        }
    }
}

/** Tinted squircle container used as the leading icon slot of a settings row. */
@Composable
fun LeadingIconBox(icon: ImageVector) {
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
}

/**
 * Settings row with an optional leading icon, a title, an optional subtitle and a trailing switch.
 *
 * @param subtitleMaxLines line cap for the subtitle; pass `Int.MAX_VALUE` to let it wrap.
 * @param style see [SettingsRowStyle].
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    subtitleMaxLines: Int = 1,
    enabled: Boolean = true,
    style: SettingsRowStyle = SettingsRowStyle.Boxed
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    ListItem(
        headlineContent = {
            Text(
                title,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    it,
                    maxLines = subtitleMaxLines,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
        },
        leadingContent = icon?.let {
            {
                when (style) {
                    SettingsRowStyle.Boxed -> LeadingIconBox(it)
                    SettingsRowStyle.Plain -> Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                }
            }
        },
        trailingContent = {
            when (style) {
                SettingsRowStyle.Boxed -> Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                SettingsRowStyle.Plain -> Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            }
        },
        modifier = when (style) {
            SettingsRowStyle.Boxed -> modifier
                .dpadFocusable(onClick = { onCheckedChange(!checked) }, shape = SquircleShape)
                .clip(SquircleShape)
            SettingsRowStyle.Plain -> modifier
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * Small heading that labels a group of settings rows.
 */
@Composable
fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Bold,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = fontWeight,
        modifier = modifier.padding(contentPadding)
    )
}

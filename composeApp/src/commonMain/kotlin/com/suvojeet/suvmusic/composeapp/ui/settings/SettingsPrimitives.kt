package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.theme.SquircleShape

/**
 * Rounded container that groups related settings rows.
 *
 * Defaults render the bordered, tonally elevated variant. Pass [border] as `null`,
 * [tonalElevation] as `0.dp` and a tighter [contentPadding] for the flat variant.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
    border: BorderStroke? = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    ),
    tonalElevation: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = border,
        tonalElevation = tonalElevation,
    ) {
        Column(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

/**
 * Squircle icon container used as the leading slot of settings rows.
 */
@Composable
fun LeadingIconBox(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(SquircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Settings row with a leading icon and a trailing switch.
 *
 * When [highlightWhenChecked] is true the row animates a tinted background and the
 * leading icon follows the checked state; when false the row stays flat and the icon
 * is always tinted with the primary color.
 */
@Composable
fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    highlightWhenChecked: Boolean = true,
    subtitleMaxLines: Int = 1,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (highlightWhenChecked && checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        }
    )

    val iconTint = when {
        !highlightWhenChecked -> MaterialTheme.colorScheme.primary
        checked -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val rowModifier = if (highlightWhenChecked) {
        Modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp)
    } else {
        Modifier
            .clickable { onCheckedChange(!checked) }
            .clip(SquircleShape)
    }

    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = subtitleMaxLines) } },
        leadingContent = { LeadingIconBox(icon = icon, tint = iconTint) },
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
        modifier = rowModifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

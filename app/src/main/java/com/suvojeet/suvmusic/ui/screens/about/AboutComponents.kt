package com.suvojeet.suvmusic.ui.screens.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.theme.SquircleShape

@Composable
internal fun AboutSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

@Composable
internal fun AboutCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
internal fun FeatureListItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
internal fun TechListItem(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
internal fun SocialIconBadge(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(SquircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

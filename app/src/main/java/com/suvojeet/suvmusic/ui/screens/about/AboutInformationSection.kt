package com.suvojeet.suvmusic.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable

@Composable
internal fun AboutInformationSection(
    onOpenUri: (String) -> Unit,
    onHowItWorksClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurfaceVariant = colorScheme.onSurfaceVariant
    val dividerColor = colorScheme.outlineVariant.copy(alpha = 0.2f)

    AboutSectionTitle("Information")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        ListItem(
            headlineContent = { Text("Official Website", fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("Visit the official SuvMusic website") },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SquircleShape)
                        .background(primaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Language, null,
                        tint = primaryColor, modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingContent = {
                Icon(
                    Icons.Default.OpenInNew, null,
                    modifier = Modifier.size(16.dp),
                    tint = onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier
                .dpadFocusable(
                    onClick = { onOpenUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/") },
                    shape = SquircleShape
                )
                .clip(SquircleShape),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)

        ListItem(
            headlineContent = { Text("Privacy Policy", fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("Review how SuvMusic handles your data") },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SquircleShape)
                        .background(primaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security, null,
                        tint = primaryColor, modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingContent = {
                Icon(
                    Icons.Default.OpenInNew, null,
                    modifier = Modifier.size(16.dp),
                    tint = onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier
                .dpadFocusable(
                    onClick = { onOpenUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/suvmusic-privacy.html") },
                    shape = SquircleShape
                )
                .clip(SquircleShape),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)

        ListItem(
            headlineContent = { Text("How It Works", fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("Learn how SuvMusic works with YouTube Music") },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SquircleShape)
                        .background(primaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lightbulb, null,
                        tint = primaryColor, modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    modifier = Modifier.size(16.dp),
                    tint = onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier
                .dpadFocusable(onClick = onHowItWorksClick, shape = SquircleShape)
                .clip(SquircleShape),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

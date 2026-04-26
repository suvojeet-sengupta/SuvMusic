package com.suvojeet.suvmusic.composeapp.ui.about

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AboutInformationSection(
    onOpenUri: (String) -> Unit,
    onHowItWorksClick: () -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    AboutSectionTitle("Information")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        InfoListItem(
            icon = Icons.Default.Language,
            trailingIcon = Icons.Default.OpenInNew,
            title = "Official Website",
            subtitle = "Visit the official SuvMusic website",
            onClick = { onOpenUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/") },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        InfoListItem(
            icon = Icons.Default.Security,
            trailingIcon = Icons.Default.OpenInNew,
            title = "Privacy Policy",
            subtitle = "Review how SuvMusic handles your data",
            onClick = { onOpenUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/suvmusic-privacy.html") },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        InfoListItem(
            icon = Icons.Default.Lightbulb,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            title = "How It Works",
            subtitle = "Learn how SuvMusic works with YouTube Music",
            onClick = onHowItWorksClick,
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

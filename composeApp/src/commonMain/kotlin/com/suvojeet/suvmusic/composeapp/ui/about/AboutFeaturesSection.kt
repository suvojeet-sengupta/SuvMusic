package com.suvojeet.suvmusic.composeapp.ui.about

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AboutFeaturesSection() {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    AboutSectionTitle("Features")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        FeatureListItem(
            icon = Icons.Outlined.Palette,
            title = "Best-in-Class UI",
            subtitle = "Unique and fluid Material 3 Experience",
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Outlined.Block,
            title = "100% Ad-Free",
            subtitle = "Zero interruptions, complete focus",
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Default.HighQuality,
            title = "High Fidelity",
            subtitle = "Premium audio quality and full control",
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Default.AutoAwesome,
            title = "AI Equalizer",
            subtitle = "Neural processing for perfect sound",
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

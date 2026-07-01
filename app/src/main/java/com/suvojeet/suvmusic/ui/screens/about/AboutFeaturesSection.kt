package com.suvojeet.suvmusic.ui.screens.about

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
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
            title = "Material 3 Interface",
            subtitle = "Clean, responsive design"
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Outlined.Block,
            title = "Ad-Free Playback",
            subtitle = "No interruptions while you listen"
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Default.HighQuality,
            title = "High-Quality Audio",
            subtitle = "Detailed playback controls"
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Outlined.Tune,
            title = "Equalizer",
            subtitle = "Adjust sound to your preference"
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

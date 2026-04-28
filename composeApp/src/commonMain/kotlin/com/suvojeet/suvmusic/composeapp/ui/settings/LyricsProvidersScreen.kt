package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Lyrics-providers picker — ported from
 * `app/.../ui/screens/LyricsProvidersScreen.kt` to commonMain.
 *
 * Stateless: takes per-provider `enabled` flags, the preferred-provider key,
 * and matching callbacks. Provider keys stay as String constants
 * ("BetterLyrics", "SimpMusic", "Kugou") so the persistence format on
 * either platform can keep using the values it already stores.
 *
 * No Scaffold/TopAppBar — host owns chrome.
 */
@Composable
fun LyricsProvidersScreen(
    betterLyricsEnabled: Boolean,
    simpMusicEnabled: Boolean,
    kuGouEnabled: Boolean,
    preferredProvider: String,
    onSetBetterLyricsEnabled: (Boolean) -> Unit,
    onSetSimpMusicEnabled: (Boolean) -> Unit,
    onSetKuGouEnabled: (Boolean) -> Unit,
    onSetPreferredProvider: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 20.dp),
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item {
                SectionTitle("Preferred Provider")
                Text(
                    text = "The preferred provider will be tried first. Only enabled providers can be selected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))

                GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LyricsProviderItem(
                        title = "BetterLyrics (Apple Music)",
                        subtitle = "Time-synced lyrics database",
                        enabled = betterLyricsEnabled,
                        onEnabledChange = onSetBetterLyricsEnabled,
                        isPreferred = preferredProvider == "BetterLyrics",
                        onSelectPreferred = { onSetPreferredProvider("BetterLyrics") },
                    )
                    ThinDivider()
                    LyricsProviderItem(
                        title = "SimpMusic",
                        subtitle = "Community sourced lyrics",
                        enabled = simpMusicEnabled,
                        onEnabledChange = onSetSimpMusicEnabled,
                        isPreferred = preferredProvider == "SimpMusic",
                        onSelectPreferred = { onSetPreferredProvider("SimpMusic") },
                    )
                    ThinDivider()
                    LyricsProviderItem(
                        title = "Kugou",
                        subtitle = "Massive lyrics library",
                        enabled = kuGouEnabled,
                        onEnabledChange = onSetKuGouEnabled,
                        isPreferred = preferredProvider == "Kugou",
                        onSelectPreferred = { onSetPreferredProvider("Kugou") },
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        ),
        tonalElevation = 0.dp,
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
private fun LyricsProviderItem(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    isPreferred: Boolean,
    onSelectPreferred: () -> Unit,
) {
    Column {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(subtitle) },
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            },
            modifier = Modifier.clickable { onEnabledChange(!enabled) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )

        AnimatedVisibility(
            visible = enabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "Preferred",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isPreferred) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                leadingContent = {
                    RadioButton(
                        selected = isPreferred,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelectPreferred)
                    .padding(start = 32.dp),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

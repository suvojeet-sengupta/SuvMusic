package com.suvojeet.suvmusic.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.BuildConfig
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.utils.SocialIcons
import com.suvojeet.suvmusic.util.dpadFocusable

@Composable
internal fun AboutHeroSection() {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "SuvMusic Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(SquircleShape)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SuvMusic",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(shape = SquircleShape, color = primaryColor.copy(alpha = 0.1f)) {
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelLarge,
                color = primaryColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Proudly Made in India 🇮🇳\nUnlimited Music. Ad-Free. Pure Bliss.",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
internal fun AboutDescriptionSection() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    AboutSectionTitle("About")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Suvmusic is the indian music streaming app (YT Music Client) that provides best UI unique features that not present anywhere and full control and better implementation.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Built with a vision to provide a seamless, high-fidelity auditory journey, SuvMusic bridges the gap between massive content availability and a truly personalized user experience.",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
internal fun AboutFeaturesSection() {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    AboutSectionTitle("Features")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        FeatureListItem(
            icon = Icons.Outlined.Palette,
            title = "Best-in-Class UI",
            subtitle = "Unique and fluid Material 3 Experience"
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Outlined.Block,
            title = "100% Ad-Free",
            subtitle = "Zero interruptions, complete focus"
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Default.HighQuality,
            title = "High Fidelity",
            subtitle = "Premium audio quality and full control"
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        FeatureListItem(
            icon = Icons.Default.AutoAwesome,
            title = "AI Equalizer",
            subtitle = "Neural processing for perfect sound"
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
internal fun AboutDeveloperSection(onOpenUri: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    AboutSectionTitle("Developer")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(SquircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor, colorScheme.tertiary)
                        )
                    )
                    .padding(3.dp)
                    .background(colorScheme.surface, SquircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://avatars.githubusercontent.com/u/107928380?v=4",
                    contentDescription = "Suvojeet Sengupta",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(SquircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.logo)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Suvojeet Sengupta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = colorScheme.onSurface
            )

            Text(
                text = "Vibe Coder",
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Thinking matters. I'm a self-taught Kotlin learner who believes architecture and creativity are my true + points. Delivering what AI can't—human touch in code.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SocialIconBadge(
                    icon = SocialIcons.GitHub,
                    onClick = { onOpenUri("https://github.com/suvojeet-sengupta") }
                )
                SocialIconBadge(
                    icon = Icons.Default.Email,
                    onClick = { onOpenUri("mailto:suvojeet@suvojeetsengupta.in") }
                )
                SocialIconBadge(
                    icon = Icons.Default.Language,
                    onClick = { onOpenUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/") }
                )
                SocialIconBadge(
                    icon = SocialIcons.Instagram,
                    onClick = { onOpenUri("https://www.instagram.com/suvojeet__sengupta?igsh=MWhyMXE4YzhxaDVvNg==") }
                )
                SocialIconBadge(
                    icon = SocialIcons.Telegram,
                    onClick = { onOpenUri("https://t.me/suvojeet_sengupta") }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
internal fun AboutTechStackSection() {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    AboutSectionTitle("Built With")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        TechListItem("Language", "Kotlin")
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        TechListItem("UI Framework", "Jetpack Compose")
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        TechListItem("Audio Engine", "Media3 ExoPlayer")
    }
    Spacer(modifier = Modifier.height(24.dp))
}

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

@Composable
internal fun AboutFooterSection() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Made with ❤️ in India",
            style = MaterialTheme.typography.bodySmall,
            color = onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "© 2026 Suvojeet Sengupta",
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

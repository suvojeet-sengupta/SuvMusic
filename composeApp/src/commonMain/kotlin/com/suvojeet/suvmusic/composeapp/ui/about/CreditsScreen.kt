package com.suvojeet.suvmusic.composeapp.ui.about

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.composeapp.theme.SquircleShape

/**
 * Credits & Contributors screen — ported from
 * `app/.../ui/screens/CreditsScreen.kt` to commonMain.
 *
 * Differences vs the Android original:
 *   - Stateless: takes `onOpenUri: (String) -> Unit` instead of relying on
 *     LocalContext + Intent (Intent is Android-only). Hosts wire it to a
 *     platform browser launch.
 *   - Avatar fallback drawable (R.drawable.logo) removed — Android resource
 *     IDs aren't available in commonMain. Coil 3's AsyncImage shows nothing
 *     while loading; the gradient ring still gives visual structure.
 *   - Social icons use closest-match Material icons (Code/Photo/Send/Language)
 *     to avoid carrying Android-only vector drawables. Same substitution
 *     AboutDeveloperSection already made.
 *   - No Scaffold + LargeTopAppBar — host owns chrome.
 */
@Composable
fun CreditsScreen(
    onOpenUri: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            SectionHeader("LEAD DEVELOPER")
            DeveloperCard(onOpenUri = onOpenUri)
        }

        item {
            SectionHeader("CORE ENGINE")
            LibraryCard(
                name = "NewPipe Extractor",
                description = "The powerful engine that parses YouTube data and streams without using official APIs.",
                isSpecial = true,
                onClick = { onOpenUri("https://github.com/TeamNewPipe/NewPipeExtractor") },
            )
        }

        item {
            SectionHeader("NATIVE PERFORMANCE")
            LibraryCard(
                name = "C++ Audio Processing",
                description = "High-performance native code used for spatial audio processing and ultra-fast waveform extraction via mmap.",
                isSpecial = true,
                onClick = { onOpenUri("https://developer.android.com/ndk") },
            )
        }

        item {
            SectionHeader("SPECIAL THANKS")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryCard(
                    name = "SponsorBlock",
                    description = "For providing the segments that help users skip annoying sponsors automatically.",
                    isSpecial = true,
                    onClick = { onOpenUri("https://sponsor.ajay.app/") },
                )
                LibraryCard(
                    name = "NYX / Listen Together",
                    description = "Owner of the Listen Together infrastructure (metroserver.meowery.eu). Special thanks for the server support.",
                    isSpecial = true,
                    onClick = { onOpenUri("https://nyx.meowery.eu/") },
                )
                LibraryCard(
                    name = "Discord integration",
                    description = "For the Rich Presence support allowing users to share what they are listening to on their profile.",
                    isSpecial = true,
                    onClick = { onOpenUri("https://discord.com/developers") },
                )
            }
        }

        item {
            SectionHeader("EXTERNAL SERVICES")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryCard(
                    name = "Last.fm",
                    description = "Used for scrobbling support and providing high-quality personalized recommendations.",
                    onClick = { onOpenUri("https://www.last.fm/") },
                )
                LibraryCard(
                    name = "YouTube Music Auth",
                    description = "Custom implementation for secure authentication to access user libraries and history.",
                    onClick = { onOpenUri("https://music.youtube.com/") },
                )
            }
        }

        item { SectionHeader("LYRICS ECOSYSTEM") }

        items(LYRICS_PROVIDERS) { provider ->
            LibraryCard(
                name = provider.name,
                description = provider.description,
                onClick = { provider.url?.let(onOpenUri) },
            )
        }

        item { SectionHeader("KEY LIBRARIES") }

        items(LIBRARIES) { lib ->
            LibraryCard(
                name = lib.name,
                description = lib.description,
                onClick = { lib.url?.let(onOpenUri) },
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SquircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            ) {
                Text(
                    text = "SuvMusic is built with love and respect for the open-source community.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun DeveloperCard(onOpenUri: (String) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(SquircleShape)
                        .background(
                            Brush.radialGradient(listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent)),
                        ),
                )

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(SquircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = "https://avatars.githubusercontent.com/u/107928380?v=4",
                        contentDescription = "Suvojeet Sengupta",
                        modifier = Modifier.size(90.dp).clip(SquircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Suvojeet Sengupta",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = "Main Developer & Maintainer",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SocialIconBadge(Icons.Filled.Code) { onOpenUri("https://github.com/suvojeet-sengupta") }
                SocialIconBadge(Icons.Filled.Language) { onOpenUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/") }
                SocialIconBadge(Icons.Filled.Photo) { onOpenUri("https://www.instagram.com/suvojeet__sengupta") }
                SocialIconBadge(Icons.Filled.Send) { onOpenUri("https://t.me/suvojeet_sengupta") }
                SocialIconBadge(Icons.Filled.Email) { onOpenUri("mailto:suvojeet@suvojeetsengupta.in") }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
    )
}

@Composable
private fun LibraryCard(
    name: String,
    description: String,
    isSpecial: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSpecial) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f)
        },
        border = if (isSpecial) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SquircleShape)
                    .background(
                        if (isSpecial) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSpecial) Icons.Rounded.Favorite else Icons.Rounded.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSpecial) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

private data class CreditsLibraryItem(
    val name: String,
    val description: String,
    val url: String? = null,
)

private val LYRICS_PROVIDERS = listOf(
    CreditsLibraryItem("BetterLyrics", "Fetches time-synced lyrics from Apple Music API.", "https://github.com/BetterLyrics"),
    CreditsLibraryItem("KuGou", "Provides high-quality synced lyrics from KuGou Music engine.", "https://www.kugou.com/"),
    CreditsLibraryItem("LRCLIB", "Open-source lyrics provider with a massive community-driven database.", "https://lrclib.net/"),
)

private val LIBRARIES = listOf(
    CreditsLibraryItem("Media3 / ExoPlayer", "Google's powerful media engine for Android playback.", "https://github.com/androidx/media"),
    CreditsLibraryItem("Jetpack Compose", "The modern Android toolkit for building native UI.", "https://developer.android.com/jetpack/compose"),
    CreditsLibraryItem("Metrolist", "Core logic for the 'Listen Together' protocol.", "https://github.com/MetrolistGroup/Metrolist"),
    CreditsLibraryItem("ACRA", "Robust crash reporting system for Android stability.", "https://acra.ch/"),
    CreditsLibraryItem("Hilt", "Standard dependency injection library for Android.", "https://dagger.dev/hilt/"),
    CreditsLibraryItem("Coil 3", "Next-gen image loading library for Android and Compose Multiplatform.", "https://coil-kt.github.io/coil/"),
    CreditsLibraryItem("Retrofit", "The type-safe HTTP client for Android and Java.", "https://square.github.io/retrofit/"),
    CreditsLibraryItem("Room", "The SQLite object mapping library for robust local storage.", "https://developer.android.com/training/data-storage/room"),
    CreditsLibraryItem("Material Design 3", "Latest design system for a clean and modern experience.", "https://m3.material.io/"),
)

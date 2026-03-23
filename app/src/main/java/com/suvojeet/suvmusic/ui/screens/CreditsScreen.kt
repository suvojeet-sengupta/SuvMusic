package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.utils.SocialIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Credits & Contributors", 
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Lead Developer ---
            item {
                SectionHeader("LEAD DEVELOPER")
                DeveloperCard()
            }

            // --- Core Engine ---
            item {
                SectionHeader("CORE ENGINE")
                LibraryCard(
                    name = "NewPipe Extractor",
                    description = "The powerful engine that parses YouTube data and streams without using official APIs.",
                    isSpecial = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TeamNewPipe/NewPipeExtractor"))
                        context.startActivity(intent)
                    }
                )
            }

            // --- Native Performance ---
            item {
                SectionHeader("NATIVE PERFORMANCE")
                LibraryCard(
                    name = "C++ Audio Processing",
                    description = "High-performance native code used for spatial audio processing and ultra-fast waveform extraction via mmap.",
                    isSpecial = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.android.com/ndk"))
                        context.startActivity(intent)
                    }
                )
            }

            // --- Special Thanks ---
            item {
                SectionHeader("SPECIAL THANKS")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryCard(
                        name = "SponsorBlock",
                        description = "For providing the segments that help users skip annoying sponsors automatically.",
                        isSpecial = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sponsor.ajay.app/"))
                            context.startActivity(intent)
                        }
                    )

                    LibraryCard(
                        name = "NYX / Listen Together",
                        description = "Owner of the Listen Together infrastructure (metroserver.meowery.eu). Special thanks for the server support.",
                        isSpecial = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://nyx.meowery.eu/"))
                            context.startActivity(intent)
                        }
                    )
                    
                    LibraryCard(
                        name = "Discord integration",
                        description = "For the Rich Presence support allowing users to share what they are listening to on their profile.",
                        isSpecial = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/developers"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // --- Third-Party Services ---
            item {
                SectionHeader("EXTERNAL SERVICES")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryCard(
                        name = "Last.fm",
                        description = "Used for scrobbling support and providing high-quality personalized recommendations.",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/"))
                            context.startActivity(intent)
                        }
                    )

                    LibraryCard(
                        name = "YouTube Music Auth",
                        description = "Custom implementation for secure authentication to access user libraries and history.",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // --- Lyrics Providers ---
            item {
                SectionHeader("LYRICS ECOSYSTEM")
            }
            
            items(getLyricsProviders()) { provider ->
                LibraryCard(
                    name = provider.name,
                    description = provider.description,
                    onClick = {
                        provider.url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // --- Open Source Libraries ---
            item {
                SectionHeader("KEY LIBRARIES")
            }

            items(getLibraries()) { lib ->
                LibraryCard(
                    name = lib.name,
                    description = lib.description,
                    onClick = {
                        lib.url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SquircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "SuvMusic is built with ❤️ and respect for the open-source community.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun DeveloperCard() {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Pulse animation for avatar border
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Animated pulse ring matching About screen's background glow style but with pulse
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(SquircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(SquircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://avatars.githubusercontent.com/u/107928380?v=4",
                        contentDescription = "Suvojeet Sengupta",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(SquircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.logo)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Suvojeet Sengupta",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Main Developer & Maintainer",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SocialIconBadge(icon = SocialIcons.GitHub, onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta"))
                    context.startActivity(intent)
                })
                SocialIconBadge(icon = SocialIcons.Instagram, onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/suvojeet__sengupta?igsh=MWhyMXE4YzhxaDVvNg=="))
                    context.startActivity(intent)
                })
                SocialIconBadge(icon = SocialIcons.Telegram, onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/suvojeet_sengupta"))
                    context.startActivity(intent)
                })
            }
        }
    }
}

@Composable
private fun SocialIconBadge(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(SquircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun LibraryCard(
    name: String,
    description: String,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSpecial) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
        else 
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
        border = if (isSpecial) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) 
        else 
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SquircleShape)
                    .background(
                        if (isSpecial) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSpecial) Icons.Rounded.Favorite else Icons.Rounded.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSpecial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

data class LibraryItem(
    val name: String,
    val description: String,
    val url: String? = null
)

private fun getLyricsProviders(): List<LibraryItem> {
    return listOf(
        LibraryItem("BetterLyrics", "Fetches time-synced lyrics from Apple Music API.", "https://github.com/BetterLyrics"),
        LibraryItem("KuGou", "Provides high-quality synced lyrics from KuGou Music engine.", "https://www.kugou.com/"),
        LibraryItem("LRCLIB", "Open-source lyrics provider with a massive community-driven database.", "https://lrclib.net/")
    )
}

private fun getLibraries(): List<LibraryItem> {
    return listOf(
        LibraryItem("Media3 / ExoPlayer", "Google's powerful media engine for Android playback.", "https://github.com/androidx/media"),
        LibraryItem("Jetpack Compose", "The modern Android toolkit for building native UI.", "https://developer.android.com/jetpack/compose"),
        LibraryItem("Metrolist", "Core logic for the 'Listen Together' protocol.", "https://github.com/MetrolistGroup/Metrolist"),
        LibraryItem("ACRA", "Robust crash reporting system for Android stability.", "https://acra.ch/"),
        LibraryItem("Hilt", "Standard dependency injection library for Android.", "https://dagger.dev/hilt/"),
        LibraryItem("Coil 3", "Next-gen image loading library for Android and Compose Multiplatform.", "https://coil-kt.github.io/coil/"),
        LibraryItem("Retrofit", "The type-safe HTTP client for Android and Java.", "https://square.github.io/retrofit/"),
        LibraryItem("Room", "The SQLite object mapping library for robust local storage.", "https://developer.android.com/training/data-storage/room"),
        LibraryItem("Material Design 3", "Latest design system for a clean and modern experience.", "https://m3.material.io/")
    )
}

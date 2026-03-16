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
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = "SuvMusic is built with passion and respect for the open-source community.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
    
    // Pulse animation for avatar border
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Animated pulse ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                AsyncImage(
                    model = "https://avatars.githubusercontent.com/u/suvojeet-sengupta",
                    contentDescription = "Suvojeet Sengupta",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                            CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Suvojeet Sengupta",
                style = MaterialTheme.typography.headlineSmall.copy(
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
            
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Visit GitHub Profile",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        ),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 8.dp)
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
        shape = RoundedCornerShape(24.dp),
        color = if (isSpecial) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
        else 
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = if (isSpecial) 0.dp else 1.dp,
        border = if (isSpecial) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) 
        else 
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSpecial) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSpecial) Icons.Rounded.Favorite else Icons.Rounded.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (isSpecial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
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
        LibraryItem("BetterLyrics", "Fetches time-synced lyrics from Apple Music.", "https://github.com/BetterLyrics"),
        LibraryItem("KuGou", "Provides high-quality synced lyrics from KuGou Music.", "https://www.kugou.com/"),
        LibraryItem("LRCLIB", "Open-source lyrics provider with a massive community database.", "https://lrclib.net/")
    )
}

private fun getLibraries(): List<LibraryItem> {
    return listOf(
        LibraryItem("Media3 / ExoPlayer", "The engine for media playback on Android.", "https://github.com/androidx/media"),
        LibraryItem("Jetpack Compose", "The modern toolkit for native UI development.", "https://developer.android.com/jetpack/compose"),
        LibraryItem("Hilt", "Dependency injection for cleaner code structure.", "https://dagger.dev/hilt/"),
        LibraryItem("Coil", "Image loading with Kotlin Coroutines.", "https://coil-kt.github.io/coil/"),
        LibraryItem("Retrofit", "The type-safe HTTP client for network requests.", "https://square.github.io/retrofit/"),
        LibraryItem("Room", "The local database solution for playlists and history.", "https://developer.android.com/training/data-storage/room"),
        LibraryItem("Material Design 3", "Latest design system for a modern experience.", "https://m3.material.io/")
    )
}

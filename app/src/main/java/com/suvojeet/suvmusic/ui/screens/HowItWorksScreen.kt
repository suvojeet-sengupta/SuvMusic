package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * How It Works Screen - Explains how SuvMusic works with YouTube Music
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val surfaceColor = colorScheme.surface
    val surfaceContainerColor = colorScheme.surfaceContainer
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "How It Works",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = surfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Hero Section
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = primaryColor
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Powered by YouTube Music",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = onSurfaceColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "SuvMusic gives you a premium music experience using YouTube Music's vast library",
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Step 1 - Search
            StepCard(
                stepNumber = 1,
                icon = Icons.Default.Search,
                title = "Search Any Song",
                description = "Search millions of songs, albums, artists and playlists from YouTube Music's vast library. Find exactly what you want to listen to.",
                primaryColor = primaryColor,
                surfaceContainerColor = surfaceContainerColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariant = onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Step 2 - Stream
            StepCard(
                stepNumber = 2,
                icon = Icons.Default.Cloud,
                title = "Stream in High Quality",
                description = "SuvMusic fetches the audio stream directly from YouTube Music servers. Get up to 320kbps audio quality for the best listening experience.",
                primaryColor = primaryColor,
                surfaceContainerColor = surfaceContainerColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariant = onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Step 3 - Personalization
            StepCard(
                stepNumber = 3,
                icon = Icons.Default.Person,
                title = "Login for Personalization",
                description = "Sign in with your YouTube account to get personalized recommendations, your playlists, liked songs, and listening history synced across devices.",
                primaryColor = primaryColor,
                surfaceContainerColor = surfaceContainerColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariant = onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Step 4 - Download
            StepCard(
                stepNumber = 4,
                icon = Icons.Default.Download,
                title = "Download for Offline",
                description = "Download any song to your device for offline listening. Take your music with you anywhere, no internet required.",
                primaryColor = primaryColor,
                surfaceContainerColor = surfaceContainerColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariant = onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Features Section
            SectionTitle("Key Features", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Column {
                    FeatureItem(
                        icon = Icons.Outlined.Block,
                        title = "No Ads",
                        subtitle = "Enjoy uninterrupted music without any advertisements",
                        primaryColor = primaryColor
                    )
                    FeatureItem(
                        icon = Icons.Default.Lyrics,
                        title = "Live Lyrics",
                        subtitle = "Sing along with synced lyrics for most songs",
                        primaryColor = primaryColor
                    )
                    FeatureItem(
                        icon = Icons.Default.Radio,
                        title = "Radio Mode",
                        subtitle = "Endless similar songs based on what you're listening to",
                        primaryColor = primaryColor
                    )
                    FeatureItem(
                        icon = Icons.Default.Speed,
                        title = "Playback Speed",
                        subtitle = "Adjust speed from 0.5x to 2x for any song",
                        primaryColor = primaryColor
                    )
                    FeatureItem(
                        icon = Icons.Default.Timer,
                        title = "Sleep Timer",
                        subtitle = "Fall asleep to your music with automatic stop",
                        primaryColor = primaryColor,
                        showDivider = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Privacy Notice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primaryColor.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Privacy Matters",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = onSurfaceColor
                        )
                        Text(
                            text = "SuvMusic doesn't collect or store your personal data. All playback happens directly from YouTube Music.",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: Int,
    icon: ImageVector,
    title: String,
    description: String,
    primaryColor: Color,
    surfaceContainerColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariant: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step Number Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = primaryColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = primaryColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = onSurfaceColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    color = onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primaryColor: Color,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

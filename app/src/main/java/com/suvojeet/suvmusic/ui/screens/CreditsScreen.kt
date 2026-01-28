package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.ui.theme.Purple40
import com.suvojeet.suvmusic.ui.theme.Purple80

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
                title = { Text("Credits", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Main Developer ---
            item {
                DeveloperCard()
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- Special Mentions ---
            item {
                SectionHeader("Powered By")
            }

            item {
                LibraryCard(
                    name = "NewPipe Extractor",
                    description = "The core engine behind SuvMusic. Parses YouTube data, streams, and metadata without API keys.",
                    isSpecial = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TeamNewPipe/NewPipeExtractor"))
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                LibraryCard(
                    name = "SponsorBlock",
                    description = "Community-driven platform for skipping sponsor segments in YouTube videos.",
                    isSpecial = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sponsor.ajay.app/"))
                        context.startActivity(intent)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- Lyrics Providers ---
            item {
                SectionHeader("Lyrics Providers")
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

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- Open Source Libraries ---
            item {
                SectionHeader("Open Source Libraries")
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
                Text(
                    text = "Made with ❤️ using Kotlin & Jetpack Compose",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DeveloperCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Purple40, Purple80)
                        )
                    )
                    .border(2.dp, MaterialTheme.colorScheme.onPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Suvojeet Sengupta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "Lead Developer & Designer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { 
                        // Link would go here (e.g. GitHub/Portfolio)
                    }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Visit Portfolio",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun LibraryCard(
    name: String,
    description: String,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpecial) 
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isSpecial) BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSpecial) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                )
                
                if (isSpecial) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Special",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        LibraryItem("SimpMusic / LRCLIB", "Open-source lyrics provider with a massive database.", "https://lrclib.net/")
    )
}

private fun getLibraries(): List<LibraryItem> {
    return listOf(
        LibraryItem("Media3 / ExoPlayer", "Google's media player framework for Android.", "https://github.com/androidx/media"),
        LibraryItem("Jetpack Compose", "Modern toolkit for building native UI.", "https://developer.android.com/jetpack/compose"),
        LibraryItem("Hilt", "Dependency injection library for Android.", "https://dagger.dev/hilt/"),
        LibraryItem("Coil", "Image loading library for Kotlin Coroutines.", "https://coil-kt.github.io/coil/"),
        LibraryItem("Retrofit", "Type-safe HTTP client for Android and Java.", "https://square.github.io/retrofit/"),
        LibraryItem("OkHttp", "Square's meticulous HTTP client for Java and Kotlin.", "https://square.github.io/okhttp/"),
        LibraryItem("Room", "Persistence library provides an abstraction layer over SQLite.", "https://developer.android.com/training/data-storage/room"),
        LibraryItem("Material Design 3", "Latest design system from Google.", "https://m3.material.io/")
    )
}

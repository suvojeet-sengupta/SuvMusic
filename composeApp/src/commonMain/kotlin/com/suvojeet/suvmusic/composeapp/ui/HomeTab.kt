package com.suvojeet.suvmusic.composeapp.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.domain.player.MusicPlayer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Home tab — visual mirror of app/.../ui/screens/HomeScreen.kt's section
 * structure (greeting, quick actions, trending, etc.) but using only data
 * sources reachable from commonMain on Desktop:
 *
 *  - Greeting derived from kotlinx-datetime (no SessionManager / TZ
 *    preference yet).
 *  - Quick action cards driving file picker, search-tab nav, now-playing.
 *  - Trending search chips (preset queries) — taps invoke the search
 *    callback with that query so the Search tab populates.
 *  - "Now Playing" card if a song is loaded — large variant of the
 *    bottom-bar info, tappable to expand the player.
 *
 * Real Android Home (recommendations / quick picks / mood-based mixes
 * / pull-to-refresh / FAB) needs HomeViewModel + YouTubeRepository +
 * RecommendationEngine ported to commonMain — multi-session work.
 * This is the visual shell that replaces the previous "Welcome card"
 * placeholder.
 */
@Composable
fun HomeTab(
    appVersion: String,
    player: MusicPlayer,
    onPickFile: () -> Unit,
    onGoToSearch: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onExpandPlayer: () -> Unit,
) {
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { GreetingHeader(appVersion = appVersion) }

        if (currentSong != null) {
            item {
                NowPlayingCard(
                    title = currentSong?.title.orEmpty(),
                    artist = currentSong?.artist.orEmpty(),
                    thumbnailUrl = currentSong?.thumbnailUrl,
                    isPlaying = isPlaying,
                    onClick = onExpandPlayer,
                )
            }
        }

        item { SectionTitle("Quick actions") }
        item {
            QuickActionsRow(
                onPickFile = onPickFile,
                onGoToSearch = onGoToSearch,
            )
        }

        item { SectionTitle("Trending searches") }
        item {
            TrendingChips(onSearchQuery = onSearchQuery)
        }

        item { SectionTitle("How to use") }
        item { UsageTipsCard() }
    }
}

@Composable
private fun GreetingHeader(appVersion: String) {
    val greeting = remember { greetingForCurrentTime() }
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "SuvMusic · v$appVersion",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun NowPlayingCard(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(
                thumbnailUrl = thumbnailUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPlaying) "Now playing" else "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onPickFile: () -> Unit,
    onGoToSearch: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            QuickActionCard(
                icon = Icons.Filled.FolderOpen,
                title = "Pick a file",
                subtitle = "Play local audio",
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                ),
                onClick = onPickFile,
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Filled.Search,
                title = "Search YouTube",
                subtitle = "Find any song",
                colors = listOf(
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primary,
                ),
                onClick = onGoToSearch,
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.TopStart),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

private val TRENDING_QUERIES = listOf(
    "Bollywood Hits 2025",
    "Top Hindi Songs",
    "Lo-Fi Chill",
    "Arijit Singh",
    "Ed Sheeran",
    "Workout Mix",
    "Punjabi Hits",
    "AR Rahman",
    "Coke Studio",
)

@Composable
private fun TrendingChips(onSearchQuery: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TRENDING_QUERIES) { query ->
            AssistChip(
                onClick = { onSearchQuery(query) },
                label = { Text(query) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

@Composable
private fun UsageTipsCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageTip("• Click the bottom player bar to expand the full player view.")
            UsageTip("• Drag the seek bar to jump anywhere in a track.")
            UsageTip("• Use the Search tab to find any song on YouTube and play instantly.")
            UsageTip("• Lyrics, library scanning, and history sync arrive in upcoming releases.")
        }
    }
}

@Composable
private fun UsageTip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalTime::class)
private fun greetingForCurrentTime(): String {
    // kotlinx-datetime 0.7+ deprecates `kotlinx.datetime.Clock` in favour
    // of `kotlin.time.Clock` from the standard library (still
    // ExperimentalTime). The Instant returned interoperates with
    // kotlinx-datetime's `toLocalDateTime` extension.
    val hour = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .hour
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}

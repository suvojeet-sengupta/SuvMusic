package com.suvojeet.suvmusic.composeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.setSingletonImageLoaderFactory
import com.suvojeet.suvmusic.composeapp.image.buildAppImageLoader
import com.suvojeet.suvmusic.composeapp.theme.SuvMusicTheme
import com.suvojeet.suvmusic.composeapp.ui.AboutTab
import com.suvojeet.suvmusic.composeapp.ui.AlbumArt
import com.suvojeet.suvmusic.composeapp.ui.HomeTab
import com.suvojeet.suvmusic.composeapp.ui.LibraryTab
import com.suvojeet.suvmusic.composeapp.ui.LyricsTab
import com.suvojeet.suvmusic.composeapp.ui.NowPlayingScreen
import com.suvojeet.suvmusic.composeapp.ui.RemoteSearchResult
import com.suvojeet.suvmusic.composeapp.ui.SearchTab
import com.suvojeet.suvmusic.composeapp.ui.VlcWarningBanner
import com.suvojeet.suvmusic.composeapp.ui.audioFileToSong
import com.suvojeet.suvmusic.composeapp.ui.formatMs
import com.suvojeet.suvmusic.core.domain.player.MusicPlayer
import com.suvojeet.suvmusic.core.model.Song

/**
 * Top-level Desktop app shell. Three regions:
 *  1. NavigationRail on the left — Home / Search / Library / About
 *  2. Content area on the right — the active tab fills it
 *  3. Persistent player bar at the bottom — shows whatever is loaded in
 *     the [MusicPlayer], play/pause + seek, always visible
 *
 * The player is constructed once at app start and threaded into both
 * content tabs (so they can call setQueue when a track is picked) and
 * the bottom bar (so it can render state and accept controls).
 *
 * Phase 5.1 status: real production screens (HomeScreen, PlayerScreen
 * etc. from :app) haven't moved to commonMain yet — those need their VM
 * + repo stack ported. Tabs here are placeholders or simple
 * implementations; they get progressively replaced with the Android UI
 * code as it migrates.
 */
@Composable
fun App(
    appVersion: String = "0.0.0-dev",
    onOpenUrl: (String) -> Unit = {},
    onPickAudioFile: () -> String? = { null },
    onSearchYouTube: (suspend (String) -> List<RemoteSearchResult>)? = null,
    onResolveStreamSong: (suspend (RemoteSearchResult) -> Song?)? = null,
) {
    setSingletonImageLoaderFactory { context -> buildAppImageLoader(context) }

    val musicPlayer = remember { MusicPlayer() }
    DisposableEffect(musicPlayer) {
        onDispose { musicPlayer.release() }
    }

    var selectedTab by remember { mutableStateOf(Tab.Home) }
    var playerExpanded by remember { mutableStateOf(false) }
    var searchSeedQuery by remember { mutableStateOf("") }

    SuvMusicTheme {
        if (playerExpanded) {
            NowPlayingScreen(
                player = musicPlayer,
                onCollapse = { playerExpanded = false },
            )
            return@SuvMusicTheme
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AppNavRail(
                        selected = selectedTab,
                        onSelect = { selectedTab = it },
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxSize().padding(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (!musicPlayer.isAvailable) {
                                VlcWarningBanner(onOpenUrl = onOpenUrl)
                            }

                            when (selectedTab) {
                                Tab.Home -> HomeTab(
                                    appVersion = appVersion,
                                    player = musicPlayer,
                                    onPickFile = {
                                        val path = onPickAudioFile() ?: return@HomeTab
                                        musicPlayer.setQueue(listOf(audioFileToSong(path)))
                                    },
                                    onGoToSearch = { selectedTab = Tab.Search },
                                    onSearchQuery = { q ->
                                        searchSeedQuery = q
                                        selectedTab = Tab.Search
                                    },
                                    onExpandPlayer = { playerExpanded = true },
                                )
                                Tab.Search -> SearchTab(
                                    onSearch = onSearchYouTube,
                                    onPlayResult = { result ->
                                        val song = onResolveStreamSong?.invoke(result) ?: return@SearchTab
                                        musicPlayer.setQueue(listOf(song))
                                    },
                                    seedQuery = searchSeedQuery,
                                )
                                Tab.Library -> LibraryTab(
                                    onPickFile = {
                                        val path = onPickAudioFile() ?: return@LibraryTab
                                        musicPlayer.setQueue(listOf(audioFileToSong(path)))
                                    },
                                )
                                Tab.Lyrics -> LyricsTab(player = musicPlayer)
                                Tab.About -> AboutTab(
                                    appVersion = appVersion,
                                    onOpenUrl = onOpenUrl,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BottomPlayerBar(
                    player = musicPlayer,
                    onExpand = { playerExpanded = true },
                )
            }
        }
    }
}

private enum class Tab(val label: String) {
    Home("Home"),
    Search("Search"),
    Library("Library"),
    Lyrics("Lyrics"),
    About("About"),
}

@Composable
private fun AppNavRail(
    selected: Tab,
    onSelect: (Tab) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.width(96.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "SuvMusic",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(24.dp))

        NavigationRailItem(
            selected = selected == Tab.Home,
            onClick = { onSelect(Tab.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
        )
        NavigationRailItem(
            selected = selected == Tab.Search,
            onClick = { onSelect(Tab.Search) },
            icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            label = { Text("Search") },
        )
        NavigationRailItem(
            selected = selected == Tab.Library,
            onClick = { onSelect(Tab.Library) },
            icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "Library") },
            label = { Text("Library") },
        )
        NavigationRailItem(
            selected = selected == Tab.Lyrics,
            onClick = { onSelect(Tab.Lyrics) },
            icon = { Icon(Icons.Outlined.Lyrics, contentDescription = "Lyrics") },
            label = { Text("Lyrics") },
        )
        NavigationRailItem(
            selected = selected == Tab.About,
            onClick = { onSelect(Tab.About) },
            icon = { Icon(Icons.Outlined.Info, contentDescription = "About") },
            label = { Text("About") },
        )
    }
}

@Composable
private fun BottomPlayerBar(player: MusicPlayer, onExpand: () -> Unit) {
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()

    val canExpand = currentSong != null
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (canExpand) it.clickable(onClick = onExpand) else it },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AlbumArt(
                thumbnailUrl = currentSong?.thumbnailUrl,
                contentDescription = currentSong?.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Column(modifier = Modifier.weight(0.3f)) {
                Text(
                    text = currentSong?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentSong?.artist ?: "Pick a song to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = { player.togglePlayPause() },
                enabled = currentSong != null && player.isAvailable,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = positionMs.toFloat(),
                    onValueChange = { player.seekTo(it.toLong()) },
                    valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    enabled = currentSong != null && player.isAvailable,
                )
                Text(
                    text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

package com.suvojeet.suvmusic.composeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.domain.player.MusicPlayer
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import kotlinx.coroutines.launch

/**
 * Top-level app composable. Renders About header, a "play local file"
 * section, and a "search YouTube" section.
 *
 * Platform callbacks (file picker, URL opener, search) are passed down
 * so the composable stays free of Desktop-specific imports — Android
 * could eventually wire equivalents.
 */
@Composable
fun App(
    appVersion: String = "0.0.0-dev",
    onOpenUrl: (String) -> Unit = {},
    onPickAudioFile: () -> String? = { null },
    onSearchYouTube: (suspend (String) -> List<RemoteSearchResult>)? = null,
    onResolveStreamSong: (suspend (RemoteSearchResult) -> Song?)? = null,
) {
    val musicPlayer = remember { MusicPlayer() }
    DisposableEffect(musicPlayer) {
        onDispose { musicPlayer.release() }
    }

    val currentSong by musicPlayer.currentSong.collectAsState()
    val isPlaying by musicPlayer.isPlaying.collectAsState()
    val positionMs by musicPlayer.positionMs.collectAsState()
    val durationMs by musicPlayer.durationMs.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AboutHeader(appVersion = appVersion, onOpenUrl = onOpenUrl)

                if (!musicPlayer.isAvailable) {
                    VlcWarning(onOpenUrl = onOpenUrl)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.widthIn(max = 600.dp))
                Spacer(Modifier.height(8.dp))

                PlayerSection(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onPickFile = {
                        val path = onPickAudioFile() ?: return@PlayerSection
                        val song = audioFileToSong(path)
                        musicPlayer.setQueue(listOf(song))
                    },
                    onTogglePlayPause = { musicPlayer.togglePlayPause() },
                    onSeek = { ms -> musicPlayer.seekTo(ms) },
                )

                if (onSearchYouTube != null && onResolveStreamSong != null) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.widthIn(max = 600.dp))
                    Spacer(Modifier.height(8.dp))
                    SearchSection(
                        onSearch = onSearchYouTube,
                        onPlayResult = { result ->
                            val song = onResolveStreamSong(result) ?: return@SearchSection
                            musicPlayer.setQueue(listOf(song))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHeader(appVersion: String, onOpenUrl: (String) -> Unit) {
    Text(
        text = "SuvMusic",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Version $appVersion",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "Now multiplatform — this screen renders from shared Kotlin code on Android and Windows.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 480.dp),
    )
    OutlinedButton(onClick = { onOpenUrl("https://github.com/suvojeet-sengupta/SuvMusic") }) {
        Text("View on GitHub")
    }
}

/**
 * Banner shown only when MusicPlayer.isAvailable is false (Desktop without
 * VLC installed). Tells the user how to fix it instead of leaving them to
 * wonder why the play button does nothing.
 */
@Composable
private fun VlcWarning(onOpenUrl: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.widthIn(max = 600.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "VLC media player not detected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "SuvMusic Desktop uses VLC's playback engine (LibVLC). " +
                    "Install VLC media player and relaunch the app.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Windows: open PowerShell and run  winget install VideoLAN.VLC",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { onOpenUrl("https://www.videolan.org/vlc/") }) {
                Text("Download VLC")
            }
        }
    }
}

@Composable
private fun PlayerSection(
    currentSong: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPickFile: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    Text(
        text = "Player",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )

    Button(onClick = onPickFile) {
        Text("Pick local audio file")
    }

    if (currentSong != null) {
        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.widthIn(max = 600.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = currentSong.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Slider(
            value = positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
        )

        Text(
            text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onTogglePlayPause) {
            Text(if (isPlaying) "Pause" else "Play")
        }
    } else {
        Text(
            text = "No song loaded. Pick an audio file or search YouTube below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchSection(
    onSearch: suspend (String) -> List<RemoteSearchResult>,
    onPlayResult: suspend (RemoteSearchResult) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RemoteSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Text(
        text = "Search YouTube",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )

    Row(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Song / artist / video") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        Button(
            onClick = {
                errorMessage = null
                isSearching = true
                scope.launch {
                    try {
                        results = onSearch(query)
                    } catch (t: Throwable) {
                        errorMessage = t.message ?: "Search failed"
                        results = emptyList()
                    } finally {
                        isSearching = false
                    }
                }
            },
            enabled = query.isNotBlank() && !isSearching,
        ) {
            Text(if (isSearching) "..." else "Search")
        }
    }

    errorMessage?.let { msg ->
        Text(
            text = msg,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (results.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .heightIn(max = 360.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(results) { result ->
                Card(
                    onClick = {
                        scope.launch {
                            try {
                                onPlayResult(result)
                            } catch (t: Throwable) {
                                errorMessage = "Failed to play: ${t.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${result.uploader} · ${formatDurationSeconds(result.durationSeconds)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Platform-neutral search result type. The Desktop main wires this up to
 * NewPipe's StreamInfoItem; an Android-side implementation could feed the
 * existing YouTubeRepository the same way.
 */
data class RemoteSearchResult(
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val url: String,
    val thumbnailUrl: String?,
)

/** Build a minimal [Song] from a local file path. Title is the filename. */
private fun audioFileToSong(path: String): Song = Song(
    id = path.hashCode().toString(),
    title = path.substringAfterLast('/').substringAfterLast('\\'),
    artist = "Unknown",
    album = "Unknown",
    duration = 0L,
    thumbnailUrl = null,
    source = SongSource.LOCAL,
    localUri = path,
)

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatDurationSeconds(seconds: Long): String {
    if (seconds <= 0) return "—"
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

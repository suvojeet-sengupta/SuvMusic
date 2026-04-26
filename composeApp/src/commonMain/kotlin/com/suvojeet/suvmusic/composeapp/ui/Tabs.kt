package com.suvojeet.suvmusic.composeapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import kotlinx.coroutines.launch

/**
 * Tab composables for the Phase 5.1 Desktop shell. Intentionally light —
 * each tab is the smallest thing that justifies the navigation slot.
 * Real Android screens (HomeScreen, LibraryScreen, etc.) replace these
 * one at a time as Phase 5 progresses.
 */

// ----------------------------------------------------------------------
// Home tab — welcome + quick actions.
// ----------------------------------------------------------------------

@Composable
fun HomeTab(
    appVersion: String,
    onPickFile: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Welcome to SuvMusic",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Version $appVersion · Multiplatform build",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Quick start: pick a local audio file, or use the Search tab to find something on YouTube.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.widthIn(max = 600.dp),
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = onPickFile) {
            Text("Play a local file")
        }
    }
}

// ----------------------------------------------------------------------
// Search tab — YouTube search via NewPipe (actual hookup is in Main.kt).
// ----------------------------------------------------------------------

@Composable
fun SearchTab(
    onSearch: (suspend (String) -> List<RemoteSearchResult>)?,
    onPlayResult: suspend (RemoteSearchResult) -> Unit,
) {
    if (onSearch == null) {
        Text(
            text = "Search not wired on this build.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RemoteSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Text(
        text = "Search YouTube",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )

    Row(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
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
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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

// ----------------------------------------------------------------------
// Library tab — placeholder for "scan a folder" flow. For now only a
// "pick a file" button. Folder scanning lands in 5.2.
// ----------------------------------------------------------------------

@Composable
fun LibraryTab(
    onPickFile: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Folder scanning lands in a future update. For now, pick individual files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 600.dp),
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier.widthIn(max = 600.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pick a local file",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "MP3, FLAC, M4A, OGG, OPUS, WAV",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onPickFile) {
                    Text("Browse")
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// About tab — version + links.
// ----------------------------------------------------------------------

@Composable
fun AboutTab(
    appVersion: String,
    onOpenUrl: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "SuvMusic",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Version $appVersion",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "A YouTube Music client and local audio player. Same codebase on Android and Windows.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.widthIn(max = 600.dp),
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.widthIn(max = 600.dp))
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Created by Suvojeet Sengupta",
            style = MaterialTheme.typography.titleMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onOpenUrl("https://github.com/suvojeet-sengupta/SuvMusic") }) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("GitHub")
            }
            TextButton(onClick = { onOpenUrl("https://t.me/suvojeet_sengupta") }) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Telegram")
            }
        }
    }
}

// ----------------------------------------------------------------------
// Banner shown when MusicPlayer.isAvailable is false (no VLC installed).
// ----------------------------------------------------------------------

@Composable
fun VlcWarningBanner(onOpenUrl: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
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
                text = "SuvMusic Desktop uses VLC's playback engine (LibVLC). Install VLC media player and relaunch.",
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

// ----------------------------------------------------------------------
// Helpers exposed to App.kt — types + small utilities.
// ----------------------------------------------------------------------

/**
 * Platform-neutral search result type. The Desktop main wires this up to
 * NewPipe's StreamInfoItem; an Android-side wiring would feed it from
 * the existing YouTubeRepository.
 */
data class RemoteSearchResult(
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val url: String,
    val thumbnailUrl: String?,
)

/** Build a minimal [Song] from a local file path. Title is the filename. */
fun audioFileToSong(path: String): Song = Song(
    id = path.hashCode().toString(),
    title = path.substringAfterLast('/').substringAfterLast('\\'),
    artist = "Unknown",
    album = "Unknown",
    duration = 0L,
    thumbnailUrl = null,
    source = SongSource.LOCAL,
    localUri = path,
)

fun formatMs(ms: Long): String {
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

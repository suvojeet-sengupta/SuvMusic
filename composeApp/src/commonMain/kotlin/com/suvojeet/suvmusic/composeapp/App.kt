package com.suvojeet.suvmusic.composeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.domain.player.MusicPlayer
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource

/**
 * Top-level app composable shared between Android and Desktop. Renders
 * the About header followed by a minimal "play a local file" player
 * section — first end-to-end Desktop audio playback proof.
 *
 * The player section uses the [MusicPlayer] expect class:
 *  - On Desktop: VLCJ-backed; produces real audio if LibVLC / VLC is
 *    installed on the host machine.
 *  - On Android: a Phase 4.0 stub (logs only). Doesn't conflict with the
 *    existing :app player since this composable isn't used by the Android
 *    activity yet.
 *
 * Phase 4.3 expands this: real PlayerScreen UI, navigation, queue list,
 * library access, etc. For now: file picker, current track, play/pause,
 * seek bar.
 */
@Composable
fun App(
    appVersion: String = "0.0.0-dev",
    onOpenUrl: (String) -> Unit = {},
    onPickAudioFile: () -> String? = { null },
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

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.widthIn(max = 480.dp))
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
        Text("Pick audio file")
    }

    if (currentSong != null) {
        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.widthIn(max = 480.dp),
        )

        Slider(
            value = positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
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
            text = "No song loaded. Pick an audio file to start.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

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

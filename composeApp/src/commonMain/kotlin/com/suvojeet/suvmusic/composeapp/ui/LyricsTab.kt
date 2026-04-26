package com.suvojeet.suvmusic.composeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.image.rememberDominantColors
import com.suvojeet.suvmusic.core.domain.player.MusicPlayer

/**
 * Lyrics tab — currently a placeholder. Real fetching needs the
 * existing LyricsRepository + 4 lyric provider modules
 * (lyric-simpmusic, lyric-lrclib, lyric-kugou, BetterLyrics) ported to
 * commonMain. That's a multi-session chunk (each provider has its own
 * scraping logic). For now: shows "no lyrics" or "lyrics for current
 * song unavailable" with the dominant colour background so the UX
 * doesn't feel dead.
 *
 * The Android equivalent (app/.../ui/screens/LyricsScreen.kt) is
 * 1369 lines with synced scrolling, animated word highlight, manual
 * provider switcher, time-offset adjustment, etc. — all of that lands
 * piecewise once the provider chain is reachable from commonMain.
 */
@Composable
fun LyricsTab(player: MusicPlayer) {
    val currentSong by player.currentSong.collectAsState()
    val isDark = isSystemInDarkTheme()
    val dominant = rememberDominantColors(currentSong?.thumbnailUrl, isDarkTheme = isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(dominant.primary, dominant.secondary),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.Lyrics,
                contentDescription = null,
                tint = dominant.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text(
                text = currentSong?.title ?: "Nothing playing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = dominant.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )
            currentSong?.artist?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = dominant.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = if (currentSong == null) {
                    "Pick a song to see its lyrics."
                } else {
                    "Lyrics fetching not yet wired on Desktop. Lyrics provider modules " +
                        "(LRCLIB, KuGou, SimpMusic, BetterLyrics) port to commonMain in " +
                        "a future round."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = dominant.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )
        }
    }
}

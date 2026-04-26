package com.suvojeet.suvmusic.composeapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.suvojeet.suvmusic.composeapp.image.rememberDominantColors
import com.suvojeet.suvmusic.composeapp.ui.lyrics.Lyrics
import com.suvojeet.suvmusic.composeapp.ui.lyrics.LyricsScreen
import com.suvojeet.suvmusic.core.domain.player.MusicPlayer

/**
 * Lyrics tab — Desktop wrapper around the shared
 * [com.suvojeet.suvmusic.composeapp.ui.lyrics.LyricsScreen]. Uses the
 * dominant-colour gradient derived from the current track's album art.
 *
 * Provider chain (LRCLIB / KuGou / SimpMusic / BetterLyrics) hasn't yet
 * been ported to commonMain, so this passes a null [Lyrics] in. The
 * shared LyricsScreen renders its empty / no-song state, with the same
 * gradient background it would use when synced lyrics are present —
 * meaning the tab no longer feels dead, and the moment lyrics fetching
 * arrives the actual lines will render in place.
 */
@Composable
fun LyricsTab(player: MusicPlayer) {
    val currentSong by player.currentSong.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val isDark = isSystemInDarkTheme()
    val dominant = rememberDominantColors(currentSong?.thumbnailUrl, isDarkTheme = isDark)

    LyricsScreen(
        lyrics = null,
        isFetching = false,
        currentTimeMs = positionMs,
        songTitle = currentSong?.title.orEmpty(),
        artistName = currentSong?.artist.orEmpty(),
        onSeekTo = { player.seekTo(it) },
        onClose = { /* No-op on tab — Lyrics tab doesn't have a "close" affordance */ },
        dominantColors = dominant,
        modifier = Modifier,
    )
}

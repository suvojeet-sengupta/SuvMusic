package com.suvojeet.suvmusic.ui.screens.player

import com.suvojeet.suvmusic.core.model.Song

/**
 * Represents the currently active overlay on the Player screen.
 * Only one overlay can be visible at a time, preventing sheet "pileups"
 * and simplifying back-press handling.
 */
sealed interface PlayerOverlay {
    data object None : PlayerOverlay
    data object Queue : PlayerOverlay
    data object Lyrics : PlayerOverlay
    data object Comments : PlayerOverlay
    data class Actions(val targetSong: Song? = null) : PlayerOverlay
    data object SongInfo : PlayerOverlay
    data object SleepTimer : PlayerOverlay
    data object OutputDevice : PlayerOverlay
    data object PlaybackSpeed : PlayerOverlay
    data object ListenTogether : PlayerOverlay
    data object Equalizer : PlayerOverlay
}

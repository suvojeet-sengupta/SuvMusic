package com.suvojeet.suvmusic.composeapp.ui

/**
 * Platform-specific copy for the banner shown when [MusicPlayer.isAvailable]
 * is false. Desktop blames a missing LibVLC; Android explains that the audio
 * engine isn't initialized yet (a transient state during cold-start before
 * Application.attachBaseContext wires the player context).
 */
data class PlaybackEngineHintCopy(
    val title: String,
    val body: String,
    val detail: String? = null,
    val actionLabel: String? = null,
    val actionUrl: String? = null,
)

expect val playbackEngineHint: PlaybackEngineHintCopy

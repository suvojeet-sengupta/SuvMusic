package com.suvojeet.suvmusic.ui.utils

/**
 * Centralized shared element transition keys.
 * Using consistent keys ensures seamless animations between composables
 * that share the same visual element (e.g., album artwork across screens).
 */
object SharedTransitionKeys {

    /**
     * Key for the primary artwork that transitions between
     * MiniPlayer, Home Screen song cards, and the full Player Screen.
     *
     * Uses the song's video ID to ensure the correct artwork animates
     * when the current song changes.
     */
    fun playerArtwork(songId: String): String = "player_artwork_$songId"

    /**
     * Key for the song title text shared between MiniPlayer and Player.
     */
    fun songTitle(songId: String): String = "song_title_$songId"

    /**
     * Key for the artist name text shared between MiniPlayer and Player.
     */
    fun songArtist(songId: String): String = "song_artist_$songId"
}

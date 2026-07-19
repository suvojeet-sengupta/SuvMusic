package com.suvojeet.suvmusic.player

object AutoMix {

    /** Display title for a YouTube Music auto-mix playlist id. */
    fun resolveTitle(playlistId: String): String = when {
        playlistId.startsWith("RDAMPL") -> "Mixed For You"
        playlistId.startsWith("RDCLAK") -> "Discover Mix"
        playlistId.startsWith("RDGMUK") -> "Replay Mix"
        playlistId.startsWith("RTM") || playlistId.startsWith("RDTMAK") -> "My Supermix"
        else -> "Your Mix"
    }
}

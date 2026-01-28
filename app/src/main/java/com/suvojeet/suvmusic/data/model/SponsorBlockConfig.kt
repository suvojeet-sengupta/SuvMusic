package com.suvojeet.suvmusic.data.model

import androidx.compose.ui.graphics.Color

/**
 * Enum representing supported SponsorBlock categories with their default colors.
 * These match the standard SponsorBlock extension colors.
 */
enum class SponsorCategory(val key: String, val displayName: String, val color: Color) {
    SPONSOR("sponsor", "Sponsor", Color(0xFF00D400)),
    SELFPROMO("selfpromo", "Self Promo", Color(0xFFFFFF00)),
    INTERACTION("interaction", "Interaction (Subscribe)", Color(0xFFCC00FF)),
    INTRO("intro", "Intermission/Intro", Color(0xFF00FFFF)),
    OUTRO("outro", "Endcards/Credits", Color(0xFF020225)), // Usually distinct, but dark blue is standard for credits
    MUSIC_OFFTOPIC("music_offtopic", "Non-Music Section", Color(0xFFFF9900));

    companion object {
        fun fromKey(key: String): SponsorCategory? {
            return entries.find { it.key == key }
        }
    }
}
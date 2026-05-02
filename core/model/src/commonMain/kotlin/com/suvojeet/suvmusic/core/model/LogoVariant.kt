package com.suvojeet.suvmusic.core.model

/**
 * User-selectable app branding identity. The app surfaces this in a few
 * places — the About screen hero, the home top-bar mark, and the
 * developer-section avatar fallback — so choosing a variant changes the
 * "feel" of the app without affecting the launcher icon (which can only
 * change via icon-aliases at install time).
 *
 * [PULSE] is the default — the dark/red premium identity shipped in the
 * 2026 design refresh. [CLASSIC] preserves the original SuvMusic mark for
 * users who'd prefer the previous look.
 */
enum class LogoVariant(val displayName: String, val description: String) {
    PULSE(
        displayName = "Pulse",
        description = "Premium red/gold S monogram with a sine-wave pulse. The 2026 default.",
    ),
    RESONANCE(
        displayName = "Resonance",
        description = "Half vinyl groove, half EQ bars in royal purple and gold.",
    ),
    AETHER(
        displayName = "Aether",
        description = "Music note inside three orbital rings on cyan/violet.",
    ),
    CLASSIC(
        displayName = "Classic",
        description = "The original SuvMusic mark.",
    );

    companion object {
        val DEFAULT: LogoVariant = PULSE
    }
}

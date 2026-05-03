package com.suvojeet.suvmusic.core.model

/**
 * User-selectable app branding identity. Each entry is a (concept, style)
 * pair — three premium concepts shipped in the 2026 design refresh
 * (Pulse, Resonance, Aether), each available in 5 styles, plus a single
 * Classic entry preserving the original SuvMusic mark.
 *
 * The concept (visible via [conceptKey]) drives the launcher icon — only
 * one launcher icon ships per concept, so picking any Pulse style points
 * the launcher at the Pulse activity-alias. Style only affects the in-app
 * brand display (About hero, Home top-bar, avatar fallback) and the
 * splash screen drawable.
 *
 * [DEFAULT] is [PULSE] — the dark/red premium identity that's the 2026
 * default for new installs and unset users.
 */
enum class LogoVariant(
    val displayName: String,
    val description: String,
    /** Logical group key — picks the launcher icon and groups the picker. */
    val conceptKey: String,
) {
    // --- Pulse ---
    PULSE(
        displayName = "Pulse",
        description = "Premium red/gold S monogram with a sine-wave pulse.",
        conceptKey = "PULSE",
    ),
    PULSE_APP_ICON(
        displayName = "Pulse · App Icon",
        description = "Simplified S on a crimson squircle.",
        conceptKey = "PULSE",
    ),
    PULSE_MONO(
        displayName = "Pulse · Monochrome",
        description = "Cream S + halo on dark.",
        conceptKey = "PULSE",
    ),
    PULSE_LIGHT(
        displayName = "Pulse · On Light",
        description = "Crimson S + halo on cream.",
        conceptKey = "PULSE",
    ),
    PULSE_TONE(
        displayName = "Pulse · Single Tone",
        description = "Black S on brass.",
        conceptKey = "PULSE",
    ),

    // --- Resonance ---
    RESONANCE(
        displayName = "Resonance",
        description = "Half vinyl, half EQ bars in royal purple and gold.",
        conceptKey = "RESONANCE",
    ),
    RESONANCE_APP_ICON(
        displayName = "Resonance · App Icon",
        description = "Vinyl + EQ on midnight gradient.",
        conceptKey = "RESONANCE",
    ),
    RESONANCE_MONO(
        displayName = "Resonance · Monochrome",
        description = "Cream vinyl + EQ on dark.",
        conceptKey = "RESONANCE",
    ),
    RESONANCE_LIGHT(
        displayName = "Resonance · On Light",
        description = "Midnight violet vinyl + EQ on cream.",
        conceptKey = "RESONANCE",
    ),
    RESONANCE_TONE(
        displayName = "Resonance · Single Tone",
        description = "Black vinyl + EQ on brass.",
        conceptKey = "RESONANCE",
    ),

    // --- Aether ---
    AETHER(
        displayName = "Aether",
        description = "Music note inside three orbital rings on cyan/violet.",
        conceptKey = "AETHER",
    ),
    AETHER_APP_ICON(
        displayName = "Aether · App Icon",
        description = "Rings + note on teal-deep gradient.",
        conceptKey = "AETHER",
    ),
    AETHER_MONO(
        displayName = "Aether · Monochrome",
        description = "Cream rings + note on dark.",
        conceptKey = "AETHER",
    ),
    AETHER_LIGHT(
        displayName = "Aether · On Light",
        description = "Teal/violet rings + note on cream.",
        conceptKey = "AETHER",
    ),
    AETHER_TONE(
        displayName = "Aether · Single Tone",
        description = "Black rings + note on cyan/violet.",
        conceptKey = "AETHER",
    ),

    // --- Classic ---
    CLASSIC(
        displayName = "Classic",
        description = "The original SuvMusic mark.",
        conceptKey = "CLASSIC",
    );

    /** Human-readable concept label for picker section headers. */
    val conceptLabel: String
        get() = when (conceptKey) {
            "PULSE" -> "Pulse"
            "RESONANCE" -> "Resonance"
            "AETHER" -> "Aether"
            "CLASSIC" -> "Classic"
            else -> conceptKey
        }

    /** Short style label (without the concept prefix) for nested rows. */
    val styleLabel: String
        get() = when (this) {
            PULSE, RESONANCE, AETHER, CLASSIC -> "Hero"
            PULSE_APP_ICON, RESONANCE_APP_ICON, AETHER_APP_ICON -> "App Icon"
            PULSE_MONO, RESONANCE_MONO, AETHER_MONO -> "Monochrome"
            PULSE_LIGHT, RESONANCE_LIGHT, AETHER_LIGHT -> "On Light"
            PULSE_TONE, RESONANCE_TONE, AETHER_TONE -> "Single Tone"
        }

    companion object {
        val DEFAULT: LogoVariant = PULSE
    }
}

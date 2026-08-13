package com.suvojeet.suvmusic.core.model

/**
 * Artwork size preset for the player screen, expressed as a fraction of
 * the screen width plus a human-readable label.
 *
 * Lifted from `app/.../ui/screens/player/components/PlayerArtwork.kt` so
 * commonMain settings (ArtworkSizeScreen) can read/write it without
 * depending on Android-only player UI code.
 */
enum class ArtworkSize(val fraction: Float, val label: String) {
    SMALL(0.65f, "Small"),
    MEDIUM(0.75f, "Medium"),
    LARGE(0.85f, "Large"),
    EXTRA_LARGE(0.92f, "Extra Large"),
    FULL(1.0f, "Full Width");

    companion object {
        /**
         * The largest preset's fraction. Player layouts reserve a container of this
         * size and scale the artwork down to the selected preset, so every reference
         * point must be this value — hardcoding [LARGE] would clip anything above it.
         */
        val MAX_FRACTION: Float by lazy { entries.maxOf { it.fraction } }
    }
}

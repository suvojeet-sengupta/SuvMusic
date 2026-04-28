package com.suvojeet.suvmusic.core.model

/**
 * Artwork display shape on the player screen.
 * Pure-data enum lifted from `app/.../ui/screens/player/components/PlayerArtwork.kt`
 * so commonMain settings screens (ArtworkShapeScreen) can read/write the choice
 * without depending on Android-only player UI code.
 */
enum class ArtworkShape {
    ROUNDED_SQUARE,
    CIRCLE,
    VINYL,
    SQUARE,
}

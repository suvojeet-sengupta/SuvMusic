package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shapes — port of `app/.../ui/theme/Shapes.kt`. Lives in
 * commonMain so both Android and Desktop pull from the same shape tokens.
 *
 * Note: the Android original also exposed an `ExpressiveShapes` object
 * built from `androidx.compose.material3.MaterialShapes` (the M3
 * Expressive morph shapes — Cookie9Sided, Clover4Leaf, Arch, Fan, etc.).
 * Those symbols are marked **internal** in Compose Multiplatform 1.10's
 * material3 artifact, so they're not callable from commonMain. The
 * shapes themselves still ship; this file falls back to plain
 * RoundedCornerShape tokens until CMP exposes the M3E API as stable.
 * When that lands, restore the ExpressiveShapes object from
 * `app/.../ui/theme/Shapes.kt` lines 65–83 verbatim.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Custom shapes used throughout the app
val MusicCardShape = RoundedCornerShape(20.dp)
val PlayerCardShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 32.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp,
)
val AlbumArtShape = RoundedCornerShape(16.dp)
val PillShape = RoundedCornerShape(50)
val SquircleShape = RoundedCornerShape(28.dp)

val QuickAccessShape = RoundedCornerShape(
    topStart = 8.dp,
    bottomStart = 8.dp,
    topEnd = 20.dp,
    bottomEnd = 20.dp,
)

val NewReleaseCardShape = RoundedCornerShape(
    topStart = 20.dp,
    bottomStart = 20.dp,
    topEnd = 12.dp,
    bottomEnd = 12.dp,
)

val CardShapeToken = RoundedCornerShape(20.dp)
val SheetShapeToken = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ChipShapeToken = RoundedCornerShape(16.dp)


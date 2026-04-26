package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shapes — verbatim port of
 * `app/.../ui/theme/Shapes.kt`. Lives in commonMain so both Android and
 * Desktop pull from the same shape tokens.
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

/**
 * M3E Expressive shapes — morph-ready shapes from Material's shape library.
 * These are drawn from `androidx.compose.material3.MaterialShapes`. Compose
 * Multiplatform 1.10 + material3 1.4 surface them on commonMain.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ExpressiveShapes {
    val AlbumArt = MaterialShapes.Cookie9Sided
    val Fab = MaterialShapes.Clover4Leaf
    val MiniPlayer = MaterialShapes.Arch
    val NowPlaying = MaterialShapes.Cookie6Sided
    val ActionChip = MaterialShapes.Pill
    val GenreCard = MaterialShapes.Fan

    val Button = MaterialShapes.Pill
    val ButtonPressed = MaterialShapes.Cookie6Sided
    val FabResting = MaterialShapes.Clover4Leaf
    val FabPressed = MaterialShapes.Cookie9Sided
    val IconButton = MaterialShapes.Pill
    val IconButtonPressed = MaterialShapes.Cookie6Sided
}

val CardShapeToken = RoundedCornerShape(20.dp)
val SheetShapeToken = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ChipShapeToken = RoundedCornerShape(16.dp)

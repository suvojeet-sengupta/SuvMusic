package com.suvojeet.suvmusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shapes.
 * Combines standard Material shapes with custom music-focused variants.
 */
val Shapes = Shapes(
    // Extra Small - for small chips, elements
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small - for buttons, text fields
    small = RoundedCornerShape(12.dp),
    
    // Medium - for cards
    medium = RoundedCornerShape(16.dp),
    
    // Large - for dialogs, sheets
    large = RoundedCornerShape(24.dp),
    
    // Extra Large - for full screen containers
    extraLarge = RoundedCornerShape(32.dp)
)

// Custom shapes used throughout the app
val MusicCardShape = RoundedCornerShape(20.dp)
val PlayerCardShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 32.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
val AlbumArtShape = RoundedCornerShape(16.dp)
val PillShape = RoundedCornerShape(50)
val SquircleShape = RoundedCornerShape(28.dp)

// Asymmetric — QuickAccess card (image left, text right)
val QuickAccessShape = RoundedCornerShape(
    topStart = 8.dp,
    bottomStart = 8.dp,
    topEnd = 20.dp,
    bottomEnd = 20.dp
)

// Asymmetric — NewRelease card (text left, image right)
val NewReleaseCardShape = RoundedCornerShape(
    topStart = 20.dp,
    bottomStart = 20.dp,
    topEnd = 12.dp,
    bottomEnd = 12.dp
)

/**
 * M3E Expressive shapes — morph-ready shapes from Material's shape library.
 * Use these for elements that benefit from organic, expressive forms:
 *   - Album art → Cookie9Sided gives a playful, distinctive look
 *   - FABs → Clover4Leaf morphs beautifully on press
 *   - Mini player indicator → Arch for a flowing, musical feel
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ExpressiveShapes {
    // Existing artwork + surface shapes
    val AlbumArt = MaterialShapes.Cookie9Sided
    val Fab = MaterialShapes.Clover4Leaf
    val MiniPlayer = MaterialShapes.Arch
    val NowPlaying = MaterialShapes.Cookie6Sided
    val ActionChip = MaterialShapes.Pill
    val GenreCard = MaterialShapes.Fan

    // Morph pairs — semantic aliases for press-state shape morphing.
    // Resting → Pressed pairs drive the M3 Expressive "squish on tap" motion.
    val Button = MaterialShapes.Pill
    val ButtonPressed = MaterialShapes.Cookie6Sided
    val FabResting = MaterialShapes.Clover4Leaf
    val FabPressed = MaterialShapes.Cookie9Sided
    val IconButton = MaterialShapes.Pill
    val IconButtonPressed = MaterialShapes.Cookie6Sided
}

// Standard-shape semantic aliases — use when you need a plain RoundedCornerShape
// that matches our token scale, without opting into M3E morph shapes.
val CardShapeToken = RoundedCornerShape(20.dp)
val SheetShapeToken = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ChipShapeToken = RoundedCornerShape(16.dp)

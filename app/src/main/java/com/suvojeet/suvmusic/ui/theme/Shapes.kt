package com.suvojeet.suvmusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shapes.
 * Embraces tension by mixing corner types.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Shapes = Shapes(
    extraSmall = ShapeDefaults.ExtraSmall,   // 4.dp rounded
    small      = ShapeDefaults.Small,        // 8.dp rounded  
    medium     = ShapeDefaults.Medium,       // 12.dp rounded
    large      = ShapeDefaults.Large,        // 16.dp rounded
    extraLarge = ShapeDefaults.ExtraLarge,   // 28.dp rounded
)

// Custom expressive shapes
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

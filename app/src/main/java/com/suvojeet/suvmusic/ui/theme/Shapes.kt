package com.suvojeet.suvmusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shapes.
 * Embraces tension by mixing corner types.
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

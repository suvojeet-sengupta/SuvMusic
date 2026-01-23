package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.LoadingArtworkOverlay
import com.suvojeet.suvmusic.ui.screens.player.getHighResThumbnail

/**
 * Artwork display modes
 */
enum class ArtworkShape {
    ROUNDED_SQUARE,  // Default 16dp corners
    CIRCLE,          // Circular artwork
    VINYL,           // Vinyl record style with hole in center
    SQUARE           // Sharp corners
}

/**
 * Artwork size options for player screen
 */
enum class ArtworkSize(val fraction: Float, val label: String) {
    SMALL(0.65f, "Small"),
    MEDIUM(0.75f, "Medium"),
    LARGE(0.85f, "Large")
}

@Composable
fun AlbumArtwork(
    imageUrl: String?,
    title: String?,
    dominantColors: DominantColors,
    isLoading: Boolean = false,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    initialShape: ArtworkShape = ArtworkShape.ROUNDED_SQUARE,
    artworkSize: ArtworkSize = ArtworkSize.LARGE,
    onShapeChange: ((ArtworkShape) -> Unit)? = null,
    onDoubleTapLeft: () -> Unit = {},
    onDoubleTapRight: () -> Unit = {}
) {
    val context = LocalContext.current

    // Shape state - uses initial shape from settings
    var currentShape by remember { mutableStateOf(initialShape) }
    var showShapeMenu by remember { mutableStateOf(false) }
    
    // Sync with external shape changes
    LaunchedEffect(initialShape) {
        currentShape = initialShape
    }
    
    // Vinyl rotation animation (only for vinyl mode)
    var vinylRotation by remember { mutableStateOf(0f) }
    val animatedVinylRotation by animateFloatAsState(
        targetValue = vinylRotation,
        animationSpec = tween(durationMillis = 100),
        label = "vinyl_rotation"
    )
    
    // Animate corner radius based on shape - use coerceAtLeast to prevent negative values
    val targetCornerRadius = when (currentShape) {
        ArtworkShape.ROUNDED_SQUARE -> 16.dp
        ArtworkShape.CIRCLE, ArtworkShape.VINYL -> 500.dp // Very large for circle
        ArtworkShape.SQUARE -> 0.dp
    }
    
    val cornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy, // Prevent overshooting into negative
            stiffness = Spring.StiffnessLow
        ),
        label = "corner_radius"
    )
    
    // Ensure corner radius is never negative
    val safeCornerRadius = cornerRadius.coerceAtLeast(0.dp)
    
    // Track horizontal drag offset
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 300f

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = if (offsetX == 0f) 200 else 0),
        label = "swipe_offset"
    )

    val scale = 1f - (kotlin.math.abs(animatedOffsetX) / 1500f).coerceIn(0f, 0.1f)
    val rotation = animatedOffsetX / 30f

    val currentOnDoubleTapLeft by rememberUpdatedState(onDoubleTapLeft)
    val currentOnDoubleTapRight by rememberUpdatedState(onDoubleTapRight)
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)

    BoxWithConstraints {
        val isWideLayout = maxWidth > 500.dp

        Box(
            modifier = Modifier
                .then(
                    if (isWideLayout) {
                        Modifier.fillMaxHeight(artworkSize.fraction).aspectRatio(1f)
                    } else {
                        Modifier.fillMaxWidth(artworkSize.fraction).aspectRatio(1f)
                    }
                )
                .graphicsLayer {
                    translationX = animatedOffsetX
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation + if (currentShape == ArtworkShape.VINYL) animatedVinylRotation else 0f
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                currentOnDoubleTapLeft()
                            } else {
                                currentOnDoubleTapRight()
                            }
                        },
                        onLongPress = {
                            showShapeMenu = true
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX < -swipeThreshold -> currentOnSwipeLeft()
                                offsetX > swipeThreshold -> currentOnSwipeRight()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX += dragAmount
                            // Rotate vinyl while dragging
                            if (currentShape == ArtworkShape.VINYL) {
                                vinylRotation += dragAmount * 0.1f
                            }
                        }
                    )
                }
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(safeCornerRadius),
                    spotColor = dominantColors.primary.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(safeCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Main artwork image
            if (imageUrl != null) {
                // Try high-res first, fallback to original on error
                var model by remember(imageUrl) { mutableStateOf<Any?>(getHighResThumbnail(imageUrl)) }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(model)
                        .crossfade(true)
                        .size(600)
                        .listener(
                            onError = { _, _ ->
                                if (model != imageUrl) {
                                    model = imageUrl
                                }
                            }
                        )
                        .build(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Vinyl center hole overlay
            if (currentShape == ArtworkShape.VINYL) {
                VinylCenterOverlay(dominantColors = dominantColors)
            }

            LoadingArtworkOverlay(isVisible = isLoading)
        }
        
        // Shape selection popup menu
        if (showShapeMenu) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showShapeMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                ShapeSelectionMenu(
                    currentShape = currentShape,
                    dominantColors = dominantColors,
                    onShapeSelected = { shape ->
                        currentShape = shape
                        onShapeChange?.invoke(shape)
                        showShapeMenu = false
                    },
                    onDismiss = { showShapeMenu = false }
                )
            }
        }
    }
}

/**
 * Vinyl record center hole overlay
 */
@Composable
private fun VinylCenterOverlay(dominantColors: DominantColors) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.7f),
                            dominantColors.primary.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(2.dp, dominantColors.accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner hole
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }
        
        // Vinyl grooves effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.1f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Shape selection menu popup
 */
@Composable
private fun ShapeSelectionMenu(
    currentShape: ArtworkShape,
    dominantColors: DominantColors,
    onShapeSelected: (ArtworkShape) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Artwork Style",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShapeOption(
                    icon = Icons.Rounded.RoundedCorner,
                    label = "Rounded",
                    isSelected = currentShape == ArtworkShape.ROUNDED_SQUARE,
                    accentColor = dominantColors.accent,
                    onClick = { onShapeSelected(ArtworkShape.ROUNDED_SQUARE) }
                )
                
                ShapeOption(
                    icon = Icons.Default.Circle,
                    label = "Circle",
                    isSelected = currentShape == ArtworkShape.CIRCLE,
                    accentColor = dominantColors.accent,
                    onClick = { onShapeSelected(ArtworkShape.CIRCLE) }
                )
                
                ShapeOption(
                    icon = Icons.Default.Album,
                    label = "Vinyl",
                    isSelected = currentShape == ArtworkShape.VINYL,
                    accentColor = dominantColors.accent,
                    onClick = { onShapeSelected(ArtworkShape.VINYL) }
                )
                
                ShapeOption(
                    icon = Icons.Default.Square,
                    label = "Square",
                    isSelected = currentShape == ArtworkShape.SQUARE,
                    accentColor = dominantColors.accent,
                    onClick = { onShapeSelected(ArtworkShape.SQUARE) }
                )
            }
        }
    }
}

/**
 * Individual shape option button
 */
@Composable
private fun ShapeOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg_color"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "icon_color"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        animationSpec = spring(),
        label = "border_color"
    )
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor,
            fontSize = 10.sp
        )
    }
}

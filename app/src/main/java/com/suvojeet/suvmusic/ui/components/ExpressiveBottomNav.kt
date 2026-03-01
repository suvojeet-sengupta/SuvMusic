package com.suvojeet.suvmusic.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.navigation.Destination

/**
 * iOS 26-style Liquid Glass Bottom Navigation Bar.
 *
 * Features a floating pill-shaped navbar with multi-layered glass material:
 * - Frosted translucent base with adaptive tint
 * - Specular highlight gradient (top-to-bottom light reflection)
 * - Luminous gradient border rim
 * - Soft diffused shadow for depth
 * - Animated selected indicator with spring physics
 * - Dark/light theme adaptive glass appearance
 *
 * Uses Material You dynamic theming for colors that adapt to wallpaper.
 */
@Composable
fun ExpressiveBottomNav(
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 0.85f,
    iosLiquidGlassEnabled: Boolean = false
) {
    val navItems = listOf(
        BottomNavItem(Destination.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem(Destination.Search, "Search", Icons.Outlined.Search, Icons.Filled.Search),
        BottomNavItem(Destination.Library, "Your Library", Icons.Outlined.ViewWeek, Icons.Filled.ViewWeek),
        BottomNavItem(Destination.Settings, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    )

    if (iosLiquidGlassEnabled) {
        LiquidGlassNavBar(
            navItems = navItems,
            currentDestination = currentDestination,
            onDestinationChange = onDestinationChange,
            modifier = modifier
        )
    } else {
        StandardNavBar(
            navItems = navItems,
            currentDestination = currentDestination,
            onDestinationChange = onDestinationChange,
            modifier = modifier,
            alpha = alpha
        )
    }
}

// ─── iOS Liquid Glass Navigation Bar ─────────────────────────────────────────

@Composable
private fun LiquidGlassNavBar(
    navItems: List<BottomNavItem>,
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val glassShape = RoundedCornerShape(26.dp)

    // Glass material colors – adaptive for dark/light
    val glassBaseAlpha = if (isDarkTheme) 0.32f else 0.55f
    val glassBaseColor = if (isDarkTheme) {
        surfaceColor.copy(alpha = glassBaseAlpha)
    } else {
        Color.White.copy(alpha = glassBaseAlpha)
    }

    // Specular highlight (simulates light hitting the top of the glass)
    val specularHighlight = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = if (isDarkTheme) 0.14f else 0.35f),
        0.35f to Color.White.copy(alpha = if (isDarkTheme) 0.04f else 0.10f),
        0.55f to Color.Transparent,
        1.0f to Color.Black.copy(alpha = if (isDarkTheme) 0.06f else 0.02f)
    )

    // Border rim – luminous on top, fading to transparent
    val borderBrush = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = if (isDarkTheme) 0.28f else 0.50f),
        0.5f to Color.White.copy(alpha = if (isDarkTheme) 0.08f else 0.18f),
        1.0f to Color.White.copy(alpha = if (isDarkTheme) 0.03f else 0.06f)
    )

    // Inner tint – subtle color wash from primary/accent
    val innerTintColor = primaryColor.copy(alpha = if (isDarkTheme) 0.06f else 0.04f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        // Layer 1: Soft shadow for depth and floating feel
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = if (isDarkTheme) 24.dp else 16.dp,
                    shape = glassShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.35f else 0.12f),
                    spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.25f else 0.08f)
                )
        )

        // Layer 2: Frosted glass base – blurred semi-transparent fill
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(glassShape)
                .background(glassBaseColor)
                .then(
                    if (Build.VERSION.SDK_INT >= 31) {
                        Modifier.blur(40.dp)
                    } else {
                        Modifier.blur(20.dp)
                    }
                )
        )

        // Layer 3: Glass tint overlay – subtle color from theme
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(glassShape)
                .background(innerTintColor)
        )

        // Layer 4: Specular highlight – light reflection gradient
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(glassShape)
                .background(specularHighlight)
        )

        // Layer 5: Inner edge glow – faint top highlight line
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(glassShape)
                .drawBehind {
                    // Top inner glow line
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = if (isDarkTheme) 0.10f else 0.18f),
                            0.08f to Color.Transparent
                        ),
                        size = Size(size.width, size.height * 0.15f)
                    )
                }
        )

        // Layer 6: Border rim
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 0.5.dp,
                    brush = borderBrush,
                    shape = glassShape
                )
        )

        // Layer 7: Content – nav items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentDestination == item.destination
                LiquidGlassNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onDestinationChange(item.destination) },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Animated content color
    val selectedTextColor = MaterialTheme.colorScheme.onSurface
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "glassNavColor"
    )

    // Animated scale – subtle bounce on selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glassNavScale"
    )

    // Animated indicator background alpha
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "glassIndicatorAlpha"
    )

    // Selected indicator pill colors
    val indicatorColor = if (isDarkTheme) {
        primaryColor.copy(alpha = 0.18f * indicatorAlpha)
    } else {
        primaryColor.copy(alpha = 0.12f * indicatorAlpha)
    }

    val indicatorBorderColor = if (isDarkTheme) {
        primaryColor.copy(alpha = 0.22f * indicatorAlpha)
    } else {
        primaryColor.copy(alpha = 0.15f * indicatorAlpha)
    }

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Selected indicator pill
            .background(
                color = indicatorColor,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 0.5.dp,
                        color = indicatorBorderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )

            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.3.sp
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

// ─── Standard (Non-Glass) Navigation Bar ─────────────────────────────────────

@Composable
private fun StandardNavBar(
    navItems: List<BottomNavItem>,
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 0.85f
) {
    val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = alpha)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentDestination == item.destination
                StandardNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onDestinationChange(item.destination) }
                )
            }
        }
    }
}

@Composable
private fun StandardNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contentColor = if (isSelected) selectedColor else unselectedColor

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier.size(26.dp),
                tint = contentColor
            )

            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = contentColor
            )
        }
    }
}

private data class BottomNavItem(
    val destination: Destination,
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

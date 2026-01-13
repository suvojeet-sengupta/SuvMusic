package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.navigation.Destination
import com.suvojeet.suvmusic.ui.theme.Cyan50
import com.suvojeet.suvmusic.ui.theme.GradientEnd
import com.suvojeet.suvmusic.ui.theme.GradientMiddle
import com.suvojeet.suvmusic.ui.theme.GradientStart
import com.suvojeet.suvmusic.ui.theme.GlassWhite
import com.suvojeet.suvmusic.ui.theme.Magenta50
import com.suvojeet.suvmusic.ui.theme.Purple50

// iOS-style Liquid Glass Colors
private val LiquidGlassBackground = Color(0xFF1A1A1E)
private val LiquidGlassOverlay = Color(0x12FFFFFF)
private val LiquidGlassBorder = Color(0x20FFFFFF)
private val LiquidGlassHighlight = Color(0x30FFFFFF)
private val LiquidGlassInnerGlow = Color(0x08FFFFFF)
private val PrismaticBlue = Color(0xFF6EC6FF)
private val PrismaticPurple = Color(0xFFB388FF)
private val PrismaticPink = Color(0xFFFF80AB)
private val PrismaticCyan = Color(0xFF84FFFF)

/**
 * iOS-style Liquid Glass Bottom Navigation.
 * Features glassmorphism with blur effects, prismatic reflections,
 * and smooth spring animations.
 */
@Composable
fun ExpressiveBottomNav(
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem(Destination.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavItem(Destination.Search, "Search", Icons.Outlined.Search, Icons.Filled.Search),
        NavItem(Destination.Library, "Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
        NavItem(Destination.Settings, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    )
    
    val glassShape = RoundedCornerShape(28.dp)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        // Main glass container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 24.dp,
                    shape = glassShape,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(glassShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            LiquidGlassBackground.copy(alpha = 0.85f),
                            LiquidGlassBackground.copy(alpha = 0.95f)
                        )
                    )
                )
                // Prismatic glass overlay effect
                .drawBehind {
                    // Top highlight shimmer
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LiquidGlassHighlight,
                                Color.Transparent,
                                LiquidGlassHighlight.copy(alpha = 0.05f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height * 0.3f)
                        )
                    )
                    // Subtle prismatic rainbow reflection
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                PrismaticBlue.copy(alpha = 0.03f),
                                PrismaticPurple.copy(alpha = 0.04f),
                                PrismaticPink.copy(alpha = 0.03f),
                                PrismaticCyan.copy(alpha = 0.03f),
                                PrismaticBlue.copy(alpha = 0.02f)
                            )
                        )
                    )
                }
                .background(LiquidGlassOverlay)
                // Glass border with gradient
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            LiquidGlassBorder.copy(alpha = 0.5f),
                            LiquidGlassBorder.copy(alpha = 0.1f),
                            LiquidGlassBorder.copy(alpha = 0.3f)
                        )
                    ),
                    shape = glassShape
                )
        ) {
            // Inner content glow layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LiquidGlassInnerGlow,
                                Color.Transparent
                            ),
                            radius = 600f
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentDestination == item.destination
                        
                        LiquidGlassNavItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onDestinationChange(item.destination) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Smooth spring animations for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 26.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconSize"
    )
    
    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 64.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillWidth"
    )
    
    val pillHeight by animateDpAsState(
        targetValue = if (isSelected) 36.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillHeight"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "contentColor"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "glowAlpha"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection pill with liquid glass effect
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .height(pillHeight)
                    .graphicsLayer { alpha = glowAlpha }
                    // Outer glow for selection
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Purple50.copy(alpha = 0.4f),
                        spotColor = Cyan50.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    // Gradient background with glass effect
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Purple50.copy(alpha = 0.6f),
                                Magenta50.copy(alpha = 0.5f),
                                Cyan50.copy(alpha = 0.4f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    // Inner glass overlay
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
            )
        }
        
        // Icon and label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(28.dp)
            ) {
                // Subtle glow behind selected icon
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer { alpha = glowAlpha * 0.5f }
                            .blur(8.dp, BlurredEdgeTreatment.Unbounded)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }
                
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    modifier = Modifier.size(iconSize),
                    tint = contentColor
                )
            }
            
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = contentColor
                )
            )
        }
    }
}

private data class NavItem(
    val destination: Destination,
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.navigation.Destination

/**
 * iOS-style Liquid Glass Bottom Navigation with Dynamic Colors.
 * Uses Material You dynamic theming for colors that adapt to wallpaper.
 */
@Composable
fun ExpressiveBottomNav(
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 0.9f
) {
    val navItems = listOf(
        NavItem(Destination.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavItem(Destination.Search, "Search", Icons.Outlined.Search, Icons.Filled.Search),
        NavItem(Destination.Library, "Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
        NavItem(Destination.Settings, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    )
    
    // Spotify-style transparent dark background
    // Glass Bottom Navigation with Dynamic Colors
    val transparentBg = MaterialTheme.colorScheme.surface.copy(alpha = alpha)
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(transparentBg)
            .drawBehind {
                // Top border line - subtle glass edge
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.5f
                )
            }
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentDestination == item.destination
                
                GlassNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onDestinationChange(item.destination) }
                )
            }
        }
    }
}

@Composable
private fun GlassNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Dynamic colors from Material You
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconSize"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else onSurfaceVariant,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "contentColor"
    )
    
    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "pillAlpha"
    )
    
    // Use primary color with transparency for the pill
    val pillColor = primaryColor.copy(alpha = 0.15f)
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer { alpha = 1f }
                    .drawBehind {
                        // Subtle pill background for selected state with dynamic color
                        if (pillAlpha > 0f) {
                            drawRoundRect(
                                color = pillColor.copy(alpha = pillColor.alpha * pillAlpha),
                                cornerRadius = CornerRadius(28f, 28f),
                                size = Size(size.width + 24.dp.toPx(), size.height + 12.dp.toPx()),
                                topLeft = Offset(-12.dp.toPx(), -6.dp.toPx())
                            )
                        }
                    }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    modifier = Modifier.size(iconSize),
                    tint = contentColor
                )
            }
            
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
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

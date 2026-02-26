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
import androidx.compose.ui.unit.sp
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
        BottomNavItem(Destination.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem(Destination.Search, "Search", Icons.Outlined.Search, Icons.Filled.Search),
        BottomNavItem(Destination.Library, "Your Library", Icons.Outlined.ViewWeek, Icons.Filled.ViewWeek),
        BottomNavItem(Destination.Settings, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    )
    
    // Spotify-style transparent dark background
    // Use MaterialTheme.colorScheme.surface with alpha for transparency
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
                .padding(horizontal = 8.dp, vertical = 2.dp), // Reduced vertical padding
            horizontalArrangement = Arrangement.SpaceAround, // SpaceAround for better spacing
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
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Adaptive colors based on theme
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
            .padding(12.dp), // Increased touch target, reduced visual constraints
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier.size(26.dp), // Fixed size
                tint = contentColor
            )
            
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), // Smaller text
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

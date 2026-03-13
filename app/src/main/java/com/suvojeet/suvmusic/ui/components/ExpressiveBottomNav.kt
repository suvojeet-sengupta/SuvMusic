package com.suvojeet.suvmusic.ui.components

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.navigation.Destination

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveBottomNav(
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
    iosLiquidGlassEnabled: Boolean = false,
    backgroundColor: Color? = null
) {
    val navItems = listOf(
        BottomNavItem(Destination.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem(Destination.Search, "Search", Icons.Outlined.Search, Icons.Filled.Search),
        BottomNavItem(Destination.Library, "Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
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
        StandardNavBarM3E(
            navItems = navItems,
            currentDestination = currentDestination,
            onDestinationChange = onDestinationChange,
            modifier = modifier,
            alpha = alpha,
            backgroundColor = backgroundColor
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StandardNavBarM3E(
    navItems: List<BottomNavItem>,
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
    backgroundColor: Color? = null
) {
    val containerColor = backgroundColor ?: MaterialTheme.colorScheme.surface.copy(alpha = alpha)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .navigationBarsPadding(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentDestination == item.destination
                M3ENavigationItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onDestinationChange(item.destination) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3ENavigationItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "nav_item_scale"
    )

    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(interactionSource, indication = null) { onClick() }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(64.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer 
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                        .togetherWith(scaleOut(tween(80)))
                },
                label = "nav_icon_${item.label}"
            ) { selected ->
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Keep LiquidGlassNavBar for variety, but enhance it slightly
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

    val glassBaseAlpha = if (isDarkTheme) 0.32f else 0.55f
    val glassBaseColor = if (isDarkTheme) surfaceColor.copy(alpha = glassBaseAlpha) else Color.White.copy(alpha = glassBaseAlpha)

    val specularHighlight = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = if (isDarkTheme) 0.14f else 0.35f),
        0.35f to Color.White.copy(alpha = if (isDarkTheme) 0.04f else 0.10f),
        0.55f to Color.Transparent,
        1.0f to Color.Black.copy(alpha = if (isDarkTheme) 0.06f else 0.02f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
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

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(glassShape)
                .background(glassBaseColor)
                .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(40.dp) else Modifier.blur(20.dp))
        )

        Box(modifier = Modifier.matchParentSize().clip(glassShape).background(specularHighlight))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentDestination == item.destination
                LiquidGlassNavItemM3E(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onDestinationChange(item.destination) },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LiquidGlassNavItemM3E(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isSelected) 1.06f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "glassNavScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        label = "glassNavColor"
    )

    Box(
        modifier = Modifier
            .clickable(interactionSource, indication = null, onClick = onClick)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)).togetherWith(scaleOut(tween(80)))
                },
                label = "glass_nav_icon"
            ) { selected ->
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor
                )
            }

            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = contentColor,
                maxLines = 1
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

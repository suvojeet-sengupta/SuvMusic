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
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.navigation.Destination
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.random.Random

/**
 * iOS 26-style Liquid Glass Bottom Navigation Bar.
 *
 * Features a floating pill-shaped navbar with multi-layered glass material:
 * - Frosted translucent base with adaptive tint
 * - Specular highlight gradient (top-to-bottom light reflection)
 * - Luminous gradient border rim
 * - Soft diffused shadow for depth
 * - Morphing selected indicator with spring physics
 * - Dark/light theme adaptive glass appearance
 *
 * Uses Material You dynamic theming for colors that adapt to wallpaper.
 */
@Composable
fun ExpressiveBottomNav(
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    onReClick: (Destination) -> Unit = {},
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
    iosLiquidGlassEnabled: Boolean = false,
    backgroundColor: Color? = null,
    iosNavBarBlur: Float = 60f
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
            onReClick = onReClick,
            modifier = modifier,
            blurAmount = iosNavBarBlur
        )
    } else {
        StandardNavBar(
            navItems = navItems,
            currentDestination = currentDestination,
            onDestinationChange = onDestinationChange,
            onReClick = onReClick,
            modifier = modifier,
            alpha = alpha,
            backgroundColor = backgroundColor
        )
    }
}

// ─── iOS Liquid Glass Navigation Bar ─────────────────────────────────────────

@Composable
private fun LiquidGlassNavBar(
    navItems: List<BottomNavItem>,
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    onReClick: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    blurAmount: Float = 60f
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val glassShape = RoundedCornerShape(28.dp)
    val density = LocalDensity.current

    // Glass material colors – adaptive for dark/light
    val glassBaseAlpha = if (isDarkTheme) 0.45f else 0.35f
    val glassBaseColor = if (isDarkTheme) {
        surfaceColor.copy(alpha = glassBaseAlpha)
    } else {
        Color.White.copy(alpha = glassBaseAlpha)
    }

    // Specular highlight (simulates light hitting the top of the glass)
    val specularHighlight = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = if (isDarkTheme) 0.18f else 0.25f),
        0.3f to Color.White.copy(alpha = if (isDarkTheme) 0.06f else 0.08f),
        0.5f to Color.Transparent,
        1.0f to Color.Black.copy(alpha = if (isDarkTheme) 0.08f else 0.02f)
    )

    // Border rim – luminous on top, fading to transparent
    val borderBrush = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = if (isDarkTheme) 0.35f else 0.45f),
        0.4f to Color.White.copy(alpha = if (isDarkTheme) 0.12f else 0.15f),
        1.0f to Color.White.copy(alpha = if (isDarkTheme) 0.05f else 0.08f)
    )

    // Inner tint – subtle color wash from primary/accent
    val innerTintColor = primaryColor.copy(alpha = if (isDarkTheme) 0.08f else 0.04f)

    // Track positions for morphing indicator
    var selectedItemIndex by remember { mutableIntStateOf(0) }
    var itemWidths by remember { mutableStateOf(FloatArray(navItems.size)) }
    var itemPositions by remember { mutableStateOf(FloatArray(navItems.size)) }
    
    selectedItemIndex = navItems.indexOfFirst { it.destination == currentDestination }.coerceAtLeast(0)

    val indicatorX by animateFloatAsState(
        targetValue = if (itemPositions.isNotEmpty()) itemPositions[selectedItemIndex] else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "indicatorX"
    )
    
    val indicatorWidth by animateFloatAsState(
        targetValue = if (itemWidths.isNotEmpty()) itemWidths[selectedItemIndex] else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "indicatorWidth"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(72.dp)
    ) {
        // Layer 1: Enhanced shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = if (isDarkTheme) 32.dp else 20.dp,
                    shape = glassShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.15f),
                    spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.35f else 0.10f)
                )
        )

        // Layer 2: Glass material stack
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(glassShape)
                .then(
                    if (Build.VERSION.SDK_INT >= 31) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                blurAmount,
                                blurAmount,
                                android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                        }
                    } else {
                        Modifier.blur((blurAmount / 2).dp)
                    }
                )
                .drawWithContent {
                    // Draw frosted background
                    drawRect(color = glassBaseColor)
                    
                    // Draw noise/grain texture for realistic glass
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            alpha = (if (isDarkTheme) 8 else 5)
                        }
                        val random = Random(42)
                        for (i in 0 until 1000) {
                            val x = random.nextFloat() * size.width
                            val y = random.nextFloat() * size.height
                            canvas.nativeCanvas.drawPoint(x, y, paint)
                        }
                    }
                    
                    drawContent()
                }
        )

        // Layer 3: Color tint & Specular highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(glassShape)
                .background(innerTintColor)
                .background(specularHighlight)
        )

        // Layer 4: Morphing Indicator
        if (indicatorWidth > 0) {
            Box(
                modifier = Modifier
                    .offset(x = with(density) { indicatorX.toDp() })
                    .width(with(density) { indicatorWidth.toDp() })
                    .fillMaxHeight()
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .background(
                        color = primaryColor.copy(alpha = if (isDarkTheme) 0.15f else 0.12f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = primaryColor.copy(alpha = if (isDarkTheme) 0.25f else 0.18f),
                        shape = RoundedCornerShape(22.dp)
                    )
            )
        }

        // Layer 5: Luminous rim
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 0.8.dp,
                    brush = borderBrush,
                    shape = glassShape
                )
        )

        // Layer 6: Items
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEachIndexed { index, item ->
                val isSelected = currentDestination == item.destination
                LiquidGlassNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { 
                        if (currentDestination == item.destination) {
                            onReClick(item.destination)
                        } else {
                            onDestinationChange(item.destination)
                        }
                    },
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coords ->
                            val newWidths = itemWidths.copyOf()
                            newWidths[index] = coords.size.width.toFloat()
                            itemWidths = newWidths
                            
                            val newPositions = itemPositions.copyOf()
                            newPositions[index] = coords.positionInParent().x
                            itemPositions = newPositions
                        }
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
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface 
                      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(300),
        label = "itemColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "itemScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    letterSpacing = 0.2.sp
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
    onReClick: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
    backgroundColor: Color? = null
) {
    val containerColor = if (alpha >= 1.0f && backgroundColor != null) {
        backgroundColor
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = alpha)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
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
                    onClick = {
                        if (currentDestination == item.destination) {
                            onReClick(item.destination)
                        } else {
                            onDestinationChange(item.destination)
                        }
                    }
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

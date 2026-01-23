package com.suvojeet.suvmusic.ui.screens

import androidx.compose.material3.Divider

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkShape
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkSize
import com.suvojeet.suvmusic.ui.theme.GradientEnd
import com.suvojeet.suvmusic.ui.theme.GradientMiddle
import com.suvojeet.suvmusic.ui.theme.GradientStart
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.luminance
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel

/**
 * Customization settings screen for player appearance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(), // Injected for dynamic colors
    onBack: () -> Unit,
    onSeekbarStyleClick: () -> Unit = {},
    onArtworkShapeClick: () -> Unit = {},
    onArtworkSizeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    // Player State for Dynamic Colors
    val playerState by playerViewModel.playerState.collectAsState()
    val currentSong = playerState.currentSong
    val isAppInDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // Dynamic Colors
    val dominantColors = rememberDominantColors(
        imageUrl = currentSong?.thumbnailUrl,
        isDarkTheme = isAppInDarkTheme
    )
    
    val seekbarStyleString by sessionManager.seekbarStyleFlow.collectAsState(initial = "WAVEFORM")
    val artworkShapeString by sessionManager.artworkShapeFlow.collectAsState(initial = "ROUNDED_SQUARE")
    
    val currentSeekbarStyle = try {
        SeekbarStyle.valueOf(seekbarStyleString)
    } catch (e: Exception) {
        SeekbarStyle.WAVEFORM
    }
    
    val currentArtworkShape = try {
        ArtworkShape.valueOf(artworkShapeString)
    } catch (e: Exception) {
        ArtworkShape.ROUNDED_SQUARE
    }
    
    val artworkSizeString by sessionManager.artworkSizeFlow.collectAsState(initial = "LARGE")
    val currentArtworkSize = try {
        ArtworkSize.valueOf(artworkSizeString)
    } catch (e: Exception) {
        ArtworkSize.LARGE
    }

    val miniPlayerAlpha by sessionManager.miniPlayerAlphaFlow.collectAsState(initial = 1f)
    val navBarAlpha by sessionManager.navBarAlphaFlow.collectAsState(initial = 0.9f)
    val scope = rememberCoroutineScope()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Use dominant colors if available, otherwise fallback to theme
    val primaryColor = if (currentSong != null) dominantColors.accent else MaterialTheme.colorScheme.primary
    val surfaceColor = if (currentSong != null) dominantColors.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
    val backgroundColor = if (currentSong != null) dominantColors.primary else MaterialTheme.colorScheme.background
    val onBackgroundColor = if (currentSong != null) dominantColors.onBackground else MaterialTheme.colorScheme.onBackground
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = backgroundColor,
        topBar = {
            LargeTopAppBar(
                title = { Text("Customization", color = onBackgroundColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = onBackgroundColor)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = backgroundColor.copy(alpha = 0.9f),
                    titleContentColor = onBackgroundColor
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Section: Player Appearance
            CustomizationSection(title = "Player Appearance", color = primaryColor) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = surfaceColor.copy(alpha = 0.5f),
                        contentColor = onBackgroundColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        SeekbarStyleNavigationItem(
                            currentStyle = currentSeekbarStyle,
                            primaryColor = primaryColor,
                            contentColor = onBackgroundColor,
                            onClick = onSeekbarStyleClick
                        )
                        
                        Divider(color = onBackgroundColor.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                        
                        ArtworkShapeNavigationItem(
                            currentShape = currentArtworkShape,
                            primaryColor = primaryColor,
                            contentColor = onBackgroundColor,
                            onClick = onArtworkShapeClick
                        )
                        
                        Divider(color = onBackgroundColor.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

                        ArtworkSizeNavigationItem(
                            currentSize = currentArtworkSize,
                            primaryColor = primaryColor,
                            contentColor = onBackgroundColor,
                            onClick = onArtworkSizeClick
                        )
                    }
                }
            }
            
            // Section: UI Customization
            CustomizationSection(title = "Interface", color = primaryColor) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = surfaceColor.copy(alpha = 0.5f),
                        contentColor = onBackgroundColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mini Player Transparency
                        TransparencySliderItem(
                            title = "Mini Player Transparency",
                            icon = Icons.Default.MusicNote,
                            alpha = miniPlayerAlpha,
                            primaryColor = primaryColor,
                            contentColor = onBackgroundColor,
                            onAlphaChange = { newAlpha ->
                                scope.launch {
                                    sessionManager.setMiniPlayerAlpha(newAlpha)
                                }
                            }
                        )

                        Divider(color = onBackgroundColor.copy(alpha = 0.1f))

                        // Navigation Bar Transparency
                        TransparencySliderItem(
                            title = "Navigation Bar Transparency",
                            icon = Icons.Default.Square,
                            alpha = navBarAlpha,
                            primaryColor = primaryColor,
                            contentColor = onBackgroundColor,
                            onAlphaChange = { newAlpha ->
                                scope.launch {
                                    sessionManager.setNavBarAlpha(newAlpha)
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info text
            Text(
                text = "Pro Tip: You can also change these settings directly from the player by long-pressing on the seekbar or artwork.",
                style = MaterialTheme.typography.bodySmall,
                color = onBackgroundColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun CustomizationSection(
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun SeekbarStyleNavigationItem(
    currentStyle: SeekbarStyle,
    primaryColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val styleName = when (currentStyle) {
        SeekbarStyle.WAVEFORM -> "Waveform"
        SeekbarStyle.WAVE_LINE -> "Wave Line"
        SeekbarStyle.CLASSIC -> "Classic"
        SeekbarStyle.DOTS -> "Dots"
        SeekbarStyle.GRADIENT_BAR -> "Gradient"
        SeekbarStyle.MATERIAL -> "Material 3"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Seekbar Style",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor
            )
            Text(
                text = styleName,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = contentColor.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ArtworkShapeNavigationItem(
    currentShape: ArtworkShape,
    primaryColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val shapeName = when (currentShape) {
        ArtworkShape.ROUNDED_SQUARE -> "Rounded"
        ArtworkShape.CIRCLE -> "Circle"
        ArtworkShape.VINYL -> "Vinyl"
        ArtworkShape.SQUARE -> "Square"
    }
    
    val icon = when (currentShape) {
        ArtworkShape.ROUNDED_SQUARE -> Icons.Rounded.RoundedCorner
        ArtworkShape.CIRCLE -> Icons.Default.Circle
        ArtworkShape.VINYL -> Icons.Default.Album
        ArtworkShape.SQUARE -> Icons.Default.Square
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Artwork Shape",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor
            )
            Text(
                text = shapeName,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = contentColor.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ArtworkSizeNavigationItem(
    currentSize: ArtworkSize,
    primaryColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple size indicator boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val boxCount = when (currentSize) {
                ArtworkSize.SMALL -> 1
                ArtworkSize.MEDIUM -> 2
                ArtworkSize.LARGE -> 3
            }
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index < boxCount) 10.dp else 8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < boxCount) primaryColor 
                            else contentColor.copy(alpha = 0.2f)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(18.dp)) // Aligned with other icons which are 24dp (boxes are approx 24dp wide total)
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Artwork Size",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor
            )
            Text(
                text = currentSize.label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = contentColor.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun TransparencySliderItem(
    title: String,
    icon: ImageVector,
    alpha: Float,
    primaryColor: Color,
    contentColor: Color,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = contentColor
                )
                Text(
                    text = "Opacity: ${(alpha * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        androidx.compose.material3.Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            valueRange = 0f..1f,
            steps = 20,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SeekbarStylePreviewCard(
    style: SeekbarStyle,
    isSelected: Boolean,
    primaryColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(),
        label = "border"
    )
    
    val styleName = when (style) {
        SeekbarStyle.WAVEFORM -> "Waveform"
        SeekbarStyle.WAVE_LINE -> "Wave Line"
        SeekbarStyle.CLASSIC -> "Classic"
        SeekbarStyle.DOTS -> "Dots"
        SeekbarStyle.GRADIENT_BAR -> "Gradient"
        SeekbarStyle.MATERIAL -> "Material 3"
    }
    
    val previewAmplitudes = remember { List(25) { Random.nextFloat() * 0.6f + 0.4f } }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview canvas
            Canvas(
                modifier = Modifier
                    .width(100.dp)
                    .height(40.dp)
            ) {
                val progress = 0.6f
                when (style) {
                    SeekbarStyle.WAVEFORM -> drawWaveformPreview(progress, previewAmplitudes, primaryColor, surfaceColor)
                    SeekbarStyle.WAVE_LINE -> drawWaveLinePreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.CLASSIC -> drawClassicPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.DOTS -> drawDotsPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.GRADIENT_BAR -> drawGradientPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.MATERIAL -> drawClassicPreview(progress, primaryColor, surfaceColor) // Reuse classic preview for Material since it's similar (bar + thumb)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = styleName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                )
                
                if (isSelected) {
                    Text(
                        text = "Current style",
                        style = MaterialTheme.typography.bodySmall,
                        color = primaryColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtworkShapeCard(
    shape: ArtworkShape,
    isSelected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(),
        label = "border"
    )
    
    val (shapeName, icon) = when (shape) {
        ArtworkShape.ROUNDED_SQUARE -> "Rounded" to Icons.Rounded.RoundedCorner
        ArtworkShape.CIRCLE -> "Circle" to Icons.Default.Circle
        ArtworkShape.VINYL -> "Vinyl" to Icons.Default.Album
        ArtworkShape.SQUARE -> "Square" to Icons.Default.Square
    }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = shapeName,
                tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = shapeName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Preview drawing functions
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveformPreview(
    progress: Float,
    amplitudes: List<Float>,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val barWidth = width / amplitudes.size
    val maxBarHeight = height * 0.8f
    val progressX = progress * width
    
    amplitudes.forEachIndexed { index, amplitude ->
        val x = index * barWidth + barWidth / 2
        val isPast = x < progressX
        val barHeight = amplitude * maxBarHeight
        val topY = centerY - barHeight / 2
        
        drawRoundRect(
            color = if (isPast) activeColor else inactiveColor.copy(alpha = 0.4f),
            topLeft = Offset(x - barWidth * 0.3f, topY),
            size = Size(barWidth * 0.6f, barHeight),
            cornerRadius = CornerRadius(barWidth * 0.3f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveLinePreview(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val progressX = progress * width
    val amplitude = height * 0.3f
    
    val path = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        while (x <= width) {
            val y = centerY + sin(x * 0.1f) * amplitude
            lineTo(x, y.toFloat())
            x += 3f
        }
    }
    
    drawPath(path = path, color = inactiveColor, style = Stroke(width = 2.dp.toPx()))
    
    val playedPath = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        while (x <= progressX) {
            val y = centerY + sin(x * 0.1f) * amplitude
            lineTo(x, y.toFloat())
            x += 3f
        }
    }
    
    drawPath(path = playedPath, color = activeColor, style = Stroke(width = 2.dp.toPx()))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClassicPreview(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val trackHeight = 4.dp.toPx()
    
    drawRoundRect(
        color = inactiveColor.copy(alpha = 0.3f),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
    
    drawRoundRect(
        color = activeColor,
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(progress * width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDotsPreview(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val progressX = progress * width
    val dotCount = 15
    val dotSpacing = width / dotCount
    
    for (i in 0 until dotCount) {
        val x = i * dotSpacing + dotSpacing / 2
        val isPast = x < progressX
        drawCircle(
            color = if (isPast) activeColor else inactiveColor.copy(alpha = 0.4f),
            radius = 3.dp.toPx(),
            center = Offset(x, centerY)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGradientPreview(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val trackHeight = 5.dp.toPx()
    
    drawRoundRect(
        color = inactiveColor.copy(alpha = 0.2f),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
    
    drawRoundRect(
        brush = Brush.horizontalGradient(colors = listOf(GradientStart, GradientMiddle, GradientEnd)),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(progress * width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
}

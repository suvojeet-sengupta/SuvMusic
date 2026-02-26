package com.suvojeet.suvmusic.ui.screens

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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Customization settings screen for player appearance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSeekbarStyleClick: () -> Unit = {},
    onArtworkShapeClick: () -> Unit = {},
    onArtworkSizeClick: () -> Unit = {},
    showStyleSheet: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    val seekbarStyleString by sessionManager.seekbarStyleFlow.collectAsState(initial = "WAVE_LINE")
    val artworkShapeString by sessionManager.artworkShapeFlow.collectAsState(initial = "ROUNDED_SQUARE")
    
    val currentSeekbarStyle = try {
        SeekbarStyle.valueOf(seekbarStyleString)
    } catch (e: Exception) {
        SeekbarStyle.WAVE_LINE
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

    val miniPlayerAlpha by sessionManager.miniPlayerAlphaFlow.collectAsState(initial = 0f)
    val navBarAlpha by sessionManager.navBarAlphaFlow.collectAsState(initial = 0.85f)
    val currentMiniPlayerStyle by sessionManager.miniPlayerStyleFlow.collectAsState(initial = com.suvojeet.suvmusic.data.model.MiniPlayerStyle.FLOATING_PILL)

    val scope = rememberCoroutineScope()
    
    // Style Selection Dialog/Sheet
    var showMiniPlayerStyleSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    if (showMiniPlayerStyleSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showMiniPlayerStyleSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
             Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Mini Player Style",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                com.suvojeet.suvmusic.data.model.MiniPlayerStyle.entries.forEach { style ->
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(style.label) },
                        leadingContent = {
                            androidx.compose.material3.RadioButton(
                                selected = currentMiniPlayerStyle == style,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    sessionManager.setMiniPlayerStyle(style)
                                    showMiniPlayerStyleSheet = false
                                }
                            },
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }


    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Customization") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Section: Player Design
            CustomizationSection(title = "Player Design") {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                ) {
                    Column {
                        SettingItem(
                            title = "Seekbar Style",
                            subtitle = formatSeekbarStyleName(currentSeekbarStyle),
                            icon = Icons.Default.GraphicEq,
                            onClick = onSeekbarStyleClick
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        SettingItem(
                            title = "Artwork Shape",
                            subtitle = formatArtworkShapeName(currentArtworkShape),
                            icon = getArtworkShapeIcon(currentArtworkShape),
                            onClick = onArtworkShapeClick
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingItem(
                            title = "Artwork Size",
                            subtitle = currentArtworkSize.label,
                            icon = Icons.Default.Image, // Generic icon or custom one
                            customIcon = { ArtworkSizeIndicator(currentArtworkSize) },
                            onClick = onArtworkSizeClick
                        )
                    }
                }
            }
            
            // Section: Interface Transparency
            CustomizationSection(title = "Interface Transparency") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Preview Card
                    MiniPlayerPreview(alpha = miniPlayerAlpha, style = currentMiniPlayerStyle)

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Mini Player Transparency
                            TransparencySliderItem(
                                title = "Mini Player Transparency",
                                icon = Icons.Default.MusicNote,
                                alpha = 1f - miniPlayerAlpha, // Show transparency
                                onAlphaChange = { transparency ->
                                    scope.launch {
                                        // Store opacity
                                        sessionManager.setMiniPlayerAlpha(1f - transparency)
                                    }
                                }
                            )

                            HorizontalDivider()

                            // Nav Bar Transparency
                            TransparencySliderItem(
                                title = "Navigation Bar Transparency",
                                icon = Icons.Default.Square, // Or a more suitable icon
                                alpha = 1f - navBarAlpha, // Show transparency to user
                                onAlphaChange = { transparency ->
                                    scope.launch {
                                        // Store opacity (1 - transparency)
                                        sessionManager.setNavBarAlpha(1f - transparency)
                                    }
                                }
                            )

                            HorizontalDivider()

                            // Mini Player Style
                            SettingItem(
                                title = "Mini Player Style",
                                subtitle = currentMiniPlayerStyle.label,
                                icon = Icons.Default.MusicNote, 
                                onClick = { showMiniPlayerStyleSheet = true }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info text
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "You can also change these settings directly from the player by long-pressing on the seekbar or artwork.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CustomizationSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    customIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customIcon != null) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                customIcon()
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ArtworkSizeIndicator(size: ArtworkSize) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val boxCount = when (size) {
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
                        if (index < boxCount) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun TransparencySliderItem(
    title: String,
    icon: ImageVector,
    alpha: Float,
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${(alpha * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            valueRange = 0f..0.85f,
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun MiniPlayerPreview(alpha: Float, style: com.suvojeet.suvmusic.data.model.MiniPlayerStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Mock background content to show transparency
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                )
            }
        }

        // Mini Player Mockup
        val isPill = style == com.suvojeet.suvmusic.data.model.MiniPlayerStyle.FLOATING_PILL
        val shape = if (isPill) RoundedCornerShape(32.dp) else RoundedCornerShape(14.dp)
        val horizontalPadding = if (isPill) 24.dp else 12.dp
        
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isPill) 0.85f else 0.95f)
                .height(if (isPill) 56.dp else 64.dp)
                .clip(shape),
            color = Color.Transparent,
            shape = shape
        ) {
            val effectiveAlpha = alpha
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = effectiveAlpha),
                                MaterialTheme.colorScheme.secondary.copy(alpha = effectiveAlpha)
                            )
                        )
                    )
                    .padding(horizontal = horizontalPadding),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(if (isPill) 38.dp else 42.dp)
                            .clip(if (isPill) CircleShape else RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Box(modifier = Modifier.width(100.dp).height(8.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.width(60.dp).height(6.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}

private fun formatSeekbarStyleName(style: SeekbarStyle): String {
    return when (style) {
        SeekbarStyle.WAVEFORM -> "Waveform"
        SeekbarStyle.WAVE_LINE -> "Wave Line"
        SeekbarStyle.CLASSIC -> "Classic"
        SeekbarStyle.DOTS -> "Dots"
        SeekbarStyle.GRADIENT_BAR -> "Gradient"
        SeekbarStyle.MATERIAL -> "Material 3"
    }
}

private fun formatArtworkShapeName(shape: ArtworkShape): String {
    return when (shape) {
        ArtworkShape.ROUNDED_SQUARE -> "Rounded Square"
        ArtworkShape.CIRCLE -> "Circle"
        ArtworkShape.VINYL -> "Vinyl"
        ArtworkShape.SQUARE -> "Square"
    }
}

private fun getArtworkShapeIcon(shape: ArtworkShape): ImageVector {
    return when (shape) {
        ArtworkShape.ROUNDED_SQUARE -> Icons.Rounded.RoundedCorner
        ArtworkShape.CIRCLE -> Icons.Default.Circle
        ArtworkShape.VINYL -> Icons.Default.Album
        ArtworkShape.SQUARE -> Icons.Default.Square
    }
}

// Keeping the preview drawing functions as they might be needed for other screens or sub-screens
// (Though they are not used in the main list anymore, keeping them to prevent breaking if other files depend on them or for future use in detail screens)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveformPreview(
    progress: Float,
    amplitudes: List<Float>,
    activeColor: Color,
    inactiveColor: Color
) {
    // ... (Existing implementation kept safe if needed, or can be removed if strictly unused. 
    // Since this file was replacing content, and I am not including the preview cards in the main screen anymore, 
    // I will include them if they were used by the detail screens which might be in this file.
    // Wait, the previous file had them as private functions at the bottom.
    // The previous file defined `SeekbarStylePreviewCard` which used them. 
    // That component was not used in the main screen in the previous code either? 
    // Ah, it seems `SeekbarStyleScreen` might be a separate file, but `CustomizationScreen` had these previews defined.
    // I will keep them to be safe, but I won't use them in the main view for now as requested "organized" view.)
    
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

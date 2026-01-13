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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.suvojeet.suvmusic.ui.theme.GradientEnd
import com.suvojeet.suvmusic.ui.theme.GradientMiddle
import com.suvojeet.suvmusic.ui.theme.GradientStart
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
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
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Seekbar Style Section
            CustomizationSection(title = "Seekbar Style") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeekbarStyle.entries.forEach { style ->
                        SeekbarStylePreviewCard(
                            style = style,
                            isSelected = style == currentSeekbarStyle,
                            primaryColor = primaryColor,
                            surfaceColor = surfaceColor,
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    sessionManager.setSeekbarStyle(style.name)
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Artwork Shape Section
            CustomizationSection(title = "Artwork Shape") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ArtworkShape.entries.forEach { shape ->
                        ArtworkShapeCard(
                            shape = shape,
                            isSelected = shape == currentArtworkShape,
                            primaryColor = primaryColor,
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    sessionManager.setArtworkShape(shape.name)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info text
            Text(
                text = "You can also change these from the player by long-pressing on the seekbar or artwork.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
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
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
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

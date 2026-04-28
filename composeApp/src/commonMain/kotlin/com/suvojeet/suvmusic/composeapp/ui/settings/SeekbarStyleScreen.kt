package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.theme.GradientEnd
import com.suvojeet.suvmusic.composeapp.theme.GradientMiddle
import com.suvojeet.suvmusic.composeapp.theme.GradientStart
import com.suvojeet.suvmusic.core.model.SeekbarStyle
import kotlin.math.sin
import kotlin.random.Random

/**
 * Seekbar style picker — ported from
 * `app/.../ui/screens/SeekbarStyleScreen.kt` to commonMain.
 *
 * Stateless: takes the current style + onSelect callback. The :app side
 * keeps SessionManager/DataStore wiring. No Scaffold/TopAppBar — host
 * owns chrome.
 *
 * Each row renders a small Canvas preview of how the seekbar will look.
 * MATERIAL falls back to the CLASSIC preview (matches Android).
 */
@Composable
fun SeekbarStyleScreen(
    current: SeekbarStyle,
    onSelect: (SeekbarStyle) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Choose how the seekbar appears on the player screen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SeekbarStyle.entries.forEach { style ->
            SeekbarStylePreviewCard(
                style = style,
                isSelected = style == current,
                primaryColor = primaryColor,
                surfaceColor = surfaceColor,
                onClick = { onSelect(style) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You can also change this from the player by long-pressing on the seekbar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun SeekbarStylePreviewCard(
    style: SeekbarStyle,
    isSelected: Boolean,
    primaryColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(),
        label = "border",
    )

    val styleName = when (style) {
        SeekbarStyle.WAVEFORM -> "Waveform"
        SeekbarStyle.WAVE_LINE -> "Wave Line"
        SeekbarStyle.CLASSIC -> "Classic"
        SeekbarStyle.DOTS -> "Dots"
        SeekbarStyle.GRADIENT_BAR -> "Gradient"
        SeekbarStyle.NEON -> "Neon Glow"
        SeekbarStyle.BLOCKS -> "Blocks"
        SeekbarStyle.MATERIAL -> "Material 3"
        SeekbarStyle.M3E_WAVY -> "M3 Expressive"
    }

    val previewAmplitudes = remember { List(25) { Random.nextFloat() * 0.6f + 0.4f } }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.width(120.dp).height(48.dp)) {
                val progress = 0.6f
                when (style) {
                    SeekbarStyle.WAVEFORM -> drawWaveformPreview(progress, previewAmplitudes, primaryColor, surfaceColor)
                    SeekbarStyle.WAVE_LINE -> drawWaveLinePreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.CLASSIC -> drawClassicPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.DOTS -> drawDotsPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.GRADIENT_BAR -> drawGradientPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.NEON -> drawNeonPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.BLOCKS -> drawBlocksPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.MATERIAL -> drawClassicPreview(progress, primaryColor, surfaceColor)
                    SeekbarStyle.M3E_WAVY -> drawM3EWavyPreview(progress, primaryColor, surfaceColor)
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = styleName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                )

                if (isSelected) {
                    Text(
                        text = "Current style",
                        style = MaterialTheme.typography.bodySmall,
                        color = primaryColor.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNeonPreview(progress: Float, activeColor: Color, inactiveColor: Color) {
    val centerY = size.height / 2
    val stroke = 3.dp.toPx()
    drawLine(inactiveColor.copy(alpha = 0.3f), Offset(0f, centerY), Offset(size.width, centerY), stroke, cap = StrokeCap.Round)
    drawLine(activeColor, Offset(0f, centerY), Offset(progress * size.width, centerY), stroke, cap = StrokeCap.Round)
    drawCircle(activeColor, radius = 5.dp.toPx(), center = Offset(progress * size.width, centerY))
}

private fun DrawScope.drawBlocksPreview(progress: Float, activeColor: Color, inactiveColor: Color) {
    val count = 10
    val w = size.width / count
    for (i in 0 until count) {
        val color = if (progress >= (i + 1).toFloat() / count) activeColor else inactiveColor.copy(alpha = 0.3f)
        drawRoundRect(
            color = color,
            topLeft = Offset(i * w + 1.dp.toPx(), size.height * 0.2f),
            size = Size(w - 2.dp.toPx(), size.height * 0.6f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}

private fun DrawScope.drawWaveformPreview(
    progress: Float,
    amplitudes: List<Float>,
    activeColor: Color,
    inactiveColor: Color,
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
            cornerRadius = CornerRadius(barWidth * 0.3f),
        )
    }
}

private fun DrawScope.drawWaveLinePreview(progress: Float, activeColor: Color, inactiveColor: Color) {
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
            lineTo(x, y)
            x += 3f
        }
    }
    drawPath(path = path, color = inactiveColor, style = Stroke(width = 2.dp.toPx()))

    val playedPath = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        while (x <= progressX) {
            val y = centerY + sin(x * 0.1f) * amplitude
            lineTo(x, y)
            x += 3f
        }
    }
    drawPath(path = playedPath, color = activeColor, style = Stroke(width = 2.dp.toPx()))
}

private fun DrawScope.drawClassicPreview(progress: Float, activeColor: Color, inactiveColor: Color) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val trackHeight = 4.dp.toPx()

    drawRoundRect(
        color = inactiveColor.copy(alpha = 0.3f),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )

    drawRoundRect(
        color = activeColor,
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(progress * width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )
}

private fun DrawScope.drawDotsPreview(progress: Float, activeColor: Color, inactiveColor: Color) {
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
            center = Offset(x, centerY),
        )
    }
}

private fun DrawScope.drawGradientPreview(progress: Float, activeColor: Color, inactiveColor: Color) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val trackHeight = 5.dp.toPx()

    drawRoundRect(
        color = inactiveColor.copy(alpha = 0.2f),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )

    drawRoundRect(
        brush = Brush.horizontalGradient(colors = listOf(GradientStart, GradientMiddle, GradientEnd)),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(progress * width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )
}

private fun DrawScope.drawM3EWavyPreview(progress: Float, activeColor: Color, inactiveColor: Color) {
    val centerY = size.height / 2
    val amplitude = size.height * 0.22f
    val frequency = 0.10f
    val strokePx = 3.dp.toPx()

    val inactivePath = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        while (x <= size.width) {
            val y = centerY + sin(x * frequency) * amplitude
            lineTo(x, y)
            x += 2f
        }
    }
    drawPath(
        path = inactivePath,
        color = inactiveColor.copy(alpha = 0.25f),
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
    )

    val activePath = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        while (x <= progress * size.width) {
            val y = centerY + sin(x * frequency) * amplitude
            lineTo(x, y)
            x += 2f
        }
    }
    drawPath(
        path = activePath,
        color = activeColor,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
    )
}

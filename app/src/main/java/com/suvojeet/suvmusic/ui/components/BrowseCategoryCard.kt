package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.data.model.BrowseCategory

/**
 * Category color palette - vibrant gradients inspired by Apple Music
 */
private val categoryGradients = listOf(
    listOf(Color(0xFFE91E63), Color(0xFF9C27B0)), // Pink to Purple
    listOf(Color(0xFF00BCD4), Color(0xFF3F51B5)), // Cyan to Indigo
    listOf(Color(0xFFFF5722), Color(0xFFE91E63)), // Orange to Pink
    listOf(Color(0xFF4CAF50), Color(0xFF00BCD4)), // Green to Cyan
    listOf(Color(0xFF9C27B0), Color(0xFF673AB7)), // Purple to Deep Purple
    listOf(Color(0xFFFF9800), Color(0xFFFF5722)), // Orange shades
    listOf(Color(0xFF2196F3), Color(0xFF00BCD4)), // Blue to Cyan
    listOf(Color(0xFFE91E63), Color(0xFFFF5722)), // Pink to Orange
    listOf(Color(0xFF673AB7), Color(0xFF2196F3)), // Deep Purple to Blue
    listOf(Color(0xFF795548), Color(0xFF9C27B0)), // Brown to Purple
    listOf(Color(0xFF009688), Color(0xFF4CAF50)), // Teal to Green
    listOf(Color(0xFFF44336), Color(0xFFE91E63)), // Red to Pink
)

/**
 * Beautiful browse category card with gradient background.
 * Used in the Apple Music-style search screen grid.
 */
@Composable
fun BrowseCategoryCard(
    category: BrowseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )
    
    // Select gradient based on index or derive from category color
    val gradient = if (category.color != null) {
        val baseColor = Color(category.color or 0xFF000000) // Ensure alpha
        val darkerColor = baseColor.copy(
            red = (baseColor.red * 0.7f).coerceIn(0f, 1f),
            green = (baseColor.green * 0.7f).coerceIn(0f, 1f),
            blue = (baseColor.blue * 0.7f).coerceIn(0f, 1f)
        )
        listOf(baseColor, darkerColor)
    } else {
        categoryGradients[index % categoryGradients.size]
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(gradient)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        // Subtle dark overlay at bottom for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        
        Text(
            text = category.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(12.dp)
        )
    }
}

package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MoodChipsSection(
    selectedMood: String?,
    onMoodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods = remember {
        listOf(
            "Sleep", "Relax", "Sad", "Romance", 
            "Feel Good", "Party", "Focus", "Energize"
        )
    }
    
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        moods.forEach { mood ->
            item(key = mood) {
                MoodChip(
                    mood = mood,
                    isSelected = mood == selectedMood,
                    onClick = { onMoodSelected(mood) }
                )
            }
        }
    }
}

@Composable
fun MoodChip(
    mood: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Animate scale for a "pop" effect when selected
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "selectionScale"
    )

    val backgroundColor = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f) 

    val textColor = if (isSelected) 
        MaterialTheme.colorScheme.onPrimary 
    else 
        MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = selectionScale
                scaleY = selectionScale
            }
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Glow behind the selected chip
        if (isSelected) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                drawRoundRect(
                    color = backgroundColor,
                    size = size.copy(width = size.width + 12.dp.toPx(), height = size.height + 6.dp.toPx()),
                    topLeft = androidx.compose.ui.geometry.Offset(-6.dp.toPx(), -3.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    alpha = 0.4f
                )
            }
        }

        Surface(
            modifier = Modifier.height(38.dp),
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor,
            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    text = mood,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy, 
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "bounce"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

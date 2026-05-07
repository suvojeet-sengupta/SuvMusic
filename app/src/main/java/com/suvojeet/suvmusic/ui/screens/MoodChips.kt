package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mood chips, redesigned. Each mood ships its own emoji glyph and an
 * accent color, and chips alternate between filled (selected/even) and
 * outlined (odd) treatments so the row reads as a curated palette
 * instead of a uniform `FilterChip` strip.
 */
private data class Mood(val name: String, val emoji: String, val accent: Color)

private val MOODS = listOf(
    Mood("Sleep",     "🌙", Color(0xFF4A4E96)),
    Mood("Relax",     "🍃", Color(0xFF67B26F)),
    Mood("Sad",       "💧", Color(0xFF4682B4)),
    Mood("Romance",   "🌹", Color(0xFFE91E63)),
    Mood("Feel Good", "✨", Color(0xFFFFB347)),
    Mood("Party",     "🎉", Color(0xFFFF5E62)),
    Mood("Focus",     "🎯", Color(0xFF6A2C70)),
    Mood("Energize",  "⚡", Color(0xFFFFC107))
)

@Composable
fun MoodChipsSection(
    selectedMood: String?,
    onMoodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(MOODS, key = { _, m -> m.name }) { index, mood ->
            MoodChip(
                mood = mood,
                isSelected = mood.name == selectedMood,
                isOutlined = index % 2 == 1,
                onClick = { onMoodSelected(mood.name) }
            )
        }
    }
}

@Composable
private fun MoodChip(
    mood: Mood,
    isSelected: Boolean,
    isOutlined: Boolean,
    onClick: () -> Unit
) {
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "moodChipScale"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "moodChipPress"
    )

    val isFilledStyle = isSelected || !isOutlined
    val containerColor = when {
        isSelected -> mood.accent
        isFilledStyle -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color.Transparent
    }
    val labelColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .scale(selectionScale * pressScale)
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .then(
                if (!isFilledStyle) Modifier.border(
                    1.5.dp,
                    mood.accent.copy(alpha = 0.6f),
                    RoundedCornerShape(50)
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(start = 6.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Emoji on a circular tinted disc — gives the chip a "tag" feel.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color.White.copy(alpha = 0.22f)
                    else mood.accent.copy(alpha = 0.18f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = mood.emoji, fontSize = 16.sp)
        }
        Text(
            text = mood.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = labelColor
        )
    }
}

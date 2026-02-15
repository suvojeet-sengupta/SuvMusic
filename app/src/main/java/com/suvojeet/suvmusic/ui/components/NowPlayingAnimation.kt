package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NowPlayingAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    isPlaying: Boolean = true,
    barCount: Int = 3,
    barWidth: Dp = 4.dp,
    maxBarHeight: Dp = 18.dp,
    minBarHeight: Dp = 4.dp
) {
    Row(
        modifier = modifier.height(maxBarHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { index ->
            val transition = rememberInfiniteTransition(label = "bar_$index")
            
            // Staggered durations for more natural look
            val duration = when (index % 3) {
                0 -> 400
                1 -> 600
                else -> 500
            }
            
            val height by if (isPlaying) {
                transition.animateValue(
                    initialValue = minBarHeight,
                    targetValue = maxBarHeight,
                    typeConverter = Dp.VectorConverter,
                    animationSpec = infiniteRepeatable(
                        animation = tween(duration, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_height_$index"
                )
            } else {
                remember { mutableStateOf(when(index % 3) {
                    0 -> maxBarHeight * 0.4f
                    1 -> maxBarHeight * 0.7f
                    else -> maxBarHeight * 0.5f
                }) }
            }

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(height)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color)
            )
        }
    }
}

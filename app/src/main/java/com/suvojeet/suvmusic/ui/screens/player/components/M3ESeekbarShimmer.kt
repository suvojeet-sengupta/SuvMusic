// File: app/src/main/java/com/suvojeet/suvmusic/ui/screens/player/components/M3ESeekbarShimmer.kt
package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.components.DominantColors

@Composable
fun M3ESeekbarShimmer(
    isVisible: Boolean,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(spring(Spring.DampingRatioNoBouncy)),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            dominantColors.accent.copy(alpha = 0.1f),
                            dominantColors.accent.copy(alpha = 0.5f),
                            dominantColors.accent.copy(alpha = 0.1f)
                        ),
                        startX = shimmerX * 1000f - 500f,
                        endX = shimmerX * 1000f + 500f
                    )
                )
        )
    }
}

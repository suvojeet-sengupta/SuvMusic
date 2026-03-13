// File: app/src/main/java/com/suvojeet/suvmusic/ui/screens/player/components/M3ELoadingOverlay.kt
package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.components.DominantColors

/**
 * M3E Expressive loading overlay for song loading state.
 * Uses the new bouncy LoadingIndicator instead of CircularProgressIndicator.
 *
 * Show this OVER the artwork when PlayerState.isLoading == true.
 */
@Composable
fun M3ELoadingOverlay(
    isLoading: Boolean,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialScale = 0.7f
        ),
        exit = fadeOut() + scaleOut(targetScale = 0.7f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            // Glass-morphism container for the loader
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(dominantColors.primary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                // M3E Expressive LoadingIndicator — bouncy, animated dots
                LoadingIndicator(
                    modifier = Modifier.size(48.dp),
                    color = dominantColors.accent
                )
            }
        }
    }
}

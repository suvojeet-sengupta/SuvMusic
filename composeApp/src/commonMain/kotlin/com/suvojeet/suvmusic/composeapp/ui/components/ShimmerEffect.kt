package com.suvojeet.suvmusic.composeapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer placeholder set — verbatim port of
 * `app/.../ui/components/ShimmerEffect.kt`. Single-source-of-truth for
 * skeleton screens across both Android and Desktop. Used by Home, Library,
 * Search, Artist, Album loading states.
 */

private val shimmerColors = listOf(
    Color(0xFF2A2A2A),
    Color(0xFF3A3A3A),
    Color(0xFF2A2A2A),
)

/**
 * Modifier that paints a sliding gradient shimmer behind any content. The
 * animation runs forever via [rememberInfiniteTransition], driving the
 * gradient endpoint diagonally across the bounds.
 */
fun Modifier.shimmerBackground(
    shape: Shape = RoundedCornerShape(0.dp),
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    this.clip(shape).drawWithCache {
        onDrawBehind {
            val translate = translateAnim.value
            drawRect(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset.Zero,
                    end = Offset(x = translate, y = translate),
                ),
            )
        }
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 20.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shimmerBackground(shape),
    )
}

@Composable
fun FeaturedPlaylistSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .shimmerBackground(RoundedCornerShape(16.dp)),
    )
}

@Composable
fun CompactMusicCardSkeleton() {
    Column(modifier = Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shimmerBackground(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(width = 120.dp, height = 14.dp)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(width = 80.dp, height = 12.dp)
    }
}

@Composable
fun PlaylistCardSkeleton() {
    Column(modifier = Modifier.width(150.dp)) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .shimmerBackground(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(width = 130.dp, height = 14.dp)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(width = 90.dp, height = 12.dp)
    }
}

@Composable
fun MusicCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shimmerBackground(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(width = 180.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(width = 120.dp, height = 14.dp)
        }
    }
}

@Composable
fun SectionHeaderSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(width = 120.dp, height = 24.dp, modifier = modifier)
}

@Composable
fun HomeCardSkeleton() {
    Column(modifier = Modifier.width(160.dp)) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .shimmerBackground(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(width = 140.dp, height = 16.dp)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(width = 100.dp, height = 14.dp)
    }
}

@Composable
fun HomeLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(width = 200.dp, height = 32.dp)
        }

        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeaderSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false,
                ) {
                    items(List(4) { it }) { HomeCardSkeleton() }
                }
            }
        }
    }
}

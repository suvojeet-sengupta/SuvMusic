package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect colors
 */
private val shimmerColors = listOf(
    Color(0xFF2A2A2A),
    Color(0xFF3A3A3A),
    Color(0xFF2A2A2A)
)

/**
 * Shared shimmer progress (0f..1000f). When a group of skeletons is wrapped in
 * [ShimmerContainer], they all read from a single [rememberInfiniteTransition]
 * instead of each allocating their own. `null` means no shared container is present,
 * in which case [shimmerBackground] falls back to a per-box transition (unchanged
 * standalone behavior).
 */
private val LocalShimmerProgress = compositionLocalOf<State<Float>?> { null }

/**
 * Provides a single shared shimmer animation for all descendant skeletons.
 * Wrap large clusters of shimmering placeholders (e.g. the home loading grid) with
 * this so ~12 boxes share one infinite transition rather than one each.
 */
@Composable
fun ShimmerContainer(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer_shared")
    // Keep this as a State<Float> (no `by`) so it can be provided down and read in the
    // DRAW phase (state.value) without a composable context — reading it there animates
    // the shimmer without recomposing any descendant.
    val translateAnim: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_shared"
    )
    CompositionLocalProvider(LocalShimmerProgress provides translateAnim) {
        content()
    }
}

/**
 * Modifier that applies a shimmer effect background without triggering recomposition.
 *
 * If rendered inside a [ShimmerContainer] it consumes the shared animation value;
 * otherwise it spins up its own transition (preserving prior standalone behavior).
 */
fun Modifier.shimmerBackground(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp)
): Modifier = composed {
    val shared = LocalShimmerProgress.current
    // Whether a shared container is present is stable for a given call-site instance,
    // so branching on it here does not violate the rules of composition. Either way we end
    // up with a State<Float> that is read in the DRAW phase (progress.value) — never at
    // composition — so the animation drives redraws without recomposition.
    val progress: State<Float> = shared ?: run {
        val transition = rememberInfiniteTransition(label = "shimmer")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1200,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )
    }

    this.clip(shape).drawWithCache {
        onDrawBehind {
            val translate = progress.value
            drawRect(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset.Zero,
                    end = Offset(x = translate, y = translate)
                )
            )
        }
    }
}

/**
 * Shimmer box placeholder
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 20.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shimmerBackground(shape)
    )
}

/**
 * Featured playlist skeleton
 */
@Composable
fun FeaturedPlaylistSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .shimmerBackground(RoundedCornerShape(16.dp))
    )
}

/**
 * Compact music card skeleton (for horizontal scroll)
 */
@Composable
fun CompactMusicCardSkeleton() {
    Column(
        modifier = Modifier.width(140.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(140.dp)
                .shimmerBackground(RoundedCornerShape(12.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        ShimmerBox(width = 120.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Artist
        ShimmerBox(width = 80.dp, height = 12.dp)
    }
}

/**
 * Playlist card skeleton
 */
@Composable
fun PlaylistCardSkeleton() {
    Column(
        modifier = Modifier.width(150.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(150.dp)
                .shimmerBackground(RoundedCornerShape(12.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        ShimmerBox(width = 130.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Subtitle
        ShimmerBox(width = 90.dp, height = 12.dp)
    }
}

/**
 * Music card skeleton (full width)
 */
@Composable
fun MusicCardSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .shimmerBackground(RoundedCornerShape(8.dp))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // Title
            ShimmerBox(width = 180.dp, height = 16.dp)
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Artist
            ShimmerBox(width = 120.dp, height = 14.dp)
        }
    }
}

/**
 * Section header skeleton
 */
@Composable
fun SectionHeaderSkeleton(
    modifier: Modifier = Modifier
) {
    ShimmerBox(
        width = 120.dp,
        height = 24.dp,
        modifier = modifier
    )
}

/**
 * Home card skeleton (160dp width to match HomeItemCard)
 */
@Composable
fun HomeCardSkeleton() {
    Column(
        modifier = Modifier.width(160.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(160.dp)
                .shimmerBackground(RoundedCornerShape(8.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        ShimmerBox(width = 140.dp, height = 16.dp)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Subtitle
        ShimmerBox(width = 100.dp, height = 14.dp)
    }
}

/**
 * Search results loading skeleton — a column of song-row placeholders shown while
 * a search is in flight, replacing the bare spinner so the layout doesn't jump
 * when results land.
 */
@Composable
fun SearchResultsSkeleton(
    rows: Int = 8,
    modifier: Modifier = Modifier
) {
    ShimmerContainer {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            repeat(rows) {
                MusicCardSkeleton()
            }
        }
    }
}

/**
 * Home screen loading skeleton
 */
@Composable
fun HomeLoadingSkeleton() {
    // All shimmering placeholders below share a single infinite transition instead of
    // each allocating their own (previously ~12+ concurrent transitions during home load).
    ShimmerContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Greeting Header
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(width = 200.dp, height = 32.dp)
            }

            // Simulate 3 sections
            repeat(3) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Section Title
                    SectionHeaderSkeleton(modifier = Modifier.padding(horizontal = 16.dp))

                    // Horizontal List
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = false
                    ) {
                        items(4) {
                            HomeCardSkeleton()
                        }
                    }
                }
            }
        }
    }
}

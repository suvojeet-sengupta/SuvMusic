package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A slim, stylish vertical scrollbar drawn on the right edge of a scrollable
 * list. It fades in while the user is actively scrolling and fades back out
 * shortly after they stop — like the overlay scrollbars in modern music apps.
 *
 * The thumb size/position is derived from item indices (assuming roughly
 * uniform item heights), which is accurate enough for library lists/grids and
 * avoids the cost of measuring every item. All divisions are guarded so an
 * empty or fully-visible list simply draws nothing.
 */
fun Modifier.stylishScrollbar(
    state: LazyListState,
    color: Color,
    thickness: Dp = 4.dp,
    cornerRadius: Dp = 2.dp,
    minThumbHeight: Dp = 32.dp,
    activeAlpha: Float = 0.55f,
): Modifier = composed {
    val isActive = state.isScrollInProgress
    val alpha by animateFloatAsState(
        targetValue = if (isActive) activeAlpha else 0f,
        animationSpec = tween(durationMillis = if (isActive) 150 else 700),
        label = "listScrollbarAlpha"
    )
    drawWithContent {
        drawContent()
        val info = state.layoutInfo
        val totalItems = info.totalItemsCount
        val visible = info.visibleItemsInfo
        if (alpha <= 0f || totalItems == 0 || visible.isEmpty() || totalItems <= visible.size) {
            return@drawWithContent
        }
        drawScrollbarThumb(
            firstVisibleIndex = visible.first().index,
            visibleCount = visible.size,
            totalItems = totalItems,
            alpha = alpha,
            color = color,
            thicknessPx = thickness.toPx(),
            minThumbPx = minThumbHeight.toPx(),
            cornerPx = cornerRadius.toPx(),
        )
    }
}

/** [LazyGridState] variant — thumb derived from row-approximated item indices. */
fun Modifier.stylishScrollbar(
    state: LazyGridState,
    color: Color,
    thickness: Dp = 4.dp,
    cornerRadius: Dp = 2.dp,
    minThumbHeight: Dp = 32.dp,
    activeAlpha: Float = 0.55f,
): Modifier = composed {
    val isActive = state.isScrollInProgress
    val alpha by animateFloatAsState(
        targetValue = if (isActive) activeAlpha else 0f,
        animationSpec = tween(durationMillis = if (isActive) 150 else 700),
        label = "gridScrollbarAlpha"
    )
    drawWithContent {
        drawContent()
        val info = state.layoutInfo
        val totalItems = info.totalItemsCount
        val visible = info.visibleItemsInfo
        if (alpha <= 0f || totalItems == 0 || visible.isEmpty() || totalItems <= visible.size) {
            return@drawWithContent
        }
        drawScrollbarThumb(
            firstVisibleIndex = visible.first().index,
            visibleCount = visible.size,
            totalItems = totalItems,
            alpha = alpha,
            color = color,
            thicknessPx = thickness.toPx(),
            minThumbPx = minThumbHeight.toPx(),
            cornerPx = cornerRadius.toPx(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScrollbarThumb(
    firstVisibleIndex: Int,
    visibleCount: Int,
    totalItems: Int,
    alpha: Float,
    color: Color,
    thicknessPx: Float,
    minThumbPx: Float,
    cornerPx: Float,
) {
    val viewportH = size.height
    if (viewportH <= 0f) return

    val proportionVisible = visibleCount.toFloat() / totalItems.toFloat()
    val thumbH = (viewportH * proportionVisible).coerceIn(minThumbPx.coerceAtMost(viewportH), viewportH)
    val maxOffset = (viewportH - thumbH).coerceAtLeast(0f)

    val scrollableItems = (totalItems - visibleCount).coerceAtLeast(1)
    val progress = (firstVisibleIndex.toFloat() / scrollableItems.toFloat()).coerceIn(0f, 1f)
    val thumbY = maxOffset * progress

    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(size.width - thicknessPx, thumbY),
        size = Size(thicknessPx, thumbH),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
    )
}

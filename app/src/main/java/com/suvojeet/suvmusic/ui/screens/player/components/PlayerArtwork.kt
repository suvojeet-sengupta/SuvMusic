package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.LoadingArtworkOverlay

@Composable
fun AlbumArtwork(
    imageUrl: String?,
    title: String?,
    dominantColors: DominantColors,
    isLoading: Boolean = false,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    val context = LocalContext.current

    // Track horizontal drag offset
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 150f // Minimum swipe distance to trigger action

    // Animate offset when dragging ends
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = if (offsetX == 0f) 200 else 0),
        label = "swipe_offset"
    )

    // Calculate scale and rotation based on offset for visual feedback
    val scale = 1f - (kotlin.math.abs(animatedOffsetX) / 1500f).coerceIn(0f, 0.1f)
    val rotation = animatedOffsetX / 30f

    BoxWithConstraints {
        // Use width-based check for dynamic resizing support
        val isWideLayout = maxWidth > 500.dp

        Box(
            modifier = Modifier
                .then(
                    if (isWideLayout) {
                        Modifier.fillMaxHeight(0.85f).aspectRatio(1f)
                    } else {
                        Modifier.fillMaxWidth(0.85f).aspectRatio(1f)
                    }
                )
                .graphicsLayer {
                    translationX = animatedOffsetX
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX < -swipeThreshold -> {
                                    // Swiped left - next song
                                    onSwipeLeft()
                                }
                                offsetX > swipeThreshold -> {
                                    // Swiped right - previous song
                                    onSwipeRight()
                                }
                            }
                            // Reset offset
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX += dragAmount
                        }
                    )
                }
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = dominantColors.primary.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .size(600)
                        .build(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LoadingArtworkOverlay(
                isVisible = isLoading
            )
        }
    }
}

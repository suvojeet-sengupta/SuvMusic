package com.suvojeet.suvmusic.composeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Renders the album art for a song. If [thumbnailUrl] is non-null, fetches
 * via Coil 3; while loading or on error, falls back to a gradient
 * placeholder using SuvMusic's theme colours. Local files (no
 * thumbnailUrl yet) always show the gradient.
 *
 * Used in two places: the bottom bar (small square) and the NowPlaying
 * full-screen view (large square). Same composable so the look stays
 * consistent.
 */
@Composable
fun AlbumArt(
    thumbnailUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val container = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    if (thumbnailUrl.isNullOrBlank()) {
        GradientPlaceholder(modifier = container)
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
            .data(thumbnailUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        loading = { GradientPlaceholder(modifier = Modifier.fillMaxSize()) },
        error = { GradientPlaceholder(modifier = Modifier.fillMaxSize()) },
        modifier = container,
    )
}

@Composable
private fun GradientPlaceholder(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.7f),
                    secondary.copy(alpha = 0.6f),
                    tertiary.copy(alpha = 0.7f),
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxSize(0.4f),
        )
    }
}

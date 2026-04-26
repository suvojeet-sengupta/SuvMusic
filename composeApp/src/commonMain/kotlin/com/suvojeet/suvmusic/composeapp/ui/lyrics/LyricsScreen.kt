package com.suvojeet.suvmusic.composeapp.ui.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.composeapp.image.DominantColors
import com.suvojeet.suvmusic.composeapp.image.defaultDominantColors
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Lyrics rendering surface — shared port of `app/.../ui/screens/LyricsScreen.kt`,
 * trimmed to the visual / interaction core. The Android original is
 * 1369 lines and includes provider switcher, font tuner, time-offset
 * adjuster, blur slider, mood-aware backgrounds, PDF export, etc. — all
 * of which depend on `:app/.../providers/lyrics` and need that chain
 * ported first.
 *
 * What this version does:
 *  - Renders synced lyrics as a [LazyColumn] of lines, current line
 *    auto-scrolled to the centre and visually highlighted (scale +
 *    colour).
 *  - Falls back to a scrollable plain-text view when [Lyrics.plain]
 *    is set and no synced lines are available.
 *  - Empty / loading / no-song states.
 *  - Tappable lines call [onSeekTo] with the target timestamp.
 *  - Background uses the album-art [DominantColors] gradient (same
 *    treatment as [LyricsTab]).
 *
 * Designed to render identically on Android (when `:app` migrates its
 * LyricsScreen to consume this) and Desktop (already wired into the
 * Lyrics tab via a wrapper). Provider switcher, font tuner, and blur
 * slider land in a follow-up once the lyrics provider chain reaches
 * commonMain.
 */
@Composable
fun LyricsScreen(
    lyrics: Lyrics?,
    isFetching: Boolean,
    currentTimeMs: Long,
    songTitle: String,
    artistName: String,
    onSeekTo: (Long) -> Unit,
    onClose: () -> Unit,
    dominantColors: DominantColors = defaultDominantColors(true),
    textPosition: LyricsTextPosition = LyricsTextPosition.CENTER,
    animationType: LyricsAnimationType = LyricsAnimationType.LINE,
    fontSize: Float = 22f,
    lineSpacing: Float = 1.5f,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.primary,
                        dominantColors.secondary,
                        dominantColors.primary,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LyricsHeader(
                songTitle = songTitle,
                artistName = artistName,
                source = lyrics?.source,
                onColor = dominantColors.onBackground,
                onClose = onClose,
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isFetching -> CenteredLoading(dominantColors.onBackground)
                    lyrics == null -> EmptyLyricsState(
                        message = if (songTitle.isBlank()) {
                            "Pick a song to see its lyrics."
                        } else {
                            "No lyrics found for this track."
                        },
                        onColor = dominantColors.onBackground,
                    )
                    lyrics.isSynced -> SyncedLyricsList(
                        lines = lyrics.lines,
                        currentTimeMs = currentTimeMs,
                        textPosition = textPosition,
                        animationType = animationType,
                        fontSize = fontSize,
                        lineSpacing = lineSpacing,
                        onColor = dominantColors.onBackground,
                        accentColor = dominantColors.accent,
                        onLineClick = onSeekTo,
                    )
                    !lyrics.plain.isNullOrBlank() -> PlainLyricsBlock(
                        text = lyrics.plain,
                        fontSize = fontSize,
                        textPosition = textPosition,
                        onColor = dominantColors.onBackground,
                    )
                    else -> EmptyLyricsState(
                        message = "No lyrics available.",
                        onColor = dominantColors.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsHeader(
    songTitle: String,
    artistName: String,
    source: String?,
    onColor: androidx.compose.ui.graphics.Color,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Text(
                text = songTitle.ifBlank { "Lyrics" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onColor,
            )
            if (artistName.isNotBlank()) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = onColor.copy(alpha = 0.7f),
                )
            }
            source?.let {
                Text(
                    text = "via $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = onColor.copy(alpha = 0.5f),
                )
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close lyrics",
                tint = onColor,
            )
        }
    }
}

@Composable
private fun CenteredLoading(onColor: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = onColor)
    }
}

@Composable
private fun EmptyLyricsState(
    message: String,
    onColor: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lyrics,
            contentDescription = null,
            tint = onColor.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp).padding(bottom = 12.dp),
        )
        Text(
            text = message,
            color = onColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.widthIn(max = 360.dp),
        )
    }
}

@Composable
private fun PlainLyricsBlock(
    text: String,
    fontSize: Float,
    textPosition: LyricsTextPosition,
    onColor: androidx.compose.ui.graphics.Color,
) {
    val align = when (textPosition) {
        LyricsTextPosition.LEFT -> TextAlign.Start
        LyricsTextPosition.CENTER -> TextAlign.Center
        LyricsTextPosition.RIGHT -> TextAlign.End
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = text,
            color = onColor,
            fontSize = fontSize.sp,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SyncedLyricsList(
    lines: List<LyricLine>,
    currentTimeMs: Long,
    textPosition: LyricsTextPosition,
    animationType: LyricsAnimationType,
    fontSize: Float,
    lineSpacing: Float,
    onColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    onLineClick: (Long) -> Unit,
) {
    // Compute current line index from currentTimeMs (last line whose
    // timestamp is <= now). derivedStateOf keeps this O(1) on stable input.
    val currentIndex by remember(lines) {
        derivedStateOf {
            if (lines.isEmpty()) -1
            else {
                val idx = lines.indexOfLast { it.timeMs <= currentTimeMs }
                idx.coerceAtLeast(0)
            }
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll the active line into the middle of the viewport.
    LaunchedEffect(listState, currentIndex) {
        snapshotFlow { currentIndex }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx >= 0) {
                    val viewportSize = listState.layoutInfo.viewportSize.height
                    val centerOffset = -viewportSize / 3
                    listState.animateScrollToItem(idx, centerOffset)
                }
            }
    }

    val align = when (textPosition) {
        LyricsTextPosition.LEFT -> Alignment.Start
        LyricsTextPosition.CENTER -> Alignment.CenterHorizontally
        LyricsTextPosition.RIGHT -> Alignment.End
    }
    val textAlign = when (textPosition) {
        LyricsTextPosition.LEFT -> TextAlign.Start
        LyricsTextPosition.CENTER -> TextAlign.Center
        LyricsTextPosition.RIGHT -> TextAlign.End
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = align,
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == currentIndex
            val targetAlpha = when {
                isActive -> 1f
                animationType == LyricsAnimationType.NONE -> 0.7f
                else -> 0.45f
            }
            val targetScale = if (isActive && animationType != LyricsAnimationType.NONE) 1.05f else 1f
            val animatedAlpha by animateFloatAsState(targetAlpha, label = "line_alpha")
            val animatedScale by animateFloatAsState(targetScale, label = "line_scale")
            val color by animateColorAsState(
                if (isActive) accentColor else onColor.copy(alpha = animatedAlpha),
                label = "line_color",
            )

            Text(
                text = line.text,
                color = color,
                fontSize = fontSize.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLineClick(line.timeMs) }
                    .scale(animatedScale)
                    .padding(vertical = (lineSpacing * 4).dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

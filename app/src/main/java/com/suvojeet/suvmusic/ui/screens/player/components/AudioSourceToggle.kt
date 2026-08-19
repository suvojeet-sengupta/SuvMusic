package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.core.model.MusicSource
import com.suvojeet.suvmusic.ui.components.DominantColors

/**
 * Two-state YouTube ⇄ HQ Audio switch. The highlighted segment is where the current
 * song's audio is *actually* streaming from (read from the loaded stream, not the
 * preference); tapping the other segment re-resolves the song there in place. While a
 * switch is in flight the target segment shows a spinner and the control ignores taps.
 *
 * [compact] drops the label so it fits inside the like/dislike capsule.
 */
@Composable
fun AudioSourceToggle(
    activeAudioSource: MusicSource,
    isSwitching: Boolean,
    onSwitch: () -> Unit,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val height = if (compact) 30.dp else 36.dp
    val activeLabel = if (activeAudioSource == MusicSource.REMOTE) "HQ Audio" else "YouTube"
    Row(
        modifier = modifier
            .semantics { contentDescription = "Playing from $activeLabel. Tap the other source to switch." }
            .height(height)
            .clip(CircleShape)
            .background(dominantColors.onBackground.copy(alpha = 0.08f))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SourceSegment(
            source = MusicSource.YOUTUBE,
            label = "YouTube",
            active = activeAudioSource == MusicSource.YOUTUBE,
            switchingTo = isSwitching && activeAudioSource == MusicSource.REMOTE,
            enabled = !isSwitching,
            onClick = onSwitch,
            dominantColors = dominantColors,
            compact = compact,
            height = height - 6.dp
        )
        SourceSegment(
            source = MusicSource.REMOTE,
            label = "HQ Audio",
            active = activeAudioSource == MusicSource.REMOTE,
            switchingTo = isSwitching && activeAudioSource == MusicSource.YOUTUBE,
            enabled = !isSwitching,
            onClick = onSwitch,
            dominantColors = dominantColors,
            compact = compact,
            height = height - 6.dp
        )
    }
}

@Composable
private fun SourceSegment(
    source: MusicSource,
    label: String,
    active: Boolean,
    switchingTo: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    dominantColors: DominantColors,
    compact: Boolean,
    height: Dp
) {
    val highlight by animateColorAsState(
        targetValue = if (active) dominantColors.onBackground.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(220),
        label = "source_segment_bg"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (active || switchingTo) 1f else 0.5f,
        animationSpec = tween(220),
        label = "source_segment_alpha"
    )
    val tint = dominantColors.onBackground
    val iconSize = if (compact) 15.dp else 16.dp
    Row(
        modifier = Modifier
            .height(height)
            .clip(CircleShape)
            .background(highlight)
            .clickable(enabled = enabled && !active, onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 10.dp)
            .alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(iconSize + 2.dp), contentAlignment = Alignment.Center) {
            if (switchingTo) {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize - 1.dp),
                    strokeWidth = 1.5.dp,
                    color = tint
                )
            } else if (source == MusicSource.YOUTUBE) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_youtube),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.HighQuality,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(iconSize + 2.dp)
                )
            }
        }
        if (!compact && (active || switchingTo)) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = tint,
                maxLines = 1
            )
        }
    }
}

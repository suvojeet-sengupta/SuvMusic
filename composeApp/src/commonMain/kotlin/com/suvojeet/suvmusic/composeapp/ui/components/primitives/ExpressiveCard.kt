package com.suvojeet.suvmusic.composeapp.ui.components.primitives

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.theme.ElevationTokens
import com.suvojeet.suvmusic.composeapp.theme.MotionTokens

/**
 * Card with M3 Expressive press feedback — elevation and scale both animate
 * on press. Verbatim port of
 * `app/.../ui/components/primitives/ExpressiveCard.kt`.
 *
 * For interactive cards (tap-to-open). For static display cards, use the
 * stock [Card].
 */
@Composable
fun ExpressiveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = if (pressed) ElevationTokens.Level3 else ElevationTokens.Level1,
        animationSpec = MotionTokens.springSnappy(),
        label = "expressive_card_elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = MotionTokens.springSnappy(),
        label = "expressive_card_scale",
    )

    Card(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = border,
        interactionSource = source,
        content = content,
    )
}

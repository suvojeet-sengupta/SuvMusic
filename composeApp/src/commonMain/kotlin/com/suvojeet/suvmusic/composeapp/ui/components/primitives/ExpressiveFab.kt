package com.suvojeet.suvmusic.composeapp.ui.components.primitives

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.theme.MotionTokens

/**
 * FAB with M3 Expressive press response — scales to 0.92 and nudges a
 * small rotation on press for a playful "squish" feel. Verbatim port of
 * `app/.../ui/components/primitives/ExpressiveFab.kt`.
 */
@Composable
fun ExpressiveFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = MotionTokens.springBouncy(),
        label = "fab_scale",
    )
    val rotation by animateFloatAsState(
        targetValue = if (pressed) -4f else 0f,
        animationSpec = MotionTokens.springBouncy(),
        label = "fab_rotation",
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .rotate(rotation),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = source,
        content = content,
    )
}

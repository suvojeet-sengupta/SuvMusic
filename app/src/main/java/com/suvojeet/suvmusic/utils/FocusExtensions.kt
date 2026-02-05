package com.suvojeet.suvmusic.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Modifier to handle focus state for TV D-pad navigation.
 * Applies a scale effect, border, and background highlight when focused.
 * 
 * @param onClick Optional click handler. If provided, applies clickable with the same interaction source to avoid double-focus nodes.
 */
fun Modifier.dpadFocusable(
    onClick: (() -> Unit)? = null,
    focusedScale: Float = 1.1f,
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: androidx.compose.ui.unit.Dp = 2.dp,
    borderColor: Color = Color.White,
    focusBackgroundColor: Color = Color.White.copy(alpha = 0.1f),
    showBorder: Boolean = true
) = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }

    this
        .onFocusChanged { isFocused = it.isFocused }
        .scale(if (isFocused) focusedScale else 1f)
        .then(
             if (onClick != null) {
                 Modifier.clickable(
                     interactionSource = interactionSource,
                     indication = androidx.compose.foundation.LocalIndication.current,
                     onClick = onClick
                 )
             } else {
                 Modifier.focusable(interactionSource = interactionSource)
             }
        )
        .then(
            if (isFocused) {
                Modifier
                    .background(focusBackgroundColor, shape)
                    .then(if (showBorder) Modifier.border(borderWidth, borderColor, shape) else Modifier)
            } else {
                Modifier
            }
        )
}

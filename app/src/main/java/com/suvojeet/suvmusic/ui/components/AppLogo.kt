package com.suvojeet.suvmusic.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.core.model.LogoVariant
import com.suvojeet.suvmusic.data.SessionManager
import org.koin.compose.koinInject

/**
 * Resolve the drawable resource id for the user's selected [LogoVariant].
 * [LogoVariant.CLASSIC] keeps the original `R.drawable.logo` shipped before
 * the 2026 design refresh; the three new variants are PNG renders of the
 * SVGs the designer delivered.
 */
@DrawableRes
fun LogoVariant.drawableRes(): Int = when (this) {
    LogoVariant.PULSE -> R.drawable.logo_pulse
    LogoVariant.RESONANCE -> R.drawable.logo_resonance
    LogoVariant.AETHER -> R.drawable.logo_aether
    LogoVariant.CLASSIC -> R.drawable.logo
}

/**
 * Renders the active app logo as an [Image]. Reactively swaps when the
 * user picks a different variant from Appearance settings.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "SuvMusic",
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
) {
    val sessionManager: SessionManager = koinInject()
    val variant by sessionManager.logoVariantFlow.collectAsState(initial = LogoVariant.DEFAULT)
    Image(
        painter = painterResource(id = variant.drawableRes()),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
    )
}

/** Convenience overload for callers that just want a square logo at a fixed size. */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    AppLogo(modifier = modifier.size(size))
}

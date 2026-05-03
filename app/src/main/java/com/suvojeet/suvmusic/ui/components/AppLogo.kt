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
    LogoVariant.PULSE_APP_ICON -> R.drawable.logo_pulse_app_icon
    LogoVariant.PULSE_MONO -> R.drawable.logo_pulse_mono
    LogoVariant.PULSE_LIGHT -> R.drawable.logo_pulse_light
    LogoVariant.PULSE_TONE -> R.drawable.logo_pulse_tone
    LogoVariant.RESONANCE -> R.drawable.logo_resonance
    LogoVariant.RESONANCE_APP_ICON -> R.drawable.logo_resonance_app_icon
    LogoVariant.RESONANCE_MONO -> R.drawable.logo_resonance_mono
    LogoVariant.RESONANCE_LIGHT -> R.drawable.logo_resonance_light
    LogoVariant.RESONANCE_TONE -> R.drawable.logo_resonance_tone
    LogoVariant.AETHER -> R.drawable.logo_aether
    LogoVariant.AETHER_APP_ICON -> R.drawable.logo_aether_app_icon
    LogoVariant.AETHER_MONO -> R.drawable.logo_aether_mono
    LogoVariant.AETHER_LIGHT -> R.drawable.logo_aether_light
    LogoVariant.AETHER_TONE -> R.drawable.logo_aether_tone
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

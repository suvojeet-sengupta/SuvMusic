package com.suvojeet.suvmusic.composeapp.ui.about

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SuvMusic About screen — ported from app/.../ui/screens/AboutScreen.kt.
 *
 * Drops compared to the Android original:
 *   - Scaffold + TopAppBar — the desktop window already has its own
 *     navigation; the Android version's scaffold makes sense in a
 *     phone navigation graph but here it would double-bar.
 *   - dpadFocusable (TV remote helper, Android-only)
 *   - animateEnter (per-row staggered animation utility — drop for
 *     parity, re-add when the animation utility is in commonMain)
 *   - AboutViewModel — the original VM is currently a no-op placeholder
 *     (param marked @Suppress("UNUSED_PARAMETER")) so we just drop it.
 *   - R.drawable.logo — replaced with a music-note icon in
 *     AboutHeroSection until Compose Multiplatform resources carry the
 *     real asset over.
 *
 * Same composables otherwise: 7 sections in identical order with the
 * exact same text content as the Android version.
 */
@Composable
fun AboutScreen(
    appVersion: String,
    onOpenUri: (String) -> Unit,
    onHowItWorksClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { AboutHeroSection(appVersion = appVersion) }
        item { AboutDescriptionSection() }
        item { AboutFeaturesSection() }
        item { AboutDeveloperSection(onOpenUri = onOpenUri) }
        item { AboutTechStackSection() }
        item {
            AboutInformationSection(
                onOpenUri = onOpenUri,
                onHowItWorksClick = onHowItWorksClick,
            )
        }
        item { AboutFooterSection() }
    }
}

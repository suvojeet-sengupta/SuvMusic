package com.suvojeet.suvmusic.ui.sponsorblock

import androidx.compose.ui.graphics.Color
import com.suvojeet.suvmusic.core.model.SponsorCategory

/**
 * UI color swatches for SponsorBlock categories. Lives in the UI layer
 * (Android side) because [SponsorCategory] in :core:model is pure Kotlin
 * and cannot depend on Compose. Colors mirror the upstream SponsorBlock
 * browser extension's defaults.
 *
 * The desktop/shared mirror lives at
 * `composeApp/.../ui/settings/SponsorCategoryColors.kt`.
 */
val SponsorCategory.color: Color
    get() = when (this) {
        SponsorCategory.SPONSOR -> Color(0xFF00D400)
        SponsorCategory.SELFPROMO -> Color(0xFFFFFF00)
        SponsorCategory.INTERACTION -> Color(0xFFCC00FF)
        SponsorCategory.INTRO -> Color(0xFF00FFFF)
        SponsorCategory.OUTRO -> Color(0xFF020225)
        SponsorCategory.MUSIC_OFFTOPIC -> Color(0xFFFF9900)
    }

package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.ui.graphics.Color
import com.suvojeet.suvmusic.core.model.SponsorCategory

/**
 * UI color swatches for SponsorBlock categories — Compose Multiplatform
 * mirror of `app/.../ui/sponsorblock/SponsorCategoryColors.kt`. Lives
 * here (not in :core:model) because the data module intentionally
 * stays free of Compose deps.
 *
 * Keep these hex values in sync with the Android side and the upstream
 * SponsorBlock browser extension.
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

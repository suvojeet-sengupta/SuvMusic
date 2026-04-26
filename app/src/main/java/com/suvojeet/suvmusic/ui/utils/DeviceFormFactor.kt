package com.suvojeet.suvmusic.ui.utils

import androidx.compose.runtime.Stable

/**
 * Sealed hierarchy representing the device form factor.
 */
@Stable
sealed class DeviceFormFactor {

    /** Standard portrait/landscape phone (< 7" effective diagonal) */
    data object Phone : DeviceFormFactor()

    /**
     * Large phone / phablet (6.5"–7" range).
     * Gets phone layout but may use slightly wider content panels.
     */
    data object LargePhone : DeviceFormFactor()

    /**
     * True tablet (≥ 7" diagonal AND sw ≥ 600 dp confirmed).
     * Eligible for two-pane / side-sheet layouts.
     */
    data object Tablet : DeviceFormFactor()

    /**
     * Android TV or set-top box.
     */
    data object TV : DeviceFormFactor()

    /**
     * Foldable in *flat/open* state — treat like Tablet.
     * Foldable in *half-open* state — treat depending on posture.
     */
    data class Foldable(
        val posture: FoldablePosture,
    ) : DeviceFormFactor()

    // ── convenience ──
    val isTabletLike: Boolean
        get() = this is Tablet || (this is Foldable && posture == FoldablePosture.Flat)

    val isPhoneLike: Boolean
        get() = this is Phone || this is LargePhone ||
                (this is Foldable && posture == FoldablePosture.HalfOpen)
}

enum class FoldablePosture { Flat, HalfOpen, Unknown }

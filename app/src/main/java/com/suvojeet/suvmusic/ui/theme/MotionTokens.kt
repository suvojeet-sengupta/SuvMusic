package com.suvojeet.suvmusic.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * M3 Expressive motion tokens — single source of truth for durations, easing and spring specs.
 * Use these instead of inlining tween()/spring() so cadence stays consistent across screens.
 */
object MotionTokens {

    // Durations (ms) — M3 short/medium/long scale
    const val DurationShort1: Int = 50
    const val DurationShort2: Int = 100
    const val DurationShort3: Int = 150
    const val DurationShort4: Int = 200
    const val DurationMedium1: Int = 250
    const val DurationMedium2: Int = 300
    const val DurationMedium3: Int = 350
    const val DurationMedium4: Int = 400
    const val DurationLong1: Int = 450
    const val DurationLong2: Int = 500
    const val DurationLong3: Int = 550
    const val DurationLong4: Int = 600

    // Easing — M3 expressive curves
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    // Spring specs — playful bounce for expressive moments
    fun <T> springBouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> springGentle(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> springSnappy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Tween helpers bound to emphasized easing
    fun <T> tweenEmphasized(durationMs: Int = DurationMedium2): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMs, easing = Emphasized)

    fun <T> tweenStandard(durationMs: Int = DurationMedium2): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMs, easing = Standard)
}

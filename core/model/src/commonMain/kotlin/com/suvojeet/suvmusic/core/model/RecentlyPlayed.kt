@file:OptIn(ExperimentalTime::class)

package com.suvojeet.suvmusic.core.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Represents a recently played song with timestamp.
 *
 * `playedAt` defaults to "now" via `kotlin.time.Clock.System.now()`. Was
 * `System.currentTimeMillis()` when this lived in :app — same epoch-millis
 * semantics. kotlinx-datetime 0.7+ deprecates its own Clock in favour of
 * the stdlib one (see HomeTab.kt for the same idiom).
 */
data class RecentlyPlayed(
    val song: Song,
    val playedAt: Long = Clock.System.now().toEpochMilliseconds()
)

package com.suvojeet.suvmusic.core.model

import kotlinx.datetime.Clock

/**
 * Represents a recently played song with timestamp.
 *
 * `playedAt` defaults to "now" via kotlinx-datetime so the data class is
 * usable from commonMain. Was `System.currentTimeMillis()` when this lived
 * in :app — same epoch-millis semantics.
 */
data class RecentlyPlayed(
    val song: Song,
    val playedAt: Long = Clock.System.now().toEpochMilliseconds()
)

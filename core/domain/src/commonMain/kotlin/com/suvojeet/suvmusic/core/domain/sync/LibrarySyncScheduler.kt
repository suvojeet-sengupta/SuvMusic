package com.suvojeet.suvmusic.core.domain.sync

/** Shared scheduling boundary for background library synchronization. */
interface LibrarySyncScheduler {
    fun schedulePeriodic(intervalHours: Long)
    fun enqueueNow()
    fun cancel()
}

package com.suvojeet.suvmusic.core.domain.sync

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Linux/JVM periodic scheduler for desktop-owned background synchronization. */
class DesktopLibrarySyncScheduler(
    private val onSync: suspend () -> Unit,
) : LibrarySyncScheduler {
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "suvmusic-library-sync").apply { isDaemon = true }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var periodicTask: ScheduledFuture<*>? = null

    @Synchronized
    override fun schedulePeriodic(intervalHours: Long) {
        periodicTask?.cancel(false)
        val interval = intervalHours.coerceAtLeast(1L)
        periodicTask = executor.scheduleAtFixedRate(
            { scope.launch { runCatching { onSync() } } },
            interval,
            interval,
            TimeUnit.HOURS,
        )
    }

    override fun enqueueNow() {
        scope.launch { runCatching { onSync() } }
    }

    @Synchronized
    override fun cancel() {
        periodicTask?.cancel(false)
        periodicTask = null
    }
}

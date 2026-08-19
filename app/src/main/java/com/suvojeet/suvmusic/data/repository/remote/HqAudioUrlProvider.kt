package com.suvojeet.suvmusic.data.repository.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Decides which HQ Audio host a request goes to, and keeps that decision honest when a
 * host goes down.
 *
 * Two layers:
 *
 *  1. **Geo routing** — the system timezone is sampled once a day; users in Asia get the
 *     Asia edge, everyone else goes straight to the main server.
 *
 *  2. **Circuit breaker** — the Asia edge is the only host that can be diverted away from
 *     (the main server is the last resort, so tripping on it would leave nowhere to go).
 *     After [FAILURE_THRESHOLD] consecutive failures the breaker opens and every request
 *     is served by the main server for a cooldown window. When the window expires the
 *     breaker half-opens: exactly one request is allowed through to probe Asia while the
 *     rest keep going to main. That probe closing the breaker restores normal routing; a
 *     failing probe re-opens it with the next, longer cooldown.
 *
 * Routing is applied per request by [HqAudioRouteInterceptor], not baked into the Retrofit
 * base URL — otherwise a decision made at DI time would hold for the whole process
 * lifetime and a diversion could never take effect or recover.
 */
object HqAudioUrlProvider {

    private const val TAG = "HqAudioRoute"

    private const val PREFS_NAME = "hq_audio_api_prefs"
    private const val KEY_LAST_CHECK_TIMESTAMP = "last_tz_check_timestamp"
    private const val KEY_IS_ASIA_TZ = "is_asia_tz"
    private const val KEY_OPEN_UNTIL = "breaker_open_until"
    private const val KEY_TRIP_COUNT = "breaker_trip_count"
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    const val ASIA_BASE_URL = "https://hqaudio-asia.suvojeetsengupta.in/api/"
    const val DEFAULT_BASE_URL = "https://hqaudio.suvojeetsengupta.in/api/"

    const val ASIA_HOST = "hqaudio-asia.suvojeetsengupta.in"
    const val DEFAULT_HOST = "hqaudio.suvojeetsengupta.in"

    /** Consecutive Asia failures that trip the breaker. */
    private const val FAILURE_THRESHOLD = 3

    /**
     * How long traffic stays diverted, escalating on each successive trip so a genuinely
     * dead edge is not re-probed every few minutes for hours.
     */
    private val COOLDOWNS_MS = longArrayOf(
        5 * 60_000L,
        15 * 60_000L,
        30 * 60_000L,
        60 * 60_000L
    )

    /** A half-open probe that never comes back must not wedge the breaker open forever. */
    private const val PROBE_TIMEOUT_MS = 60_000L

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var stateLoaded = false

    private val consecutiveFailures = AtomicInteger(0)

    /** Epoch millis until which traffic is diverted; 0 means the breaker is closed. */
    private val openUntil = AtomicLong(0L)

    private val tripCount = AtomicInteger(0)

    private val probeInFlight = AtomicBoolean(false)
    private val probeStartedAt = AtomicLong(0L)

    private val _isAsiaEdgeDegraded = MutableStateFlow(false)

    /** True while Asia traffic is being diverted to the main server. */
    val isAsiaEdgeDegraded: StateFlow<Boolean> = _isAsiaEdgeDegraded.asStateFlow()

    /**
     * Caches the application context so routing decisions can be made from the OkHttp
     * interceptor, which has no context of its own. Safe to call more than once.
     */
    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        loadPersistedStateIfNeeded()
    }

    /**
     * The base URL this install should be built against. Kept for Retrofit construction —
     * the effective host per request is [activeHost].
     */
    fun getBaseUrl(context: Context): String {
        init(context)
        return if (isAsiaPreferred()) ASIA_BASE_URL else DEFAULT_BASE_URL
    }

    /** True if [host] is one of the HQ Audio hosts this provider routes between. */
    fun isManagedHost(host: String): Boolean =
        host.equals(ASIA_HOST, ignoreCase = true) || host.equals(DEFAULT_HOST, ignoreCase = true)

    /**
     * The host the next request should use. Calling this may consume the single half-open
     * probe slot, so call it once per request.
     */
    fun activeHost(): String {
        if (!isAsiaPreferred()) return DEFAULT_HOST
        loadPersistedStateIfNeeded()

        val until = openUntil.get()
        if (until == 0L) return ASIA_HOST

        val now = System.currentTimeMillis()
        if (now < until) return DEFAULT_HOST

        // Cooldown elapsed — half-open. Exactly one request probes Asia; a probe that has
        // been outstanding longer than PROBE_TIMEOUT_MS is treated as abandoned so the
        // breaker can never get stuck open.
        val probing = probeInFlight.get()
        val probeStale = probing && now - probeStartedAt.get() > PROBE_TIMEOUT_MS
        return if (!probing || probeStale) {
            probeInFlight.set(true)
            probeStartedAt.set(now)
            Log.i(TAG, "Cooldown elapsed, probing Asia edge")
            ASIA_HOST
        } else {
            DEFAULT_HOST
        }
    }

    /** Records a healthy response from [host]. Only Asia results move the breaker. */
    fun recordSuccess(host: String) {
        if (!host.equals(ASIA_HOST, ignoreCase = true)) return

        consecutiveFailures.set(0)
        probeInFlight.set(false)
        if (openUntil.getAndSet(0L) != 0L) {
            tripCount.set(0)
            persistBreakerState()
            _isAsiaEdgeDegraded.value = false
            Log.i(TAG, "Asia edge recovered, breaker closed")
            com.suvojeet.suvmusic.util.HqDiagnostics.log("route", "Asia edge recovered — breaker closed, HQ traffic back on Asia")
        }
    }

    /** Records a failed response or transport error from [host]. */
    fun recordFailure(host: String) {
        if (!host.equals(ASIA_HOST, ignoreCase = true)) return

        val wasProbe = probeInFlight.getAndSet(false)
        val now = System.currentTimeMillis()
        val wasHalfOpen = wasProbe && openUntil.get() != 0L && now >= openUntil.get()

        if (wasHalfOpen) {
            // The probe is the verdict on its own — no need to burn more requests.
            trip(now, "probe failed")
            return
        }

        val failures = consecutiveFailures.incrementAndGet()
        if (failures >= FAILURE_THRESHOLD) {
            trip(now, "$failures consecutive failures")
        } else {
            Log.w(TAG, "Asia edge failure $failures/$FAILURE_THRESHOLD")
        }
    }

    private fun trip(now: Long, reason: String) {
        val cooldown = COOLDOWNS_MS[tripCount.getAndIncrement().coerceAtMost(COOLDOWNS_MS.lastIndex)]
        openUntil.set(now + cooldown)
        consecutiveFailures.set(0)
        persistBreakerState()
        _isAsiaEdgeDegraded.value = true
        Log.w(TAG, "Diverting to main server for ${cooldown / 60_000}min ($reason)")
        com.suvojeet.suvmusic.util.HqDiagnostics.log("route", "Asia edge breaker TRIPPED ($reason) — diverting to main server for ${cooldown / 60_000}min")
    }

    /**
     * Samples the system timezone at most once a day and caches the verdict, so the common
     * path is a single SharedPreferences read.
     */
    private fun isAsiaPreferred(): Boolean {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?: return false
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCheckTime >= ONE_DAY_MS || !prefs.contains(KEY_IS_ASIA_TZ)) {
            val tzId = TimeZone.getDefault().id
            val isAsia = tzId.startsWith("Asia/", ignoreCase = true) ||
                tzId.contains("Asia", ignoreCase = true)
            prefs.edit()
                .putLong(KEY_LAST_CHECK_TIMESTAMP, currentTime)
                .putBoolean(KEY_IS_ASIA_TZ, isAsia)
                .apply()
            return isAsia
        }
        return prefs.getBoolean(KEY_IS_ASIA_TZ, false)
    }

    /**
     * Restores the breaker across process death — a restart in the middle of an outage
     * would otherwise send the first requests straight back at the dead edge.
     */
    private fun loadPersistedStateIfNeeded() {
        if (stateLoaded) return
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        synchronized(this) {
            if (stateLoaded) return
            val until = prefs.getLong(KEY_OPEN_UNTIL, 0L)
            // A stale window (clock changed, or it simply expired while the app was dead)
            // must not divert traffic for an unbounded time.
            val valid = until > System.currentTimeMillis() &&
                until - System.currentTimeMillis() <= COOLDOWNS_MS.last()
            openUntil.set(if (valid) until else 0L)
            tripCount.set(if (valid) prefs.getInt(KEY_TRIP_COUNT, 0) else 0)
            _isAsiaEdgeDegraded.value = valid
            stateLoaded = true
        }
    }

    private fun persistBreakerState() {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        prefs.edit()
            .putLong(KEY_OPEN_UNTIL, openUntil.get())
            .putInt(KEY_TRIP_COUNT, tripCount.get())
            .apply()
    }
}

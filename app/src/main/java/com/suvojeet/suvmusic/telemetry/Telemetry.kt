package com.suvojeet.suvmusic.telemetry

import com.suvojeet.suvmusic.core.model.AppError
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * A non-crash failure that the app used to swallow silently.
 *
 * ACRA already captures *crashes*; the bulk of real-world breakage in this app is the
 * opposite — calls that fail quietly and return an empty list, leaving the user with a
 * blank screen and us with no signal. [FailureEvent] gives those failures a heartbeat.
 */
data class FailureEvent(
    /** Logical operation, dotted for grouping, e.g. "stream.resolve", "search", "lyrics.fetch". */
    val operation: String,
    /** Where the data was coming from, e.g. "youtube", "remoteaudio", "kugou". */
    val source: String,
    /** The typed reason it failed. */
    val error: AppError,
    /** Optional extra context (ids, query lengths) — keep it free of PII. */
    val context: Map<String, String> = emptyMap(),
) {
    /** Stable aggregation key, e.g. "search:remoteaudio:NoNetwork". */
    val key: String get() = "$operation:$source:${error.kind}"
}

/** Sink for [FailureEvent]s. Pluggable so we can swap in a real backend later. */
interface FailureReporter {
    fun report(event: FailureEvent)
}

/** Default no-op reporter, used until the app installs a real one. */
object NoopFailureReporter : FailureReporter {
    override fun report(event: FailureEvent) {}
}

/**
 * Process-wide telemetry entry point for swallowed (non-crash) failures.
 *
 * Deliberately a global holder rather than a DI-injected dependency: the failure points
 * are scattered across many repositories whose construction we don't want to perturb, and
 * observability is a cross-cutting concern (like logging). Install a real [FailureReporter]
 * once from `Application.onCreate`; everything else just calls [report].
 */
object Telemetry {
    @Volatile
    private var reporter: FailureReporter = NoopFailureReporter

    fun install(reporter: FailureReporter) {
        this.reporter = reporter
    }

    fun report(event: FailureEvent) {
        try {
            reporter.report(event)
        } catch (_: Throwable) {
            // Telemetry must never take down a call path.
        }
    }

    /** Convenience for the common call shape at a catch site. */
    fun report(
        operation: String,
        source: String,
        error: AppError,
        context: Map<String, String> = emptyMap(),
    ) = report(FailureEvent(operation, source, error, context))
}

/**
 * The default reporter: logs a structured, greppable line and keeps in-memory counters.
 *
 * The log line ("SuvTelemetry FAIL …") rides along in ACRA's logcat capture, so even
 * without a backend a crash report now shows the *recent failure history* that led up to
 * it. [snapshot] backs a future in-app diagnostics screen.
 */
class LogFailureReporter : FailureReporter {
    private val counts = ConcurrentHashMap<String, AtomicLong>()

    override fun report(event: FailureEvent) {
        counts.getOrPut(event.key) { AtomicLong(0) }.incrementAndGet()
        val ctx = if (event.context.isEmpty()) "" else " " + event.context.entries.joinToString(" ") { "${it.key}=${it.value}" }
        android.util.Log.w(
            "SuvTelemetry",
            "FAIL op=${event.operation} src=${event.source} err=${event.error.kind}" +
                (event.error.detail?.let { " detail=$it" } ?: "") + ctx,
        )
    }

    /** Current failure counts keyed by [FailureEvent.key]. */
    fun snapshot(): Map<String, Long> = counts.mapValues { it.value.get() }
}

package com.suvojeet.suvmusic.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TEMPORARY diagnostics recorder for the HQ Audio playback chain.
 *
 * The user starts a session from Playback settings, plays a few songs, stops it, and is
 * prompted to share the resulting log file. Every decision point in the chain (search,
 * matching, backoff gates, API fallback, routing, source switches) appends a line here
 * while a session is active; when idle every call is a no-op.
 *
 * Remove this file and every `HqDiagnostics.log(...)` call site once the field debugging
 * round is over — `grep -rn HqDiagnostics` lists them all.
 */
object HqDiagnostics {

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    @Volatile
    private var writer: PrintWriter? = null

    @Volatile
    private var file: File? = null

    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun start(context: Context): Boolean {
        if (writer != null) return true
        return try {
            val dir = File(context.cacheDir, "diag").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val f = File(dir, "hq-diag-$stamp.log")
            val w = PrintWriter(f.bufferedWriter())
            val version = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "?"
            }
            w.println("SuvMusic HQ diagnostics — started ${Date()}")
            w.println("app=$version device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} android=${android.os.Build.VERSION.RELEASE} tz=${java.util.TimeZone.getDefault().id}")
            w.println("────────────────────────────────────────")
            w.flush()
            writer = w
            file = f
            _active.value = true
            log("diag", "session started")
            true
        } catch (e: Exception) {
            android.util.Log.e("HqDiagnostics", "start failed: ${e.message}")
            false
        }
    }

    @Synchronized
    fun stop(): File? {
        val w = writer ?: return null
        log("diag", "session stopped")
        w.flush()
        w.close()
        writer = null
        _active.value = false
        val f = file
        file = null
        return f
    }

    /** Appends one timestamped line while a session is active; no-op otherwise. */
    fun log(tag: String, msg: String) {
        if (writer == null) return
        synchronized(this) {
            val w = writer ?: return
            w.println("${timeFmt.format(Date())} [$tag] $msg")
            w.flush()
        }
    }
}

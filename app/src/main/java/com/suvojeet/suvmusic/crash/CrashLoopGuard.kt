package com.suvojeet.suvmusic.crash

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import java.io.File

/**
 * Detects startup crash loops and degrades the app to a safe mode instead of
 * letting it crash forever.
 *
 * Mechanism: every uncaught exception that happens within [EARLY_CRASH_WINDOW_MS]
 * of process start bumps a counter in plain (non-encrypted, commit-synchronous)
 * SharedPreferences before the crash is handed to the previous handler (ACRA),
 * so the count survives the process death. Once the process stays alive for
 * [STABLE_AFTER_MS] the counter resets to zero.
 *
 * When [SAFE_MODE_THRESHOLD] consecutive early crashes are seen at the next
 * launch, that launch runs in safe mode: the disk caches most likely to hold a
 * poison payload are wiped, and native DSP consumers ([isSafeMode]) skip loading
 * the C++ engine for this one process. The counter is reset when safe mode
 * engages, so the launch after a successful safe-mode run is a normal one.
 */
object CrashLoopGuard {
    private const val TAG = "CrashLoopGuard"
    private const val PREFS_NAME = "crash_loop_guard"
    private const val KEY_EARLY_CRASHES = "consecutive_early_crashes"

    private const val EARLY_CRASH_WINDOW_MS = 60_000L
    private const val STABLE_AFTER_MS = 45_000L
    private const val SAFE_MODE_THRESHOLD = 3

    /** True for the whole process lifetime when this launch is a recovery launch. */
    @Volatile
    var isSafeMode: Boolean = false
        private set

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val earlyCrashes = prefs.getInt(KEY_EARLY_CRASHES, 0)
        if (earlyCrashes >= SAFE_MODE_THRESHOLD) {
            isSafeMode = true
            prefs.edit().putInt(KEY_EARLY_CRASHES, 0).commit()
            android.util.Log.w(
                TAG,
                "$earlyCrashes consecutive early crashes — entering SAFE MODE " +
                    "(native DSP disabled, disk caches wiped) for this launch",
            )
            wipeCaches(context)
        } else if (earlyCrashes > 0) {
            android.util.Log.w(TAG, "early-crash count is $earlyCrashes (safe mode at $SAFE_MODE_THRESHOLD)")
        }

        val processStartAt = System.currentTimeMillis()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            recordCrash(prefs, System.currentTimeMillis() - processStartAt)
            previous?.uncaughtException(thread, throwable)
        }

        // Surviving the startup window means we're not in a crash loop.
        Handler(Looper.getMainLooper()).postDelayed({
            prefs.edit().putInt(KEY_EARLY_CRASHES, 0).apply()
        }, STABLE_AFTER_MS)
    }

    private fun recordCrash(prefs: SharedPreferences, uptimeMs: Long) {
        try {
            if (uptimeMs <= EARLY_CRASH_WINDOW_MS) {
                // commit(), not apply(): the process is about to die.
                prefs.edit()
                    .putInt(KEY_EARLY_CRASHES, prefs.getInt(KEY_EARLY_CRASHES, 0) + 1)
                    .commit()
            }
        } catch (_: Throwable) {
            // Never interfere with crash delivery to ACRA.
        }
    }

    /**
     * Wipe the caches a corrupt entry could crash-loop on. The small JSON caches
     * are deleted synchronously (they're read during startup); the potentially
     * large cacheDir (image/stream caches) is cleared on a background thread.
     */
    private fun wipeCaches(context: Context) {
        runCatching {
            File(context.filesDir, "offline_search").deleteRecursively()
            File(context.filesDir, "offline_lists").deleteRecursively()
        }
        Thread {
            runCatching {
                context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            }
        }.apply { name = "safe-mode-cache-wipe"; isDaemon = true }.start()
    }
}

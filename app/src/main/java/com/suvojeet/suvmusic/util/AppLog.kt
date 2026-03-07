package com.suvojeet.suvmusic.util

import android.content.Context
import android.util.Log
import com.suvojeet.suvmusic.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Debug-gated logging utility with optional persistent file logging.
 */
object AppLog {
    @PublishedApi
    internal var isLoggingEnabled = false
    
    private var _logFile: File? = null

    val logFile: File? get() = _logFile

    private val executor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context, enabled: Boolean) {
        isLoggingEnabled = enabled
        if (enabled) {
            val logDir = File(context.cacheDir, "logs")
            if (!logDir.exists()) logDir.mkdirs()
            // Keep one main log file, maybe rotate it if it gets too big
            _logFile = File(logDir, "app_logs.txt")
            
            // Optional: Start with a separator for new session
            logToFile("SYSTEM", "--- App Started / Logging Initialized ---")
        } else {
            _logFile = null
        }
    }

    @PublishedApi
    internal fun logToFile(tag: String, message: String, throwable: Throwable? = null) {
        val currentFile = _logFile
        if (!isLoggingEnabled || currentFile == null) return

        executor.execute {
            try {
                FileOutputStream(currentFile, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        val timestamp = dateFormat.format(Date())
                        pw.println("$timestamp [$tag] $message")
                        throwable?.printStackTrace(pw)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppLog", "Failed to write to log file", e)
            }
        }
    }

    inline fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG || isLoggingEnabled) {
            val msg = message()
            Log.d(tag, msg)
            if (isLoggingEnabled) logToFile(tag, msg)
        }
    }

    inline fun i(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG || isLoggingEnabled) {
            val msg = message()
            Log.i(tag, msg)
            if (isLoggingEnabled) logToFile(tag, msg)
        }
    }

    inline fun w(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG || isLoggingEnabled) {
            val msg = message()
            Log.w(tag, msg)
            if (isLoggingEnabled) logToFile(tag, msg)
        }
    }

    inline fun w(tag: String, message: () -> String, throwable: Throwable) {
        if (BuildConfig.DEBUG || isLoggingEnabled) {
            val msg = message()
            Log.w(tag, msg, throwable)
            if (isLoggingEnabled) logToFile(tag, msg, throwable)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        if (isLoggingEnabled) {
            logToFile(tag, message, throwable)
        }
    }
    
    fun clearLogs() {
        executor.execute {
            try {
                _logFile?.delete()
                _logFile?.createNewFile()
            } catch (e: Exception) {
                Log.e("AppLog", "Failed to clear log file", e)
            }
        }
    }
}

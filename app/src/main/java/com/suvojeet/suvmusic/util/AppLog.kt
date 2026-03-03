package com.suvojeet.suvmusic.util

import android.util.Log
import com.suvojeet.suvmusic.BuildConfig

/**
 * Debug-gated logging utility.
 * All debug/info/warning logs are stripped in release builds.
 * Error logs are always emitted since they indicate real problems.
 */
object AppLog {
    
    inline fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message())
        }
    }
    
    inline fun i(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message())
        }
    }
    
    inline fun w(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message())
        }
    }
    
    inline fun w(tag: String, message: () -> String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message(), throwable)
        }
    }
    
    /** Error logs are always emitted — they indicate real problems. */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}

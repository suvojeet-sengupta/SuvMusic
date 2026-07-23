package com.suvojeet.suvmusic.data.repository.remote

import android.content.Context
import java.util.TimeZone

object HqAudioUrlProvider {

    private const val PREFS_NAME = "hq_audio_api_prefs"
    private const val KEY_LAST_CHECK_TIMESTAMP = "last_tz_check_timestamp"
    private const val KEY_IS_ASIA_TZ = "is_asia_tz"
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    private const val ASIA_BASE_URL = "https://hqaudio-asia.suvojeetsengupta.in/api/"
    private const val DEFAULT_BASE_URL = "https://hqaudio.suvojeetsengupta.in/api/"

    /**
     * Checks the system timezone once per day and saves the state.
     * Returns the Asia HQ Audio API URL if in Asia, otherwise returns the default API URL.
     */
    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()

        val isAsia: Boolean
        if (currentTime - lastCheckTime >= ONE_DAY_MS || !prefs.contains(KEY_IS_ASIA_TZ)) {
            val tzId = TimeZone.getDefault().id
            isAsia = tzId.startsWith("Asia/", ignoreCase = true) || tzId.contains("Asia", ignoreCase = true)
            prefs.edit()
                .putLong(KEY_LAST_CHECK_TIMESTAMP, currentTime)
                .putBoolean(KEY_IS_ASIA_TZ, isAsia)
                .apply()
        } else {
            isAsia = prefs.getBoolean(KEY_IS_ASIA_TZ, false)
        }

        return if (isAsia) ASIA_BASE_URL else DEFAULT_BASE_URL
    }
}

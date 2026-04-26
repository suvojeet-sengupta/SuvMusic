package com.suvojeet.suvmusic.player

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timer options for sleep timer.
 */
enum class SleepTimerOption(val minutes: Int, val label: String) {
    OFF(0, "Off"),
    FIVE_MIN(5, "5 minutes"),
    TEN_MIN(10, "10 minutes"),
    FIFTEEN_MIN(15, "15 minutes"),
    THIRTY_MIN(30, "30 minutes"),
    FORTY_FIVE_MIN(45, "45 minutes"),
    ONE_HOUR(60, "1 hour"),
    TWO_HOURS(120, "2 hours"),
    CUSTOM(-2, "Custom"),
    FADE_OUT_GENTLE(-3, "Fade out (5% every 2 min)"),
    FADE_OUT_FAST(-4, "Fade out (5% every 1 min)"),
    END_OF_SONG(-1, "End of song")
}

/**
 * Manages sleep timer functionality.
 * Tracks countdown and notifies when time is up.
 */
@Singleton
class SleepTimerManager @Inject constructor() {
    companion object {
        private const val FADE_STEP_PERCENT = 0.05f
        private const val FADE_STEP_COUNT = 20
    }
    
    private var countDownTimer: CountDownTimer? = null
    
    private val _remainingTimeMs = MutableStateFlow<Long?>(null)
    val remainingTimeMs: StateFlow<Long?> = _remainingTimeMs.asStateFlow()
    
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private val _currentOption = MutableStateFlow(SleepTimerOption.OFF)
    val currentOption: StateFlow<SleepTimerOption> = _currentOption.asStateFlow()
    
    // Callback when timer finishes
    private var onTimerFinished: (() -> Unit)? = null
    private var onFadeStep: ((Float) -> Unit)? = null
    
    // Flag for "End of Song" mode
    private val _endOfSongMode = MutableStateFlow(false)
    val endOfSongMode: StateFlow<Boolean> = _endOfSongMode.asStateFlow()
    
    fun setOnTimerFinished(callback: () -> Unit) {
        onTimerFinished = callback
    }

    fun setOnFadeStep(callback: (Float) -> Unit) {
        onFadeStep = callback
    }
    
    /**
     * Start sleep timer with specified option.
     */
    fun startTimer(option: SleepTimerOption, customMinutes: Int? = null) {
        cancelTimer()
        
        _currentOption.value = option
        
        if (option == SleepTimerOption.OFF) {
            return
        }
        
        if (option == SleepTimerOption.END_OF_SONG) {
            _endOfSongMode.value = true
            _isActive.value = true
            _remainingTimeMs.value = null
            return
        }

        if (option == SleepTimerOption.FADE_OUT_GENTLE || option == SleepTimerOption.FADE_OUT_FAST) {
            _endOfSongMode.value = false
            val intervalMs = if (option == SleepTimerOption.FADE_OUT_FAST) 60_000L else 120_000L
            val durationMs = intervalMs * FADE_STEP_COUNT
            _remainingTimeMs.value = durationMs
            _isActive.value = true
            startFadeOutTimer(intervalMs, durationMs)
            return
        }
        
        _endOfSongMode.value = false
        
        // Determine duration
        val durationMs = if (option == SleepTimerOption.CUSTOM && customMinutes != null) {
             customMinutes * 60 * 1000L
        } else {
             option.minutes * 60 * 1000L
        }
        
        // Safety check
        if (durationMs <= 0) return
        
        countDownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingTimeMs.value = millisUntilFinished
            }
            
            override fun onFinish() {
                _remainingTimeMs.value = 0
                _isActive.value = false
                _currentOption.value = SleepTimerOption.OFF
                onTimerFinished?.invoke()
            }
        }
        
        _isActive.value = true
        countDownTimer?.start()
    }

    private fun startFadeOutTimer(intervalMs: Long, totalDurationMs: Long) {
        var accumulatedPercent = 0f

        countDownTimer = object : CountDownTimer(totalDurationMs, intervalMs) {
            override fun onTick(millisUntilFinished: Long) {
                accumulatedPercent = (accumulatedPercent + FADE_STEP_PERCENT).coerceAtMost(1f)
                _remainingTimeMs.value = millisUntilFinished
                onFadeStep?.invoke(accumulatedPercent)
            }

            override fun onFinish() {
                _remainingTimeMs.value = 0
                _isActive.value = false
                _currentOption.value = SleepTimerOption.OFF
                onFadeStep?.invoke(1f)
                onTimerFinished?.invoke()
            }
        }

        countDownTimer?.start()
    }
    
    /**
     * Cancel the current timer.
     */
    fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _remainingTimeMs.value = null
        _isActive.value = false
        _currentOption.value = SleepTimerOption.OFF
        _endOfSongMode.value = false
    }
    
    /**
     * Called when a song ends - triggers stop if in "End of Song" mode.
     * @return true if the timer triggered (stop requested), false otherwise.
     */
    fun onSongEnded(): Boolean {
        if (_endOfSongMode.value) {
            _endOfSongMode.value = false
            _isActive.value = false
            _currentOption.value = SleepTimerOption.OFF
            onTimerFinished?.invoke()
            return true
        }
        return false
    }
    
    /**
     * Format remaining time as MM:SS string.
     */
    fun formatRemainingTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

package com.suvojeet.suvmusic.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.HapticsIntensity
import com.suvojeet.suvmusic.data.model.HapticsMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Music Haptics Manager - Provides synchronized haptic feedback with music playback.
 * Similar to Apple Music's Music Haptics feature on iOS.
 * 
 * Features:
 * - Beat detection based on audio amplitude
 * - Multiple haptic modes (Basic, Advanced, Custom)
 * - Intensity customization
 * - Battery optimization with minimum interval between vibrations
 */
@Singleton
class MusicHapticsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    // State tracking
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private var lastHapticTime = 0L
    private var previousAmplitude = 0f
    private var beatDetectionJob: Job? = null
    
    // Configurable parameters
    private var isEnabled = false
    private var currentMode = HapticsMode.BASIC
    private var currentIntensity = HapticsIntensity.MEDIUM
    
    // Minimum interval between haptics to prevent motor wear and battery drain
    private val minHapticIntervalMs = 50L
    
    // Threshold values for beat detection
    private val basicModeThreshold = 0.65f
    private val advancedModeThreshold = 0.35f
    private val customModeThreshold = 0.50f
    
    init {
        // Load settings on initialization
        scope.launch {
            refreshSettings()
        }
    }
    
    /**
     * Refresh haptics settings from SessionManager.
     */
    suspend fun refreshSettings() {
        isEnabled = sessionManager.musicHapticsEnabledFlow.first()
        currentMode = sessionManager.hapticsModeFlow.first()
        currentIntensity = sessionManager.hapticsIntensityFlow.first()
    }
    
    /**
     * Start the haptics engine.
     * Called when music playback starts.
     */
    fun start() {
        if (!isEnabled || currentMode == HapticsMode.OFF) {
            return
        }
        
        if (!vibrator.hasVibrator()) {
            return
        }
        
        _isActive.value = true
        lastHapticTime = 0L
        previousAmplitude = 0f
    }
    
    /**
     * Stop the haptics engine.
     * Called when music playback stops or is paused.
     */
    fun stop() {
        _isActive.value = false
        beatDetectionJob?.cancel()
        beatDetectionJob = null
        vibrator.cancel()
    }
    
    /**
     * Process audio amplitude for beat detection.
     * Should be called periodically with current audio level (0.0 to 1.0).
     * 
     * @param amplitude Current audio amplitude normalized between 0 and 1
     */
    fun processAmplitude(amplitude: Float) {
        if (!_isActive.value || !isEnabled) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Enforce minimum interval between haptics
        if (currentTime - lastHapticTime < minHapticIntervalMs) {
            previousAmplitude = amplitude
            return
        }
        
        // Get threshold based on mode
        val threshold = when (currentMode) {
            HapticsMode.OFF -> return
            HapticsMode.BASIC -> basicModeThreshold
            HapticsMode.ADVANCED -> advancedModeThreshold
            HapticsMode.CUSTOM -> customModeThreshold
        }
        
        // Beat detection: check for sudden amplitude increase
        val amplitudeDelta = amplitude - previousAmplitude
        val isBeat = amplitude > threshold && amplitudeDelta > 0.15f
        
        if (isBeat) {
            triggerHaptic(amplitude)
            lastHapticTime = currentTime
        }
        
        previousAmplitude = amplitude
    }
    
    /**
     * Trigger haptic feedback based on amplitude.
     * Uses VibrationEffect on Android 8+ for better control.
     */
    private fun triggerHaptic(amplitude: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                triggerHapticModern(amplitude)
            } else {
                triggerHapticLegacy()
            }
        } catch (e: Exception) {
            // Silently fail - haptics are a nice-to-have feature
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun triggerHapticModern(amplitude: Float) {
        // Calculate vibration intensity based on mode and user preference
        val intensityMultiplier = currentIntensity.multiplier
        
        // Map amplitude (0-1) to vibration amplitude (1-255)
        val baseIntensity = (amplitude * 200).toInt().coerceIn(50, 200)
        val finalIntensity = (baseIntensity * intensityMultiplier).toInt().coerceIn(1, 255)
        
        // Duration varies based on mode
        val duration = when (currentMode) {
            HapticsMode.BASIC -> 25L      // Quick tap
            HapticsMode.ADVANCED -> 35L   // Slightly longer for feel
            HapticsMode.CUSTOM -> 30L     // Medium
            HapticsMode.OFF -> return
        }
        
        val effect = VibrationEffect.createOneShot(duration, finalIntensity)
        vibrator.vibrate(effect)
    }
    
    @Suppress("DEPRECATION")
    private fun triggerHapticLegacy() {
        // Legacy vibration for older Android versions
        vibrator.vibrate(20L)
    }
    
    /**
     * Trigger a preview haptic for settings screen.
     * Allows user to feel the haptic intensity before applying.
     */
    fun triggerPreview(intensity: HapticsIntensity) {
        if (!vibrator.hasVibrator()) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationIntensity = (180 * intensity.multiplier).toInt().coerceIn(1, 255)
                val effect = VibrationEffect.createOneShot(50L, vibrationIntensity)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    /**
     * Trigger a rhythmic pattern preview to demonstrate haptics.
     */
    fun triggerPatternPreview() {
        if (!vibrator.hasVibrator()) return
        
        scope.launch {
            try {
                // Simulate a beat pattern: quick-quick-longer
                repeat(3) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intensity = (150 * currentIntensity.multiplier).toInt().coerceIn(1, 255)
                        vibrator.vibrate(VibrationEffect.createOneShot(40L, intensity))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(40L)
                    }
                    delay(150)
                }
                
                // Stronger beat
                delay(100)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intensity = (220 * currentIntensity.multiplier).toInt().coerceIn(1, 255)
                    vibrator.vibrate(VibrationEffect.createOneShot(80L, intensity))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80L)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    /**
     * Check if the device supports haptic feedback.
     */
    fun hasHapticSupport(): Boolean {
        return vibrator.hasVibrator()
    }
    
    /**
     * Update settings without requiring restart.
     */
    fun updateSettings(enabled: Boolean, mode: HapticsMode, intensity: HapticsIntensity) {
        isEnabled = enabled
        currentMode = mode
        currentIntensity = intensity
        
        if (!enabled || mode == HapticsMode.OFF) {
            stop()
        }
    }
}

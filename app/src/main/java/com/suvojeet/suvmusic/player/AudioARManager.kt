package com.suvojeet.suvmusic.player

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos

/**
 * Manages "Audio AR" features by monitoring device rotation
 * and calculating stereo balance to simulate a fixed soundstage.
 */
@Singleton
class AudioARManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val scope = CoroutineScope(Dispatchers.Main)

    // -1.0 (Left) to 1.0 (Right)
    private val _stereoBalance = MutableStateFlow(0f)
    val stereoBalance: StateFlow<Float> = _stereoBalance.asStateFlow()

    private var isEnabled = false
    private var anchorAzimuth: Float? = null // The "front" direction

    // Smoothing
    private var smoothedBalance = 0f
    private val alpha = 0.2f // low-pass filter factor (increased for responsiveness)

    private var isPlaying = false
    private var isSettingsEnabled = false
    private var sensitivity = 1.0f
    private var autoCalibrateEnabled = true

    // Drift / Auto-Calibrate logic
    private var lastStableAzimuth: Float? = null
    private var stableStartTime: Long = 0
    private val stableThreshold = 0.05f // Radians (~3 degrees)
    private val stableDuration = 5000L // 5 seconds
    private val driftSpeed = 0.005f // How fast it shifts back to center per event

    init {
        scope.launch {
            sessionManager.audioArEnabledFlow.collect { enabled ->
                isSettingsEnabled = enabled
                updateSensorState()
            }
        }
        scope.launch {
            sessionManager.audioArSensitivityFlow.collect { value ->
                sensitivity = value
            }
        }
        scope.launch {
            sessionManager.audioArAutoCalibrateFlow.collect { enabled ->
                autoCalibrateEnabled = enabled
            }
        }
    }

    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        updateSensorState()
    }

    private fun updateSensorState() {
        if (isSettingsEnabled && isPlaying) {
            start()
        } else {
            stop()
        }
    }

    private fun start() {
        if (isEnabled || rotationVectorSensor == null) return
        isEnabled = true
        anchorAzimuth = null // Reset anchor on start
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
    }
    
    private fun stop() {
        if (!isEnabled) return
        isEnabled = false
        sensorManager.unregisterListener(this)
        _stereoBalance.value = 0f // Reset to center
    }

    fun calibrate() {
        anchorAzimuth = null // Next sensor event will set current direction as front
    }

    internal object AudioARMath {
        fun normalizeAngle(angle: Float): Float {
            var normalized = angle
            while (normalized > Math.PI) normalized -= (2 * Math.PI).toFloat()
            while (normalized < -Math.PI) normalized += (2 * Math.PI).toFloat()
            return normalized
        }

        fun calculateTargetBalance(delta: Float, sensitivity: Float): Float {
            // Map delta to balance
            // If delta is 0 (looking at anchor) -> Balance 0
            // If delta is +PI/2 (looking right) -> Sound should come from LEFT (Balance -1.0)
            // If delta is -PI/2 (looking left) -> Sound should come from RIGHT (Balance +1.0)
            return -sin(delta * sensitivity).coerceIn(-1f, 1f)
        }

        fun calculateDrift(delta: Float, driftSpeed: Float): Float {
            return delta * driftSpeed
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isEnabled) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR || 
            event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            
            // Convert rotation vector to azimuth (yaw)
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            
            val azimuth = orientation[0] // Radians, -PI to PI
            
            if (anchorAzimuth == null) {
                anchorAzimuth = azimuth
            }
            
            val anchor = anchorAzimuth ?: return
            
            // Calculate delta angle
            var delta = azimuth - anchor
            
            // Normalize to -PI to PI
            delta = AudioARMath.normalizeAngle(delta)

            // Auto-Calibration / Drift Logic
            if (autoCalibrateEnabled) {
                val currentTime = System.currentTimeMillis()
                if (lastStableAzimuth == null || abs(azimuth - (lastStableAzimuth ?: 0f)) > stableThreshold) {
                    lastStableAzimuth = azimuth
                    stableStartTime = currentTime
                } else if (currentTime - stableStartTime > stableDuration) {
                    // Orientation has been stable at a new position for > 5s
                    // Slowly drift the anchor towards current azimuth to re-center
                    val driftAmount = AudioARMath.calculateDrift(delta, driftSpeed)
                    anchorAzimuth = AudioARMath.normalizeAngle((anchorAzimuth ?: 0f) + driftAmount)
                }
            }
            
            // Map delta to balance
            val targetBalance = AudioARMath.calculateTargetBalance(delta, sensitivity)
            
            // Apply smoothing
            smoothedBalance += alpha * (targetBalance - smoothedBalance)
            
            // Clamp
            val finalBalance = smoothedBalance.coerceIn(-1f, 1f)
            
            _stereoBalance.value = finalBalance
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

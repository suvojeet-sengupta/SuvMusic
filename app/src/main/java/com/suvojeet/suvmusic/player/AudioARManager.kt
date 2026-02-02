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
    @ApplicationContext private val context: Context,
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
    private val alpha = 0.1f // low-pass filter factor

    init {
        scope.launch {
            sessionManager.audioArEnabledFlow.collect { enabled ->
                if (enabled) {
                    start()
                } else {
                    stop()
                }
            }
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
            if (delta > Math.PI) delta -= (2 * Math.PI).toFloat()
            if (delta < -Math.PI) delta += (2 * Math.PI).toFloat()
            
            // Map delta to balance
            // If delta is 0 (looking at anchor) -> Balance 0
            // If delta is +PI/2 (looking right) -> Sound should come from LEFT (Balance -1.0)
            // If delta is -PI/2 (looking left) -> Sound should come from RIGHT (Balance +1.0)
            
            // Logic: 
            // Turning Head Right (Positive Delta) -> Source moves to Left Ear relative to head -> Balance Left (-1)
            // Turning Head Left (Negative Delta) -> Source moves to Right Ear relative to head -> Balance Right (+1)
            
            // We clamp rotation to +/- 90 degrees (PI/2) for full pan? 
            // Or maybe smoother 180 degrees?
            // Let's use sin(delta) for natural falloff, negated.
            
            val targetBalance = -sin(delta)
            
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

package com.suvojeet.suvmusic.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.suvojeet.suvmusic.MainActivity
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Media3 MediaSessionService for background music playback.
 * Supports gapless playback and automix based on user settings.
 */
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    @Inject
    @com.suvojeet.suvmusic.di.PlayerDataSource
    lateinit var dataSourceFactory: androidx.media3.datasource.DataSource.Factory
    
    private var mediaSession: MediaSession? = null
    
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var dynamicsProcessing: android.media.audiofx.DynamicsProcessing? = null
    
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // Initialize with defaults to avoid blocking main thread
        // Logic for gapless/automix is handled by player configuration and queue management
        
        // Ultra-fast buffer for instant playback
        // Minimum buffering = faster start (may rebuffer on slow networks)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,     // Min buffer: 5 seconds (aggressive)
                30_000,    // Max buffer: 30 seconds  
                500,       // Buffer for playback start: 0.5s (INSTANT!)
                1_500      // Buffer for rebuffer: 1.5 seconds
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
            
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            )
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                // Configure for gapless playback
                // When gapless is enabled, ExoPlayer will seamlessly transition between tracks
                // When disabled, there may be small gaps between tracks
                // ExoPlayer handles gapless automatically when media items are queued
                // Enabling pause at end is DISABLED for gapless playback
                pauseAtEndOfMediaItems = false
                
                // Add listener to attach Audio Normalization when audio session changes
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                            setupAudioNormalization(audioSessionId)
                        }
                    }
                })
            }
        
        // Observe Volume Normalization setting
        serviceScope.launch {
            sessionManager.volumeNormalizationEnabledFlow.collect { enabled ->
                if (enabled) {
                    // Apply effect if player is ready
                    val sessionId = (mediaSession?.player as? ExoPlayer)?.audioSessionId
                    if (sessionId != null && sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        setupAudioNormalization(sessionId)
                    }
                } else {
                    // Release effects
                    releaseAudioEffects()
                }
            }
        }
        
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setBitmapLoader(CoilBitmapLoader(this))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val connectionResult = super.onConnect(session, controller)
                    val sessionCommands = connectionResult.availableSessionCommands
                        .buildUpon()
                        .add(androidx.media3.session.SessionCommand("SET_OUTPUT_DEVICE", android.os.Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.accept(
                        sessionCommands,
                        connectionResult.availablePlayerCommands
                    )
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: androidx.media3.session.SessionCommand,
                    args: android.os.Bundle
                ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                    if (customCommand.customAction == "SET_OUTPUT_DEVICE") {
                        val deviceId = args.getString("DEVICE_ID")
                        if (deviceId != null) {
                            val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                            val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                            
                            val targetDevice = if (deviceId == "phone_speaker") {
                                devices.find { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                            } else {
                                devices.find { it.id.toString() == deviceId }
                            }
                            
                            val player = session.player
                            if (player is ExoPlayer) {
                                // If targetDevice is null, it clears the preference (default routing)
                                player.setPreferredAudioDevice(targetDevice)
                            }
                        }
                        return com.google.common.util.concurrent.Futures.immediateFuture(
                            androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
                        )
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
            
        // Customize the notification provider to ensure seekbar and controls work perfectly
        val notificationProvider = object : androidx.media3.session.DefaultMediaNotificationProvider(this) {
            override fun getMediaButtons(
                session: MediaSession,
                playerCommands: androidx.media3.common.Player.Commands,
                customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                showPauseButton: Boolean
            ): com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton> {
                // Ensure standard transport controls are used
                return super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
            }
        }
        setMediaNotificationProvider(notificationProvider)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Fix: Background Playback Termination -> Only stop if NOT playing
        if (player?.playWhenReady == false && player.mediaItemCount == 0) {
            stopSelf()
        }
    }
    
    private fun setupAudioNormalization(sessionId: Int) {
        serviceScope.launch {
            if (!sessionManager.isVolumeNormalizationEnabled()) return@launch
            
            releaseAudioEffects()

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    // API 28+: Use DynamicsProcessing for real-time normalization (Limiter)
                    // We create a Limiter configuration: Boost input -> Hard Limit -> Consistent Output
                    
                    val config = android.media.audiofx.DynamicsProcessing.Config.Builder(
                        android.media.audiofx.DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        2, // Assume stereo
                        false, 0, // preEq
                        false, 0, // mbc
                        false, 0, // postEq
                        true      // limiter
                    )
                    .setPreferredFrameDuration(10.0f)
                    .build()

                    dynamicsProcessing = android.media.audiofx.DynamicsProcessing(0, sessionId, config)
                    
                    dynamicsProcessing?.apply {
                        // 1. Boost Input Gain (Pre-Gain)
                        // Moderate boost (+5.5dB) to bring quiet songs up without destroying dynamics
                        setInputGainAllChannelsTo(5.5f)

                        // 2. Set Limiter (Ceiling)
                        // Gentler limiting to prevent "pumping" or stuttering artifacts
                        val limiterConfig = android.media.audiofx.DynamicsProcessing.Limiter(
                            true,   // inUse
                            true,   // enabled
                            0,      // linkGroup
                            10.0f,  // attackTimeMs (slower attack to let transients punch through)
                            200.0f, // releaseTimeMs (smoother recovery to avoid pumping)
                            4.0f,   // ratio (gentler compression, 4:1)
                            -3.0f,  // thresholdDb (slightly lower ceiling)
                            0.0f    // postGainDb
                        )
                        setLimiterAllChannelsTo(limiterConfig)
                        
                        enabled = true
                    }
                } else {
                    // Fallback for older devices (API < 28)
                    // Just use LoudnessEnhancer with a static gain
                    loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
                    loudnessEnhancer?.setTargetGain(800) // 800mB gain
                    loudnessEnhancer?.enabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun releaseAudioEffects() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
            dynamicsProcessing = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel() // Cancel scope
        releaseAudioEffects()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}


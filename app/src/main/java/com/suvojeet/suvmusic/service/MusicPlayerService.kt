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
    
    private var mediaSession: MediaSession? = null
    
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val isGaplessEnabled = sessionManager.isGaplessPlaybackEnabled()
        val isAutomixEnabled = sessionManager.isAutomixEnabled()
        
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
                if (isGaplessEnabled || isAutomixEnabled) {
                    // ExoPlayer handles gapless automatically when media items are queued
                    // Enabling pause at end is DISABLED for gapless playback
                    pauseAtEndOfMediaItems = false
                } else {
                    // Add small pause between tracks when gapless is disabled
                    pauseAtEndOfMediaItems = false
                }
                
                // Add listener to attach LoudnessEnhancer when audio session changes
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                            setupLoudnessEnhancer(audioSessionId)
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
                        setupLoudnessEnhancer(sessionId)
                    }
                } else {
                    // Release enhancer
                     try {
                        loudnessEnhancer?.enabled = false
                        loudnessEnhancer?.release()
                        loudnessEnhancer = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            stopSelf()
        }
    }
    
    private fun setupLoudnessEnhancer(sessionId: Int) {
        if (!sessionManager.isVolumeNormalizationEnabled()) return
        
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
            // Target gain in millibels. 500mB to 800mB is a reasonable boost for normalization without distortion
            loudnessEnhancer?.setTargetGain(800) 
            loudnessEnhancer?.enabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel() // Cancel scope
        
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}


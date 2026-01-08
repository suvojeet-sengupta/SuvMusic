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
    
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val isGaplessEnabled = sessionManager.isGaplessPlaybackEnabled()
        val isAutomixEnabled = sessionManager.isAutomixEnabled()
        
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                32 * 1024, // Min buffer 32s
                64 * 1024, // Max buffer 64s
                500,       // Buffer for playback 0.5s (Faster start)
                1000       // Buffer for rebuffer 1s
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
            .build()
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
    
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}


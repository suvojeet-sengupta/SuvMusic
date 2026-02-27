package com.suvojeet.suvmusic.pip

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.player.MusicPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates Picture-in-Picture logic for both audio and video modes.
 * 
 * - Video mode: 16:9 aspect ratio with play/pause, next, previous actions
 * - Audio mode: 1:1 aspect ratio with play/pause, next, previous actions
 */
@Singleton
class PipHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val musicPlayer: MusicPlayer
) {

    /**
     * Build PiP parameters with appropriate aspect ratio and remote actions.
     * Returns null if PiP is not supported on this API level.
     */
    fun buildPipParams(isVideoMode: Boolean, isPlaying: Boolean, isPipEnabled: Boolean = true): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val aspectRatio = if (isVideoMode) {
            Rational(16, 9) // Widescreen for video
        } else {
            Rational(1, 1) // Square for album art
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(createRemoteActions(isPlaying))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(if (isPipEnabled) isVideoMode else false)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }

    /**
     * Enter PiP mode if conditions are met.
     * For video mode: always enter (YouTube behavior).
     * For audio mode: only enter if "Dynamic Island" setting is enabled (existing behavior).
     */
    fun enterPipIfEligible(activity: Activity, forceVideoPip: Boolean = false, isPipEnabled: Boolean = true) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val state = musicPlayer.playerState.value
        val hasSong = state.currentSong != null

        if (!hasSong) return

        if (!isPipEnabled) return

        val isVideoMode = state.isVideoMode || forceVideoPip
        val params = buildPipParams(isVideoMode, state.isPlaying, isPipEnabled) ?: return

        try {
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            // PiP not supported or activity state doesn't allow it
        }
    }

    /**
     * Update PiP params dynamically (e.g., when play state changes in PiP).
     */
    fun updatePipParams(activity: Activity, isPipEnabled: Boolean = true) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val state = musicPlayer.playerState.value
        val params = buildPipParams(state.isVideoMode, state.isPlaying, isPipEnabled) ?: return

        try {
            activity.setPictureInPictureParams(params)
        } catch (e: Exception) {
            // Ignore — activity might not be in valid state
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteActions(isPlaying: Boolean): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()

        // Previous
        actions.add(
            RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_pip_previous),
                "Previous",
                "Skip to previous track",
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PREVIOUS,
                    Intent(PipActionReceiver.ACTION_PREVIOUS).setPackage(context.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        // Play/Pause
        val playPauseIcon = if (isPlaying) {
            R.drawable.ic_pip_pause
        } else {
            R.drawable.ic_pip_play
        }
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        actions.add(
            RemoteAction(
                Icon.createWithResource(context, playPauseIcon),
                playPauseTitle,
                "Toggle playback",
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PLAY_PAUSE,
                    Intent(PipActionReceiver.ACTION_PLAY_PAUSE).setPackage(context.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        // Next
        actions.add(
            RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_pip_next),
                "Next",
                "Skip to next track",
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_NEXT,
                    Intent(PipActionReceiver.ACTION_NEXT).setPackage(context.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        return actions
    }

    companion object {
        private const val REQUEST_CODE_PLAY_PAUSE = 100
        private const val REQUEST_CODE_NEXT = 101
        private const val REQUEST_CODE_PREVIOUS = 102
    }
}

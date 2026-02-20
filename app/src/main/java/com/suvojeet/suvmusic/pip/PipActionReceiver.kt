package com.suvojeet.suvmusic.pip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.suvojeet.suvmusic.player.MusicPlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles PiP remote action intents (play/pause, next, previous).
 * Registered in AndroidManifest and receives broadcasts from PiP action buttons.
 */
@AndroidEntryPoint
class PipActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var musicPlayer: MusicPlayer

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> musicPlayer.togglePlayPause()
            ACTION_NEXT -> musicPlayer.seekToNext()
            ACTION_PREVIOUS -> musicPlayer.seekToPrevious()
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.suvojeet.suvmusic.pip.PLAY_PAUSE"
        const val ACTION_NEXT = "com.suvojeet.suvmusic.pip.NEXT"
        const val ACTION_PREVIOUS = "com.suvojeet.suvmusic.pip.PREVIOUS"
    }
}

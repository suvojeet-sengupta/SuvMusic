package com.suvojeet.suvmusic.service

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object CallPlaybackPolicy {
    const val RESUME_WINDOW_MS = 2 * 60 * 1000L

    fun shouldResume(
        callDurationMs: Long,
        autoResumeEnabled: Boolean,
        personalOutputLost: Boolean,
    ): Boolean = autoResumeEnabled && !personalOutputLost && callDurationMs < RESUME_WINDOW_MS
}

/**
 * Pauses for the whole of a call and decides on resume itself, because over Bluetooth the
 * player can stop without a transient focus loss: the A2DP to SCO switch reports
 * becoming-noisy, and some headsets send their own pause when the call rings.
 */
internal class CallPlaybackGuard(
    context: Context,
    private val scope: CoroutineScope,
    private val playerProvider: () -> Player?,
    private val isPlayDuringCallsEnabled: suspend () -> Boolean,
    private val isAutoResumeEnabled: suspend () -> Boolean,
) {
    private enum class State { IDLE, DETECTING, IN_CALL }

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var state = State.IDLE
    private var job: Job? = null
    private var lastUserPauseAt = NEVER
    private var modeListener: AudioManager.OnModeChangedListener? = null

    val isHandlingCall: Boolean get() = state == State.IN_CALL

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || modeListener != null) return
        val listener = AudioManager.OnModeChangedListener(::onModeChanged)
        audioManager?.addOnModeChangedListener(ContextCompat.getMainExecutor(appContext), listener)
        modeListener = listener
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modeListener?.let { audioManager?.removeOnModeChangedListener(it) }
        }
        modeListener = null
        job?.cancel()
        job = null
        state = State.IDLE
    }

    /** The player stopped or was silenced without the user asking; this may be a call. */
    fun onPlaybackInterrupted() {
        if (state != State.IDLE) return
        state = State.DETECTING
        job = scope.launch(Dispatchers.Main.immediate) {
            val deadline = SystemClock.elapsedRealtime() + CALL_DETECT_GRACE_MS
            while (SystemClock.elapsedRealtime() < deadline) {
                if (isAnyCallMode()) {
                    watchCall()
                    return@launch
                }
                delay(CALL_DETECT_POLL_MS)
            }
            state = State.IDLE
        }
    }

    fun onInterruptionCleared() {
        if (state != State.DETECTING) return
        job?.cancel()
        state = State.IDLE
    }

    fun onUserPaused() {
        if (state == State.IN_CALL) return
        lastUserPauseAt = SystemClock.elapsedRealtime()
        onInterruptionCleared()
    }

    private fun onModeChanged(mode: Int) {
        if (state == State.IN_CALL || !isTelephonyMode(mode)) return
        val player = playerProvider() ?: return
        val pausedByHeadset = SystemClock.elapsedRealtime() - lastUserPauseAt < HEADSET_PAUSE_WINDOW_MS
        if (!player.playWhenReady && !pausedByHeadset) return
        job?.cancel()
        state = State.DETECTING
        job = scope.launch(Dispatchers.Main.immediate) { watchCall() }
    }

    private suspend fun watchCall() {
        if (isPlayDuringCallsEnabled()) {
            state = State.IDLE
            return
        }
        state = State.IN_CALL
        val startedAt = SystemClock.elapsedRealtime()
        val hadPersonalOutput = hasPersonalOutput()
        playerProvider()?.takeIf { it.playWhenReady }?.pause()

        val endedAt = awaitCallEnd()
        if (hadPersonalOutput) delay(ROUTE_SETTLE_MS)

        val resume = CallPlaybackPolicy.shouldResume(
            callDurationMs = endedAt - startedAt,
            autoResumeEnabled = isAutoResumeEnabled(),
            personalOutputLost = hadPersonalOutput && !hasPersonalOutput(),
        )
        state = State.IDLE
        val player = playerProvider() ?: return
        if (resume) player.play() else if (player.playWhenReady) player.pause()
    }

    /** Mode can dip to normal between ringing and answering, so it must stay normal a moment. */
    private suspend fun awaitCallEnd(): Long {
        var normalSince = NEVER
        while (true) {
            delay(CALL_END_POLL_MS)
            val now = SystemClock.elapsedRealtime()
            if (isAnyCallMode()) {
                normalSince = NEVER
            } else {
                if (normalSince == NEVER) normalSince = now
                if (now - normalSince >= CALL_END_DEBOUNCE_MS) return normalSince
            }
        }
    }

    private fun isAnyCallMode(): Boolean =
        (audioManager?.mode ?: AudioManager.MODE_NORMAL) != AudioManager.MODE_NORMAL

    /** Excludes MODE_IN_COMMUNICATION, which voice-chat apps can hold while music is wanted. */
    private fun isTelephonyMode(mode: Int): Boolean =
        mode == AudioManager.MODE_RINGTONE ||
            mode == AudioManager.MODE_IN_CALL ||
            mode == MODE_CALL_SCREENING ||
            mode == MODE_CALL_REDIRECT

    private fun hasPersonalOutput(): Boolean {
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return false
        return devices.any { it.type in PERSONAL_OUTPUT_TYPES }
    }

    private companion object {
        const val NEVER = -1L
        const val CALL_DETECT_GRACE_MS = 4_000L
        const val CALL_DETECT_POLL_MS = 250L
        const val CALL_END_POLL_MS = 500L
        const val CALL_END_DEBOUNCE_MS = 2_000L
        const val ROUTE_SETTLE_MS = 1_000L
        const val HEADSET_PAUSE_WINDOW_MS = 3_000L
        const val MODE_CALL_SCREENING = 4
        const val MODE_CALL_REDIRECT = 5

        val PERSONAL_OUTPUT_TYPES = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}

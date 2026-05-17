package com.suvojeet.suvmusic.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Robust TTS announcer.
 *
 * - Handles late initialization with a queue of pending requests.
 * - Routes audio through STREAM_MUSIC + AudioAttributes so it reaches Bluetooth
 *   A2DP and BLE headsets correctly.
 * - Per-utterance audio focus + ducking + completion/error callbacks so the
 *   caller can reliably restore the music player's volume.
 * - Falls back to English if the device locale isn't supported.
 */
@Singleton
class TTSManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false
    @Volatile private var initFailed = false

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private data class Pending(
        val id: String,
        val text: String,
        val volume: Float,
        val onStart: (() -> Unit)?,
        val onDone: (() -> Unit)?,
        val onError: (() -> Unit)?,
    )

    private val pending = mutableListOf<Pending>()
    private val callbacks = ConcurrentHashMap<String, Pending>()
    private var focusRequest: AudioFocusRequest? = null

    init {
        attemptInit()
    }

    private fun attemptInit() {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("TTSManager", "TTS construction failed", e)
            initFailed = true
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e("TTSManager", "TTS init failed with status $status")
            initFailed = true
            return
        }

        val engine = tts ?: return
        val deviceLocale = Locale.getDefault()
        val r = try { engine.setLanguage(deviceLocale) } catch (e: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTSManager", "Locale $deviceLocale unsupported, falling back to English")
            try { engine.setLanguage(Locale.ENGLISH) } catch (_: Exception) {}
        }

        // Route TTS through media stream so it follows the active audio
        // device (Bluetooth headset, BLE LE audio, wired, speaker).
        try {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        } catch (_: Exception) { /* setAudioAttributes is best-effort */ }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId ?: return
                callbacks[utteranceId]?.onStart?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                utteranceId ?: return
                val p = callbacks.remove(utteranceId)
                p?.onDone?.invoke()
                releaseFocusIfIdle()
            }

            @Deprecated("Replaced by onError(utteranceId, errorCode)")
            override fun onError(utteranceId: String?) {
                utteranceId ?: return
                val p = callbacks.remove(utteranceId)
                p?.onError?.invoke()
                releaseFocusIfIdle()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId ?: return
                Log.w("TTSManager", "TTS error code=$errorCode for $utteranceId")
                val p = callbacks.remove(utteranceId)
                p?.onError?.invoke()
                releaseFocusIfIdle()
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                utteranceId ?: return
                val p = callbacks.remove(utteranceId)
                p?.onError?.invoke()
                releaseFocusIfIdle()
            }
        })

        isInitialized = true

        // Drain anything that arrived while we were initializing.
        synchronized(pending) {
            val snapshot = pending.toList()
            pending.clear()
            snapshot
        }.forEach { performSpeak(it) }
    }

    /**
     * Speak [text] with optional callbacks. [volume] is 0.0..1.0 and applies
     * only to the TTS utterance — the caller is responsible for restoring
     * music-player volume on `onDone`/`onError`.
     */
    fun speak(
        text: String,
        volume: Float = 1.0f,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: (() -> Unit)? = null,
    ) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        val req = Pending(
            id = "TTS_${UUID.randomUUID()}",
            text = text,
            volume = volume.coerceIn(0f, 1f),
            onStart = onStart,
            onDone = onDone,
            onError = onError,
        )

        if (initFailed) {
            // Best-effort recovery: try once more in case the engine became available.
            attemptInit()
        }

        if (!isInitialized) {
            synchronized(pending) { pending.add(req) }
            return
        }

        performSpeak(req)
    }

    private fun performSpeak(req: Pending) {
        val engine = tts ?: run {
            req.onError?.invoke()
            return
        }
        callbacks[req.id] = req

        requestFocus()

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, req.volume)
            // STREAM_MUSIC keeps us routed through the active audio device,
            // including Bluetooth A2DP / BLE headsets. Without this, some
            // engines fall back to STREAM_NOTIFICATION which a Bluetooth
            // headset may not render.
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        // QUEUE_FLUSH: replaces any pending/in-progress utterance with this one.
        // Why: when the user skips rapidly, each song transition calls speak();
        // QUEUE_ADD would line up every skipped song, announcing them all in sequence.
        // QUEUE_FLUSH ensures only the latest announcement is spoken. The cancelled
        // utterance fires onStop (interrupted=true) → its onError callback restores
        // ducked music volume so we don't leak ducking state.
        val result = try {
            engine.speak(req.text, TextToSpeech.QUEUE_FLUSH, params, req.id)
        } catch (e: Exception) {
            Log.e("TTSManager", "speak() threw", e)
            TextToSpeech.ERROR
        }

        if (result == TextToSpeech.ERROR) {
            callbacks.remove(req.id)
            req.onError?.invoke()
            releaseFocusIfIdle()
        }
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest != null) return
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val req = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { /* the music player handles ducking itself */ }
                .build()
            try { audioManager.requestAudioFocus(req) } catch (_: Exception) {}
            focusRequest = req
        } else {
            @Suppress("DEPRECATION")
            try {
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                )
            } catch (_: Exception) {}
        }
    }

    private fun releaseFocusIfIdle() {
        if (callbacks.isNotEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { try { audioManager.abandonAudioFocusRequest(it) } catch (_: Exception) {} }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            try { audioManager.abandonAudioFocus(null) } catch (_: Exception) {}
        }
    }

    fun stop() {
        try { tts?.stop() } catch (_: Exception) {}
        callbacks.values.forEach { it.onError?.invoke() }
        callbacks.clear()
        synchronized(pending) { pending.clear() }
        releaseFocusIfIdle()
    }

    fun shutdown() {
        try { tts?.stop() } catch (_: Exception) {}
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        isInitialized = false
        initFailed = false
        callbacks.clear()
        synchronized(pending) { pending.clear() }
        releaseFocusIfIdle()
    }
}

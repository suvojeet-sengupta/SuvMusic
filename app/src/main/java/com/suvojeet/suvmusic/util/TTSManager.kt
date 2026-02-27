package com.suvojeet.suvmusic.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeak: String? = null

    init {
        // Initialize TTS
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "Language not supported")
            } else {
                isInitialized = true
                // Speak any pending message
                pendingSpeak?.let {
                    speak(it)
                    pendingSpeak = null
                }
            }
        } else {
            Log.e("TTSManager", "Initialization failed")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
        } else {
            pendingSpeak = text // Queue it up for when initialized
            // Attempt re-init if null
            if (tts == null) {
                tts = TextToSpeech(context, this)
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

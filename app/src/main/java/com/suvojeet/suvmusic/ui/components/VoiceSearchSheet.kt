package com.suvojeet.suvmusic.ui.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val MAX_RMS_DB = 10f
private const val MAX_RING_GROWTH = 0.45f

internal fun voiceSearchErrorMessage(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that"
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Check your connection"
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone access is off"
    else -> "Couldn't hear you"
}

/** Listens inside the app, showing words as they're recognised, then searches for them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSearchSheet(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    var attempt by remember { mutableIntStateOf(0) }
    var transcript by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var level by remember { mutableFloatStateOf(0f) }

    DisposableEffect(attempt) {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                level = 0f
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onRmsChanged(rmsdB: Float) {
                level = (rmsdB / MAX_RMS_DB).coerceIn(0f, 1f)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { transcript = it }
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: transcript.takeIf { it.isNotBlank() }
                if (text != null) currentOnResult(text) else error = voiceSearchErrorMessage(SpeechRecognizer.ERROR_NO_MATCH)
            }

            override fun onError(code: Int) {
                level = 0f
                error = voiceSearchErrorMessage(code)
            }
        })
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        )
        onDispose { recognizer.destroy() }
    }

    val ringScale by animateFloatAsState(
        targetValue = 1f + level * MAX_RING_GROWTH,
        animationSpec = spring(stiffness = 400f),
        label = "voiceLevel"
    )
    val colors = MaterialTheme.colorScheme
    val isListening = error == null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = error ?: transcript.ifBlank { "Say a song or artist" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (transcript.isBlank() && error == null) colors.onSurfaceVariant else colors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(ringScale)
                        .background(colors.primary.copy(alpha = 0.18f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(if (isListening) colors.primary else colors.surfaceVariant, CircleShape)
                        .clickable(enabled = !isListening) {
                            error = null
                            transcript = ""
                            attempt++
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isListening) "Listening" else "Try again",
                        tint = if (isListening) colors.onPrimary else colors.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isListening) "Listening" else "Tap the mic to try again",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

package com.inialpha.executiveai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class VoiceRecognitionState {
    object Idle : VoiceRecognitionState()
    object Listening : VoiceRecognitionState()
    data class PartialResult(val text: String) : VoiceRecognitionState()
    data class FinalResult(val text: String) : VoiceRecognitionState()
    data class Error(val message: String) : VoiceRecognitionState()
}

/**
 * Thin wrapper around Android's native [SpeechRecognizer] (android.speech), preferring on-device
 * recognition where the device supports it, per REQUIREMENTS.md section 12. This produces plain
 * recognized text only — turning that text into a structured executive action (the
 * "Intent/action interpretation" step of the voice → proposed action → confirmation → execution
 * pipeline) is a separate, not-yet-implemented NLU step; see [com.inialpha.executiveai.viewmodel.AssistantViewModel]
 * for where that hook belongs.
 */
class SpeechRecognizerManager(private val context: Context) {

    fun isRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(): Flow<VoiceRecognitionState> = callbackFlow {
        val recognizer = if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(VoiceRecognitionState.Listening)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                trySend(VoiceRecognitionState.FinalResult(text))
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (text.isNotBlank()) trySend(VoiceRecognitionState.PartialResult(text))
            }

            override fun onError(error: Int) {
                trySend(VoiceRecognitionState.Error(describeError(error)))
                close()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    private fun describeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy — try again."
        else -> "Voice recognition error ($error)"
    }
}

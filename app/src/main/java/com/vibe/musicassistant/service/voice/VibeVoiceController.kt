package com.vibe.musicassistant.service.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Tap-to-talk wrapper around Android's [SpeechRecognizer]. One utterance per
 * [start]; the recogniser is destroyed after results, errors, [cancel] or a timeout.
 * No audio is stored by VIBE. Must be used on the main thread.
 */
class VibeVoiceController(private val context: Context) {

    interface Listener {
        fun onListening()
        fun onPartial(text: String)
        fun onProcessing()
        fun onResults(alternatives: List<String>)
        fun onError(message: String)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private var recognizer: SpeechRecognizer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var finished = true

    val isActive: Boolean get() = recognizer != null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(languageTag: String, listener: Listener) {
        cancel()
        if (!isAvailable()) {
            listener.onError("Speech recognition isn't available. Install or enable the Google app.")
            return
        }
        finished = false
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                duckMusic()
                listener.onListening()
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                restoreMusic()
                if (!finished) listener.onProcessing()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults.texts().firstOrNull()
                if (!text.isNullOrBlank() && !finished) listener.onPartial(text)
            }
            override fun onResults(results: Bundle?) {
                if (finished) return
                finish()
                val texts = results.texts()
                if (texts.isEmpty()) listener.onError("I didn't catch that. Tap and try again.")
                else listener.onResults(texts)
            }
            override fun onError(error: Int) {
                if (finished) return
                finish()
                listener.onError(messageFor(error))
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        r.startListening(intent)

        handler.postDelayed({
            if (!finished) {
                finish()
                listener.onError("I didn't hear a command. Tap the orb and try again.")
            }
        }, TIMEOUT_MS)
    }

    fun cancel() {
        finished = true
        handler.removeCallbacksAndMessages(null)
        restoreMusic()
        recognizer?.let {
            runCatching { it.cancel() }
            runCatching { it.destroy() }
        }
        recognizer = null
    }

    private fun finish() {
        finished = true
        handler.removeCallbacksAndMessages(null)
        restoreMusic()
        // Destroy outside the recogniser callback.
        handler.post { cancel() }
    }

    /** Briefly lowers the music so the command is heard over it. */
    private fun duckMusic() {
        if (focusRequest != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .build()
        if (audio?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusRequest = request
        }
    }

    private fun restoreMusic() {
        focusRequest?.let { audio?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun Bundle?.texts(): List<String> =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().filter { it.isNotBlank() }

    private fun messageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "I didn't catch that. Tap and try again."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
            "Speech needs a connection right now. Check your internet."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone access is off for VIBE."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The microphone is busy. Try again in a moment."
        SpeechRecognizer.ERROR_AUDIO -> "Couldn't use the microphone."
        else -> "Speech recognition stopped. Tap to try again."
    }

    private companion object {
        const val TIMEOUT_MS = 12_000L
    }
}

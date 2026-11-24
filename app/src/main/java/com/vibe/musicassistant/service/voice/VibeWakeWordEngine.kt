package com.vibe.musicassistant.service.voice

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

/**
 * Offline "Hey VIBE" / "Hello VIBE" spotter built on Vosk. Audio is processed on the
 * phone only, restricted to a tiny grammar, and never stored or sent anywhere.
 * [load] must run off the main thread; everything else on the main thread.
 */
class VibeWakeWordEngine(private val modelPath: String) {
    private val handler = Handler(Looper.getMainLooper())
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speech: SpeechService? = null
    private var triggered = false

    val isRunning: Boolean get() = speech != null

    fun load(): Boolean = try {
        if (model == null) model = Model(modelPath)
        true
    } catch (e: Throwable) {
        false
    }

    fun start(onWake: () -> Unit): Boolean {
        val m = model ?: return false
        stop()
        triggered = false
        return try {
            val rec = Recognizer(m, SAMPLE_RATE, GRAMMAR)
            val service = SpeechService(rec, SAMPLE_RATE)
            recognizer = rec
            speech = service
            service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) = check(hypothesis, "partial", onWake)
                override fun onResult(hypothesis: String?) = check(hypothesis, "text", onWake)
                override fun onFinalResult(hypothesis: String?) = check(hypothesis, "text", onWake)
                override fun onError(exception: Exception?) {
                    handler.post { stop() }
                }
                override fun onTimeout() = Unit
            })
        } catch (e: Exception) {
            stop()
            false
        }
    }

    private fun check(hypothesis: String?, key: String, onWake: () -> Unit) {
        if (triggered || hypothesis.isNullOrBlank()) return
        val text = runCatching { JSONObject(hypothesis).optString(key) }.getOrDefault("")
        if (isWakePhrase(text)) {
            triggered = true
            handler.post {
                stop()
                onWake()
            }
        }
    }

    fun stop() {
        speech?.let {
            runCatching { it.stop() }
            runCatching { it.shutdown() }
        }
        speech = null
        recognizer?.let { runCatching { it.close() } }
        recognizer = null
    }

    fun release() {
        stop()
        model?.let { runCatching { it.close() } }
        model = null
    }

    companion object {
        private const val SAMPLE_RATE = 16_000f
        private const val GRAMMAR =
            "[\"hey vibe\", \"hello vibe\", \"hi vibe\", \"okay vibe\", \"hey vibes\", \"[unk]\"]"
        private val WAKE = Regex("\\b(hey|hello|hi|okay|ok)\\s+vibes?\\b")

        fun isWakePhrase(text: String): Boolean = WAKE.containsMatchIn(text.lowercase())
    }
}

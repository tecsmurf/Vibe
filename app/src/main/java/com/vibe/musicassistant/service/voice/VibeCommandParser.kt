package com.vibe.musicassistant.service.voice

import com.vibe.musicassistant.domain.model.VoiceCommand
import java.util.Locale

/**
 * Turns recognised speech (English, Hindi in Devanagari, or romanised Hinglish)
 * into a [VoiceCommand]. Pure Kotlin so it is unit-testable and can later be swapped
 * for an AI intent engine behind the same `parse` signature.
 */
object VibeCommandParser {

    data class Match(val command: VoiceCommand, val heard: String)

    /** Tries each recogniser alternative in order and returns the first that parses. */
    fun parseBest(alternatives: List<String>): Match? =
        alternatives.firstNotNullOfOrNull { text -> parse(text)?.let { Match(it, text) } }

    fun parse(text: String): VoiceCommand? {
        val normalized = normalize(text)
        if (normalized.isBlank()) return null
        val words = normalized.split(' ').toSet()
        val padded = " $normalized "
        fun has(vararg options: String) = options.any { it in words }
        fun phrase(vararg options: String) = options.any { padded.contains(" $it ") }

        val volumeWords = has("volume", "awaaz", "awaz", "aawaz", "sound", "louder", "quieter") ||
            phrase("turn it up", "turn it down", "turn up", "turn down")
        if (volumeWords) {
            return when {
                has("down", "kam", "decrease", "lower", "low", "quieter", "quiet", "reduce", "less",
                    "ghatao", "ghata", "dheere", "dheema", "dheemi", "slow") ||
                    phrase("turn it down", "turn down") -> VoiceCommand.VOLUME_DOWN
                has("up", "badhao", "badha", "badhaao", "barhao", "increase", "raise", "louder",
                    "loud", "tez", "zyada", "jyada", "high", "more", "full", "max") ||
                    phrase("turn it up", "turn up") -> VoiceCommand.VOLUME_UP
                else -> null
            }
        }

        return when {
            phrase("fast forward", "skip ahead", "aage badhao", "aage karo", "go forward") ||
                has("forward") -> VoiceCommand.SEEK_FORWARD
            has("next", "skip", "agla", "agle", "agli", "aglaa", "change", "doosra", "dusra",
                "dusri", "doosri") -> VoiceCommand.NEXT_TRACK
            has("previous", "prev", "pichla", "pichle", "pichli", "pichhla", "pichhle", "pichhli",
                "back", "wapas", "vapas") || phrase("last song", "last track") -> VoiceCommand.PREVIOUS_TRACK
            has("rewind", "peeche", "piche") -> VoiceCommand.SEEK_BACKWARD
            has("pause", "stop", "band", "bandh", "ruko", "roko", "rok", "ruk", "bas", "chup",
                "mute", "silence", "hold") -> VoiceCommand.PAUSE
            has("play", "resume", "chalao", "chala", "chalu", "chalaao", "bajao", "baja", "bajaao",
                "shuru", "start", "continue", "unpause") -> VoiceCommand.PLAY
            else -> null
        }
    }

    internal fun normalize(text: String): String {
        var t = text.lowercase(Locale.ROOT)
        for ((hindi, roman) in DEVANAGARI) t = t.replace(hindi, " $roman ")
        return t.replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** Hindi words recognisers return in Devanagari, mapped to the romanised keywords above. */
    private val DEVANAGARI: List<Pair<String, String>> = listOf(
        "अगला" to "agla", "अगले" to "agle", "अगली" to "agli",
        "पिछला" to "pichla", "पिछले" to "pichle", "पिछली" to "pichli",
        "गाना" to "gaana", "गाने" to "gaane", "गीत" to "geet",
        "बंद" to "band", "बन्द" to "band",
        "रोको" to "roko", "रुको" to "ruko", "रोक" to "rok",
        "चलाओ" to "chalao", "चालू" to "chalu", "चला" to "chala",
        "बजाओ" to "bajao", "बजा" to "baja", "शुरू" to "shuru",
        "वॉल्यूम" to "volume", "वॉल्युम" to "volume", "वोल्यूम" to "volume",
        "आवाज़" to "awaaz", "आवाज" to "awaaz",
        "बढ़ाओ" to "badhao", "बढाओ" to "badhao", "बढ़ा" to "badha", "बढा" to "badha",
        "तेज़" to "tez", "तेज" to "tez", "ज़्यादा" to "zyada", "ज्यादा" to "zyada",
        "कम" to "kam", "घटाओ" to "ghatao", "धीमा" to "dheema", "धीरे" to "dheere",
        "म्यूजिक" to "music", "म्यूज़िक" to "music",
        "नेक्स्ट" to "next", "प्रीवियस" to "previous", "प्ले" to "play",
        "पॉज़" to "pause", "पॉज" to "pause", "स्टॉप" to "stop",
        "वापस" to "wapas", "पीछे" to "peeche", "आगे" to "aage",
        "करो" to "karo", "कर" to "kar", "दो" to "do",
    ).sortedByDescending { it.first.length }
}

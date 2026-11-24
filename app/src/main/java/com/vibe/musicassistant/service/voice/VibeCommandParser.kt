package com.vibe.musicassistant.service.voice

import com.vibe.musicassistant.domain.model.VoiceCommand
import java.util.Locale

/**
 * Turns recognised speech (English, Hindi in Devanagari, or romanised Hinglish)
 * into a [VoiceCommand]. Pure Kotlin so it is unit-testable and can later be swapped
 * for an AI intent engine behind the same `parse` signature.
 */
object VibeCommandParser {

    /** [query] is set only for [VoiceCommand.PLAY_SEARCH] ("play Kesariya"). */
    data class Match(val command: VoiceCommand, val heard: String, val query: String? = null)

    /** Tries each recogniser alternative in order and returns the first that parses. */
    fun parseBest(alternatives: List<String>): Match? =
        alternatives.firstNotNullOfOrNull { text -> parseFull(text) }

    fun parseFull(text: String): Match? {
        // "Hey VIBE, next song" said in one breath: ignore the wake phrase.
        val command = text.trim().replace(WAKE_PREFIX, "").trim()
        searchQuery(command)?.let { return Match(VoiceCommand.PLAY_SEARCH, text, it) }
        return parse(command)?.let { Match(it, text) }
    }

    private val WAKE_PREFIX = Regex("^(hey|hello|hi|ok|okay)[ ,]+vibes?[ ,.!]*", RegexOption.IGNORE_CASE)

    /**
     * Detects "play <something>" / "<something> chalao|bajao|lagao" and returns <something>,
     * or null when the request is a plain command ("play music", "agla gaana chalao").
     */
    fun searchQuery(text: String): String? {
        val original = text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s']"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .toMutableList()
        if (original.isEmpty()) return null
        val roman = original.map { DEVANAGARI_WORDS[it] ?: it }.toMutableList()

        // Drop politeness/auxiliary words at the end: "... chalao na", "... play karo please".
        while (roman.size > 1 && roman.last() in TRAILING_AUX) {
            roman.removeAt(roman.lastIndex)
            original.removeAt(original.lastIndex)
        }

        val rest: List<String> = when {
            roman.first() == "play" -> original.drop(1)
            roman.size >= 2 && roman.first() == "please" && roman[1] == "play" -> original.drop(2)
            roman.last() in PLAY_VERBS -> original.dropLast(1)
            else -> return null
        }
        val restRoman = rest.map { DEVANAGARI_WORDS[it] ?: it }
        if (restRoman.any { it in COMMAND_WORDS }) return null
        if (restRoman.all { it in FILLER }) return null

        // Trim filler from both ends: "the song kesariya" -> "kesariya".
        var start = 0
        var end = rest.size
        while (start < end && restRoman[start] in EDGE_FILLER) start++
        while (end > start && restRoman[end - 1] in EDGE_FILLER) end--
        val query = rest.subList(start, end).joinToString(" ").trim()
        return query.ifBlank { null }
    }

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
                "picchla", "picchle", "pichala", "peechla", "peechle", "piche", "previos", "privious",
                "back", "wapas", "vapas") || phrase("last song", "last track") -> VoiceCommand.PREVIOUS_TRACK
            has("rewind", "peeche") -> VoiceCommand.SEEK_BACKWARD
            has("pause", "stop", "band", "bandh", "ruko", "roko", "rok", "ruk", "bas", "chup",
                "mute", "silence", "hold") -> VoiceCommand.PAUSE
            has("play", "resume", "chalao", "chala", "chalu", "chalaao", "bajao", "baja", "bajaao",
                "shuru", "start", "continue", "unpause") -> VoiceCommand.PLAY
            else -> null
        }
    }

    private val PLAY_VERBS = setOf(
        "chalao", "chala", "chalaao", "bajao", "baja", "bajaao", "lagao", "laga", "lagaao", "sunao", "suna", "play",
    )
    private val TRAILING_AUX = setOf("karo", "kar", "do", "dena", "de", "na", "please", "now", "abhi", "zara", "jara")
    private val COMMAND_WORDS = setOf(
        "next", "previous", "prev", "agla", "agle", "agli", "pichla", "pichle", "pichli", "picchla", "pichhla",
        "volume", "awaaz", "skip", "forward", "rewind", "pause", "stop", "band", "bandh",
    )
    private val EDGE_FILLER = setOf(
        "the", "a", "song", "songs", "gaana", "gaane", "gana", "gane", "geet", "track", "music", "ke", "ka", "ki",
        "wala", "wale", "wali", "mera", "mere", "my", "on", "spotify", "youtube",
    )
    private val FILLER = EDGE_FILLER + setOf(
        "some", "something", "it", "again", "me", "koi", "ek", "sa", "bhi", "phir", "fir", "se", "wapas", "now",
        "abhi", "please", "this", "that", "yeh", "ye", "woh", "wo", "kuch", "acha", "accha", "achha", "nice",
    )

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

    private val DEVANAGARI_WORDS: Map<String, String> = DEVANAGARI.toMap()
}

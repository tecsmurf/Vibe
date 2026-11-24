package com.vibe.musicassistant.service.voice

import com.vibe.musicassistant.domain.model.VoiceCommand
import com.vibe.musicassistant.domain.model.VoiceCommand.NEXT_TRACK
import com.vibe.musicassistant.domain.model.VoiceCommand.PAUSE
import com.vibe.musicassistant.domain.model.VoiceCommand.PLAY
import com.vibe.musicassistant.domain.model.VoiceCommand.PREVIOUS_TRACK
import com.vibe.musicassistant.domain.model.VoiceCommand.SEEK_BACKWARD
import com.vibe.musicassistant.domain.model.VoiceCommand.SEEK_FORWARD
import com.vibe.musicassistant.domain.model.VoiceCommand.VOLUME_DOWN
import com.vibe.musicassistant.domain.model.VoiceCommand.VOLUME_UP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VibeCommandParserTest {
    private fun check(expected: VoiceCommand?, vararg phrases: String) {
        for (p in phrases) assertEquals("\"$p\"", expected, VibeCommandParser.parse(p))
    }

    @Test fun english() {
        check(NEXT_TRACK, "Next song", "next", "Skip this track", "play the next song", "change the song")
        check(PREVIOUS_TRACK, "Previous song", "go back", "play the last song")
        check(PAUSE, "Pause music", "stop the music", "Pause.")
        check(PLAY, "Play music", "resume", "play")
        check(VOLUME_UP, "Volume up", "turn it up", "louder", "increase the volume")
        check(VOLUME_DOWN, "Volume down", "turn it down", "lower the volume", "quieter")
        check(SEEK_FORWARD, "fast forward", "skip ahead")
        check(SEEK_BACKWARD, "rewind")
    }

    @Test fun hinglish() {
        check(NEXT_TRACK, "Agla gaana", "agla song chalao", "agle gaane pe jao", "dusra gaana")
        check(PREVIOUS_TRACK, "Pichla gaana", "pichle gaane pe jao", "wapas")
        check(PAUSE, "Gaana band karo", "Music band karo", "ruko", "bas karo")
        check(PLAY, "Music chalao", "gaana bajao", "shuru karo")
        check(VOLUME_UP, "Volume badhao", "awaaz tez karo", "volume zyada karo")
        check(VOLUME_DOWN, "Volume kam karo", "awaaz kam karo", "volume ghatao")
    }

    @Test fun devanagari() {
        check(NEXT_TRACK, "अगला गाना", "अगला गाना चलाओ")
        check(PREVIOUS_TRACK, "पिछला गाना")
        check(PAUSE, "गाना बंद करो", "म्यूजिक बंद करो")
        check(PLAY, "म्यूजिक चलाओ", "गाना बजाओ")
        check(VOLUME_UP, "वॉल्यूम बढ़ाओ", "आवाज़ तेज़ करो")
        check(VOLUME_DOWN, "वॉल्यूम कम करो", "आवाज कम करो")
    }

    @Test fun unknownSpeechIsIgnored() {
        check(null, "", "what's the weather", "hello there", "volume")
    }

    @Test fun picksFirstAlternativeThatParses() {
        val match = VibeCommandParser.parseBest(listOf("hello", "agla gaana", "pause"))
        assertEquals(NEXT_TRACK, match?.command)
        assertEquals("agla gaana", match?.heard)
        assertNull(VibeCommandParser.parseBest(listOf("hello")))
    }
}

class VibeSearchParserTest {
    private fun query(text: String) = VibeCommandParser.parseFull(text)

    @Test fun playsSpecificSongs() {
        assertEquals("despacito", query("Play Despacito")?.query)
        assertEquals("kesariya", query("kesariya chalao")?.query)
        assertEquals("tum hi ho", query("tum hi ho gaana bajao")?.query)
        assertEquals("arijit singh", query("play songs by arijit singh")?.query?.removePrefix("by "))
        assertEquals("shape of you", query("hey vibe play shape of you")?.query)
        assertEquals(VoiceCommand.PLAY_SEARCH, query("play believer by imagine dragons")?.command)
    }

    @Test fun plainCommandsAreNotSearches() {
        assertEquals(PLAY, query("play music")?.command)
        assertEquals(PLAY, query("play")?.command)
        assertEquals(PLAY, query("music chalao")?.command)
        assertEquals(NEXT_TRACK, query("play the next song")?.command)
        assertEquals(NEXT_TRACK, query("agla gaana chalao")?.command)
        assertEquals(PREVIOUS_TRACK, query("pichla gaana chalao")?.command)
        assertEquals(PREVIOUS_TRACK, query("hey vibe previous song")?.command)
        assertEquals(PAUSE, query("gaana band karo")?.command)
        assertEquals(PLAY, query("play it again")?.command)
    }

    @Test fun wakePhraseDetection() {
        assertEquals(true, com.vibe.musicassistant.service.voice.VibeWakeWordEngine.isWakePhrase("hey vibe"))
        assertEquals(true, com.vibe.musicassistant.service.voice.VibeWakeWordEngine.isWakePhrase("hello vibe"))
        assertEquals(false, com.vibe.musicassistant.service.voice.VibeWakeWordEngine.isWakePhrase("vibe"))
        assertEquals(false, com.vibe.musicassistant.service.voice.VibeWakeWordEngine.isWakePhrase("[unk]"))
    }
}

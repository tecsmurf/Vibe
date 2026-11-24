package com.vibe.musicassistant.utils

/** Well-known music players. Any other app with an active media session also counts. */
object MusicApps {
    val KNOWN: Set<String> = setOf(
        "com.spotify.music",
        "com.spotify.lite",
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube",
        "com.jio.media.jiobeats",
        "com.bsbportal.music",
        "com.gaana",
        "com.amazon.mp3",
        "com.apple.android.music",
        "com.soundcloud.android",
        "deezer.android.app",
        "com.aspiro.tidal",
        "com.maxmpz.audioplayer",
        "com.sec.android.app.music",
        "com.miui.player",
        "com.google.android.apps.youtube.music.pwa",
        "in.startv.hotstar",
        "com.shazam.android",
        "com.pandora.android",
        "com.audible.application",
        "org.videolan.vlc",
    )
}

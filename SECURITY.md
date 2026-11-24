# VIBE security notes

VIBE runs with powerful Android access (overlay, notification access, microphone), so it is
built to do as little as possible with it. No app can honestly claim a perfect score; this
file lists what is covered and what is left.

## What VIBE can and can't do

- It can only send media actions to music players (play, pause, next, previous, seek,
  play-by-name) and change the **media** volume. There is no code path that opens arbitrary
  apps, links, files or settings based on what is spoken.
- "Play <song>" text is sent only to a music player (the one already playing, or an
  installed Spotify / YouTube Music / JioSaavn / Wynk / Gaana / Amazon Music) by explicit
  package name.
- Notification access is used only to discover media sessions. VIBE never reads, stores or
  forwards notification content (`onNotificationPosted` is not implemented).
- No accounts, servers, analytics, ads or crash reporting. The only network request is the
  optional one-time "Hey VIBE" model download.

## Hardening in place

| Area | Measure |
| --- | --- |
| Exported components | Only the launcher activity and the notification listener. The listener requires `BIND_NOTIFICATION_LISTENER_SERVICE`, so only the system can bind it. The overlay service is `exported="false"`. |
| Intents | All internal intents are explicit. All `PendingIntent`s are `FLAG_IMMUTABLE` and target VIBE's own components. The launcher activity reads no extras. |
| Task hijacking | `taskAffinity=""` on the activity. |
| Tapjacking | Other apps' overlays are hidden over VIBE's screens (`setHideOverlayWindows`, Android 12+). Permission switches live in Android's own Settings, which the system protects. |
| Microphone | Tap-to-talk uses the microphone only for one command. "Hey VIBE" is opt-in, runs on-device with a 5-phrase grammar, and keeps Android's microphone indicator and a notification visible. No audio is stored. Transcripts are never written to disk or logcat; the Diagnostics screen keeps the last 150 events (including heard text) in memory only, wiped when the app process ends or on Clear. Copying it marks the clipboard item sensitive (Android 13+). |
| Network | HTTPS only (`cleartextTrafficPermitted=false`), system CAs only (user-installed CAs are not trusted). |
| Model download | HTTPS, size-capped (80 MB download / 200 MB unpacked), SHA-256 pinned, unzipped with path-traversal ("Zip Slip") checks, stored in no-backup app storage. |
| Backups | `allowBackup=false`, and data-extraction rules exclude everything from cloud backup and device transfer. |
| Release builds | R8 minification and resource shrinking; verbose logging stripped; signing keys come only from GitHub secrets, never the repo. |
| Dependencies | AndroidX, Kotlin coroutines and Vosk/JNA only. |

## Remaining risks and recommendations

1. **Debug APK.** The APK shared during development is a debug build: it is debuggable over
   USB and signed with a throwaway debug key. Before giving VIBE to anyone else, build the
   signed release APK (add the `VIBE_KEYSTORE_BASE64`, `VIBE_KEYSTORE_PASSWORD`,
   `VIBE_KEY_ALIAS`, `VIBE_KEY_PASSWORD` secrets; the `release` CI job then produces it) and
   keep the keystore private and backed up.
2. **Sideloading.** Installing from a file bypasses Play Protect's store review. Only install
   VIBE APKs that come from this repository's CI.
3. **Speech service.** Commands are transcribed by the phone's speech service (usually
   Google), which may process audio online under Google's own policy.
4. **Powerful permissions by design.** Anyone who already controls the phone can use any
   app's permissions. VIBE limits damage by only exposing media actions.
5. **Third-party native code.** Vosk's native library parses the downloaded model. The
   download is pinned by hash, which limits this to the published model.

Report security issues privately to the repository owner rather than in public issues.

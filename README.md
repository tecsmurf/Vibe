# VIBE

**Your music. Your voice. Zero touch.**

VIBE is a floating, voice-controlled assistant for the music apps you already use
(Spotify, YouTube Music and any player that exposes an Android media session).
It is a control layer, not a streaming service.

## Status

Phase 1 of 9: project setup, package structure, dark VIBE theme and launch screen.

## Tech

Kotlin · Jetpack Compose · Material 3 · Gradle Kotlin DSL · minSdk 26 · target/compile SDK 36

## Build

Requirements: JDK 17+ and the Android SDK (platform 36). Point Gradle at the SDK with
`ANDROID_HOME` or a `local.properties` file containing `sdk.dir=/path/to/Android/sdk`.

```bash
./gradlew assembleDebug            # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest        # unit tests
./gradlew installDebug             # install on a connected device (USB debugging on)
# or
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.vibe.musicassistant/.MainActivity
```

Every push also builds the debug APK in GitHub Actions; download it from the
run's **vibe-debug-apk** artifact.

## Package layout

```
com.vibe.musicassistant
├── data/          media, preferences, repository
├── domain/        model, repository, usecase
├── presentation/  onboarding, home, settings, launch, components, theme
├── service/       overlay, media, voice
├── permissions/
└── utils/
```

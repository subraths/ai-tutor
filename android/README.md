# TutorAI — Android app

**Phases 5–6.** Kotlin + Jetpack Compose, MVVM + Clean Architecture. Enter a topic →
the app calls the backend, polls the async job, shows the lesson, and plays it: the
SVG renders in a WebView and each part **glows in sync** with the narration as
ExoPlayer plays the per-segment audio clips.

It also **saves lessons offline**: "Save offline" downloads the SVG + audio to
app-private files (Room metadata), and the History screen replays saved lessons with
no network. Player is source-agnostic — same screen for online and offline lessons.

Verified: `:app:assembleDebug` builds a debug APK (AGP 8.9.1, Gradle 8.13, Kotlin
2.1.0, compileSdk 35, Media3 1.5.1, Room 2.6.1 via KSP).

> Runtime not yet exercised here (no AVD). To actually hear/see playback you need the
> **gemini** backend — mock audio bytes are placeholders and won't decode.

## Architecture (see ../docs/07-android-architecture.md)

```
app/src/main/java/com/tutorai/app/
├── domain/      # Lesson/Segment, GenerationStatus, LessonRepository, GenerateLessonUseCase
├── data/        # Retrofit TutorApi, DTOs, mappers, LessonRepositoryImpl (job polling)
├── di/          # AppContainer (manual DI; clean seam to swap for Hilt later)
└── ui/
    ├── topic/   # TopicScreen + ViewModel (generate, show manifest, save offline)
    ├── player/  # PlayerScreen: WebView SVG + ExoPlayer segment loop + highlight sync
    └── history/ # HistoryScreen: saved lessons, open offline, delete
data/local/      # Room (LessonEntity/SegmentEntity/DAO) + LessonFileStore
```

> **DI note:** this skeleton uses a small manual `AppContainer` instead of Hilt — fewer
> moving parts and no annotation processing, so it builds reliably. Hilt can replace
> the container later without touching the layers.

## Build

```bash
# from android/
./gradlew :app:assembleDebug         # -> app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` points `sdk.dir` at the Android SDK (not committed).

## Run against the backend

1. Start the backend (see ../backend/README.md), e.g. on `:8000`.
2. The app's `BASE_URL` defaults to `http://10.0.2.2:8000/` — the address an
   **emulator** uses to reach the host machine. For a **physical device**, change the
   `BASE_URL` `buildConfigField` in [app/build.gradle.kts](app/build.gradle.kts) to the
   host's LAN IP (e.g. `http://192.168.1.50:8000/`).
3. Build/install/run:
   ```bash
   ./gradlew :app:installDebug      # requires a running emulator or connected device
   adb shell am start -n com.tutorai.app/.MainActivity
   ```

> No AVD exists in this environment yet. Create one in Android Studio (Device Manager)
> or via `avdmanager` (needs `cmdline-tools`), then `emulator -avd <name>`.

Cleartext HTTP to `10.0.2.2`/`localhost` is allowed for dev via
[network_security_config.xml](app/src/main/res/xml/network_security_config.xml).

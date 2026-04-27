# AGENTS.md

## Project Snapshot
- Project: native Android Couch-to-5K app (`C25K Coach`)
- Stack: Kotlin, Jetpack Compose, Room, DataStore, Foreground Service, TTS, GPS
- Map: **OSM via osmdroid** (not MapLibre in this repo)
- Plan: NHS Couch-to-5K seeded from `app/src/main/assets/nhs_c25k_plan.json`
- Languages: English default + German switch for UI and voice cues

## What Is Implemented
- Foreground workout service with lock-screen/pocket-safe interval guidance.
- TTS cues include interval duration (e.g. "Start running for X minutes").
- Pause/resume/stop controls (in app + notification).
- GPS capture per point and per-segment stats (run/walk pace split).
- History + workout detail screen with colored route overlays (run/walk).
- Local-only persistence (Room), no backend.

## Key Files
- App entry: `app/src/main/java/com/example/c25k/MainActivity.kt`
- App wiring: `app/src/main/java/com/example/c25k/AppContainer.kt`
- Service runtime: `app/src/main/java/com/example/c25k/service/WorkoutForegroundService.kt`
- UI screens: `app/src/main/java/com/example/c25k/ui/C25kApp.kt`
- Persistence: `app/src/main/java/com/example/c25k/data/*`
- Localization: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`

## Environment/Build Notes (CLI Only)
- Gradle wrapper exists: `./gradlew`
- Project pins JDK 17 in `gradle.properties`:
  - `org.gradle.java.home=/mnt/c/Entwicklung/playground/codex/c25k/.jdks/jdk-17.0.19+10`
- Android SDK setup script:
  - `./scripts/setup_android_sdk.sh`
- SDK path is written to `local.properties` (machine-specific; do not commit blindly).

## Verified Commands
- Build: `GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleDebug`
- Unit tests: `GRADLE_USER_HOME=$PWD/.gradle ./gradlew testDebugUnitTest`
- Last verified result: both commands succeeded.
- APK output:
  - `app/build/outputs/apk/debug/app-debug.apk`

## Important Constraints / Decisions
- MapLibre was replaced with osmdroid due to repository resolution issues in this environment.
- No cloud sync/export in v1.
- Pace unit currently shown as `min/km`.
- Language switches apply to UI and subsequent TTS cues.

## Recommended Next Steps
- Add instrumentation tests for service background behavior + location mocking.
- Improve route coloring by segment boundaries (currently grouped by type).
- Add workout recovery handling after process death/reboot.

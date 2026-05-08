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
- Warm-up and cooldown are configurable per phase, default to 5 minutes, can be disabled, appear as explicit `WARMUP`/`COOLDOWN` segments in the UI, and are excluded from route tracking and pace calculations.
- Pause/resume/stop controls (in app + notification).
- GPS capture per point and per-segment stats (run/walk pace split).
- History + workout detail screen with colored route overlays (run/walk).
- First screen shows the full 9-week training plan plus a compact summary.
- Plan sessions can be `PENDING`, `COMPLETED`, or `SKIPPED`.
- Skipped sessions remain visible and can be started later; completed sessions can be started again.
- Each plan session shows its latest completion date; repeated runs create additional history entries.
- Local-only persistence (Room), no backend.

## Key Files
- App entry: `app/src/main/java/de/libutzki/c25k/MainActivity.kt`
- App wiring: `app/src/main/java/de/libutzki/c25k/AppContainer.kt`
- Service runtime: `app/src/main/java/de/libutzki/c25k/service/WorkoutForegroundService.kt`
- UI screens: `app/src/main/java/de/libutzki/c25k/ui/C25kApp.kt`
- Persistence: `app/src/main/java/de/libutzki/c25k/data/*`
- Migration guide: `docs/database-migrations.md`
- Exported Room schemas: `app/schemas/de.libutzki.c25k.data.AppDatabase/*`
- Warm-up/cooldown setting: `app/src/main/java/de/libutzki/c25k/settings/WarmupCooldownRepository.kt`
- Localization: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`

## Environment/Build Notes (CLI Only)
- Gradle wrapper exists: `./gradlew`
- Project pins JDK 17 in `gradle.properties`:
  - `org.gradle.java.home=/mnt/c/Entwicklung/playground/codex/c25k/.jdks/jdk-17.0.19+10`
- Android SDK setup script:
  - `./scripts/setup_android_sdk.sh`
- SDK path is written to `local.properties` (machine-specific; do not commit blindly).
- `assembleDevDebug`, `assembleProdRelease`, or test tasks may fail inside the sandbox with `Could not determine a usable wildcard IP for this machine`; rerun outside the sandbox if that happens.

## Verified Commands
- Dev build: `GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleDevDebug`
- Prod release build: `GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleProdRelease`
- Unit tests: `GRADLE_USER_HOME=$PWD/.gradle ./gradlew testProdDebugUnitTest testDevDebugUnitTest`
- Last verified result: all three commands succeeded.
- APK output:
  - Dev debug: `app/build/outputs/apk/dev/debug/app-dev-debug.apk`
  - Prod release: `app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk` unless release signing is configured

## Device Install Workflow
- Preferred install method for manual testing is wireless `adb` to the user's physical device.
- Do not assume an existing APK artifact is current; rebuild with `GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleDevDebug` before installing unless the user explicitly asks to reuse an existing build.
- Prefer installing the freshly built Dev APK with `adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk` to an already paired and connected wireless device when the goal is side-by-side testing with the published app.
- Keep device-specific connection details ephemeral: do not write pairing codes, IP addresses, ports, or other credentials into repository files, including `AGENTS.md`.
- If the wireless device is not currently connected, ask the user for the current Wireless debugging pairing/connect details and use those only for the current session.
- After installing, verify the package is present on-device:
  - Dev app: `com.example.c25k.dev`
  - Published/prod app: `com.example.c25k`

## Verification Rules
- Before `git commit` or `git push`, use judgment instead of running verification by default.
- Run the repository's standard verification when the change could plausibly break the build, tests, packaging, resources, manifests, dependency graph, generated code, or affected call sites.
- Small documentation-only or clearly isolated non-build-affecting changes do not require rerunning the full verification suite before commit.
- For Android code changes in this repo, the minimum required verification is:
  - `GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleDevDebug`
  - `GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleProdRelease`
  - `GRADLE_USER_HOME=$PWD/.gradle ./gradlew testProdDebugUnitTest testDevDebugUnitTest`
- `:app:compileDevDebugKotlin` or `:app:compileProdDebugKotlin` alone is not sufficient to claim a change is verified.
- If Gradle fails in the sandbox with `Could not determine a usable wildcard IP for this machine`, rerun the same verification outside the sandbox before finishing.
- If a change modifies a shared model, constructor, data class, DAO contract, or repository API, update all affected call sites, including tests.
- If a change modifies the Room schema, increment `AppDatabase` version, add an explicit migration, regenerate `app/schemas/`, and verify the upgrade path before release.

## Important Constraints / Decisions
- MapLibre was replaced with osmdroid due to repository resolution issues in this environment.
- No cloud sync/export in v1.
- Pace unit currently shown as `min/km`.
- Language switches apply to UI and subsequent TTS cues.
- Persisted workout and plan data should be treated as durable user data for app upgrades after `0.1.0`.
- Room schema changes must preserve existing local data through explicit migrations; destructive fallback is not acceptable for released upgrades.

## Recommended Next Steps
- Add instrumentation tests for service background behavior + location mocking.
- Improve route coloring by segment boundaries (currently grouped by type).
- Add workout recovery handling after process death/reboot.

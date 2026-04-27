# C25K Coach (Android)

Native Android Couch-to-5K coach app (Kotlin + Compose) with:
- NHS 9-week plan seeding
- Foreground-service interval guidance (TTS + fallback tone)
- English default with German switching (UI + speech cues)
- GPS tracking and pace per run/walk segment
- Workout history and route map (run/walk colored)

## Notes
- Includes Gradle Wrapper (`./gradlew`), no Android Studio required.
- Build requires Android SDK (platform 34 + build-tools 34.0.0).

## CLI setup (no Android Studio)
```bash
cd /mnt/c/Entwicklung/playground/codex/c25k
./scripts/setup_android_sdk.sh
```

If you want a custom SDK path:
```bash
ANDROID_SDK_ROOT=/your/path ./scripts/setup_android_sdk.sh
```

Then build:
```bash
cd /mnt/c/Entwicklung/playground/codex/c25k
GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleDebug
```

## Runtime permissions
- Fine/coarse location
- Notifications (Android 13+)

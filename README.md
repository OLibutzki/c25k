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

## Release workflow
- Official release tags use plain SemVer without a prefix, for example `0.1.0`.
- Releases are published manually from GitHub Actions after the tag already exists.
- The release workflow builds a signed `release` APK from the selected tag and uploads it to a GitHub Release.

### One-time signing setup
Create a release keystore once and keep it outside git:

```bash
keytool -genkeypair \
  -v \
  -keystore c25k-release.keystore \
  -alias c25k-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650
```

For local signed release builds, create `keystore.properties` in the repo root:

```properties
storeFile=/absolute/path/to/c25k-release.keystore
storePassword=your-keystore-password
keyAlias=c25k-release
keyPassword=your-key-password
```

`keystore.properties` is gitignored. You can then build a signed release APK locally with:

```bash
cd /mnt/c/Entwicklung/playground/codex/c25k
GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleRelease -PreleaseVersionName=0.1.0
```

### GitHub Actions secrets
Add these repository secrets before using the manual release workflow:
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Generate the base64 keystore value locally:

```bash
base64 -w 0 c25k-release.keystore
```

### Publish a release from an existing tag
1. Verify the app locally:
   ```bash
   cd /mnt/c/Entwicklung/playground/codex/c25k
   GRADLE_USER_HOME=$PWD/.gradle ./gradlew assembleDebug testDebugUnitTest
   ```
2. Create and push the release tag:
   ```bash
   git tag 0.1.0
   git push origin 0.1.0
   ```
3. In GitHub, open `Actions` -> `Android Release`.
4. Click `Run workflow` and enter the existing tag, for example `0.1.0`.
5. After the workflow succeeds, download `c25k-coach-0.1.0.apk` from the GitHub Release.

### Install or update on your device
If your wireless `adb` device is already connected:

```bash
adb install -r c25k-coach-0.1.0.apk
adb shell pm list packages | grep com.example.c25k
```

## Runtime permissions
- Fine/coarse location
- Notifications (Android 13+)

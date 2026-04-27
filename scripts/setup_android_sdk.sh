#!/usr/bin/env bash
set -euo pipefail

# CLI setup for Android SDK without Android Studio.
# Installs command line tools + required packages for this project.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS_DIR="$ANDROID_SDK_ROOT/cmdline-tools/latest"
ZIP_PATH="/tmp/commandlinetools-linux-latest.zip"

mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"

if [[ ! -d "$CMDLINE_TOOLS_DIR" ]]; then
  echo "Downloading Android command line tools..."
  curl -sL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$ZIP_PATH"
  TMP_DIR="$(mktemp -d)"
  unzip -q "$ZIP_PATH" -d "$TMP_DIR"
  mv "$TMP_DIR/cmdline-tools" "$CMDLINE_TOOLS_DIR"
  rm -rf "$TMP_DIR"
fi

export ANDROID_SDK_ROOT
export PATH="$CMDLINE_TOOLS_DIR/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

(yes | sdkmanager --licenses >/dev/null) || true
sdkmanager \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0"

cat > "$ROOT_DIR/local.properties" <<EOF
sdk.dir=$ANDROID_SDK_ROOT
EOF

echo "Android SDK configured at: $ANDROID_SDK_ROOT"
echo "local.properties written to: $ROOT_DIR/local.properties"

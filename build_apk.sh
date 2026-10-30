#!/usr/bin/env bash
set -euo pipefail
if ! command -v gradle >/dev/null; then
  echo 'Gradle 8.9 not found. Open the project in Android Studio or use GitHub Actions.' >&2
  exit 2
fi
if [[ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]]; then
  echo 'Set ANDROID_HOME or ANDROID_SDK_ROOT to your installed Android SDK.' >&2
  exit 2
fi
gradle :app:assembleDebug --no-daemon
echo 'APK: app/build/outputs/apk/debug/app-debug.apk'

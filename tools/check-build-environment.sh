#!/usr/bin/env bash
set -euo pipefail

missing=0
if ! command -v java >/dev/null 2>&1; then
  echo 'MISSING: Java (JDK 17 or newer required)'
  missing=1
else
  java -version 2>&1 | head -n 1
fi
if ! command -v gradle >/dev/null 2>&1; then
  echo 'MISSING: Gradle (project uses Android Gradle Plugin 8.6.1 (Gradle 8.7+))'
  missing=1
else
  gradle --version | head -n 8
  gradle_version=$(gradle --version | awk '/^Gradle / {print $2; exit}')
  if [[ -n "$gradle_version" ]] && [[ "$(printf '%s\n' 8.7 "$gradle_version" | sort -V | head -n1)" != 8.7 ]]; then
    echo "MISMATCH: Gradle $gradle_version found; Android Gradle Plugin 8.6.1 needs Gradle 8.7+"
    missing=1
  fi
fi
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk" ]]; then
  echo 'MISSING: ANDROID_SDK_ROOT / ANDROID_HOME'
  missing=1
elif [[ ! -f "$sdk/platforms/android-35/android.jar" ]]; then
  echo "MISSING: Android SDK Platform 35 at $sdk/platforms/android-35/android.jar"
  missing=1
else
  echo "FOUND: Android SDK Platform 35 ($sdk)"
fi
if [[ "$missing" -ne 0 ]]; then
  echo 'APK_BUILD_READY=NO'
  exit 2
fi
echo 'APK_BUILD_READY=YES'

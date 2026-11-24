#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
command -v kotlinc >/dev/null 2>&1 || { echo 'MISSING: kotlinc'; exit 2; }
command -v java >/dev/null 2>&1 || { echo 'MISSING: java'; exit 2; }
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
kotlinc \
  "$ROOT/app/src/main/java/com/example/universalremote/IrProfile.kt" \
  "$ROOT/tools/ProfileRegistrySmokeTest.kt" \
  -include-runtime -d "$OUT/profile-tests.jar"
java -jar "$OUT/profile-tests.jar"

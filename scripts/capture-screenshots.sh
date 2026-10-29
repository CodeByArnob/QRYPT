#!/usr/bin/env bash
set -euo pipefail

# QRYPT Screenshot Capture Utility
# Supports:
#   1. Headless deterministic Jetpack Compose / Robolectric Native-Graphics capture (no emulator required)
#   2. Live physical device / emulator capture via ADB if a device is connected

OUT_DIR="docs/screenshots"
mkdir -p "$OUT_DIR"

if command -v adb >/dev/null 2>&1 && [ "$(adb devices | grep -w 'device' | wc -l)" -gt 0 ]; then
  echo "[QRYPT] Connected Android device/emulator detected via ADB."
  echo "[QRYPT] Capturing live framebuffer screenshot to $OUT_DIR/live-device-capture.png..."
  adb exec-out screencap -p > "$OUT_DIR/live-device-capture.png"
  echo "[QRYPT] Saved $OUT_DIR/live-device-capture.png"
else
  echo "[QRYPT] No hardware Android device/emulator attached via ADB."
  echo "[QRYPT] Running deterministic Robolectric Native-Graphics screenshot suite..."
  if command -v gradle >/dev/null 2>&1; then
    gradle :app:testDebugUnitTest --tests "com.example.ExampleRobolectricTest"
  else
    ./gradlew :app:testDebugUnitTest --tests "com.example.ExampleRobolectricTest"
  fi
  echo "[QRYPT] Genuine rendered QRYPT UI screenshots generated in $OUT_DIR/:"
  ls -lh "$OUT_DIR"
fi

#!/usr/bin/env bash
set -euo pipefail
ADB="${ADB:-/home/binarygeek119/Android/Sdk/platform-tools/adb}"
OUT="/tmp/opencode/CoveLounge-latest.apk"
echo "Downloading latest APK..."
gh release download latest --repo binarygeek119/CoveLounge --pattern "*.apk" --output "$OUT" --clobber
echo "Installing to emulator..."
"$ADB" install -r "$OUT"
echo "Launching app..."
"$ADB" shell am start -n com.binarygeek119.covelounge.debug/com.binarygeek119.covelounge.MainActivity
echo "Done."

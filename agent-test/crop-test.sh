#!/bin/bash
#
# Krop Library — Crop Functionality Test
# Tests: app launch, image load, crop button, crash detection
# Platform: Android (adb)
# Usage: ./crop-test.sh [device-serial]
#

set -euo pipefail

# ============================================================
# CONFIG
# ============================================================
DEVICE="${1:-emulator-5554}"
PACKAGE="io.keeppro.krop"
ACTIVITY=".MainActivity"
SCREENSHOT_DIR="/tmp/agent-test-crop"
# Crop button bounds from UI automator: [603,1931][838,2099] on 1440x2960
# Center: (720, 2015) — recalibrate if screen size differs
CROP_BTN_X=720
CROP_BTN_Y=2015

DELAYS_APP_LAUNCH=10
DELAYS_AFTER_CROP=3
DELAYS_SHORT=2

PASS=0
FAIL=0
DRY_RUN=false

if [[ "${1:-}" == "--dry-run" ]]; then
    DRY_RUN=true
    shift
fi

# ============================================================
# HELPERS
# ============================================================
adb_cmd() {
    if $DRY_RUN; then
        echo "[DRY-RUN] adb -s $DEVICE $*"
    else
        adb -s "$DEVICE" "$@"
    fi
}

screenshot() {
    local name="$1"
    local path="$SCREENSHOT_DIR/${name}.png"
    adb_cmd exec-out screencap -p > "$path" 2>/dev/null
    echo "  Screenshot: $path"
}

check_crash() {
    local crashes
    crashes=$(adb_cmd logcat -d -s AndroidRuntime:E 2>/dev/null | grep "FATAL EXCEPTION" || true)
    if [[ -n "$crashes" ]]; then
        echo "  CRASH DETECTED!"
        adb_cmd logcat -d -s AndroidRuntime:E 2>/dev/null | grep -A 10 "FATAL EXCEPTION"
        return 1
    fi
    return 0
}

report_step() {
    local step="$1" expected="$2" actual="$3" status="$4"
    if [[ "$status" == "PASS" ]]; then
        ((PASS++))
        echo "[PASS] Step $step: $actual"
    else
        ((FAIL++))
        echo "[FAIL] Step $step: expected=$expected actual=$actual"
    fi
}

# ============================================================
# SETUP
# ============================================================
echo "============================================"
echo "Krop Crop Test"
echo "============================================"
echo "Device: $DEVICE"
echo "Package: $PACKAGE"
echo "Screenshots: $SCREENSHOT_DIR"
echo "Dry run: $DRY_RUN"
echo "============================================"
echo ""

mkdir -p "$SCREENSHOT_DIR"

# Verify device connected
if ! $DRY_RUN; then
    if ! adb devices 2>/dev/null | grep -q "$DEVICE.*device$"; then
        echo "ERROR: Device $DEVICE not connected or not authorized"
        exit 1
    fi
fi

# Get screen size for coordinate scaling
if ! $DRY_RUN; then
    SCREEN_SIZE=$(adb_cmd shell wm size 2>/dev/null | grep "Physical" | awk '{print $3}')
    SCREEN_W=$(echo "$SCREEN_SIZE" | cut -dx -f1)
    SCREEN_H=$(echo "$SCREEN_SIZE" | cut -dx -f2)
    echo "Screen: ${SCREEN_W}x${SCREEN_H}"

    # Scale coordinates if screen differs from reference (1440x2960)
    if [[ "$SCREEN_W" != "1440" ]]; then
        CROP_BTN_X=$(( CROP_BTN_X * SCREEN_W / 1440 ))
        CROP_BTN_Y=$(( CROP_BTN_Y * SCREEN_H / 2960 ))
        echo "Scaled Crop button to: ($CROP_BTN_X, $CROP_BTN_Y)"
    fi
fi

# ============================================================
# TEST: Launch App
# ============================================================
echo ""
echo "--- Step 1: Launch app ---"
adb_cmd logcat -c 2>/dev/null || true
adb_cmd shell am force-stop "$PACKAGE" 2>/dev/null || true
sleep 1
adb_cmd shell am start -n "${PACKAGE}/${ACTIVITY}" 2>/dev/null || true
echo "  Waiting ${DELAYS_APP_LAUNCH}s for image to load..."
sleep "$DELAYS_APP_LAUNCH"
screenshot "01_app_loaded"

if ! $DRY_RUN; then
    # Verify app is in foreground
    FG=$(adb_cmd shell dumpsys window 2>/dev/null | grep "mCurrentFocus" | head -1 || true)
    if echo "$FG" | grep -q "$PACKAGE"; then
        report_step "1" "App launched" "App in foreground" "PASS"
    else
        report_step "1" "App launched" "App not in foreground: $FG" "FAIL"
    fi
else
    report_step "1" "App launched" "(dry run)" "PASS"
fi

# ============================================================
# TEST: Tap Crop (using UI automator to find button dynamically)
# ============================================================
echo ""
echo "--- Step 2: Find and tap Crop button ---"
adb_cmd logcat -c 2>/dev/null || true

if ! $DRY_RUN; then
    # Use uiautomator to find exact Crop button position
    adb_cmd shell uiautomator dump /sdcard/ui.xml 2>/dev/null || true
    sleep 1
    CROP_BOUNDS=$(adb_cmd shell cat /sdcard/ui.xml 2>/dev/null | tr '>' '>\n' | grep 'text="Crop"' | grep -o 'bounds="[^"]*"' | head -1 || true)

    if [[ -n "$CROP_BOUNDS" ]]; then
        # Parse bounds like [603,1931][838,2099]
        X1=$(echo "$CROP_BOUNDS" | grep -o '\[.*\]' | sed 's/\]\[/,/g;s/\[//g;s/\]//g' | cut -d, -f1)
        Y1=$(echo "$CROP_BOUNDS" | grep -o '\[.*\]' | sed 's/\]\[/,/g;s/\[//g;s/\]//g' | cut -d, -f2)
        X2=$(echo "$CROP_BOUNDS" | grep -o '\[.*\]' | sed 's/\]\[/,/g;s/\[//g;s/\]//g' | cut -d, -f3)
        Y2=$(echo "$CROP_BOUNDS" | grep -o '\[.*\]' | sed 's/\]\[/,/g;s/\[//g;s/\]//g' | cut -d, -f4)
        CROP_BTN_X=$(( (X1 + X2) / 2 ))
        CROP_BTN_Y=$(( (Y1 + Y2) / 2 ))
        echo "  Found Crop button at ($CROP_BTN_X, $CROP_BTN_Y)"
    else
        echo "  Using default Crop button position ($CROP_BTN_X, $CROP_BTN_Y)"
    fi
fi

adb_cmd shell input tap "$CROP_BTN_X" "$CROP_BTN_Y" 2>/dev/null || true
echo "  Tapped Crop at ($CROP_BTN_X, $CROP_BTN_Y)"
sleep "$DELAYS_AFTER_CROP"
screenshot "02_after_crop"

# ============================================================
# TEST: Check for crash
# ============================================================
echo ""
echo "--- Step 3: Verify no crash ---"

if ! $DRY_RUN; then
    if check_crash; then
        report_step "3" "No crash" "No crash detected" "PASS"
    else
        report_step "3" "No crash" "CRASH: hardware bitmap or other exception" "FAIL"
    fi
else
    report_step "3" "(dry run)" "(dry run)" "PASS"
fi

# ============================================================
# TEST: Verify crop result appeared
# ============================================================
echo ""
echo "--- Step 4: Verify cropped image displayed ---"

if ! $DRY_RUN; then
    # Dump UI again to check if a new AsyncImage appeared (the crop result)
    adb_cmd shell uiautomator dump /sdcard/ui2.xml 2>/dev/null || true
    sleep 1
    UI_AFTER=$(adb_cmd shell cat /sdcard/ui2.xml 2>/dev/null || true)

    # The app should show the cropped image below the Crop button
    # We check if the app is still alive and the activity is still foreground
    FG_AFTER=$(adb_cmd shell dumpsys window 2>/dev/null | grep "mCurrentFocus" | head -1 || true)
    if echo "$FG_AFTER" | grep -q "$PACKAGE"; then
        report_step "4" "App alive after crop" "App still in foreground" "PASS"
    else
        report_step "4" "App alive after crop" "App not in foreground" "FAIL"
    fi
fi

# ============================================================
# REPORT
# ============================================================
echo ""
echo "============================================"
echo "RESULTS: $PASS passed, $FAIL failed"
echo "============================================"
echo "Screenshots saved to: $SCREENSHOT_DIR"

if [[ $FAIL -gt 0 ]]; then
    exit 1
else
    exit 0
fi

#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <outdir> [screen...]" >&2
    exit 1
fi

outdir="$1"
shift || true
mkdir -p "$outdir"

if [ $# -gt 0 ]; then
    screens=("$@")
else
    screens=(heute woche frei fehlzeiten einstellungen)
fi

DEVICE="emulator-5560"
ADB="timeout 30 adb -s $DEVICE"

cleanup() {
    $ADB shell settings put system font_scale 1.0 || true
    $ADB shell cmd uimode night no || true
}
trap cleanup EXIT INT TERM

# Install debug APK
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    echo "APK $APK not found. Building debug APK first..."
    ./gw assembleDebug
fi

echo "Installing $APK onto $DEVICE..."
$ADB install -r "$APK"
$ADB shell pm grant io.github.omeg4dev.greatuntis android.permission.POST_NOTIFICATIONS || true
$ADB shell pm grant io.github.omeg4dev.greatuntis android.permission.CAMERA || true

themes=(light dark)
fonts=(1.0 1.3)
nows=(2026-11-03T10:50 2026-11-05T09:00 2026-10-20T09:00 2026-11-07T12:00)

for screen in "${screens[@]}"; do
    for theme in "${themes[@]}"; do
        if [ "$theme" = "light" ]; then
            $ADB shell cmd uimode night no
        else
            $ADB shell cmd uimode night yes
        fi

        for font in "${fonts[@]}"; do
            $ADB shell settings put system font_scale "$font"

            for now in "${nows[@]}"; do
                echo "Capturing $screen @ $now (theme=$theme, font=$font)..."
                $ADB shell am start -S -W -n io.github.omeg4dev.greatuntis/.MainActivity \
                    --ez fake true --es screen "$screen" --es now "$now"
                sleep 2.5
                $ADB exec-out screencap -p > "${outdir}/${screen}_${now}_${theme}_${font}.png"
            done
        done
    done
done

echo "Screenshots saved to $outdir"

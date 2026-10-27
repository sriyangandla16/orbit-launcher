#!/usr/bin/env bash
# Orbit Launcher installer for Fire TV (Mac / Linux)
# Usage:  ./install.sh 192.168.1.50
# Put OrbitLauncher.apk in the same folder as this file.
set -e

if [ -z "$1" ]; then
  echo "Usage: ./install.sh YOUR_FIRE_TV_IP"
  echo "Find the IP on the TV: Settings > Device (or My Fire TV) > About > Network"
  exit 1
fi
TV="$1:5555"
cd "$(dirname "$0")"

echo "Connecting to $TV ... accept the 'Allow USB debugging?' popup on the TV if it appears."
adb connect "$TV"
adb -s "$TV" wait-for-device

echo "Installing Orbit Launcher..."
adb -s "$TV" install -r OrbitLauncher.apk

echo "Turning on the Home button service..."
adb -s "$TV" shell settings put secure enabled_accessibility_services com.orbit.launcher/com.orbit.launcher.HomeWatcherService
adb -s "$TV" shell settings put secure accessibility_enabled 1

echo "Opening Orbit..."
adb -s "$TV" shell am start -n com.orbit.launcher/.MainActivity
echo "Done! Press Home on the remote to test it."

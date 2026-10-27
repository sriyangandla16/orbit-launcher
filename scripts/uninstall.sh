#!/usr/bin/env bash
# Removes Orbit Launcher and gives the Home button back to Amazon.
# Usage:  ./uninstall.sh 192.168.1.50
if [ -z "$1" ]; then echo "Usage: ./uninstall.sh YOUR_FIRE_TV_IP"; exit 1; fi
TV="$1:5555"
adb connect "$TV"
adb -s "$TV" shell settings delete secure enabled_accessibility_services
adb -s "$TV" uninstall com.orbit.launcher
echo "Orbit removed. Your Fire TV is back to normal."

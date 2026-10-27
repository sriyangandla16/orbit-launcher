#!/usr/bin/env bash
# Shows what Orbit's Home button service sees. Press Home on the remote while this runs.
# Usage:  ./debug-log.sh 192.168.1.50
if [ -z "$1" ]; then echo "Usage: ./debug-log.sh YOUR_FIRE_TV_IP"; exit 1; fi
TV="$1:5555"
adb connect "$TV"
adb -s "$TV" logcat -c
adb -s "$TV" logcat -s OrbitHome

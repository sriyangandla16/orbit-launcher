@echo off
REM Shows what Orbit's Home button service sees. Press Home on the remote while this runs.
REM Usage:  debug-log.bat 192.168.1.50
if "%~1"=="" ( echo Usage: debug-log.bat YOUR_FIRE_TV_IP & exit /b 1 )
set TV=%~1:5555
adb connect %TV%
adb -s %TV% logcat -c
adb -s %TV% logcat -s OrbitHome

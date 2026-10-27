@echo off
REM Removes Orbit Launcher and gives the Home button back to Amazon.
REM Usage:  uninstall.bat 192.168.1.50
if "%~1"=="" ( echo Usage: uninstall.bat YOUR_FIRE_TV_IP & exit /b 1 )
set TV=%~1:5555
adb connect %TV%
adb -s %TV% shell settings delete secure enabled_accessibility_services
adb -s %TV% uninstall com.orbit.launcher
echo Orbit removed. Your Fire TV is back to normal.

@echo off
cd /d "%~dp0"
echo Starting PDI Print Helper...
:: Start python in background without showing command prompt window (uses pythonw)
start pythonw print_helper.py
echo Print Helper started in background.
timeout /t 3

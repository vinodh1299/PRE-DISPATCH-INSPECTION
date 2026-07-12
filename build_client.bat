@echo off
title PDI Desktop App Compiler
echo =======================================================
echo   PDI Windows Desktop App Compiler
echo =======================================================
echo.

:: Detect the 64-bit .NET v4.0 compiler
set CSC_PATH=C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe

:: Fall back to 32-bit if 64-bit doesn't exist
if not exist "%CSC_PATH%" (
    set CSC_PATH=C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe
)

if not exist "%CSC_PATH%" (
    echo [ERROR] C# Compiler csc.exe was not found on this system.
    echo Make sure .NET Framework 4.0 or higher is installed.
    pause
    exit /b 1
)

echo [INFO] Found C# compiler at: %CSC_PATH%
echo [INFO] Compiling PdiApp.cs...

:: Compile as a Windows Forms GUI Application (/target:winexe)
:: and include System.Web to parse query string easily
"%CSC_PATH%" /target:winexe /out:PdiApp.exe /reference:System.Web.dll PdiApp.cs

if %errorlevel% equ 0 (
    echo.
    echo =======================================================
    echo   [SUCCESS] Compilation complete! 
    echo   Generated executable: PdiApp.exe
    echo =======================================================
) else (
    echo.
    echo =======================================================
    echo   [ERROR] Compilation failed! Check messages above.
    echo =======================================================
)

pause

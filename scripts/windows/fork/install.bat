@echo off
REM SimpMusic installer for Windows. Double-click this file and click Yes when Windows asks for admin.
REM Keep it in the same folder as install.ps1, simpmusic.crt and the simpmusic-*.msix.
setlocal
cd /d "%~dp0"

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Asking for admin rights...
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo.
echo === SimpMusic installer ===
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1"
echo.
pause
endlocal

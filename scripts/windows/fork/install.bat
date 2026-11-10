@echo off
REM SimpMusic installer for Windows. Double-click this file and click Yes when Windows asks for admin.
REM Keep it in the same folder as install.ps1, simpmusic.crt and the simpmusic-*.msix.
setlocal
cd /d "%~dp0"

if not exist "%~dp0install.ps1" (
    echo.
    echo [ERROR] install.ps1 is not next to install.bat.
    echo Opening the zip and double-clicking install.bat inside it does not work: Windows
    echo unpacks only that one file. Right-click the zip, choose "Extract All", then open
    echo install.bat from the extracted folder.
    echo.
    pause
    exit /b 1
)

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

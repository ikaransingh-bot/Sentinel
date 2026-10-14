@echo off
setlocal EnableExtensions

REM ============================================================
REM PROJECT SENTINEL
REM Main Launcher
REM ============================================================

REM Ga naar de project-root.
pushd "%~dp0.."

if errorlevel 1 (
    echo.
    echo Project SENTINEL kon niet worden gestart.
    echo.
    exit /b 1
)

REM Controleer marker.
if not exist ".sentinel-root" (
    echo.
    echo Project SENTINEL kon niet worden gevonden.
    echo.
    popd
    exit /b 1
)

REM ============================================================
REM Bootstrap uitvoeren
REM ============================================================

powershell.exe ^
    -NoLogo ^
    -NoProfile ^
    -ExecutionPolicy Bypass ^
    -File "%~dp0bootstrap.ps1" %*

set "RESULT=%ERRORLEVEL%"

popd

endlocal & exit /b %RESULT%
@echo off
setlocal

echo ==========================================
echo       PROJECT SENTINEL INSTALLER
echo ==========================================
echo.

REM De map waarin install.cmd zelf staat
set "PROJECT_ROOT=%~dp0"

REM Controleer marker
if not exist "%PROJECT_ROOT%.sentinel-root" (
    echo FOUT: .sentinel-root werd niet gevonden.
    echo.
    pause
    exit /b 1
)

REM Controleer installer
if not exist "%PROJECT_ROOT%scripts\install-command.cmd" (
    echo FOUT: scripts\install-command.cmd werd niet gevonden.
    echo.
    pause
    exit /b 1
)

echo Project gevonden:
echo %PROJECT_ROOT%
echo.

REM Start de echte installer
call "%PROJECT_ROOT%scripts\install-command.cmd"

set "RESULT=%ERRORLEVEL%"

endlocal & exit /b %RESULT%
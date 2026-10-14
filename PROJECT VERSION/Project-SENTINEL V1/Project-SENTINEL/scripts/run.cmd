@echo off
setlocal EnableExtensions

REM ============================================================
REM PROJECT SENTINEL GLOBAL COMMAND
REM Gebruik: run sentinel
REM ============================================================

if /I not "%~1"=="sentinel" (
    echo.
    echo Ongeldig commando.
    echo.
    echo Gebruik:
    echo.
    echo     run sentinel
    echo.
    exit /b 1
)

REM ============================================================
REM Controleer opgeslagen SENTINEL locatie
REM ============================================================

if not defined SENTINEL_ROOT (
    echo.
    echo FOUT: SENTINEL_ROOT is niet ingesteld.
    echo.
    echo Voer eerst install-command.cmd uit.
    echo.
    exit /b 1
)

if not exist "%SENTINEL_ROOT%\.sentinel-root" (
    echo.
    echo FOUT: Project SENTINEL werd niet gevonden op:
    echo.
    echo %SENTINEL_ROOT%
    echo.
    echo Voer install-command.cmd opnieuw uit.
    echo.
    exit /b 1
)

if not exist "%SENTINEL_ROOT%\scripts\run.bat" (
    echo.
    echo FOUT: scripts\run.bat kon niet worden gevonden.
    echo.
    exit /b 1
)

REM ============================================================
REM Start portable SENTINEL run.bat
REM ============================================================

call "%SENTINEL_ROOT%\scripts\run.bat"

set "RESULT=%ERRORLEVEL%"

endlocal & exit /b %RESULT%
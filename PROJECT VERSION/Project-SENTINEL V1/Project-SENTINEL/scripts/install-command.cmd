@echo off
setlocal EnableExtensions

REM ============================================================
REM PROJECT SENTINEL COMMAND INSTALLER
REM ============================================================

echo ==========================================
echo       PROJECT SENTINEL COMMAND SETUP
echo ==========================================
echo.

REM ============================================================
REM Bepaal automatisch project-root
REM install-command.cmd staat in scripts,
REM dus één map omhoog is de project-root.
REM ============================================================

for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"

echo Project gevonden:
echo %PROJECT_ROOT%
echo.

REM ============================================================
REM Controleer of dit echt SENTINEL is
REM ============================================================

if not exist "%PROJECT_ROOT%\.sentinel-root" (
    echo FOUT: .sentinel-root werd niet gevonden.
    echo.
    echo Verwachte locatie:
    echo %PROJECT_ROOT%\.sentinel-root
    echo.
    pause
    exit /b 1
)

if not exist "%PROJECT_ROOT%\scripts\run.cmd" (
    echo FOUT: scripts\run.cmd ontbreekt.
    echo.
    pause
    exit /b 1
)

if not exist "%PROJECT_ROOT%\scripts\run.bat" (
    echo FOUT: scripts\run.bat ontbreekt.
    echo.
    pause
    exit /b 1
)

REM ============================================================
REM Maak globale command-map
REM ============================================================

set "INSTALL_DIR=%LOCALAPPDATA%\ProjectSentinel\bin"

if not exist "%INSTALL_DIR%" (
    mkdir "%INSTALL_DIR%"
)

if errorlevel 1 (
    echo FOUT: Installatiemap kon niet worden gemaakt.
    echo.
    pause
    exit /b 1
)

REM ============================================================
REM Kopieer run.cmd naar globale command-map
REM ============================================================

copy /Y "%PROJECT_ROOT%\scripts\run.cmd" "%INSTALL_DIR%\run.cmd" >nul

if errorlevel 1 (
    echo FOUT: run.cmd kon niet worden gekopieerd.
    echo.
    pause
    exit /b 1
)

echo run.cmd geinstalleerd.
echo.

REM ============================================================
REM Geef waarden door aan PowerShell
REM ============================================================

set "SENTINEL_INSTALL_DIR=%INSTALL_DIR%"
set "SENTINEL_PROJECT_ROOT=%PROJECT_ROOT%"

REM ============================================================
REM User environment variables aanpassen
REM Geen administrator nodig.
REM ============================================================

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
"$install = $env:SENTINEL_INSTALL_DIR; ^
$root = $env:SENTINEL_PROJECT_ROOT; ^
[Environment]::SetEnvironmentVariable('SENTINEL_ROOT', $root, 'User'); ^
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User'); ^
if ([string]::IsNullOrWhiteSpace($userPath)) { ^
    $newPath = $install ^
} else { ^
    $parts = $userPath -split ';'; ^
    if ($parts -contains $install) { ^
        $newPath = $userPath ^
    } else { ^
        $newPath = $userPath.TrimEnd(';') + ';' + $install ^
    } ^
}; ^
[Environment]::SetEnvironmentVariable('Path', $newPath, 'User')"

if errorlevel 1 (
    echo.
    echo FOUT: Windows environment kon niet worden aangepast.
    echo.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo        INSTALLATIE GELUKT
echo ==========================================
echo.
echo SENTINEL_ROOT:
echo %PROJECT_ROOT%
echo.
echo Globaal commando:
echo %INSTALL_DIR%\run.cmd
echo.
echo BELANGRIJK:
echo.
echo Sluit nu ALLE CMD vensters.
echo Open daarna een NIEUWE CMD.
echo.
echo Daarna kun je vanuit iedere map typen:
echo.
echo     run sentinel
echo.
pause

endlocal
exit /b 0
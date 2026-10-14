@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================
REM PROJECT SENTINEL
REM Java Build Script
REM ============================================================

REM build.bat staat in scripts\
REM Project-root is daarom een map hoger.
pushd "%~dp0.."

if errorlevel 1 (
    echo FOUT: Projectmap kon niet worden geopend.
    exit /b 1
)

REM ============================================================
REM Controleer projectstructuur
REM ============================================================

if not exist ".sentinel-root" (
    echo FOUT: .sentinel-root ontbreekt.
    popd
    exit /b 1
)

if not exist "src" (
    echo FOUT: src map ontbreekt.
    popd
    exit /b 1
)

if not exist "lib" (
    echo FOUT: lib map ontbreekt.
    popd
    exit /b 1
)

REM ============================================================
REM Build-map buiten de projectmap
REM Zo bevat de projectmap alleen .java bestanden.
REM ============================================================

if not defined SENTINEL_BUILD_DIR (
    set "SENTINEL_BUILD_DIR=%LOCALAPPDATA%\ProjectSentinel\build"
)

if not exist "!SENTINEL_BUILD_DIR!" (
    mkdir "!SENTINEL_BUILD_DIR!"
)

REM ============================================================
REM Kies javac
REM ============================================================

set "JAVAC="

REM Eerst JAVA_HOME gebruiken.
REM bootstrap.ps1 kan hier onze portable JDK instellen.

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    )
)

REM Geen JAVA_HOME?
REM Dan normale javac uit PATH proberen.

if not defined JAVAC (
    for /f "delims=" %%J in ('where javac.exe 2^>nul') do (
        if not defined JAVAC (
            set "JAVAC=%%J"
        )
    )
)

if not defined JAVAC (
    echo FOUT: javac werd niet gevonden.
    popd
    exit /b 1
)

REM ============================================================
REM Zoek alle Java bronbestanden
REM javac zoekt niet zelf in submappen; daarom zetten we alle
REM .java bestanden uit alle packages in een lijst.
REM ============================================================

set "SOURCE_LIST=%TEMP%\sentinel_sources_%RANDOM%_%RANDOM%.txt"

if exist "%SOURCE_LIST%" (
    del "%SOURCE_LIST%" >nul 2>&1
)

for /R "src" %%F in (*.java) do (

    set "JAVA_FILE=%%~fF"

    REM Forward slashes voor javac argument-file.
    set "JAVA_FILE=!JAVA_FILE:\=/!"

    echo "!JAVA_FILE!" >> "%SOURCE_LIST%"
)

REM ============================================================
REM Controleer source list
REM ============================================================

if not exist "%SOURCE_LIST%" (
    echo FOUT: Geen Java bestanden gevonden.
    popd
    exit /b 1
)

for %%A in ("%SOURCE_LIST%") do (
    if %%~zA==0 (

        del "%SOURCE_LIST%" >nul 2>&1

        echo FOUT: Geen Java bestanden gevonden in src.
        popd
        exit /b 1
    )
)

REM ============================================================
REM Oude compiled classes verwijderen
REM ============================================================

if exist "%SENTINEL_BUILD_DIR%\sentinel" (
    rmdir /S /Q "%SENTINEL_BUILD_DIR%\sentinel"
)

REM ============================================================
REM Java compileren
REM -encoding UTF-8 : bronbestanden bevatten Nederlandse tekst
REM -cp "lib/*"     : MySQL Connector/J (.jar) nodig voor JDBC
REM -d              : map waar javac de .class bestanden neerzet
REM @bestand        : lijst met alle .java bestanden
REM ============================================================

"%JAVAC%" ^
    -encoding UTF-8 ^
    -cp "lib/*" ^
    -d "%SENTINEL_BUILD_DIR%" ^
    @"%SOURCE_LIST%"

set "BUILD_RESULT=%ERRORLEVEL%"

del "%SOURCE_LIST%" >nul 2>&1

if not "%BUILD_RESULT%"=="0" (
    popd
    exit /b %BUILD_RESULT%
)

popd

endlocal & exit /b 0

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$JavaManager = Join-Path $PSScriptRoot "java-manager.ps1"
$MySqlManager = Join-Path $PSScriptRoot "mysql-manager.ps1"
$BuildScript = Join-Path $PSScriptRoot "build.bat"
$LogFile = Join-Path $env:TEMP "sentinel-startup.log"

# javac zet de .class bestanden hier, buiten de projectmap.
$BuildDirectory = Join-Path $env:LOCALAPPDATA "ProjectSentinel\build"

Remove-Item $LogFile -Force -ErrorAction SilentlyContinue
Clear-Host

Write-Host "===================================="
Write-Host "          PROJECT SENTINEL"
Write-Host "===================================="
Write-Host ""
Write-Host "SENTINEL wordt gestart."
Write-Host "Een ogenblik geduld a.u.b..."
Write-Host ""

try {
    if (-not (Test-Path -LiteralPath $JavaManager)) { throw "java-manager.ps1 ontbreekt." }
    if (-not (Test-Path -LiteralPath $MySqlManager)) { throw "mysql-manager.ps1 ontbreekt." }
    if (-not (Test-Path -LiteralPath $BuildScript)) { throw "build.bat ontbreekt." }

    . $JavaManager
    . $MySqlManager

    # Oudere Windows versies gebruiken anders geen TLS 1.2 voor downloads.
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

    $javaInfo = Ensure-SentinelJava -MinimumVersion 21
    if ($null -eq $javaInfo) { throw "Java kon niet worden voorbereid." }
    Write-Host "Java $($javaInfo.Version) gevonden."

    $env:JAVA_HOME = $javaInfo.JavaHome
    $env:PATH = (Join-Path $javaInfo.JavaHome "bin") + ";" + $env:PATH
    $env:SENTINEL_ROOT = $ProjectRoot

    $databaseInfo = Ensure-SentinelMySql -ProjectRoot $ProjectRoot
    if ($null -eq $databaseInfo) { throw "De database kon niet worden voorbereid." }

    $env:SENTINEL_DB_HOST = $databaseInfo.Host
    $env:SENTINEL_DB_PORT = "$($databaseInfo.Port)"
    $env:SENTINEL_DB_NAME = $databaseInfo.Database
    $env:SENTINEL_DB_USER = $databaseInfo.User

    # 1. Compileren: build.bat roept javac aan.
    $env:SENTINEL_BUILD_DIR = $BuildDirectory

    "BUILD START" | Out-File -FilePath $LogFile -Append

    # Meldingen van javac als tekst opvangen in plaats van het script te stoppen.
    $ErrorActionPreference = "Continue"
    $buildOutput = & $BuildScript 2>&1
    $buildExitCode = $LASTEXITCODE
    $ErrorActionPreference = "Stop"

    $buildOutput | Out-File -FilePath $LogFile -Append

    if ($buildExitCode -ne 0) {
        throw "De Java-code kon niet worden gecompileerd. Controleer het startup-logbestand."
    }

    # 2. Runnen: java met de classpath (gecompileerde classes + MySQL driver) en de main class.
    $libDirectory = Join-Path $ProjectRoot "lib"
    $classPath = $BuildDirectory + ";" + (Join-Path $libDirectory "*")

    Clear-Host
    Push-Location $ProjectRoot

    try {
        & $javaInfo.JavaExe "-cp" $classPath "sentinel.app.Main"
        $applicationExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    exit $applicationExitCode
}
catch {
    $_ | Out-String | Out-File -FilePath $LogFile -Append

    Write-Host ""
    Write-Host "===================================="
    Write-Host "      SENTINEL KON NIET STARTEN"
    Write-Host "===================================="
    Write-Host ""
    Write-Host $_.Exception.Message
    Write-Host ""
    Write-Host "Technische informatie is opgeslagen in:"
    Write-Host $LogFile
    Write-Host ""

    exit 1
}

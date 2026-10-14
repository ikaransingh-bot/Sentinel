Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ============================================================
# SENTINEL draait een eigen MySQL server met een eigen datamap.
# - mysqld.exe komt van een MySQL die al op de pc staat,
#   of (als die er niet is) van een portable download.
# - root heeft in deze eigen server geen wachtwoord, zodat SENTINEL
#   altijd de gebruiker uit database.properties kan aanmaken.
# - De databases van een eigen MySQL installatie worden niet aangeraakt.
# ============================================================

$SentinelMySqlRoot = Join-Path $env:LOCALAPPDATA "ProjectSentinel\runtime\mysql"
$SentinelMySqlIni = Join-Path $SentinelMySqlRoot "sentinel-my.ini"
$SentinelMySqlData = Join-Path $SentinelMySqlRoot "data"
$SentinelSqlHashFile = Join-Path $SentinelMySqlRoot "sentinel-sql.sha256"
$SentinelBackupDir = Join-Path $env:LOCALAPPDATA "ProjectSentinel\backups"

$MySqlDownloadUrls = @(
    "https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.11-winx64.zip",
    "https://downloads.mysql.com/archives/get/p/23/file/mysql-8.4.11-winx64.zip"
)

# ============================================================
# database.properties
# ============================================================

function Read-DbProperties {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "database.properties ontbreekt."
    }

    $props = @{}

    foreach ($line in Get-Content -LiteralPath $Path) {
        $text = $line.Trim()

        if ($text -eq "" -or $text.StartsWith("#")) {
            continue
        }

        $index = $text.IndexOf("=")

        if ($index -gt 0) {
            $key = $text.Substring(0, $index).Trim()
            $value = $text.Substring($index + 1).Trim()
            $props[$key] = $value
        }
    }

    return $props
}

function Set-DbProperty {
    param(
        [string]$Path,
        [string]$Key,
        [string]$Value
    )

    $lines = Get-Content -LiteralPath $Path
    $found = $false
    $result = @()

    foreach ($line in $lines) {
        if ($line -match ("^" + [regex]::Escape($Key) + "=")) {
            $result += "$Key=$Value"
            $found = $true
        }
        else {
            $result += $line
        }
    }

    if (-not $found) {
        $result += "$Key=$Value"
    }

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($Path, $result, $encoding)
}

# ============================================================
# Poorten en processen
# ============================================================

function Test-MySqlPort {
    param([int]$Port)

    $client = New-Object System.Net.Sockets.TcpClient

    try {
        $task = $client.ConnectAsync("127.0.0.1", $Port)
        $connected = $task.Wait(800)
        return ($connected -and $client.Connected)
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Find-FreePort {
    param([int]$StartPort)

    for ($port = $StartPort; $port -lt ($StartPort + 100); $port++) {
        if (-not (Test-MySqlPort -Port $port)) {
            return $port
        }
    }

    throw "Geen vrije MySQL poort gevonden."
}

function Get-PortListenerProcessId {
    param([int]$Port)

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1

    if ($null -eq $connection) {
        return $null
    }

    return [int]$connection.OwningProcess
}

# Alle mysqld processen die met de SENTINEL configuratie zijn gestart.
function Get-SentinelMySqlProcessIds {
    $processes = Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'" -ErrorAction SilentlyContinue

    return @($processes | Where-Object {
        $_.CommandLine -and $_.CommandLine.IndexOf($SentinelMySqlIni, [StringComparison]::OrdinalIgnoreCase) -ge 0
    } | ForEach-Object { [int]$_.ProcessId })
}

# ============================================================
# Programma's uitvoeren
# ============================================================

# Start een programma, wacht tot het klaar is en vangt de foutmeldingen op.
# Invoer en uitvoer kunnen direct van/naar een bestand gaan.
function Invoke-Tool {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$InputFile = "",
        [string]$OutputFile = "",
        [string]$Password = ""
    )

    $errorFile = Join-Path $env:TEMP "sentinel-tool-$PID.err"
    $outputTarget = $OutputFile

    if ($outputTarget -eq "") {
        $outputTarget = Join-Path $env:TEMP "sentinel-tool-$PID.out"
    }

    $parameters = @{
        FilePath = $FilePath
        ArgumentList = $Arguments
        NoNewWindow = $true
        Wait = $true
        PassThru = $true
        RedirectStandardOutput = $outputTarget
        RedirectStandardError = $errorFile
    }

    if ($InputFile -ne "") {
        $parameters["RedirectStandardInput"] = $InputFile
    }

    $oldPassword = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")

    try {
        [Environment]::SetEnvironmentVariable("MYSQL_PWD", $Password, "Process")
        $process = Start-Process @parameters
        $errorText = Get-Content -LiteralPath $errorFile -Raw -ErrorAction SilentlyContinue

        return [PSCustomObject]@{
            ExitCode = $process.ExitCode
            Error = "$errorText".Trim()
        }
    }
    finally {
        [Environment]::SetEnvironmentVariable("MYSQL_PWD", $oldPassword, "Process")
        Remove-Item $errorFile -Force -ErrorAction SilentlyContinue

        if ($OutputFile -eq "") {
            Remove-Item $outputTarget -Force -ErrorAction SilentlyContinue
        }
    }
}

function Invoke-MySql {
    param(
        [string]$Client,
        [int]$Port,
        [string]$User,
        [string]$Password,
        [string]$Database = "",
        [string]$Sql
    )

    # Foutmeldingen van mysql.exe als tekst teruggeven in plaats van te stoppen.
    $ErrorActionPreference = "Continue"
    $oldPassword = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")

    try {
        [Environment]::SetEnvironmentVariable("MYSQL_PWD", $Password, "Process")

        $arguments = @("--protocol=tcp", "-h", "127.0.0.1", "-P", "$Port", "-u", $User, "-N", "-s")

        if ($Database -ne "") {
            $arguments += @("-D", $Database)
        }

        $arguments += @("-e", $Sql)
        $output = & $Client @arguments 2>&1

        return [PSCustomObject]@{
            Success = ($LASTEXITCODE -eq 0)
            Output = ($output | Out-String).Trim()
        }
    }
    finally {
        [Environment]::SetEnvironmentVariable("MYSQL_PWD", $oldPassword, "Process")
    }
}

# ============================================================
# MySQL zoeken of installeren
# ============================================================

function Get-MySqlServerVersion {
    param([string]$Server)

    try {
        $text = & $Server --version | Out-String
    }
    catch {
        return $null
    }

    # MariaDB (bijvoorbeeld XAMPP) werkt anders en wordt overgeslagen.
    if ($text -match "MariaDB") {
        return $null
    }

    if ($text -match "Ver\s+(\d+)\.(\d+)\.(\d+)") {
        return [version]"$($Matches[1]).$($Matches[2]).$($Matches[3])"
    }

    return $null
}

# Scoop zet in shims\ een doorverwijzing; het echte pad staat in het .shim bestand.
function Resolve-ScoopShim {
    param([string]$Path)

    $shim = [System.IO.Path]::ChangeExtension($Path, ".shim")

    if (Test-Path -LiteralPath $shim) {
        foreach ($line in Get-Content -LiteralPath $shim) {
            if ($line -match '^\s*path\s*=\s*"?([^"]+?)"?\s*$') {
                return $Matches[1]
            }
        }
    }

    return $Path
}

function Find-InstalledMySqlServer {
    $candidates = @()

    $command = Get-Command "mysqld.exe" -ErrorAction SilentlyContinue

    if ($null -ne $command) {
        $candidates += Resolve-ScoopShim -Path $command.Source
    }

    $patterns = @(
        "$env:USERPROFILE\scoop\apps\*\current\bin\mysqld.exe",
        "$env:ProgramFiles\MySQL\MySQL Server *\bin\mysqld.exe"
    )

    foreach ($pattern in $patterns) {
        $candidates += @(Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
    }

    foreach ($candidate in $candidates) {
        if (-not $candidate -or -not (Test-Path -LiteralPath $candidate)) {
            continue
        }

        $version = Get-MySqlServerVersion -Server $candidate

        if ($null -ne $version -and $version.Major -ge 8) {
            return $candidate
        }
    }

    return $null
}

function Get-ConfiguredBaseDir {
    if (-not (Test-Path -LiteralPath $SentinelMySqlIni)) {
        return $null
    }

    foreach ($line in Get-Content -LiteralPath $SentinelMySqlIni) {
        if ($line -match '^\s*basedir\s*=\s*(.+?)\s*$') {
            return $Matches[1].Replace("/", "\")
        }
    }

    return $null
}

function Find-SentinelMySqlServer {

    # 1. De MySQL waarmee de SENTINEL datamap al eerder is gebruikt.
    $baseDir = Get-ConfiguredBaseDir

    if ($null -ne $baseDir) {
        $server = Join-Path $baseDir "bin\mysqld.exe"

        if (Test-Path -LiteralPath $server) {
            return $server
        }
    }

    # 2. Een eerder gedownloade portable MySQL.
    $portable = Join-Path $SentinelMySqlRoot "bin\mysqld.exe"

    if (Test-Path -LiteralPath $portable) {
        return $portable
    }

    # 3. Een MySQL die al op deze pc is geinstalleerd.
    return Find-InstalledMySqlServer
}

function Install-PortableMySql {
    $server = Join-Path $SentinelMySqlRoot "bin\mysqld.exe"

    if (Test-Path -LiteralPath $server) {
        return $server
    }

    Write-Host "MySQL is niet gevonden. MySQL wordt gedownload (ca. 300 MB)..."

    $zip = Join-Path $env:TEMP "sentinel-mysql.zip"
    $extract = Join-Path $env:TEMP "sentinel-mysql-extract"

    Remove-Item $zip -Force -ErrorAction SilentlyContinue
    Remove-Item $extract -Recurse -Force -ErrorAction SilentlyContinue

    $downloaded = $false

    foreach ($url in $MySqlDownloadUrls) {
        try {
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
            $downloaded = $true
            break
        }
        catch {
            Remove-Item $zip -Force -ErrorAction SilentlyContinue
        }
    }

    if (-not $downloaded) {
        throw "MySQL kon niet worden gedownload. Controleer uw internetverbinding."
    }

    Write-Host "MySQL wordt uitgepakt..."
    Expand-Archive -Path $zip -DestinationPath $extract -Force

    $mysqld = Get-ChildItem -LiteralPath $extract -Filter "mysqld.exe" -Recurse -File | Select-Object -First 1

    if ($null -eq $mysqld) {
        throw "mysqld.exe ontbreekt in de MySQL download."
    }

    $sourceRoot = Split-Path -Parent (Split-Path -Parent $mysqld.FullName)

    # De map niet eerst leegmaken: daar kan de SENTINEL datamap al in staan.
    New-Item -ItemType Directory -Path $SentinelMySqlRoot -Force | Out-Null
    Copy-Item -Path (Join-Path $sourceRoot "*") -Destination $SentinelMySqlRoot -Recurse -Force

    Remove-Item $zip -Force -ErrorAction SilentlyContinue
    Remove-Item $extract -Recurse -Force -ErrorAction SilentlyContinue

    return $server
}

# MySQL voor Windows heeft de Microsoft Visual C++ Runtime nodig.
function Ensure-VcRuntime {
    $system32 = Join-Path $env:WINDIR "System32"

    if ((Test-Path (Join-Path $system32 "vcruntime140_1.dll")) -and (Test-Path (Join-Path $system32 "msvcp140.dll"))) {
        return
    }

    Write-Host "Microsoft Visual C++ Runtime (nodig voor MySQL) wordt geinstalleerd..."
    Write-Host "Geef toestemming als Windows daarom vraagt."

    $installer = Join-Path $env:TEMP "sentinel-vc_redist.x64.exe"
    Invoke-WebRequest -Uri "https://aka.ms/vs/17/release/vc_redist.x64.exe" -OutFile $installer -UseBasicParsing

    $process = Start-Process -FilePath $installer -ArgumentList "/install /quiet /norestart" -Verb RunAs -Wait -PassThru
    Remove-Item $installer -Force -ErrorAction SilentlyContinue

    # 1638 = nieuwere versie staat er al, 3010 = herstart aanbevolen.
    if (@(0, 1638, 3010) -notcontains $process.ExitCode) {
        throw "Visual C++ Runtime installatie is mislukt (code $($process.ExitCode))."
    }
}

# ============================================================
# SENTINEL MySQL server instellen en starten
# ============================================================

function Write-SentinelMySqlConfig {
    param(
        [string]$Server,
        [int]$Port
    )

    $baseDir = (Split-Path -Parent (Split-Path -Parent $Server)).Replace("\", "/")
    $dataDir = $SentinelMySqlData.Replace("\", "/")

    $lines = @(
        "[mysqld]",
        "basedir=$baseDir",
        "datadir=$dataDir",
        "port=$Port",
        "bind-address=127.0.0.1",
        "mysqlx=0",
        "character-set-server=utf8mb4",
        "",
        "[client]",
        "host=127.0.0.1",
        "port=$Port"
    )

    New-Item -ItemType Directory -Path $SentinelMySqlRoot -Force | Out-Null

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($SentinelMySqlIni, $lines, $encoding)
}

function Initialize-SentinelMySqlData {
    param([string]$Server)

    if (Test-Path -LiteralPath (Join-Path $SentinelMySqlData "mysql")) {
        return
    }

    Write-Host "MySQL wordt voor de eerste keer ingesteld..."

    Remove-Item $SentinelMySqlData -Recurse -Force -ErrorAction SilentlyContinue

    $result = Invoke-Tool -FilePath $Server -Arguments @("--defaults-file=`"$SentinelMySqlIni`"", "--initialize-insecure", "--console")

    if ($result.ExitCode -ne 0) {
        throw "MySQL initialisatie is mislukt.`n$($result.Error)"
    }
}

function Start-SentinelMySqlServer {
    param(
        [string]$Server,
        [int]$Port
    )

    $listenerId = Get-PortListenerProcessId -Port $Port
    $ownIds = @(Get-SentinelMySqlProcessIds)

    # MySQL op Windows draait als twee processen (monitor + server).
    # Luistert een van beide op de poort, dan draait de server al.
    if ($null -ne $listenerId -and $ownIds -contains $listenerId) {
        Write-Host "MySQL server draait al op poort $Port."
        return
    }

    # SENTINEL mysqld processen die niet op de poort luisteren zijn vastgelopen.
    foreach ($processId in $ownIds) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }

    Write-Host "MySQL server wordt gestart (mysqld --console) op poort $Port..."
    Start-Process -FilePath $Server -ArgumentList "--defaults-file=`"$SentinelMySqlIni`" --console" -WindowStyle Hidden

    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Milliseconds 500

        if (Test-MySqlPort -Port $Port) {
            return
        }
    }

    throw "De MySQL server kon niet worden gestart."
}

# ============================================================
# Gebruiker en tabellen
# ============================================================

function Ensure-SentinelDatabaseUser {
    param(
        [string]$Client,
        [int]$Port,
        [string]$Database,
        [string]$User,
        [string]$Password
    )

    $safeUser = $User.Replace("'", "''")
    $safePassword = $Password.Replace("\", "\\").Replace("'", "''")

    $sql = "CREATE DATABASE IF NOT EXISTS ``$Database`` CHARACTER SET utf8mb4;"

    foreach ($hostName in @("127.0.0.1", "localhost")) {
        $account = "'$safeUser'@'$hostName'"
        $sql += " CREATE USER IF NOT EXISTS $account IDENTIFIED BY '$safePassword';" +
                " ALTER USER $account IDENTIFIED BY '$safePassword';" +
                " GRANT ALL PRIVILEGES ON ``$Database``.* TO $account;"
    }

    $setup = Invoke-MySql -Client $Client -Port $Port -User "root" -Password "" -Sql $sql

    if (-not $setup.Success) {
        # Als root een wachtwoord heeft, moet de gebruiker al bestaan.
        $check = Invoke-MySql -Client $Client -Port $Port -User $User -Password $Password -Sql "SELECT 1;"

        if (-not $check.Success) {
            throw "Database gebruiker '$User' kon niet worden aangemaakt.`n$($setup.Output)"
        }
    }

    Write-Host "Database gebruiker '$User' is klaar."
}

function Get-SqlFileTables {
    param([string]$SqlFile)

    $text = Get-Content -LiteralPath $SqlFile -Raw
    $tableMatches = [regex]::Matches($text, 'CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?', "IgnoreCase")

    return @($tableMatches | ForEach-Object { $_.Groups[1].Value.ToLower() } | Sort-Object -Unique)
}

function Get-DatabaseTables {
    param(
        [string]$Client,
        [int]$Port,
        [string]$Database,
        [string]$User,
        [string]$Password
    )

    $result = Invoke-MySql -Client $Client -Port $Port -User $User -Password $Password -Sql "SELECT LOWER(table_name) FROM information_schema.tables WHERE table_schema='$Database';"

    if (-not $result.Success) {
        throw "De SENTINEL database kon niet worden gecontroleerd.`n$($result.Output)"
    }

    return @($result.Output -split "\r?\n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" })
}

# Importeert sentinel.sql als er tabellen ontbreken of als het bestand is veranderd.
# Zo blijven gegevens uit de applicatie bewaard zolang sentinel.sql gelijk blijft.
function Sync-SentinelDatabase {
    param(
        [string]$Client,
        [int]$Port,
        [string]$Database,
        [string]$User,
        [string]$Password,
        [string]$SqlFile
    )

    $expected = @(Get-SqlFileTables -SqlFile $SqlFile)
    $existing = @(Get-DatabaseTables -Client $Client -Port $Port -Database $Database -User $User -Password $Password)
    $missing = @($expected | Where-Object { $existing -notcontains $_ })

    $currentHash = (Get-FileHash -LiteralPath $SqlFile -Algorithm SHA256).Hash
    $importedHash = ""

    if (Test-Path -LiteralPath $SentinelSqlHashFile) {
        $importedHash = (Get-Content -LiteralPath $SentinelSqlHashFile -Raw).Trim()
    }

    if ($missing.Count -eq 0 -and $importedHash -eq $currentHash) {
        Write-Host "Database is up-to-date met sentinel.sql."
        return
    }

    $connection = @("--protocol=tcp", "-h", "127.0.0.1", "-P", "$Port", "-u", $User)

    # Eerst de huidige database bewaren, want sentinel.sql vervangt de tabellen.
    if ($existing.Count -gt 0) {
        $dump = Join-Path (Split-Path -Parent $Client) "mysqldump.exe"
        New-Item -ItemType Directory -Path $SentinelBackupDir -Force | Out-Null
        $backupFile = Join-Path $SentinelBackupDir ("sentinel-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".sql")

        $backup = Invoke-Tool -FilePath $dump -Password $Password -OutputFile $backupFile `
            -Arguments ($connection + @("--no-tablespaces", "--single-transaction", "--databases", $Database))

        if ($backup.ExitCode -ne 0) {
            throw "Backup van de huidige database is mislukt.`n$($backup.Error)"
        }

        Write-Host "Oude database bewaard in: $backupFile"
    }

    Write-Host "sentinel.sql wordt geimporteerd..."

    $import = Invoke-Tool -FilePath $Client -Password $Password -InputFile $SqlFile `
        -Arguments ($connection + @("--default-character-set=utf8mb4"))

    if ($import.ExitCode -ne 0) {
        throw "sentinel.sql import is mislukt.`n$($import.Error)"
    }

    $existing = @(Get-DatabaseTables -Client $Client -Port $Port -Database $Database -User $User -Password $Password)
    $missing = @($expected | Where-Object { $existing -notcontains $_ })

    if ($missing.Count -gt 0) {
        throw "Na de import ontbreken nog tabellen: $($missing -join ', ')"
    }

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($SentinelSqlHashFile, $currentHash, $encoding)

    Write-Host "sentinel.sql is geimporteerd ($($expected.Count) tabellen)."
}

# ============================================================
# Hoofdfunctie
# ============================================================

function Ensure-SentinelMySql {
    param([string]$ProjectRoot)

    $configFile = Join-Path $ProjectRoot "config\database.properties"
    $sqlFile = Join-Path $ProjectRoot "sentinel.sql"
    $props = Read-DbProperties -Path $configFile

    foreach ($key in @("db.port", "db.name", "db.user", "db.password")) {
        if (-not $props.ContainsKey($key)) {
            throw "database.properties mist $key."
        }
    }

    $port = [int]$props["db.port"]
    $database = $props["db.name"]
    $user = $props["db.user"]
    $password = $props["db.password"]

    if ($database -notmatch '^[A-Za-z0-9_]+$') {
        throw "Ongeldige database naam in database.properties."
    }

    if ($user -notmatch '^[A-Za-z0-9_.-]+$') {
        throw "Ongeldige database gebruiker in database.properties."
    }

    if (-not (Test-Path -LiteralPath $sqlFile)) {
        throw "sentinel.sql ontbreekt."
    }

    # 1. MySQL zoeken, anders downloaden.
    Ensure-VcRuntime

    $server = Find-SentinelMySqlServer

    if ($null -eq $server) {
        $server = Install-PortableMySql
    }

    $client = Join-Path (Split-Path -Parent $server) "mysql.exe"

    if (-not (Test-Path -LiteralPath $client)) {
        throw "mysql.exe ontbreekt naast $server."
    }

    Write-Host "MySQL $(Get-MySqlServerVersion -Server $server) gevonden."

    # 2. Is de poort bezet door een andere MySQL? Dan een vrije poort kiezen.
    $listenerId = Get-PortListenerProcessId -Port $port

    if ($null -ne $listenerId -and (Get-SentinelMySqlProcessIds) -notcontains $listenerId) {
        $port = Find-FreePort -StartPort ($port + 1)
        Set-DbProperty -Path $configFile -Key "db.port" -Value "$port"
        Write-Host "De standaardpoort is bezet. SENTINEL gebruikt poort $port."
    }

    # 3. Instellen en starten.
    Write-SentinelMySqlConfig -Server $server -Port $port
    Initialize-SentinelMySqlData -Server $server
    Start-SentinelMySqlServer -Server $server -Port $port

    # 4. Gebruiker uit database.properties aanmaken.
    Ensure-SentinelDatabaseUser -Client $client -Port $port -Database $database -User $user -Password $password

    # 5. Tabellen en gegevens uit sentinel.sql.
    Sync-SentinelDatabase -Client $client -Port $port -Database $database -User $user -Password $password -SqlFile $sqlFile

    return [PSCustomObject]@{ Host = "127.0.0.1"; Port = $port; Database = $database; User = $user }
}

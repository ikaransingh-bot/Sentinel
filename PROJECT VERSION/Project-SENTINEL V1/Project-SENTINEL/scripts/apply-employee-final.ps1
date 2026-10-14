param(
    [string]$ProjectRoot = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot =
        Split-Path `
            -Parent `
            $PSScriptRoot
}

$configFile =
    Join-Path `
        $ProjectRoot `
        "config\database.properties"

$sqlFile =
    Join-Path `
        $ProjectRoot `
        "employee-final-migration.sql"


function Read-DbProperties {

    param(
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "database.properties ontbreekt."
    }

    $properties = @{}

    foreach ($line in Get-Content -LiteralPath $Path) {

        $text = $line.Trim()

        if (
            $text -eq "" -or
            $text.StartsWith("#")
        ) {
            continue
        }

        $separator =
            $text.IndexOf("=")

        if ($separator -le 0) {
            continue
        }

        $key =
            $text.Substring(
                0,
                $separator
            ).Trim()

        $value =
            $text.Substring(
                $separator + 1
            ).Trim()

        $properties[$key] =
            $value
    }

    return $properties
}


function Find-MySqlClient {

    $command =
        Get-Command `
            "mysql.exe" `
            -ErrorAction SilentlyContinue

    if ($null -ne $command) {
        return $command.Source
    }

    $knownPaths = @(
        "$env:LOCALAPPDATA\ProjectSentinel\runtime\mysql\bin\mysql.exe",
        "$env:USERPROFILE\scoop\apps\mysql\current\bin\mysql.exe",
        "C:\xampp\mysql\bin\mysql.exe"
    )

    foreach ($path in $knownPaths) {

        if (Test-Path -LiteralPath $path) {
            return $path
        }
    }

    $mysqlFolder =
        Join-Path `
            $env:ProgramFiles `
            "MySQL"

    if (Test-Path -LiteralPath $mysqlFolder) {

        $file =
            Get-ChildItem `
                -LiteralPath $mysqlFolder `
                -Filter "mysql.exe" `
                -Recurse `
                -File `
                -ErrorAction SilentlyContinue |
            Select-Object -First 1

        if ($null -ne $file) {
            return $file.FullName
        }
    }

    return $null
}


if (-not (Test-Path -LiteralPath $sqlFile)) {
    throw "employee-final-migration.sql ontbreekt."
}

$properties =
    Read-DbProperties `
        -Path $configFile

$requiredKeys = @(
    "db.host",
    "db.port",
    "db.name",
    "db.user",
    "db.password"
)

foreach ($key in $requiredKeys) {

    if (-not $properties.ContainsKey($key)) {
        throw "database.properties mist $key."
    }
}

$client =
    Find-MySqlClient

if ($null -eq $client) {
    throw "mysql.exe werd niet gevonden. Start SENTINEL/MySQL eerst."
}

$oldPassword =
    [Environment]::GetEnvironmentVariable(
        "MYSQL_PWD",
        "Process"
    )

try {

    [Environment]::SetEnvironmentVariable(
        "MYSQL_PWD",
        $properties["db.password"],
        "Process"
    )

    Write-Host ""
    Write-Host "SENTINEL Employee database migration wordt uitgevoerd..."
    Write-Host ""

    Get-Content `
        -LiteralPath $sqlFile `
        -Raw |
    & $client `
        --protocol=tcp `
        -h $properties["db.host"] `
        -P $properties["db.port"] `
        -u $properties["db.user"] `
        -D $properties["db.name"]

    if ($LASTEXITCODE -ne 0) {
        throw "Employee database migration is mislukt."
    }

    Write-Host ""
    Write-Host "Employee database migration is klaar."
    Write-Host ""
}
finally {

    [Environment]::SetEnvironmentVariable(
        "MYSQL_PWD",
        $oldPassword,
        "Process"
    )
}

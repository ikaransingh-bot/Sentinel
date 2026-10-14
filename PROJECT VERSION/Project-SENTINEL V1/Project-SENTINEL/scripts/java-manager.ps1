Set-StrictMode -Version Latest

function Get-SentinelJavaVersion {

    param(
        [Parameter(Mandatory = $true)]
        [string]$JavacExe
    )

    try {

        $versionText =
            (& $JavacExe -version 2>&1 | Out-String).Trim()

        if ($versionText -match 'javac\s+([0-9]+)') {
            return [int]$Matches[1]
        }

        if ($versionText -match 'javac\s+1\.([0-9]+)') {
            return [int]$Matches[1]
        }

        return 0

    }
    catch {

        return 0
    }
}


function Test-SentinelJavaHome {

    param(
        [Parameter(Mandatory = $true)]
        [string]$JavaHome,

        [int]$MinimumVersion = 21
    )

    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        return $null
    }

    $javaExe =
        Join-Path $JavaHome "bin\java.exe"

    $javacExe =
        Join-Path $JavaHome "bin\javac.exe"

    if (-not (Test-Path $javaExe)) {
        return $null
    }

    if (-not (Test-Path $javacExe)) {
        return $null
    }

    $version =
        Get-SentinelJavaVersion `
            -JavacExe $javacExe

    if ($version -lt $MinimumVersion) {
        return $null
    }

    return [PSCustomObject]@{
        JavaHome = $JavaHome
        JavaExe  = $javaExe
        JavacExe = $javacExe
        Version  = $version
    }
}


function Find-SentinelSystemJava {

    param(
        [int]$MinimumVersion = 21
    )

    # Eerst JAVA_HOME proberen

    if (
        $env:JAVA_HOME -and
        -not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)
    ) {

        $result =
            Test-SentinelJavaHome `
                -JavaHome $env:JAVA_HOME `
                -MinimumVersion $MinimumVersion

        if ($null -ne $result) {
            return $result
        }
    }


    # Daarna javac zoeken via PATH

    $javacCommand =
        Get-Command javac.exe `
            -ErrorAction SilentlyContinue

    if ($null -ne $javacCommand) {

        $binDirectory =
            Split-Path `
                -Parent `
                $javacCommand.Source

        $javaHome =
            Split-Path `
                -Parent `
                $binDirectory

        $result =
            Test-SentinelJavaHome `
                -JavaHome $javaHome `
                -MinimumVersion $MinimumVersion

        if ($null -ne $result) {
            return $result
        }
    }

    return $null
}


function Get-SentinelPortableJavaHome {

    $localAppData =
        [Environment]::GetFolderPath(
            "LocalApplicationData"
        )

    if ([string]::IsNullOrWhiteSpace($localAppData)) {
        throw "LOCALAPPDATA kon niet worden bepaald."
    }

    return Join-Path `
        $localAppData `
        "ProjectSentinel\runtime\jdk"
}


function Install-SentinelPortableJava {

    param(
        [int]$Version = 21
    )

    $javaHome =
        Get-SentinelPortableJavaHome

    $existing =
        Test-SentinelJavaHome `
            -JavaHome $javaHome `
            -MinimumVersion $Version

    if ($null -ne $existing) {
        return $existing
    }


    Write-Host "Java $Version is niet gevonden. Java wordt gedownload..."

    $runtimeRoot =
        Split-Path `
            -Parent `
            $javaHome

    New-Item `
        -ItemType Directory `
        -Force `
        -Path $runtimeRoot |
        Out-Null


    $downloadUrl =
        "https://api.adoptium.net/v3/binary/latest/" +
        "$Version/ga/windows/x64/jdk/hotspot/normal/eclipse"


    $tempZip =
        Join-Path `
            $env:TEMP `
            "sentinel-jdk-$PID.zip"

    $tempDirectory =
        Join-Path `
            $env:TEMP `
            "sentinel-jdk-$PID"


    Remove-Item `
        $tempZip `
        -Force `
        -ErrorAction SilentlyContinue

    Remove-Item `
        $tempDirectory `
        -Recurse `
        -Force `
        -ErrorAction SilentlyContinue


    try {

        Invoke-WebRequest `
            -Uri $downloadUrl `
            -OutFile $tempZip `
            -UseBasicParsing `
            -ErrorAction Stop

    }
    catch {

        throw (
            "Java $Version ontbreekt en kon niet " +
            "worden gedownload. Controleer uw internetverbinding."
        )
    }


    New-Item `
        -ItemType Directory `
        -Force `
        -Path $tempDirectory |
        Out-Null


    try {

        Expand-Archive `
            -Path $tempZip `
            -DestinationPath $tempDirectory `
            -Force

    }
    catch {

        throw "De Java download kon niet worden uitgepakt."
    }


    $javacFile =
        Get-ChildItem `
            -Path $tempDirectory `
            -Filter "javac.exe" `
            -Recurse `
            -File `
            -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Directory.Name -eq "bin"
        } |
        Select-Object -First 1


    if ($null -eq $javacFile) {

        throw (
            "javac.exe werd niet gevonden " +
            "in de gedownloade JDK."
        )
    }


    $detectedBin =
        Split-Path `
            -Parent `
            $javacFile.FullName

    $detectedHome =
        Split-Path `
            -Parent `
            $detectedBin


    Remove-Item `
        $javaHome `
        -Recurse `
        -Force `
        -ErrorAction SilentlyContinue


    New-Item `
        -ItemType Directory `
        -Force `
        -Path $javaHome |
        Out-Null


    Copy-Item `
        -Path (
            Join-Path $detectedHome "*"
        ) `
        -Destination $javaHome `
        -Recurse `
        -Force


    Remove-Item `
        $tempZip `
        -Force `
        -ErrorAction SilentlyContinue

    Remove-Item `
        $tempDirectory `
        -Recurse `
        -Force `
        -ErrorAction SilentlyContinue


    $result =
        Test-SentinelJavaHome `
            -JavaHome $javaHome `
            -MinimumVersion $Version


    if ($null -eq $result) {

        throw (
            "De portable Java omgeving kon " +
            "niet correct worden voorbereid."
        )
    }


    return $result
}


function Ensure-SentinelJava {

    param(
        [int]$MinimumVersion = 21
    )

    # Eerst Java van de computer gebruiken.

    $systemJava =
        Find-SentinelSystemJava `
            -MinimumVersion $MinimumVersion


    if ($null -ne $systemJava) {

        return [PSCustomObject]@{
            JavaHome = $systemJava.JavaHome
            JavaExe  = $systemJava.JavaExe
            JavacExe = $systemJava.JavacExe
            Version  = $systemJava.Version
            Mode     = "SYSTEM"
        }
    }


    # Daarna onze portable Java proberen.

    $portableHome =
        Get-SentinelPortableJavaHome


    $portableJava =
        Test-SentinelJavaHome `
            -JavaHome $portableHome `
            -MinimumVersion $MinimumVersion


    if ($null -eq $portableJava) {

        $portableJava =
            Install-SentinelPortableJava `
                -Version $MinimumVersion
    }


    return [PSCustomObject]@{
        JavaHome = $portableJava.JavaHome
        JavaExe  = $portableJava.JavaExe
        JavacExe = $portableJava.JavacExe
        Version  = $portableJava.Version
        Mode     = "PORTABLE"
    }
}
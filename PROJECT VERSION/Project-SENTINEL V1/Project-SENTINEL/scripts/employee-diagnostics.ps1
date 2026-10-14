param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        "NETWORK",
        "PRINTER",
        "SYSTEM",
        "SECURITY",
        "UPDATE",
        "OFFICE"
    )]
    [string]$Action
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"


function Clean-Text {

    param(
        [string]$Text
    )

    if ($null -eq $Text) {
        return ""
    }

    return (
        $Text `
            -replace "`r", " " `
            -replace "`n", " "
    ).Trim()
}


function Write-Result {

    param(
        [string]$Status,
        [string]$Message,
        [string]$Detail,
        [string]$Recommendation
    )

    Write-Output (
        "STATUS=" +
        (Clean-Text $Status)
    )

    Write-Output (
        "MESSAGE=" +
        (Clean-Text $Message)
    )

    Write-Output (
        "DETAIL=" +
        (Clean-Text $Detail)
    )

    Write-Output (
        "RECOMMENDATION=" +
        (Clean-Text $Recommendation)
    )
}


function Test-Network {

    try {

        $adapters =
            Get-NetAdapter `
                -ErrorAction Stop |
            Where-Object {
                $_.Status -eq "Up"
            }

        if (-not $adapters) {

            Write-Result `
                "ERROR" `
                "Geen actieve netwerkadapter gevonden." `
                "Windows meldt geen actieve netwerkadapter." `
                "Controleer Wi-Fi, netwerkkabel of netwerkadapter."

            return
        }

        $configuration =
            Get-NetIPConfiguration `
                -ErrorAction Stop |
            Where-Object {
                $null -ne $_.IPv4Address
            } |
            Select-Object -First 1

        if ($null -eq $configuration) {

            Write-Result `
                "ERROR" `
                "Geen IPv4-adres gevonden." `
                "Er is een actieve adapter maar geen bruikbaar IPv4-adres." `
                "Controleer DHCP of de netwerkconfiguratie."

            return
        }

        $ip =
            $configuration.IPv4Address.IPAddress

        $internet =
            Test-Connection `
                -ComputerName "1.1.1.1" `
                -Count 1 `
                -Quiet `
                -ErrorAction SilentlyContinue

        if ($internet) {

            Write-Result `
                "OK" `
                "Basis netwerkverbinding is bereikbaar." `
                "IPv4-adres: $ip. Externe bereikbaarheidstest is gelukt." `
                "Geen duidelijke netwerkfout gevonden."

        } else {

            Write-Result `
                "WARNING" `
                "Lokale netwerkinfo is aanwezig maar de externe test mislukte." `
                "IPv4-adres: $ip. 1.1.1.1 was niet bereikbaar." `
                "Controleer router, internetverbinding of firewall."
        }
    }
    catch {

        Write-Result `
            "TECHNICIAN_REQUIRED" `
            "Netwerkcontrole kon niet volledig worden uitgevoerd." `
            $_.Exception.Message `
            "Laat een technician de netwerkstatus controleren."
    }
}


function Test-Printers {

    try {

        $command =
            Get-Command `
                "Get-Printer" `
                -ErrorAction SilentlyContinue

        if ($null -eq $command) {

            Write-Result `
                "TECHNICIAN_REQUIRED" `
                "Windows printerinformatie is niet beschikbaar." `
                "Get-Printer is op dit systeem niet beschikbaar." `
                "Laat een technician de printer handmatig controleren."

            return
        }

        $printers =
            Get-Printer `
                -ErrorAction Stop

        if (-not $printers) {

            Write-Result `
                "WARNING" `
                "Windows ziet geen printer." `
                "Er zijn geen printers geregistreerd in Windows." `
                "Controleer printerinstallatie, USB of netwerkverbinding."

            return
        }

        $names =
            ($printers |
                Select-Object -ExpandProperty Name) `
                -join ", "

        Write-Result `
            "OK" `
            "Windows ziet een of meer printers." `
            "Printers: $names" `
            "Als printen toch niet werkt moet een technician wachtrij, driver of verbinding controleren."
    }
    catch {

        Write-Result `
            "TECHNICIAN_REQUIRED" `
            "Printercontrole kon niet worden uitgevoerd." `
            $_.Exception.Message `
            "Laat een technician de printer controleren."
    }
}


function Test-System {

    try {

        $os =
            Get-CimInstance `
                Win32_OperatingSystem `
                -ErrorAction Stop

        $cpu =
            Get-CimInstance `
                Win32_Processor `
                -ErrorAction Stop |
            Select-Object -First 1

        $totalMemory =
            [double]$os.TotalVisibleMemorySize

        $freeMemory =
            [double]$os.FreePhysicalMemory

        $freePercent = 0

        if ($totalMemory -gt 0) {

            $freePercent =
                ($freeMemory / $totalMemory) * 100
        }

        $load =
            [int]$cpu.LoadPercentage

        $detail =
            "Vrij RAM: {0:N1} procent. CPU load: {1} procent." `
                -f $freePercent, $load

        if (
            $freePercent -lt 15 -or
            $load -gt 90
        ) {

            Write-Result `
                "WARNING" `
                "Het systeem gebruikt veel resources." `
                $detail `
                "Sluit onnodige programma's. Als de computer traag blijft, laat een technician verder controleren."

        } else {

            Write-Result `
                "OK" `
                "De gemeten systeemresources lijken normaal." `
                $detail `
                "Geen duidelijke resourcefout gevonden."
        }
    }
    catch {

        Write-Result `
            "TECHNICIAN_REQUIRED" `
            "Algemene systeemcontrole kon niet worden uitgevoerd." `
            $_.Exception.Message `
            "Laat een technician de systeeminformatie controleren."
    }
}


function Test-Security {

    try {

        $command =
            Get-Command `
                "Get-MpComputerStatus" `
                -ErrorAction SilentlyContinue

        if ($null -eq $command) {

            Write-Result `
                "TECHNICIAN_REQUIRED" `
                "Windows Security status is niet beschikbaar." `
                "Get-MpComputerStatus is niet beschikbaar." `
                "Laat een technician controleren welke beveiliging actief is."

            return
        }

        $status =
            Get-MpComputerStatus `
                -ErrorAction Stop

        $antivirus =
            $status.AntivirusEnabled

        $realtime =
            $status.RealTimeProtectionEnabled

        $detail =
            "AntivirusEnabled=$antivirus, RealTimeProtectionEnabled=$realtime"

        if ($antivirus -and $realtime) {

            Write-Result `
                "OK" `
                "Windows beveiliging is actief." `
                $detail `
                "Geen duidelijke beveiligingswaarschuwing gevonden."

        } else {

            Write-Result `
                "WARNING" `
                "Een beveiligingsonderdeel lijkt niet actief." `
                $detail `
                "Laat een technician controleren waarom de bescherming niet volledig actief is."
        }
    }
    catch {

        Write-Result `
            "TECHNICIAN_REQUIRED" `
            "Securitycontrole kon niet volledig worden uitgevoerd." `
            $_.Exception.Message `
            "Laat een technician Windows Security controleren."
    }
}


function Test-WindowsUpdate {

    try {

        $rebootPending =
            Test-Path `
                "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Component Based Servicing\RebootPending"

        $updateReboot =
            Test-Path `
                "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\WindowsUpdate\Auto Update\RebootRequired"

        $service =
            Get-Service `
                -Name "wuauserv" `
                -ErrorAction SilentlyContinue

        $serviceStatus =
            if ($null -eq $service) {
                "ONBEKEND"
            } else {
                $service.Status.ToString()
            }

        if ($rebootPending -or $updateReboot) {

            Write-Result `
                "WARNING" `
                "Windows meldt dat een herstart nodig kan zijn." `
                "Windows Update service: $serviceStatus. Reboot pending=True." `
                "Sla werk op en plan een herstart. SENTINEL installeert geen updates."

        } else {

            Write-Result `
                "OK" `
                "Geen lokale reboot-waarschuwing gevonden." `
                "Windows Update service: $serviceStatus. Reboot pending=False." `
                "SENTINEL heeft alleen lokale update-informatie gelezen."
        }
    }
    catch {

        Write-Result `
            "TECHNICIAN_REQUIRED" `
            "Windows Update controle kon niet volledig worden uitgevoerd." `
            $_.Exception.Message `
            "Laat een technician Windows Update controleren."
    }
}


function Test-Office {

    try {

        $paths = @(
            "$env:ProgramFiles\Microsoft Office\Office16\OSPP.VBS",
            "${env:ProgramFiles(x86)}\Microsoft Office\Office16\OSPP.VBS",
            "$env:ProgramFiles\Microsoft Office\root\Office16\OSPP.VBS",
            "${env:ProgramFiles(x86)}\Microsoft Office\root\Office16\OSPP.VBS"
        )

        $ospp = $null

        foreach ($path in $paths) {

            if (
                $path -and
                (Test-Path -LiteralPath $path)
            ) {

                $ospp = $path
                break
            }
        }

        if ($null -eq $ospp) {

            Write-Result `
                "TECHNICIAN_REQUIRED" `
                "Office licentie-informatie kon niet betrouwbaar worden gevonden." `
                "OSPP.VBS is niet gevonden op de standaard locaties." `
                "Controleer of Office is geinstalleerd en welke versie wordt gebruikt."

            return
        }

        $output =
            & cscript.exe `
                //NoLogo `
                $ospp `
                /dstatus `
                2>&1 |
            Out-String

        if (
            $output -match
            "LICENSE STATUS:\s+---LICENSED---"
        ) {

            Write-Result `
                "OK" `
                "Office meldt een gelicentieerde status." `
                "OSPP.VBS rapporteert LICENSED." `
                "Geen duidelijke licentiefout gevonden."

        } else {

            Write-Result `
                "WARNING" `
                "Office meldt geen duidelijke LICENSED status." `
                "De lokale Office licentiecontrole gaf geen LICENSED resultaat." `
                "Laat een technician de Office activatie en licentie controleren."
        }
    }
    catch {

        Write-Result `
            "TECHNICIAN_REQUIRED" `
            "Office controle kon niet volledig worden uitgevoerd." `
            $_.Exception.Message `
            "Laat een technician de Office installatie controleren."
    }
}


switch ($Action) {

    "NETWORK" {
        Test-Network
    }

    "PRINTER" {
        Test-Printers
    }

    "SYSTEM" {
        Test-System
    }

    "SECURITY" {
        Test-Security
    }

    "UPDATE" {
        Test-WindowsUpdate
    }

    "OFFICE" {
        Test-Office
    }
}

# Exitcode 0 = er is een resultaat geschreven. Elke Test-functie vangt zijn
# eigen fouten af; een andere exitcode betekent dat PowerShell zelf faalde.
exit 0

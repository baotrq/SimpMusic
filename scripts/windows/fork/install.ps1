# SimpMusic (fork) installer for Windows. install.bat runs this as admin.
#
# Same idea as upstream's scripts/windows/install.bat: trust the certificate the .msix is signed
# with, then sideload the .msix. The package's name, publisher and app id are read from the .msix
# itself rather than hard-coded, and only a package with that exact name and publisher is replaced.
# Conveyor names the certificate after the app (CN=SimpMusic), so the fork has upstream's identity
# (family Simpmusic_ejp2bhxmz1qq6) although its key differs: installing it updates an upstream
# SimpMusic in place, as the Mac build replaces upstream's .app.
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

function Fail($message) {
    Write-Host ""
    Write-Host "[ERROR] $message" -ForegroundColor Red
    exit 1
}

$cert = Join-Path $here 'simpmusic.crt'
if (-not (Test-Path $cert)) { Fail "simpmusic.crt not found next to install.bat." }
$msix = Get-ChildItem -Path $here -Filter 'simpmusic-*.msix' | Select-Object -First 1
if (-not $msix) { Fail "No simpmusic-*.msix found next to install.bat." }

# The package's identity and app id, from its manifest.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($msix.FullName)
try {
    $reader = New-Object IO.StreamReader($zip.GetEntry('AppxManifest.xml').Open())
    [xml]$manifest = $reader.ReadToEnd()
    $reader.Close()
} finally {
    $zip.Dispose()
}
$name = $manifest.Package.Identity.Name
$publisher = $manifest.Package.Identity.Publisher
$appId = @($manifest.Package.Applications.Application)[0].Id

Write-Host "[1/4] Trusting the SimpMusic certificate (LocalMachine\TrustedPeople) ..."
certutil -addstore -f TrustedPeople "$cert" | Out-Null
if ($LASTEXITCODE -ne 0) { Fail "Certificate import failed." }

# The earlier fork builds shipped as an .msi (publisher baotrq, installed per user). Remove it, or
# Start would list two SimpMusic apps. Settings and library live in ~/.simpmusic, not in either install,
# so nothing is lost.
Write-Host "[2/4] Removing the old .msi version, if any ..."
$uninstallKeys = @(
    'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
    'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
)
Get-ItemProperty -Path $uninstallKeys -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -eq 'SimpMusic' -and $_.Publisher -eq 'baotrq' -and $_.PSChildName -match '^\{[0-9A-Fa-f-]+\}$' } |
    ForEach-Object {
        Write-Host "      uninstalling $($_.DisplayName) $($_.DisplayVersion)"
        Start-Process msiexec.exe -ArgumentList "/x $($_.PSChildName) /qn" -Wait
    }

Write-Host "[3/4] Installing $($msix.Name) ..."
# -ForceUpdateFromAnyVersion installs over an existing copy in place (keeping its data) even at the
# same version. If that is refused, remove only OUR package and install it fresh.
try {
    Add-AppxPackage -Path $msix.FullName -ForceApplicationShutdown -ForceUpdateFromAnyVersion
} catch {
    $existing = Get-AppxPackage -Name $name | Where-Object { $_.Publisher -eq $publisher }
    if (-not $existing) {
        Write-Host $_
        Fail "Install failed. If Windows says sideloading is off, turn on Settings > System > For developers > 'Install apps from any source', then run install.bat again."
    }
    Write-Host "      In-place update refused; reinstalling ..."
    $existing | Remove-AppxPackage
    try {
        Add-AppxPackage -Path $msix.FullName -ForceApplicationShutdown
    } catch {
        Write-Host $_
        Fail "Install failed."
    }
}

Write-Host "[4/4] Opening SimpMusic ..."
$package = Get-AppxPackage -Name $name | Where-Object { $_.Publisher -eq $publisher } | Select-Object -First 1
if ($package) {
    # Through explorer, so the app starts as the normal user rather than inheriting this window's admin rights.
    Start-Process explorer.exe "shell:AppsFolder\$($package.PackageFamilyName)!$appId"
}

Write-Host ""
Write-Host "SimpMusic is installed. It is in the Start menu from now on."
Write-Host "To uninstall: Settings > Apps > Installed apps > SimpMusic > Uninstall."

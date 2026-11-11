# Checks a built Windows package before it is published: every DLL and EXE under bin/ that is NOT
# ours must still carry its vendor's own signature (Oracle, Eclipse Foundation, Azul, Microsoft).
#
# Why: Conveyor signs every unsigned binary with our self-signed CN=SimpMusic certificate, and
# Smart App Control does not trust that certificate. A JDK whose DLLs ship unsigned (Microsoft's
# OpenJDK 21.0.12 did) therefore ends up with every Java DLL signed only by us, and Windows 11 with
# Smart App Control on refuses to load them: "jimage.dll ... Bad Image, error status 0xc0e90002".
#
#   powershell -ExecutionPolicy Bypass -File scripts\windows\fork\check-signatures.ps1 <zip or msix>
#
# Exit code 0 when only our own launcher is signed by CN=SimpMusic, 1 otherwise.
param([Parameter(Mandatory = $true)][string]$Package)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Binaries we build ourselves; a CN=SimpMusic signature on these is expected.
$ours = @('SimpMusic.exe')
# Upstream ships these unsigned, so CN=SimpMusic is all they can carry. Reported, not failed: 2.5.0.10
# ran the same way. If Smart App Control ever blocks them, playback fails rather than launch.
$knownUnsigned = @('libmpv-2.dll')

$work = Join-Path ([IO.Path]::GetTempPath()) ("simpmusic-sigcheck-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $work | Out-Null
try {
    $msix = (Resolve-Path $Package).Path
    if ($msix -like '*.zip') {
        $zip = [IO.Compression.ZipFile]::OpenRead($msix)
        try {
            $entry = $zip.Entries | Where-Object { $_.Name -like '*.msix' } | Select-Object -First 1
            if (-not $entry) { throw "No .msix inside $Package" }
            $msix = Join-Path $work $entry.Name
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $msix)
        } finally { $zip.Dispose() }
    }

    $bin = Join-Path $work 'bin'
    New-Item -ItemType Directory -Path $bin | Out-Null
    $pkg = [IO.Compression.ZipFile]::OpenRead($msix)
    try {
        $pkg.Entries |
            Where-Object { $_.FullName -match '^bin/.+\.(dll|exe)$' } |
            ForEach-Object {
                $dest = Join-Path $bin ($_.FullName.Substring(4) -replace '/', '_')
                [IO.Compression.ZipFileExtensions]::ExtractToFile($_, $dest)
            }
    } finally { $pkg.Dispose() }

    $bad = @()
    Get-ChildItem $bin | ForEach-Object {
        $subject = (Get-AuthenticodeSignature $_.FullName).SignerCertificate.Subject
        $name = $_.Name -replace '^.*_', ''
        if ($name -in $knownUnsigned) {
            Write-Host "note: $name is signed only by $subject (known, upstream ships it unsigned)" -ForegroundColor Yellow
        } elseif ($name -notin $ours -and ($null -eq $subject -or $subject -eq 'CN=SimpMusic')) {
            $bad += [pscustomobject]@{ File = $_.Name; Signer = if ($subject) { $subject } else { '(unsigned)' } }
        }
    }

    if ($bad.Count -gt 0) {
        Write-Host "FAIL: $($bad.Count) binaries lost their vendor signature; Smart App Control will block them:" -ForegroundColor Red
        $bad | Format-Table -AutoSize | Out-String | Write-Host
        exit 1
    }
    $sample = Get-AuthenticodeSignature (Get-ChildItem $bin -Filter 'jimage.dll').FullName
    Write-Host "OK: every bundled binary keeps its vendor signature (jimage.dll: $($sample.SignerCertificate.Subject))" -ForegroundColor Green
    exit 0
} finally {
    Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
}

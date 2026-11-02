param(
    [Parameter(Mandatory=$true)][string]$DecodedDirectory,
    [Parameter(Mandatory=$true)][string]$Keystore,
    [Parameter(Mandatory=$true)][string]$Zipalign,
    [Parameter(Mandatory=$true)][string]$ApksignerJar,
    [Parameter(Mandatory=$true)][int]$VersionCode,
    [Parameter(Mandatory=$true)][string]$VersionName,
    [string]$AppLabel = '워치 케어',
    [string]$KeyAlias = 'androiddebugkey',
    [string]$StorePasswordEnv = 'WATCHCARE_STOREPASS',
    [string]$KeyPasswordEnv = 'WATCHCARE_STOREPASS',
    [string]$ApktoolJar = '',
    [string]$OverlayDirectory = '',
    [string]$OutputDirectory = 'dist',
    [string]$Python = 'python'
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $ApktoolJar) { $ApktoolJar = Join-Path $repoRoot 'workspace/SMART_TIME_PRO_customization/tools/apktool.jar' }
foreach ($inputFile in @($Keystore,$Zipalign,$ApksignerJar,$ApktoolJar)) {
    if (-not (Test-Path -LiteralPath $inputFile -PathType Leaf)) { throw "Required file missing: $inputFile" }
}
foreach ($passwordVariable in @($StorePasswordEnv,$KeyPasswordEnv)) {
    if ($passwordVariable -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') { throw 'Invalid password environment variable name.' }
    if (-not [Environment]::GetEnvironmentVariable($passwordVariable)) { throw "Set environment variable $passwordVariable before signing." }
}
$decodedPath = (Resolve-Path -LiteralPath $DecodedDirectory).Path
$keyPath = (Resolve-Path -LiteralPath $Keystore).Path
$alignPath = (Resolve-Path -LiteralPath $Zipalign).Path
$signerPath = (Resolve-Path -LiteralPath $ApksignerJar).Path
$toolPath = (Resolve-Path -LiteralPath $ApktoolJar).Path
if (-not (Test-Path -LiteralPath (Join-Path $decodedPath 'AndroidManifest.xml'))) { throw 'Decoded APK manifest missing.' }
if ($OverlayDirectory) {
    $overlayPath = (Resolve-Path -LiteralPath $OverlayDirectory).Path
    $overlayFiles = @(Get-ChildItem -LiteralPath $overlayPath -File -Recurse)
    $separators = [char[]]@([IO.Path]::DirectorySeparatorChar,[IO.Path]::AltDirectorySeparatorChar)
    foreach ($overlayFile in $overlayFiles) {
        $relativePath = $overlayFile.FullName.Substring($overlayPath.Length).TrimStart($separators)
        $firstPart = $relativePath.Split($separators)[0]
        if ($firstPart -like 'smali*' -and -not (Test-Path -LiteralPath (Join-Path $decodedPath $firstPart))) {
            throw 'Smali overlays require a full code decode. Do not add a one-class Smali folder to a no-src workspace.'
        }
    }
    foreach ($overlayFile in $overlayFiles) {
        $relativePath = $overlayFile.FullName.Substring($overlayPath.Length).TrimStart($separators)
        $destinationPath = Join-Path $decodedPath $relativePath
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destinationPath) | Out-Null
        Copy-Item -LiteralPath $overlayFile.FullName -Destination $destinationPath
    }
}
& $Python (Join-Path $PSScriptRoot 'UpdateWatchCareMetadata.py') --decoded $decodedPath --version-code $VersionCode --version-name $VersionName --label $AppLabel
if ($LASTEXITCODE -ne 0) { throw 'Metadata update failed; build stopped.' }
$buildPath = Join-Path $repoRoot ('work/care-build-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $buildPath,$OutputDirectory | Out-Null
$outputPath = (Resolve-Path -LiteralPath $OutputDirectory).Path
$apkPath = Join-Path $outputPath ("Watch_Care_" + $VersionName + '.apk')
$unsignedPath = Join-Path $buildPath 'unsigned.apk'
$alignedPath = Join-Path $buildPath 'aligned.apk'
& java '-Xmx4g' '-jar' $toolPath b '-j' '6' '-p' (Join-Path $buildPath 'framework') '-o' $unsignedPath $decodedPath
if ($LASTEXITCODE -ne 0) { throw 'Apktool build failed.' }
& $alignPath '-f' '-P' '16' '4' $unsignedPath $alignedPath
if ($LASTEXITCODE -ne 0) { throw 'ZIP alignment failed; use Android SDK Build Tools with -P support.' }
& java '-jar' $signerPath sign '--ks' $keyPath '--ks-key-alias' $KeyAlias '--ks-pass' ("env:" + $StorePasswordEnv) '--key-pass' ("env:" + $KeyPasswordEnv) '--out' $apkPath $alignedPath
if ($LASTEXITCODE -ne 0) { throw 'Signing failed.' }
& java '-jar' $signerPath verify '--verbose' $apkPath
if ($LASTEXITCODE -ne 0) { throw 'Signature verification failed.' }
& $alignPath '-c' '-P' '16' '4' $apkPath
if ($LASTEXITCODE -ne 0) { throw 'ZIP alignment verification failed.' }
$digest = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText(($apkPath + '.sha256'),($digest + '  ' + (Split-Path -Leaf $apkPath) + [Environment]::NewLine),[Text.UTF8Encoding]::new($false))
Write-Output "APK ready: $apkPath"
Write-Output "SHA256: $digest"

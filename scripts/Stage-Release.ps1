param(
    [Parameter(Mandatory)][string]$SignedApk,
    [Parameter(Mandatory)][string]$SdkPath,
    [string]$OutputDirectory = 'dist',
    [string]$DraftTag
)
$ErrorActionPreference = 'Stop'
$tools = Join-Path $SdkPath 'build-tools/36.0.0'
$apk = (Resolve-Path -LiteralPath $SignedApk).Path
$report = & (Join-Path $tools 'apksigner.bat') verify --print-certs $apk
if ($LASTEXITCODE -ne 0 -or $report -notcontains 'Signer #1 certificate SHA-256 digest: 6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65') {
    throw 'Expected a release APK signed with the existing release key.'
}
$package = & (Join-Path $tools 'aapt.exe') dump badging $apk | Select-Object -First 1
if ($LASTEXITCODE -ne 0 -or $package -notmatch "^package: name='com\.magdi\.tube' versionCode='([1-9][0-9]*)'") {
    throw 'APK package or version metadata is invalid.'
}
$code = $Matches[1]
$directory = New-Item -ItemType Directory -Force $OutputDirectory
$destination = Join-Path $directory.FullName 'MM-Tube.apk'
if ($apk -ne $destination) { Copy-Item -LiteralPath $apk -Destination $destination -Force }
if ($DraftTag) {
    if ($DraftTag -notmatch '^v[0-9]+\.[0-9]+\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?$') { throw 'Invalid release tag.' }
    $repo = 'moustafamagdi/MagdiTube'
    $release = gh release view $DraftTag --repo $repo --json isDraft,assets | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or !$release.isDraft -or @($release.assets).Count -ne 0) {
        throw 'Expected an existing draft with no uploaded assets.'
    }
    gh release upload $DraftTag "$destination#versionCode=$code" --repo $repo
    if ($LASTEXITCODE -ne 0) { throw 'Release upload failed.' }
    $uploaded = gh release view $DraftTag --repo $repo --json assets | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or @($uploaded.assets).Count -ne 1 -or
        $uploaded.assets[0].name -cne "MM-Tube.apk" -or $uploaded.assets[0].label -cne "versionCode=$code") {
        throw 'Uploaded asset name or label is incorrect.'
    }
}
[pscustomobject]@{
    path = $destination
    name = 'MM-Tube.apk'
    label = "versionCode=$code"
    sha256 = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
} | ConvertTo-Json -Compress

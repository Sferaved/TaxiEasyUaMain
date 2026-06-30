# Publish release AAB to Google Play (local, no GitHub Actions).
# Requires: keystore/service-account.json or app/keystore/service-account.json
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
    Write-Host $Message -ForegroundColor Cyan
}

function Get-Base64Url([byte[]]$Bytes) {
    [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function Get-Base64UrlFromString([string]$Text) {
    Get-Base64Url ([System.Text.Encoding]::UTF8.GetBytes($Text))
}

function Invoke-GoogleApi {
    param(
        [string]$Method,
        [string]$Url,
        [string]$AccessToken,
        [byte[]]$Body = $null,
        [string]$ContentType = "application/json"
    )
    $headers = @{ Authorization = "Bearer $AccessToken" }
    if ($null -ne $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -Body $Body -ContentType $ContentType
    }
    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -ContentType $ContentType
}

Set-Location $ProjectRoot
Write-Step "===== Google Play publish ====="
Write-Host "Project: $ProjectRoot" -ForegroundColor Gray

$gradleFile = Join-Path $ProjectRoot "app/build.gradle"
if (-not (Test-Path $gradleFile)) {
    throw "build.gradle not found: $gradleFile"
}

$gradle = Get-Content $gradleFile -Raw
if ($gradle -notmatch 'applicationId\s+"([^"]+)"') {
    throw "applicationId not found in build.gradle"
}
$packageName = $Matches[1]
Write-Host "Package: $packageName" -ForegroundColor Gray

$aabPath = Join-Path $ProjectRoot "app/build/outputs/bundle/release/app-release.aab"
if (-not (Test-Path $aabPath)) {
    throw "AAB not found. Run: gradlew.bat bundleRelease`n$aabPath"
}

$jsonCandidates = @(
    (Join-Path $ProjectRoot "keystore/service-account.json"),
    (Join-Path $ProjectRoot "app/keystore/service-account.json")
)
$jsonPath = $jsonCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $jsonPath) {
    throw "service-account.json not found. Copy Google Play API key to:`n  keystore/service-account.json"
}

$sa = Get-Content $jsonPath -Raw | ConvertFrom-Json
if (-not $sa.client_email -or -not $sa.private_key) {
    throw "Invalid service-account.json"
}

$playDir = Join-Path $ProjectRoot "app/play"
$noteFiles = @(
    (Join-Path $playDir "en-US/whatsnew.txt"),
    (Join-Path $playDir "ru-RU/whatsnew.txt"),
    (Join-Path $playDir "uk/whatsnew.txt")
)
foreach ($file in $noteFiles) {
    if (-not (Test-Path $file)) {
        throw "Release notes not found: $file"
    }
    $len = (Get-Content $file -Raw).Length
    if ($len -gt 500) {
        throw "$file is $len chars (Google Play max 500)"
    }
}

Write-Step "Getting access token..."
$headerJson = '{"alg":"RS256","typ":"JWT"}'
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$claimJson = (@{
    iss   = $sa.client_email
    scope = "https://www.googleapis.com/auth/androidpublisher"
    aud   = "https://oauth2.googleapis.com/token"
    exp   = $now + 3600
    iat   = $now
} | ConvertTo-Json -Compress)

$unsigned = "$(Get-Base64UrlFromString $headerJson).$(Get-Base64UrlFromString $claimJson)"

$rsa = [System.Security.Cryptography.RSA]::Create()
try {
    $rsa.ImportFromPem($sa.private_key)
    $signature = $rsa.SignData(
        [System.Text.Encoding]::UTF8.GetBytes($unsigned),
        [System.Security.Cryptography.HashAlgorithmName]::SHA256,
        [System.Security.Cryptography.RSASignaturePadding]::Pkcs1
    )
} catch {
    throw "RSA sign failed (need PowerShell 7+ / .NET 5+): $_"
}

$jwt = "$unsigned.$(Get-Base64Url $signature)"
$tokenResponse = Invoke-RestMethod -Method Post -Uri "https://oauth2.googleapis.com/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"

$accessToken = $tokenResponse.access_token
if ([string]::IsNullOrWhiteSpace($accessToken)) {
    throw "Failed to get Google access token"
}

Write-Step "Creating Play edit..."
$edit = Invoke-GoogleApi -Method Post `
    -Url "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$packageName/edits" `
    -AccessToken $accessToken
$editId = $edit.id
if ([string]::IsNullOrWhiteSpace($editId)) {
    throw "Failed to create Play edit"
}

Write-Step "Uploading AAB..."
$aabBytes = [System.IO.File]::ReadAllBytes($aabPath)
$upload = Invoke-GoogleApi -Method Post `
    -Url "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$packageName/edits/$editId/bundles?uploadType=media" `
    -AccessToken $accessToken `
    -Body $aabBytes `
    -ContentType "application/octet-stream"

$versionCode = $upload.versionCode
if (-not $versionCode) {
    throw "AAB upload failed"
}
Write-Host "Uploaded versionCode: $versionCode" -ForegroundColor Green

$tracks = @("internal", "alpha", "beta", "production")
Write-Step "Updating tracks: $($tracks -join ', ')..."
foreach ($track in $tracks) {
    $body = @{
        releases = @(
            @{
                versionCodes = @([int]$versionCode)
                status       = "completed"
                releaseNotes = @(
                    @{ language = "en-US"; text = (Get-Content $noteFiles[0] -Raw).Trim() }
                    @{ language = "ru-RU"; text = (Get-Content $noteFiles[1] -Raw).Trim() }
                    @{ language = "uk"; text = (Get-Content $noteFiles[2] -Raw).Trim() }
                )
            }
        )
    } | ConvertTo-Json -Depth 6 -Compress

    Invoke-GoogleApi -Method Put `
        -Url "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$packageName/edits/$editId/tracks/$track" `
        -AccessToken $accessToken `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) | Out-Null
}

Write-Step "Committing edit..."
Invoke-GoogleApi -Method Post `
    -Url "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$packageName/edits/${editId}:commit" `
    -AccessToken $accessToken | Out-Null

Write-Host ""
Write-Host "OK: Published to Google Play (versionCode $versionCode)" -ForegroundColor Green

Write-Host "===== PAS_1 - Version Update =====" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$gradleFile = Join-Path $projectRoot "app/build.gradle"

if (!(Test-Path $gradleFile)) {
    Write-Host "ERROR: $gradleFile not found" -ForegroundColor Red
    exit 1
}

# ===== 0. Pre-release checks (unit tests + release build) =====
Write-Host "===== Pre-release checks =====" -ForegroundColor Cyan

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaCandidates = @(
        "$env:LOCALAPPDATA\Programs\Android\Android Studio\jbr",
        "C:\Program Files\Android\Android Studio\jbr",
        "C:\Program Files\Java\jdk-21"
    )
    $foundJava = $false
    foreach ($candidate in $javaCandidates) {
        if (Test-Path "$candidate\bin\java.exe") {
            $env:JAVA_HOME = $candidate
            Write-Host "JAVA_HOME: $candidate" -ForegroundColor Gray
            $foundJava = $true
            break
        }
    }
    if (-not $foundJava) {
        Write-Host "ERROR: JAVA_HOME not set and Java not found" -ForegroundColor Red
        exit 1
    }
}

$gradlew = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host "ERROR: gradlew.bat not found" -ForegroundColor Red
    exit 1
}

Write-Host "Running unit tests (testDebugUnitTest)..." -ForegroundColor Yellow
& $gradlew testDebugUnitTest --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: unit tests failed - version bump cancelled" -ForegroundColor Red
    exit 1
}
Write-Host "OK: unit tests passed" -ForegroundColor Green

Write-Host "Running release build (assembleRelease)..." -ForegroundColor Yellow
& $gradlew assembleRelease --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "assembleRelease failed, trying compileReleaseJavaWithJavac..." -ForegroundColor Yellow
    & $gradlew compileReleaseJavaWithJavac --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: release build check failed - version bump cancelled" -ForegroundColor Red
        exit 1
    }
}
Write-Host "OK: release build check passed" -ForegroundColor Green
Write-Host ""

# ===== 1. Read build.gradle =====
$content = Get-Content $gradleFile -Raw -Encoding UTF8

$versionCodeMatch = [regex]::Match($content, "versionCode\s*=\s*(\d+)")
$versionNameMatch = [regex]::Match($content, "versionName\s*=\s*'([^']+)'")

if (!$versionCodeMatch.Success -or !$versionNameMatch.Success) {
    Write-Host "ERROR: Cannot find versionCode or versionName" -ForegroundColor Red
    exit
}

$currentVersionCode = [int]$versionCodeMatch.Groups[1].Value
$currentVersionName = $versionNameMatch.Groups[1].Value

Write-Host "Current versionCode: $currentVersionCode"
Write-Host "Current versionName: $currentVersionName"
Write-Host ""

# ===== 2. Increment version =====
$newVersionCode = $currentVersionCode + 1
# ВАЖНО: первая цифра версии фиксированная "1.", а хвост идёт 999 -> 1000 -> 1001 ...
# Для PAS_1 версия отображается как 1.(versionCode - 1000), чтобы после 1.999 шло 1.1000.
$visibleSuffix = $newVersionCode - 1000
if ($visibleSuffix -lt 0) {
    Write-Host "ERROR: versionCode too small for PAS_1 scheme (expected >= 1000)" -ForegroundColor Red
    exit 1
}
$newVersionName = "1.$visibleSuffix"

Write-Host "New versionCode: $newVersionCode"
Write-Host "New versionName: $newVersionName"
Write-Host ""

# ===== 3. Update build.gradle =====
$content = $content -replace "versionCode\s*=\s*\d+", "versionCode = $newVersionCode"
$content = $content -replace "versionName\s*=\s*'[^']+'", "versionName = '$newVersionName'"

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
try {
    [System.IO.File]::WriteAllText($gradleFile, $content, $utf8NoBom)
    Write-Host "OK: build.gradle updated" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to write build.gradle: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# ===== 4. Update XML files (с сохранением форматирования) =====
$xmlFiles = @(
    "app/src/main/res/values/strings_app.xml",
    "app/src/main/res/values-ru/strings_app.xml",
    "app/src/main/res/values-uk/strings_app.xml",
    "app/src/main/res/values-en/strings_app.xml"
)

foreach ($file in $xmlFiles) {
    $fullPath = Join-Path $projectRoot $file
    if (!(Test-Path $fullPath)) {
        Write-Host "WARNING: File not found: $fullPath"
        continue
    }

    Write-Host "Updating: $fullPath"

    $content = Get-Content $fullPath -Raw -Encoding UTF8

    # Обновляем version_code (любое число после тега)
    $content = $content -replace '(?<=<string name="version_code">).*?(?=</string>)', $newVersionName

    # Обновляем version в зависимости от языка
    if ($file -like "*values-ru*") {
        $content = $content -replace '(?<=<string name="version">).*?(?=</string>)', "# $newVersionName"
    }
    elseif ($file -like "*values-uk*") {
        $content = $content -replace '(?<=<string name="version">).*?(?=</string>)', "# $newVersionName"
    }
    elseif ($file -like "*values-en*") {
        $content = $content -replace '(?<=<string name="version">).*?(?=</string>)', "# $newVersionName"
    }
    else {
        $content = $content -replace '(?<=<string name="version">).*?(?=</string>)', "# $newVersionName"
    }

    [System.IO.File]::WriteAllText($fullPath, $content, $utf8NoBom)
    Write-Host "   OK: updated" -ForegroundColor Green
}

Write-Host ""
Write-Host "SUCCESS: Version updated to $newVersionName ($newVersionCode)" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# ===================== GIT BLOCK =============================
# ============================================================

Write-Host "===== Git operations =====" -ForegroundColor Yellow

# Показываем изменения
Write-Host "`nChanged files:" -ForegroundColor Cyan
git status --short | Out-Host

Write-Host "`nFull list of changes:" -ForegroundColor Cyan
git diff --stat | Out-Host

# Спрашиваем подтверждение
#$confirm = Read-Host "`nDo you want to commit these changes? (y/n)"
$confirm = 'y'

if ($confirm -ne 'y' -and $confirm -ne 'Y') {
    Write-Host "Git operations cancelled." -ForegroundColor Yellow
    exit
}


# Add changes
Write-Host "`nAdding changes..." -ForegroundColor Yellow
git add .

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: git add failed" -ForegroundColor Red
    exit
}

# Проверяем изменения
$status = git status --porcelain
if ([string]::IsNullOrEmpty($status)) {
    Write-Host "No changes to commit." -ForegroundColor Yellow
    exit
}

# Commit
$commitMessage = "Bump version to $newVersionName ($newVersionCode)"
Write-Host "Committing: $commitMessage" -ForegroundColor Yellow
git commit -m $commitMessage

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: git commit failed" -ForegroundColor Red
    exit
}

# Create tag
$tagName = "v$newVersionName"
Write-Host "Creating tag: $tagName" -ForegroundColor Yellow
$tagExists = git tag -l $tagName
if ($tagExists) {
    Write-Host "Tag $tagName already exists, skipping tag creation" -ForegroundColor Yellow
} else {
    git tag $tagName
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: tag creation failed" -ForegroundColor Red
        exit 1
    }
}

# Спрашиваем про push
#$pushConfirm = Read-Host "`nDo you want to push changes and tag to remote? (y/n)"
$pushConfirm = 'y'

if ($pushConfirm -eq 'y' -or $pushConfirm -eq 'Y') {
    Write-Host "Pushing commit..." -ForegroundColor Yellow
    git push

    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: git push failed" -ForegroundColor Red
        exit
    }

    if (-not $tagExists) {
        Write-Host "Pushing tag..." -ForegroundColor Yellow
        git push origin $tagName
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERROR: tag push failed" -ForegroundColor Red
            exit 1
        }
    }

    Write-Host "OK: Git push completed, tag $tagName ready" -ForegroundColor Green
} else {
    Write-Host "OK: Commit and tag created locally. Push skipped." -ForegroundColor Green
    Write-Host "To push later use: git push && git push origin $tagName/actions" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done." -ForegroundColor Green
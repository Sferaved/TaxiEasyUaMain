Write-Host "===== PAS_1 - Version Update =====" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$gradleFile = "app/build.gradle"

if (!(Test-Path $gradleFile)) {
    Write-Host "ERROR: $gradleFile not found" -ForegroundColor Red
    exit
}

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
$newVersionName = "1." + $newVersionCode.ToString().Substring($newVersionCode.ToString().Length - 3)

Write-Host "New versionCode: $newVersionCode"
Write-Host "New versionName: $newVersionName"
Write-Host ""

# ===== 3. Update build.gradle =====
$content = $content -replace "versionCode\s*=\s*\d+", "versionCode = $newVersionCode"
$content = $content -replace "versionName\s*=\s*'[^']+'", "versionName = '$newVersionName'"

# Сохраняем build.gradle с UTF-8 без BOM
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($gradleFile, $content, $utf8NoBom)
Write-Host "OK: build.gradle updated" -ForegroundColor Green
Write-Host ""

# ===== 4. Update XML files (с сохранением форматирования) =====
$xmlFiles = @(
    "app/src/main/res/values/strings_app.xml",
    "app/src/main/res/values-ru/strings_app.xml",
    "app/src/main/res/values-uk/strings_app.xml",
    "app/src/main/res/values-en/strings_app.xml"
)

foreach ($file in $xmlFiles) {
    if (!(Test-Path $file)) {
        Write-Host "WARNING: File not found: $file"
        continue
    }

    Write-Host "Updating: $file"

    # Читаем файл как текст с UTF-8
    $content = Get-Content $file -Raw -Encoding UTF8

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

    # Сохраняем с UTF-8 без BOM
    [System.IO.File]::WriteAllText($file, $content, $utf8NoBom)
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
git tag $tagName

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: tag creation failed" -ForegroundColor Red
    exit
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

    Write-Host "Pushing tag..." -ForegroundColor Yellow
    git push origin $tagName

    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: tag push failed" -ForegroundColor Red
        exit
    }

    Write-Host "OK: Git push completed, tag $tagName created and pushed" -ForegroundColor Green
} else {
    Write-Host "OK: Commit and tag created locally. Push skipped." -ForegroundColor Green
    Write-Host "To push later use: git push && git push origin $tagName" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done." -ForegroundColor Green
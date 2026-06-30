@echo off
chcp 65001 > nul
echo.
echo ===== PAS deploy (local Google Play) =====
echo   1. Unit tests (testDebugUnitTest)
echo   2. Release build check (assembleRelease)
echo   3. Bump version in build.gradle
echo   4. bundleRelease + publish to Google Play
echo   5. Git commit, tag, push (no GitHub Actions)
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0git_commit.ps1"
if errorlevel 1 (
    echo.
    echo DEPLOY FAILED.
    pause
    exit /b 1
)
echo.
echo DEPLOY OK.
pause

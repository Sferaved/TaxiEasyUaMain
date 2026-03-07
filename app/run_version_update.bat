@echo off
chcp 65001 > nul
powershell -ExecutionPolicy Bypass -File "%~dp0git_commit.ps1"
pause
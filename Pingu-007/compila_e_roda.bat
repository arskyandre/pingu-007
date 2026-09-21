@echo off
setlocal
cls

cd /d "%~dp0"

call ..\gradlew.bat -p .. clean run
exit /b %errorlevel%

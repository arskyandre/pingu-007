@echo off
setlocal
cls

cd /d "%~dp0"

set "LOGFILE=game.log"

javac -cp ".;*" *.java

if errorlevel 1 (
    echo falha em compilacao
    pause
    exit /b 1
)

echo.

powershell -NoProfile -Command ^
  "& { java -cp '.;*' GameCore 2>&1 | Tee-Object -FilePath '%LOGFILE%' }"

echo.
echo log salvo em %LOGFILE%.

del /q "*.class"

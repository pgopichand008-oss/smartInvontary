@echo off
setlocal
title Smart Inventory and Expiry Management System
set "PATH=C:\Users\User\.m2\apache-maven-3.9.9\bin;%PATH%"
cd /d "%~dp0"

if not exist "%~dp0Smart Inventory.lnk" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%~dp0Smart Inventory.lnk'); $s.TargetPath = '%~dp0run-app.bat'; $s.WorkingDirectory = '%~dp0'; $s.IconLocation = '%~dp0app-icon.ico,0'; $s.Save()" >nul 2>&1
)

echo ====================================================================
echo Starting Smart Inventory and Expiry Management Desktop Application...
echo ====================================================================

call mvn.cmd exec:java -Dexec.mainClass="com.smartinventory.Main"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with an error.
    pause
)

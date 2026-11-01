@echo off
setlocal EnableDelayedExpansion
title Smart Inventory and Expiry Management System
cd /d "%~dp0"

echo ===============================================================================
echo        Smart Inventory ^& Expiry Management Desktop Application
echo ===============================================================================
echo.

:: -----------------------------------------------------------------------------
:: 1. Detect Java Runtime Environment (Requires Java 17 or newer)
:: -----------------------------------------------------------------------------
set "JAVA_CMD="

:: Check system PATH
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 set "JAVA_CMD=java"

:: Check JAVA_HOME
if not defined JAVA_CMD (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

:: Check standard Windows 64-bit JDK install locations
if not defined JAVA_CMD (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\java.exe" set "JAVA_CMD=%%D\bin\java.exe"
    )
)
if not defined JAVA_CMD (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
        if exist "%%D\bin\java.exe" set "JAVA_CMD=%%D\bin\java.exe"
    )
)
if not defined JAVA_CMD (
    for /d %%D in ("C:\Program Files\Microsoft\jdk*") do (
        if exist "%%D\bin\java.exe" set "JAVA_CMD=%%D\bin\java.exe"
    )
)

if not defined JAVA_CMD goto :JAVA_NOT_FOUND

:: Verify Java execution
echo [1/4] Checking Java Runtime...
"%JAVA_CMD%" -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to execute Java at: %JAVA_CMD%
    pause
    exit /b 1
)

for /f "tokens=*" %%g in ('%JAVA_CMD% -version 2^>^&1') do (
    set "JAVA_VER_STR=%%~g"
    goto :JAVA_VER_FOUND
)
:JAVA_VER_FOUND
echo       Detected Java: !JAVA_VER_STR!

:: -----------------------------------------------------------------------------
:: 2. Verify Database Configuration (db.properties)
:: -----------------------------------------------------------------------------
echo [2/4] Verifying database configuration...

if not exist "%~dp0db.properties" (
    if exist "%~dp0db.properties.example" (
        echo.
        echo [SETUP NOTICE] 'db.properties' not found in application folder.
        echo                Auto-creating 'db.properties' from 'db.properties.example'...
        copy "%~dp0db.properties.example" "%~dp0db.properties" >nul
        echo [SETUP NOTICE] File created: %~dp0db.properties
        echo.
        echo ===============================================================================
        echo [ACTION REQUIRED] Please configure your MySQL credentials:
        echo ===============================================================================
        echo 1. Open 'db.properties' in Notepad.
        echo 2. Update 'db.user' and 'db.password' with your MySQL credentials.
        echo 3. Ensure MySQL Server is running on port 3306.
        echo ===============================================================================
        echo.
        pause
    ) else (
        echo [WARNING] Neither 'db.properties' nor 'db.properties.example' was found.
        echo           The application will attempt to connect using internal defaults.
    )
) else (
    echo       Database configuration found: %~dp0db.properties
)

:: -----------------------------------------------------------------------------
:: 3. Locate Application Runnable JAR
:: -----------------------------------------------------------------------------
echo [3/4] Locating application JAR...
set "APP_JAR="

if exist "%~dp0smart-inventory-system-1.0.0.jar" (
    set "APP_JAR=%~dp0smart-inventory-system-1.0.0.jar"
) else if exist "%~dp0target\smart-inventory-system-1.0.0.jar" (
    set "APP_JAR=%~dp0target\smart-inventory-system-1.0.0.jar"
)

if not defined APP_JAR goto :JAR_NOT_FOUND
echo       Application JAR located: !APP_JAR!

:: -----------------------------------------------------------------------------
:: 4. Create Desktop Shortcut if Not Exists
:: -----------------------------------------------------------------------------
if not exist "%~dp0Smart Inventory.lnk" (
    if exist "%~dp0app-icon.ico" (
        powershell -NoProfile -ExecutionPolicy Bypass -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%~dp0Smart Inventory.lnk'); $s.TargetPath = '%~dp0run-app.bat'; $s.WorkingDirectory = '%~dp0'; $s.IconLocation = '%~dp0app-icon.ico,0'; $s.Save()" >nul 2>&1
    )
)

:: -----------------------------------------------------------------------------
:: 5. Launch the Desktop Application
:: -----------------------------------------------------------------------------
echo [4/4] Starting Smart Inventory and Expiry Management System...
echo.

"%JAVA_CMD%" -jar "!APP_JAR!"

set "APP_EXIT_CODE=%ERRORLEVEL%"
if %APP_EXIT_CODE% NEQ 0 (
    echo.
    echo ===============================================================================
    echo [NOTICE] Application closed with exit code %APP_EXIT_CODE%.
    echo ===============================================================================
    echo If the application encountered an error, review the messages above.
    pause
)

exit /b %APP_EXIT_CODE%

:: -----------------------------------------------------------------------------
:: Error Handlers
:: -----------------------------------------------------------------------------
:JAVA_NOT_FOUND
echo ===============================================================================
echo [ERROR] Java Runtime Environment is NOT installed or not in PATH.
echo ===============================================================================
echo The Smart Inventory ^& Expiry Management System requires Java 17 or higher.
echo.
echo To install Java on Windows:
echo   1. Download Eclipse Temurin JDK 17 LTS or 21 LTS from:
echo      https://adoptium.net/
echo   2. Run the installer and select:
echo      - Add to PATH
echo      - Set JAVA_HOME variable
echo   3. Re-launch this script after installation completes.
echo ===============================================================================
echo.
pause
exit /b 1

:JAR_NOT_FOUND
echo ===============================================================================
echo [ERROR] Application executable JAR not found!
echo ===============================================================================
echo Looked for:
echo   - %~dp0smart-inventory-system-1.0.0.jar
echo   - %~dp0target\smart-inventory-system-1.0.0.jar
echo.
echo If building from source, build the JAR first using:
echo   mvn clean package -DskipTests
echo ===============================================================================
pause
exit /b 1

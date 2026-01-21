@echo off
setlocal enabledelayedexpansion

:: Change to tools directory (where this script lives)
cd /d "%~dp0"

:: Check for debug parameter
set "BUILD_MODE=release"
if /i "%~1"=="debug" set "BUILD_MODE=debug"

echo ========================================
if "%BUILD_MODE%"=="debug" (
    echo Build and Upload Android Debug APK
) else (
    echo Build and Upload Android Release APK
)
echo ========================================
echo.

:: Step 1-3: Build Android APK using shared build script
call "%~dp0build_android.bat" %BUILD_MODE%
if errorlevel 1 (
    echo.
    echo ERROR: Build failed!
    exit /b 1
)

:: Step 4: Upload to FTP
echo [Step 4/4] Uploading to FTP...
if "%BUILD_MODE%"=="debug" (
    call "%~dp0upload_debug-apk-to-ftp.bat"
) else (
    call "%~dp0upload_release-apk-to-ftp.bat"
)
if errorlevel 1 (
    echo.
    echo ERROR: Upload failed!
    exit /b 1
)

:: Get version from libs.versions.toml
set "VERSION=Unknown"
set "VERSIONS_FILE=%~dp0..\gradle\libs.versions.toml"
if exist "%VERSIONS_FILE%" (
    for /f "tokens=2 delims== " %%a in ('findstr /r "^versionName" "%VERSIONS_FILE%"') do (
        set "VERSION=%%~a"
    )
)

echo.
echo ========================================
echo Build and upload completed successfully!
echo Version: %VERSION%
echo ========================================
echo.
endlocal

cd %~dp0
